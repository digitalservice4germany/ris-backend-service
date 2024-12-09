import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import ListInputEdit from "@/components/input/listInput/ListInputEdit.vue"

function renderComponent(
  options: {
    props?: {
      label: string
      modelValue: string
      sortAlphabetically: boolean
      listItemCount: number
    }
  } = {
    props: {
      label: "Liste",
      modelValue: "",
      sortAlphabetically: false,
      listItemCount: 0,
    },
  },
) {
  const user = userEvent.setup()

  return {
    user,
    ...render(ListInputEdit, {
      props: options.props,
    }),
  }
}

describe("List input edit mode", () => {
  test("renders textarea with label", async () => {
    renderComponent()

    expect(screen.getByLabelText("Liste Input")).toBeInTheDocument()
    expect(screen.getByText("Liste", { exact: true })).toBeInTheDocument()
  })

  test("if modelValue, displays value correctly in textarea", async () => {
    renderComponent({
      props: {
        label: "Liste",
        modelValue: "one\ntwo",
        sortAlphabetically: false,
        listItemCount: 2,
      },
    })

    const textarea = screen.getByLabelText("Liste Input")
    expect(textarea).toHaveValue("one\ntwo")
  })

  test('click on "Übernehmen" with input, emits update model value event', async () => {
    const { user, emitted } = renderComponent({
      props: {
        label: "Liste",
        modelValue: "one",
        sortAlphabetically: false,
        listItemCount: 2,
      },
    })

    const button = screen.getByLabelText("Liste übernehmen")
    await user.click(button)

    expect(emitted()["update:modelValue"]).toBeTruthy()
    expect(emitted()["update:modelValue"][0]).toEqual(["one"])
  })

  test('click on "Übernehmen" with empty input emits update model value event', async () => {
    const { user, emitted } = renderComponent()

    const button = screen.getByLabelText("Liste übernehmen")
    await user.click(button)

    expect(emitted()["update:modelValue"]).toBeTruthy()
    expect(emitted()["update:modelValue"][0]).toEqual([""])
  })

  test('click on "Abbrechen" with input emits toggle event', async () => {
    const { user, emitted } = renderComponent({
      props: {
        label: "Liste",
        modelValue: "one\ntwo",
        sortAlphabetically: false,
        listItemCount: 2,
      },
    })

    const textarea = screen.getByLabelText("Liste Input")
    await user.type(textarea, "new item")
    const cancelButton = screen.getByLabelText("Abbrechen")
    await user.click(cancelButton)

    expect(emitted().toggle).toBeTruthy()
    expect(emitted().toggle).toHaveLength(1)
    expect(emitted()["update:modelValue"]).toBeFalsy()
  })

  test('click on "Abbrechen" with no input emits toggle event', async () => {
    const { user, emitted } = renderComponent()

    await user.click(screen.getByLabelText("Abbrechen"))

    expect(emitted().toggle).toBeTruthy()
    expect(emitted().toggle).toHaveLength(1)
    expect(emitted()["update:modelValue"]).toBeFalsy()
  })

  test("click on sort alphabetically, emits event", async () => {
    const { user, emitted } = renderComponent({
      props: {
        label: "Liste",
        modelValue: "one\ntwo",
        sortAlphabetically: false,
        listItemCount: 2,
      },
    })

    const checkbox = screen.getByLabelText("Alphabetisch sortieren")
    await user.click(checkbox)

    expect(emitted().toggleSorting).toBeTruthy()
    expect(emitted().toggleSorting[0]).toEqual([true])
  })
})
