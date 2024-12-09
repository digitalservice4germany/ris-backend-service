<script setup lang="ts">
import FieldOfLawSearchResultsListItem from "@/components/field-of-law/FieldOfLawSearchResultsListItem.vue"
import Pagination, { Page } from "@/components/Pagination.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"
import errorMessages from "@/i18n/errors.json"

defineProps<{
  currentPage?: Page<FieldOfLaw>
  results?: FieldOfLaw[]
}>()

const emit = defineEmits<{
  search: [page: number]
  "node:add": [node: FieldOfLaw]
  "linkedField:clicked": [node: FieldOfLaw]
}>()
</script>

<template>
  <div v-if="currentPage" class="flex flex-1 flex-col">
    <Pagination
      navigation-position="bottom"
      :page="currentPage"
      @update-page="(page: number) => emit('search', page)"
    >
      <FieldOfLawSearchResultsListItem
        v-for="(fieldOfLawNode, idx) in results"
        :key="idx"
        :field-of-law="fieldOfLawNode"
        @linked-field:clicked="emit('linkedField:clicked', $event)"
        @node:add="emit('node:add', fieldOfLawNode)"
      />
    </Pagination>
    <div v-if="!currentPage?.content || currentPage?.content?.length == 0">
      {{ errorMessages.SEARCH_RESULTS_NOT_FOUND.title }}
    </div>
  </div>
</template>
