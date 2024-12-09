import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import SearchResultList, {
  SearchResults,
} from "@/components/SearchResultList.vue"
import DocumentUnit from "@/domain/documentUnit"
import RelatedDocumentation from "@/domain/relatedDocumentation"
import routes from "~/test-helper/routes"

function renderSearchResults(
  searchResults?: SearchResults<RelatedDocumentation>,
  isLoading?: boolean,
) {
  const props: {
    searchResults: SearchResults<RelatedDocumentation>
    isLoading: boolean
  } = {
    searchResults: searchResults ?? [
      {
        decision: new RelatedDocumentation({
          ...{
            court: {
              type: "fooType",
              location: "fooLocation",
              label: "fooLabel",
            },
            documentNumber: "fooDocumentNumber",
            referencedDocumentationUnitId: "foo",
          },
        }),
        isLinked: false,
      },
    ],
    isLoading: isLoading ?? false,
  }

  const router = createRouter({
    history: createWebHistory(),
    routes: routes,
  })

  const utils = render(SearchResultList, {
    props,

    global: {
      plugins: [
        createTestingPinia({
          initialState: {
            docunitStore: {
              documentUnit: new DocumentUnit("foo", {
                documentNumber: "1234567891234",
              }),
            },
          },
        }),
        [router],
      ],
      stubs: { routerLink: { template: "<a><slot/></a>" } },
    },
  })

  const user = userEvent.setup()
  return { user, ...utils }
}

describe("Search result list", () => {
  it("renders correctly", async () => {
    renderSearchResults()

    expect(await screen.findByText("fooLabel")).toBeVisible()
    expect(await screen.findByText("fooDocumentNumber")).toBeVisible()

    expect(await screen.findByTestId("add-decision-button")).toBeVisible()
    expect(screen.queryByText(/Bereits hinzugefügt/)).not.toBeInTheDocument()
  })

  it("indicates not yet added previous decisions", async () => {
    renderSearchResults()

    expect(await screen.findByTestId("add-decision-button")).toBeVisible()
    expect(screen.queryByText(/Bereits hinzugefügt/)).not.toBeInTheDocument()
  })

  it("indicates already added previous decisions", async () => {
    renderSearchResults([
      {
        decision: new RelatedDocumentation({
          ...{
            court: {
              type: "fooType",
              location: "fooLocation",
              label: "fooLabel",
            },
            documentNumber: "fooDocumentNumber",
          },
        }),
        isLinked: true,
      },
    ])

    expect(await screen.findByTestId("add-decision-button")).toBeVisible()
    expect(await screen.findByText(/Bereits hinzugefügt/)).toBeVisible()
  })

  it("clicking on 'Übernehmen' emits link decision event", async () => {
    const { user, emitted } = renderSearchResults()

    expect(await screen.findByText("fooLabel")).toBeVisible()
    expect(await screen.findByText("fooDocumentNumber")).toBeVisible()

    expect(await screen.findByTestId("add-decision-button")).toBeVisible()
    expect(screen.queryByText(/Bereits hinzugefügt/)).not.toBeInTheDocument()

    const button = await screen.findByTestId("add-decision-button")
    await user.click(button)

    expect(emitted().linkDecision).toBeTruthy()
  })

  it("renders search results for active citations", async () => {
    renderSearchResults([
      {
        decision: new RelatedDocumentation({
          ...{
            court: {
              type: "fooType",
              location: "fooLocation",
              label: "fooLabel",
            },
            documentNumber: "fooDocumentNumber",
          },
        }),
        isLinked: true,
      },
    ])

    expect(await screen.findByTestId("add-decision-button")).toBeVisible()
    expect(await screen.findByText(/Bereits hinzugefügt/)).toBeVisible()
  })

  it("renders loading spinner, when loading", async () => {
    renderSearchResults([], true)
    expect(await screen.findByLabelText("Ladestatus")).toBeVisible()
  })
})
