import { Editor } from "@tiptap/core"
import { NodeType } from "prosemirror-model"
import { nextTick } from "vue"
import BorderNumberService from "@/services/borderNumberService"

/**
 * Main handler for Delete which triggers recalculation of border numbers if needed.
 */
export function handleDelete(editor: Editor): boolean {
  const { schema, selection } = editor.state
  const { from, to } = selection
  const borderNumberNumberNodeType: NodeType = schema.nodes.borderNumberNumber
  const borderNumberNodeType: NodeType = schema.nodes.borderNumber

  const selectedBorderNumbers: string[] = []

  editor.state.doc.nodesBetween(from, to, (node, pos, parent) => {
    if (
      node.type === borderNumberNumberNodeType ||
      (parent?.type === borderNumberNodeType && pos === 0)
    ) {
      selectedBorderNumbers.push(node.textContent)
    }
  })

  const isSelection = from !== to
  const isSelectionWithBorderNumbers =
    isSelection && selectedBorderNumbers.length > 0

  if (isSelectionWithBorderNumbers) {
    void nextTick().then(() => {
      BorderNumberService.invalidateBorderNumberLinks(selectedBorderNumbers)
      BorderNumberService.makeBorderNumbersSequential()
    })
  }
  return false
}
