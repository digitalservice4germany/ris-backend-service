<script lang="ts" setup>
import { ref, watch } from "vue"
import FieldOfLawListEntry from "@/components/FieldOfLawListEntry.vue"
import TextButton from "@/components/input/TextButton.vue"
import TextInput from "@/components/input/TextInput.vue"
import Pagination, { Page } from "@/components/Pagination.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"
import errorMessages from "@/i18n/errors.json"
import service from "@/services/fieldOfLawService"
import StringsUtil from "@/utils/stringsUtil"

const emit = defineEmits<{
  "node:select": [node: FieldOfLaw]
  "linked-field:select": [node: FieldOfLaw]
  "node:unselect": []
  "do-show-norms": []
}>()

const searchStr = ref("")
const results = ref<FieldOfLaw[]>()
const currentPage = ref<Page<FieldOfLaw>>()
const itemsPerPage = 10

async function submitSearch(page: number) {
  if (StringsUtil.isEmpty(searchStr.value)) {
    return removeSelectedNode()
  }

  const response = await service.searchForFieldsOfLaw(
    page,
    itemsPerPage,
    searchStr.value,
  )
  if (response.data) {
    currentPage.value = response.data
    results.value = response.data.content
    if (results.value?.[0]) {
      emit("node:select", results.value[0] as FieldOfLaw)
    }
    if (searchStr.value.includes("norm:")) {
      emit("do-show-norms")
    }
  } else {
    currentPage.value = undefined
    results.value = undefined
    console.error("Error searching for Nodes")
  }
}

function removeSelectedNode() {
  emit("node:unselect")
}

watch(
  searchStr,
  async () => {
    if (StringsUtil.isEmpty(searchStr.value)) {
      removeSelectedNode()
    }
  },
  { immediate: true },
)
</script>

<template>
  <h1 class="ds-heading-03-reg pb-8">Suche</h1>
  <div class="flex flex-col">
    <div class="pb-28">
      <div class="flex flex-row items-stretch">
        <div class="grow">
          <TextInput
            id="FieldOfLawSearchTextInput"
            v-model="searchStr"
            aria-label="Sachgebiete Suche"
            full-height
            size="medium"
            @enter-released="submitSearch(0)"
          />
        </div>
        <div class="pl-8">
          <TextButton
            aria-label="Sachgebietssuche ausführen"
            button-type="secondary"
            class="w-fit"
            label="Suchen"
            @click="submitSearch(0)"
          />
        </div>
      </div>
    </div>
    <Pagination
      v-if="currentPage"
      navigation-position="bottom"
      :page="currentPage"
      @update-page="submitSearch"
    >
      <FieldOfLawListEntry
        v-for="(fieldOfLawNode, idx) in results"
        :key="idx"
        :field-of-law="fieldOfLawNode"
        @linked-field:select="emit('linked-field:select', $event)"
        @node:select="emit('node:select', fieldOfLawNode)"
      />
    </Pagination>
    <div v-if="!currentPage?.content || currentPage?.content?.length == 0">
      {{ errorMessages.SEARCH_RESULTS_NOT_FOUND.title }}
    </div>
  </div>
</template>
