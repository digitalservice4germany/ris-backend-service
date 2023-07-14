import userEvent from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import {
  defineTextField,
  defineChipsField,
} from "@/fields/caselaw/coreDataFields"
import NestedInput from "@/shared/components/input/NestedInput.vue"
import {
  NestedInputAttributes,
  NestedInputModelType,
} from "@/shared/components/input/types"

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
      ),
      child: defineTextField(
        "text input 2",
        "text input 2",
        "text input 2 label",
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
      "text input 1 label",
    ) as HTMLInputElement
    const input2 = screen.queryByLabelText(
      "text input 2 label",
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
      "text input 1 label",
    ) as HTMLInputElement
    const input2 = screen.queryByLabelText(
      "text input 2 label",
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
        ),
        child: defineChipsField(
          "chips input",
          "chips input",
          "chips input label",
          "",
        ),
      },
    })

    const input1 = screen.queryByLabelText(
      "text input label",
    ) as HTMLInputElement
    const input2 = screen.queryByLabelText(
      "chips input label",
    ) as HTMLInputElement

    expect(input1).toHaveAttribute("type", "text")
    expect(input2).toHaveAttribute("type", "text")
  })
})
