export enum FieldType {
  TEXT,
  CHECKBOX,
  RADIO,
  CHIPS,
  DROPDOWN,
}

export type FieldValue = string | boolean | string[]
// FIXME: How to relate these two types to each other?
export type FieldValueTypeMapping = {
  [FieldType.TEXT]: string
  [FieldType.CHECKBOX]: boolean
  [FieldType.RADIO]: boolean
  [FieldType.CHIPS]: string[]
  [FieldType.DROPDOWN]: string
}

// FIXME: resolve awkward mixture with value and values.
export type Field<
  Type extends FieldType,
  Value extends FieldValueTypeMapping[Type]
> = {
  type: Type
  id: string
  label: string
} & (
  | { value: Value; values?: never }
  | { value?: never; values?: (Value | undefined)[] }
)

export type AnyField = Field<FieldType, FieldValue>

// Resolve that mess...
export type MetadataInputSection = {
  id?: string
  heading?: string
  isRepeatedSection?: boolean
  isSingleFieldSection?: boolean
  isExpandableNotRepeatable?: boolean
  fields?: AnyField[]
  sections?: MetadataInputSection[]
}
