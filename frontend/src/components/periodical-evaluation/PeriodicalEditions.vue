<script lang="ts" setup>
import { computed, ref, watch, onMounted } from "vue"
import { useRouter } from "vue-router"
import DateUtil from "../../utils/dateUtil"
import Tooltip from "../Tooltip.vue"
import CellHeaderItem from "@/components/CellHeaderItem.vue"
import CellItem from "@/components/CellItem.vue"
import ComboboxInput from "@/components/ComboboxInput.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import InfoModal from "@/components/InfoModal.vue"
import InputField from "@/components/input/InputField.vue"
import TextButton from "@/components/input/TextButton.vue"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import TableHeader from "@/components/TableHeader.vue"
import TableRow from "@/components/TableRow.vue"
import TableView from "@/components/TableView.vue"
import { useInternalUser } from "@/composables/useInternalUser"
import useQuery from "@/composables/useQueryFromRoute"
import LegalPeriodical from "@/domain/legalPeriodical"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import errorMessages from "@/i18n/errors.json"
import ComboboxItemService from "@/services/comboboxItemService"
import { ResponseError } from "@/services/httpClient"
import LegalPeriodicalEditionService from "@/services/legalPeriodicalEditionService"
import { useEditionStore } from "@/stores/editionStore"
import IconDelete from "~icons/ic/baseline-close"
import IconEdit from "~icons/ic/outline-edit"

const emptyStatus = computed(() => {
  if (!currentEditions.value) {
    return "Wählen Sie ein Periodikum um die Ausgaben anzuzeigen."
  } else if (currentEditions.value.length === 0) {
    return errorMessages.SEARCH_RESULTS_NOT_FOUND.title
  } else return undefined
})

const router = useRouter()
const selectedLegalPeriodical = ref<LegalPeriodical>()
const currentEditions = ref<LegalPeriodicalEdition[]>()
const { pushQueryToRoute, route, resetQuery } = useQuery<"q">()
const searchResponseError = ref<ResponseError | undefined>()
const saveResponseError = ref<ResponseError | undefined>()
const deleteResponseError = ref<ResponseError | undefined>()
const isLoading = ref(false)
const editionStore = useEditionStore()
const isInternalUser = useInternalUser()

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
  if (response.error) {
    searchResponseError.value = response.error
  } else if (response.data) {
    currentEditions.value = response.data
  }

  isLoading.value = false
}

async function addEdition() {
  saveResponseError.value = undefined
  const edition = new LegalPeriodicalEdition({
    legalPeriodical: { uuid: legalPeriodical?.value?.value.uuid },
  })
  const response = await LegalPeriodicalEditionService.save(edition)

  if (response.error) {
    saveResponseError.value = {
      title:
        "Neue Ausgabe konnte nicht erstellt werden. Bitte laden Sie die Seite neu.",
    }
  } else if (response.data) {
    editionStore.edition = undefined
    await router.push({
      name: "caselaw-periodical-evaluation-editionId-edition",
      params: { editionId: response.data.id },
      query: {},
    })
  }
}

const legalPeriodical = computed({
  get: () =>
    selectedLegalPeriodical.value?.abbreviation
      ? {
          label: selectedLegalPeriodical.value.abbreviation,
          value: selectedLegalPeriodical.value,
        }
      : undefined,
  set: (newValue) => {
    const legalPeriodical = { ...newValue } as LegalPeriodical
    if (newValue) {
      selectedLegalPeriodical.value = legalPeriodical
    } else {
      selectedLegalPeriodical.value = undefined
    }
  },
})

async function handleDeleteEdition(edition: LegalPeriodicalEdition) {
  deleteResponseError.value = undefined
  if (edition?.id) {
    const response = await LegalPeriodicalEditionService.delete(edition.id)
    if (response.error) {
      alert("Fehler beim Löschen der Ausgabe")
      deleteResponseError.value = response.error
    } else if (currentEditions.value) {
      currentEditions.value = currentEditions.value.filter(
        (item) => item.id !== edition.id,
      )
    }
  }
}

watch(
  selectedLegalPeriodical,
  async (newFilter) => {
    if (newFilter && newFilter.uuid) {
      await getEditions(newFilter.uuid)
      pushQueryToRoute({ q: newFilter.uuid })
    } else {
      currentEditions.value = undefined
      resetQuery()
    }
  },
  { deep: true },
)

/**
 * Check if there is an edition id param and load edition if present
 */
onMounted(async () => {
  const legalPeriodicalId = route.query.q as string
  if (legalPeriodicalId) {
    await getEditions(legalPeriodicalId)
  }
})
</script>

<template>
  <div class="flex flex-col gap-24 p-24">
    <div>
      <FlexContainer class="pb-16" justify-content="justify-between">
        <h1 class="ds-heading-02-reg" data-testid="periodical-evaluation-title">
          Periodika
        </h1>
      </FlexContainer>
      <FlexContainer>
        <div
          class="flex flex-grow flex-col gap-16 bg-blue-200 p-16"
          role="search"
        >
          <InputField id="legalPeriodical" label="Periodikum*">
            <ComboboxInput
              id="legalPeriodical"
              v-model="legalPeriodical"
              aria-label="Periodikum"
              class="flex-shrink flex-grow-0 basis-1/2"
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
          <div v-if="saveResponseError">
            <InfoModal
              :description="saveResponseError.description"
              :title="saveResponseError.title"
            />
          </div>
        </div>
      </FlexContainer>
    </div>
    <!-- Delete Error State -->

    <div class="flex h-full flex-col gap-24">
      <div v-if="deleteResponseError">
        <InfoModal
          :description="deleteResponseError.description"
          :title="deleteResponseError.title"
        />
      </div>
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
              <Tooltip v-if="isInternalUser" text="Bearbeiten">
                <router-link
                  class="flex cursor-pointer border-2 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
                  target="_blank"
                  :to="{
                    name: 'caselaw-periodical-evaluation-editionId-references',
                    params: { editionId: edition.id },
                  }"
                >
                  <IconEdit class="text-blue-800" />
                </router-link>
              </Tooltip>

              <div
                v-else
                aria-label="Ausgabe kann nicht editiert werden"
                class="border-2 border-solid border-gray-600 p-4 text-gray-600"
              >
                <IconEdit />
              </div>
              <Tooltip
                v-if="edition.references?.length == 0 && isInternalUser"
                text="Löschen"
              >
                <button
                  aria-label="Ausgabe löschen"
                  class="flex cursor-pointer border-2 border-l-0 border-solid border-blue-800 p-4 text-blue-800 hover:bg-blue-200 focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800 active:border-blue-200 active:bg-blue-200"
                  @click="
                    handleDeleteEdition(edition as LegalPeriodicalEdition)
                  "
                  @keyup.enter="
                    handleDeleteEdition(edition as LegalPeriodicalEdition)
                  "
                >
                  <IconDelete class="text-blue-800" />
                </button>
              </Tooltip>

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

      <!-- Error State -->
      <div v-if="searchResponseError">
        <InfoModal
          :description="searchResponseError.description"
          :title="searchResponseError.title"
        />
      </div>

      <!-- Empty State -->
      <div
        v-if="!searchResponseError && !isLoading"
        class="my-112 grid justify-items-center"
      >
        <span class="mb-16">{{ emptyStatus }}</span>
        <slot name="newlink" />
      </div>
    </div>
  </div>
</template>
