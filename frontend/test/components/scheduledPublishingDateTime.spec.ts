import { createTestingPinia } from "@pinia/testing"
import { fireEvent, render, screen } from "@testing-library/vue"
import { setActivePinia } from "pinia"
import ScheduledPublishingDateTime from "@/components/ScheduledPublishingDateTime.vue"
import DocumentUnit from "@/domain/documentUnit"
import { RisJsonPatch } from "@/domain/risJsonPatch"
import { ServiceResponse } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import useSessionStore from "@/stores/sessionStore"

function mockDocUnitStore({
  scheduledPublicationDateTime,
  scheduledByEmail,
  errorTitle,
}: {
  scheduledPublicationDateTime?: string
  scheduledByEmail?: string
  errorTitle?: string
} = {}) {
  const mockedSessionStore = useDocumentUnitStore()
  mockedSessionStore.documentUnit = new DocumentUnit("q834", {
    managementData: {
      scheduledPublicationDateTime,
      scheduledByEmail,
      borderNumbers: [],
    },
  })

  const response = errorTitle ? { error: { title: errorTitle } } : {}
  vi.spyOn(mockedSessionStore, "updateDocumentUnit").mockResolvedValue(
    response as ServiceResponse<RisJsonPatch>,
  )

  return mockedSessionStore
}

describe("ScheduledPublishingDateTime", () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia())
    const sessionStore = useSessionStore()
    sessionStore.user = { name: "test user", email: "test@mail.local" }
  })

  it("should show empty default state if no date is set and doc unit is publishable", async () => {
    mockDocUnitStore()
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    const dateField = screen.getByLabelText("Terminiertes Datum")
    expect(dateField).toHaveValue("")
    expect(dateField).not.toHaveAttribute("readonly")
    expect(dateField).toBeEnabled()

    const timeField = screen.getByLabelText("Terminierte Uhrzeit")
    expect(timeField).toHaveValue("05:00")
    expect(timeField).not.toHaveAttribute("readonly")
    expect(timeField).toBeEnabled()

    expect(screen.getByLabelText("Termin setzen")).toBeDisabled()

    expect(
      screen.queryByTestId("scheduledPublishingDate_errors"),
    ).not.toBeInTheDocument()
  })

  it("should be disabled if docunit is not publishable and no scheduled date is set", async () => {
    mockDocUnitStore()
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: false },
    })

    const dateField = screen.getByLabelText("Terminiertes Datum")
    expect(dateField).toHaveValue("")
    expect(dateField).toBeDisabled()

    const timeField = screen.getByLabelText("Terminierte Uhrzeit")
    expect(timeField).toHaveValue("05:00")
    expect(dateField).toBeDisabled()

    expect(screen.getByLabelText("Termin setzen")).toBeDisabled()

    expect(
      screen.queryByTestId("scheduledPublishingDate_errors"),
    ).not.toBeInTheDocument()
  })

  it("should render previously set publishing date", () => {
    mockDocUnitStore({
      scheduledPublicationDateTime: "2080-10-10T23:00:00.000Z",
    })
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    const dateField = screen.getByLabelText("Terminiertes Datum")
    expect(dateField).toHaveValue("11.10.2080")
    expect(dateField).toHaveAttribute("readonly")

    const timeField = screen.getByLabelText("Terminierte Uhrzeit")
    expect(timeField).toHaveValue("01:00")
    expect(timeField).toHaveAttribute("readonly")

    expect(screen.queryByLabelText("Termin setzen")).not.toBeInTheDocument()

    expect(screen.queryByLabelText("Termin löschen")).toBeEnabled()

    expect(
      screen.queryByTestId("scheduledPublishingDate_errors"),
    ).not.toBeInTheDocument()
  })

  it("should set scheduling date", async () => {
    const store = mockDocUnitStore()
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    const dateField = screen.getByLabelText("Terminiertes Datum")
    await fireEvent.update(dateField, "01.01.2050")

    await fireEvent.click(screen.getByLabelText("Termin setzen"))

    expect(
      store.documentUnit?.managementData.scheduledPublicationDateTime,
    ).toEqual("2050-01-01T04:00:00.000Z")
    expect(store.documentUnit?.managementData.scheduledByEmail).toEqual(
      "test@mail.local",
    )

    expect(dateField).toHaveValue("01.01.2050")
    expect(screen.getByLabelText("Terminierte Uhrzeit")).toHaveValue("05:00")
    expect(screen.getByLabelText("Termin löschen")).toBeEnabled()

    expect(
      screen.queryByTestId("scheduledPublishingDate_errors"),
    ).not.toBeInTheDocument()
  })

  it("should reset state after deleting scheduling", async () => {
    const store = mockDocUnitStore({
      scheduledPublicationDateTime: "2080-10-11T02:00:00.000Z",
      scheduledByEmail: "other@example.com",
    })
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    await fireEvent.click(screen.getByLabelText("Termin löschen"))

    expect(
      store.documentUnit?.managementData.scheduledPublicationDateTime,
    ).toBeUndefined()
    expect(store.documentUnit?.managementData.scheduledByEmail).toBeUndefined()

    const dateField = screen.getByLabelText("Terminiertes Datum")
    expect(dateField).toHaveValue("")
    expect(dateField).toBeEnabled()
    expect(dateField).not.toHaveAttribute("readonly")

    const timeField = screen.getByLabelText("Terminierte Uhrzeit")
    expect(timeField).toHaveValue("05:00")
    expect(timeField).toBeEnabled()
    expect(timeField).not.toHaveAttribute("readonly")

    expect(screen.getByLabelText("Termin setzen")).toBeDisabled()

    expect(
      screen.queryByTestId("scheduledPublishingDate_errors"),
    ).not.toBeInTheDocument()
  })

  it("should show error on invalid date", async () => {
    mockDocUnitStore()
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    const dateField = screen.getByLabelText("Terminiertes Datum")
    await fireEvent.update(dateField, "40.01.2050")

    expect(dateField).toHaveValue("40.01.2050")
    expect(screen.getByLabelText("Termin setzen")).toBeDisabled()

    expect(
      screen.getByTestId("scheduledPublishingDate_errors"),
    ).toBeInTheDocument()
    expect(screen.getByText("Kein valides Datum.")).toBeVisible()
  })

  it("should show error on incomplete date", async () => {
    mockDocUnitStore()
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    const dateField = screen.getByLabelText("Terminiertes Datum")
    await fireEvent.update(dateField, "40.01.205")
    await fireEvent.blur(dateField)

    expect(dateField).toHaveValue("40.01.205")
    expect(screen.getByLabelText("Termin setzen")).toBeDisabled()

    expect(
      screen.getByTestId("scheduledPublishingDate_errors"),
    ).toBeInTheDocument()
    expect(screen.getByText("Unvollständiges Datum.")).toBeVisible()
  })

  it("should show error on invalid time", async () => {
    mockDocUnitStore()
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    const timeField = screen.getByLabelText("Terminierte Uhrzeit")
    await fireEvent.update(timeField, "invalid")
    await fireEvent.blur(timeField)

    expect(timeField).toHaveValue("")
    expect(screen.getByLabelText("Termin setzen")).toBeDisabled()

    expect(
      screen.getByTestId("scheduledPublishingDate_errors"),
    ).toBeInTheDocument()
    expect(screen.getByText("Unvollständige Uhrzeit.")).toBeVisible()
  })

  it("should show error when scheduling date_time is not in the future", async () => {
    mockDocUnitStore()
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    const dateField = screen.getByLabelText("Terminiertes Datum")
    await fireEvent.update(dateField, "01.01.2024")

    expect(dateField).toHaveValue("01.01.2024")

    expect(screen.getByLabelText("Termin setzen")).toBeDisabled()

    expect(
      screen.getByTestId("scheduledPublishingDate_errors"),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        "Der Terminierungszeitpunkt muss in der Zukunft liegen.",
      ),
    ).toBeVisible()
  })

  it("should not show error when date_time is not in the future, but already scheduled", async () => {
    mockDocUnitStore({
      scheduledPublicationDateTime: "2020-10-11T03:00:00.000Z",
    })
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    expect(
      screen.queryByTestId("scheduledPublishingDate_errors"),
    ).not.toBeInTheDocument()
  })

  it("should show error if scheduled date is set and doc unit is not publishable", () => {
    mockDocUnitStore({
      scheduledPublicationDateTime: "2080-10-11T03:00:00.000Z",
    })
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: false },
    })

    expect(screen.getByLabelText("Termin löschen")).toBeEnabled()

    expect(
      screen.getByTestId("scheduledPublishingDate_errors"),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        "Die terminierte Abgabe kann aufgrund von Fehlern in der Plausibilitätsprüfung nicht durchgeführt werden.",
      ),
    ).toBeVisible()
  })

  it("should show error if doc unit cannot be saved when deleting scheduling", async () => {
    mockDocUnitStore({
      scheduledPublicationDateTime: "2080-10-11T03:00:00.000Z",
      errorTitle: "Speichern leider fehlgeschlagen.",
    })
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    await fireEvent.click(screen.getByLabelText("Termin löschen"))

    expect(
      screen.queryByTestId("scheduledPublishingDate_errors"),
    ).not.toBeInTheDocument()
    expect(screen.getByText("Speichern leider fehlgeschlagen.")).toBeVisible()

    // Allow saving
    mockDocUnitStore()

    // New attempt resets error state
    await fireEvent.click(screen.getByLabelText("Termin löschen"))
    expect(
      screen.queryByText("Speichern leider fehlgeschlagen."),
    ).not.toBeInTheDocument()
  })

  it("should show error if doc unit cannot be saved when creating scheduling", async () => {
    mockDocUnitStore({
      errorTitle: "Speichern leider fehlgeschlagen.",
    })
    render(ScheduledPublishingDateTime, {
      props: { isPublishable: true },
    })

    const dateField = screen.getByLabelText("Terminiertes Datum")
    await fireEvent.update(dateField, "01.01.2050")
    await fireEvent.click(screen.getByLabelText("Termin setzen"))

    expect(
      screen.queryByTestId("scheduledPublishingDate_errors"),
    ).not.toBeInTheDocument()
    expect(screen.getByText("Speichern leider fehlgeschlagen.")).toBeVisible()

    // Allow saving
    mockDocUnitStore()

    // New attempt resets error state
    await fireEvent.click(screen.getByLabelText("Termin setzen"))
    expect(
      screen.queryByText("Speichern leider fehlgeschlagen."),
    ).not.toBeInTheDocument()
  })
})
