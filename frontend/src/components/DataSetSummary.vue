<script lang="ts" setup generic="T">
import { computed, h, defineComponent } from "vue"
import type { VNode } from "vue"
import Ourselves from "@/components/DataSetSummary.vue"

const props = defineProps<{
  data: T | T[]
  summarizer?: (dataEntry: T) => string | VNode
}>()

const dataAsList = computed(() =>
  Array.isArray(props.data) ? props.data : [props.data],
)

const summarizer = computed(() => props.summarizer ?? defaultSummarizer)
const summaries = computed(() => dataAsList.value.map(wrappedSummarizer))

function wrappedSummarizer(dataEntry: T): VNode {
  const summary = summarizer.value(dataEntry)
  return typeof summary == "string" ? h("span", summary) : summary
}
</script>

<script lang="ts">
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function defaultSummarizer(dataEntry: any): string {
  if (["string", "boolean", "number"].includes(typeof dataEntry)) {
    return `${dataEntry}`
  } else if (Array.isArray(dataEntry)) {
    return dataEntry
      .map(defaultSummarizer)
      .filter((value) => value != "")
      .join(", ")
  } else if (typeof dataEntry == "object" && dataEntry !== null) {
    return Object.values(dataEntry)
      .map(defaultSummarizer)
      .filter((value) => value != "")
      .join(" | ")
  } else {
    return ""
  }
}

/**
 * Creates a new component variant of the DataSetSummary by attaching a fixed
 * summarizer function to it. The new component can be used as before by just
 * providing the `data`, but instead of the default summarizer, the provided one
 * is used. This higher order component generator works a little like bounding
 * a context to a function in JavaScript.
 * This is especially useful when the DataSetSummary component is passed as
 * property to another component.
 */
export function withSummarizer(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  summarizer: (dataEntry: any) => string | VNode,
) {
  return defineComponent({
    props: {
      data: {
        type: undefined,
        default: undefined,
      },
    },
    setup(props) {
      return () =>
        h(Ourselves, {
          ...props,
          summarizer,
        })
    },
  })
}
</script>

<template>
  <ul class="m-0 flex w-full flex-col items-start p-0 text-start">
    <li
      v-for="(summary, index) in summaries"
      :key="index"
      class="flex-start ds-label-01-reg flex w-full border-b-1 border-b-blue-300 py-10 first:pt-0 last:border-none last:pb-0"
    >
      <component :is="summary" />
    </li>
  </ul>
</template>
