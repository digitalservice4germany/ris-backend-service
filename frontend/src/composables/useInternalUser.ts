import { storeToRefs } from "pinia"
import { computed, Ref } from "vue"
import useSessionStore from "@/stores/sessionStore"

export function useInternalUser(): Ref<boolean> {
  const { user } = storeToRefs(useSessionStore())
  return computed(() => user.value?.roles?.includes("Internal") ?? false)
}
