<script lang="ts" setup>
import { nextTick } from "vue"
import TextEditor from "../input/TextEditor.vue"
import CategoryWrapper from "@/components/CategoryWrapper.vue"
import { TextAreaInputAttributes } from "@/components/input/types"

interface Props {
  id: string
  label: string
  shouldShowButton: boolean
  modelValue?: string
  fieldSize?: TextAreaInputAttributes["fieldSize"]
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  fieldSize: "big",
})

defineEmits<{
  "update:modelValue": [value: string]
}>()

async function focusEditor() {
  await nextTick()
  const editorElement = document.getElementById(props.id)
    ?.firstElementChild as HTMLElement
  editorElement?.focus()
}
</script>

<template>
  <CategoryWrapper
    :label="label"
    :should-show-button="shouldShowButton"
    @toggled="focusEditor"
  >
    <div class="flex flex-col">
      <label class="ds-label-02-reg mb-4" :for="id">{{ label }}</label>

      <TextEditor
        :id="id"
        :aria-label="label"
        class="shadow-blue focus-within:shadow-focus hover:shadow-hover"
        editable
        :field-size="fieldSize"
        :value="modelValue"
        @update-value="$emit('update:modelValue', $event)"
      />
    </div>
  </CategoryWrapper>
</template>
