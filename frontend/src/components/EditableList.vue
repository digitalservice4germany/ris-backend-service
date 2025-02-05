<script lang="ts" setup generic="T extends ListItem">
import type { Component, ComponentPublicInstance, Ref } from "vue"
import { computed, nextTick, ref, watch } from "vue"
import Tooltip from "./Tooltip.vue"
import DefaultSummary from "@/components/DefaultSummary.vue"
import TextButton from "@/components/input/TextButton.vue"
import ListItem from "@/domain/editableListItem"
import IconArrowDown from "~icons/ic/baseline-keyboard-arrow-down"
import IconAdd from "~icons/material-symbols/add"

interface Props {
  editComponent: Component
  summaryComponent?: Component
  modelValue?: T[]
  defaultValue: T
}

const props = withDefaults(defineProps<Props>(), {
  summaryComponent: DefaultSummary,
  modelValue: () => [],
})

const emit = defineEmits<{
  "update:modelValue": [value: T[]]
}>()

const editEntry = ref<T | undefined>() as Ref<T | undefined>
const displayDefaultValue = ref(false)
const modelValueList = ref<T[]>([...props.modelValue]) as Ref<T[]>

const editedItemRef = ref<Map<number, HTMLElement | ComponentPublicInstance>>(
  new Map(),
)
const containerRef = ref<HTMLElement | null>(null)

/**
 * Computed mergedValues is a list for rendering only. It's a computed helper list to keep the default value
 * separated from the modelValue. Only when saved it will move to modelValue
 */
const mergedValues = computed(() => {
  return displayDefaultValue.value
    ? [...modelValueList.value, props.defaultValue]
    : [...modelValueList.value]
})

/**
 * Scrolls the edited item at the specified index into view (if needed).
 *
 * @param index - The index of the edited item to scroll into.
 * @returns A promise that resolves after the DOM updates.
 */
async function scrollToEditedItem(index: number) {
  await nextTick()
  const refToScroll = editedItemRef.value.get(index)
  if (refToScroll) {
    // If ref is a Vue component, access the DOM element via $el
    const editedItemSummary =
      "$el" in refToScroll ? refToScroll.$el : refToScroll
    if (
      editedItemSummary instanceof HTMLElement &&
      "scrollIntoView" in editedItemSummary
    ) {
      editedItemSummary.scrollIntoView({
        block: "nearest",
      })
    }
  }
}

/**
 * Scrolls editable list container into view.
 *
 * @returns A promise that resolves after the DOM updates.
 */
async function scrollToContainer() {
  await nextTick()
  if (
    containerRef.value instanceof HTMLElement &&
    "scrollIntoView" in containerRef.value
  ) {
    containerRef.value.scrollIntoView({
      block: "nearest",
    })
  }
}

/**
 * Toggles the display of the default entry and sets the editing entry.
 * @param {boolean} shouldDisplay
 * @param index (is needed in case we want to scroll into view of list item summary)
 */
async function toggleDisplayDefaultValue(
  shouldDisplay: boolean,
  index?: number,
) {
  const hasModeSwitched = shouldDisplay !== displayDefaultValue.value
  if (shouldDisplay) {
    displayDefaultValue.value = true
    const { defaultValue } = props
    setEditEntry(defaultValue as T)
    if (hasModeSwitched && index !== undefined) {
      await scrollToEditedItem(index)
    }
  } else {
    displayDefaultValue.value = false
    setEditEntry()
  }
}

/**
 * Setting the edit entry, renders the edit component of the given entry, the summary component is invisible
 * @param entry
 */
function setEditEntry(entry?: T) {
  editEntry.value = entry
}

/**
 * Resetting the edit to undefined, to show all list items in summary mode
 */
async function cancelEdit(index: number) {
  await toggleDisplayDefaultValue(false, index)
  await scrollToEditedItem(index)
}

/**
 *
 * Removes a new list item, with the given entry, by propagating an updated list without the list item
 * at the given index to the parent component. Resets edited entry reset, to show list in summary mode.
 * @param entry
 */
async function removeEntry(entry: T) {
  const updatedEntries = filterEntries(props.modelValue, entry)
  emit("update:modelValue", updatedEntries)
  setEditEntry()
  await scrollToContainer()
}

/**
 * Filters out the specified entry from the list of entries.
 * @param {T[]} entries - The array of entries to filter.
 * @param {T} entryToRemove - The entry to remove from the array.
 * @returns {T[]} A new array with the specified entry removed.
 */
function filterEntries(entries: T[], entryToRemove?: T): T[] {
  if (!entryToRemove) return entries
  return [...entries].filter((item) => !entryToRemove.equals(item))
}

function isSelected(entry: T): boolean {
  if (editEntry.value !== undefined) {
    return editEntry.value.equals(entry)
  }
  return false
}

/**
 * Method to check if entry is given in model value
 */
function isSaved(entries: T[], entry?: T): boolean {
  if (entry) {
    return entries.some((item) => entry.equals(item))
  }
  return false
}

/**
 * Updating the modelValue with the local modelValue list, is not propagated, until the user actively
 * decides to click the save button in edit mode. The edit index is reset, to show list in summary mode.
 */
async function updateModel(index: number) {
  emit("update:modelValue", modelValueList.value)
  // await toggleDisplayDefaultValue(true, index)
}

/**
 * When the modelValue changes, it is copied to a local copy. The user can update an item in that local model value list,
 * it is not saved until the save button is clicked.
 */
watch(
  () => props.modelValue,
  (newValue) => {
    // Update modelValueList based on the newValue while keeping the edit item intact
    modelValueList.value = [...newValue].map((item) =>
      editEntry.value !== undefined && editEntry.value.equals(item)
        ? editEntry.value
        : item,
    )
  },
  {
    immediate: true,
    deep: true,
  },
)

/**
 * When the local model value list is empty, (e.g. on mount or by removing an item) a default entry is displayed
 */
watch(
  () => modelValueList,
  async () => {
    if (modelValueList.value.length == 0) {
      await toggleDisplayDefaultValue(true)
    }
  },
  {
    immediate: true,
    deep: true,
  },
)

// Expose the method
defineExpose({
  toggleDisplayDefaultValue,
})
</script>

<template>
  <div
    ref="containerRef"
    class="w-full scroll-m-64"
    data-testid="editable-list-container"
  >
    <div
      v-for="(entry, index) in mergedValues"
      :key="index"
      aria-label="Listen Eintrag"
    >
      <div
        v-if="!isSelected(entry as T)"
        :key="index"
        class="group flex gap-8 border-b-1 border-blue-300 py-16"
        :class="{ 'border-t-1': index == 0 }"
      >
        <component
          :is="summaryComponent"
          :ref="
            (el: HTMLElement | ComponentPublicInstance | null) =>
              el && editedItemRef.set(index, el)
          "
          class="flex scroll-m-64"
          :data="entry"
        />

        <Tooltip text="Aufklappen">
          <button
            id="editable-list-select-button"
            class="flex h-32 w-32 items-center justify-center text-blue-800 hover:bg-blue-100 focus:shadow-[inset_0_0_0_0.125rem] focus:shadow-blue-800 focus:outline-none"
            :data-testid="`list-entry-${index}`"
            @click="
              () => {
                toggleDisplayDefaultValue(false)
                setEditEntry(entry as T)
              }
            "
            @keypress.enter="
              () => {
                toggleDisplayDefaultValue(false)
                setEditEntry(entry as T)
              }
            "
          >
            <IconArrowDown />
          </button>
        </Tooltip>
      </div>

      <component
        :is="editComponent"
        v-if="isSelected(entry as T)"
        v-model="modelValueList[index]"
        class="py-24"
        :class="{ 'pt-0': index == 0 }"
        :is-saved="isSaved(modelValue, modelValueList[index])"
        :model-value-list="modelValueList"
        @add-entry="updateModel(index)"
        @cancel-edit="cancelEdit(index)"
        @remove-entry="removeEntry(entry as T)"
      />
    </div>

    <TextButton
      v-if="!editEntry"
      aria-label="Weitere Angabe"
      button-type="tertiary"
      class="my-24 first:mt-0"
      :icon="IconAdd"
      label="Weitere Angabe"
      size="small"
      @click="toggleDisplayDefaultValue(true)"
    />
  </div>
</template>
