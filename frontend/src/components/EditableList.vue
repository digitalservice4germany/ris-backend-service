<script lang="ts" setup generic="T extends ListItem">
import type { Component, Ref } from "vue"
import { ref, watch, computed } from "vue"
import DataSetSummary from "@/components/DataSetSummary.vue"
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
  summaryComponent: DataSetSummary,
  modelValue: () => [],
})

const emit = defineEmits<{
  "update:modelValue": [value: T[]]
}>()

const editEntry = ref<T | undefined>() as Ref<T | undefined>
const displayDefaultValue = ref(false)
const modelValueList = ref<T[]>([...props.modelValue]) as Ref<T[]>

/**
 * Computed mergedValues is a list for rendering only. It's a computed helper list to keep the default value
 * seperated from the modelValue. Only when saved it will move to modelValue
 */
const mergedValues = computed(() => {
  return displayDefaultValue.value
    ? [...modelValueList.value, props.defaultValue]
    : [...modelValueList.value]
})

/**
 * Toggles the display of the default entry and sets the editing entry.
 * @param {boolean} shouldDisplay
 */
function toggleDisplayDefaultValue(shouldDisplay: boolean) {
  if (shouldDisplay) {
    displayDefaultValue.value = true
    const { defaultValue } = props
    setEditEntry(defaultValue as T)
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
function cancelEdit() {
  toggleDisplayDefaultValue(false)
}

/**
 *
 * Removes a new list item, with the given entry, by propagating an updated list without the list item
 * at the given index to the parent component. Resets edited entry reset, to show list in summary mode.
 * @param entry
 */
function removeEntry(entry: T) {
  const updatedEntries = filterEntries(props.modelValue, entry)
  emit("update:modelValue", updatedEntries)
  setEditEntry()
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
 * decides to click the save button in edit mode. The edit index is resetted, to show list in summary mode.
 */
function updateModel() {
  emit("update:modelValue", modelValueList.value)
  toggleDisplayDefaultValue(true)
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
  () => {
    if (modelValueList.value.length == 0) {
      toggleDisplayDefaultValue(true)
    }
  },
  {
    immediate: true,
    deep: true,
  },
)
</script>

<template>
  <div class="w-full">
    <div
      v-for="(entry, index) in mergedValues"
      :key="index"
      aria-label="Listen Eintrag"
      class="border-b-1 border-blue-300"
      :class="
        index == 0 ? 'first:border-t-1' : 'first:border-t-0 last:border-b-0'
      "
    >
      <div
        v-if="!isSelected(entry)"
        :key="index"
        class="group flex gap-8 py-16"
      >
        <component :is="summaryComponent" :data="entry" />
        <button
          id="editable-list-select-button"
          class="flex h-32 w-32 items-center justify-center text-blue-800 hover:bg-blue-100 focus:shadow-[inset_0_0_0_0.125rem] focus:shadow-blue-800 focus:outline-none"
          :data-testid="`list-entry-${index}`"
          @click="
            () => {
              toggleDisplayDefaultValue(false)
              setEditEntry(entry)
            }
          "
          @keypress.enter="
            () => {
              toggleDisplayDefaultValue(false)
              setEditEntry(entry)
            }
          "
        >
          <IconArrowDown />
        </button>
      </div>

      <component
        :is="editComponent"
        v-if="isSelected(entry)"
        v-model="modelValueList[index]"
        class="py-24"
        :is-saved="isSaved(modelValue, modelValueList[index])"
        :model-value-list="modelValueList"
        @add-entry="updateModel"
        @cancel-edit="cancelEdit"
        @remove-entry="removeEntry(entry)"
      />
    </div>

    <TextButton
      v-if="!editEntry"
      aria-label="Weitere Angabe"
      button-type="tertiary"
      class="mt-24 first:mt-0"
      :icon="IconAdd"
      label="Weitere Angabe"
      size="small"
      @click="toggleDisplayDefaultValue(true)"
    />
  </div>
</template>
