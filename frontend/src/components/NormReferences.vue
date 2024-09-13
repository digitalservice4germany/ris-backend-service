<script lang="ts" setup>
import { computed, h } from "vue"
import { withSummarizer } from "@/components/DataSetSummary.vue"
import EditableList from "@/components/EditableList.vue"
import IconBadge from "@/components/IconBadge.vue"
import NormReferenceInput from "@/components/NormReferenceInput.vue"
import NormReference from "@/domain/normReference"
import SingleNorm from "@/domain/singleNorm"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import IconError from "~icons/ic/baseline-error"
import IconBook from "~icons/material-symbols/book-2"
import IconBreakingNews from "~icons/material-symbols/breaking-news"
import IconArrowRight from "~icons/material-symbols/subdirectory-arrow-right"

const store = useDocumentUnitStore()

const norms = computed({
  get: () => store.documentUnit!.contentRelatedIndexing.norms,
  set: (newValues) => {
    store.documentUnit!.contentRelatedIndexing.norms = newValues
  },
})

const ambiguousNormReferenceError = "Mehrdeutiger Verweis"
const missingDataError = "Fehlende Daten"

const defaultValue = new NormReference() as NormReference

/**
 * Summarizer for a legal force
 * @param singleNorm The single Norm that has a legal force
 */
function legalForceSummarizer(singleNorm: SingleNorm) {
  return h("div", { class: ["flex flex-row items-center"] }, [
    h("div", {}, "|"),
    h(IconBreakingNews, { class: ["mr-8 ml-8"] }),
    h("div", { class: ["link-01-reg mr-8"] }, singleNorm.renderLegalForce),
    singleNorm.legalForce?.hasMissingRequiredFields
      ? errorBadgeSummarizer(missingDataError)
      : null,
  ])
}

/**
 * Returns a render function with an error icon badge
 @param errorLabel error message to be displayed in the badge.
 */
function errorBadgeSummarizer(errorLabel: string) {
  return h(IconBadge, {
    class: "mr-8",
    backgroundColor: "bg-red-300",
    color: "text-red-900",
    icon: IconError,
    label: errorLabel,
  })
}

/**
 * @deprecated Please look at the implementation of ActiveCitations.vue with summary-component template rendering
 * Summarizer for a normEntry. With no or only one single norm, it is rendered in one line, if more single norms present,
 * they are rendered as sub list.
 * @param normEntry Norm Entry to be summarized.
 */
function decisionSummarizer(normEntry: NormReference) {
  if (normEntry.singleNorms?.length === 1) {
    return h("div", { class: ["flex flex-col gap-32"] }, [
      h("div", { class: ["flex flex-row items-center"] }, [
        h(IconBook, { class: ["mr-8"] }),
        h(
          "div",
          { class: ["link-01-reg mr-8"] },
          normEntry.renderDecision + renderSingleNorm(normEntry.singleNorms[0]),
        ),
        normEntry.singleNorms[0].legalForce
          ? legalForceSummarizer(normEntry.singleNorms[0])
          : null,
        normEntry.hasAmbiguousNormReference
          ? errorBadgeSummarizer(ambiguousNormReferenceError)
          : null,
      ]),
    ])
  } else {
    return h("div", { class: ["flex flex-col gap-32"] }, [
      h("div", { class: ["flex flex-row items-center"] }, [
        h(IconBook, { class: ["mr-8"] }),
        h("div", { class: ["link-01-reg mr-8"] }, normEntry.renderDecision),
        normEntry.hasAmbiguousNormReference
          ? errorBadgeSummarizer(ambiguousNormReferenceError)
          : null,
      ]),
      normEntry.hasSingleNorms
        ? h(
            "div",
            { class: ["flex flex-col gap-32"] },
            normEntry.singleNorms?.map((singleNorm) => {
              return !singleNorm.isEmpty
                ? h("div", { class: ["flex flex-row items-center"] }, [
                    h(IconArrowRight, { class: ["mr-8"] }),
                    h(
                      "div",
                      { class: ["link-01-reg mr-8"] },
                      normEntry.renderDecision + renderSingleNorm(singleNorm),
                    ),
                    singleNorm.legalForce
                      ? legalForceSummarizer(singleNorm)
                      : null,
                  ])
                : null
            }),
          )
        : null,
    ])
  }
}

function renderSingleNorm(singleNorm: SingleNorm): string {
  return singleNorm.renderDecision.length > 0
    ? ", " + singleNorm.renderDecision
    : "" + singleNorm.renderDecision
}

const NormsSummary = withSummarizer(decisionSummarizer)
</script>
<template>
  <div aria-label="Norm" class="bg-white p-32">
    <h2 class="ds-heading-03-reg mb-24">Normen</h2>
    <div class="flex flex-row">
      <div class="flex-1">
        <EditableList
          v-model="norms"
          :default-value="defaultValue"
          :edit-component="NormReferenceInput"
          :summary-component="NormsSummary"
        />
      </div>
    </div>
  </div>
</template>
