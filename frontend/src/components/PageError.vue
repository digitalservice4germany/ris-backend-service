<script setup lang="ts">
import TextButton from "@/components/input/TextButton.vue"
import router from "@/router"

interface Props {
  title?: string
  description?: string
  backButtonLabel?: string
  backRouterName?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: undefined,
  description: undefined,
  backButtonLabel: "Zurück zur Startseite",
  backRouterName: "caselaw",
})
</script>

<template>
  <div class="ml-48 mt-48 gap-16 p-16">
    <h1 class="ds-heading-02-reg mb-32">{{ props.title }}</h1>
    <p class="mb-32">{{ props.description }}</p>
    <p class="mb-16">
      <strong>Haben Sie die Internetadresse direkt eingegeben?</strong><br />
      Überprüfen Sie die Schreibweise.
    </p>
    <p class="mb-32">
      <strong>Haben Sie die Internetadresse kopiert?</strong><br />
      Überprüfen Sie, ob Sie die gesamte Adresse kopiert haben.
    </p>
    <TextButton
      :aria-label="backButtonLabel"
      class="w-auto"
      :label="backButtonLabel"
      @click="
        router.push({
          name: backRouterName,
        })
      "
    />
  </div>
</template>
