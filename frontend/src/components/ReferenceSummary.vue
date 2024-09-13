<script lang="ts" setup>
import { computed } from "vue"
import IconBadge from "@/components/IconBadge.vue"
import Reference from "@/domain/reference"

const props = defineProps<{
  data: Reference
}>()

const primaryReference = computed(() =>
  props.data?.legalPeriodical && props.data?.legalPeriodical.primaryReference
    ? "primär"
    : "sekundär",
)
const hasAmbiguousLegalPeriodicalAbbreviation = computed(
  () => !props.data?.legalPeriodical && !!props.data?.legalPeriodicalRawValue,
)
</script>

<template>
  <div class="flex w-full justify-between">
    <div class="flex flex-row items-center">
      <div class="ds-label-01-reg mr-8">
        {{ data.renderDecision }}
      </div>
      <IconBadge
        background-color="bg-blue-300"
        class="mr-8"
        color="text-blue-900"
        :label="primaryReference"
      />
      <IconBadge
        v-if="hasAmbiguousLegalPeriodicalAbbreviation"
        background-color="bg-red-300"
        color="text-red-900"
        label="Mehrdeutiger Verweis"
      />
    </div>
  </div>
</template>
