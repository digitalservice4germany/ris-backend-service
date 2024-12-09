<script lang="ts" setup>
import { computed } from "vue"
import Tooltip from "./Tooltip.vue"
import IconChevronLeft from "~icons/ic/baseline-chevron-left"
import IconChevronRight from "~icons/ic/baseline-chevron-right"

interface Props {
  isExpanded?: boolean
  openingDirection?: OpeningDirection
  label?: string
  shortcut?: string
}

const props = withDefaults(defineProps<Props>(), {
  isExpanded: false,
  openingDirection: OpeningDirection.RIGHT,
  label: "side toggle",
  shortcut: undefined,
})

const emit = defineEmits<{
  "update:isExpanded": [value: boolean]
}>()

const postFix = computed(() => (props.isExpanded ? "schließen" : "öffnen"))

const classes = computed(() => ({
  "pl-24":
    props.openingDirection == OpeningDirection.RIGHT && !props.isExpanded,
  "pr-24": props.openingDirection == OpeningDirection.LEFT && !props.isExpanded,
}))

const buttonClasses = computed(() => ({
  "right-0 -mr-16": props.openingDirection == OpeningDirection.RIGHT,
  "left-0 -ml-16": props.openingDirection == OpeningDirection.LEFT,
}))

const toggle = () => {
  emit("update:isExpanded", !props.isExpanded)
}
</script>

<script lang="ts">
export enum OpeningDirection {
  LEFT = "left",
  RIGHT = "right",
}
</script>

<template>
  <div class="relative bg-white" :class="classes">
    <Tooltip
      class="!absolute top-16 z-20"
      :class="buttonClasses"
      :shortcut="shortcut"
      :text="props.label + ' ' + postFix"
    >
      <button
        :aria-label="props.label + ' ' + postFix"
        class="w-icon relative flex min-h-32 min-w-32 items-center justify-center rounded-full border-1 border-solid border-gray-400 bg-white text-gray-900"
        @click="toggle"
      >
        <IconChevronLeft
          v-if="
            props.openingDirection === OpeningDirection.LEFT
              ? !isExpanded
              : isExpanded
          "
        />
        <IconChevronRight v-else />
      </button>
    </Tooltip>
    <div v-show="isExpanded">
      <slot />
    </div>
  </div>
</template>
