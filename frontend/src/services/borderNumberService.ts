import { storeToRefs } from "pinia"
import DocumentUnit, { LongTexts, ShortTexts } from "@/domain/documentUnit"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

type ShortTextKeys = keyof ShortTexts
type LongTextKeys = keyof Omit<LongTexts, "participatingJudges">
const orderedCategoriesWithBorderNumbers: LongTextKeys[] = [
  "reasons",
  "caseFacts",
  "decisionReasons",
  "dissentingOpinion",
  "otherLongText",
]

/**
 * Will take all border numbers in the long text and renumber them sequentially starting from the given number.
 *
 * @param longText text for which the border numbers will be updated
 * @param nextBorderNumber The first border number in this text will start with this number
 */
function makeBorderNumbersSequentialForCategory(
  longText: string,
  nextBorderNumber: number,
) {
  const updatedBorderNumbers = new Map<string, number>()
  const textXml = new DOMParser().parseFromString(longText, "text/html")
  const borderNumbers = textXml.getElementsByTagName("border-number")
  for (const borderNumber of borderNumbers) {
    const [numberElement] = <HTMLCollectionOf<HTMLElement>>(
      borderNumber.getElementsByTagName("number")
    )
    // innerText is not available in JSDom
    const originalLinkTarget = (numberElement.innerHTML ?? "").trim()
    numberElement.innerHTML = `${nextBorderNumber}`
    updatedBorderNumbers.set(originalLinkTarget, nextBorderNumber)
    nextBorderNumber++
  }
  const updatedText = textXml.body.innerHTML
  return { nextBorderNumber, updatedText, updatedBorderNumbers }
}

/**
 * Ensures that border numbers in this text are sequential and start with given number
 *
 * @param longText text to be validated
 * @param nextExpectedBorderNumber The first border number in this text should start with this number
 */
function validateBorderNumbersForCategory(
  longText: string,
  nextExpectedBorderNumber: number,
):
  | { isValid: true; nextBorderNumber: number }
  | {
      isValid: false
      firstInvalidBorderNumber: string
      expectedBorderNumber: number
    } {
  const textXml = new DOMParser().parseFromString(longText, "text/html")
  const borderNumbers = textXml.getElementsByTagName("border-number")
  for (const borderNumber of borderNumbers) {
    const [numberElement] = <HTMLCollectionOf<HTMLElement>>(
      borderNumber.getElementsByTagName("number")
    )
    // innerText is not available in JSDom
    const borderNumberContent = (numberElement.innerHTML ?? "").trim()
    if (`${nextExpectedBorderNumber}` !== borderNumberContent) {
      return {
        isValid: false,
        firstInvalidBorderNumber: borderNumberContent,
        expectedBorderNumber: nextExpectedBorderNumber,
      }
    }
    nextExpectedBorderNumber++
  }
  return { isValid: true, nextBorderNumber: nextExpectedBorderNumber }
}

/**
 * Update all links in given text according to the updated border numbers.
 * @param text short- or longtext to be updated
 * @param updatedBorderNumbers border numbers that have been changed from (key) -> to (value)
 */
function updateBorderNumberLinksForText(
  text: string,
  updatedBorderNumbers: Map<string, number>,
) {
  const textXml = new DOMParser().parseFromString(text, "text/html")
  const borderNumberLinks = textXml.getElementsByTagName(
    "border-number-link",
  ) as HTMLCollectionOf<HTMLElement>

  for (const link of borderNumberLinks) {
    const linkNumber = link.getAttribute("nr")
    if (linkNumber && updatedBorderNumbers.has(linkNumber)) {
      const newLinkNumber = `${updatedBorderNumbers.get(linkNumber)}`
      link.textContent = newLinkNumber
      link.setAttribute("nr", newLinkNumber)
    }
  }

  return textXml.body.innerHTML
}

/**
 * Iterates over all short and long texts to update the border number links.
 * @param documentUnit of which border numbers were already updated
 * @param updatedBorderNumbers border numbers that have been changed from (key) -> to (value)
 */
function updateBorderNumberLinks(
  documentUnit: DocumentUnit,
  updatedBorderNumbers: Map<string, number>,
) {
  const shortTextNames = Object.keys(documentUnit.shortTexts) as ShortTextKeys[]
  for (const shortTextName of shortTextNames) {
    const shortText = documentUnit.shortTexts[shortTextName]
    if (shortText) {
      documentUnit.shortTexts[shortTextName] = updateBorderNumberLinksForText(
        shortText,
        updatedBorderNumbers,
      )
    }
  }

  const longTextNames = Object.keys(documentUnit.longTexts).filter(
    (key) => key !== "participatingJudges",
  ) as LongTextKeys[]
  for (const longTextName of longTextNames) {
    const longText = documentUnit.longTexts[longTextName]
    if (longText) {
      documentUnit.longTexts[longTextName] = updateBorderNumberLinksForText(
        longText,
        updatedBorderNumbers,
      )
    }
  }
}

/**
 * Returns all categories with invalid border number links.
 */
function getCategoriesWithInvalidLinks(
  documentUnit: DocumentUnit,
): (ShortTextKeys | LongTextKeys)[] {
  const texts = [
    ...Object.entries(documentUnit.longTexts).filter(
      ([key, value]) => key !== "participatingJudges" && value,
    ),
    ...Object.entries(documentUnit.shortTexts).filter(([, value]) => value),
  ] as [ShortTextKeys | LongTextKeys, string][] // cast needed because of participatingJudges

  const borderNumbers = texts
    .map(([, text]) => new DOMParser().parseFromString(text, "text/html"))
    .flatMap((textXml) =>
      [...textXml.getElementsByTagName("border-number")]
        .map((borderNumber) =>
          borderNumber.getElementsByTagName("number").item(0),
        )
        .filter((content) => !!content)
        .map((borderNumberContent) => borderNumberContent.innerHTML.trim()),
    )

  const invalidCategories: (ShortTextKeys | LongTextKeys)[] = []
  for (const [categoryName, text] of texts) {
    const textXml = new DOMParser().parseFromString(text, "text/html")
    const areLinksValid = [
      ...textXml.getElementsByTagName("border-number-link"),
    ]
      .map((link) => link.getAttribute("nr"))
      .filter((link): link is string => !!link)
      .every((link) => borderNumbers.some((number) => number === link))

    if (!areLinksValid) {
      invalidCategories.push(categoryName)
    }
  }
  return invalidCategories
}

type ParsedDocumentPerCategory =
  | [group: "shortTexts", categoryName: ShortTextKeys, textXml: Document]
  | [group: "longTexts", categoryName: LongTextKeys, textXml: Document]
function getParsedDocumentPerCategory(
  texts: ShortTexts | Omit<LongTexts, "participatingJudges">,
  group: "shortTexts" | "longTexts",
): ParsedDocumentPerCategory[] {
  return Object.entries(texts)
    .filter(([, text]) => !!text)
    .map(([key, text]) => [
      group,
      key,
      new DOMParser().parseFromString(text, "text/html"),
    ]) as ParsedDocumentPerCategory[]
}

/**
 * Returns all categories with invalid border number links.
 */
function invalidateBorderNumberLinks(
  documentUnit: DocumentUnit,
  numbersToBeInvalidated: string[],
): void {
  const { participatingJudges, ...longTexts } = documentUnit.longTexts
  const texts = [
    ...getParsedDocumentPerCategory(documentUnit.shortTexts, "shortTexts"),
    ...getParsedDocumentPerCategory(longTexts, "longTexts"),
  ]

  for (const [group, categoryName, textXml] of texts) {
    ;[...textXml.getElementsByTagName("border-number-link")]
      .filter((link) =>
        numbersToBeInvalidated.includes(link.getAttribute("nr")!),
      )
      .forEach((link) => {
        link.setAttribute("valid", "false")
        link.setAttribute("nr", "entfernt")
        link.innerHTML = "entfernt"
      })

    if (group === "shortTexts") {
      // This if-statement is needed for the compiler to know that group and corresponding categoryName match.
      documentUnit[group][categoryName] = textXml.body?.innerHTML
    } else {
      documentUnit[group][categoryName] = textXml.body.innerHTML
    }
  }
}

const borderNumberService = {
  /**
   * Updates the border numbers of all long texts with border numbers see {@linkcode orderedCategoriesWithBorderNumbers}
   * and makes them sequential.
   *
   * Updates all existing border number links that reference a border number that was updated previously.
   */
  makeBorderNumbersSequential: () => {
    try {
      const { documentUnit } = storeToRefs(useDocumentUnitStore())
      let nextBorderNumberCount = 1
      // key: original border number  -> value: new border number
      let allUpdatedBorderNumbers = new Map<string, number>()
      for (const category of orderedCategoriesWithBorderNumbers) {
        const longText = documentUnit.value!.longTexts[category]
        if (longText) {
          const { nextBorderNumber, updatedText, updatedBorderNumbers } =
            makeBorderNumbersSequentialForCategory(
              longText,
              nextBorderNumberCount,
            )
          nextBorderNumberCount = nextBorderNumber
          documentUnit.value!.longTexts[category] = updatedText
          allUpdatedBorderNumbers = new Map([
            ...allUpdatedBorderNumbers,
            ...updatedBorderNumbers,
          ])
        }
      }

      const hasUpdatedAnyBorderNumber = allUpdatedBorderNumbers.size > 0
      if (hasUpdatedAnyBorderNumber) {
        updateBorderNumberLinks(
          documentUnit.value as DocumentUnit,
          allUpdatedBorderNumbers,
        )
      }
    } catch (e) {
      console.error(
        "Could not make border numbers sequential. Invalid HTML?",
        e,
      )
    }
  },

  /**
   * Validates that all border numbers are sequential in the texts where we expect border numbers.
   */
  validateBorderNumbers: (): BorderNumberValidationResult => {
    try {
      const { documentUnit } = storeToRefs(useDocumentUnitStore())
      let nextExpectedBorderNumber = 1
      for (const category of orderedCategoriesWithBorderNumbers) {
        const longText = documentUnit.value!.longTexts[category]
        if (longText) {
          const validationResult = validateBorderNumbersForCategory(
            longText,
            nextExpectedBorderNumber,
          )
          if (!validationResult.isValid) {
            return {
              ...validationResult,
              invalidCategory: category,
              hasError: false,
            }
          } else {
            nextExpectedBorderNumber = validationResult.nextBorderNumber
          }
        }
      }

      return { isValid: true, hasError: false }
    } catch (e) {
      console.error("Could not validate border numbers. Invalid HTML?", e)
      return { isValid: false, hasError: true }
    }
  },

  /**
   * Validates that all border number links point to an existing border number.
   * Returns all categories in which invalid links are found.
   */
  validateBorderNumberLinks: (): BorderNumberLinkValidationResult => {
    const { documentUnit } = storeToRefs(useDocumentUnitStore())

    const invalidCategories = getCategoriesWithInvalidLinks(
      documentUnit.value as DocumentUnit,
    )

    if (invalidCategories.length > 0) {
      return { isValid: false, invalidCategories }
    } else {
      return { isValid: true }
    }
  },

  invalidateBorderNumberLinks: (numbersToBeInvalidated: string[]): void => {
    const { documentUnit } = storeToRefs(useDocumentUnitStore())

    invalidateBorderNumberLinks(
      documentUnit.value as DocumentUnit,
      numbersToBeInvalidated,
    )
  },
}

export type BorderNumberValidationResult =
  | { isValid: true; hasError: false }
  | {
      isValid: false
      hasError: false
      /** The category in which the inconsistent border number was found */
      invalidCategory: LongTextKeys
      /** This is the text we found in the border number */
      firstInvalidBorderNumber: string
      /** This is the expected text/position of the border number */
      expectedBorderNumber: number
    }
  | { isValid: false; hasError: true }

export type BorderNumberLinkValidationResult =
  | { isValid: true }
  | {
      isValid: false
      /** The categories in which an invalid border number link was found */
      invalidCategories: (LongTextKeys | ShortTextKeys)[]
    }

export default borderNumberService
