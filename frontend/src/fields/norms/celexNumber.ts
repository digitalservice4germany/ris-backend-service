import { InputField, InputType } from "@/domain"

export const celexNumber: InputField[] = [
  {
    name: "celexNumber",
    type: InputType.TEXT,
    label: "Celex Nummer",
    inputAttributes: {
      ariaLabel: "Celex Nummer",
    },
  },
]
