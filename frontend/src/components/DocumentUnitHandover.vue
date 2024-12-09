<script lang="ts" setup>
import { onMounted, ref } from "vue"
import LoadingSpinner from "./LoadingSpinner.vue"
import HandoverDocumentationUnitView from "@/components/HandoverDocumentationUnitView.vue"
import EventRecord, { HandoverMail } from "@/domain/eventRecord"
import handoverDocumentationUnitService from "@/services/handoverDocumentationUnitService"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()

const loadDone = ref(false)
const eventLog = ref<EventRecord[]>()
const errorMessage = ref<ResponseError>()
const succeedMessage = ref<{ title: string; description: string }>()

async function handoverDocument() {
  const response = await handoverDocumentationUnitService.handoverDocument(
    store.documentUnit!.uuid,
  )
  await handoverDocumentationUnitService.publishDocument(
    store.documentUnit!.uuid,
  )
  if (!eventLog.value) eventLog.value = []
  if (response.data && response.data?.success) {
    const handover = new HandoverMail(response.data)
    handover.setContent(handover.getContent().replace(/[ \t]{2,}/g, ""))

    eventLog.value.unshift(handover)

    succeedMessage.value = {
      title: "Email wurde versendet",
      description: "",
    }
    if (store.documentUnit?.documentNumber)
      await store.loadDocumentUnit(store.documentUnit.documentNumber)
  } else {
    errorMessage.value = response.error
  }
}

onMounted(async () => {
  const response = await handoverDocumentationUnitService.getEventLog(
    store.documentUnit!.uuid,
  )
  if (!response.error && response.data) {
    eventLog.value = response.data
  } else {
    errorMessage.value = response.error
  }

  loadDone.value = true
})
</script>

<template>
  <div class="w-full grow p-24">
    <HandoverDocumentationUnitView
      v-if="loadDone"
      :error-message="errorMessage"
      :event-log="eventLog"
      :succeed-message="succeedMessage"
      @handover-document="handoverDocument"
    />

    <div v-else class="my-112 grid justify-items-center bg-white bg-opacity-60">
      <h2>Überprüfung der Daten ...</h2>
      <LoadingSpinner />
    </div>
  </div>
</template>

@/services/handoverDocumentationUnitService @/domain/eventRecord
