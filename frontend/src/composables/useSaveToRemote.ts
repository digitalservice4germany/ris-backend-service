import { computed, onUnmounted, ref } from "vue"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

function getCurrentTime(dateSaved: Date) {
  const fullHour = ("0" + dateSaved.getHours()).slice(-2)
  const fullMinute = ("0" + dateSaved.getMinutes()).slice(-2)
  return `${fullHour}:${fullMinute}`
}

export function useSaveToRemote(autoSaveInterval = 0) {
  const store = useDocumentUnitStore()
  const saveIsInProgress = ref(false)
  const lastSaveError = ref<ResponseError | undefined>(undefined)
  const lastSavedOn = ref<Date | undefined>(undefined)

  const formattedLastSavedOn = computed(
    () => lastSavedOn.value && getCurrentTime(lastSavedOn.value),
  )

  async function triggerSave(): Promise<void> {
    if (saveIsInProgress.value) return

    saveIsInProgress.value = true

    try {
      const response = await store.updateDocumentUnit()

      if (response.status != 304) {
        lastSaveError.value = response.error
      }

      if (lastSaveError.value == undefined) {
        lastSavedOn.value = new Date()
      }
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
    } catch (error) {
      lastSaveError.value = { title: "Verbindung fehlgeschlagen" }
    } finally {
      saveIsInProgress.value = false
    }
  }
  const timer = setInterval(triggerSave, autoSaveInterval)

  onUnmounted(() => {
    clearInterval(timer)
  })

  return {
    saveIsInProgress,
    triggerSave,
    lastSaveError,
    formattedLastSavedOn,
  }
}
