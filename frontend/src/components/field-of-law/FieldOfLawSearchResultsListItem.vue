<script lang="ts" setup>
import TokenizeText from "@/components/TokenizeText.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"

defineProps<{
  fieldOfLaw: FieldOfLaw
}>()

const emit = defineEmits<{
  "node:add": [node: FieldOfLaw]
  "linked-field:clicked": [node: FieldOfLaw]
}>()
</script>

<template>
  <div class="flex flex-row items-start gap-8 px-16 py-8">
    <button
      :aria-label="fieldOfLaw.identifier + ' hinzufügen'"
      class="ds-link-02-bold"
      tabindex="0"
      @click="emit('node:add', fieldOfLaw)"
      @keyup.enter="emit('node:add', fieldOfLaw)"
    >
      <span class="overflow-hidden text-ellipsis whitespace-nowrap">{{
        fieldOfLaw.identifier
      }}</span>
    </button>
    <span class="ds-label-02-reg mt-2">
      <TokenizeText
        :keywords="fieldOfLaw.linkedFields ?? []"
        :text="fieldOfLaw.text"
        @linked-field:clicked="emit('linked-field:clicked', $event)"
    /></span>
  </div>
</template>
