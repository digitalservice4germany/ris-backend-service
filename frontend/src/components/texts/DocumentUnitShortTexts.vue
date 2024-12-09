<script lang="ts" setup>
import { computed } from "vue"
import TextEditorCategory from "@/components/texts/TextEditorCategory.vue"
import TextInputCategory from "@/components/texts/TextInputCategory.vue"
import { useValidBorderNumberLinks } from "@/composables/useValidBorderNumberLinks"
import { shortTextLabels } from "@/domain/documentUnit"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import TextEditorUtil from "@/utils/textEditorUtil"

const store = useDocumentUnitStore()

const decisionName = computed({
  get: () => store.documentUnit?.shortTexts.decisionName,
  set: (newValue) => {
    store.documentUnit!.shortTexts.decisionName =
      TextEditorUtil.getEditorContentIfPresent(newValue)
  },
})

const headline = computed({
  get: () =>
    store.documentUnit?.shortTexts.headline
      ? useValidBorderNumberLinks(
          store.documentUnit.shortTexts.headline,
          store.documentUnit.managementData.borderNumbers,
        )
      : undefined,
  set: (newValue) => {
    store.documentUnit!.shortTexts.headline =
      TextEditorUtil.getEditorContentIfPresent(newValue)
  },
})

const guidingPrinciple = computed({
  get: () =>
    store.documentUnit?.shortTexts.guidingPrinciple
      ? useValidBorderNumberLinks(
          store.documentUnit?.shortTexts.guidingPrinciple,
          store.documentUnit.managementData.borderNumbers,
        )
      : undefined,
  set: (newValue) => {
    store.documentUnit!.shortTexts.guidingPrinciple =
      TextEditorUtil.getEditorContentIfPresent(newValue)
  },
})

const headnote = computed({
  get: () =>
    store.documentUnit?.shortTexts.headnote
      ? useValidBorderNumberLinks(
          store.documentUnit?.shortTexts.headnote,
          store.documentUnit.managementData.borderNumbers,
        )
      : undefined,
  set: (newValue) => {
    store.documentUnit!.shortTexts.headnote =
      TextEditorUtil.getEditorContentIfPresent(newValue)
  },
})

const otherHeadnote = computed({
  get: () =>
    store.documentUnit?.shortTexts.otherHeadnote
      ? useValidBorderNumberLinks(
          store.documentUnit?.shortTexts.otherHeadnote,
          store.documentUnit.managementData.borderNumbers,
        )
      : undefined,
  set: (newValue) => {
    store.documentUnit!.shortTexts.otherHeadnote =
      TextEditorUtil.getEditorContentIfPresent(newValue)
  },
})
</script>

<template>
  <div aria-label="Kurztexte">
    <h2 class="ds-label-01-bold mb-16">Kurztexte</h2>
    <div class="flex flex-col gap-24">
      <TextInputCategory
        id="decisionName"
        v-model="decisionName"
        :data-testid="shortTextLabels.decisionName"
        editable
        :label="shortTextLabels.decisionName"
        :should-show-button="
          !store.documentUnit?.shortTexts?.decisionName?.length
        "
      />

      <TextEditorCategory
        id="headline"
        v-model="headline"
        editable
        field-size="small"
        :label="shortTextLabels.headline"
        :should-show-button="!store.documentUnit?.shortTexts?.headline?.length"
      />

      <TextEditorCategory
        id="guidingPrinciple"
        v-model="guidingPrinciple"
        editable
        :label="shortTextLabels.guidingPrinciple"
        :should-show-button="
          !store.documentUnit?.shortTexts?.guidingPrinciple?.length
        "
      />

      <TextEditorCategory
        id="headnote"
        v-model="headnote"
        editable
        :label="shortTextLabels.headnote"
        :should-show-button="!store.documentUnit?.shortTexts?.headnote?.length"
      />

      <TextEditorCategory
        id="otherHeadnote"
        v-model="otherHeadnote"
        editable
        :label="shortTextLabels.otherHeadnote"
        :should-show-button="
          !store.documentUnit?.shortTexts?.otherHeadnote?.length
        "
      />
    </div>
  </div>
</template>
