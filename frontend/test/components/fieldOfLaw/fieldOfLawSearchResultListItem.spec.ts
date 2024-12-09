import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import FieldOfLawSearchResultsListItem from "@/components/field-of-law/FieldOfLawSearchResultsListItem.vue"

function renderComponent(options?: {
  identifier?: string
  text?: string
  linkedFields?: string[]
}) {
  const fieldOfLaw = {
    identifier: options?.identifier ?? "AR",
    text: options?.text ?? "Arbeitsrecht",
    linkedFields: options?.linkedFields ?? [],
    norms: [],
    children: [],
    parent: undefined,
    hasChildren: false,
  }

  const user = userEvent.setup()

  return {
    user,
    ...render(FieldOfLawSearchResultsListItem, { props: { fieldOfLaw } }),
  }
}

describe("FieldOfLawSearchResults", () => {
  it("render search results", () => {
    renderComponent()

    expect(screen.getByText("AR")).toBeInTheDocument()
    expect(screen.getByText("Arbeitsrecht")).toBeInTheDocument()
  })

  it("on identifier click emit 'node:add'", async () => {
    const { emitted, user } = renderComponent({ identifier: "AR-01" })

    await user.click(screen.getByLabelText("AR-01 hinzufügen"))

    expect(emitted()["node:add"]).toBeTruthy()
  })

  it("on linked field click emit 'linked-field:clicked'", async () => {
    const { emitted, user } = renderComponent({
      identifier: "BR-01",
      text: "mit Link zu BR-02",
      linkedFields: ["BR-02"],
    })

    await user.click(screen.getByText("BR-02"))

    expect(emitted()["linked-field:clicked"]).toBeTruthy()
  })
})
