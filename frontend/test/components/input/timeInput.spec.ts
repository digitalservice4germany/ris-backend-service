import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import TextInput from "@/components/input/TextInput.vue"
import TimeInput from "@/components/input/TimeInput.vue"

type TimeInputProps = InstanceType<typeof TimeInput>["$props"]
type TextInputProps = InstanceType<typeof TextInput>["$props"]

function renderComponent(
  props?: Partial<TimeInputProps>,
  attrs?: Partial<TextInputProps>,
) {
  let modelValue = props?.modelValue ?? ""

  const effectiveProps: TimeInputProps = {
    id: props?.id ?? "identifier",
    modelValue,
    "onUpdate:modelValue":
      props?.["onUpdate:modelValue"] ?? ((value) => (modelValue = value ?? "")),
    disabled: props?.disabled ?? false,
  }

  return render(TimeInput, { props: effectiveProps, attrs })
}

describe("Time Input", () => {
  it("shows an text input element", () => {
    renderComponent()
    const input: HTMLInputElement | null = screen.queryByRole("textbox")
    expect(input).toBeInTheDocument()
  })

  it("shows the value", () => {
    renderComponent({ modelValue: "12:30" })
    const input: HTMLInputElement = screen.getByRole("textbox")
    expect(input).toHaveValue("12:30")
  })

  it("shows input with an aria label", () => {
    renderComponent(undefined, { ariaLabel: "test-label" })
    const input = screen.queryByLabelText("test-label")
    expect(input).toBeInTheDocument()
  })

  it("shows input with a placeholder", () => {
    renderComponent()
    const input = screen.queryByPlaceholderText("HH:MM")
    expect(input).toBeInTheDocument()
  })

  it("emits model update event when user types into input", async () => {
    const { emitted } = renderComponent()
    const input: HTMLInputElement = screen.getByRole("textbox")
    await userEvent.type(input, "12:34")

    // Why 12:03 and 12:34? Because the time input element is a bit quirky.
    // When typing a time like 12:34, the input element will emit only once
    // it's a valid time at all, which happens after typing 12:3. This is then
    // interpreted as 12:03 by the element. Once we add the 4, the time will
    // be emitted again (as it is still valid) and now be the full time we want.
    expect(emitted("update:modelValue")).toEqual([["12:03"], ["12:34"]])
  })

  it("renders a validation error", () => {
    renderComponent(undefined, { hasError: true })
    const input = screen.getByRole("textbox")
    expect(input).toHaveClass("has-error")
  })

  it("renders a read-only input", () => {
    renderComponent(undefined, { readOnly: true })
    const input = screen.getByRole("textbox")
    expect(input).toHaveAttribute("readonly")
  })

  it("does not rennder a read-only input", () => {
    renderComponent(undefined, { readOnly: false })
    const input = screen.getByRole("textbox")
    expect(input).not.toHaveAttribute("readonly")
  })

  it("renders a disabled input", () => {
    renderComponent({ disabled: true })
    const input = screen.getByRole("textbox")
    expect(input).toBeDisabled()
  })

  it("renders an enabled input", () => {
    renderComponent({ disabled: false })
    const input = screen.getByRole("textbox")
    expect(input).toBeEnabled()
  })

  it("renders an enabled input by default", () => {
    renderComponent()
    const input = screen.getByRole("textbox")
    expect(input).toBeEnabled()
  })

  it("renders the regular variant by default", () => {
    renderComponent()
    const input = screen.getByRole("textbox")
    expect(input).not.toHaveClass("ds-input-medium")
    expect(input).not.toHaveClass("ds-input-small")
  })

  it("renders the regular variant", () => {
    renderComponent(undefined, { size: "regular" })
    const input = screen.getByRole("textbox")
    expect(input).not.toHaveClass("ds-input-medium")
    expect(input).not.toHaveClass("ds-input-small")
  })

  it("renders the medium variant", () => {
    renderComponent(undefined, { size: "medium" })
    const input = screen.getByRole("textbox")
    expect(input).toHaveClass("ds-input-medium")
    expect(input).not.toHaveClass("ds-input-small")
  })

  it("renders the small variant", () => {
    renderComponent(undefined, { size: "small" })
    const input = screen.getByRole("textbox")
    expect(input).not.toHaveClass("ds-input-medium")
    expect(input).toHaveClass("ds-input-small")
  })
})
