<script lang="ts" setup>
import { useRouter } from "vue-router"
import DocumentUnitSearch from "@/components/DocumentUnitSearch.vue"
import TextButton from "@/components/input/TextButton.vue"
import { useInternalUser } from "@/composables/useInternalUser"

const router = useRouter()
const isInternalUser = useInternalUser()
</script>

<template>
  <div class="flex flex-col gap-16 p-16">
    <div class="flex justify-between">
      <h1 class="ds-heading-02-reg">Übersicht Rechtsprechung</h1>

      <TextButton
        v-if="isInternalUser"
        label="Neue Dokumentationseinheit"
        @click="router.push({ name: 'caselaw-documentUnit-new' })"
      />
    </div>
    <DocumentUnitSearch />
  </div>
</template>
