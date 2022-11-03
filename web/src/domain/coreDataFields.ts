import { InputType, LookupTableEndpoint, ValidationError } from "./types"
import type { InputField, DropdownItem } from "./types"
import legalEffectTypes from "@/data/legalEffectTypes.json"

export function defineTextField(
  name: string,
  label: string,
  ariaLabel: string,
  required?: boolean,
  placeholder?: string,
  validationError?: ValidationError,
  readOnly?: boolean
): InputField {
  return {
    name,
    type: InputType.TEXT,
    label,
    required,
    inputAttributes: { ariaLabel, placeholder, validationError, readOnly },
  }
}

export function defineDateField(
  name: string,
  label: string,
  ariaLabel: string,
  required?: boolean,
  validationError?: ValidationError
): InputField {
  return {
    name,
    type: InputType.DATE,
    label,
    required,
    inputAttributes: { ariaLabel, validationError },
  }
}

export function defineDropdownField(
  name: string,
  label: string,
  ariaLabel: string,
  required?: boolean,
  placeholder?: string,
  isCombobox?: boolean,
  dropdownItems?: DropdownItem[] | LookupTableEndpoint,
  preselectedValue?: string,
  validationError?: ValidationError
): InputField {
  return {
    name,
    type: InputType.DROPDOWN,
    label,
    required,
    inputAttributes: {
      ariaLabel,
      placeholder,
      dropdownItems,
      isCombobox,
      preselectedValue,
      validationError,
    },
  }
}

export const coreDataFields: InputField[] = [
  defineTextField("courtType", "Gerichtstyp", "Gerichtstyp", true),
  defineTextField("courtLocation", "Gerichtssitz", "Gerichtssitz"),
  defineTextField("fileNumber", "Aktenzeichen", "Aktenzeichen", true),
  defineDateField(
    "decisionDate",
    "Entscheidungsdatum",
    "Entscheidungsdatum",
    true,
    undefined
  ),
  defineTextField("appraisalBody", "Spruchkörper", "Spruchkörper"),
  defineDropdownField(
    "category",
    "Dokumenttyp",
    "Dokumenttyp",
    true,
    "Bitte auswählen",
    true,
    LookupTableEndpoint.documentTypes
  ),
  defineTextField("ecli", "ECLI", "ECLI"),
  defineTextField("procedure", "Vorgang", "Vorgang"),
  defineDropdownField(
    "legalEffect",
    "Rechtskraft",
    "Rechtskraft",
    true,
    "",
    false,
    legalEffectTypes.items,
    legalEffectTypes.items[0].value
  ),
]

export const prefilledDataFields: InputField[] = [
  defineTextField(
    "center",
    "Dokumentationsstelle",
    "Dokumentationsstelle",
    false,
    "",
    { defaultMessage: "", field: "" },
    true
  ),
  defineTextField(
    "region",
    "Region",
    "Region",
    false,
    "",
    { defaultMessage: "", field: "" },
    true
  ),
  defineTextField(
    "type",
    "Dokumentart",
    "Dokumentart",
    false,
    "",
    { defaultMessage: "", field: "" },
    true
  ),
  defineTextField(
    "judicature",
    "Gerichtbarkeit",
    "Gerichtbarkeit",
    false,
    "",
    { defaultMessage: "", field: "" },
    true
  ),
]

export const moreCategories: InputField[] = [
  defineDropdownField(
    "moreCategories",
    "Weitere Rubrik",
    "Weitere Rubrik",
    false,
    "Bitte auswählen",
    false,
    []
  ),
]
