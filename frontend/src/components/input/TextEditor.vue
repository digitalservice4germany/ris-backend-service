<script lang="ts" setup>
import { commands } from "@guardian/prosemirror-invisibles"
import { Blockquote } from "@tiptap/extension-blockquote"
import { Bold } from "@tiptap/extension-bold"
import { Color } from "@tiptap/extension-color"
import { Document } from "@tiptap/extension-document"
import { HardBreak } from "@tiptap/extension-hard-break"
import { History } from "@tiptap/extension-history"
import { Italic } from "@tiptap/extension-italic"
import { Strike } from "@tiptap/extension-strike"
import { Table } from "@tiptap/extension-table"
import { TableCell } from "@tiptap/extension-table-cell"
import { TableHeader } from "@tiptap/extension-table-header"
import { TableRow } from "@tiptap/extension-table-row"
import { Text } from "@tiptap/extension-text"
import { TextAlign } from "@tiptap/extension-text-align"
import { TextStyle } from "@tiptap/extension-text-style"
import { Underline } from "@tiptap/extension-underline"
import { BubbleMenu, Editor, EditorContent } from "@tiptap/vue-3"
import { computed, onMounted, ref, watch } from "vue"
import TextEditorMenu from "@/components/input/TextEditorMenu.vue"
import { TextAreaInputAttributes } from "@/components/input/types"
import TextCheckModal from "@/components/text-check/TextCheckModal.vue"
import {
  BorderNumber,
  borderNumberCategories,
  BorderNumberContent,
  BorderNumberNumber,
} from "@/editor/borderNumber"
import { BorderNumberLink } from "@/editor/borderNumberLink"
import { CustomBulletList } from "@/editor/bulletList"
import { EventHandler } from "@/editor/EventHandler"
import { FontSize } from "@/editor/fontSize"
import { CustomImage } from "@/editor/image"
import { Indent } from "@/editor/indent"
import { InvisibleCharacters } from "@/editor/invisibleCharacters"
import { LanguageToolExtension } from "@/editor/languagetool/languageToolExtension"
import { CustomListItem } from "@/editor/listItem"
import { CustomOrderedList } from "@/editor/orderedList"
import { CustomParagraph } from "@/editor/paragraph"
import { CustomSubscript, CustomSuperscript } from "@/editor/scriptText"
import { TableStyle } from "@/editor/tableStyle"
import { LanguageToolHelpingWords, Match } from "@/types/languagetool"

import "@/styles/language-tool.scss"

interface Props {
  value?: string
  editable?: boolean
  preview?: boolean
  ariaLabel?: string
  /* If true, the color formatting of border numbers is disabled */
  plainBorderNumbers?: boolean
  fieldSize?: TextAreaInputAttributes["fieldSize"]
  textCheck?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  value: undefined,
  editable: false,
  preview: false,
  plainBorderNumbers: false,
  ariaLabel: "Editor Feld",
  fieldSize: "medium",
  textCheck: false,
})

const emit = defineEmits<{
  updateValue: [newValue: string]
}>()
const loading = ref(false)

const editorElement = ref<HTMLElement>()
const hasFocus = ref(false)
const isHovered = ref(false)

const editor: Editor = new Editor({
  editorProps: {
    attributes: {
      tabindex: "0",
      style: props.preview
        ? "height: 100%; overflow-y: auto; outline: 0"
        : "height: 100%; overflow-y: auto; padding: 0.75rem 1rem; outline: 0",
    },
  },
  content: props.value,
  extensions: [
    Document,
    CustomParagraph,
    Text,
    BorderNumber,
    BorderNumberNumber,
    BorderNumberContent,
    BorderNumberLink,
    Bold,
    Color,
    EventHandler,
    FontSize,
    Italic,
    CustomListItem,
    CustomBulletList,
    CustomOrderedList,
    Underline,
    Strike,
    CustomSubscript,
    CustomSuperscript,
    Table.configure({
      resizable: true,
      allowTableNodeSelection: true,
    }),
    TableCell,
    TableHeader,
    TableRow,
    TableStyle,
    TextStyle,
    HardBreak,
    InvisibleCharacters,
    TextAlign.configure({
      types: ["paragraph", "span"],
      alignments: props.editable
        ? ["left", "right", "center"]
        : ["left", "right", "center", "justify"],
    }),
    CustomImage.configure({
      allowBase64: true,
      inline: true,
      HTMLAttributes: {
        class: "inline align-baseline",
      },
    }),
    History.configure({
      depth: 100,
    }),
    Blockquote,
    Indent.configure({
      names: ["listItem", "paragraph"],
    }),
    LanguageToolExtension.configure({
      automaticMode: false,
      documentId: "1",
      textToolEnabled: props.textCheck,
    }),
  ],
  onUpdate: () => {
    emit("updateValue", editor.getHTML())
    setTimeout(() => updateMatch(editor))
  },
  onFocus: () => (hasFocus.value = true),
  editable: props.editable,
  parseOptions: {
    preserveWhitespace: "full",
  },
  onSelectionUpdate: () => {
    editor.commands.handleSelection()
    setTimeout(() => updateMatch(editor))
  },
  onTransaction({ transaction: tr }) {
    loading.value = !!tr.getMeta(
      LanguageToolHelpingWords.LoadingTransactionName,
    )
  },
})

const containerWidth = ref<number>()

const match = ref<Match>()

const editorExpanded = ref(false)
const editorStyleClasses = computed(() => {
  const plainBorderNumberStyle = props.plainBorderNumbers
    ? "plain-border-number"
    : ""

  const withBorderNumbers = borderNumberCategories.includes(props.ariaLabel)
    ? "with-border-numbers"
    : ""

  const fieldSizeClasses = {
    max: "h-full",
    big: "h-320",
    medium: "h-160",
    small: "h-96",
  } as const
  const size = editorExpanded.value
    ? "h-640"
    : (fieldSizeClasses[props.fieldSize] ?? "")

  return `${size} ${plainBorderNumberStyle} ${withBorderNumbers} p-4`
})

const buttonsDisabled = computed(
  () => !(props.editable && (hasFocus.value || isHovered.value)),
)

/**
 * A function to determine rather a match menu should be shownen
 */
const shouldShowBubbleMenu = (): boolean => {
  if (editor == undefined) return false

  if (
    editor.storage.languagetool == undefined ||
    editor.storage.languagetool.languageToolService == undefined
  )
    return false
  const match = editor.storage.languagetool.languageToolService.match
  const matchRange = editor.storage.languagetool.languageToolService.matchRange

  const { from, to } = editor.state.selection

  return (
    !!match && !!matchRange && matchRange.from <= from && to <= matchRange.to
  )
}

const matchRange = ref<{ from: number; to: number }>()

// const loading = ref(false)

const updateMatch = (editor: Editor) => {
  match.value = editor.storage.languagetool.languageToolService.match
  matchRange.value = editor.storage.languagetool.languageToolService.matchRange
}

// const updateHtml = () => navigator.clipboard.writeText(editor.getHTML())

const acceptSuggestion = (suggestion: string) => {
  if (matchRange.value != undefined) {
    editor.commands.insertContentAt(matchRange.value, suggestion)
    editor.storage.languagetool.languageToolService.resetLanguageToolMatch()
  }
}

const ignoreSuggestion = () => editor.commands.ignoreLanguageToolSuggestion()

watch(
  () => hasFocus.value,
  () => {
    // When the TextEditor is editable and has focus, the invisibleCharacters should be visible
    commands.setActiveState(props.editable && hasFocus.value)(
      editor.state,
      editor.view.dispatch,
    )
  },
  { immediate: true },
)

const ariaLabel = props.ariaLabel ? props.ariaLabel : null

watch(
  () => props.value,
  (value) => {
    if (!value || value === editor.getHTML()) {
      return
    }
    // incoming changes
    // the cursor should not jump to the end of the content but stay where it is
    const cursorPos = editor.state.selection.anchor
    editor.commands.setContent(value, false)
    editor.commands.setTextSelection(cursorPos)
  },
)

watch(
  () => props.textCheck,
  (newValue) => {
    editor.commands.toggleTextCheckActiveState(newValue)
  },
)

onMounted(async () => {
  const editorContainer = document.querySelector(".editor")
  if (editorContainer != null) resizeObserver.observe(editorContainer)
})

const resizeObserver = new ResizeObserver((entries) => {
  for (const entry of entries) {
    containerWidth.value = entry.contentRect.width
  }
})

/**
 * Set the selected text of match in focus
 * @param selectedMatch
 */
function jumpToMatch(selectedMatch: Match) {
  editor
    .chain()
    .focus()
    .setTextSelection({
      from: selectedMatch.offset,
      to: selectedMatch.offset + selectedMatch.length,
    })
    .run()

  editor.commands.setTextSelection({
    from: selectedMatch.offset,
    to: selectedMatch.offset + selectedMatch.length,
  })
}

/**
 * trigger text check when text editor has focus
 */
watch(hasFocus, () => {
  if (hasFocus.value) {
    editor.commands.proofread()
  }
})

defineExpose({ jumpToMatch })
</script>

<template>
  <!-- eslint-disable vuejs-accessibility/no-static-element-interactions -->
  <div
    id="text-editor"
    ref="editorElement"
    class="editor bg-white"
    fluid
    @blur="hasFocus = false"
    @focusin="hasFocus = true"
    @focusout="!editorElement?.matches(':focus-within') && (hasFocus = false)"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
  >
    <TextEditorMenu
      v-if="editable"
      :aria-label="props.ariaLabel"
      :buttons-disabled="buttonsDisabled"
      :container-width="containerWidth"
      :editor="editor"
      :editor-expanded="editorExpanded"
      @on-editor-expanded-changed="
        (isExpanded) => (editorExpanded = isExpanded)
      "
    />
    <hr v-if="editable" class="ml-8 mr-8 border-blue-300" />
    <div>
      <EditorContent
        :class="editorStyleClasses"
        :data-testid="ariaLabel"
        :editor="editor"
      />
    </div>

    <div v-if="props.textCheck">
      <BubbleMenu
        v-if="editor"
        class="bubble-menu"
        :editor="editor"
        :should-show="shouldShowBubbleMenu"
        :tippy-options="{ placement: 'bottom', animation: 'fade' }"
      >
        <TextCheckModal
          v-if="match"
          :match="match"
          @suggestion:ignore="ignoreSuggestion"
          @suggestion:update="acceptSuggestion"
        />
      </BubbleMenu>
    </div>
  </div>
</template>
