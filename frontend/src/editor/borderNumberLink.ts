import { Mark, markInputRule } from "@tiptap/core"

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    BorderNumberLink: {
      setBorderNumberLink: () => ReturnType
    }
  }
}
export const BorderNumberLink = Mark.create({
  name: "BorderNumberLink",
  inclusive: false,

  parseHTML() {
    return [
      {
        tag: "border-number-link",
      },
    ]
  },

  renderHTML({ mark }) {
    const styleClasses =
      mark.attrs.valid === "true"
        ? // Padding / margin so that caret is still visible, see RISUP-185
          'text-white bg-blue-700 before:content-["Rd_"] ml-1 pr-1'
        : 'text-red-900 bg-red-200 before:content-["⚠Rd_"]'
    return [
      "border-number-link",
      {
        class: `font-bold ${styleClasses}`,
        valid: mark.attrs.valid,
        nr: mark.attrs.nr,
      },
      0,
    ]
  },

  addInputRules() {
    return [
      markInputRule({
        // find numbers between #'s and allow numbers up to 99999
        find: /#([1-9]|[1-9]\d|[1-9]\d\d|[1-9]\d\d\d|[1-9]\d\d\d\d)#/,
        type: this.type,
        getAttributes(match) {
          return { nr: match[1] }
        },
      }),
    ]
  },

  addAttributes() {
    return {
      nr: {
        parseHTML: (element) => element.getAttribute("nr"),
        renderHTML: (attributes) => ({
          nr: attributes.nr,
        }),
      },
      valid: {
        parseHTML: (element) => element.getAttribute("valid"),
        renderHTML: (attributes) => ({
          valid: attributes.valid,
        }),
      },
    }
  },
})
