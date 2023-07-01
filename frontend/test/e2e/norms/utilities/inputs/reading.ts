import { Page, expect } from "@playwright/test"
import {
  AnyField,
  FieldType,
  FieldValueTypeMapping,
  FootnoteInputType,
  MetadataInputSection,
} from "./types"
import { FOOTNOTE_LABELS } from "@/components/footnotes/types"
import { MetadatumType } from "@/domain/Norm"

type FieldExpecter<T> = (page: Page, id: string, value: T) => Promise<void>

type FieldExpectMapping = {
  [Type in FieldType]: FieldExpecter<FieldValueTypeMapping[Type]>
}

const expectTextInput: FieldExpecter<string> = async (page, id, value) => {
  const content = await page.locator(`input#${id}`).inputValue()
  expect(content).toBe(value)
}

const expectTextArea: FieldExpecter<string> = async (page, id, value) => {
  const content = await page.locator(`textarea#${id}`).inputValue()
  expect(content.trim()).toBe(value)
}

const expectCheckbox: FieldExpecter<boolean> = async (page, id, value) => {
  const checked = await page.locator(`input#${id}`).isChecked()
  expect(checked).toBe(value)
}

const expectRadioButton: FieldExpecter<boolean> = async (page, id, value) => {
  const checked = await page.locator(`input#${id}`).isChecked()
  expect(checked).toBe(value)
}

const expectTextEditor: FieldExpecter<FootnoteInputType[]> = async (
  page,
  id,
  value
) => {
  const input = page.locator(`[data-testid='${id}']`)
  await expect(input).toBeVisible()
  for (const footnote of value) {
    if (footnote.label != FOOTNOTE_LABELS[MetadatumType.FOOTNOTE_REFERENCE]) {
      expect(await input.innerText()).toContain(footnote.label)
    }
    expect(await input.innerText()).toContain(footnote.content)
  }
}

const expectChipsInput: FieldExpecter<string[]> = async (page, id, value) => {
  const chipValues = page
    .locator(`[data-testid='chips-input_${id}']`)
    .locator(`[data-testid='chip-value']`)

  await expect(chipValues).toHaveCount(value.length)

  for (const [index, subValue] of value.entries()) {
    await expect(chipValues.nth(index)).toHaveText(subValue)
  }
}

const expectDropdown: FieldExpecter<string> = async (page, id, value) => {
  const inputValue = await page.locator(`input#${id}`).inputValue()
  expect(inputValue).toBe(value)
}

const FIELD_EXPECTER: FieldExpectMapping = {
  [FieldType.TEXT]: expectTextInput,
  [FieldType.TEXTAREA]: expectTextArea,
  [FieldType.CHECKBOX]: expectCheckbox,
  [FieldType.RADIO]: expectRadioButton,
  [FieldType.CHIPS]: expectChipsInput,
  [FieldType.DROPDOWN]: expectDropdown,
  [FieldType.EDITOR]: expectTextEditor,
}

async function expectMetadataInputSectionToHaveCorrectData(
  page: Page,
  section: MetadataInputSection
): Promise<void> {
  if (section.isSingleFieldSection) {
    await expectInputFieldGroupHasCorrectValues(page, section.fields ?? [])
  } else if (section.isExpandableNotRepeatable) {
    await expectExpandableSectionNotRepeatableToHaveCorrectValues(page, section)
  } else {
    const heading = page.locator(`a span:text-is("${section.heading}")`)
    await expect(heading).toBeVisible()
    const legend = page.locator(
      `h2:text-is("${section.heading}"), legend:text-is("${section.heading}")`
    )
    await expect(legend.first()).toBeVisible()
    await expectInputFieldGroupHasCorrectValues(page, section.fields ?? [])

    for (const subSection of section.sections ?? []) {
      const header = page
        .locator(`legend:text-is("${subSection.heading}")`)
        .first()
      await expect(header).toBeVisible()
      await header.click()
      await expectInputFieldGroupHasCorrectValues(page, subSection.fields ?? [])
    }
  }
}

export async function expectInputFieldHasCorrectValue<
  Type extends FieldType,
  Value extends FieldValueTypeMapping[Type]
>(page: Page, type: Type, id: string, value: Value): Promise<void> {
  const expecter = FIELD_EXPECTER[type]
  return expecter(page, id, value)
}

export async function expectInputFieldGroupHasCorrectValues(
  page: Page,
  fields: AnyField[],
  valueIndex?: number
): Promise<void> {
  for (const field of fields ?? []) {
    const value =
      valueIndex !== undefined ? field.values?.[valueIndex] : field.value

    if (value !== undefined) {
      if (field.type !== FieldType.EDITOR) {
        const label = page.locator(`label:has-text("${field.label}")`).first()
        await expect(label).toBeVisible()
      }

      await expectInputFieldHasCorrectValue(page, field.type, field.id, value)
    }
  }
}

export async function expectRepeatedSectionListHasCorrectEntries(
  page: Page,
  section: MetadataInputSection,
  numberOfEntries = 1
): Promise<void> {
  const expandable = page.locator(`#${section.id}`)
  await expect(expandable).toBeVisible()
  await expect(expandable).toContainText(section.heading ?? "")

  await expandable.click()

  const numberOfSectionRepetition = section.isNotImported
    ? numberOfEntries
    : Math.max(
        ...(section.fields ?? []).map((field) => field.values?.length ?? 0)
      )
  const listEntries = expandable.getByLabel("Listen Eintrag")
  const entryCount = await listEntries.count()
  expect(entryCount).toBe(numberOfSectionRepetition)

  const fields = section.fields ?? []

  async function expectEntry(index: number): Promise<void> {
    await expectInputFieldGroupHasCorrectValues(page, fields, index)
    await page.keyboard.down("Enter") // Stop editing / close inputs again.
  }

  // Single entries are automatically in edit mode.
  if (entryCount == 1) {
    await expectEntry(0)
  } else {
    for (let index = 0; index < numberOfSectionRepetition; index++) {
      const entry = listEntries.nth(index)
      await entry.getByRole("button", { name: "Eintrag bearbeiten" }).click()
      await expectEntry(index)
    }
  }
}

export async function expectExpandableSectionNotRepeatableToHaveCorrectValues(
  page: Page,
  section: MetadataInputSection
): Promise<void> {
  const expandable = page.locator(`#${section.id}`)
  await expect(expandable).toBeVisible()
  await expect(expandable).toContainText(section.heading ?? "")

  await expandable.click()

  for (const field of section.fields ?? []) {
    if (field.values !== undefined && field.values[0] !== undefined) {
      const label = page.locator(`label:has-text("${field.label}")`).first()
      await expect(label).toBeVisible()

      await expectInputFieldHasCorrectValue(
        page,
        field.type,
        field.id,
        field.values[0]
      )
    }
  }
  const finishButton = expandable.getByRole("button", { name: "Fertig" })
  await finishButton.click()
}

export async function expectMetadataInputSectionToHaveCorrectDataOnDisplay(
  page: Page,
  section: MetadataInputSection
): Promise<void> {
  if (section.isRepeatedSection) {
    await expectRepeatedSectionListHasCorrectEntries(page, section)
  } else {
    await expectMetadataInputSectionToHaveCorrectData(page, section)
  }
}
export async function expectMetadataInputSectionToHaveCorrectDataOnEdit(
  page: Page,
  section: MetadataInputSection
): Promise<void> {
  if (section.isRepeatedSection) {
    await expectRepeatedSectionListHasCorrectEntries(
      page,
      section,
      section.numberEditedSections ?? 1
    )
  } else {
    await expectMetadataInputSectionToHaveCorrectData(page, section)
  }
}
