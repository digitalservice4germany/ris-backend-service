<script lang="ts" setup>
import { computed } from "vue"
import IconBadge from "@/components/IconBadge.vue"
import EditableListItem from "@/domain/editableListItem"
import IconError from "~icons/ic/baseline-error"
import IcBaselinePerson from "~icons/ic/baseline-person"

const props = defineProps<{
  data: EditableListItem
}>()

const iconComponent = computed(() => {
  return IcBaselinePerson
})

const showErrorBadge = computed(() => {
  return props.data?.hasMissingRequiredFields
})
</script>

<template>
  <div class="flex w-full justify-between">
    <div class="flex flex-row items-center">
      <component :is="iconComponent" class="mr-8" />
      <div class="ds-label-01-reg mr-8">{{ data?.renderDecision }}</div>
      <IconBadge
        v-if="showErrorBadge"
        background-color="bg-red-300"
        color="text-red-900"
        :icon="IconError"
        label="Fehlende Daten"
      />
    </div>
  </div>
</template>
