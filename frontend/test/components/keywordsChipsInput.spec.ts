import userEvent from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import KeywordsChipsInput from "@/shared/components/input/KeywordsChipsInput.vue"

type KeywordsChipsInputProps = InstanceType<typeof KeywordsChipsInput>["$props"]

function renderComponent(props?: Partial<KeywordsChipsInputProps>) {
  const user = userEvent.setup()

  let modelValue: string[] | undefined = props?.modelValue ?? []

  const effectiveProps: KeywordsChipsInputProps = {
    id: props?.id ?? "identifier",
    modelValue,
    "onUpdate:modelValue":
      props?.["onUpdate:modelValue"] ?? ((val) => (modelValue = val)),
    ariaLabel: props?.ariaLabel ?? "aria-label",
    validationError: props?.validationError,
    onChipAdded: props?.onChipAdded,
    onChipDeleted: props?.onChipDeleted,
    error: props?.error,
  }

  return { user, ...render(KeywordsChipsInput, { props: effectiveProps }) }
}

describe("Keywords Chips Input", () => {
  it("shows a chips input element", () => {
    renderComponent()
    const input = screen.getByRole<HTMLInputElement>("textbox")
    expect(input).toBeInTheDocument()
  })

  it("shows the value", () => {
    renderComponent({ modelValue: ["foo", "bar"] })
    const chips = screen.getAllByRole("listitem")
    expect(chips).toHaveLength(2)
    expect(chips[0]).toHaveTextContent("foo")
    expect(chips[1]).toHaveTextContent("bar")
  })

  it("shows chips input with an aria label", () => {
    renderComponent({ ariaLabel: "test-label" })
    const input = screen.queryByLabelText("test-label")
    expect(input).toBeInTheDocument()
  })

  it("emits model update when a chip is added", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({ "onUpdate:modelValue": onUpdate })

    const input = screen.getByRole("textbox")
    await user.type(input, "foo{enter}")
    expect(onUpdate).toHaveBeenCalledWith(["foo"])
  })

  it("emits an event when a chip is added", async () => {
    const onAdded = vi.fn()
    const { user } = renderComponent({ onChipAdded: onAdded })

    const input = screen.getByRole("textbox")
    await user.type(input, "foo{enter}")
    expect(onAdded).toHaveBeenCalledWith("foo")
  })

  it("removes whitespace from chips when added", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      "onUpdate:modelValue": onUpdate,
      modelValue: ["bar"],
    })

    const input = screen.getByRole("textbox")
    await user.type(input, " foo {enter}")
    expect(onUpdate).toHaveBeenCalledWith(["bar", "foo"])
  })

  it("clears the input when a chip is added", async () => {
    const { user } = renderComponent()

    const input = screen.getByRole("textbox")
    await user.type(input, "foo{enter}")
    expect(input).toHaveValue("")
  })

  it("does not add a chip when input is empty", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({ "onUpdate:modelValue": onUpdate })
    const input = screen.getByRole<HTMLInputElement>("textbox")
    expect(input).toHaveValue("")

    await user.type(input, "{enter}")

    expect(onUpdate).not.toHaveBeenCalled()
  })

  it("does not add a chip when input is only whitespaces", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({ "onUpdate:modelValue": onUpdate })
    const input = screen.getByRole<HTMLInputElement>("textbox")
    expect(input).toHaveValue("")

    await user.type(input, "   {enter}")

    expect(onUpdate).not.toHaveBeenCalled()
  })

  it("emits model update when a chip is removed", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      "onUpdate:modelValue": onUpdate,
      modelValue: ["foo", "bar"],
    })

    const button = screen.getAllByRole("button")[0]
    await user.click(button)
    expect(onUpdate).toHaveBeenCalledWith(["bar"])
  })

  it("emits an event when a chip is removed", async () => {
    const onDeleted = vi.fn()
    const { user } = renderComponent({
      modelValue: ["foo", "bar"],
      onChipDeleted: onDeleted,
    })

    const button = screen.getAllByRole("button")[0]
    await user.click(button)
    expect(onDeleted).toHaveBeenCalledWith("foo")
  })

  it("focuses the input when pressing arrow on the first chip", async () => {
    const { user } = renderComponent({ modelValue: ["foo", "bar", "baz"] })

    const chips = screen.getAllByRole("listitem")
    await user.click(chips[0])
    await user.keyboard("{arrowleft}")
    const input = screen.getByRole<HTMLInputElement>("textbox")
    expect(input).toHaveFocus()
  })

  it("focuses the first chip when pressing arrow on the input", async () => {
    const { user } = renderComponent({ modelValue: ["foo", "bar", "baz"] })

    const input = screen.getByRole<HTMLInputElement>("textbox")
    await user.click(input)
    await user.type(input, "abc")
    expect(input).toHaveFocus()
    await user.keyboard("{arrowright}")
    const chips = screen.getAllByRole("listitem")
    expect(chips[0]).toHaveFocus()
  })

  it("focuses chips with arrow keys", async () => {
    const { user } = renderComponent({ modelValue: ["foo", "bar", "baz"] })

    const input = screen.getByRole<HTMLInputElement>("textbox")
    await user.click(input)
    await user.tab()

    const chips = screen.getAllByRole("listitem")
    expect(chips[0]).toHaveFocus()
    await user.keyboard("{arrowright}")
    expect(chips[1]).toHaveFocus()
    await user.keyboard("{arrowright}")
    expect(chips[2]).toHaveFocus()
    await user.keyboard("{arrowright}")
    expect(chips[2]).toHaveFocus()
    await user.keyboard("{arrowleft}")
    expect(chips[1]).toHaveFocus()
    await user.keyboard("{arrowleft}")
    expect(chips[0]).toHaveFocus()
    await user.keyboard("{arrowleft}")
    expect(input).toHaveFocus()
  })

  it("focuses chips with tab", async () => {
    const { user } = renderComponent({ modelValue: ["foo", "bar", "baz"] })

    const input = screen.getByRole<HTMLInputElement>("textbox")
    await user.click(input)
    await user.tab()

    const chips = screen.getAllByRole("listitem")
    expect(chips[0]).toHaveFocus()
    await user.tab({ shift: true })
    expect(input).toHaveFocus()
  })

  it("deletes the focused chip on enter", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      "onUpdate:modelValue": onUpdate,
      modelValue: ["foo", "bar"],
    })

    const chips = screen.getAllByRole("listitem")
    await user.click(chips[1])
    await user.keyboard("{enter}")
    expect(onUpdate).toHaveBeenCalledWith(["foo"])
  })

  it("shows an error message", () => {
    renderComponent({ error: { title: "foo" } })
    expect(screen.getByText("foo")).toBeInTheDocument()
  })

  it("clears the error message on blur", async () => {
    const { user } = renderComponent({ error: { title: "foo" } })
    expect(screen.getByText("foo")).toBeInTheDocument()

    const input = screen.getByRole("textbox")
    await user.click(input)
    await user.tab()
    expect(screen.queryByText("foo")).not.toBeInTheDocument()
  })

  it("clears the error message on input", async () => {
    const { user } = renderComponent({ error: { title: "foo" } })
    expect(screen.getByText("foo")).toBeInTheDocument()

    const input = screen.getByRole("textbox")
    await user.click(input)
    await user.type(input, "o")
    expect(screen.queryByText("foo")).not.toBeInTheDocument()
  })

  it("shows an error message when adding a chip that already exists", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      modelValue: ["one"],
      "onUpdate:modelValue": onUpdate,
    })

    const input = screen.getByRole<HTMLInputElement>("textbox")
    await user.type(input, "one{enter}")
    expect(screen.getByText("Schlagwort bereits vergeben.")).toBeInTheDocument()
    expect(onUpdate).not.toHaveBeenCalled()
  })
})
