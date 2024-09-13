import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnitList from "@/components/DocumentUnitList.vue"
import DocumentUnitListEntry from "@/domain/documentUnitListEntry"
import { PublicationState } from "@/domain/publicationStatus"
import { User } from "@/domain/user"
import errorMessages from "@/i18n/errors.json"
import { ResponseError } from "@/services/httpClient"
import routes from "~/test-helper/routes"

function renderComponent(options?: {
  documentUnitListEntries?: DocumentUnitListEntry[]
  searchResponseError?: ResponseError
  isLoading?: boolean
  emptyState?: string
  activeUser?: User
}) {
  // eslint-disable-next-line testing-library/await-async-events
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
    renderComponent({
      documentUnitListEntries: [
        {
          id: "id",
          uuid: "1",
          documentNumber: "123",
          decisionDate: "2022-02-10",
          fileNumber: "",
          appraisalBody: "",
          documentType: { label: "Test", jurisShortcut: "T" },
          court: { type: "typeA", location: "locB", label: "typeA locB" },
          status: {
            publicationStatus: PublicationState.PUBLISHED,
            withError: false,
          },
          hasAttachments: true,
          hasHeadnoteOrPrinciple: true,
          hasNote: true,
          isDeletable: false,
          isEditable: false,
        },
        {
          id: "id",
          uuid: "2",
          documentNumber: "234",
          decisionDate: "2022-02-10",
          fileNumber: "",
          appraisalBody: "cba",
          documentType: { label: "Test", jurisShortcut: "T" },
          court: { type: "typeA", location: "locB", label: "typeA locB" },
          status: {
            publicationStatus: PublicationState.PUBLISHED,
            withError: false,
          },
          hasAttachments: false,
          hasHeadnoteOrPrinciple: false,
          hasNote: false,
          isDeletable: true,
          isEditable: true,
        },
      ],
    })

    // wait for asynchronous authService.getName method to update the UI according to the user
    expect(
      screen.getByRole("link", { name: "Dokumentationseinheit bearbeiten" }),
    ).toBeInTheDocument()

    expect(screen.getAllByTestId("listEntry").length).toBe(2)

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
    expect(screen.getByLabelText("Notiz vorhanden")).toBeVisible()
    expect(screen.getByLabelText("Keine Notiz vorhanden")).toBeVisible()

    // expect Headnote or Principal
    expect(screen.getByLabelText("Langtext vorhanden")).toBeVisible()
    expect(screen.getByLabelText("Kein Langtext vorhanden")).toBeVisible()

    // expect Attachment
    expect(screen.getByLabelText("Anhang vorhanden")).toBeVisible()
    expect(screen.getByLabelText("Kein Anhang vorhanden")).toBeVisible()

    // expect two view links
    expect(
      screen.getAllByRole("link", { name: "Dokumentationseinheit ansehen" }),
    ).toHaveLength(2)
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
    expect(emitted().deleteDocumentUnit).toBeTruthy()
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
})
