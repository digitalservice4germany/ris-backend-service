<script lang="ts" setup>
import { computed, onMounted, ref, watch } from "vue"
import { ValidationError } from "./input/types"
import SearchResultList, { SearchResults } from "./SearchResultList.vue"
import ComboboxInput from "@/components/ComboboxInput.vue"
import DateInput from "@/components/input/DateInput.vue"
import InputField from "@/components/input/InputField.vue"
import TextButton from "@/components/input/TextButton.vue"
import TextInput from "@/components/input/TextInput.vue"
import Pagination, { Page } from "@/components/Pagination.vue"
import { useValidationStore } from "@/composables/useValidationStore"
import values from "@/data/values.json"
import ActiveCitation from "@/domain/activeCitation"
import { CitationType } from "@/domain/citationType"
import RelatedDocumentation from "@/domain/relatedDocumentation"
import ComboboxItemService from "@/services/comboboxItemService"
import documentUnitService from "@/services/documentUnitService"

const props = defineProps<{
  modelValue?: ActiveCitation
  modelValueList?: ActiveCitation[]
}>()

const emit = defineEmits<{
  "update:modelValue": [value: ActiveCitation]
  addEntry: [void]
  cancelEdit: [void]
  removeEntry: [value?: boolean]
}>()

const lastSearchInput = ref(new ActiveCitation())
const lastSavedModelValue = ref(new ActiveCitation({ ...props.modelValue }))
const activeCitation = ref(new ActiveCitation({ ...props.modelValue }))

const validationStore =
  useValidationStore<(typeof ActiveCitation.fields)[number]>()
const pageNumber = ref<number>(0)
const itemsPerPage = ref<number>(15)
const isLoading = ref(false)

const activeCitationType = computed({
  get: () =>
    activeCitation?.value?.citationType
      ? {
          label: activeCitation.value.citationType.label,
          value: activeCitation.value.citationType,
          additionalInformation:
            activeCitation.value.citationType.jurisShortcut,
        }
      : undefined,
  set: (newValue) => {
    if (!newValue)
      validationStore.add("Pflichtfeld nicht befüllt", "citationType")
    if (newValue?.label) {
      activeCitation.value.citationType = { ...newValue }
    } else {
      activeCitation.value.citationType = undefined
    }
  },
})

const searchResultsCurrentPage = ref<Page<RelatedDocumentation>>()
const searchResults = ref<SearchResults<RelatedDocumentation>>()

async function search() {
  isLoading.value = true
  const activeCitationRef = new ActiveCitation({
    ...activeCitation.value,
  })

  if (
    activeCitationRef.court != lastSearchInput.value.court ||
    activeCitationRef.decisionDate != lastSearchInput.value.decisionDate ||
    activeCitationRef.fileNumber != lastSearchInput.value.fileNumber ||
    activeCitationRef.documentType != lastSearchInput.value.documentType
  ) {
    pageNumber.value = 0
  }

  if (activeCitationRef.citationType) {
    delete activeCitationRef["citationType"]
  }

  const urlParams = window.location.pathname.split("/")
  const documentNumberToExclude =
    urlParams[urlParams.indexOf("documentunit") + 1]

  const response = await documentUnitService.searchByRelatedDocumentation(
    activeCitationRef,
    {
      ...(pageNumber.value != undefined
        ? { pg: pageNumber.value.toString() }
        : {}),
      ...(itemsPerPage.value != undefined
        ? { sz: itemsPerPage.value.toString() }
        : {}),
      ...(documentNumberToExclude != undefined
        ? { documentNumber: documentNumberToExclude.toString() }
        : {}),
    },
  )
  if (response.data) {
    searchResultsCurrentPage.value = {
      ...response.data,
      content: response.data.content.map(
        (decision) => new RelatedDocumentation({ ...decision }),
      ),
    }
    searchResults.value = response.data.content.map((searchResult) => {
      return {
        decision: new RelatedDocumentation({ ...searchResult }),
        isLinked: searchResult.isLinkedWith(props.modelValueList),
      }
    })
  }
  lastSearchInput.value = activeCitationRef
  isLoading.value = false
}

async function updatePage(page: number) {
  pageNumber.value = page
  search()
}

async function validateRequiredInput() {
  validationStore.reset()

  activeCitation.value.missingRequiredFields.forEach((missingField) =>
    validationStore.add("Pflichtfeld nicht befüllt", missingField),
  )
}

async function addActiveCitation() {
  if (
    !validationStore.getByMessage("Kein valides Datum").length &&
    !validationStore.getByMessage("Unvollständiges Datum").length &&
    !validationStore.getByMessage("Das Datum darf nicht in der Zukunft liegen")
      .length
  ) {
    await validateRequiredInput()
    emit("update:modelValue", activeCitation.value as ActiveCitation)
    emit("addEntry")
  }
}

async function addActiveCitationFromSearch(decision: RelatedDocumentation) {
  const newActiveCitationType = {
    ...activeCitationType.value?.value,
  } as CitationType
  activeCitation.value = new ActiveCitation({
    ...decision,
    referenceFound: true,
    citationType: newActiveCitationType,
  })
  emit("update:modelValue", activeCitation.value as ActiveCitation)
  emit("addEntry")
  scrollToTop()
}

function scrollToTop() {
  const element = document.getElementById("activeCitations")
  if (element) {
    const headerOffset = values.headerOffset
    const elementPosition = element?.getBoundingClientRect().top
    const offsetPosition = elementPosition + window.scrollY - headerOffset

    window.scrollTo({
      top: offsetPosition,
      behavior: "smooth",
    })
  }
}

function updateDateFormatValidation(
  validationError: ValidationError | undefined,
) {
  if (validationError)
    validationStore.add(validationError.message, "decisionDate")
  else validationStore.remove("decisionDate")
}

watch(
  activeCitation,
  () => {
    if (
      !activeCitation.value.citationTypeIsSet &&
      !activeCitation.value.isEmpty
    ) {
      validationStore.add("Pflichtfeld nicht befüllt", "citationType")
    } else if (activeCitation.value.citationTypeIsSet) {
      validationStore.remove("citationType")
    }
  },
  { deep: true },
)

watch(
  () => props.modelValue,
  () => {
    activeCitation.value = new ActiveCitation({ ...props.modelValue })
    lastSavedModelValue.value = new ActiveCitation({ ...props.modelValue })
    if (lastSavedModelValue.value.isEmpty) validationStore.reset()
  },
)

onMounted(() => {
  if (props.modelValue?.isEmpty !== undefined) {
    validateRequiredInput()
  }
  activeCitation.value = new ActiveCitation({ ...props.modelValue })
})
</script>

<template>
  <div class="flex flex-col gap-24">
    <InputField
      id="activeCitationPredicate"
      v-slot="slotProps"
      label="Art der Zitierung *"
      :validation-error="validationStore.getByField('citationType')"
    >
      <ComboboxInput
        id="activeCitationPredicate"
        v-model="activeCitationType"
        aria-label="Art der Zitierung"
        clear-on-choosing-item
        :has-error="slotProps.hasError"
        :item-service="ComboboxItemService.getCitationTypes"
        @focus="validationStore.remove('citationType')"
      ></ComboboxInput>
    </InputField>
    <div class="flex flex-col gap-24">
      <div class="flex justify-between gap-24">
        <InputField
          id="activeCitationCourt"
          v-slot="slotProps"
          label="Gericht *"
          :validation-error="validationStore.getByField('court')"
        >
          <ComboboxInput
            id="activeCitationCourt"
            v-model="activeCitation.court"
            aria-label="Gericht Aktivzitierung"
            clear-on-choosing-item
            :has-error="slotProps.hasError"
            :item-service="ComboboxItemService.getCourts"
            :read-only="activeCitation.hasForeignSource"
            @focus="validationStore.remove('court')"
          >
          </ComboboxInput>
        </InputField>
        <InputField
          id="activeCitationDecisionDate"
          v-slot="slotProps"
          label="Entscheidungsdatum *"
          :validation-error="validationStore.getByField('decisionDate')"
          @update:validation-error="
            (validationError) => updateDateFormatValidation(validationError)
          "
        >
          <DateInput
            id="activeCitationDecisionDate"
            v-model="activeCitation.decisionDate"
            aria-label="Entscheidungsdatum Aktivzitierung"
            class="ds-input-medium"
            :has-error="slotProps.hasError"
            :readonly="activeCitation.hasForeignSource"
            @focus="validationStore.remove('decisionDate')"
            @update:validation-error="slotProps.updateValidationError"
          ></DateInput>
        </InputField>
      </div>
      <div class="flex justify-between gap-24">
        <InputField
          id="activeCitationFileNumber"
          v-slot="slotProps"
          label="Aktenzeichen *"
          :validation-error="validationStore.getByField('fileNumber')"
        >
          <TextInput
            id="activeCitationDocumentType"
            v-model="activeCitation.fileNumber"
            aria-label="Aktenzeichen Aktivzitierung"
            :has-error="slotProps.hasError"
            :readonly="activeCitation.hasForeignSource"
            size="medium"
            @focus="validationStore.remove('fileNumber')"
          ></TextInput>
        </InputField>
        <InputField id="activeCitationDecisionDocumentType" label="Dokumenttyp">
          <ComboboxInput
            id="activeCitationDecisionDocumentType"
            v-model="activeCitation.documentType"
            aria-label="Dokumenttyp Aktivzitierung"
            :item-service="ComboboxItemService.getDocumentTypes"
            :read-only="activeCitation.hasForeignSource"
          ></ComboboxInput>
        </InputField>
      </div>
    </div>
    <div class="flex w-full flex-row justify-between">
      <div>
        <div class="flex gap-16">
          <TextButton
            v-if="!activeCitation.hasForeignSource"
            aria-label="Nach Entscheidung suchen"
            button-type="primary"
            label="Suchen"
            size="small"
            @click="search"
          />
          <TextButton
            aria-label="Aktivzitierung speichern"
            button-type="tertiary"
            :disabled="activeCitation.isEmpty"
            label="Übernehmen"
            size="small"
            @click.stop="addActiveCitation"
          />
          <TextButton
            v-if="!lastSavedModelValue.isEmpty"
            aria-label="Abbrechen"
            button-type="ghost"
            label="Abbrechen"
            size="small"
            @click.stop="emit('cancelEdit')"
          />
        </div>
      </div>
      <TextButton
        v-if="!lastSavedModelValue.isEmpty"
        aria-label="Eintrag löschen"
        button-type="destructive"
        label="Eintrag löschen"
        size="small"
        @click.stop="emit('removeEntry', true)"
      />
    </div>

    <div class="bg-blue-200">
      <Pagination
        navigation-position="bottom"
        :page="searchResultsCurrentPage"
        @update-page="updatePage"
      >
        <SearchResultList
          :is-loading="isLoading"
          :search-results="searchResults"
          @link-decision="addActiveCitationFromSearch"
        />
      </Pagination>
    </div>
  </div>
</template>
