<script setup lang="ts">
import FlexContainer from "@/components/FlexContainer.vue"
import AccountManagementSettings from "@/components/settings/AccountManagementSettings.vue"
import ApiKeySettings from "@/components/settings/ApiKeySettings.vue"
</script>

<template>
  <FlexContainer flex-direction="flex-col">
    <h3 class="ds-heading-03-bold ml-64 mt-48">Einstellungen</h3>
    <div class="mt-56 min-h-screen bg-gray-100 pl-64 pt-56" v-bind="$attrs">
      <AccountManagementSettings />
      <ApiKeySettings class="mt-32" />
    </div>
  </FlexContainer>
</template>
