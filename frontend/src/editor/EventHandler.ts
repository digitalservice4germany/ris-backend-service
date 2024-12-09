import { Extension } from "@tiptap/core"
import { ResolvedPos, Slice } from "@tiptap/pm/model"
import { EditorState, Plugin, PluginKey, Selection } from "@tiptap/pm/state"
import { EditorView } from "@tiptap/pm/view"
import { nextTick } from "vue"
import BorderNumberService from "@/services/borderNumberService"

function hasBorderNumbersInInsertionData(data: DataTransfer | null) {
  const pastedHTML = data?.getData("text/html")

  if (pastedHTML) {
    const doc = new DOMParser().parseFromString(pastedHTML, "text/html")

    const borderNumberElements = doc.querySelectorAll("border-number")
    return borderNumberElements.length > 0
  } else {
    return false
  }
}

function isTopLevelEmptyParagraph(state: EditorState, $to: ResolvedPos) {
  const paragraphNodeType = state.schema.nodes.paragraph

  const node = $to.node(1)
  const isEmptyParagraph =
    node.type === paragraphNodeType &&
    node.childCount === 0 &&
    node.textContent?.trim() === ""
  return isEmptyParagraph && $to.depth === 1
}

function pasteOnTopLevel(view: EditorView, slice: Slice) {
  const { state, dispatch } = view
  const { selection, tr } = state
  const { $to } = selection

  void nextTick().then(() => BorderNumberService.makeBorderNumbersSequential())

  if (isTopLevelEmptyParagraph(state, $to)) {
    // Continue with normal copy-paste, e.g., if completely empty editor field.
    return false
  }

  // The depth of 1 represents the top-level nodes directly under the root.
  // Example: If $from points to an <li> in an <ol>, we will get the end position of the <ol> element
  const endPositionOfTopLevelNode = $to.end(1)
  const textSelection = Selection.near(
    tr.doc.resolve(endPositionOfTopLevelNode),
  )
  tr.setSelection(textSelection)
    .scrollIntoView()
    .insert(endPositionOfTopLevelNode, slice.content)

  dispatch(tr)
  return true
}

export const EventHandler = Extension.create({
  name: "eventHandler",

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: new PluginKey("eventHandler"),
        props: {
          handlePaste: (view, event, slice) => {
            if (!hasBorderNumbersInInsertionData(event.clipboardData)) {
              // If there are no border numbers in the clipboard, we do not overwrite the default behavior
              return false
            }

            return pasteOnTopLevel(view, slice)
          },

          handleDrop: (view, event, slice) => {
            if (!hasBorderNumbersInInsertionData(event.dataTransfer)) {
              // If there are no border numbers in the dragged content, we do not overwrite the default behavior
              return false
            }

            return pasteOnTopLevel(view, slice)
          },
        },
      }),
    ]
  },
})
