<script lang="ts" setup>
import DocumentUnitInfoPanel from "@/components/DocumentUnitInfoPanel.vue"
import DocumentUnit from "@/domain/documentUnit"
import KitchensinkPage from "@/kitchensink/components/KitchensinkPage.vue"

const documentUnit = new DocumentUnit("foo", {
  documentNumber: "1234567891234",
  coreData: {
    court: {
      type: "AG",
      location: "Test",
      label: "AG Test",
    },
  },
  shortTexts: {},
  longTexts: {},
  previousDecisions: undefined,
  ensuingDecisions: undefined,
  contentRelatedIndexing: {},
})
</script>

<template>
  <KitchensinkPage name="Document unit info panel">
    <DocumentUnitInfoPanel
      :document-unit="documentUnit"
      heading="XXRE202300002"
    />
  </KitchensinkPage>
</template>
