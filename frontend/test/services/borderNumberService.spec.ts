import { createTestingPinia } from "@pinia/testing"
import { setActivePinia } from "pinia"
import DocumentUnit, {
  longTextLabels,
  LongTexts,
  ShortTexts,
} from "@/domain/documentUnit"
import ParticipatingJudge from "@/domain/participatingJudge"
import borderNumberService from "@/services/borderNumberService"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

function mockDocUnitStore({
  shortTexts = {},
  longTexts = {},
}: {
  shortTexts?: ShortTexts
  longTexts?: LongTexts
}) {
  const mockedSessionStore = useDocumentUnitStore()
  mockedSessionStore.documentUnit = new DocumentUnit("q834", {
    shortTexts,
    longTexts,
  })

  return mockedSessionStore
}

function borderNumber(number: number | string) {
  return `<border-number><number>${number}</number><content><span>Text</span></content></border-number>`
}

function borderNumberLink(
  number: number | string,
  { valid }: { valid: boolean } = { valid: true },
) {
  return `<border-number-link nr="${number}" valid="${valid}">${number}</border-number-link>`
}

describe("borderNumberService", () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia())
  })

  describe("makeBorderNumbersSequential", () => {
    it("should handle empty long and short texts", () => {
      mockDocUnitStore({})
      expect(() =>
        borderNumberService.makeBorderNumbersSequential(),
      ).not.toThrow()
    })

    it("should handle missing border-number number node", () => {
      const errorLogSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => null)

      mockDocUnitStore({
        longTexts: {
          reasons: `<border-number></border-number>${borderNumber(10)}`,
        },
      })

      expect(() =>
        borderNumberService.makeBorderNumbersSequential(),
      ).not.toThrow()

      expect(errorLogSpy).toHaveBeenCalledOnce()
      expect(errorLogSpy).toHaveBeenCalledWith(
        expect.stringContaining("Could not make border numbers sequential"),
        expect.any(Error),
      )
    })

    it("should start border numbers with 1", () => {
      const store = mockDocUnitStore({
        longTexts: {
          reasons: borderNumber(2),
        },
      })
      borderNumberService.makeBorderNumbersSequential()
      expect(store.documentUnit?.longTexts.reasons).toEqual(borderNumber(1))
    })

    it("should handle nested border numbers", () => {
      const store = mockDocUnitStore({
        longTexts: {
          reasons: `<p><span>${borderNumber(0)}</span></p>`,
        },
      })
      borderNumberService.makeBorderNumbersSequential()
      expect(store.documentUnit?.longTexts.reasons).toEqual(
        `<p><span>${borderNumber(1)}</span></p>`,
      )
    })

    it("should handle multiple long texts and border numbers", () => {
      const store = mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumber(0)}${borderNumber(" 10 ")}`,
          caseFacts: `${borderNumber(4)}${borderNumber(8)}`,
          decisionReasons: `${borderNumber(4)}${borderNumber(8)}`,
          dissentingOpinion: `${borderNumber(2)}${borderNumber(5)}`,
          otherLongText: `${borderNumber(2)}${borderNumber(5)}`,
        },
      })
      borderNumberService.makeBorderNumbersSequential()

      expect(store.documentUnit?.longTexts.reasons).toEqual(
        `${borderNumber(1)}${borderNumber(2)}`,
      )
      expect(store.documentUnit?.longTexts.caseFacts).toEqual(
        `${borderNumber(3)}${borderNumber(4)}`,
      )
      expect(store.documentUnit?.longTexts.decisionReasons).toEqual(
        `${borderNumber(5)}${borderNumber(6)}`,
      )
      expect(store.documentUnit?.longTexts.dissentingOpinion).toEqual(
        `${borderNumber(7)}${borderNumber(8)}`,
      )
      expect(store.documentUnit?.longTexts.otherLongText).toEqual(
        `${borderNumber(9)}${borderNumber(10)}`,
      )
    })

    it("should update existing border links if referenced border number changed", () => {
      const store = mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumber(1)}${borderNumber(3)}`,
          otherLongText: `${borderNumber(1)}${borderNumberLink(3)}`,
        },
        shortTexts: {
          headline: `${borderNumberLink(3)}`,
        },
      })
      borderNumberService.makeBorderNumbersSequential()

      expect(store.documentUnit?.shortTexts.headline).toEqual(
        `${borderNumberLink(2)}`,
      )

      expect(store.documentUnit?.longTexts.otherLongText).toEqual(
        `${borderNumber(3)}${borderNumberLink(2)}`,
      )
    })

    it("should not update an existing border link if unchanged", () => {
      const store = mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumber(1)}${borderNumber(3)}`,
        },
        shortTexts: {
          headline: `${borderNumberLink(1)}${borderNumberLink(2)}`,
        },
      })
      borderNumberService.makeBorderNumbersSequential()

      expect(store.documentUnit?.shortTexts.headline).toEqual(
        `${borderNumberLink(1)}${borderNumberLink(2)}`,
      )
    })

    it("should not change border numbers in other longtexts", () => {
      const judge = new ParticipatingJudge({ name: "judge" })
      const store = mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumber(1)}${borderNumber(3)}`,
          tenor: `${borderNumber(13)}${borderNumber(30)}`,
          participatingJudges: [judge],
        },
      })
      borderNumberService.makeBorderNumbersSequential()

      expect(store.documentUnit?.longTexts.reasons).toEqual(
        `${borderNumber(1)}${borderNumber(2)}`,
      )
      expect(store.documentUnit?.longTexts.tenor).toEqual(
        `${borderNumber(13)}${borderNumber(30)}`,
      )
      expect(store.documentUnit?.longTexts.participatingJudges).toEqual([judge])
    })
  })

  describe("validateBorderNumbers", () => {
    it("should return valid for empty texts", () => {
      mockDocUnitStore({})
      const validationResult = borderNumberService.validateBorderNumbers()
      expect(validationResult.isValid).toBe(true)
    })

    it("should return valid for texts without border numbers", () => {
      mockDocUnitStore({
        longTexts: {
          reasons: `<p>Text</p><p><span>Nested</span></p>`,
          tenor: `No Structure`,
          participatingJudges: [new ParticipatingJudge({ name: "judge" })],
          dissentingOpinion: "Some text",
        },
      })
      const validationResult = borderNumberService.validateBorderNumbers()
      expect(validationResult.isValid).toBe(true)
    })

    it("should handle invalid border-number node", () => {
      const errorLogSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => null)

      mockDocUnitStore({
        longTexts: {
          reasons: `<border-number></border-number>${borderNumber(10)}`,
        },
      })

      const validationResult = borderNumberService.validateBorderNumbers()
      expect(validationResult.isValid).toBe(false)
      expect(validationResult.hasError).toBe(true)

      expect(errorLogSpy).toHaveBeenCalledOnce()
      expect(errorLogSpy).toHaveBeenCalledWith(
        expect.stringContaining("Could not validate border numbers"),
        expect.any(Error),
      )
    })

    it("should return valid for texts with valid border numbers", () => {
      mockDocUnitStore({
        longTexts: {
          tenor: "Rubrik ohne Randnummern",
          reasons: `${borderNumber(1)}<p>${borderNumber(" 2 ")}</p>`,
          decisionReasons: "Text ohne Randnummern",
          dissentingOpinion: `${borderNumber(3)}<p>${borderNumber(4)}</p>`,
          otherLongText: `<ul><li>${borderNumber(5)}</li>${borderNumber(6)}</ul>`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumbers()
      expect(validationResult.isValid).toBe(true)
    })

    it("should return invalid and include the first invalid position", () => {
      mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumber(1)}<p>${borderNumber(2)}</p>`,
          decisionReasons: `${borderNumber(3)}${borderNumber(3)}`,
          otherLongText: `<ul><li>${borderNumber(4)}</li>${borderNumber(6)}</ul>`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumbers()
      expect(validationResult.isValid).toBe(false)
      expect(validationResult.hasError).toBe(false)
      if (!validationResult.isValid && !validationResult.hasError) {
        expect(validationResult.invalidCategory).toBe("decisionReasons")
        expect(validationResult.firstInvalidBorderNumber).toBe("3")
        expect(validationResult.expectedBorderNumber).toBe(4)
      }
    })

    it("should return invalid for non-number border numbers", () => {
      mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumber("a")}`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumbers()
      expect(validationResult.isValid).toBe(false)
      expect(validationResult.hasError).toBe(false)
      if (!validationResult.isValid && !validationResult.hasError) {
        expect(validationResult.invalidCategory).toBe("reasons")
        expect(validationResult.firstInvalidBorderNumber).toBe("a")
        expect(validationResult.expectedBorderNumber).toBe(1)
      }
    })

    it("should return invalid for empty border numbers", () => {
      mockDocUnitStore({
        longTexts: {
          otherLongText: `${borderNumber("")}`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumbers()
      expect(validationResult.isValid).toBe(false)
      expect(validationResult.hasError).toBe(false)
      if (!validationResult.isValid && !validationResult.hasError) {
        expect(validationResult.invalidCategory).toBe("otherLongText")
        expect(validationResult.firstInvalidBorderNumber).toBe("")
        expect(validationResult.expectedBorderNumber).toBe(1)
      }
    })
  })

  describe("validateBorderNumberLinks", () => {
    it("should return valid for empty texts", () => {
      mockDocUnitStore({})
      const validationResult = borderNumberService.validateBorderNumberLinks()
      expect(validationResult.isValid).toBe(true)
    })

    it("should handle invalid html nodes", () => {
      mockDocUnitStore({
        longTexts: {
          reasons: `<border-number></border-number>`,
        },
        shortTexts: {
          headline: `<border-number-link></border-number-link>`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumberLinks()
      expect(validationResult.isValid).toBe(true)
    })

    it("should return valid for texts without border numbers", () => {
      mockDocUnitStore({
        longTexts: {
          reasons: `<p>Text</p><p><span>Nested</span></p>`,
          tenor: `No Structure`,
          participatingJudges: [new ParticipatingJudge({ name: "judge" })],
          dissentingOpinion: "Some text",
        },
      })
      const validationResult = borderNumberService.validateBorderNumberLinks()
      expect(validationResult.isValid).toBe(true)
    })

    it("should return valid for texts with valid border numbers", () => {
      mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumber(1)}<p>${borderNumber(" 2 ")}</p>`,
          decisionReasons: "Text ohne Randnummern",
          otherLongText: `<ul><li>${borderNumber(3)}</li>${borderNumber(4)}</ul>`,
          tenor: "Rubrik ohne Randnummern",
          dissentingOpinion: `${borderNumber(1)}<p>${borderNumber(2)}</p>`,
        },
        shortTexts: {
          headline: `${borderNumberLink(1)}${borderNumberLink(2)}`,
          decisionName: `${borderNumberLink(3)}${borderNumberLink(4)}`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumberLinks()
      expect(validationResult.isValid).toBe(true)
    })

    it("should allow for the same border number to be linked multiple times", () => {
      mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumber(1)}<p>${borderNumber(2)}</p>`,
          dissentingOpinion: `${borderNumber(3)}`,
        },
        shortTexts: {
          headline: `${borderNumberLink(1)}${borderNumberLink(2)}`,
          decisionName: `${borderNumberLink(2)}${borderNumberLink(2)}`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumberLinks()
      expect(validationResult.isValid).toBe(true)
    })

    it("should return invalid for non-existent border number", () => {
      mockDocUnitStore({
        shortTexts: {
          headline: `${borderNumberLink(1)}`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumberLinks()
      expect(validationResult.isValid).toBe(false)
      if (!validationResult.isValid) {
        expect(validationResult.invalidCategories).toEqual(["headline"])
      }
    })

    it("should return invalid for multiple non-existent border number", () => {
      mockDocUnitStore({
        longTexts: {
          reasons: `${borderNumberLink(1)}${borderNumber(3)}`,
          otherLongText: `${borderNumberLink(1)}${borderNumberLink(2)}`,
        },
        shortTexts: {
          headnote: `${borderNumberLink(1)}`,
          headline: `${borderNumberLink(3)}`,
        },
      })
      const validationResult = borderNumberService.validateBorderNumberLinks()
      expect(validationResult.isValid).toBe(false)
      if (!validationResult.isValid) {
        expect(validationResult.invalidCategories).toEqual([
          "reasons",
          "otherLongText",
          "headnote",
        ])
      }
    })
  })

  describe("invalidateBorderNumberLinks", () => {
    it("should return valid for empty texts", () => {
      const store = mockDocUnitStore({})
      borderNumberService.invalidateBorderNumberLinks(["1", "2"])
      expect(store.documentUnit).toEqual(store.documentUnit)
    })

    it("should handle invalid html nodes", () => {
      const store = mockDocUnitStore({
        longTexts: {
          reasons: `<border-number></border-number>`,
        },
        shortTexts: {
          headline: `<border-number-link></border-number-link>`,
        },
      })
      borderNumberService.invalidateBorderNumberLinks(["1", "2"])
      expect(store.documentUnit).toEqual(store.documentUnit)
    })

    it("should not change different border number links", () => {
      const store = mockDocUnitStore({
        shortTexts: { headline: borderNumberLink(3) },
      })
      borderNumberService.invalidateBorderNumberLinks(["2"])
      expect(store.documentUnit?.shortTexts.headline).toEqual(
        borderNumberLink(3),
      )
    })

    it("should invalidate affected border number links", () => {
      const store = mockDocUnitStore({
        shortTexts: { headline: borderNumberLink(2) },
      })
      borderNumberService.invalidateBorderNumberLinks(["2"])
      expect(store.documentUnit?.shortTexts.headline).toEqual(
        borderNumberLink("entfernt", { valid: false }),
      )
    })

    it("should invalidate affected multiple border number links in all categories", () => {
      const store = mockDocUnitStore({
        shortTexts: {
          headline: `${borderNumberLink(2)} Text ${borderNumberLink(2, { valid: false })}`,
          headnote: `${borderNumberLink(5)}${borderNumberLink(6)}`,
          decisionName: `${borderNumberLink(2)}${borderNumberLink(5)}`,
          guidingPrinciple: borderNumberLink(2),
          otherHeadnote: borderNumberLink(2),
        },
        longTexts: {
          reasons: borderNumberLink(2),
          otherLongText: borderNumberLink(2),
          participatingJudges: [new ParticipatingJudge()],
          dissentingOpinion: borderNumberLink(2),
          decisionReasons: borderNumberLink(2),
          caseFacts: borderNumberLink(2),
          tenor: borderNumberLink(2),
          outline: borderNumberLink(2),
        },
      })
      borderNumberService.invalidateBorderNumberLinks(["2", "5"])
      expect(store.documentUnit?.shortTexts.headline).toEqual(
        `${borderNumberLink("entfernt", { valid: false })} Text ${borderNumberLink("entfernt", { valid: false })}`,
      )
      expect(store.documentUnit?.shortTexts.headnote).toEqual(
        `${borderNumberLink("entfernt", { valid: false })}${borderNumberLink(6)}`,
      )
      expect(store.documentUnit?.shortTexts.decisionName).toEqual(
        `${borderNumberLink("entfernt", { valid: false })}${borderNumberLink("entfernt", { valid: false })}`,
      )
      expect(store.documentUnit?.shortTexts.guidingPrinciple).toEqual(
        borderNumberLink("entfernt", { valid: false }),
      )
      expect(store.documentUnit?.shortTexts.otherHeadnote).toEqual(
        borderNumberLink("entfernt", { valid: false }),
      )

      Object.keys(longTextLabels).forEach(
        (key) =>
          key !== "participatingJudges" &&
          expect(store.documentUnit?.longTexts[key as keyof LongTexts]).toEqual(
            borderNumberLink("entfernt", { valid: false }),
          ),
      )
    })
  })
})
