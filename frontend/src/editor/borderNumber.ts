import { CommandProps } from "@tiptap/core"
import { Node } from "@tiptap/vue-3"
import { TextSelection } from "prosemirror-state"

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    removeBorderNumbers: {
      removeBorderNumbers: () => ReturnType
    }
  }
}

export const BorderNumber = Node.create({
  name: "borderNumber",
  priority: 1000,
  group: "block",
  content: "borderNumberNumber borderNumberContent",
  parseHTML() {
    return [{ tag: "border-number" }]
  },
  renderHTML() {
    return ["border-number", { style: "display: flex; margin-bottom: 10px" }, 0]
  },
  addCommands() {
    return {
      removeBorderNumbers:
        () =>
        ({ state, dispatch }: CommandProps) => {
          const { selection, doc, tr } = state
          const { from, to } = selection

          const borderNumberPositions: number[] = []
          const borderNumberSizes: number[] = []
          let modified = false
          let isLastNodeEmpty = false

          // Find positions of all borderNumber nodes within the selection
          doc.nodesBetween(from, to, (node, pos) => {
            if (node.type.name === "borderNumber") {
              borderNumberPositions.push(pos)
            }
            isLastNodeEmpty = node.isTextblock && node.content.size === 0
          })

          // Traverse in reverse to avoid shifting positions
          borderNumberPositions.toReversed().forEach((pos) => {
            const borderNumberNode = doc.nodeAt(pos)

            if (borderNumberNode) {
              const contentNode = borderNumberNode.child(1)
              borderNumberSizes.push(
                borderNumberNode.nodeSize - contentNode.nodeSize,
              )

              if (contentNode.textContent.length === 0) {
                tr.delete(pos, pos + borderNumberNode.nodeSize)
              } else {
                tr.replaceWith(
                  pos,
                  pos + borderNumberNode.nodeSize,
                  contentNode,
                )
              }

              modified = true
            }
          })

          if (modified && dispatch) {
            const borderNumberSizeSum = borderNumberSizes.reduce(
              (acc, size) => acc + size,
              0,
            )
            const borderNumberCount = borderNumberPositions.length

            // Adjust the positions of the selection
            const fromPos =
              from - borderNumberSizes[borderNumberSizes.length - 1]
            const toPos = to - borderNumberSizeSum - 2 * (borderNumberCount - 1)

            tr.setSelection(
              TextSelection.create(
                tr.doc,
                fromPos < 0 ? 0 : fromPos,
                // We need this rule to avoid a RangeError
                isLastNodeEmpty ? toPos - 1 : toPos,
              ),
            )
            dispatch(tr)
            return true
          }
          return false
        },
    }
  },
  addKeyboardShortcuts() {
    return {
      Backspace: ({ editor }) => {
        const { state } = editor
        const { selection } = state
        const { $from, $to } = selection

        const isCollapsedSelection = $from.pos === $to.pos
        const isBorderNumberContent =
          $from.node($from.depth - 1).type.name === "borderNumberContent"
        const isCursorAtStart = $from.parentOffset === 0
        const isBorderNumberNumber =
          $from.node($from.depth).type.name === "borderNumberNumber"

        // Check if the current paragraph is the first child of the borderNumberContent node
        const isFirstChild = $from.index($from.depth - 1) === 0

        if (
          (isCollapsedSelection &&
            isBorderNumberContent &&
            isFirstChild &&
            isCursorAtStart) ||
          isBorderNumberNumber
        ) {
          return editor.commands.removeBorderNumbers()
        }

        return false
      },
    }
  },
  addAttributes() {
    return {
      ...this.parent?.(),
      class: { default: null },
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

export const BorderNumberNumber = Node.create({
  name: "borderNumberNumber",
  priority: 1000,
  group: "border",
  content: "inline*",
  parseHTML() {
    return [{ tag: "number" }]
  },
  renderHTML() {
    return [
      "number",
      { style: "padding-left: 10px; min-width: 40px; editable: false" },
      0,
    ]
  },
})

export const BorderNumberContent = Node.create({
  name: "borderNumberContent",
  priority: 1000,
  group: "border",
  content: "block*",
  parseHTML() {
    return [{ tag: "content" }]
  },
  renderHTML() {
    return ["content", 0]
  },
})
