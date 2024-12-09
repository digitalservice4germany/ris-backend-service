import { useStatusBadge } from "@/composables/useStatusBadge"
import { PublicationState } from "@/domain/publicationStatus"

describe.each([
  {
    status: PublicationState.UNPUBLISHED,
    withError: undefined,
    expected: "Unveröffentlicht",
  },
  {
    status: PublicationState.UNPUBLISHED,
    withError: false,
    expected: "Unveröffentlicht",
  },
  {
    status: PublicationState.UNPUBLISHED,
    withError: true,
    expected: "Unveröffentlicht",
  },
  {
    status: PublicationState.PUBLISHING,
    withError: undefined,
    expected: "In Veröffentlichung",
  },
  {
    status: PublicationState.PUBLISHING,
    withError: false,
    expected: "In Veröffentlichung",
  },
  {
    status: PublicationState.PUBLISHING,
    withError: true,
    expected: "In Veröffentlichung",
  },
  {
    status: PublicationState.PUBLISHED,
    withError: undefined,
    expected: "Veröffentlicht",
  },
  {
    status: PublicationState.PUBLISHED,
    withError: false,
    expected: "Veröffentlicht",
  },
  {
    status: PublicationState.PUBLISHED,
    withError: true,
    expected: "Veröffentlicht",
  },
  {
    status: PublicationState.DUPLICATED,
    withError: undefined,
    expected: "Dublette",
  },
  {
    status: PublicationState.DUPLICATED,
    withError: false,
    expected: "Dublette",
  },
  {
    status: PublicationState.DUPLICATED,
    withError: true,
    expected: "Dublette",
  },
  {
    status: PublicationState.LOCKED,
    withError: undefined,
    expected: "Gesperrt",
  },
  { status: PublicationState.LOCKED, withError: false, expected: "Gesperrt" },
  { status: PublicationState.LOCKED, withError: true, expected: "Gesperrt" },
  {
    status: PublicationState.DELETING,
    withError: undefined,
    expected: "Löschen",
  },
  { status: PublicationState.DELETING, withError: false, expected: "Löschen" },
  { status: PublicationState.DELETING, withError: true, expected: "Löschen" },
  {
    status: PublicationState.EXTERNAL_HANDOVER_PENDING,
    withError: undefined,
    expected: "Fremdanlage",
  },
  {
    status: PublicationState.EXTERNAL_HANDOVER_PENDING,
    withError: true,
    expected: "Fremdanlage",
  },
  {
    status: PublicationState.EXTERNAL_HANDOVER_PENDING,
    withError: false,
    expected: "Fremdanlage",
  },
])("useStatusBadge", ({ status, withError, expected }) => {
  test(`'${status}' with error '${withError}' should return '${expected}'`, () => {
    const publicationStatus = {
      publicationStatus: status,
      withError: withError,
    }

    const statusBadge = useStatusBadge(publicationStatus)

    expect(statusBadge.value.label).toBe(expected)
  })
})
