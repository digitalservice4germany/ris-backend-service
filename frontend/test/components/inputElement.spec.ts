import userEvent from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import InputElement from "@/components/InputElement.vue"
import { InputType } from "@/domain"
import type { ModelType } from "@/domain"

function renderComponent(options?: {
  id?: string
  type?: InputType
  modelValue?: ModelType
}) {
  const props = {
    id: "test",
    type: options?.type,
    modelValue: options?.modelValue,
    attributes: {
      id: "test-id",
      ariaLabel: "test-label",
    },
  }
  const utils = render(InputElement, { props })
  const user = userEvent.setup()
  return { user, ...utils }
}

describe("InputElement", () => {
  it("renders per default a textbox element", () => {
    renderComponent({ type: undefined })

    const textbox = screen.queryByRole("textbox")

    expect(textbox).toBeInTheDocument()
  })

  it("renders a textbox when type property defines so", () => {
    renderComponent({ type: InputType.TEXT })

    const textbox = screen.queryByRole("textbox")

    expect(textbox).toBeInTheDocument()
  })

  it("displays model value into input element", () => {
    renderComponent({
      modelValue: "test value",
    })

    const input = screen.queryByDisplayValue("test value")

    expect(input).toBeInTheDocument()
  })

  it("emits update model value event when input is used", async () => {
    const { emitted, user } = renderComponent({
      type: InputType.TEXT,
    })
    const input = screen.getByRole("textbox")
    await user.type(input, "a")
    await userEvent.tab()

    expect(emitted()["update:modelValue"]).toHaveLength(1)
    expect(emitted()["update:modelValue"]).toEqual([["a"]])
  })
})
