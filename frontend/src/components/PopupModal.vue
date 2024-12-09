<script lang="ts" setup>
import { onMounted } from "vue"
import TextButton from "./input/TextButton.vue"

defineProps<{
  ariaLabel?: string
  headerText?: string
  contentText: string
  confirmText: string
  cancelButtonType?: string
  cancelButtonText?: string
  confirmButtonType?: string
}>()

defineEmits<{
  closeModal: []
  confirmAction: []
}>()

onMounted(() => {
  const popupModalElem = document.getElementsByClassName(
    "popup-modal-wrapper",
  )[0] as HTMLElement
  popupModalElem.focus()
  const focusableElemsSelector = ".modal-buttons-container button"
  const focusableElems = document.querySelectorAll(focusableElemsSelector)
  /** Modal focus trap event */
  document.addEventListener("keydown", (e) => {
    const isTabPressed = e.key === "Tab"
    if (isTabPressed) {
      if (e.shiftKey) {
        /** Shift + Tab select popupModal */
        if (document.activeElement === popupModalElem) {
          ;(focusableElems[focusableElems.length - 1] as HTMLElement).focus()
          e.preventDefault()
        }
      } else if (
        /** Jump to first button */
        document.activeElement ===
        (focusableElems[focusableElems.length - 1] as HTMLElement)
      ) {
        ;(focusableElems[0] as HTMLElement).focus()
        e.preventDefault()
      }
    }
  })
})
</script>

<template>
  <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
  <div
    :aria-label="ariaLabel"
    class="popup-modal-wrapper fixed left-0 top-0 z-50 flex h-full w-screen items-center justify-center bg-background"
    role="dialog"
    tabindex="0"
    @click.self="$emit('closeModal')"
    @keydown.esc="$emit('closeModal')"
  >
    <div
      class="modal-container box-border flex flex-col items-start gap-[1rem] rounded-sm bg-white px-[1.5rem] py-[1.5rem]"
    >
      <div class="ds-label-01-bold text-black">
        {{ headerText }}
      </div>
      <div class="ds-label-03-reg text-black">{{ contentText }}</div>
      <div
        class="modal-buttons-container flex w-full flex-row justify-end gap-[1rem]"
      >
        <TextButton
          :aria-label="confirmText"
          :button-type="confirmButtonType"
          :label="confirmText"
          size="small"
          @click="$emit('confirmAction')"
        />
        <TextButton
          :aria-label="cancelButtonText || 'Abbrechen'"
          :button-type="cancelButtonType"
          :label="cancelButtonText || 'Abbrechen'"
          size="small"
          @click="$emit('closeModal')"
        />
      </div>
    </div>
  </div>
</template>
