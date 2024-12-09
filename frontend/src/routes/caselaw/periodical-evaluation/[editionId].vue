<script lang="ts" setup>
import { useInterval } from "@vueuse/core"
import { storeToRefs } from "pinia"
import { computed, onBeforeUnmount, onMounted, Ref, ref, watch } from "vue"
import { useRoute } from "vue-router"
import ExtraContentSidePanel from "@/components/ExtraContentSidePanel.vue"
import NavbarSide from "@/components/NavbarSide.vue"
import ErrorPage from "@/components/PageError.vue"
import PeriodicalEditionInfoPanel from "@/components/periodical-evaluation/PeriodicalEditionInfoPanel.vue"
import { usePeriodicalEvaluationMenuItems } from "@/composables/usePeriodicalEvaluationMenuItems"
import DocumentUnit from "@/domain/documentUnit"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import { useEditionStore } from "@/stores/editionStore"
import { useExtraContentSidePanelStore } from "@/stores/extraContentSidePanelStore"
import StringsUtil from "@/utils/stringsUtil"

const store = useEditionStore()
const documentUnitStore = useDocumentUnitStore()
const extraContentSidePanelStore = useExtraContentSidePanelStore()

const {
  counter: loadDocumentUnitTimer,
  pause,
  resume,
} = useInterval(10_000, {
  controls: true,
})

const responseError = ref<ResponseError>()
const route = useRoute()

const { documentUnit } = storeToRefs(documentUnitStore) as {
  documentUnit: Ref<DocumentUnit | undefined>
}
const infoSubtitle = computed(() =>
  StringsUtil.mergeNonBlankStrings(
    [store.edition?.legalPeriodical?.abbreviation, store.edition?.name],
    " ",
  ),
)

const menuItems = usePeriodicalEvaluationMenuItems(
  store.edition?.id,
  route.query,
)

const handleKeyDown = (event: KeyboardEvent) => {
  // List of tag names where shortcuts should be disabled
  const tagName = (event.target as HTMLElement).tagName.toLowerCase()

  // Check if the active element is an input, textarea, or any element with contenteditable
  if (
    ["input", "textarea", "select"].includes(tagName) ||
    (event.target as HTMLElement).isContentEditable
  ) {
    if (event.key === "Escape") (event.target as HTMLElement).blur() // Remove focus from the input field
    return // Do nothing if the user is typing in an input field or editable area
  }

  switch (event.key) {
    case "v": // Ctrl + V
      extraContentSidePanelStore.togglePanel(true)
      extraContentSidePanelStore.setSidePanelMode("preview")
      break
    default:
      break
  }
}

/**
 * Resumes the loading document unit timer if it is expanded on extra content side panel
 * @param expanded
 */
function handleSidePanelIsExpanded(expanded: boolean) {
  if (expanded) {
    resume()
  } else {
    pause()
  }
}

/**
 * To make sure the latest version is displayed,
 * when timer is resumed it will reload the documentation unit if in store.
 */
watch(loadDocumentUnitTimer, async () => {
  if (documentUnit.value?.documentNumber) {
    await documentUnitStore.loadDocumentUnit(documentUnit.value?.documentNumber)
  }
})

onBeforeUnmount(() => {
  // Remove the event listener when the component is unmounted
  window.removeEventListener("keydown", handleKeyDown)
})

onMounted(async () => {
  window.addEventListener("keydown", handleKeyDown)

  const response = await store.loadEdition()
  if (response.error) {
    responseError.value = response.error
  }
})
</script>

<template>
  <div class="flex w-screen grow">
    <div
      class="sticky top-0 z-50 flex flex-col border-r-1 border-solid border-gray-400 bg-white"
    >
      <NavbarSide :is-child="false" :menu-items="menuItems" :route="route" />
    </div>

    <div class="flex w-full flex-col bg-gray-100">
      <PeriodicalEditionInfoPanel :subtitle="infoSubtitle" />
      <ErrorPage
        v-if="responseError"
        :error="responseError"
        :title="responseError?.title"
      />
      <div v-else class="flex grow flex-row items-start">
        <router-view class="flex-1" />
        <ExtraContentSidePanel
          v-if="documentUnit && route.path.includes('references')"
          :document-unit="documentUnit"
          :enabled-panels="['preview']"
          hide-panel-mode-bar
          show-edit-button
          @side-panel-is-expanded="handleSidePanelIsExpanded"
        />
      </div>
    </div>
  </div>
</template>
