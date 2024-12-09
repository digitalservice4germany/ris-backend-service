import { computed, FunctionalComponent, SVGAttributes } from "vue"
import DocumentUnit from "@/domain/documentUnit"
import { Label, PublicationState } from "@/domain/publicationStatus"

export interface Badge {
  label: string
  icon?: FunctionalComponent<SVGAttributes>
  color: string
  backgroundColor: string
}

export function useStatusBadge(status: DocumentUnit["status"]) {
  const badge: Badge = {
    label: "",
    icon: undefined,
    color: "black",
    backgroundColor: "white",
  }

  return computed(() => {
    if (!status) return badge

    switch (status.publicationStatus) {
      case PublicationState.PUBLISHED:
        badge.label = Label.PUBLISHED
        badge.backgroundColor = "bg-green-300"
        break
      case PublicationState.UNPUBLISHED:
        badge.label = Label.UNPUBLISHED
        badge.backgroundColor = "bg-blue-300"
        break
      case PublicationState.PUBLISHING:
        badge.label = Label.PUBLISHING
        badge.backgroundColor = "bg-orange-300"
        break
      case PublicationState.DUPLICATED:
        badge.label = Label.DUPLICATED
        badge.backgroundColor = "bg-red-300"
        break
      case PublicationState.LOCKED:
        badge.label = Label.LOCKED
        badge.backgroundColor = "bg-red-300"
        break
      case PublicationState.DELETING:
        badge.label = Label.DELETING
        badge.backgroundColor = "bg-red-300"
        break
      case PublicationState.EXTERNAL_HANDOVER_PENDING:
        badge.label = Label.EXTERNAL_HANDOVER_PENDING
        badge.backgroundColor = "bg-orange-300"
        break
    }
    return badge
  })
}
