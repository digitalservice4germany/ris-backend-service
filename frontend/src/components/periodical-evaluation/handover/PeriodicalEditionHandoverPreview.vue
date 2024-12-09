<script lang="ts" setup>
import CodeSnippet from "@/components/CodeSnippet.vue"
import ExpandableContent from "@/components/ExpandableContent.vue"
import InfoModal from "@/components/InfoModal.vue"
import { Preview } from "@/domain/eventRecord"
import { ResponseError } from "@/services/httpClient"

defineProps<{
  preview?: Preview[]
  previewError?: ResponseError
}>()
</script>

<template>
  <div>
    <!-- Preview -->
    <ExpandableContent
      v-if="preview && preview?.length > 0"
      aria-label="XML Vorschau"
      as-column
      class="border-b-1 border-gray-400 pb-24"
      :data-set="preview"
      header="XML Vorschau"
      header-class="font-bold"
      :is-expanded="false"
      title="XML Vorschau"
    >
      <CodeSnippet
        v-for="(item, index) in preview"
        :key="index"
        class="mb-16"
        title=""
        :xml="item.xml!"
      />
    </ExpandableContent>
    <InfoModal
      v-else-if="previewError"
      aria-label="Fehler beim Laden der Preview"
      class="mt-8"
      :description="previewError.description"
      :title="previewError.title"
    />
  </div>
</template>
