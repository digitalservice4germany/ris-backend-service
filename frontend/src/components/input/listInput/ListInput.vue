<script lang="ts" setup>
import { ref, computed, onMounted, nextTick } from "vue"
import ListInputDisplay from "@/components/input/listInput/ListInputDisplay.vue"
import ListInputEdit from "@/components/input/listInput/ListInputEdit.vue"

const props = defineProps<{
  label: string
  modelValue: string[] | undefined
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string[]]
  reset: []
}>()

const list = ref(props.modelValue ?? [])
const editMode = ref(true)
const sortAlphabetically = ref(false)

/**
 * Computed property to manage the list input value.
 * - Get: Joins the array into a newline-separated string for textarea display.
 * - Set: Updates the list based on the textarea input, trims, filters out empty values, removes duplicates,
 *        and sorts alphabetically if the option is enabled.
 * @returns {string} The joined string of list items separated by newlines.
 */
const listInputValue = computed<string>({
  get: () => list.value.join("\n"),
  set: async (newValues: string) => {
    list.value = newValues
      .split("\n")
      .map((listItem) => listItem.trim())
      .filter((listItem) => listItem !== "")

    // Sort alphabetically if the option is set
    if (sortAlphabetically.value && list.value) {
      list.value = list.value.sort((a: string, b: string) => a.localeCompare(b))
    }

    // Remove duplicates
    list.value = [...new Set(list.value)] as string[]

    // Emit the updated value
    emit("update:modelValue", list.value)

    await toggleEditMode()

    // Reset sorting option
    sortAlphabetically.value = false
  },
})

const listInputDisplayRef = ref<InstanceType<typeof ListInputDisplay> | null>(
  null,
)

/**
 * Emit reset if the list is empty, to show the category wrapper again.
 * Otherwise, toggles between edit mode and display mode.
 */
async function toggleEditMode() {
  // Reset sorting option
  sortAlphabetically.value = false
  if (list.value?.length) {
    editMode.value = !editMode.value
  } else emit("reset")

  if (!editMode.value) {
    // Toggle from edit to display: As height of display mode can be less than edit mode -> scroll into view.
    await nextTick()
    listInputDisplayRef.value?.$el?.scrollIntoView({
      block: "nearest",
    })
  }
}

/**
 * Initializes the edit mode based on whether the list is empty or not.
 */
onMounted(() => {
  editMode.value = !props.modelValue?.length
})
</script>

<template>
  <ListInputEdit
    v-if="editMode"
    v-model="listInputValue"
    :label="label"
    :list-item-count="modelValue?.length ?? 0"
    :sort-alphabetically="sortAlphabetically"
    @toggle="toggleEditMode"
    @toggle-sorting="sortAlphabetically = !sortAlphabetically"
  />
  <ListInputDisplay
    v-else
    ref="listInputDisplayRef"
    v-model="list"
    :label="label"
    @toggle="toggleEditMode"
  />
</template>
