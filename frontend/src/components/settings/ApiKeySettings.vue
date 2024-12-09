<script setup lang="ts">
import dayjs from "dayjs"
import { ref, onMounted } from "vue"
import CopyableLabel from "@/components/CopyableLabel.vue"
import FlexContainer from "@/components/FlexContainer.vue"
import TextButton from "@/components/input/TextButton.vue"
import { ApiKey } from "@/domain/apiKey"
import authService from "@/services/authService"

const apiKey = ref<ApiKey>()

async function generateApiKey() {
  const response = await authService.generateImportApiKey()
  if (response.data) apiKey.value = response.data
}

async function invalidateApiKey() {
  if (apiKey.value?.apiKey) {
    const response = await authService.invalidateImportApiKey(
      apiKey.value?.apiKey,
    )
    if (response.data) apiKey.value = response.data
  }
}

onMounted(async () => {
  const response = await authService.getImportApiKey()
  if (response.data) apiKey.value = response.data
})
</script>

<template>
  <FlexContainer flex-direction="flex-col">
    <span class="ds-label-02-bold">API Key</span>
    <div v-if="apiKey">
      <div class="ds-body-01-reg mt-24">
        <CopyableLabel
          v-if="apiKey.valid"
          name="API Key"
          :text="apiKey.apiKey"
        />
        <div
          v-if="apiKey.valid"
          class="ds-label-02-reg-italic mt-24 text-gray-900"
        >
          gültig bis:
          {{ dayjs(apiKey.validUntil).format("DD.MM.YYYY HH:mm:ss") }}
        </div>
        <div v-else class="ds-label-02-bold mt-24 text-red-900">
          API-Key ist abgelaufen!
        </div>
      </div>
      <div v-if="apiKey.valid">
        <TextButton
          class="mt-20"
          label="Sperren"
          @click="invalidateApiKey"
        ></TextButton>
      </div>
      <div v-else>
        <TextButton
          class="mt-20"
          label="Neuen API-Schlüssel erstellen"
          @click="generateApiKey"
        ></TextButton>
      </div>
    </div>
    <div v-else>
      <TextButton
        class="mt-20"
        label="Neuen API-Schlüssel erstellen"
        @click="generateApiKey"
      ></TextButton>
    </div>
  </FlexContainer>
</template>
