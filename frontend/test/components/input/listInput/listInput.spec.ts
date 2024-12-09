import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import ListInput from "@/components/input/listInput/ListInput.vue"

const scrollIntoViewMock = vi.fn()
window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock

function renderComponent(
  options: {
    props?: {
      label: string
      modelValue: string[]
    }
  } = {
    props: {
      label: "Liste",
      modelValue: [],
    },
  },
) {
  const user = userEvent.setup()

  return {
    user,
    ...render(ListInput, {
      props: options.props,
    }),
  }
}

describe("List input", () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  test("if no model value, renders edit mode", async () => {
    renderComponent()

    // renders label
    expect(screen.getByText("Liste", { exact: true })).toBeInTheDocument()
    expect(
      screen.getByPlaceholderText(
        "Geben Sie jeden Wert in eine eigene Zeile ein",
      ),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText("Liste bearbeiten")).not.toBeInTheDocument()
  })

  test("if model value, renders display mode", async () => {
    renderComponent({
      props: {
        label: "Liste",
        modelValue: ["one", "two"],
      },
    })
    expect(await screen.findByLabelText("Liste bearbeiten")).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)
    expect(
      screen.queryByPlaceholderText(
        "Geben Sie jeden Wert in eine eigene Zeile ein",
      ),
    ).not.toBeInTheDocument()
  })

  test('in display mode, clicking on "Liste bearbeiten" opens edit mode', async () => {
    const { user } = renderComponent({
      props: {
        label: "Liste",
        modelValue: ["one", "two"],
      },
    })
    expect(await screen.findByLabelText("Liste bearbeiten")).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)

    await user.click(screen.getByLabelText("Liste bearbeiten"))
    expect(screen.getByLabelText("Liste Input")).toHaveValue("one\ntwo")
    expect(scrollIntoViewMock).not.toHaveBeenCalled()
  })

  test('in edit mode, click on "Übernehmen" with input, emits new input, open display mode', async () => {
    const { user, emitted } = renderComponent()

    await user.type(screen.getByLabelText("Liste Input"), "one {enter}")
    await user.click(screen.getByLabelText("Liste übernehmen"))
    // emits input
    expect(emitted()["update:modelValue"]).toEqual([[["one"]]])
    // renders display mode
    expect(await screen.findByLabelText("Liste bearbeiten")).toBeVisible()
    // Display component will be scrolled into view
    expect(scrollIntoViewMock).toHaveBeenCalledOnce()
  })

  test('in edit mode, click on "Übernehmen" adds new content to existing keywords correctly', async () => {
    const { user, emitted } = renderComponent({
      props: {
        label: "Liste",
        modelValue: ["one", "two"],
      },
    })

    expect(await screen.findByLabelText("Liste bearbeiten")).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)

    await user.click(screen.getByLabelText("Liste bearbeiten"))
    await user.type(
      screen.getByLabelText("Liste Input"),
      "{enter}three {enter}",
    )

    await user.click(screen.getByLabelText("Liste übernehmen"))
    // emits input
    expect(emitted()["update:modelValue"]).toEqual([[["one", "two", "three"]]])
  })

  test('in edit mode, click on "Übernehmen" with no input stays in edit mode', async () => {
    const { user } = renderComponent()

    await user.click(screen.getByLabelText("Liste übernehmen"))
    expect(screen.queryByLabelText("Liste bearbeiten")).not.toBeInTheDocument()
    expect(screen.getByLabelText("Liste Input")).toHaveValue("")
    expect(scrollIntoViewMock).not.toHaveBeenCalled()
  })

  test('in edit mode, click on "Abbrechen" with input reverts to display mode without changes', async () => {
    const { user } = renderComponent({
      props: {
        label: "Liste",
        modelValue: ["one", "two"],
      },
    })

    expect(await screen.findByLabelText("Liste bearbeiten")).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)

    await user.click(screen.getByLabelText("Liste bearbeiten"))
    await user.type(screen.getByLabelText("Liste Input"), "three {enter}")

    await user.click(screen.getByLabelText("Abbrechen"))
    expect(screen.getByLabelText("Liste bearbeiten")).toBeInTheDocument()
    expect(await screen.findByLabelText("Liste bearbeiten")).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)
    expect(screen.queryByText(/three/)).not.toBeInTheDocument()
    expect(scrollIntoViewMock).toHaveBeenCalledOnce()
  })

  test('in edit mode, click on "Abbrechen" with no input stays in edit mode', async () => {
    const { user } = renderComponent()

    await user.click(screen.getByLabelText("Abbrechen"))

    expect(screen.queryByLabelText("Liste bearbeiten")).not.toBeInTheDocument()
    expect(scrollIntoViewMock).not.toHaveBeenCalled()
  })

  test("sort alphabetically", async () => {
    const { user } = renderComponent({
      props: {
        label: "Liste",
        modelValue: ["one", "two", "three"],
      },
    })
    expect(await screen.findByLabelText("Liste bearbeiten")).toBeVisible()
    const chips = screen.getAllByTestId("chip").map((chip) => chip.textContent)
    expect(chips[0]).toBe("one")
    expect(chips[1]).toBe("two")
    expect(chips[2]).toBe("three")

    await user.click(screen.getByLabelText("Liste bearbeiten"))
    await user.click(screen.getByLabelText("Alphabetisch sortieren"))
    await user.click(screen.getByLabelText("Liste übernehmen"))

    const chipsSorted = screen
      .getAllByTestId("chip")
      .map((chip) => chip.textContent)
    expect(chipsSorted[0]).toBe("one")
    expect(chipsSorted[1]).toBe("three")
    expect(chipsSorted[2]).toBe("two")
  })

  test("add duplicates not possible", async () => {
    const { user } = renderComponent({
      props: {
        label: "Liste",
        modelValue: ["one", "two"],
      },
    })

    expect(await screen.findByLabelText("Liste bearbeiten")).toBeVisible()
    expect(screen.getAllByTestId("chip").length).toBe(2)

    await user.click(screen.getByLabelText("Liste bearbeiten"))
    await user.type(screen.getByLabelText("Liste Input"), "{enter}two {enter}")

    await user.click(screen.getByLabelText("Liste übernehmen"))
    expect(screen.getByLabelText("Liste bearbeiten")).toBeInTheDocument()
    expect(screen.getAllByTestId("chip").length).toBe(2)
  })
})
