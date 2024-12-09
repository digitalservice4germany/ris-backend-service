import { ref } from "vue"
import featureToggleService from "@/services/featureToggleService"

export function useFeatureToggle(featureName: string) {
  const toggleState = ref(false)

  featureToggleService
    .isEnabled(featureName)
    .then((response) => (toggleState.value = response.data ?? false))
    .catch(() =>
      console.error(`Could not fetch feature toggle: ${featureName}`),
    )
  return toggleState
}
