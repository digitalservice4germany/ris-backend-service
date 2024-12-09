import { Extension } from "@tiptap/core"
import "../styles/tables.css"

export const TableStyle = Extension.create({
  addGlobalAttributes() {
    return [
      {
        types: ["table", "tableCell", "tableHeader"],
        attributes: {
          style: {
            renderHTML: (attributes) => {
              const existingStyle = attributes.style || ""

              // Split the style string into individual declarations
              const styles: string[] = existingStyle.split(";")

              const hasBorderStyle = styles.some((style) => {
                const trimmedStyle = style.trim()
                return (
                  trimmedStyle.startsWith("border") &&
                  !trimmedStyle.includes("none")
                )
              })

              const invisibleTableClass = hasBorderStyle
                ? ""
                : "invisible-table"

              return {
                class: invisibleTableClass,
                style: existingStyle,
              }
            },
          },
        },
      },
      {
        types: ["tableRow"],
        attributes: {
          style: {
            renderHTML: (attributes) => {
              return {
                style: attributes.style,
              }
            },
          },
        },
      },
    ]
  },
})
