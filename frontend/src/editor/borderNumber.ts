import "../styles/border-numbers.css"
import { commands } from "@guardian/prosemirror-invisibles"
import { CommandProps } from "@tiptap/core"
import { Node } from "@tiptap/vue-3"
import addBorderNumbers from "@/editor/commands/addBorderNumbers"
import { handleSelection } from "@/editor/commands/handleSelection"
import removeBorderNumbers from "@/editor/commands/removeBorderNumbers"
import handleBackspace from "@/editor/shortcuts/handleBackspace"
import { handleDelete } from "@/editor/shortcuts/handleDelete"

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    removeBorderNumbers: {
      removeBorderNumbers: () => ReturnType
    }
    addBorderNumbers: {
      addBorderNumbers: () => ReturnType
    }
    handleSelection: {
      handleSelection: () => ReturnType
    }
    paragraph: {
      getParagraph: () => ReturnType
    }
    borderNumber: {
      setBorderNumber: () => ReturnType
    }
    borderNumberNumber: {
      setBorderNumberNumber: () => ReturnType
    }
    borderNumberContent: {
      setBorderNumberContent: () => ReturnType
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
    return ["border-number", {}, 0]
  },
  addCommands() {
    return {
      removeBorderNumbers: () => (commandProps: CommandProps) => {
        return removeBorderNumbers(commandProps)
      },
      addBorderNumbers: () => addBorderNumbers,
      handleSelection: () => handleSelection,
    }
  },
  addKeyboardShortcuts() {
    return {
      Backspace: ({ editor }) => handleBackspace(editor),
      Delete: ({ editor }) => handleDelete(editor),
      "Mod-Alt-.": ({ editor }) => editor.commands.addBorderNumbers(),
      "Mod-Alt--": ({ editor }) => editor.commands.removeBorderNumbers(),
      "Mod-Alt-#": ({ editor }) =>
        commands.toggleActiveState()(editor.state, editor.view.dispatch),
      // ‘ is the keycode for Alt+# on Macbook
      "Mod-Alt-‘": ({ editor }) =>
        commands.toggleActiveState()(editor.state, editor.view.dispatch),
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
    return ["number", {}, 0]
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
