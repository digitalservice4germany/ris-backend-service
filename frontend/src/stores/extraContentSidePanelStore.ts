import { defineStore } from "pinia"
import { ref } from "vue"
import { useRoute } from "vue-router"
import useQuery from "@/composables/useQueryFromRoute"
import { SelectablePanelContent } from "@/types/panelContentMode"

export const useExtraContentSidePanelStore = defineStore(
  "extraSidePanelStore",
  () => {
    const panelMode = ref<SelectablePanelContent | undefined>(undefined)
    const isExpanded = ref<boolean>(false)
    const currentAttachmentIndex = ref(0)

    const { pushQueryToRoute } = useQuery()
    const route = useRoute()

    function setSidePanelMode(mode: SelectablePanelContent) {
      panelMode.value = mode
    }

    /**
     * Expands or collapses the panel.
     * Can be forced by passing a boolean parameter. Otherwise, it will collapse when expanded and expand when collapsed.
     * Pushes the state to the route as a query parameter.
     * @param expand optional boolean to enforce expanding or collapsing
     * @param mode
     */
    function togglePanel(
      expand?: boolean,
      mode?: SelectablePanelContent,
    ): boolean {
      isExpanded.value = expand === undefined ? !isExpanded.value : expand
      pushQueryToRoute({
        ...route.query,
        showAttachmentPanel: isExpanded.value.toString(),
      })
      if (mode) setSidePanelMode(mode)
      return isExpanded.value
    }

    /**
     * Sets the panel content to "attachments", so that the attachment view is displayed in the panel.
     * If a selected attachment index is provided, the local attachment index reference is updated accordingly,
     * so that the selected attachment is displayed in the attachment view.
     * @param selectedIndex (optional) selected attachment index
     */
    function selectAttachments(selectedIndex?: number) {
      if (selectedIndex !== undefined) {
        currentAttachmentIndex.value = selectedIndex
      }
      setSidePanelMode("attachments")
    }

    /**
     * Adjusts the local attachment index reference if necessary.
     * If all attachments have been deleted, switches to display the note instead.
     * @param index the deleted attachment index
     * @param attachmentsSize
     */
    function onAttachmentDeleted(index: number, attachmentsSize: number) {
      if (currentAttachmentIndex.value >= index) {
        currentAttachmentIndex.value = attachmentsSize - 1
      }
      if (attachmentsSize === 0) {
        setSidePanelMode("note")
      }
    }

    return {
      setSidePanelMode,
      selectAttachments,
      togglePanel,
      onAttachmentDeleted,
      isExpanded,
      currentAttachmentIndex,
      panelMode,
    }
  },
)
