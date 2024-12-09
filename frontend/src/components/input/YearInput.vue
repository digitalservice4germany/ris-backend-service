<script lang="ts" setup>
import dayjs from "dayjs"
import customParseFormat from "dayjs/plugin/customParseFormat"
import { Mask } from "maska"
import { vMaska } from "maska/vue"
import { computed, onMounted, ref, watch } from "vue"
import TextInput from "@/components/input/TextInput.vue"
import { ValidationError } from "@/components/input/types"

interface Props {
  id: string
  modelValue: string | undefined
  hasError?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  "update:modelValue": [value: string | undefined]
  "update:validationError": [value?: ValidationError]
}>()

/* -------------------------------------------------- *
 * Model handling                                     *
 * -------------------------------------------------- */

// We'll need to keep an internal state for the input value since there are
// some conditions that need to be checked before we can emit the value to the
// parent component.
const localModelValue = ref(props.modelValue)

// This will be true if we think that the user is no longer working on the
// input, e.g. on blur. We'll set it to false if the local value changes, i.e.
// the user is typing. We'll try not to annoy users with validation errors
// if they are still changing the value.
const userHasFinished = ref<boolean>(false)

const shouldShowValidationState = computed(() =>
  Boolean(
    localModelValue.value && (inputCompleted.value || userHasFinished.value),
  ),
)

watch(
  () => props.modelValue,
  () => {
    localModelValue.value = props.modelValue
  },
)

// Use this for changing the localModelValue. This will automatically clean up
// the new value, update dependent states, and communicate the state change to
// the parent.
function emitModelValue(value: string | undefined): void {
  if (value === localModelValue.value) return

  const nextValue = onlyAllowNumbers(value)
  userHasFinished.value = false

  localModelValue.value = nextValue

  if (validateYear(nextValue)) emit("update:modelValue", nextValue)
  else if (!nextValue) emit("update:modelValue", undefined)
}

function onlyAllowNumbers(input: string | undefined): string | undefined {
  return input ? input.replace(/\D/g, "") : input
}

/* -------------------------------------------------- *
 * Validation                                         *
 * -------------------------------------------------- */

dayjs.extend(customParseFormat)

const mask = "####"
const inputCompleted = computed(
  () =>
    localModelValue.value &&
    new Mask({ mask }).completed(localModelValue.value),
)

const isValid = computed(() => validateYear(localModelValue.value))

function validateInput() {
  if (!isValid.value && localModelValue.value) {
    emit("update:validationError", {
      message: "Kein valides Jahr",
      instance: props.id,
    })
  } else {
    emit("update:validationError", undefined)
  }
}

function backspaceDelete() {
  emit("update:validationError", undefined)
  if (localModelValue.value === "")
    emit("update:modelValue", localModelValue.value)
}

watch(shouldShowValidationState, (is) => {
  if (is) validateInput()
})

onMounted(() => {
  if (localModelValue.value) validateInput()
})

// Valid years = 1000 - 9999
function validateYear(input: string | undefined): boolean {
  if (!input || input.length < 4) return false

  const date = dayjs(input, "YYYY", true)
  return date.isValid() && date.year() >= 1000 && date.year() <= 9999
}
</script>

<template>
  <TextInput
    :id="id"
    v-maska="mask"
    :aria-label="($attrs.ariaLabel as string) ?? ''"
    :has-error="hasError || (shouldShowValidationState && !isValid)"
    maxlength="4"
    :model-value="localModelValue"
    placeholder="JJJJ"
    type="text"
    @blur="userHasFinished = true"
    @keydown.delete="backspaceDelete"
    @update:model-value="emitModelValue"
  />
</template>
