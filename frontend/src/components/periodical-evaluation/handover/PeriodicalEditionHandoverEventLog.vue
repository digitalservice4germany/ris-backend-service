<script lang="ts" setup>
import { computed } from "vue"
import CodeSnippet from "@/components/CodeSnippet.vue"
import ExpandableContent from "@/components/ExpandableContent.vue"
import InfoModal from "@/components/InfoModal.vue"
import EventRecord, {
  EventRecordType,
  HandoverMail,
} from "@/domain/eventRecord"
import { ResponseError } from "@/services/httpClient"
import IconKeyboardArrowDown from "~icons/ic/baseline-keyboard-arrow-down"
import IconKeyboardArrowUp from "~icons/ic/baseline-keyboard-arrow-up"

const props = defineProps<{
  eventLog?: EventRecord[]
  eventLogError?: ResponseError
}>()

const isFirstTimeHandover = computed(() => {
  return !props.eventLog || props.eventLog.length === 0
})

function getHeader(item: EventRecord) {
  switch (item.type) {
    case EventRecordType.HANDOVER_REPORT:
      return "Juris Protokoll - " + item.getDate()
    case EventRecordType.HANDOVER:
      return "Xml Email Abgabe - " + item.getDate()
    default:
      return "Unbekanntes Ereignis - " + item.getDate()
  }
}
</script>

<template>
  <div aria-label="Letzte Ereignisse">
    <h2 class="ds-label-01-bold mb-16">Letzte Ereignisse</h2>
    <InfoModal
      v-if="eventLogError"
      aria-label="Fehler beim Laden des Event Logs"
      class="mt-8"
      :description="eventLogError.description"
      :title="eventLogError.title"
    />
    <div v-else class="flex flex-col gap-24">
      <p v-if="isFirstTimeHandover">
        Diese Ausgabe wurde bisher nicht an die jDV übergeben
      </p>
      <div v-else class="flex flex-col gap-24">
        <div v-for="(item, index) in eventLog" :key="index">
          <ExpandableContent
            as-column
            class="border-b-1 border-gray-400 bg-white p-10"
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
              v-html="item.getContent()"
            />
            <div v-else-if="item.type == EventRecordType.HANDOVER">
              <div class="ds-label-section pt-20 text-gray-900">ÜBER</div>
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
              <div class="ds-label-section text-gray-900">ALS</div>
              <CodeSnippet
                v-for="(attachment, attachmentIndex) in (item as HandoverMail)
                  .attachments"
                :key="attachmentIndex"
                class="mb-16"
                :title="attachment.fileName!"
                :xml="attachment.fileContent!"
              />
            </div>
          </ExpandableContent>
        </div>
      </div>
    </div>
  </div>
</template>
