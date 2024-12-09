<script setup lang="ts" generic="T extends InputModelProps">
import * as Sentry from "@sentry/vue"
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  shallowRef,
  watch,
} from "vue"
import FlexContainer from "@/components/FlexContainer.vue"
import {
  ComboboxAttributes,
  ComboboxInputModelType,
  ComboboxItem,
} from "@/components/input/types"
import IconKeyboardArrowDown from "~icons/ic/baseline-keyboard-arrow-down"
import IconKeyboardArrowUp from "~icons/ic/baseline-keyboard-arrow-up"
import IconClear from "~icons/material-symbols/close-small"

const props = defineProps<{
  id: string
  itemService: ComboboxAttributes["itemService"]
  modelValue: T
  ariaLabel: string
  placeholder?: string
  clearOnChoosingItem?: boolean
  manualEntry?: boolean
  noClear?: boolean
  hasError?: boolean
  readOnly?: boolean
}>()

const emit = defineEmits<{
  "update:modelValue": [value?: ComboboxInputModelType]
  focus: [void]
}>()

const isDefined = <A,>(item: A | undefined): item is A => !!item

const NO_MATCHING_ENTRY = "Kein passender Eintrag"

const candidateForSelection = ref<ComboboxItem>() // <-- the top search result
const inputText = ref<string>()
const existingItems = ref<ComboboxItem[]>()
const currentlyDisplayedItems = computed(() =>
  [...(existingItems.value ?? []), createNewItem.value].filter(isDefined),
)
const createNewItem = ref<ComboboxItem>()
const showDropdown = ref(false)
const filter = ref<string>()
const dropdownContainerRef = ref<HTMLElement>()
const dropdownItemsRef = shallowRef<HTMLElement[]>([])
const inputFieldRef = ref<HTMLInputElement>()
const focusedItemIndex = ref<number>(-1)

const isUpdating = ref(false)
const hasToUpdate = ref(false)

const ariaLabelDropdownIcon = computed(() =>
  showDropdown.value ? "Dropdown schließen" : "Dropdown öffnen",
)
const noMatchingExistingItems = computed(
  () => !existingItems.value || existingItems.value.length === 0,
)

const conditionalClasses = computed(() => ({
  "!shadow-red-900 !bg-red-200": props.hasError,
  "!shadow-none !bg-blue-300": props.readOnly,
}))

const toggleDropdown = async () => {
  focusedItemIndex.value = -1
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    filter.value = inputText.value
    await updateCurrentItems()
    inputFieldRef.value?.focus()
  }
}

const showUpdatedDropdown = async () => {
  emit("focus")
  focusedItemIndex.value = -1
  showDropdown.value = true
  filter.value = inputText.value
  await updateCurrentItems()
}

const clearDropdown = async () => {
  if (props.noClear) return

  emit("update:modelValue", undefined)
  filter.value = ""
  inputText.value = ""
  focusedItemIndex.value = -1
  if (showDropdown.value) {
    await updateCurrentItems()
  }
  inputFieldRef.value?.focus()
}

const setChosenItem = (item: ComboboxItem) => {
  if (item.label === NO_MATCHING_ENTRY) return
  showDropdown.value = false
  emit("update:modelValue", item.value)
  if (props.clearOnChoosingItem) {
    filter.value = ""
    inputText.value = ""
  } else {
    filter.value = item.label
  }
  candidateForSelection.value = undefined
}

const onEnter = async () => {
  if (candidateForSelection.value) {
    setChosenItem(candidateForSelection.value)
    return
  }
  await toggleDropdown()
}

const onInput = async () => {
  if (inputText.value === "") {
    if (!props.noClear) emit("update:modelValue", undefined)
  }

  await showUpdatedDropdown()
}

const keyArrowUp = () => {
  if (focusedItemIndex.value > 0) {
    focusedItemIndex.value -= 1
  }
  updateFocusedItem()
}

const keyArrowDown = () => {
  if (focusedItemIndex.value < dropdownItemsRef.value.length - 1) {
    focusedItemIndex.value += 1
  }
  updateFocusedItem()
}

const updateFocusedItem = () => {
  candidateForSelection.value = undefined
  const item = dropdownItemsRef.value[focusedItemIndex.value]
  if (item && item.innerText !== NO_MATCHING_ENTRY) item.focus()
}

const updateCurrentItems = async () => {
  hasToUpdate.value = true
  if (isUpdating.value && hasToUpdate.value) {
    return
  }

  isUpdating.value = true

  let response
  let tries = 0
  do {
    hasToUpdate.value = false
    response = await props.itemService(filter.value)
    isUpdating.value = false
    tries++
  } while (hasToUpdate.value && tries < 6)

  if (tries >= 6) {
    Sentry.captureMessage(
      "more than 5 tries to call the item service at the combobox",
      "error",
    )
  }

  if (!response.data) {
    console.error(response.error)
    return
  }

  existingItems.value = response.data

  if (
    noMatchingExistingItems.value ||
    //no exact match found when add manual entry option set
    (props.manualEntry &&
      filter.value &&
      !existingItems.value.find((item) => item.label === filter.value?.trim()))
  ) {
    handleNoSearchResults(filter.value)
  } else {
    createNewItem.value = undefined
    candidateForSelection.value = existingItems.value[0]
    focusedItemIndex.value = 0
  }
}

function handleNoSearchResults(searchStr?: string) {
  if (props.manualEntry && searchStr) {
    createNewItem.value = {
      label: `${searchStr} neu erstellen`,
      value: { label: searchStr },
      labelCssClasses: "ds-label-01-bold text-blue-800 underline",
    }
    candidateForSelection.value = { label: searchStr }
  } else {
    existingItems.value = [{ label: NO_MATCHING_ENTRY }]
    candidateForSelection.value = undefined
  }
}

const handleClickOutside = (event: MouseEvent) => {
  const dropdown = dropdownContainerRef.value
  if (
    !dropdown ||
    (event.target as HTMLElement) === dropdown ||
    event.composedPath().includes(dropdown)
  )
    return
  closeDropdownAndRevertToLastSavedValue()
}

const selectAllText = () => {
  if (!props.readOnly) inputFieldRef.value?.select()
}

const closeDropdownAndRevertToLastSavedValue = () => {
  showDropdown.value = false
  inputText.value = props.modelValue?.label
}

watch(
  props,
  () => {
    inputText.value = props.modelValue?.label
  },
  { immediate: true },
)

onMounted(() => {
  window.addEventListener("click", handleClickOutside)
})

onBeforeUnmount(() => {
  window.removeEventListener("click", handleClickOutside)
})
</script>

<script lang="ts">
export type InputModelProps =
  | {
      label: string
    }
  | undefined
</script>

<template>
  <div ref="dropdownContainerRef" class="relative w-full">
    <div
      class="space-between flex h-48 flex-row whitespace-nowrap bg-white px-16 py-12 shadow-button shadow-blue-800"
      :class="conditionalClasses"
    >
      <input
        :id="id"
        ref="inputFieldRef"
        v-model="inputText"
        :aria-label="ariaLabel"
        autocomplete="off"
        class="w-full bg-transparent placeholder:font-font-family-sans placeholder:not-italic placeholder:text-gray-800 focus:outline-none"
        :placeholder="placeholder"
        :readonly="readOnly"
        tabindex="0"
        @click="selectAllText"
        @focus="showUpdatedDropdown"
        @input="onInput"
        @keydown.down.prevent="keyArrowDown"
        @keydown.enter="onEnter"
        @keydown.esc="closeDropdownAndRevertToLastSavedValue"
        @keydown.tab="closeDropdownAndRevertToLastSavedValue"
      />
      <div v-if="!readOnly" class="flex flex-row">
        <button
          v-if="inputText && !noClear"
          aria-label="Auswahl zurücksetzen"
          class="flex items-center text-blue-800 focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800"
          tabindex="0"
          @click="clearDropdown"
        >
          <IconClear />
        </button>

        <button
          :aria-label="ariaLabelDropdownIcon"
          class="input-expand-icon flex items-center text-blue-800"
          tabindex="-1"
          @click="toggleDropdown"
        >
          <IconKeyboardArrowDown v-if="!showDropdown" />
          <IconKeyboardArrowUp v-else />
        </button>
      </div>
    </div>
    <div
      v-if="showDropdown && !readOnly"
      class="absolute left-0 right-0 top-[100%] z-20 flex max-h-[300px] flex-col overflow-y-scroll bg-white px-8 py-12 drop-shadow-md"
      tabindex="-1"
    >
      <button
        v-for="(item, index) in currentlyDisplayedItems"
        :key="index"
        ref="dropdownItemsRef"
        aria-label="dropdown-option"
        class="cursor-pointer px-16 py-12 text-left hover:bg-blue-100 focus:border-l-4 focus:border-solid focus:border-l-blue-800 focus:bg-blue-200 focus:outline-none"
        :class="{
          'border-l-4 border-solid border-l-blue-800 bg-blue-200':
            candidateForSelection === item,
        }"
        tabindex="0"
        @click="setChosenItem(item)"
        @keydown.down.prevent="keyArrowDown"
        @keydown.enter="setChosenItem(item)"
        @keydown.tab="closeDropdownAndRevertToLastSavedValue"
        @keydown.up.prevent="keyArrowUp"
      >
        <FlexContainer
          align-items="items-end"
          justify-content="justify-between"
        >
          <span>
            <span :class="item.labelCssClasses">{{ item.label }}</span>
            <div
              v-if="item.additionalInformation"
              aria-label="additional-dropdown-info"
              class="ds-label-02-reg text-neutral-700"
            >
              {{ item.additionalInformation }}
            </div>
          </span>

          <span
            v-if="item.sideInformation"
            id="dropDownSideInformation"
            class="ds-label-02-reg text-neutral-700"
          >
            {{ item.sideInformation }}
          </span>
        </FlexContainer>
      </button>
    </div>
  </div>
</template>
