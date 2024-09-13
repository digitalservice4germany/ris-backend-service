<script lang="ts" setup>
import { computed, ref, watch, onMounted } from "vue"
import { useRouter } from "vue-router"
import DateUtil from "../../utils/dateUtil"
import CellHeaderItem from "@/components/CellHeaderItem.vue"
import CellItem from "@/components/CellItem.vue"
import ComboboxInput from "@/components/ComboboxInput.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import InputField from "@/components/input/InputField.vue"
import TextButton from "@/components/input/TextButton.vue"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import SearchResultStatus from "@/components/SearchResultStatus.vue"
import TableHeader from "@/components/TableHeader.vue"
import TableRow from "@/components/TableRow.vue"
import TableView from "@/components/TableView.vue"
import { useInternalUser } from "@/composables/useInternalUser"
import useQuery, { Query } from "@/composables/useQueryFromRoute"
import LegalPeriodical from "@/domain/legalPeriodical"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import ComboboxItemService from "@/services/comboboxItemService"
import { ResponseError } from "@/services/httpClient"
import LegalPeriodicalEditionService from "@/services/legalPeriodicalEditionService"
import { useEditionStore } from "@/stores/editionStore"
import IconDelete from "~icons/ic/baseline-close"
import IconEdit from "~icons/ic/outline-edit"

const emptyResponse: ResponseError = {
  title: "Wählen Sie ein Periodikum um die Ausgaben anzuzeigen.",
}

const router = useRouter()
const filter = ref<LegalPeriodical>()
const currentEditions = ref<LegalPeriodicalEdition[]>()
const { getQueryFromRoute, pushQueryToRoute, route } = useQuery<"q">()
const query = ref(getQueryFromRoute())
const searchResponseError = ref<ResponseError | undefined>(emptyResponse)
const isLoading = ref(false)
const editionStore = useEditionStore()
const isInternalUser = useInternalUser()

/**
 * Sets a timeout before pushing the search query to the route,
 * in order to only change the url params when the user input pauses.
 */
const debouncedPushQueryToRoute = (() => {
  let timeoutId: number | null = null

  return (currentQuery: Query<string>) => {
    if (timeoutId != null) window.clearTimeout(timeoutId)

    timeoutId = window.setTimeout(() => pushQueryToRoute(currentQuery), 500)
  }
})()

/**
 * Loads all editions of a legal periodical
 */
async function getEditions(legalPeriodicalId: string) {
  isLoading.value = true
  searchResponseError.value = undefined
  const response =
    await LegalPeriodicalEditionService.getAllByLegalPeriodicalId(
      legalPeriodicalId,
    )
  if (response.data) {
    currentEditions.value = response.data
  }
  if (response.error) {
    searchResponseError.value = response.error
  }
  isLoading.value = false
}

async function addEdition() {
  const edition = new LegalPeriodicalEdition({
    legalPeriodical: { uuid: legalPeriodical?.value?.value.uuid },
  })
  const response = await LegalPeriodicalEditionService.save(edition)
  editionStore.edition = response.data
  await router.push({
    name: "caselaw-periodical-evaluation-editionId-edition",
    params: { editionId: response.data?.id },
  })
}

const legalPeriodical = computed({
  get: () =>
    filter.value?.abbreviation
      ? {
          label: filter.value.abbreviation,
          value: filter.value,
        }
      : undefined,
  set: (newValue) => {
    const legalPeriodical = { ...newValue } as LegalPeriodical
    if (newValue) {
      filter.value = legalPeriodical
    } else {
      filter.value = undefined
      searchResponseError.value = emptyResponse
    }
  },
})

async function handleDeleteEdition(edition: LegalPeriodicalEdition) {
  if (edition?.id) {
    await LegalPeriodicalEditionService.delete(edition.id)
    await getEditions(filter.value?.uuid?.toString() || "")
  }
}

/**
 * Get query from url and set local query value
 */
watch(route, () => {
  const currentQuery = getQueryFromRoute()
  if (JSON.stringify(query.value) != JSON.stringify(currentQuery))
    query.value = currentQuery
})

watch(
  filter,
  async (newFilter) => {
    if (newFilter && newFilter.uuid) {
      await getEditions(newFilter.uuid)
      debouncedPushQueryToRoute({ q: newFilter.uuid })
    } else {
      currentEditions.value = []
    }
  },
  { deep: true },
)

onMounted(() => {
  if (query.value.q) {
    getEditions(query.value.q)
  }
})
</script>

<template>
  <div>
    <div class="gap-16 p-16">
      <FlexContainer class="pb-16" justify-content="justify-between">
        <h1 class="ds-heading-02-reg" data-testid="periodical-evaluation-title">
          Periodika
        </h1>
      </FlexContainer>
      <FlexContainer align-items="items-end" flex-direction="flex-row">
        <div
          class="flex flex-grow flex-col gap-16 bg-blue-200 p-16"
          role="search"
        >
          <InputField id="legalPeriodical" label="Periodikum*">
            <ComboboxInput
              id="legalPeriodical"
              v-model="legalPeriodical"
              aria-label="Periodikum"
              class="max-w-[672px]"
              clear-on-choosing-item
              :has-error="false"
              :item-service="ComboboxItemService.getLegalPeriodicals"
              placeholder="Nach Periodikum suchen"
            ></ComboboxInput>
          </InputField>
          <TextButton
            v-if="legalPeriodical && isInternalUser"
            aria-label="Neue Periodikumsauswertung"
            class="ds-button-02-reg"
            label="Neue Periodikumsauswertung"
            @click="addEdition"
          ></TextButton>
        </div>
      </FlexContainer>
    </div>

    <div class="flex h-full flex-col p-24">
      <TableView class="relative table w-full border-separate">
        <TableHeader>
          <CellHeaderItem>Ausgabe</CellHeaderItem>
          <CellHeaderItem>Periodikum</CellHeaderItem>
          <CellHeaderItem>Anzahl der Fundstellen</CellHeaderItem>
          <CellHeaderItem>Hinzugefügt</CellHeaderItem>
          <CellHeaderItem />
        </TableHeader>

        <TableRow v-for="edition in currentEditions" :key="edition.id">
          <CellItem> {{ edition.name }}</CellItem>
          <CellItem>
            {{ edition.legalPeriodical?.abbreviation || "" }}
          </CellItem>
          <CellItem>
            {{ edition.references?.length }}
          </CellItem>
          <CellItem>
            <span :class="{ 'text-gray-800': !edition.createdAt }">
              {{ DateUtil.formatDate(edition.createdAt) || "Datum unbekannt" }}
            </span>
          </CellItem>
          <CellItem class="flex">
            <div class="float-end flex">
              <router-link
                v-if="isInternalUser"
                class="cursor-pointer border-2 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
                target="_blank"
                :to="{
                  name: 'caselaw-periodical-evaluation-editionId-references',
                  params: { editionId: edition.id },
                }"
              >
                <IconEdit class="text-blue-800" />
              </router-link>
              <div
                v-else
                aria-label="Ausgabe kann nicht editiert werden"
                class="border-2 border-solid border-gray-600 p-4 text-gray-600"
              >
                <IconEdit />
              </div>
              <button
                v-if="edition.references?.length == 0 && isInternalUser"
                aria-label="Ausgabe löschen"
                class="cursor-pointer border-2 border-l-0 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
                @click="handleDeleteEdition(edition)"
                @keyup.enter="handleDeleteEdition(edition)"
              >
                <IconDelete class="text-blue-800" />
              </button>

              <div
                v-else
                aria-label="Ausgabe kann nicht gelöscht werden"
                class="border-2 border-l-0 border-solid border-gray-600 p-4 text-gray-600"
              >
                <IconDelete />
              </div>
            </div>
          </CellItem>
        </TableRow>
      </TableView>
      <div
        v-if="isLoading"
        class="grid justify-items-center bg-white bg-opacity-60 py-112"
      >
        <LoadingSpinner />
      </div>
      <SearchResultStatus
        v-if="!isLoading"
        :response-error="searchResponseError"
      />
    </div>
  </div>
</template>
