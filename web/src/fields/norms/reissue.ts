import { InputField, InputType } from "@/domain"

export const reissue: InputField[] = [
  {
    name: "reissueNote",
    type: InputType.TEXT,
    label: "Neufassungshinweis",
    inputAttributes: {
      ariaLabel: "Neufassungshinweis",
    },
  },
  {
    name: "reissueArticle",
    type: InputType.TEXT,
    label: "Bezeichnung der Bekanntmachung",
    inputAttributes: {
      ariaLabel: "Bezeichnung der Bekanntmachung",
    },
  },
  {
    name: "Bezeichnung der Bekanntmachung",
    type: InputType.DATE,
    label: "Datum der Bekanntmachung",
    inputAttributes: {
      ariaLabel: "Datum der Bekanntmachung",
    },
  },
  {
    name: "reissueReference",
    type: InputType.TEXT,
    label: "Fundstelle der Bekanntmachung",
    inputAttributes: {
      ariaLabel: "Fundstelle der Bekanntmachung",
    },
  },
]
