import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import ChipsYearInput from "@/components/input/ChipsYearInput.vue"

type YearChipsInputProps = InstanceType<typeof ChipsYearInput>["$props"]

function renderComponent(props?: Partial<YearChipsInputProps>) {
  const user = userEvent.setup()

  let modelValue: string[] | undefined = props?.modelValue ?? []

  const effectiveProps: YearChipsInputProps = {
    id: props?.id ?? "identifier",
    modelValue,
    "onUpdate:modelValue":
      props?.["onUpdate:modelValue"] ??
      ((val: string[] | undefined) => (modelValue = val)),
    "onUpdate:validationError": props?.["onUpdate:validationError"],
    ariaLabel: props?.ariaLabel ?? "aria-label",
  }

  return { user, ...render(ChipsYearInput, { props: effectiveProps }) }
}

describe("ChipsYearInput", () => {
  it("shows a chips input element", () => {
    renderComponent()
    const input = screen.getByRole<HTMLInputElement>("textbox")

    expect(input).toBeInTheDocument()
    expect(input?.type).toBe("text")
  })

  it("shows dates in correct format", () => {
    renderComponent({ modelValue: ["2020"] })

    const chips = screen.getAllByRole("listitem")
    expect(chips).toHaveLength(1)
    expect(chips[0]).toHaveTextContent("2020")
  })

  it("converts date format of user input", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({ "onUpdate:modelValue": onUpdate })

    const input = screen.getByRole("textbox")
    await user.type(input, "2020{enter}")
    expect(onUpdate).toHaveBeenCalledWith(["2020"])
  })

  it("uses date input maska", async () => {
    const { user } = renderComponent()

    const input = screen.getByRole("textbox")
    await user.type(input, "abd§202sa0")
    expect(input).toHaveValue("2020")
  })

  it("does not accept incorrect year", async () => {
    const id = "id"

    const onError = vi.fn()
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      id: id,
      "onUpdate:modelValue": onUpdate,
      "onUpdate:validationError": onError,
    })

    const input = screen.getByRole("textbox")
    await user.type(input, "999{enter}")

    expect(onUpdate).not.toHaveBeenCalled()
    expect(onError).toHaveBeenCalledWith({
      message: "Kein valides Jahr",
      instance: id,
    })
  })

  it("does not accept dates in future", async () => {
    const id = "id"
    const ariaLabel = "chip"

    const onError = vi.fn()
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      id: id,
      ariaLabel: ariaLabel,
      "onUpdate:modelValue": onUpdate,
      "onUpdate:validationError": onError,
    })

    const input = screen.getByRole("textbox")
    await user.type(input, "2100{enter}")

    expect(onUpdate).not.toHaveBeenCalled()
    expect(onError).toHaveBeenCalledWith({
      message: ariaLabel + " darf nicht in der Zukunft liegen",
      instance: id,
    })
  })

  it("does not accept existing dates", async () => {
    const id = "id"
    const ariaLabel = "chip"

    const onError = vi.fn()
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      id: id,
      ariaLabel: ariaLabel,
      "onUpdate:modelValue": onUpdate,
      "onUpdate:validationError": onError,
      modelValue: ["2020", "2021"],
    })

    const input = screen.getByRole("textbox")
    await user.type(input, "2021{enter}")

    expect(onUpdate).not.toHaveBeenCalled()
    expect(onError).toHaveBeenCalledWith({
      message: "2021 bereits vorhanden",
      instance: id,
    })
  })

  it("deletes the focused chip on enter", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      "onUpdate:modelValue": onUpdate,
      modelValue: ["2020", "2021"],
    })

    const chips = screen.getAllByRole("listitem")
    await user.click(chips[1])
    await user.keyboard("{enter}")
    expect(onUpdate).toHaveBeenCalledWith(["2020"])
  })
})
