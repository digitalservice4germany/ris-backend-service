<script lang="ts" setup>
import { computed, ref, watch } from "vue"
import IconAdd from "~icons/ic/baseline-add"
import IconHorizontalRule from "~icons/ic/baseline-horizontal-rule"

interface Props {
  header?: string
  headerClass?: string
  isExpanded?: boolean
  headerId?: string
  iconsOnLeft?: boolean
  marginLevel?: number
  preventExpandOnClick?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  header: undefined,
  isExpanded: false,
  headerId: "",
  headerClass: "",
  iconsOnLeft: false,
  marginLevel: 0,
})

const emit = defineEmits<{
  "update:isExpanded": [value: boolean]
}>()

const localIsExpanded = ref(false)

const ariaLabel = computed(() =>
  localIsExpanded.value ? "Zuklappen" : "Aufklappen",
)

function toggleContentVisibility(): void {
  if (props.preventExpandOnClick) return
  localIsExpanded.value = !localIsExpanded.value
}

function toggleContentVisibilityOnButton(event: Event): void {
  localIsExpanded.value = !localIsExpanded.value
  event.stopPropagation()
}

watch(
  () => props.isExpanded,
  () => (localIsExpanded.value = props.isExpanded ?? false),
  { immediate: true },
)

watch(localIsExpanded, () => emit("update:isExpanded", localIsExpanded.value))
</script>

<template>
  <!-- Ignore requirement to have a keyboard listener as it's only a convenience
  for mouse users, but keyboard users can already do the same thing by tabbing
  just fine -->
  <!-- eslint-disable vuejs-accessibility/click-events-have-key-events -->
  <div>
    <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
    <div
      :aria-labelledby="headerId"
      class="flex w-full items-center justify-between focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800"
      :class="{ 'cursor-pointer': !preventExpandOnClick }"
      @click.stop="toggleContentVisibility"
    >
      <button
        v-if="props.iconsOnLeft"
        :aria-label="ariaLabel"
        class="icon self-start text-blue-800 focus:outline-none focus-visible:outline-4 focus-visible:outline-offset-4 focus-visible:outline-blue-800"
        data-testid="icons-open-close"
        :style="{ marginLeft: `${marginLevel * 24}px` }"
        @click.stop="toggleContentVisibilityOnButton"
      >
        <slot v-if="localIsExpanded" name="close-icon">
          <IconHorizontalRule />
        </slot>
        <slot v-else name="open-icon">
          <IconAdd />
        </slot>
      </button>

      <slot name="header">
        <span :class="headerClass">{{ header }}</span>
      </slot>

      <button
        v-if="!props.iconsOnLeft"
        :aria-label="ariaLabel"
        class="text-blue-800 focus:outline-none focus-visible:outline-blue-800"
        data-testid="icons-open-close"
        @click.stop="toggleContentVisibilityOnButton"
      >
        <slot v-if="localIsExpanded" name="close-icon">
          <IconHorizontalRule />
        </slot>
        <slot v-else name="open-icon">
          <IconAdd />
        </slot>
      </button>
    </div>

    <div v-if="localIsExpanded">
      <slot />
    </div>
  </div>
</template>
