<script lang="ts" setup>
import { onMounted, onUnmounted, ref, watch } from "vue"
import type { Component } from "vue"
import Tooltip from "../Tooltip.vue"
import IconDropdown from "~icons/ic/baseline-arrow-drop-down"

const props = defineProps<EditorButton>()

const emits = defineEmits<{
  toggle: [value: EditorButton]
}>()

const showDropdown = ref(false)
const clickedInside = ref(false)

const button = ref<HTMLElement>()
const children = ref<HTMLElement[]>([])

function onClickToggle(button: EditorButton) {
  clickedInside.value = true
  if (!button.childButtons || button.type === "more") emits("toggle", button)
  else if (button.type === "menu") showDropdown.value = !showDropdown.value
}

const closeDropDownWhenClickOutSide = () => {
  if (clickedInside.value) {
    clickedInside.value = false
    return
  }
  showDropdown.value = false
}

onMounted(() => {
  document.addEventListener("click", closeDropDownWhenClickOutSide)
})
onUnmounted(() => {
  document.removeEventListener("click", closeDropDownWhenClickOutSide)
})
defineExpose({ button, children })

watch(
  () => props.disabled,
  (isDisabled) => {
    if (isDisabled) {
      showDropdown.value = false
    }
  },
)

export interface EditorButton {
  type: string
  icon: Component
  ariaLabel: string
  childButtons?: EditorButton[]
  tabIndex?: number
  isLast?: boolean
  isActive?: boolean
  isCollapsable?: boolean
  disabled?: boolean
  group?: string
  callback?: () => void
  shortcut?: string
}
</script>

<template>
  <Tooltip :shortcut="shortcut" :text="ariaLabel">
    <!-- eslint-disable vuejs-accessibility/no-static-element-interactions -->
    <div @keydown.esc="showDropdown = false">
      <div class="flex flex-row">
        <button
          ref="button"
          :aria-label="ariaLabel"
          class="flex cursor-pointer p-8 text-blue-800 hover:bg-blue-200 focus:shadow-focus focus:outline-none disabled:bg-transparent disabled:text-gray-600"
          :class="{
            'bg-blue-200': isActive && !childButtons,
          }"
          :disabled="disabled"
          :tabindex="tabIndex"
          @click="onClickToggle(props)"
          @keydown.m="onClickToggle(props)"
          @mousedown.prevent=""
        >
          <component :is="icon" />
          <IconDropdown v-if="type === 'menu'" class="-mr-8" />
        </button>
        <div v-if="isLast" class="h-24 w-1 self-center bg-blue-300"></div>
      </div>
      <div
        v-if="showDropdown"
        class="absolute z-50 mt-1 flex flex-row items-center border-1 border-solid border-blue-800 bg-white"
      >
        <div v-for="(childButton, index) in childButtons" :key="index">
          <Tooltip
            :shortcut="childButton.shortcut"
            :text="childButton.ariaLabel"
          >
            <button
              ref="children"
              :aria-label="childButton.ariaLabel"
              class="hover:bg-blue-20 z-50 cursor-pointer items-center p-8 text-blue-900 focus:shadow-focus focus:outline-none disabled:bg-transparent disabled:text-gray-600"
              :class="{
                'bg-blue-200': isActive,
              }"
              :disabled="disabled"
              :tabindex="tabIndex"
              @click="emits('toggle', childButton)"
              @keydown.m="emits('toggle', childButton)"
              @mousedown.prevent=""
            >
              <component :is="childButton.icon" />
            </button>
          </Tooltip>
        </div>
      </div>
    </div>
  </Tooltip>
</template>
