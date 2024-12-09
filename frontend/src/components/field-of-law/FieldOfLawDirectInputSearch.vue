<script lang="ts" setup>
import { ref, watch } from "vue"
import ComboboxInput from "@/components/ComboboxInput.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"
import ComboboxItemService from "@/services/comboboxItemService"

const emit = defineEmits<{
  "add-to-list": [item: FieldOfLaw]
}>()
const fieldOfLawNode = ref()
watch(fieldOfLawNode, () => {
  emit("add-to-list", fieldOfLawNode.value as FieldOfLaw)
})
</script>

<template>
  <div class="flex w-full flex-col">
    <p class="ds-label-02-reg pb-4">Direkteingabe Sachgebiet</p>
    <ComboboxInput
      id="directInputCombobox"
      v-model="fieldOfLawNode"
      aria-label="Direkteingabe-Sachgebietssuche eingeben"
      clear-on-choosing-item
      :item-service="ComboboxItemService.getFieldOfLawSearchByIdentifier"
      placeholder="Sachgebiet"
    >
    </ComboboxInput>
  </div>
</template>
