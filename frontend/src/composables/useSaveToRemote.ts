import { ref } from "vue"
import { ServiceResponse, ResponseError } from "@/services/httpClient"

export function useSaveToRemote(
  saveCallback: () => Promise<ServiceResponse<void>>,
  autoSaveInterval = 0
) {
  const saveIsInProgress = ref(false)
  const lastSaveError = ref<ResponseError | undefined>(undefined)
  const lastSavedOn = ref<Date | undefined>(undefined)

  async function triggerSave(): Promise<void> {
    if (saveIsInProgress.value) {
      return
    }

    saveIsInProgress.value = true

    try {
      const response = await saveCallback()
      lastSaveError.value = response.error

      if (lastSaveError.value == undefined) {
        lastSavedOn.value = new Date()
        lastSaveError.value = undefined
      }
    } catch (error) {
      lastSaveError.value = { title: "Verbindung fehlgeschlagen" }
    } finally {
      saveIsInProgress.value = false
    }
  }
  const timer = setInterval(triggerSave, autoSaveInterval)

  return {
    saveIsInProgress,
    triggerSave,
    lastSaveError,
    lastSavedOn,
    timer,
  }
}
