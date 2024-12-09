<script lang="ts" setup>
import PageError from "@/components/PageError.vue"
import KitchensinkPage from "@/kitchensink/components/KitchensinkPage.vue"
</script>

<template>
  <KitchensinkPage name="Page Error">
    <PageError
      description="Optionale Beschreibung kommt hier hin"
      title="Optionale Beschreibung kommt hier hin"
    />
  </KitchensinkPage>
</template>
