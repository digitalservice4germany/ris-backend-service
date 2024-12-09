<script lang="ts" setup>
import { watch, ref } from "vue"

const props = defineProps<{
  title: string
  xml: string
}>()

const getCodeLines = (): string[] => {
  if (props.xml.includes("<?xml")) {
    return props.xml.split("\n").filter((line) => line.length > 0)
  }
  return []
}
const codeLines = ref(getCodeLines())
watch(
  () => props.xml,
  () => {
    codeLines.value = getCodeLines()
  },
)
</script>

<!-- TODO use tiptap with XML plugin instead of building it ourselves -->
<template>
  <div class="xml-container flex flex-col gap-4 text-base">
    <p class="ds-label-01-bold">{{ title }}</p>
    <div class="border-1 border-solid border-white" data-testid="code-snippet">
      <div v-for="(line, index) in codeLines" :key="index" class="code-line">
        <code
          class="line-number"
          :style="{
            'min-width': `${codeLines.length.toString().length * 15}px`,
          }"
          ><span>{{ index + 1 }}</span></code
        >
        <code class="line"
          ><span>{{ line }}</span></code
        >
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.xml-container {
  .code-line {
    display: flex;
    box-sizing: border-box;
    flex-direction: row;
    justify-content: flex-start;
    row-gap: 10px;

    .line-number {
      display: flex;
      justify-content: flex-end;
      padding-right: 5px;
      background-color: #ebecf0;
      user-select: none;
    }

    .line {
      padding-left: 20px;
      overflow-wrap: break-word;
      white-space: pre-wrap;
    }
  }
}
</style>
