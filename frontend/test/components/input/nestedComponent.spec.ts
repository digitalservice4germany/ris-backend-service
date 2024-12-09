import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import NestedComponent from "@/components/NestedComponents.vue"

function renderComponent(options?: { ariaLabel?: string; isOpen?: boolean }) {
  const props = {
    ariaLabel: options?.ariaLabel ?? "test aria label",
    isOpen: options?.isOpen,
  }
  const slots = {
    default: "<p>Parent Slot</p>",
    children: "<p>Child Slot</p>",
  }
  const utils = render(NestedComponent, { props, slots })
  const user = userEvent.setup()
  return { user, ...utils }
}

describe("Nested Component", () => {
  it("renders per default a closed nested component", () => {
    renderComponent()

    const parentContent = screen.getByText("Parent Slot")
    expect(parentContent).toBeInTheDocument()

    // Check that the child slot content is not visible
    const childContent = screen.queryByText("Child Slot")
    expect(childContent).not.toBeVisible()
  })

  it("renders child if prop isOpen is true", () => {
    renderComponent({ isOpen: true })

    const parentContent = screen.getByText("Parent Slot")
    expect(parentContent).toBeInTheDocument()

    // Check that the child slot content is not visible
    const childContent = screen.queryByText("Child Slot")
    expect(childContent).toBeVisible()
  })
})
