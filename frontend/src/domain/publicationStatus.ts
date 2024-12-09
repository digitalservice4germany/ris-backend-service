export type PublicationStatus = {
  publicationStatus?: PublicationState
  withError?: boolean
}

export enum PublicationState {
  PUBLISHED = "PUBLISHED",
  UNPUBLISHED = "UNPUBLISHED",
  PUBLISHING = "PUBLISHING",
  DUPLICATED = "DUPLICATED",
  LOCKED = "LOCKED",
  DELETING = "DELETING",
  EXTERNAL_HANDOVER_PENDING = "EXTERNAL_HANDOVER_PENDING",
}

export const Label: Record<PublicationState, string> = {
  [PublicationState.PUBLISHED]: "Veröffentlicht",
  [PublicationState.UNPUBLISHED]: "Unveröffentlicht",
  [PublicationState.PUBLISHING]: "In Veröffentlichung",
  [PublicationState.DUPLICATED]: "Dublette",
  [PublicationState.LOCKED]: "Gesperrt",
  [PublicationState.DELETING]: "Löschen",
  [PublicationState.EXTERNAL_HANDOVER_PENDING]: "Fremdanlage",
}
