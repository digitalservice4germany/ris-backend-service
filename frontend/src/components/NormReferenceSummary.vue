<script lang="ts" setup>
import IconBadge from "@/components/IconBadge.vue"
import NormReference from "@/domain/normReference"
import IconError from "~icons/ic/baseline-error"
import IconBook from "~icons/material-symbols/book-2"
import IconBreakingNews from "~icons/material-symbols/breaking-news"
import IconArrowRight from "~icons/material-symbols/subdirectory-arrow-right"

const props = defineProps<{
  data: NormReference
}>()
</script>

<template>
  <div class="flex w-full justify-between">
    <div class="flex flex-row items-center">
      <div
        v-if="data.singleNorms?.length === 1"
        class="flex flex-row items-center"
      >
        <component :is="IconBook" class="mr-8" />
        <div class="ds-label-01-reg mr-8">
          {{ data.renderDecision + ", " + data.singleNorms[0].renderDecision }}
        </div>
        <div
          v-if="data.singleNorms[0].legalForce"
          class="flex flex-row items-center"
        >
          {{ " | " }}
          <component :is="IconBreakingNews" class="mx-8" />
          {{ data.singleNorms[0].renderLegalForce }}
          <IconBadge
            v-if="data.singleNorms[0].legalForce?.hasMissingRequiredFields"
            background-color="bg-red-300"
            class="ml-8"
            color="text-red-900"
            :icon="IconError"
            label="Fehlende Daten"
          />
        </div>
        <IconBadge
          v-if="props.data?.hasAmbiguousNormReference"
          background-color="bg-red-300"
          color="text-red-900"
          :icon="IconError"
          label="Mehrdeutiger Verweis"
        />
      </div>
      <div v-else class="flex flex-col gap-24">
        <div class="flex flex-row items-center">
          <component :is="IconBook" class="mr-8" />
          <div class="ds-label-01-reg mr-8">
            {{ data.renderDecision }}
          </div>
          <IconBadge
            v-if="data.hasAmbiguousNormReference"
            background-color="bg-red-300"
            color="text-red-900"
            :icon="IconError"
            label="Mehrdeutiger Verweis"
          />
        </div>

        <div v-if="data.singleNorms?.length" class="flex flex-col gap-24">
          <div v-for="(singleNorm, index) in data.singleNorms" :key="index">
            <div v-if="!singleNorm.isEmpty" class="flex flex-row items-center">
              <component :is="IconArrowRight" class="mr-8" />
              {{
                data.renderDecision +
                ", " +
                data.singleNorms[index].renderDecision
              }}
              <div
                v-if="data.singleNorms[index].legalForce"
                class="flex flex-row items-center"
              >
                {{ " | " }}
                <component :is="IconBreakingNews" class="mx-8" />
                {{ data.singleNorms[index].renderLegalForce }}
                <IconBadge
                  v-if="
                    data.singleNorms[index].legalForce?.hasMissingRequiredFields
                  "
                  background-color="bg-red-300"
                  class="ml-8"
                  color="text-red-900"
                  :icon="IconError"
                  label="Fehlende Daten"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
