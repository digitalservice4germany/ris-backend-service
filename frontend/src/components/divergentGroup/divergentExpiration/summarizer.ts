import { createTextVNode, VNode } from "vue"
import {
  divergentDefinedSummary,
  divergentUndefinedSummary,
} from "@/components/divergentGroup/divergentSummaryFunctions"
import { MetadataSections } from "@/domain/Norm"

export function divergentExpirationSummarizer(data?: MetadataSections): VNode {
  if (!data) return createTextVNode("")

  if (data.DIVERGENT_EXPIRATION_DEFINED) {
    return divergentDefinedSummary(data.DIVERGENT_EXPIRATION_DEFINED[0])
  } else if (data.DIVERGENT_EXPIRATION_UNDEFINED) {
    return divergentUndefinedSummary(data.DIVERGENT_EXPIRATION_UNDEFINED[0])
  } else return createTextVNode("")
}
