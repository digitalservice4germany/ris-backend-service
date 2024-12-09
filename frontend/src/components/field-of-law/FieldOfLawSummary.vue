<script lang="ts" setup>
import TokenizeText from "@/components/TokenizeText.vue"
import Tooltip from "@/components/Tooltip.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"
import MaterialSymbolsClose from "~icons/material-symbols/close"

defineProps<{
  fieldsOfLaw: FieldOfLaw[]
}>()

const emit = defineEmits<{
  "node:remove": [node: FieldOfLaw]
  "node:clicked": [node: FieldOfLaw]
}>()

function removeFieldOfLaw(fieldOfLaw: FieldOfLaw) {
  emit("node:remove", fieldOfLaw)
}

function fieldOfLawClicked(fieldOfLaw: FieldOfLaw) {
  emit("node:clicked", fieldOfLaw)
}
</script>

<template>
  <div class="flex w-full justify-between">
    <div class="flex w-full flex-col">
      <div
        v-for="fieldOfLaw in fieldsOfLaw"
        :key="fieldOfLaw.identifier"
        class="field-of-law flex w-full flex-row items-center border-b-1 border-blue-300 py-16 first:mt-16 first:border-t-1"
      >
        <div class="ds-label-01-reg mr-8 flex-grow">
          <button
            :aria-label="
              fieldOfLaw.identifier +
              ' ' +
              fieldOfLaw.text +
              ' im Sachgebietsbaum anzeigen'
            "
            class="ds-link-01-bold mr-8"
            @click="fieldOfLawClicked(fieldOfLaw)"
          >
            {{ fieldOfLaw.identifier }}
          </button>

          <TokenizeText
            :keywords="fieldOfLaw.linkedFields ?? []"
            :text="fieldOfLaw.text"
            @linked-field:clicked="fieldOfLawClicked"
          />
        </div>

        <Tooltip text="Entfernen">
          <button
            :aria-label="
              fieldOfLaw.identifier +
              ' ' +
              fieldOfLaw.text +
              ' aus Liste entfernen'
            "
            class="flex items-center justify-center text-blue-800 hover:bg-blue-100 focus:shadow-[inset_0_0_0_0.125rem] focus:shadow-blue-800 focus:outline-none"
            @click="removeFieldOfLaw(fieldOfLaw)"
            @keypress.enter="removeFieldOfLaw(fieldOfLaw)"
          >
            <MaterialSymbolsClose />
          </button>
        </Tooltip>
      </div>
    </div>
  </div>
</template>
