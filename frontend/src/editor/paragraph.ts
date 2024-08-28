import { Paragraph } from "@tiptap/extension-paragraph"

export const CustomParagraph = Paragraph.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      class: { default: null },
      listIndexStyle: {
        default: null,
        parseHTML: (element) => element.getAttribute("style"),
        renderHTML: (attributes) => {
          if (attributes.style === null) {
            return {}
          }
          const listIndexStyle = [
            ...new Set(
              JSON.stringify(attributes.listIndexStyle)
                .replace(/"/g, "")
                .split(";")
                .map((s) => s.trim())
                .filter(
                  (s) =>
                    s.length > 0 &&
                    (s.includes("display") || s.includes("text-align")),
                ),
            ),
          ]
          const display = listIndexStyle.filter((style) =>
            style.includes("display"),
          )
          const displayStyle =
            display.length > 0 ? display[display.length - 1] + ";" : ""
          const textAlign = listIndexStyle.filter((style) =>
            style.includes("text-align"),
          )
          const textAlignStyle =
            !JSON.stringify(attributes).includes("textAlign") &&
            textAlign.length > 0
              ? textAlign[textAlign.length - 1] + ";"
              : ""
          const indexStyle = displayStyle + textAlignStyle
          if (indexStyle.length > 0) {
            return {
              style: indexStyle,
            }
          }
          return {}
        },
      },
      elementId: {
        default: null,
        parseHTML: (element) => element.getAttribute("element-id"),
        renderHTML: () => {
          return {}
        },
      },
    }
  },
})
