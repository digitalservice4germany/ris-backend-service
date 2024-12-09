import { CommandProps } from "@tiptap/core"
import { Node as ProseMirrorNode, NodeType } from "prosemirror-model"
import { Transaction, Selection } from "prosemirror-state"
import { nextTick } from "vue"
import BorderNumberService from "@/services/borderNumberService"

/**
 * Main command handler to remove borderNumber nodes.
 */
function removeBorderNumbers({ state, dispatch }: CommandProps): boolean {
  const { selection, doc, tr, schema } = state
  const { from: initialFrom, to: initialTo } = selection
  const borderNumberNodeType: NodeType = schema.nodes.borderNumber

  const borderNumberPositions = findBorderNumberPositions(
    doc,
    initialFrom,
    initialTo,
    borderNumberNodeType,
  )

  const { modified, firstBorderNumberSize, removedBorderNumbers } =
    processBorderNumbers(tr, doc, borderNumberPositions)

  void nextTick().then(() => {
    if (removedBorderNumbers.length > 0) {
      BorderNumberService.invalidateBorderNumberLinks(removedBorderNumbers)
    }
    BorderNumberService.makeBorderNumbersSequential()
  })

  if (modified && dispatch) {
    const updatedFrom = initialFrom - firstBorderNumberSize - 2
    const selection = Selection.near(tr.doc.resolve(Math.max(1, updatedFrom)))
    tr.setSelection(selection)
    dispatch(tr)
    return true
  }

  return false
}

/**
 * Finds all the positions of `borderNumber` nodes between the selection range.
 */
function findBorderNumberPositions(
  doc: ProseMirrorNode,
  initialFrom: number,
  initialTo: number,
  borderNumberType: NodeType,
): number[] {
  const borderNumberPositions: number[] = []

  doc.nodesBetween(initialFrom, initialTo, (node, pos) => {
    if (node.type === borderNumberType) {
      borderNumberPositions.push(pos)
    }
  })

  return borderNumberPositions
}

/**
 * Processes each `borderNumber` node: either removes it if it's empty or replaces it with its content.
 */
function processBorderNumbers(
  tr: Transaction,
  doc: ProseMirrorNode,
  borderNumberPositions: number[],
): {
  modified: boolean
  firstBorderNumberSize: number
  removedBorderNumbers: string[]
} {
  let modified = false
  let firstBorderNumberSize: number = 0
  const removedBorderNumbers: string[] = []

  // Traverse in reverse order to avoid shifting positions
  borderNumberPositions.toReversed().forEach((pos) => {
    const borderNumberNode = doc.nodeAt(pos)

    if (borderNumberNode) {
      const contentNode = borderNumberNode.child(1)

      if (isNodeEmpty(contentNode)) {
        tr.delete(pos, pos + borderNumberNode.nodeSize)
      } else {
        tr.replaceWith(
          pos,
          pos + borderNumberNode.nodeSize,
          contentNode.content,
        )
      }

      modified = true

      firstBorderNumberSize = borderNumberNode.nodeSize - contentNode.nodeSize

      const borderNumberNumberNode = borderNumberNode.child(0)
      removedBorderNumbers.push(borderNumberNumberNode.textContent)
    }
  })

  return { modified, firstBorderNumberSize, removedBorderNumbers }
}

/**
 * Utility function to check if a node is empty.
 */
function isNodeEmpty(node: ProseMirrorNode): boolean {
  return node.childCount === 0
}

export default removeBorderNumbers
