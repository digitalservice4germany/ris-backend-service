<script lang="ts" setup>
import EnsuingDecisions from "@/components/EnsuingDecisions.vue"
import { DocumentUnitCatagoriesEnum } from "@/components/enumDocumentUnitCatagories"
import PreviousDecisions from "@/components/PreviousDecisions.vue"
import TitleElement from "@/components/TitleElement.vue"
</script>

<template>
  <div class="flex flex-col gap-24 bg-white p-24">
    <TitleElement>Rechtszug</TitleElement>
    <PreviousDecisions :id="DocumentUnitCatagoriesEnum.PROCEEDINGS_DECISIONS" />
    <EnsuingDecisions />
  </div>
</template>
