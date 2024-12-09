<script setup lang="ts">
import { onMounted, ref } from "vue"
import FlexContainer from "@/components/FlexContainer.vue"
import TextButton from "@/components/input/TextButton.vue"
import adminService from "@/services/adminService"

const link = ref<string | undefined>(undefined)

async function fetchUrl() {
  link.value = (await adminService.getAccountManagementUrl()).data
}

onMounted(async () => {
  await fetchUrl()
})
</script>

<template>
  <FlexContainer v-if="link" flex-direction="flex-col">
    <span class="ds-label-02-bold">Kontoverwaltung</span>
    <TextButton
      class="mt-16"
      :href="link"
      label="Bare.ID öffnen"
      target="_blank"
    >
    </TextButton>
  </FlexContainer>
</template>
