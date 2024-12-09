<script setup lang="ts">
import { ref } from "vue"
import Tooltip from "@/components/Tooltip.vue"
import IconBaselineContentCopy from "~icons/ic/baseline-content-copy"
import IconCheckRounded from "~icons/material-symbols/check-rounded"

const props = withDefaults(
  defineProps<{
    /** Visible text. */
    text: string

    /**
     * Value that should be copied. If no value is provided, copying will
     * copy the `text` by default.
     */
    value?: string

    /**
     * Human-readable description of the value that should be copied. This
     * will be used to provide an accessible label for the control.
     *
     * @default "Wert"
     */
    name?: string
  }>(),
  {
    value: undefined,
    name: "Wert",
  },
)

const copySuccess = ref(false)

async function copy() {
  try {
    await navigator.clipboard.writeText(props.value ?? props.text)
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 1000)
  } catch (err) {
    console.error(err)
  }
}
</script>

<template>
  <button
    :aria-label="`${name} in die Zwischenablage kopieren`"
    class="inline-flex items-center gap-8 text-left"
    :title="`${name} in die Zwischenablage kopieren`"
    type="button"
    @click="copy"
  >
    {{ text }}
    <Tooltip text="Kopieren">
      <IconBaselineContentCopy
        v-if="!copySuccess"
        class="flex-none text-blue-900"
      />
      <IconCheckRounded v-else class="flex-none text-blue-900" />
    </Tooltip>
  </button>
</template>
