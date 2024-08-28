<script lang="ts" setup>
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
import { EditorContent, Editor } from "@tiptap/vue-3"
import { storeToRefs } from "pinia"
import { ref, watch } from "vue"
import TextButton from "@/components/input/TextButton.vue"
import {
  BorderNumber,
  BorderNumberContent,
  BorderNumberNumber,
} from "@/editor/borderNumber"
import { BorderNumberLink } from "@/editor/borderNumberLink"
import { CustomBulletList } from "@/editor/bulletList"
import { FontSize } from "@/editor/fontSize"
import { CustomImage } from "@/editor/image"
import { Indent } from "@/editor/indent"
import { InvisibleCharacters } from "@/editor/invisibleCharacters"
import { CustomListItem } from "@/editor/listItem"
import { CustomOrderedList } from "@/editor/orderedList"
import { CustomParagraph } from "@/editor/paragraph"
import { CustomSubscript, CustomSuperscript } from "@/editor/scriptText"
import { TableStyle } from "@/editor/tableStyle"
import attachmentService from "@/services/attachmentService"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()
const { documentUnit } = storeToRefs(store)

const html = ref("")

const editor = new Editor({
  editorProps: {
    attributes: {
      tabindex: "0",
      style:
        "height: 100%; overflow-y: auto; padding: 0.75rem 1rem; outline: 0",
    },
  },
  content: html,
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
    FontSize,
    Italic,
    CustomListItem,
    CustomBulletList,
    CustomOrderedList,
    Underline,
    Strike,
    CustomSubscript,
    CustomSuperscript,
    Table,
    TableCell,
    TableHeader,
    TableRow,
    TableStyle,
    TextStyle,
    HardBreak,
    InvisibleCharacters,
    TextAlign.configure({ types: ["paragraph", "span"] }),
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
  ],
  editable: true,
  parseOptions: {
    preserveWhitespace: "full",
  },
})

watch(
  () => html.value,
  (value: string) => {
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

const handleReconvertDocument = async () => {
  const documentationUnitId = documentUnit.value?.uuid
  const firstAttachmentS3Path =
    documentUnit.value?.attachments && documentUnit.value.attachments.length > 0
      ? documentUnit.value.attachments[0].s3path
      : undefined

  if (documentationUnitId && firstAttachmentS3Path) {
    const response = await attachmentService.reconvertDocument(
      documentationUnitId,
      firstAttachmentS3Path,
    )

    if (response.error === undefined) {
      let responseContent = ""
      response.data.forEach((element) => (responseContent += element.content))
      html.value = responseContent
    }
  }
}

const handleRemoveBorderNumbers = async () => {
  const documentationUnitId = documentUnit.value?.uuid
  const firstAttachmentS3Path =
    documentUnit.value?.attachments && documentUnit.value.attachments.length > 0
      ? documentUnit.value.attachments[0].s3path
      : undefined

  if (documentationUnitId && firstAttachmentS3Path) {
    const response = await attachmentService.removeBorderNumbers(
      documentationUnitId,
      firstAttachmentS3Path,
    )

    if (response.error === undefined) {
      let responseContent = ""
      response.data.forEach((element) => (responseContent += element.content))
      html.value = responseContent
    }
  }
}

const handleAddBorderNumbers = async () => {
  const documentationUnitId = documentUnit.value?.uuid
  const firstAttachmentS3Path =
    documentUnit.value?.attachments && documentUnit.value.attachments.length > 0
      ? documentUnit.value.attachments[0].s3path
      : undefined

  if (documentationUnitId && firstAttachmentS3Path) {
    const response = await attachmentService.addBorderNumbers(
      documentationUnitId,
      firstAttachmentS3Path,
    )

    if (response.error === undefined) {
      let responseContent = ""
      response.data.forEach((element) => (responseContent += element.content))
      html.value = responseContent
    }
  }
}

const handleAddBorderNumbersWithStart = async () => {
  const elementId = getElementId()

  const documentationUnitId = documentUnit.value?.uuid
  const firstAttachmentS3Path =
    documentUnit.value?.attachments && documentUnit.value.attachments.length > 0
      ? documentUnit.value.attachments[0].s3path
      : undefined

  if (documentationUnitId && firstAttachmentS3Path) {
    const response = await attachmentService.addBorderNumbers(
      documentationUnitId,
      firstAttachmentS3Path,
      elementId,
    )

    if (response.error === undefined) {
      let responseContent = ""
      response.data.forEach((element) => (responseContent += element.content))
      html.value = responseContent
    }
  }
}

const handleRemoveSingleBorderNumber = async () => {
  const elementId = getElementId()

  const documentationUnitId = documentUnit.value?.uuid
  const firstAttachmentS3Path =
    documentUnit.value?.attachments && documentUnit.value.attachments.length > 0
      ? documentUnit.value.attachments[0].s3path
      : undefined

  if (documentationUnitId && firstAttachmentS3Path) {
    const response = await attachmentService.removeSingleBorderNumber(
      documentationUnitId,
      firstAttachmentS3Path,
      elementId,
    )

    if (response.error === undefined) {
      let responseContent = ""
      response.data.forEach((element) => (responseContent += element.content))
      html.value = responseContent
    }
  }
}

const handleJoinBorderNumberWithAbove = async () => {
  const elementId = getElementId()

  const documentationUnitId = documentUnit.value?.uuid
  const firstAttachmentS3Path =
    documentUnit.value?.attachments && documentUnit.value.attachments.length > 0
      ? documentUnit.value.attachments[0].s3path
      : undefined

  if (documentationUnitId && firstAttachmentS3Path) {
    const response = await attachmentService.joinBorderNumbers(
      documentationUnitId,
      firstAttachmentS3Path,
      elementId,
    )

    if (response.error === undefined) {
      let responseContent = ""
      response.data.forEach((element) => (responseContent += element.content))
      html.value = responseContent
    }
  }
}

function getElementId() {
  const cursorPos = editor.state.selection.$anchor.pos
  let nodePos = undefined

  nodePos = editor.$pos(cursorPos)
  while (!nodePos?.node.attrs["elementId"]) {
    nodePos = nodePos?.parent
    console.log("new nodePos", nodePos)
  }

  const elementId = nodePos?.node.attrs["elementId"]
  console.log(nodePos?.node, elementId)

  return elementId
}

const getConvertedElementList = async () => {
  const documentationUnitId = documentUnit.value?.uuid
  const firstAttachmentS3Path =
    documentUnit.value?.attachments && documentUnit.value.attachments.length > 0
      ? documentUnit.value.attachments[0].s3path
      : undefined

  if (documentationUnitId && firstAttachmentS3Path) {
    const response = await attachmentService.getConvertedElementList(
      documentationUnitId,
      firstAttachmentS3Path,
    )

    if (response.error === undefined) {
      let responseContent = ""
      response.data.forEach((element) => {
        responseContent += element.content
      })
      html.value = responseContent
    }
  }
}
getConvertedElementList()
</script>

<template>
  <div id="converted-document">
    <div class="top-60 z-4 sticky">
      <TextButton label="reconvert document" @click="handleReconvertDocument" />
      <TextButton
        label="remove border numbers"
        @click="handleRemoveBorderNumbers"
      />
      <TextButton label="add border numbers" @click="handleAddBorderNumbers" />
      <TextButton
        label="add border numbers starts at cursor"
        @click="handleAddBorderNumbersWithStart"
      />
      <TextButton
        label="remove single border number"
        @click="handleRemoveSingleBorderNumber"
      />
      <TextButton
        label="join border number with above"
        @click="handleJoinBorderNumberWithAbove"
      />
    </div>
    <div class="top-200">
      <EditorContent
        class="h-full"
        data-testid="converted-document-elements"
        :editor="editor"
      />
    </div>
  </div>
</template>
