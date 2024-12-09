import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { nextTick } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnitList from "@/components/DocumentUnitList.vue"
import DocumentUnitListEntry from "@/domain/documentUnitListEntry"
import { PublicationState } from "@/domain/publicationStatus"
import { User } from "@/domain/user"
import errorMessages from "@/i18n/errors.json"
import featureToggleService from "@/services/featureToggleService"
import { ResponseError } from "@/services/httpClient"
import routes from "~/test-helper/routes"

function renderComponent(options?: {
  documentUnitListEntries?: DocumentUnitListEntry[]
  searchResponseError?: ResponseError
  isLoading?: boolean
  emptyState?: string
  activeUser?: User
  showPublicationDate?: boolean
}) {
  const user = userEvent.setup()

  return {
    user,
    ...render(DocumentUnitList, {
      props: {
        documentUnitListEntries: options?.documentUnitListEntries,
        searchResponseError: options?.searchResponseError ?? undefined,
        isLoading: options?.isLoading ?? false,
        emptyState:
          options?.emptyState ??
          (!options?.documentUnitListEntries
            ? "Starten Sie die Suche oder erstellen Sie eine neue Dokumentationseinheit."
            : errorMessages.SEARCH_RESULTS_NOT_FOUND.title),
        showPublicationDate: options?.showPublicationDate,
      },
      global: {
        plugins: [
          createTestingPinia({
            initialState: {
              session: {
                user: options?.activeUser || {
                  name: "user",
                  documentationOffice: { abbreviation: "DS" },
                },
              },
            },
          }),
          createRouter({
            history: createWebHistory(),
            routes: routes,
          }),
        ],
      },
    }),
  }
}

describe("documentUnit list", () => {
  beforeEach(() => {
    vi.spyOn(featureToggleService, "isEnabled").mockResolvedValue({
      status: 200,
      data: true,
    })
  })

  test("initial state feedback", async () => {
    renderComponent({})

    expect(
      screen.getByText(/Starten Sie die Suche oder erstellen Sie eine/),
    ).toBeVisible()
  })

  test("no results feedback", async () => {
    renderComponent({
      documentUnitListEntries: [],
    })

    expect(
      screen.getByText(errorMessages.SEARCH_RESULTS_NOT_FOUND.title),
    ).toBeVisible()
  })

  test("shows error", () => {
    renderComponent({
      searchResponseError: {
        title: "error title",
        description: "error description",
      },
    })
    expect(screen.getByText(/error title/)).toBeVisible()
    expect(screen.getByText(/error description/)).toBeVisible()
  })

  test("shows loading state", () => {
    renderComponent({ isLoading: true })
    expect(screen.getByLabelText("Ladestatus")).toBeInTheDocument()
  })

  test("renders documentUnit list", async () => {
    const longNote = "Long note will be trimmed Lorem ipsum dolor sit ama"
    const trimmedNote = "Long note will be trimmed Lorem ipsum dolor sit am..."
    const { user } = renderComponent({
      documentUnitListEntries: [
        {
          id: "id",
          uuid: "1",
          documentNumber: "123",
          decisionDate: "2022-02-10",
          fileNumber: "",
          note: "a note",
          appraisalBody: "",
          documentType: { label: "Test", jurisShortcut: "T" },
          court: { type: "typeA", location: "locB", label: "typeA locB" },
          status: {
            publicationStatus: PublicationState.PUBLISHED,
            withError: false,
          },
          hasAttachments: true,
          hasHeadnoteOrPrinciple: true,
          isDeletable: false,
          isEditable: false,
        },
        {
          id: "id",
          uuid: "2",
          documentNumber: "234",
          decisionDate: "2022-02-10",
          fileNumber: "",
          note: longNote,
          appraisalBody: "cba",
          documentType: { label: "Test", jurisShortcut: "T" },
          court: { type: "typeA", location: "locB", label: "typeA locB" },
          status: {
            publicationStatus: PublicationState.PUBLISHED,
            withError: false,
          },
          hasAttachments: false,
          hasHeadnoteOrPrinciple: false,
          isDeletable: true,
          isEditable: true,
        },
        {
          id: "id",
          uuid: "3",
          documentNumber: "567",
          decisionDate: "2024-02-10",
          fileNumber: "",
          appraisalBody: "1. Senat",
          documentType: { label: "Urteil", jurisShortcut: "Urt" },
          court: { type: "LG", location: "Berlin", label: "LG Berlin" },
          status: {
            publicationStatus: PublicationState.EXTERNAL_HANDOVER_PENDING,
            withError: false,
          },
          hasAttachments: false,
          hasHeadnoteOrPrinciple: false,
          isDeletable: false,
          isEditable: false,
          source: "NJW",
          creatingDocumentationOffice: {
            id: "creatingDocumentationOfficeId",
            abbreviation: "DS",
          },
        },
      ],
    })

    // wait for asynchronous authService.getName method to update the UI according to the user
    expect(
      screen.getByRole("link", { name: "Dokumentationseinheit bearbeiten" }),
    ).toBeInTheDocument()

    expect(screen.getAllByRole("row").length).toBe(3)

    //Spruchkörper visible
    expect(screen.getByText("cba")).toBeVisible()

    // expect only one edit link to 234 documentation unit
    expect(
      screen.getByRole("link", { name: "Dokumentationseinheit bearbeiten" }),
    ).toHaveAttribute("href", "/caselaw/documentUnit/234/categories")

    // expect only one delete button for 234 documentation unit
    expect(
      screen.getByRole("button", { name: "Dokumentationseinheit löschen" }),
    ).toBeVisible()

    // expect Notes
    expect(screen.getByLabelText("Keine Notiz vorhanden")).toBeVisible()
    expect(screen.queryByText("a note")).not.toBeInTheDocument()
    await user.hover(screen.getByLabelText("a note"))
    // The tooltip of the short note can be seen fully after hovering over the icon
    expect(screen.getByText("a note")).toBeVisible()

    await user.hover(screen.getByLabelText(trimmedNote))
    // After hovering another note, the previous tooltip is hidden
    expect(screen.queryByText("a note")).not.toBeInTheDocument()
    // The long tooltip will be shown trimmed to 50 chars after hovering over the icon
    expect(screen.getByText(trimmedNote)).toBeVisible()

    // expect Headnote or Principal
    expect(screen.getAllByLabelText("Kein Kurztext vorhanden")).toHaveLength(2)
    expect(screen.getByLabelText("Kurztext vorhanden")).toBeVisible()

    // expect Attachment
    expect(screen.getAllByLabelText("Kein Anhang vorhanden")).toHaveLength(2)
    expect(screen.getByLabelText("Anhang vorhanden")).toBeVisible()

    // expect three view links
    expect(
      screen.getAllByRole("link", { name: "Dokumentationseinheit ansehen" }),
    ).toHaveLength(3)

    expect(screen.getByText("Fremdanlage")).toBeVisible()
    expect(screen.getByText("aus NJW (DS)")).toBeVisible()

    expect(screen.queryByText("should not show source")).not.toBeInTheDocument()
    expect(
      screen.queryByText("should not show doc office"),
    ).not.toBeInTheDocument()
  })

  test("delete emits event", async () => {
    const { user, emitted } = renderComponent({
      documentUnitListEntries: [
        {
          id: "id",
          uuid: "1",
          documentNumber: "123",
          decisionDate: "2022-02-10",
          fileNumber: "",
          documentType: { label: "Test", jurisShortcut: "T" },
          court: { type: "typeA", location: "locB", label: "typeA locB" },
          status: {
            publicationStatus: PublicationState.PUBLISHED,
            withError: false,
          },
          isDeletable: true,
          isEditable: true,
        },
      ],
    })

    expect(
      screen.getByRole("link", { name: "Dokumentationseinheit bearbeiten" }),
    ).toBeInTheDocument()

    await screen.findByText("123")
    await user.click(screen.getByLabelText("Dokumentationseinheit löschen"))
    const confirmButton = screen.getByRole("button", { name: "Löschen" })
    expect(confirmButton).toBeInTheDocument()
    await user.click(confirmButton)
    expect(emitted().deleteDocumentationUnit).toBeTruthy()
  })

  test("disables edit and delete buttons if foreign documentation office", async () => {
    renderComponent({
      documentUnitListEntries: [
        {
          id: "id",
          uuid: "1",
          documentNumber: "123",
          decisionDate: "2022-02-10",
          fileNumber: "",
          documentType: { label: "Test", jurisShortcut: "T" },
          court: { type: "typeA", location: "locB", label: "typeA locB" },
          status: {
            publicationStatus: PublicationState.PUBLISHED,
            withError: false,
          },
        },
      ],
      activeUser: {
        name: "fooUser",
        documentationOffice: { abbreviation: "fooDocumentationOffice" },
      },
    })

    expect(
      screen.queryByRole("link", { name: "Dokumentationseinheit bearbeiten" }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Dokumentationseinheit löschen" }),
    ).not.toBeInTheDocument()
  })

  test("shows 'Übernehmen' icon instead if edit icon, if status equals EXTERNAL_HANDOVER_PENDING", async () => {
    renderComponent({
      documentUnitListEntries: [
        {
          id: "id",
          uuid: "1",
          documentNumber: "123",
          decisionDate: "2022-02-10",
          fileNumber: "",
          documentType: { label: "Test", jurisShortcut: "T" },
          court: { type: "typeA", location: "locB", label: "typeA locB" },
          status: {
            publicationStatus: PublicationState.EXTERNAL_HANDOVER_PENDING,
            withError: false,
          },
        },
      ],
      activeUser: {
        name: "fooUser",
        documentationOffice: { abbreviation: "fooDocumentationOffice" },
      },
    })

    expect(
      screen.queryByRole("link", { name: "Dokumentationseinheit bearbeiten" }),
    ).not.toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: "Dokumentationseinheit übernehmen" }),
    ).toBeInTheDocument()
  })

  test("shows 'jDV Übergabe' column if showPublicationDate is true", async () => {
    renderComponent({
      showPublicationDate: true,
    })

    await nextTick() // can be removed after feature flag removal

    expect(screen.getByText("jDV Übergabe")).toBeInTheDocument()
  })

  test("shows schedulingToolTip for scheduled future date", async () => {
    renderComponent({
      documentUnitListEntries: [
        {
          id: "id",
          uuid: "1",
          documentNumber: "123",
          scheduledPublicationDateTime: "2100-01-23T23:00:00",
        },
      ],
      showPublicationDate: true,
    })

    await nextTick() // should be removed after feature flag removal

    expect(screen.getByText("24.01.2100 00:00")).toBeInTheDocument()
    expect(
      screen.getByLabelText("Terminierte Übergabe am 24.01.2100 00:00"),
    ).toBeInTheDocument()
    expect(screen.getByTestId("scheduling-icon")).toBeInTheDocument()
  })

  test("shows schedulingToolTip for last publication date", async () => {
    renderComponent({
      documentUnitListEntries: [
        {
          id: "id",
          uuid: "1",
          documentNumber: "123",
          lastPublicationDateTime: "2000-01-23T23:00:00",
        },
      ],
      showPublicationDate: true,
    })

    await nextTick() // should be removed after feature flag removal

    expect(screen.getByText("24.01.2000 00:00")).toBeInTheDocument()
    expect(
      screen.getByLabelText("Keine Übergabe terminiert"),
    ).toBeInTheDocument()
    expect(screen.getByTestId("scheduling-icon")).toBeInTheDocument()
  })
})
