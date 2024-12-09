<script lang="ts" setup>
import dayjs from "dayjs"
import Tooltip from "./Tooltip.vue"
import CellHeaderItem from "@/components/CellHeaderItem.vue"
import CellItem from "@/components/CellItem.vue"
import TableHeader from "@/components/TableHeader.vue"
import TableRow from "@/components/TableRow.vue"
import TableView from "@/components/TableView.vue"
import Attachment from "@/domain/attachment"
import IconDelete from "~icons/ic/baseline-close"

const props = defineProps<{
  files?: Attachment[]
}>()

const emit = defineEmits<{
  (e: "delete", id: number): void
  (e: "select", id: number): void
}>()

/**
 * Propagates delete event to parent and closes modal again
 */
const onDelete = (index: number) => {
  emit("delete", index)
}

const onSelect = (index: number) => {
  emit("select", index)
}
</script>

<template>
  <TableView
    id="attachment-list"
    class="relative table w-full border-separate"
    data-testid="attachment-list"
  >
    <TableHeader>
      <CellHeaderItem> Dateiname</CellHeaderItem>
      <CellHeaderItem> Format</CellHeaderItem>
      <CellHeaderItem> Hochgeladen am</CellHeaderItem>
      <CellHeaderItem></CellHeaderItem>
    </TableHeader>
    <TableRow
      v-for="(file, index) in props.files"
      :key="index"
      class="cursor-pointer"
      :data-testid="`list-entry-${index}`"
    >
      <CellItem @click="onSelect(index)">
        {{ file.name ?? "-" }}
      </CellItem>
      <CellItem @click="onSelect(index)">
        {{ file.format ?? "-" }}
      </CellItem>

      <CellItem data-testid="uploaded-at-cell" @click="onSelect(index)">
        {{
          file.uploadTimestamp
            ? dayjs(file.uploadTimestamp).format("DD.MM.YYYY")
            : "-"
        }}
      </CellItem>
      <CellItem class="text-end">
        <Tooltip text="Löschen">
          <button
            aria-label="Datei löschen"
            class="cursor-pointer align-middle text-blue-800 focus:outline-none focus-visible:outline-blue-800"
            @click="onDelete(index)"
            @keyup.enter="null"
          >
            <IconDelete />
          </button>
        </Tooltip>
      </CellItem>
    </TableRow>
  </TableView>
</template>
