<script lang="ts" setup>
import { useHead } from "@unhead/vue"
import { storeToRefs } from "pinia"
import { onMounted, ref, Ref } from "vue"
import { useRoute } from "vue-router"
import DocumentUnitInfoPanel from "@/components/DocumentUnitInfoPanel.vue"
import ExtraContentSidePanel from "@/components/ExtraContentSidePanel.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import { ValidationError } from "@/components/input/types"
import NavbarSide from "@/components/NavbarSide.vue"
import ErrorPage from "@/components/PageError.vue"
import SideToggle from "@/components/SideToggle.vue"
import { useCaseLawMenuItems } from "@/composables/useCaseLawMenuItems"
import useQuery from "@/composables/useQueryFromRoute"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const props = defineProps<{
  documentNumber: string
}>()

useHead({
  title: props.documentNumber + " · NeuRIS Rechtsinformationssystem",
})

const store = useDocumentUnitStore()

const route = useRoute()
const menuItems = useCaseLawMenuItems(props.documentNumber, route.query)
const { pushQueryToRoute } = useQuery()

const { documentUnit } = storeToRefs(store)

const validationErrors = ref<ValidationError[]>([])
const showNavigationPanelRef: Ref<boolean> = ref(
  route.query.showNavigationPanel !== "false",
)

const responseError = ref<ResponseError>()

const extraContentSidePanel = ref<InstanceType<
  typeof ExtraContentSidePanel
> | null>(null)

const toggleNavigationPanel = () => {
  showNavigationPanelRef.value = !showNavigationPanelRef.value
  pushQueryToRoute({
    ...route.query,
    showNavigationPanel: showNavigationPanelRef.value.toString(),
  })
}

async function requestDocumentUnitFromServer() {
  const response = await store.loadDocumentUnit(props.documentNumber)

  if (!response.data) {
    responseError.value = response.error
  }
}

async function attachmentIndexSelected(index: number) {
  extraContentSidePanel.value?.togglePanel(true)
  extraContentSidePanel.value?.selectAttachments(index)
}

async function attachmentIndexDeleted(index: number) {
  await requestDocumentUnitFromServer()
  extraContentSidePanel.value?.onAttachmentDeleted(index)
}

async function attachmentsUploaded(anySuccessful: boolean) {
  if (anySuccessful) {
    await requestDocumentUnitFromServer()
    extraContentSidePanel.value?.togglePanel(true)
    extraContentSidePanel.value?.selectAttachments(
      documentUnit.value ? documentUnit.value.attachments.length - 1 : 0,
    )
  }
}

onMounted(async () => {
  await requestDocumentUnitFromServer()
})
</script>

<template>
  <div class="flex w-screen grow">
    <div
      v-if="!route.path.includes('preview') && documentUnit"
      class="sticky top-0 z-50 flex flex-col border-r-1 border-solid border-gray-400 bg-white"
    >
      <SideToggle
        class="sticky top-0 z-20"
        data-testid="side-toggle-navigation"
        :is-expanded="showNavigationPanelRef"
        label="Navigation"
        tabindex="0"
        test-id="side-toggle-navigation"
        @update:is-expanded="toggleNavigationPanel"
      >
        <NavbarSide :is-child="false" :menu-items="menuItems" :route="route" />
      </SideToggle>
    </div>
    <div v-if="documentUnit" class="flex w-full min-w-0 flex-col bg-gray-100">
      <DocumentUnitInfoPanel
        v-if="documentUnit && !route.path.includes('preview')"
        data-testid="document-unit-info-panel"
        :document-unit="documentUnit"
        :heading="documentUnit?.documentNumber ?? ''"
      />
      <div class="flex grow flex-col items-start">
        <FlexContainer
          v-if="documentUnit"
          class="h-full w-full flex-grow"
          :class="
            route.path.includes('preview')
              ? 'flex-row bg-white'
              : 'flex-row-reverse'
          "
        >
          <ExtraContentSidePanel
            v-if="
              !(
                route.path.includes('handover') ||
                route.path.includes('preview')
              )
            "
            ref="extraContentSidePanel"
          ></ExtraContentSidePanel>
          <router-view
            :validation-errors="validationErrors"
            @attachment-index-deleted="attachmentIndexDeleted"
            @attachment-index-selected="attachmentIndexSelected"
            @attachments-uploaded="attachmentsUploaded"
          />
        </FlexContainer>
      </div>
    </div>
    <ErrorPage
      v-if="responseError"
      :error="responseError"
      :title="responseError?.title"
    />
  </div>
</template>
