<script setup lang="ts">
import ActiveCitations from "@/components/ActiveCitations.vue"
import FieldOfLawMain from "@/components/FieldOfLawMain.vue"
import JobProfiles from "@/components/JobProfiles.vue"
import KeyWords from "@/components/KeyWords.vue"
import Norms from "@/components/NormReferences.vue"
</script>

<template>
  <div class="flex flex-col bg-white">
    <h2 class="ds-heading-03-bold me-32 ms-32 mt-32">
      Inhaltliche Erschließung
    </h2>
    <KeyWords />
    <FieldOfLawMain />
    <Norms />
    <ActiveCitations />
    <JobProfiles />
  </div>
</template>
