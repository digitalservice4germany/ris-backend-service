import { render, screen } from "@testing-library/vue"
import InputField, {
  LabelPosition,
} from "@/shared/components/input/InputField.vue"

function renderComponent(options?: {
  id?: string
  label?: string | string[]
  slot?: string
  validationError?: string
  required?: true
  labelPosition?: LabelPosition
}) {
  const id = options?.id ?? "identifier"
  const slots = { default: options?.slot ?? `<input id="${id}" />` }
  const props = {
    id,
    label: options?.label,
    required: options?.required ?? options?.required,
    validationError: options?.validationError,
    labelPosition: options?.labelPosition,
  }

  return render(InputField, { slots, props })
}

describe("InputField", () => {
  it("shows input with given label", () => {
    renderComponent({ label: "test label" })

    const input = screen.queryByLabelText("test label", { exact: false })

    expect(input).toBeInTheDocument()
  })

  it("shows input with given label and required text", () => {
    renderComponent({
      label: "test label",
      required: true,
    })

    const input = screen.queryByLabelText("test label *", { exact: false })
    expect(input).toBeInTheDocument()
  })

  it("shows input with given error message", () => {
    renderComponent({ validationError: "error message" })

    const text = screen.getByText("error message") as HTMLElement

    expect(text).toBeInTheDocument()
  })

  it("injects given input element into slot", () => {
    renderComponent({
      slot: "<template v-slot='slotProps'><input aria-label='test-input' v-bind='slotProps' type='radio' /></template>",
      id: "test-identifier",
      label: "test label",
    })

    const input = screen.getByLabelText("test-input") as HTMLInputElement

    expect(input).toBeInTheDocument()
    expect(input?.type).toBe("radio")
  })

  it("does not render label if not given", () => {
    renderComponent({
      id: "test",
    })
    expect(screen.queryByLabelText("test")).not.toBeInTheDocument
  })

  it("shows label after the input field", () => {
    renderComponent({ label: "test label", labelPosition: LabelPosition.RIGHT })

    const input = screen.queryByLabelText("test label", {
      exact: false,
    }) as HTMLInputElement
    expect(input).toBeInTheDocument()
    const label = screen.queryByText("test label", {
      exact: false,
    }) as HTMLLabelElement
    expect(label).toBeInTheDocument()

    expect(input.compareDocumentPosition(label)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    )
  })

  it("shows input with given label in two lines", () => {
    renderComponent({
      label: ["test label 1", "test label 2"],
    })
    const spanLabelFirstPart = screen.queryByText("test label 1") as HTMLElement
    expect(spanLabelFirstPart).toBeInTheDocument()
    const spanLabelSecondPart = screen.queryByText(
      "test label 2",
    ) as HTMLElement
    expect(spanLabelSecondPart).toBeInTheDocument()

    expect(
      spanLabelFirstPart.compareDocumentPosition(spanLabelSecondPart),
    ).toBe(Node.DOCUMENT_POSITION_FOLLOWING)
  })

  it("shows input with given label in two lines and required", () => {
    renderComponent({
      label: ["test label 1", "test label 2"],
      required: true,
    })

    const spanLabelFirstPart = screen.queryByText("test label 1") as HTMLElement
    expect(spanLabelFirstPart).toBeInTheDocument()
    const spanLabelSecondPart = screen.queryByText(
      "test label 2",
    ) as HTMLElement
    expect(spanLabelSecondPart).toBeInTheDocument()

    expect(
      spanLabelFirstPart.compareDocumentPosition(spanLabelSecondPart),
    ).toBe(Node.DOCUMENT_POSITION_FOLLOWING)

    const spanLabelRequired = screen.queryByText("*") as HTMLElement
    expect(spanLabelRequired).toBeInTheDocument()

    expect(spanLabelSecondPart.contains(spanLabelRequired)).toBe(true)
  })

  it("updates the label when the label prop changes", async () => {
    const { rerender } = renderComponent({ label: "test label" })

    const label = screen.queryByText("test label", { exact: false })
    expect(label).toBeInTheDocument()

    await rerender({ label: "new label" })
    const newLabel = screen.queryByText("new label", { exact: false })
    expect(newLabel).toBeInTheDocument()
  })
})
