import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import KeywordsComponent from "@/components/KeyWords.vue"
import DocumentUnit from "@/domain/documentUnit"

const scrollIntoViewMock = vi.fn()
window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock

function renderComponent(keywords?: string[]) {
  const user = userEvent.setup()

  return {
    user,
    ...render(KeywordsComponent, {
      global: {
        plugins: [
          [
            createTestingPinia({
              initialState: {
                docunitStore: {
                  documentUnit: new DocumentUnit("foo", {
                    documentNumber: "1234567891234",
                    contentRelatedIndexing: {
                      keywords: keywords ?? [],
                    },
                  }),
                },
              },
            }),
          ],
        ],
        stubs: { routerLink: { template: "<a><slot/></a>" } },
      },
    }),
  }
}

describe("Keywords", () => {
  test("if no keywords render edit mode", async () => {
    renderComponent()

    expect(
      screen.getByPlaceholderText(
        "Geben Sie jeden Wert in eine eigene Zeile ein",
      ),
    ).toBeInTheDocument()
  })

  test("if keywords render display mode", async () => {
    renderComponent(["one", "two"])
    expect(
      await screen.findByLabelText("Schlagwörter bearbeiten"),
    ).toBeVisible()
    expect(
      screen.queryByPlaceholderText(
        "Geben Sie jeden Wert in eine eigene Zeile ein",
      ),
    ).not.toBeInTheDocument()
  })

  test('in display mode, clicking on "Schlagwörter bearbeiten" opens edit mode', async () => {
    const { user } = renderComponent(["one", "two"])
    expect(
      await screen.findByLabelText("Schlagwörter bearbeiten"),
    ).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)

    await user.click(screen.getByLabelText("Schlagwörter bearbeiten"))
    expect(screen.getByLabelText("Schlagwörter Input")).toHaveValue("one\ntwo")
  })

  test('in edit mode, click on "Übernehmen" with input, saves input and opens display mode', async () => {
    const { user } = renderComponent()

    await user.type(screen.getByLabelText("Schlagwörter Input"), "one {enter}")
    await user.click(screen.getByLabelText("Schlagwörter übernehmen"))
    expect(
      await screen.findByLabelText("Schlagwörter bearbeiten"),
    ).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(1)
    expect(screen.getByText(/one/)).toBeVisible()
  })

  test('in edit mode, click on "Übernehmen" adds new content to existing keywords correctly', async () => {
    const { user } = renderComponent(["one", "two"])

    expect(
      await screen.findByLabelText("Schlagwörter bearbeiten"),
    ).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)

    await user.click(screen.getByLabelText("Schlagwörter bearbeiten"))
    await user.type(
      screen.getByLabelText("Schlagwörter Input"),
      "{enter}three {enter}",
    )

    await user.click(screen.getByLabelText("Schlagwörter übernehmen"))
    expect(screen.getByLabelText("Schlagwörter bearbeiten")).toBeInTheDocument()
    expect(screen.getAllByTestId("chip").length).toBe(3)
    expect(screen.getByText(/three/)).toBeVisible()
  })

  test('in edit mode, click on "Übernehmen" with no input stays in edit mode', async () => {
    const { user } = renderComponent()

    await user.click(screen.getByLabelText("Schlagwörter übernehmen"))
    expect(
      screen.queryByLabelText("Schlagwörter bearbeiten"),
    ).not.toBeInTheDocument()
    expect(screen.getByLabelText("Schlagwörter Input")).toHaveValue("")
  })

  test('in edit mode, click on "Abbrechen" with input reverts to display mode without changes', async () => {
    const { user } = renderComponent(["one", "two"])

    expect(
      await screen.findByLabelText("Schlagwörter bearbeiten"),
    ).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)

    await user.click(screen.getByLabelText("Schlagwörter bearbeiten"))
    await user.type(
      screen.getByLabelText("Schlagwörter Input"),
      "three {enter}",
    )

    await user.click(screen.getByLabelText("Abbrechen"))
    expect(screen.getByLabelText("Schlagwörter bearbeiten")).toBeInTheDocument()
    expect(
      await screen.findByLabelText("Schlagwörter bearbeiten"),
    ).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)
    expect(screen.queryByText(/three/)).not.toBeInTheDocument()
  })

  test('in edit mode, click on "Abbrechen" with no input stays in edit mode', async () => {
    const { user } = renderComponent()

    await user.click(screen.getByLabelText("Abbrechen"))

    expect(
      screen.queryByLabelText("Schlagwörter bearbeiten"),
    ).not.toBeInTheDocument()
  })

  test("sort alphabetically", async () => {
    const { user } = renderComponent(["one", "two", "three"])
    expect(
      await screen.findByLabelText("Schlagwörter bearbeiten"),
    ).toBeVisible()
    const chips = screen.getAllByTestId("chip").map((chip) => chip.textContent)
    expect(chips[0]).toBe("one")
    expect(chips[1]).toBe("two")
    expect(chips[2]).toBe("three")

    await user.click(screen.getByLabelText("Schlagwörter bearbeiten"))
    await user.click(screen.getByLabelText("Alphabetisch sortieren"))
    await user.click(screen.getByLabelText("Schlagwörter übernehmen"))

    const chipsSorted = screen
      .getAllByTestId("chip")
      .map((chip) => chip.textContent)
    expect(chipsSorted[0]).toBe("one")
    expect(chipsSorted[1]).toBe("three")
    expect(chipsSorted[2]).toBe("two")
  })

  test("add duplicates not possible", async () => {
    const { user } = renderComponent(["one", "two"])

    expect(
      await screen.findByLabelText("Schlagwörter bearbeiten"),
    ).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)

    await user.click(screen.getByLabelText("Schlagwörter bearbeiten"))
    await user.type(
      screen.getByLabelText("Schlagwörter Input"),
      "{enter}two {enter}",
    )

    await user.click(screen.getByLabelText("Schlagwörter übernehmen"))
    expect(screen.getByLabelText("Schlagwörter bearbeiten")).toBeInTheDocument()
    expect(screen.getAllByTestId("chip").length).toBe(2)
  })
})
