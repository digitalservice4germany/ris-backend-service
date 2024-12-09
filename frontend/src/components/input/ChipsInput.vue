<script lang="ts" setup>
import * as Sentry from "@sentry/vue"
import { produce } from "immer"
import { vMaska } from "maska/vue"
import { nextTick, ref, watch, watchEffect, computed } from "vue"
import ChipsList from "@/components/input/ChipsList.vue"
import { ValidationError } from "@/components/input/types"
import IconSubdirectoryArrowLeft from "~icons/ic/baseline-subdirectory-arrow-left"

interface Props {
  id: string
  modelValue?: string[]
  ariaLabel?: string
  hasError?: boolean
  readOnly?: boolean
  maska?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  "update:modelValue": [value?: string[]]
  "update:validationError": [value?: ValidationError]
}>()

/* -------------------------------------------------- *
 * Data handling                                      *
 * -------------------------------------------------- */

const newChipText = ref<string>("")

const addChip = () => {
  if (props.readOnly) return

  const chip = newChipText.value.trim()
  if (!chip) return

  const current = props.modelValue ?? []

  if (current.includes(chip)) {
    emit("update:validationError", {
      message: chip + " bereits vorhanden",
      instance: props.id,
    })
  } else {
    let next = produce(current, (draft) => {
      draft.push(chip)
    })

    next = next.filter((value) => {
      if (!value) {
        Sentry.captureMessage("Chip list contains empty string.", "error")
        return false
      } else {
        return true
      }
    })

    emit("update:modelValue", next)
    emit("update:validationError", undefined)
  }

  newChipText.value = ""
}

function onDeleteChip() {
  if (props.readOnly) return

  focusInputIfEmpty()
}

/* -------------------------------------------------- *
 * Focus management                                   *
 * -------------------------------------------------- */

const chipsInput = ref<HTMLInputElement | null>(null)

const focusedChip = ref<number | undefined>(undefined)

const conditionalClasses = computed(() => ({
  "!shadow-red-800 !bg-red-200": props.hasError,
  "!shadow-none !bg-blue-300": props.readOnly,
}))

function maybeFocusPrevious() {
  if (chipsInput.value?.selectionStart === 0) {
    focusedChip.value =
      props.modelValue !== undefined ? props.modelValue.length - 1 : undefined
  }
}

const focusInput = () => {
  focusedChip.value = undefined
  chipsInput.value?.focus()
}

function focusInputIfEmpty() {
  if (props.modelValue?.length === 1) focusInput()
}

/* -------------------------------------------------- *
 * Input width management (needed for the enter icon) *
 * -------------------------------------------------- */

const wrapperEl = ref<HTMLElement | null>(null)

const inputContentWidth = ref<string | undefined>("auto")

async function determineInputWidth() {
  if (!chipsInput.value) return

  // We first need to reset the height to auto, so that the scrollHeight
  // is not limited by the current height. Then wait for the next tick
  // so that the input has time to resize.
  inputContentWidth.value = undefined
  await nextTick()

  const { borderLeftWidth, borderRightWidth } = getComputedStyle(
    chipsInput.value as Element,
  )

  const borderLeft = parseInt(borderLeftWidth)
  const borderRight = parseInt(borderRightWidth)

  // Constrain the width to the wrapper width. This helps to prevent the component
  // from growing in unexpected ways when the user types long words. (Technically
  // this could also be solved by using hidden overflow and other layout
  // constraints in the parent component, but we want to avoid bothering users
  // of the component with this complexity.)
  let maxWidth: number | undefined = undefined
  if (wrapperEl.value) {
    const { paddingLeft, paddingRight } = getComputedStyle(wrapperEl.value)
    const padding = parseInt(paddingLeft) + parseInt(paddingRight)
    maxWidth = wrapperEl.value.clientWidth - padding - 16 // 16px for the icon
  }

  inputContentWidth.value = `min(${maxWidth ?? "9999"}px, ${
    chipsInput.value.scrollWidth + borderLeft + borderRight
  }px)`
}

watchEffect(() => {
  determineInputWidth().catch(() => {
    // left blank intentionally
  })
})

watch(newChipText, async () => {
  await determineInputWidth()
})
</script>

<template>
  <div class="w-full">
    <!-- Ignore requirement to have a keyboard listener as it's only a convenience
    for mouse users, but keyboard users can already do the same thing by tabbing
    just fine -->
    <!-- eslint-disable vuejs-accessibility/click-events-have-key-events -->
    <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
    <div
      ref="wrapperEl"
      class="flex min-h-48 w-full cursor-text flex-wrap bg-white p-8 shadow-button shadow-blue-800"
      :class="conditionalClasses"
      :data-testid="`chips-input_${id}`"
      @click="focusInput"
    >
      <ChipsList
        v-model:focused-item="focusedChip"
        :model-value="modelValue"
        :read-only="readOnly"
        @chip-deleted="onDeleteChip"
        @next-clicked-on-last="focusInput"
        @update:model-value="$emit('update:modelValue', $event)"
      />

      <span
        v-if="!readOnly"
        class="ml-8 flex max-w-full flex-auto items-center justify-start"
      >
        <span :id="`enter-note-for-${id}`" class="sr-only">
          Enter drücken, um die Eingabe zu bestätigen
        </span>
        <input
          :id="id"
          ref="chipsInput"
          v-model="newChipText"
          v-maska
          :aria-describedby="`enter-note-for-${id}`"
          :aria-label="ariaLabel"
          class="peer w-4 min-w-0 border-none bg-transparent outline-none"
          :data-maska="maska ?? null"
          :style="{ width: inputContentWidth }"
          type="text"
          @blur="addChip"
          @focus="focusedChip = undefined"
          @keydown.enter.stop.prevent="addChip"
          @keydown.left="maybeFocusPrevious"
        />
        <span class="flex-none text-transparent peer-focus:text-gray-900">
          <IconSubdirectoryArrowLeft height="16px" width="16px" />
        </span>
      </span>
    </div>
  </div>
</template>
