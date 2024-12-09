<script lang="ts" setup>
import { computed } from "vue"
import CategoryWrapper from "@/components/CategoryWrapper.vue"
import CollectiveAgreements from "@/components/CollectiveAgreements.vue"
import DismissalInputs from "@/components/DismissalInputs.vue"
import JobProfiles from "@/components/JobProfiles.vue"
import LegislativeMandate from "@/components/LegislativeMandate.vue"
import { useInjectCourtType } from "@/composables/useCourtType"
import constitutionalCourtTypes from "@/data/constitutionalCourtTypes.json"
import laborCourtTypes from "@/data/laborCourtTypes.json"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

const store = useDocumentUnitStore()
const courtTypeRef = useInjectCourtType()

const contentRelatedIndexing = computed({
  get: () => store.documentUnit!.contentRelatedIndexing,
  set: (newValues) => {
    store.documentUnit!.contentRelatedIndexing = newValues
  },
})

const hasCollectiveAgreement = computed<boolean>(
  () =>
    !!contentRelatedIndexing.value.collectiveAgreements &&
    contentRelatedIndexing.value.collectiveAgreements?.length > 0,
)
const hasDismissalInput = computed<boolean>(
  () =>
    (!!contentRelatedIndexing.value.dismissalTypes &&
      contentRelatedIndexing.value.dismissalTypes?.length > 0) ||
    (!!contentRelatedIndexing.value.dismissalGrounds &&
      contentRelatedIndexing.value.dismissalGrounds?.length > 0),
)
const hasJobProfiles = computed<boolean>(() =>
  contentRelatedIndexing.value.jobProfiles
    ? contentRelatedIndexing.value.jobProfiles?.length > 0
    : false,
)
const hasLegislativeMandate = computed(() => {
  return contentRelatedIndexing.value.hasLegislativeMandate
})

const shouldDisplayLegislativeMandateCategory = computed(() => {
  return (
    constitutionalCourtTypes.items.includes(courtTypeRef.value) ||
    hasLegislativeMandate.value
  )
})

const isLaborCourt = computed(() =>
  laborCourtTypes.items.includes(courtTypeRef.value),
)

const shouldDisplayDismissalAttributes = computed(
  () => isLaborCourt.value || hasDismissalInput.value,
)

const shouldDisplayCollectiveAgreements = computed(
  () => isLaborCourt.value || hasCollectiveAgreement.value,
)
</script>

<template>
  <div aria-label="Weitere Rubriken">
    <h2 class="ds-label-01-bold mb-16">Weitere Rubriken</h2>
    <div class="flex flex-col gap-24">
      <CategoryWrapper
        v-if="shouldDisplayCollectiveAgreements"
        v-slot="slotProps"
        label="Tarifvertrag"
        :should-show-button="!hasCollectiveAgreement"
      >
        <CollectiveAgreements label="Tarifvertrag" @reset="slotProps.reset" />
      </CategoryWrapper>
      <CategoryWrapper
        v-if="shouldDisplayDismissalAttributes"
        label="Kündigung"
        :should-show-button="!hasDismissalInput"
      >
        <DismissalInputs />
      </CategoryWrapper>
      <CategoryWrapper label="Berufsbild" :should-show-button="!hasJobProfiles">
        <JobProfiles label="Berufsbild" />
      </CategoryWrapper>
      <CategoryWrapper
        v-if="shouldDisplayLegislativeMandateCategory"
        label="Gesetzgebungsauftrag"
        :should-show-button="!hasLegislativeMandate"
      >
        <LegislativeMandate
          headline="Gesetzgebungsauftrag"
          label="Gesetzgebungsauftrag vorhanden"
        />
      </CategoryWrapper>
    </div>
  </div>
</template>
