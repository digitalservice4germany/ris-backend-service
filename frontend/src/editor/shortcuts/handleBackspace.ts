import { Editor } from "@tiptap/core"
import { MarkType, NodeType } from "prosemirror-model"
import { EditorState } from "prosemirror-state"
import { nextTick } from "vue"
import BorderNumberService from "@/services/borderNumberService"

/**
 * Main handler for Backspace which removes borderNumber or borderNumberLink if needed.
 */
function handleBackspace(editor: Editor): boolean {
  const { state, schema } = editor
  const { selection, tr, doc } = state
  const { $from, $to } = selection

  const borderNumberNumberNodeType: NodeType = schema.nodes.borderNumberNumber
  const borderNumberNodeType: NodeType = schema.nodes.borderNumber
  const borderNumberContentType: NodeType = schema.nodes.borderNumberContent
  const borderNumberLinkType: MarkType = schema.marks.BorderNumberLink
  const selectedBorderNumbers: string[] = []

  doc.nodesBetween($from.pos, $to.pos, (node, pos, parent) => {
    if (
      node.type === borderNumberNumberNodeType ||
      (parent?.type === borderNumberNodeType && pos === 0)
    ) {
      selectedBorderNumbers.push(node.textContent)
    }
  })

  const isSelection = $from.pos !== $to.pos
  const isBorderNumberContent =
    $from.node($from.depth - 1)?.type === borderNumberContentType
  const isCursorAtStart = $from.parentOffset === 0
  const isBorderNumberNumber =
    $from.node($from.depth)?.type === borderNumberNumberNodeType

  const isFirstChild = $from.index($from.depth - 1) === 0

  const isCursorBehindBorderNumberLink = state.doc.rangeHasMark(
    $from.pos - 1,
    $from.pos,
    borderNumberLinkType,
  )
  const isCursorBehindOrInsideBorderNumberNumber =
    (!isSelection &&
      isBorderNumberContent &&
      isFirstChild &&
      isCursorAtStart) ||
    isBorderNumberNumber

  const isSelectionWithBorderNumbers =
    isSelection && selectedBorderNumbers.length > 0

  if (isCursorBehindOrInsideBorderNumberNumber) {
    return editor.commands.removeBorderNumbers()
  }

  if (isSelectionWithBorderNumbers) {
    void nextTick().then(() => {
      BorderNumberService.invalidateBorderNumberLinks(selectedBorderNumbers)
      BorderNumberService.makeBorderNumbersSequential()
    })
    return false
  }

  if (isCursorBehindBorderNumberLink) {
    const start = calculateStart($from.pos, state, borderNumberLinkType)
    const end = calculateEnd($from.pos, state, borderNumberLinkType)

    tr.delete(start, end)
    editor.view.dispatch(tr)
    return true
  }

  return false
}

function calculateStart(
  start: number,
  state: EditorState,
  borderNumberLinkType: MarkType,
) {
  while (
    start > 0 &&
    state.doc.rangeHasMark(start - 1, start, borderNumberLinkType)
  ) {
    start--
  }
  return start
}

function calculateEnd(
  end: number,
  state: EditorState,
  borderNumberLinkType: MarkType,
) {
  while (
    end < state.doc.content.size &&
    state.doc.rangeHasMark(end, end + 1, borderNumberLinkType)
  ) {
    end++
  }
  return end
}

export default handleBackspace
