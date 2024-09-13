<script lang="ts" setup>
import { ref, computed, onMounted } from "vue"
import { RouterLink } from "vue-router"
import ExpandableContent from "./ExpandableContent.vue"
import CodeSnippet from "@/components/CodeSnippet.vue"
import { InfoStatus } from "@/components/enumInfoStatus"
import InfoModal from "@/components/InfoModal.vue"
import TextButton from "@/components/input/TextButton.vue"
import ActiveCitation, { activeCitationLabels } from "@/domain/activeCitation"
import EnsuingDecision, {
  ensuingDecisionFieldLabels,
} from "@/domain/ensuingDecision"
import EventRecord, { EventRecordType } from "@/domain/eventRecord"
import PreviousDecision, {
  previousDecisionFieldLabels,
} from "@/domain/previousDecision"
import { fieldLabels } from "@/fields/caselaw"
import handoverService from "@/services/handoverService"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import IconCheck from "~icons/ic/baseline-check"
import IconErrorOutline from "~icons/ic/baseline-error-outline"
import IconKeyboardArrowDown from "~icons/ic/baseline-keyboard-arrow-down"
import IconKeyboardArrowUp from "~icons/ic/baseline-keyboard-arrow-up"
import IconHandover from "~icons/ic/outline-campaign"

const props = defineProps<{
  handoverResult?: EventRecord
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

const preview = ref<EventRecord>()
const frontendError = ref()
const previewError = ref()
const errorMessage = computed(
  () => frontendError.value ?? previewError.value ?? props.errorMessage,
)

onMounted(async () => {
  if (fieldsMissing.value || isOutlineInvalid.value) return
  const previewResponse = await handoverService.getPreview(
    store.documentUnit!.uuid,
  )
  if (previewResponse.error) {
    previewError.value = previewResponse.error
  } else if (previewResponse.data?.xml) {
    preview.value = previewResponse.data
  }
})

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
      return "Juris Protokoll - " + item.date
    case EventRecordType.HANDOVER:
      return "Xml Email Abgabe - " + item.date
    case EventRecordType.MIGRATION:
      return "Letzter Import/Delta Migration - " + item.date
    default:
      return "Unbekanntes Ereignis - " + item.date
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

const isOutlineInvalid = computed(
  () =>
    // Outline is written into otherHeadnote in jdv -> Only one of the fields may be filled at the same time
    store.documentUnit?.texts.outline && store.documentUnit.texts.otherHeadnote,
)
</script>

<template>
  <div
    v-if="store.documentUnit"
    class="flex-start flex max-w-[80rem] flex-col justify-start gap-40"
  >
    <h1 class="ds-heading-02-reg">Übergabe an jDV</h1>
    <div aria-label="Plausibilitätsprüfung" class="flex flex-row gap-16">
      <div class="w-[15.625rem]">
        <p class="ds-subhead">Plausibilitätsprüfung</p>
      </div>
      <div v-if="fieldsMissing || isOutlineInvalid" class="flex flex-row gap-8">
        <div>
          <IconErrorOutline class="text-red-800" />
        </div>
        <div class="flex flex-col gap-32">
          <div v-if="fieldsMissing" class="flex flex-col gap-32">
            <div>
              <p class="ds-body-01-reg">
                Die folgenden Rubriken-Pflichtfelder sind nicht befüllt:
              </p>
              <ul class="list-disc">
                <li
                  v-for="field in missingCoreDataFields"
                  :key="field"
                  class="ds-body-01-reg ml-[1rem] list-item"
                >
                  {{ field }}
                </li>
                <li
                  v-if="
                    missingPreviousDecisionFields &&
                    missingPreviousDecisionFields.length > 0
                  "
                  class="ds-body-01-reg ml-[1rem] list-item"
                >
                  Vorgehende Entscheidungen
                  <ul>
                    <li
                      v-for="fields in missingPreviousDecisionFields"
                      :key="missingPreviousDecisionFields.indexOf(fields)"
                      class="ds-body-01-reg ml-[1rem] list-item"
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
                  class="ds-body-01-reg ml-[1rem] list-item"
                >
                  Nachgehende Entscheidungen
                  <ul>
                    <li
                      v-for="fields in missingEnsuingDecisionFields"
                      :key="missingEnsuingDecisionFields.indexOf(fields)"
                      class="ds-body-01-reg ml-[1rem] list-item"
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
                  class="ds-body-01-reg ml-[1rem] list-item"
                >
                  Normen
                  <ul>
                    <li
                      v-for="fields in missingNormsFields"
                      :key="missingNormsFields.indexOf(fields)"
                      class="ds-body-01-reg ml-[1rem] list-item"
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
                  class="ds-body-01-reg ml-[1rem] list-item"
                >
                  Aktivzitierung
                  <ul>
                    <li
                      v-for="fields in missingActiveCitationFields"
                      :key="missingActiveCitationFields.indexOf(fields)"
                      class="ds-body-01-reg ml-[1rem] list-item"
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
          <div v-if="isOutlineInvalid">
            Die Rubriken "Gliederung" und "Sonstiger Orientierungssatz" sind
            befüllt. Es darf nur eine der beiden Rubriken befüllt sein.
          </div>
          <RouterLink :to="categoriesRoute">
            <TextButton
              aria-label="Rubriken bearbeiten"
              button-type="tertiary"
              class="w-fit"
              label="Rubriken bearbeiten"
            />
          </RouterLink>
        </div>
      </div>
      <div v-else class="flex flex-row gap-8">
        <IconCheck class="text-green-700" />
        <p class="ds-body-01-reg">Alle Pflichtfelder sind korrekt ausgefüllt</p>
      </div>
    </div>
    <div class="border-b-1 border-b-gray-400"></div>

    <ExpandableContent
      v-if="
        !fieldsMissing &&
        !isOutlineInvalid &&
        preview?.success &&
        !!preview?.xml
      "
      as-column
      class="border-b-1 border-r-1 border-gray-400 bg-white p-10"
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
      v-if="succeedMessage"
      aria-label="Erfolg der jDV Übergabe"
      class="mt-8"
      v-bind="succeedMessage"
      :status="InfoStatus.SUCCEED"
    />
    <TextButton
      aria-label="Dokumentationseinheit an jDV übergeben"
      button-type="secondary"
      class="w-fit"
      :icon="IconHandover"
      label="Dokumentationseinheit an jDV übergeben"
      @click="handoverDocumentUnit"
    />
    <div aria-label="Letzte Ereignisse" class="flex flex-col gap-24">
      <h2 class="ds-heading-03-reg">Letzte Ereignisse</h2>
      <p v-if="isFirstTimeHandover">
        Diese Dokumentationseinheit wurde bisher nicht an die jDV übergeben
      </p>
      <div v-else class="flex flex-col gap-24">
        <div v-for="(item, index) in eventLog" :key="index">
          <ExpandableContent
            as-column
            class="border-b-1 border-r-1 border-gray-400 bg-white p-10"
            :data-set="item"
            :header="getHeader(item)"
            header-class="font-bold"
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
              v-html="item.content"
            />
            <div v-else-if="item.type == EventRecordType.HANDOVER">
              <div class="ds-label-section pt-20 text-gray-900">ÜBER</div>
              <div class="ds-label-02-reg">
                <div>
                  <span class="ds-label-02-bold">E-Mail an:</span>
                  {{ item.receiverAddress }}
                </div>
                <div>
                  <span class="ds-label-02-bold"> Betreff: </span>
                  {{ item.mailSubject }}
                </div>
              </div>
              <div class="ds-label-section text-gray-900">ALS</div>
              <CodeSnippet
                v-if="item.attachments?.[0]"
                title="XML"
                :xml="item.attachments?.[0].fileContent!"
              />
            </div>
            <div
              v-else-if="item.type == EventRecordType.MIGRATION"
              class="p-20"
            >
              <CodeSnippet v-if="!!item?.xml" title="XML" :xml="item.xml" />
            </div>
          </ExpandableContent>
        </div>
      </div>
    </div>
  </div>
</template>
@/services/handoverService @/domain/eventRecord
