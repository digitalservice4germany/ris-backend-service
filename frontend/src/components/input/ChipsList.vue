<script lang="ts" setup>
import { ref, watch } from "vue"
import IconClear from "~icons/ic/baseline-clear"

interface Props {
  modelValue: string[] | undefined
  focusedItem?: number
  readOnly?: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  chipDeleted: [index: number, value: string]
  nextClickedOnLast: []
  previousClickedOnFirst: []
  "update:modelValue": [value?: string[]]
  "update:focusedItem": [index: number | undefined]
}>()

/* -------------------------------------------------- *
 * Model value                                        *
 * -------------------------------------------------- */

function deleteChip(index: number, value: string) {
  if (props.readOnly) return

  const temp: string[] = props.modelValue
    ? [...props.modelValue]
        .map((item, itemIndex) => ({ item, itemIndex }))
        .filter(({ item, itemIndex }) => item !== value && itemIndex != index)
        .map(({ item }) => item)
    : []

  emit("update:modelValue", temp)
  emit("chipDeleted", index, value)
}

/* -------------------------------------------------- *
 * Focus management                                   *
 * -------------------------------------------------- */

const containerRef = ref<HTMLElement>()

// Decoupling the local state for the focused item from the model value allows
// us to manage the focus even if no model value has been passed. Parents are
// free to pass a model value though if they want to take control of the focus.
const localFocusedItem = ref<number | undefined>(props.focusedItem)

// Sync prop to local state, making sure it's within the bounds of the model
// value
watch(
  () => props.focusedItem,
  (is) => {
    if (is === localFocusedItem.value) return
    localFocusedItem.value = is !== undefined ? toExistingIndex(is) : undefined
  },
)

// Sync local state to prop
watch(
  () => localFocusedItem.value,
  (is) => {
    if (is === props.focusedItem) return
    emit("update:focusedItem", is)
  },
)

// Adjust local state when model value changes to make sure it's still adressing
// a valid item
watch(
  () => props.modelValue,
  (is) => {
    if (!is) localFocusedItem.value = undefined
    else localFocusedItem.value = toExistingIndex(localFocusedItem.value)
  },
)

watch(localFocusedItem, (is) => {
  // TODO: Remove focus when changing to undefined
  if (is === undefined) return
  const item = containerRef.value?.children?.[is] as HTMLElement
  item?.focus()
})

/**
 * Returns the value if it falls within the bounds of the model value, or the
 * closest valid value otherwise.
 */
function toExistingIndex(index: number | undefined): number | undefined {
  if (!props.modelValue?.length || index === undefined) return undefined
  else if (index < 0) return 0
  else if (index >= props.modelValue.length) return props.modelValue.length - 1
  else return index
}

function focusPrevious() {
  if (localFocusedItem.value === undefined) return
  if (localFocusedItem.value === 0) emit("previousClickedOnFirst")
  localFocusedItem.value = Math.max(0, localFocusedItem.value - 1)
}

function focusNext() {
  if (localFocusedItem.value === undefined || !props.modelValue) return
  if (localFocusedItem.value === props.modelValue.length - 1) {
    emit("nextClickedOnLast")
  }
  const next = Math.min(props.modelValue.length - 1, localFocusedItem.value + 1)
  localFocusedItem.value = next
}
</script>

<template>
  <ul
    ref="containerRef"
    class="m-0 flex min-w-0 flex-row flex-wrap items-center gap-8 p-0 empty:m-0"
  >
    <template v-if="modelValue">
      <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
      <li
        v-for="(chip, i) in modelValue"
        :key="i"
        class="group ds-body-01-reg relative -mt-1 mr-6 flex min-w-0 cursor-pointer items-center rounded-full bg-blue-500 outline-none"
        :class="{ 'pr-32': !readOnly }"
        data-testid="chip"
        tabindex="0"
        @click.stop="localFocusedItem = i"
        @focus="localFocusedItem = i"
        @keydown.enter.stop.prevent="deleteChip(i, chip)"
        @keydown.left.stop.prevent="focusPrevious"
        @keydown.right.stop.prevent="focusNext"
      >
        <span
          class="ds-label-03-reg inline overflow-hidden text-ellipsis whitespace-nowrap py-6 pl-8 text-14"
          :class="{ 'pr-12': readOnly }"
          data-testid="chip-value"
          >{{ chip }}
        </span>

        <button
          v-if="!readOnly"
          aria-label="Löschen"
          class="absolute inset-y-0 right-0 flex h-full items-center rounded-r-full pl-3 pr-6 text-14 group-focus:bg-blue-900 group-focus:text-white"
          tabindex="-1"
          type="button"
          @click="deleteChip(i, chip)"
        >
          <IconClear />
        </button>
      </li>
    </template>
  </ul>
</template>
