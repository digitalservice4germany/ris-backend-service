<script lang="ts" setup>
import { createNode, FieldOfLaw } from "@/domain/fieldOfLaw"

interface Props {
  text: string
  keywords: string[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  "linked-field:clicked": [node: FieldOfLaw]
}>()

type Token = {
  content: string
  isLink: boolean
}

function tokenizeText(): Token[] {
  if (props.keywords.length === 0) {
    return [{ content: props.text, isLink: false }]
  }
  return props.text
    .split(new RegExp(`(${props.keywords.join("|")})`))
    .map((part) => ({
      content: part,
      isLink: props.keywords.includes(part),
    }))
}

function handleTokenClick(token: Token) {
  if (!token.isLink) return
  const toNode = createNode(token.content)
  emit("linked-field:clicked", toNode)
}
</script>

<template>
  <!-- eslint-disable vue/require-v-for-key -->
  <!-- eslint-disable-next-line vuejs-accessibility/no-static-element-interactions -->
  <span
    v-for="token in tokenizeText()"
    class="text-left"
    :class="token.isLink && 'linked-field'"
    :tabindex="token.isLink ? 0 : undefined"
    @click="handleTokenClick(token)"
    @keyup.enter="handleTokenClick(token)"
  >
    {{ token.content }}
  </span>
</template>

<style lang="scss" scoped>
.linked-field {
  cursor: pointer;
  text-decoration: underline;
}
</style>
