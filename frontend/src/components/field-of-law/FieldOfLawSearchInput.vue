<script setup lang="ts">
import InputField from "@/components/input/InputField.vue"
import TextButton from "@/components/input/TextButton.vue"
import TextInput from "@/components/input/TextInput.vue"
import IconSearch from "~icons/ic/baseline-search"

defineProps<{
  errorLabel?: string
}>()

const emit = defineEmits<{
  search: [void]
}>()

const identifier = defineModel<string>("identifier")
const description = defineModel<string>("description")
const norm = defineModel<string>("norm")
</script>

<template>
  <div class="flex w-full flex-col">
    <div class="flex w-full flex-row items-end gap-16">
      <InputField id="fieldOfLawIdentifierInput" label="Sachgebiet">
        <TextInput
          id="fieldOfLawIdentifierInput"
          v-model="identifier"
          aria-label="Sachgebietskürzel"
          size="medium"
          @enter-released="emit('search')"
        />
      </InputField>
      <InputField id="fieldOfLawDescriptionInput" label="Bezeichnung">
        <TextInput
          id="fieldOfLawDescriptionInput"
          v-model="description"
          aria-label="Sachgebietsbezeichnung"
          size="medium"
          @enter-released="emit('search')"
        />
      </InputField>
      <InputField id="fieldOfLawNormInput" label="Norm">
        <TextInput
          id="fieldOfLawNormInput"
          v-model="norm"
          aria-label="Sachgebietsnorm"
          size="medium"
          @enter-released="emit('search')"
        />
      </InputField>

      <TextButton
        aria-label="Sachgebietssuche ausführen"
        button-type="primary"
        :icon="IconSearch"
        @click="emit('search')"
      />
    </div>

    <span v-if="errorLabel" class="ds-label-03-reg min-h-[1rem] text-red-800">{{
      errorLabel
    }}</span>
  </div>
</template>
