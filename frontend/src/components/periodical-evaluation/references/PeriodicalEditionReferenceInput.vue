<script lang="ts" setup>
import { computed, nextTick, onMounted, ref, watch } from "vue"
import ComboboxInput from "@/components/ComboboxInput.vue"
import CreateNewFromSearch from "@/components/CreateNewFromSearch.vue"
import DecisionSummary from "@/components/DecisionSummary.vue"
import { DisplayMode } from "@/components/enumDisplayMode"
import DateInput from "@/components/input/DateInput.vue"
import InputField, { LabelPosition } from "@/components/input/InputField.vue"
import RadioInput from "@/components/input/RadioInput.vue"
import TextButton from "@/components/input/TextButton.vue"
import TextInput from "@/components/input/TextInput.vue"
import { ValidationError } from "@/components/input/types"
import Pagination, { Page } from "@/components/Pagination.vue"
import PopupModal from "@/components/PopupModal.vue"
import SearchResultList, {
  SearchResults,
} from "@/components/SearchResultList.vue"
import { useValidationStore } from "@/composables/useValidationStore"
import DocumentUnit, {
  DocumentationUnitParameters,
} from "@/domain/documentUnit"
import { PublicationState } from "@/domain/publicationStatus"
import Reference from "@/domain/reference"
import RelatedDocumentation from "@/domain/relatedDocumentation"
import ComboboxItemService from "@/services/comboboxItemService"
import documentUnitService from "@/services/documentUnitService"
import FeatureToggleService from "@/services/featureToggleService"
import { ResponseError } from "@/services/httpClient"
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

const containerRef = ref<HTMLElement | null>(null)

const store = useEditionStore()
const reference = ref<Reference>(new Reference({ ...props.modelValue }))
const validationStore = useValidationStore()
const pageNumber = ref<number>(0)
const itemsPerPage = ref<number>(15)
const isLoading = ref(false)
const featureToggle = ref()
const showModal = ref(false)

const searchResultsCurrentPage = ref<Page<RelatedDocumentation>>()
const searchResults = ref<SearchResults<RelatedDocumentation>>()
const createNewFromSearchResponseError = ref<ResponseError | undefined>()

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

const createDocumentationUnitParameters = ref<DocumentationUnitParameters>()

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

/**
 * In case of validation errors it will scroll  back to input fields
 *
 * @returns A promise that resolves after the DOM updates.
 */
async function scrollToTopPosition() {
  await nextTick()
  if (
    containerRef.value instanceof HTMLElement &&
    "scrollIntoView" in containerRef.value
  ) {
    containerRef.value.scrollIntoView({
      block: "start",
    })
  }
}

async function search() {
  isLoading.value = true
  createNewFromSearchResponseError.value = undefined

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

function validateRequiredInput(referenceToValidate?: Reference): boolean {
  // Use the provided referenceToValidate if available, otherwise use the local reference
  const referenceToCheck = referenceToValidate || reference.value

  // Check for missing required fields
  if (
    referenceToCheck.missingRequiredFields?.length &&
    referenceToValidate?.referenceType == "caselaw"
  ) {
    referenceToCheck.missingRequiredFields.forEach((missingField: string) => {
      validationStore.add("Pflichtfeld nicht befüllt", missingField)
    })
    return false // Validation failed
  } else if (
    referenceToCheck.missingRequiredLiteratureFields?.length &&
    referenceToValidate?.referenceType == "literature"
  ) {
    referenceToCheck.missingRequiredLiteratureFields.forEach(
      (missingField: string) => {
        validationStore.add("Pflichtfeld nicht befüllt", missingField)
      },
    )
    return false // Validation failed
  } else {
    return true // Validation passed
  }
}

async function addReference(decision: RelatedDocumentation) {
  validationStore.reset()

  const newReference: Reference = new Reference({
    id: reference.value.id,
    citation: props.isSaved ? reference.value.citation : buildCitation(),
    referenceSupplement: reference.value.referenceSupplement,
    author: reference.value.author,
    documentType: reference.value.documentType,
    referenceType: reference.value.referenceType,
    footnote: reference.value.footnote,
    legalPeriodical: reference.value.legalPeriodical,
    legalPeriodicalRawValue: reference.value.legalPeriodicalRawValue,
    documentationUnit: new RelatedDocumentation({ ...decision }),
  })

  validateRequiredInput(newReference)

  if (validationStore.isValid()) {
    emit("update:modelValue", newReference)
    emit("addEntry")
  } else {
    await scrollToTopPosition()
  }
}

async function addReferenceWithCreatedDocumentationUnit(docUnit: DocumentUnit) {
  if (!docUnit) return
  await addReference(
    new RelatedDocumentation({
      uuid: docUnit.uuid,
      fileNumber: docUnit.coreData.fileNumbers
        ? docUnit.coreData.fileNumbers[0]
        : undefined,
      decisionDate: docUnit.coreData.decisionDate,
      court: docUnit.coreData.court,
      documentType: docUnit.coreData.documentType,
      documentNumber: docUnit.documentNumber,
      status: docUnit.status,
      referenceFound: true,
      createdByReference: reference.value.id,
    }),
  )
}

/**
 * Stops propagation of scrolling event, and toggles the showModal value.
 * @param {boolean} [shouldOpen] - Optional parameter to explicitly set the modal state.
 */
function toggleDeletionConfirmationModal(shouldOpen: boolean | undefined) {
  showModal.value = shouldOpen ?? !showModal.value

  if (showModal.value) {
    const scrollLeft = document.documentElement.scrollLeft
    const scrollTop = document.documentElement.scrollTop
    window.onscroll = () => {
      window.scrollTo(scrollLeft, scrollTop)
    }
  } else {
    window.onscroll = () => {
      return
    }
  }
}

function deleteReference() {
  emit("removeEntry", reference.value)
  toggleDeletionConfirmationModal(false)
}

async function deleteReferenceAndDocUnit() {
  await documentUnitService.delete(
    reference.value.documentationUnit?.uuid || "",
  )
  deleteReference()
}

/*
Relates the legal periodical of edition to the reference
 */
watch(
  () => store.edition?.legalPeriodical,
  (legalPeriodical) => {
    if (legalPeriodical) {
      reference.value.legalPeriodical = legalPeriodical
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

watch(
  () => relatedDocumentationUnit.value,
  () => {
    createDocumentationUnitParameters.value = {
      documentationOffice:
        relatedDocumentationUnit.value.court?.responsibleDocOffice,
      documentType: relatedDocumentationUnit.value.documentType,
      decisionDate: relatedDocumentationUnit.value.decisionDate,
      fileNumber: relatedDocumentationUnit.value.fileNumber,
      court: relatedDocumentationUnit.value.court,
      reference: reference.value,
    }
  },
  { deep: true },
)

/** watches the changes of query related documentations params
 * resets the page if change took place.
 */
watch(searchResultsCurrentPage, () => {
  pageNumber.value = 0
})

onMounted(async () => {
  featureToggle.value = (
    await FeatureToggleService.isEnabled("neuris.new-from-search")
  ).data
})
</script>

<template>
  <div ref="containerRef" class="flex flex-col border-b-1">
    <PopupModal
      v-if="showModal"
      aria-label="Eintrag löschen"
      cancel-button-text="Nur Fundstelle löschen"
      cancel-button-type="tertiary"
      confirm-button-type="destructive"
      confirm-text="Dokumentationseinheit löschen"
      content-text="Die dazugehörige Dokumentationseinheit existiert noch. Soll sie gelöscht werden?"
      header-text="Dazugehörige Dokumentationseinheit löschen?"
      @close-modal="deleteReference"
      @confirm-action="deleteReferenceAndDocUnit"
    />
    <h2 v-if="!isSaved" class="ds-label-01-bold mb-16">
      Fundstelle hinzufügen
    </h2>
    <div class="flex flex-col gap-24">
      <DecisionSummary
        v-if="
          reference.documentationUnit &&
          reference.documentationUnit?.documentNumber
        "
        data-testid="reference-input-summary"
        :decision="reference.documentationUnit"
        :display-mode="DisplayMode.SIDEPANEL"
      />
      <div v-if="!isSaved" class="flex items-center gap-16">
        <div class="flex items-center">
          <InputField
            id="caselaw"
            class="flex items-center"
            label="Rechtsprechung"
            label-class="ds-body-01-reg"
            :label-position="LabelPosition.RIGHT"
          >
            <RadioInput
              v-model="reference.referenceType"
              aria-label="Rechtsprechung Fundstelle"
              name="referenceType"
              size="medium"
              value="caselaw"
              @click="validationStore.reset()"
            />
          </InputField>
        </div>

        <div class="flex items-center">
          <InputField
            id="literature"
            class="flex items-center"
            label="Literatur"
            label-class="ds-body-01-reg"
            :label-position="LabelPosition.RIGHT"
          >
            <RadioInput
              v-model="reference.referenceType"
              aria-label="Literatur Fundstelle"
              name="referenceType"
              size="medium"
              value="literature"
              @click="validationStore.reset()"
            />
          </InputField>
        </div>
      </div>
      <div class="flex justify-between gap-24">
        <div id="citationInputField" class="flex w-full flex-col">
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
          v-if="reference.referenceType === 'caselaw'"
          id="referenceSupplement"
          v-slot="slotProps"
          label="Klammernzusatz *"
          :validation-error="validationStore.getByField('referenceSupplement')"
        >
          <TextInput
            id="referenceSupplement"
            v-model="reference.referenceSupplement"
            aria-label="Klammernzusatz"
            :has-error="slotProps.hasError"
            size="medium"
            @blur="validateRequiredInput(reference)"
            @focus="validationStore.remove('referenceSupplement')"
          ></TextInput>
        </InputField>
        <InputField
          v-if="reference.referenceType === 'literature'"
          id="literatureReferenceDocumentType"
          v-slot="slotProps"
          label="Dokumenttyp *"
          :validation-error="validationStore.getByField('documentType')"
        >
          <ComboboxInput
            id="literatureReferenceDocumentType"
            v-model="reference.documentType"
            aria-label="Dokumenttyp Literaturfundstelle"
            :has-error="slotProps.hasError"
            :item-service="
              ComboboxItemService.getDependentLiteratureDocumentTypes
            "
            @focus="validationStore.remove('documentType')"
          ></ComboboxInput>
        </InputField>
      </div>
      <div
        v-if="reference.referenceType === 'literature'"
        class="w-[calc(50%-10px)]"
      >
        <InputField
          id="literatureReferenceAuthor"
          v-slot="slotProps"
          label="Autor *"
          :validation-error="validationStore.getByField('author')"
        >
          <TextInput
            id="literatureReferenceAuthor"
            v-model="reference.author"
            aria-label="Autor Literaturfundstelle"
            :has-error="slotProps.hasError"
            size="medium"
            @focus="validationStore.remove('author')"
          ></TextInput>
        </InputField>
      </div>

      <div v-if="!isSaved" id="documentationUnit">
        <h2 class="ds-label-01-bold mb-16">Entscheidung hinzufügen</h2>

        <div class="flex flex-col gap-24">
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
            <InputField id="decisionDocumentType" label="Dokumenttyp">
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
      </div>

      <div class="flex w-full flex-row justify-between">
        <div>
          <div class="flex gap-16">
            <TextButton
              v-if="!isSaved"
              aria-label="Nach Entscheidung suchen"
              button-type="primary"
              label="Suchen"
              size="small"
              @click="search"
            />
            <TextButton
              v-if="isSaved"
              aria-label="Fundstelle vermerken"
              button-type="tertiary"
              data-testid="previous-decision-save-button"
              :disabled="reference.isEmpty"
              label="Übernehmen"
              size="small"
              @click.stop="addReference(relatedDocumentationUnit)"
            />
            <TextButton
              v-if="isSaved"
              aria-label="Abbrechen"
              button-type="ghost"
              label="Abbrechen"
              size="small"
              @click.stop="emit('cancelEdit')"
            />
          </div>
        </div>
        <div v-if="isSaved">
          <TextButton
            v-if="
              reference?.documentationUnit?.status?.publicationStatus ===
                PublicationState.UNPUBLISHED &&
              reference?.getIsDocumentationUnitCreatedByReference()
            "
            aria-label="Eintrag löschen"
            button-type="destructive"
            label="Eintrag löschen"
            size="small"
            @click.stop="toggleDeletionConfirmationModal"
          />

          <TextButton
            v-else-if="
              reference.documentationUnit?.status?.publicationStatus ===
                PublicationState.EXTERNAL_HANDOVER_PENDING &&
              reference?.getIsDocumentationUnitCreatedByReference()
            "
            aria-label="Fundstelle und Dokumentationseinheit löschen"
            button-type="destructive"
            label="Fundstelle und Dokumentationseinheit löschen"
            size="small"
            @click.stop="deleteReferenceAndDocUnit"
          />
          <TextButton
            v-else
            aria-label="Eintrag löschen"
            button-type="destructive"
            label="Eintrag löschen"
            size="small"
            @click.stop="modelValue && emit('removeEntry', modelValue)"
          />
        </div>
      </div>
      <div v-if="isLoading || searchResults" class="bg-blue-200">
        <Pagination
          navigation-position="bottom"
          :page="searchResultsCurrentPage"
          @update-page="updatePage"
        >
          <SearchResultList
            allow-multiple-links
            :display-mode="DisplayMode.SIDEPANEL"
            :is-loading="isLoading"
            :search-results="searchResults"
            @link-decision="addReference"
          />
        </Pagination>
      </div>
      <CreateNewFromSearch
        v-if="searchResults && featureToggle"
        :parameters="createDocumentationUnitParameters"
        :validate-required-input="() => validateRequiredInput(reference)"
        @created-documentation-unit="addReferenceWithCreatedDocumentationUnit"
      />
    </div>
  </div>
</template>
@/stores/editionStore
