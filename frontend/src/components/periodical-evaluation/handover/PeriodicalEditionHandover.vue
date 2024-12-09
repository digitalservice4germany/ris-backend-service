<script lang="ts" setup>
import { storeToRefs } from "pinia"
import { watch, ref, computed } from "vue"
import PeriodicalEditionHandoverEventLog from "./PeriodicalEditionHandoverEventLog.vue"
import PeriodicalEditionHandoverPreview from "./PeriodicalEditionHandoverPreview.vue"
import { InfoStatus } from "@/components/enumInfoStatus"
import InfoModal from "@/components/InfoModal.vue"
import TextButton from "@/components/input/TextButton.vue"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import TitleElement from "@/components/TitleElement.vue"
import EventRecord, { HandoverMail, Preview } from "@/domain/eventRecord"
import FeatureToggleService from "@/services/featureToggleService"
import HandoverEditionService from "@/services/handoverEditionService"
import { ResponseError } from "@/services/httpClient"
import { useEditionStore } from "@/stores/editionStore"
import IconCheck from "~icons/ic/baseline-check"
import IconErrorOutline from "~icons/ic/baseline-error-outline"
import IconHandover from "~icons/ic/outline-campaign"

const store = useEditionStore()
const { edition } = storeToRefs(store)

const eventLog = ref<EventRecord[]>()
const eventLogError = ref<ResponseError>()
const handoverError = ref<ResponseError>()
const handoverSucceedMessage = ref()
const preview = ref<Preview[]>()
const previewError = ref<ResponseError>()
const numberOfReferences = computed(() => {
  return store.edition?.references?.length
})
const featureToggle = ref()

async function handoverEdition() {
  if (!edition.value) {
    return
  }
  const response = await HandoverEditionService.handoverEdition(
    edition.value.id,
  )
  if (response.error) {
    handoverError.value = response.error
  } else if (response.data?.success) {
    if (!eventLog.value) eventLog.value = []
    const handover = new HandoverMail(response.data)
    handover.setContent(handover.getContent().replace(/[ \t]{2,}/g, ""))

    eventLog.value.unshift(handover)

    handoverSucceedMessage.value = {
      title: "Email wurde versendet",
    }
  }
}

async function getEventLog() {
  if (!edition.value) {
    return
  }
  const response = await HandoverEditionService.getEventLog(edition.value.id)
  if (response.error) {
    eventLogError.value = response.error
  } else if (response.data) {
    eventLog.value = response.data
  }
}

async function getPreview() {
  if (!edition.value) {
    return
  }
  const previewResponse = await HandoverEditionService.getPreview(
    edition.value.id,
  )
  if (previewResponse.error) {
    previewError.value = previewResponse.error
  } else if (previewResponse.data) {
    preview.value = previewResponse.data
  }
}

watch(
  edition,
  async () => {
    featureToggle.value = (
      await FeatureToggleService.isEnabled("neuris.evaluation-handover")
    ).data
    await getEventLog()
    await getPreview()
  },
  { immediate: true },
)
</script>

<template>
  <div class="w-full grow p-24">
    <div v-if="edition" class="flex flex-col gap-24 bg-white p-24">
      <TitleElement data-testid="handover-title">Übergabe an jDV</TitleElement>
      <div aria-label="Datenprüfung" class="flex flex-row">
        <div
          v-if="!numberOfReferences"
          class="flex flex-row items-center gap-8"
        >
          <IconErrorOutline class="text-red-800" />
          <p class="flexds-body-01-reg">
            Es wurden noch keine Fundstellen hinzugefügt
          </p>
        </div>
        <div v-else class="flex flex-row items-center gap-8">
          <IconCheck class="text-green-700" />
          <p class="ds-body-01-reg">
            Die Ausgabe enthält {{ numberOfReferences }} Fundstellen
          </p>
        </div>
      </div>
      <div class="border-b-1 border-b-gray-400"></div>

      <!-- Preview -->
      <PeriodicalEditionHandoverPreview
        :preview="preview"
        :preview-error="previewError"
      />

      <!-- Handover -->
      <InfoModal
        v-if="handoverError"
        aria-label="Fehler bei jDV Übergabe"
        class="mt-8"
        v-bind="handoverError"
      />
      <InfoModal
        v-else-if="handoverSucceedMessage"
        aria-label="Erfolg der jDV Übergabe"
        class="mt-8"
        v-bind="handoverSucceedMessage"
        :status="InfoStatus.SUCCEED"
      />
      <TextButton
        v-if="featureToggle"
        aria-label="Fundstellen der Ausgabe an jDV übergeben"
        button-type="secondary"
        class="w-fit"
        :disabled="!numberOfReferences"
        :icon="IconHandover"
        label="Fundstellen der Ausgabe an jDV übergeben"
        @click="handoverEdition"
      />

      <!-- Event Log -->
      <PeriodicalEditionHandoverEventLog
        :event-log="eventLog"
        :event-log-error="eventLogError"
      />
    </div>

    <div v-else class="my-112 grid justify-items-center bg-white bg-opacity-60">
      <h2>Ausgabe wird geladen ...</h2>
      <LoadingSpinner />
    </div>
  </div>
</template>

@/services/handoverEditionService @/domain/eventRecord
