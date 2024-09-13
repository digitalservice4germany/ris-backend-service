<script lang="ts" setup>
import { ref, computed, watch } from "vue"
import { ValidationError } from "../input/types"
import ComboboxInput from "@/components/ComboboxInput.vue"
import DateInput from "@/components/input/DateInput.vue"
import InputField from "@/components/input/InputField.vue"
import TextButton from "@/components/input/TextButton.vue"
import TextInput from "@/components/input/TextInput.vue"
import Pagination, { Page } from "@/components/Pagination.vue"
import SearchResultList, {
  SearchResults,
} from "@/components/SearchResultList.vue"
import { useValidationStore } from "@/composables/useValidationStore"
import Reference from "@/domain/reference"
import RelatedDocumentation from "@/domain/relatedDocumentation"
import ComboboxItemService from "@/services/comboboxItemService"
import documentUnitService from "@/services/documentUnitService"
import { useEditionStore } from "@/stores/editionStore"
import StringsUtil from "@/utils/stringsUtil"

const props = defineProps<{
  modelValue?: Reference
  modelValueList: Reference[]
  isSaved: boolean
}>()
const emit = defineEmits<{
  "update:modelValue": [value: Reference]
  addEntry: [void]
  cancelEdit: [void]
  removeEntry: [value: Reference]
}>()

const store = useEditionStore()
const lastSavedModelValue = ref(new Reference({ ...props.modelValue }))
const reference = ref<Reference>(new Reference({ ...props.modelValue }))
const validationStore = useValidationStore()
const pageNumber = ref<number>(0)
const itemsPerPage = ref<number>(15)
const isLoading = ref(false)

const searchResultsCurrentPage = ref<Page<RelatedDocumentation>>()
const searchResults = ref<SearchResults<RelatedDocumentation>>()

const legalPeriodical = computed(() =>
  store.edition && store.edition?.legalPeriodical
    ? {
        label: store.edition.legalPeriodical.abbreviation,
        value: store.edition.legalPeriodical,
      }
    : undefined,
)

const relatedDocumentationUnit = ref<RelatedDocumentation>(
  props.modelValue?.documentationUnit
    ? new RelatedDocumentation({ ...props.modelValue.documentationUnit })
    : new RelatedDocumentation(),
)

const prefix = computed({
  get: () =>
    store.edition && store.edition?.prefix ? store.edition.prefix : undefined,
  set: (newValue) => {
    store.edition!.prefix = newValue
  },
})
const suffix = computed({
  get: () =>
    store.edition && store.edition?.suffix ? store.edition.suffix : undefined,
  set: (newValue) => {
    store.edition!.suffix = newValue
  },
})

function buildCitation(): string | undefined {
  if (StringsUtil.isEmpty(reference.value.citation)) {
    return undefined
  } else {
    return StringsUtil.mergeNonBlankStrings(
      [prefix.value, reference.value.citation, suffix.value],
      "",
    )
  }
}

function updateDateFormatValidation(
  validationError: ValidationError | undefined,
) {
  if (validationError)
    validationStore.add(validationError.message, "decisionDate")
  else validationStore.remove("decisionDate")
}

async function search() {
  isLoading.value = true

  const response = await documentUnitService.searchByRelatedDocumentation(
    relatedDocumentationUnit.value,
    {
      ...(pageNumber.value != undefined
        ? { pg: pageNumber.value.toString() }
        : {}),
      ...(itemsPerPage.value != undefined
        ? { sz: itemsPerPage.value.toString() }
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
        isLinked: searchResult.isLinkedWith(
          props.modelValueList.map((item) => item.documentationUnit!),
        ),
      }
    })
  }
  isLoading.value = false
}

async function updatePage(page: number) {
  pageNumber.value = page
  await search()
}

async function validateRequiredInput(toValidateReference: Reference) {
  if (toValidateReference.missingRequiredFields?.length) {
    toValidateReference.missingRequiredFields.forEach((missingField) => {
      validationStore.add("Pflichtfeld nicht befüllt", missingField)
    })
  }
}

async function addReference(decision: RelatedDocumentation) {
  validationStore.reset()
  relatedDocumentationUnit.value = new RelatedDocumentation({ ...decision })
  const newReference: Reference = new Reference({
    id: reference.value.id,
    citation: props.isSaved ? reference.value.citation : buildCitation(),
    referenceSupplement: reference.value.referenceSupplement,
    footnote: reference.value.footnote,
    legalPeriodical: reference.value.legalPeriodical,
    legalPeriodicalRawValue: reference.value.legalPeriodicalRawValue,
    documentationUnit: new RelatedDocumentation({ ...decision }),
  })

  lastSavedModelValue.value = newReference

  await validateRequiredInput(newReference)

  if (validationStore.isValid()) {
    emit("update:modelValue", newReference)
    emit("addEntry")
  }
}

watch(
  () => store.edition?.legalPeriodical,
  (legalPeriodical) => {
    if (legalPeriodical) {
      reference.value = new Reference({
        ...props.modelValue,
        legalPeriodical: legalPeriodical,
      })
    }
  },
  { immediate: true },
)

watch(
  () => props.modelValue,
  () => {
    reference.value = new Reference({ ...props.modelValue })
    validationStore.reset()

    relatedDocumentationUnit.value = props.modelValue?.documentationUnit
      ? new RelatedDocumentation({ ...props.modelValue.documentationUnit })
      : new RelatedDocumentation()
  },
)
</script>

<template>
  <div class="flex h-full flex-col space-y-24 py-16">
    <div class="flex flex-col gap-24">
      <div class="flex flex-col gap-24">
        <div class="flex justify-between gap-24">
          <div id="citationInputField" class="flex-1">
            <InputField
              v-if="!isSaved"
              id="citation"
              v-slot="slotProps"
              label="Zitatstelle *"
              :validation-error="validationStore.getByField('citation')"
            >
              <div class="flex flex-grow flex-row gap-16">
                <TextInput
                  id="citation prefix"
                  v-model="prefix"
                  aria-label="Zitatstelle Präfix"
                  placeholder="Präfix"
                  read-only
                  size="medium"
                ></TextInput>
                <TextInput
                  id="citation"
                  v-model="reference.citation"
                  aria-label="Zitatstelle *"
                  :has-error="slotProps.hasError"
                  placeholder="Variable"
                  size="medium"
                  @blur="validateRequiredInput(reference)"
                  @focus="validationStore.remove('citation')"
                ></TextInput>
                <TextInput
                  id="citation suffix"
                  v-model="suffix"
                  aria-label="Zitatstelle Suffix"
                  placeholder="Suffix"
                  read-only
                  size="medium"
                ></TextInput>
              </div>
            </InputField>

            <InputField
              v-else
              id="citation"
              v-slot="slotProps"
              label="Zitatstelle *"
              :validation-error="validationStore.getByField('citation')"
            >
              <div class="flex flex-grow flex-row gap-16">
                <TextInput
                  id="citation"
                  v-model="reference.citation"
                  aria-label="Zitatstelle *"
                  :has-error="slotProps.hasError"
                  size="medium"
                  @blur="validateRequiredInput(reference)"
                  @focus="validationStore.remove('citation')"
                ></TextInput>
              </div>
            </InputField>

            <div v-if="legalPeriodical" class="ds-label-03-reg pt-4">
              Zitierbeispiel: {{ legalPeriodical.value.citationStyle }}
            </div>
          </div>

          <InputField
            id="referenceSupplement"
            v-slot="slotProps"
            class="flex-1"
            label="Klammernzusatz"
          >
            <TextInput
              id="referenceSupplement"
              v-model="reference.referenceSupplement"
              aria-label="Klammernzusatz"
              :has-error="slotProps.hasError"
              size="medium"
            ></TextInput>
          </InputField>
        </div>
      </div>
    </div>
    <div class="flex flex-col gap-24">
      <div v-if="!isSaved" class="ds-heading-03-reg pt-24">
        Entscheidung hinzufügen
      </div>
      <div class="flex justify-between gap-24">
        <InputField
          id="courtInput"
          v-slot="slotProps"
          label="Gericht"
          :validation-error="validationStore.getByField('court')"
        >
          <ComboboxInput
            id="courtInput"
            v-model="relatedDocumentationUnit.court"
            aria-label="Gericht"
            clear-on-choosing-item
            :has-error="slotProps.hasError"
            :item-service="ComboboxItemService.getCourts"
            :read-only="reference?.documentationUnit?.hasForeignSource"
            @focus="validationStore.remove('court')"
          >
          </ComboboxInput>
        </InputField>
        <InputField
          id="decisionDate"
          v-slot="slotProps"
          label="Entscheidungsdatum"
          :validation-error="validationStore.getByField('decisionDate')"
          @update:validation-error="
            (validationError: any) =>
              updateDateFormatValidation(validationError)
          "
        >
          <DateInput
            id="decisionDate"
            v-model="relatedDocumentationUnit.decisionDate"
            aria-label="Entscheidungsdatum"
            class="ds-input-medium"
            :has-error="slotProps.hasError"
            :read-only="reference?.documentationUnit?.hasForeignSource"
            @focus="validationStore.remove('decisionDate')"
            @update:validation-error="slotProps.updateValidationError"
          ></DateInput>
        </InputField>
      </div>
      <div class="flex justify-between gap-24">
        <InputField
          id="fileNumber"
          v-slot="slotProps"
          label="Aktenzeichen"
          :validation-error="validationStore.getByField('fileNumber')"
        >
          <TextInput
            id="fileNumber"
            v-model="relatedDocumentationUnit.fileNumber"
            aria-label="Aktenzeichen"
            :has-error="slotProps.hasError"
            :read-only="reference?.documentationUnit?.hasForeignSource"
            size="medium"
            @focus="validationStore.remove('fileNumber')"
          ></TextInput>
        </InputField>
        <InputField
          id="decisionDocumentType"
          label="Dokumenttyp"
          :validation-error="validationStore.getByField('documentType')"
        >
          <ComboboxInput
            id="decisionDocumentType"
            v-model="relatedDocumentationUnit.documentType"
            aria-label="Dokumenttyp"
            :item-service="ComboboxItemService.getDocumentTypes"
            :read-only="reference?.documentationUnit?.hasForeignSource"
          ></ComboboxInput>
        </InputField>
      </div>
    </div>

    <div class="flex w-full flex-row justify-between">
      <div>
        <div class="flex gap-16">
          <TextButton
            v-if="!modelValue?.hasForeignSource"
            aria-label="Nach Entscheidung suchen"
            button-type="primary"
            label="Suchen"
            size="small"
            @click="search"
          />
          <TextButton
            v-if="!lastSavedModelValue.isEmpty"
            aria-label="Fundstelle vermerken"
            button-type="tertiary"
            data-testid="previous-decision-save-button"
            :disabled="reference.isEmpty"
            label="Übernehmen"
            size="small"
            @click.stop="addReference(relatedDocumentationUnit)"
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
        v-if="isSaved"
        aria-label="Eintrag löschen"
        button-type="destructive"
        label="Eintrag löschen"
        size="small"
        @click.stop="modelValue && emit('removeEntry', modelValue)"
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
          @link-decision="addReference"
        />
      </Pagination>
    </div>
  </div>
</template>
@/stores/editionStore
