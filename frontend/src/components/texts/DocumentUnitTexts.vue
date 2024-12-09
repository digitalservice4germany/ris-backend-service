<script lang="ts" setup>
import DocumentUnitAdditionalTexts from "@/components/texts/DocumentUnitAdditionalTexts.vue"
import DocumentUnitLongTexts from "@/components/texts/DocumentUnitLongTexts.vue"
import DocumentUnitShortTexts from "@/components/texts/DocumentUnitShortTexts.vue"
import TitleElement from "@/components/TitleElement.vue"
import { useInternalUser } from "@/composables/useInternalUser"

const isInternalUser = useInternalUser()
</script>

<template>
  <div class="flex flex-col gap-24 bg-white p-24">
    <TitleElement>Kurz- & Langtexte</TitleElement>
    <DocumentUnitShortTexts />
    <DocumentUnitLongTexts v-if="isInternalUser" />
    <DocumentUnitAdditionalTexts v-if="isInternalUser" />
  </div>
</template>
