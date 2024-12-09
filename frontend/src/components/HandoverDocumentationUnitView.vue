<script lang="ts" setup>
import { ref, computed, onMounted } from "vue"
import { RouterLink } from "vue-router"
import ExpandableContent from "./ExpandableContent.vue"
import CodeSnippet from "@/components/CodeSnippet.vue"
import { InfoStatus } from "@/components/enumInfoStatus"
import InfoModal from "@/components/InfoModal.vue"
import TextButton from "@/components/input/TextButton.vue"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import PopupModal from "@/components/PopupModal.vue"
import ScheduledPublishingDateTime from "@/components/ScheduledPublishingDateTime.vue"
import TitleElement from "@/components/TitleElement.vue"
import { useFeatureToggle } from "@/composables/useFeatureToggle"
import ActiveCitation, { activeCitationLabels } from "@/domain/activeCitation"
import { longTextLabels, shortTextLabels } from "@/domain/documentUnit"
import EnsuingDecision, {
  ensuingDecisionFieldLabels,
} from "@/domain/ensuingDecision"
import EventRecord, {
  EventRecordType,
  HandoverMail,
  Preview,
} from "@/domain/eventRecord"
import PreviousDecision, {
  previousDecisionFieldLabels,
} from "@/domain/previousDecision"
import { fieldLabels } from "@/fields/caselaw"
import borderNumberService from "@/services/borderNumberService"
import handoverDocumentationUnitService from "@/services/handoverDocumentationUnitService"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import IconCheck from "~icons/ic/baseline-check"
import IconErrorOutline from "~icons/ic/baseline-error-outline"
import IconKeyboardArrowDown from "~icons/ic/baseline-keyboard-arrow-down"
import IconKeyboardArrowUp from "~icons/ic/baseline-keyboard-arrow-up"

const props = defineProps<{
  eventLog?: EventRecord[]
  errorMessage?: ResponseError
  succeedMessage?: { title: string; description: string }
}>()

const emits = defineEmits<{
  handoverDocument: []
}>()

const store = useDocumentUnitStore()

const categoriesRoute = computed(() => ({
  name: "caselaw-documentUnit-documentNumber-categories",
  params: { documentNumber: store.documentUnit!.documentNumber },
}))
const isFirstTimeHandover = computed(() => {
  return !props.eventLog || props.eventLog.length === 0
})

const preview = ref<Preview>()
const frontendError = ref()
const previewError = ref()
const errorMessage = computed(
  () => frontendError.value ?? previewError.value ?? props.errorMessage,
)

onMounted(async () => {
  // Save doc unit in case there are any unsaved local changes before fetching xml preview
  await store.updateDocumentUnit()

  await fetchPreview()
})

async function fetchPreview() {
  if (
    fieldsMissing.value ||
    isOutlineInvalid.value ||
    isCaseFactsInvalid.value ||
    isDecisionReasonsInvalid.value
  )
    return

  const previewResponse = await handoverDocumentationUnitService.getPreview(
    store.documentUnit!.uuid,
  )
  if (previewResponse.error) {
    previewError.value = previewResponse.error
  } else if (previewResponse.data?.xml) {
    preview.value = previewResponse.data
  }
}

function handoverDocumentUnit() {
  if (fieldsMissing.value) {
    frontendError.value = {
      title: "Es sind noch nicht alle Pflichtfelder befüllt.",
      description: "Die Dokumentationseinheit kann nicht übergeben werden.",
    }
  } else if (isOutlineInvalid.value) {
    frontendError.value = {
      title: "Gliederung und Sonstiger Orientierungssatz sind befüllt.",
      description: "Die Dokumentationseinheit kann nicht übergeben werden.",
    }
  } else if (isCaseFactsInvalid.value) {
    frontendError.value = {
      title: "Gründe und Tatbestand sind befüllt.",
      description: "Die Dokumentationseinheit kann nicht übergeben werden.",
    }
  } else if (isDecisionReasonsInvalid.value) {
    frontendError.value = {
      title: "Gründe und Entscheidungsgründe sind befüllt.",
      description: "Die Dokumentationseinheit kann nicht übergeben werden.",
    }
  } else if (!areBorderNumbersAndLinksValid.value) {
    // If there are invalid border numbers, you need to confirm a modal before handing over
    showHandoverModal.value = true
  } else {
    emits("handoverDocument")
  }
}

//Required Core Data fields
const missingCoreDataFields = ref(
  store.documentUnit!.missingRequiredFields.map((field) => fieldLabels[field]),
)

//Required Previous Decision fields
const missingPreviousDecisionFields = ref(
  store.documentUnit && store.documentUnit.previousDecisions
    ? store.documentUnit.previousDecisions
        .filter((previousDecision) => {
          return (
            getMissingPreviousDecisionFields(
              previousDecision as PreviousDecision,
            ).length > 0
          )
        })
        .map((previousDecision) => {
          return {
            identifier: previousDecision.renderDecision,
            missingFields: getMissingPreviousDecisionFields(
              previousDecision as PreviousDecision,
            ),
          }
        })
    : [],
)

function getMissingPreviousDecisionFields(previousDecision: PreviousDecision) {
  return previousDecision.missingRequiredFields.map(
    (field) => previousDecisionFieldLabels[field],
  )
}

//Required Ensuing Decision fields
const missingEnsuingDecisionFields = ref(
  store.documentUnit && store.documentUnit.ensuingDecisions
    ? store
        .documentUnit!.ensuingDecisions?.filter((ensuingDecision) => {
          return (
            getMissingEnsuingDecisionFields(ensuingDecision as EnsuingDecision)
              .length > 0
          )
        })
        .map((ensuingDecision) => {
          return {
            identifier: ensuingDecision.renderDecision,
            missingFields: getMissingEnsuingDecisionFields(
              ensuingDecision as EnsuingDecision,
            ),
          }
        })
    : [],
)

function getMissingEnsuingDecisionFields(ensuingDecision: EnsuingDecision) {
  return ensuingDecision.missingRequiredFields.map(
    (field) => ensuingDecisionFieldLabels[field],
  )
}

function getHeader(item: EventRecord) {
  switch (item.type) {
    case EventRecordType.HANDOVER_REPORT:
      return "Juris Protokoll - " + item.getDate()
    case EventRecordType.HANDOVER:
      return "Xml Email Abgabe - " + item.getDate()
    case EventRecordType.MIGRATION:
      return "Letzter Import/Delta Migration - " + item.getDate()
    default:
      return "Unbekanntes Ereignis - " + item.getDate()
  }
}

//Required Norms fields
const missingNormsFields = ref(
  store
    .documentUnit!.contentRelatedIndexing?.norms?.filter((normReference) => {
      return normReference.hasMissingFieldsInLegalForce
    })
    .map((normReference) => {
      return {
        identifier: normReference.renderDecision,
        missingFields: ["Gesetzeskraft"],
      }
    }),
)

const showHandoverModal = ref(false)
function confirmHandoverDialog() {
  emits("handoverDocument")
  showHandoverModal.value = false
}

const borderNumberValidationResult = ref(
  borderNumberService.validateBorderNumbers(),
)
const borderNumberLinkValidationResult = ref(
  borderNumberService.validateBorderNumberLinks(),
)
const areBorderNumbersAndLinksValid = computed<boolean>(
  () =>
    borderNumberValidationResult.value.isValid &&
    borderNumberLinkValidationResult.value.isValid,
)

// We want to display the action with a fake delay (recalculation only takes a couple of ms)
const showRecalculatingBorderNumbersFakeDelay = ref(false)

async function recalculateBorderNumbers() {
  showRecalculatingBorderNumbersFakeDelay.value = true
  borderNumberService.makeBorderNumbersSequential()

  borderNumberValidationResult.value =
    borderNumberService.validateBorderNumbers()
  borderNumberLinkValidationResult.value =
    borderNumberService.validateBorderNumberLinks()

  setTimeout(
    () => (showRecalculatingBorderNumbersFakeDelay.value = false),
    3_000,
  )
  // Preview is generated by the backend, so the updated border numebers need to be saved first.
  await store.updateDocumentUnit()
  await fetchPreview()
}

//Required Active Citation fields
const missingActiveCitationFields = ref(
  store.documentUnit &&
    store.documentUnit.contentRelatedIndexing &&
    store.documentUnit.contentRelatedIndexing.activeCitations
    ? store
        .documentUnit!.contentRelatedIndexing?.activeCitations?.filter(
          (activeCitation) => {
            return (
              getActiveCitationsFields(activeCitation as ActiveCitation)
                .length > 0
            )
          },
        )
        .map((activeCitation) => {
          return {
            identifier: activeCitation.renderDecision,
            missingFields: getActiveCitationsFields(
              activeCitation as ActiveCitation,
            ),
          }
        })
    : [],
)

function getActiveCitationsFields(activeCitation: ActiveCitation) {
  return activeCitation.missingRequiredFields.map(
    (field) => activeCitationLabels[field],
  )
}

const fieldsMissing = computed(() => {
  return (
    !!missingCoreDataFields.value.length ||
    !!missingPreviousDecisionFields.value?.length ||
    !!missingEnsuingDecisionFields.value?.length ||
    !!missingNormsFields.value?.length ||
    !!missingActiveCitationFields.value?.length
  )
})

const isOutlineInvalid = computed<boolean>(
  () =>
    // Outline is written into otherHeadnote in jdv -> Only one of the fields may be filled at the same time
    !!store.documentUnit?.longTexts.outline &&
    !!store.documentUnit.shortTexts.otherHeadnote,
)

const isCaseFactsInvalid = computed<boolean>(
  () =>
    !!store.documentUnit?.longTexts.reasons &&
    !!store.documentUnit?.longTexts.caseFacts,
)
const isDecisionReasonsInvalid = computed<boolean>(
  () =>
    !!store.documentUnit?.longTexts.reasons &&
    !!store.documentUnit?.longTexts.decisionReasons,
)

const schedulingFeatureToggle = useFeatureToggle("neuris.scheduledPublishing")

const isScheduled = computed<boolean>(
  () => !!store.documentUnit!.managementData.scheduledPublicationDateTime,
)

const isPublishable = computed<boolean>(
  () =>
    !isOutlineInvalid.value &&
    !fieldsMissing.value &&
    !isCaseFactsInvalid.value &&
    !isDecisionReasonsInvalid.value &&
    !!preview.value?.success,
)
</script>

<template>
  <div v-if="store.documentUnit">
    <div class="flex flex-col gap-24 bg-white p-24">
      <TitleElement>Übergabe an jDV</TitleElement>

      <div aria-label="Plausibilitätsprüfung" class="flex flex-col">
        <h2 class="ds-label-01-bold mb-16">Plausibilitätsprüfung</h2>

        <div
          v-if="
            fieldsMissing ||
            isOutlineInvalid ||
            isCaseFactsInvalid ||
            isDecisionReasonsInvalid
          "
        >
          <div class="flex flex-row gap-8">
            <IconErrorOutline class="text-red-800" />

            <div class="ds-body-01-reg flex flex-col">
              <div v-if="fieldsMissing" class="flex flex-col gap-24">
                <div>
                  <p>
                    Die folgenden Rubriken-Pflichtfelder sind nicht befüllt:
                  </p>
                  <ul class="list-disc">
                    <li v-for="field in missingCoreDataFields" :key="field">
                      {{ field }}
                    </li>
                    <li
                      v-if="
                        missingPreviousDecisionFields &&
                        missingPreviousDecisionFields.length > 0
                      "
                    >
                      Vorgehende Entscheidungen
                      <ul>
                        <li
                          v-for="fields in missingPreviousDecisionFields"
                          :key="missingPreviousDecisionFields.indexOf(fields)"
                          class="ds-body-01-reg ml-[1rem]"
                        >
                          <div v-if="fields && fields.missingFields.length > 0">
                            <span>{{ fields.identifier }}</span>
                            -
                            <span class="ds-label-02-bold">{{
                              fields.missingFields.join(", ")
                            }}</span>
                          </div>
                        </li>
                      </ul>
                    </li>
                    <li
                      v-if="
                        missingEnsuingDecisionFields &&
                        missingEnsuingDecisionFields.length > 0
                      "
                      class="ds-body-01-reg ml-[1rem]"
                    >
                      Nachgehende Entscheidungen
                      <ul>
                        <li
                          v-for="fields in missingEnsuingDecisionFields"
                          :key="missingEnsuingDecisionFields.indexOf(fields)"
                          class="ds-body-01-reg ml-[1rem]"
                        >
                          <div v-if="fields && fields.missingFields.length > 0">
                            <span>{{ fields.identifier }}</span>
                            -
                            <span class="ds-label-02-bold">{{
                              fields.missingFields.join(", ")
                            }}</span>
                          </div>
                        </li>
                      </ul>
                    </li>
                    <li
                      v-if="missingNormsFields && missingNormsFields.length > 0"
                      class="ds-body-01-reg ml-[1rem]"
                    >
                      Normen
                      <ul>
                        <li
                          v-for="fields in missingNormsFields"
                          :key="missingNormsFields.indexOf(fields)"
                          class="ds-body-01-reg ml-[1rem]"
                        >
                          <div v-if="fields && fields.missingFields.length > 0">
                            <span>{{ fields.identifier }}</span>
                            -
                            <span class="ds-label-02-bold">{{
                              fields.missingFields.join(", ")
                            }}</span>
                          </div>
                        </li>
                      </ul>
                    </li>
                    <li
                      v-if="
                        missingActiveCitationFields &&
                        missingActiveCitationFields.length > 0
                      "
                      class="ds-body-01-reg ml-[1rem]"
                    >
                      Aktivzitierung
                      <ul>
                        <li
                          v-for="fields in missingActiveCitationFields"
                          :key="missingActiveCitationFields.indexOf(fields)"
                          class="ds-body-01-reg ml-[1rem]"
                        >
                          <div v-if="fields && fields.missingFields.length > 0">
                            <span>{{ fields.identifier }}</span>
                            -
                            <span class="ds-label-02-bold">{{
                              fields.missingFields.join(", ")
                            }}</span>
                          </div>
                        </li>
                      </ul>
                    </li>
                  </ul>
                </div>
              </div>
              <div
                v-if="
                  isOutlineInvalid ||
                  isCaseFactsInvalid ||
                  isDecisionReasonsInvalid
                "
                class="mb-24 flex flex-col gap-8"
              >
                <div v-if="isOutlineInvalid">
                  Die Rubriken "Gliederung" und "Sonstiger Orientierungssatz"
                  sind befüllt.<br />
                  Es darf nur eine der beiden Rubriken befüllt sein.
                </div>
                <div v-if="isCaseFactsInvalid">
                  Die Rubriken "Gründe" und "Tatbestand" sind befüllt.<br />
                  Es darf nur eine der beiden Rubriken befüllt sein.
                </div>
                <div v-if="isDecisionReasonsInvalid">
                  Die Rubriken "Gründe" und "Entscheidungsgründe" sind
                  befüllt.<br />
                  Es darf nur eine der beiden Rubriken befüllt sein.
                </div>
              </div>
            </div>
          </div>
          <RouterLink class="inline-block" :to="categoriesRoute">
            <TextButton
              aria-label="Rubriken bearbeiten"
              button-type="tertiary"
              class="w-fit"
              label="Rubriken bearbeiten"
              size="small"
            />
          </RouterLink>
        </div>
        <div v-else class="flex flex-row gap-8">
          <IconCheck class="text-green-700" />
          <p>Alle Pflichtfelder sind korrekt ausgefüllt</p>
        </div>
      </div>
      <div aria-label="Randnummernprüfung" class="flex flex-col">
        <h2 class="ds-label-01-bold mb-16">Randnummernprüfung</h2>

        <div v-if="!areBorderNumbersAndLinksValid">
          <div class="flex flex-row gap-8">
            <IconErrorOutline class="text-red-800" />

            <div class="ds-body-01-reg flex flex-col gap-24">
              <div
                v-if="
                  !borderNumberValidationResult.isValid &&
                  !borderNumberValidationResult.hasError
                "
              >
                Die Reihenfolge der Randnummern ist nicht korrekt.
                <dl class="my-16">
                  <div class="grid grid-cols-3 gap-24 px-0">
                    <dt class="ds-label-02-bold self-center">Rubrik</dt>
                    <dd class="ds-body-02-reg">
                      {{
                        longTextLabels[
                          borderNumberValidationResult.invalidCategory
                        ]
                      }}
                    </dd>
                  </div>
                  <div class="grid grid-cols-3 gap-24 px-0">
                    <dt class="ds-label-02-bold self-center">
                      Erwartete Randnummer
                    </dt>
                    <dd class="ds-body-02-reg">
                      {{ borderNumberValidationResult.expectedBorderNumber }}
                    </dd>
                  </div>
                  <div class="grid grid-cols-3 gap-24 px-0">
                    <dt class="ds-label-02-bold self-center">
                      Tatsächliche Randnummer
                    </dt>
                    <dd class="ds-body-02-reg">
                      {{
                        borderNumberValidationResult.firstInvalidBorderNumber
                      }}
                    </dd>
                  </div>
                </dl>
              </div>
              <div v-if="!borderNumberLinkValidationResult.isValid">
                Es gibt ungültige Randnummern-Verweise in folgenden Rubriken:
                <ul class="list-disc">
                  <li
                    v-for="key in borderNumberLinkValidationResult.invalidCategories"
                    :key="key"
                  >
                    {{
                      shortTextLabels[key as keyof typeof shortTextLabels] ??
                      longTextLabels[key as keyof typeof longTextLabels]
                    }}
                  </li>
                </ul>
              </div>
              <div v-if="borderNumberValidationResult.hasError">
                Bei der Randnummernprüfung ist ein Fehler aufgetreten. Bitte
                prüfen Sie die Randnummern manuell und informieren Sie den
                Support.
              </div>
            </div>
          </div>
          <TextButton
            v-if="
              !borderNumberValidationResult.isValid &&
              !borderNumberValidationResult.hasError &&
              !showRecalculatingBorderNumbersFakeDelay
            "
            aria-label="Randnummern neu berechnen"
            button-type="tertiary"
            class="mt-8 w-fit"
            label="Randnummern neu berechnen"
            size="small"
            @click="recalculateBorderNumbers"
          />
        </div>
        <div
          v-else-if="!showRecalculatingBorderNumbersFakeDelay"
          class="flex flex-row gap-8"
        >
          <IconCheck class="text-green-700" />
          <p>Die Reihenfolge der Randnummern ist korrekt</p>
        </div>
        <div
          v-if="showRecalculatingBorderNumbersFakeDelay"
          class="flex flex-row gap-8"
        >
          <LoadingSpinner size="small" />
          <p>Die Randnummern werden neu berechnet</p>
        </div>
      </div>
      <div class="border-b-1 border-b-gray-400"></div>

      <ExpandableContent
        v-if="
          !fieldsMissing &&
          !isOutlineInvalid &&
          !isCaseFactsInvalid &&
          !isDecisionReasonsInvalid &&
          preview?.success &&
          !!preview?.xml
        "
        as-column
        class="border-b-1 border-gray-400 pb-24"
        :data-set="preview"
        header="XML Vorschau"
        header-class="font-bold"
        :is-expanded="false"
        title="XML Vorschau"
      >
        <CodeSnippet title="" :xml="preview.xml" />
      </ExpandableContent>
      <InfoModal
        v-if="errorMessage"
        aria-label="Fehler bei jDV Übergabe"
        class="mt-8"
        :description="errorMessage.description"
        :title="errorMessage.title"
      />
      <InfoModal
        v-else-if="succeedMessage"
        aria-label="Erfolg der jDV Übergabe"
        class="mt-8"
        v-bind="succeedMessage"
        :status="InfoStatus.SUCCEED"
      />
      <PopupModal
        v-if="showHandoverModal"
        aria-label="Bestätigung für Übergabe bei Fehlern"
        cancel-button-type="tertiary"
        confirm-button-type="primary"
        confirm-text="Trotzdem übergeben"
        content-text="Die Randnummern sind nicht korrekt. Wollen Sie das Dokument dennoch übergeben?"
        header-text="Warnung: Randnummern inkorrekt"
        @close-modal="showHandoverModal = false"
        @confirm-action="confirmHandoverDialog"
      />
      <TextButton
        aria-label="Dokumentationseinheit an jDV übergeben"
        button-type="primary"
        class="w-fit"
        :disabled="!isPublishable || (schedulingFeatureToggle && isScheduled)"
        :icon="IconCheck"
        label="Dokumentationseinheit an jDV übergeben"
        size="medium"
        @click="handoverDocumentUnit"
      />

      <ScheduledPublishingDateTime
        v-if="schedulingFeatureToggle"
        :is-publishable="isPublishable"
      />

      <div aria-label="Letzte Ereignisse">
        <h2 class="ds-label-01-bold mb-16">Letzte Ereignisse</h2>
        <div class="flex flex-col gap-24">
          <p v-if="isFirstTimeHandover">
            Diese Dokumentationseinheit wurde bisher nicht an die jDV übergeben
          </p>
          <div v-else class="flex flex-col gap-24">
            <div v-for="(item, index) in eventLog" :key="index">
              <ExpandableContent
                as-column
                :data-set="item"
                :header="getHeader(item)"
                :is-expanded="index == 0"
                :title="item.type"
              >
                <template #open-icon>
                  <IconKeyboardArrowDown />
                </template>

                <template #close-icon>
                  <IconKeyboardArrowUp />
                </template>

                <!-- eslint-disable vue/no-v-html -->
                <div
                  v-if="item.type == EventRecordType.HANDOVER_REPORT"
                  class="p-20"
                  v-html="item.getContent()"
                />
                <div
                  v-else-if="item instanceof HandoverMail"
                  class="flex flex-col gap-24 pt-24"
                >
                  <div class="ds-label-02-reg">
                    <div>
                      <span class="ds-label-02-bold">E-Mail an:</span>
                      {{ (item as HandoverMail).receiverAddress }}
                    </div>
                    <div>
                      <span class="ds-label-02-bold"> Betreff: </span>
                      {{ (item as HandoverMail).mailSubject }}
                    </div>
                  </div>

                  <CodeSnippet
                    v-if="(item as HandoverMail).attachments?.[0]"
                    title="XML"
                    :xml="(item as HandoverMail).attachments?.[0].fileContent!"
                  />
                </div>
                <div
                  v-else-if="item.type == EventRecordType.MIGRATION"
                  class="p-20"
                >
                  <CodeSnippet
                    v-if="item.getContent()"
                    title="XML"
                    :xml="item.getContent()"
                  />
                </div>
              </ExpandableContent>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
@/services/handoverDocumentationUnitService @/domain/eventRecord
