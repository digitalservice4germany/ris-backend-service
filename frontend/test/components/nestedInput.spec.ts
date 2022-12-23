import userEvent from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import NestedInput from "@/components/NestedInput.vue"
import { NestedInputAttributes, NestedInputModelType } from "@/domain"
import { defineTextField, defineDateField } from "@/domain/coreDataFields"

function renderComponent(options?: {
  ariaLabel?: string
  modelValue?: NestedInputModelType
  fields?: NestedInputAttributes["fields"]
}) {
  const props = {
    ariaLabel: options?.ariaLabel ?? "Toggle label",
    modelValue: options?.modelValue,
    fields: options?.fields ?? {
      parent: defineTextField(
        "text input 1",
        "text input 1",
        "text input 1 label",
        false
      ),
      child: defineTextField(
        "text input 2",
        "text input 2",
        "text input 2 label",
        false
      ),
    },
  }
  const utils = render(NestedInput, { props })
  const user = userEvent.setup()
  return { user, props, ...utils }
}

describe("NestedInput", () => {
  global.ResizeObserver = require("resize-observer-polyfill")
  it("renders nested input with two text input fields", async () => {
    renderComponent()

    const input1 = screen.queryByLabelText(
      "text input 1 label"
    ) as HTMLInputElement
    const input2 = screen.queryByLabelText(
      "text input 2 label"
    ) as HTMLInputElement

    expect(input1).toBeInTheDocument()
    expect(input2).toBeInTheDocument()

    expect(input1).toBeVisible()
    expect(input2).not.toBeVisible()
  })

  it("updates value when user types in input fields", async () => {
    const { user } = renderComponent({
      modelValue: { fields: { parent: "foo", child: "bar" } },
    })

    const input1 = screen.queryByLabelText(
      "text input 1 label"
    ) as HTMLInputElement
    const input2 = screen.queryByLabelText(
      "text input 2 label"
    ) as HTMLInputElement

    await user.type(input1, " bar")
    expect(input1).toHaveValue("foo bar")

    await user.type(input2, " foo")
    expect(input2).toHaveValue("bar foo")

    expect(screen.getByDisplayValue("foo bar")).toBeInTheDocument()
    expect(screen.getByDisplayValue("bar foo")).toBeInTheDocument()
  })

  it("renders input with dynamic types", async () => {
    renderComponent({
      fields: {
        parent: defineTextField(
          "text input 1",
          "text input 1",
          "text input label",
          false
        ),
        child: defineDateField(
          "decisionDate",
          "Entscheidungsdatum",
          "date input label",
          true,
          undefined
        ),
      },
    })

    const input1 = screen.queryByLabelText(
      "text input label"
    ) as HTMLInputElement
    const input2 = screen.queryByLabelText(
      "date input label"
    ) as HTMLInputElement

    expect(input1).toHaveAttribute("type", "text")
    expect(input2).toHaveAttribute("type", "date")
  })
})
