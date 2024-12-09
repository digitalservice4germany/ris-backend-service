<script setup lang="ts">
import ErrorPage from "@/components/PageError.vue"
import { PAGE_NOT_FIND } from "@/i18n/errors.json"

const errorTitle = PAGE_NOT_FIND.title
</script>

<template>
  <ErrorPage :title="errorTitle"></ErrorPage>
</template>
