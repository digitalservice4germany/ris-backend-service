/* eslint-disable vue/one-component-per-file */
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import {
  Component,
  computed,
  nextTick,
  defineComponent,
  markRaw,
  ref,
} from "vue"
import EditableList from "@/shared/components/EditableList.vue"

const SimpleTextEditComponent = defineComponent({
  props: {
    modelValue: {
      type: String,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  setup(props, { emit }) {
    const value = computed({
      get: () => props.modelValue,
      set: (newValue) => emit("update:modelValue", newValue),
    })

    return { value }
  },
  template: `<input v-model='value' />`,
})

const SimpleRadioGroupComponent = defineComponent({
  components: { SimpleTextEditComponent },
  props: {
    modelValue: {
      type: String,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  setup(props, { emit }) {
    const value = computed({
      get: () => props.modelValue,
      set: (newValue) => emit("update:modelValue", newValue),
    })

    const text = ref("")

    return { value, text }
  },
  template: `
    <div>
      <label>
        <input type="radio" value="foo" v-model="value" name="group" />
        Foo
      </label>
      <label>
        <input type="radio" value="bar" v-model="value" name="group" />
        Bar
      </label>
      <label>
        <input type="radio" value="baz" v-model="value" name="group" />
        Baz
      </label>
      <SimpleTextEditComponent v-model="text" />
    </div>
  `,
})

const JsonStringifySummary = defineComponent({
  props: {
    data: {
      default: undefined,
      validator: () => true,
    },
  },
  template: "<span>{{ JSON.stringify(data) }}</span>",
})

async function renderComponent(options?: {
  editComponent?: Component
  summaryComponent?: Component
  modelValue?: unknown[]
  defaultValue?: unknown
  disableMultiEntry?: boolean
}) {
  const props = {
    editComponent: markRaw(options?.editComponent ?? SimpleTextEditComponent),
    summaryComponent: markRaw(
      options?.summaryComponent ?? JsonStringifySummary,
    ),
    modelValue: options?.modelValue,
    defaultValue: options?.defaultValue ?? "",
    disableMultiEntry: options?.disableMultiEntry ?? false,
  }

  render(EditableList, { props })
  await nextTick() // Wait for onMounted hook.
}

async function clickEditButtonOfEntry(
  entryIndex: number,
  user?: ReturnType<typeof userEvent.setup>,
): Promise<HTMLInputElement> {
  user = user ?? userEvent.setup()
  const allEditButtons = screen.getAllByRole("button", {
    name: "Eintrag bearbeiten",
  })

  await user.click(allEditButtons[entryIndex])
  return screen.getByRole("textbox") as HTMLInputElement
}

async function clickDeleteButtonOfEntry(
  entryIndex: number,
  user?: ReturnType<typeof userEvent.setup>,
): Promise<void> {
  user = user ?? userEvent.setup()
  const allDeleteButtons = screen.getAllByRole("button", {
    name: "Eintrag löschen",
  })

  await user.click(allDeleteButtons[entryIndex])
}

async function clickAddEntryButton(
  user?: ReturnType<typeof userEvent.setup>,
): Promise<void> {
  user = user ?? userEvent.setup()
  const addButton = screen.getByRole("button", { name: "Weitere Angabe" })
  await user.click(addButton)
}

describe("EditableList", () => {
  it("renders a summary per model entry on initial render", async () => {
    await renderComponent({
      summaryComponent: JsonStringifySummary,
      modelValue: ["entry 1", "entry 2"],
    })

    expect(screen.queryByText('"entry 1"')).toBeVisible()
    expect(screen.queryByText('"entry 2"')).toBeVisible()
  })

  it("shows edit component for default value when adding new new entry via button click", async () => {
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue: ["entry 1", "entry 2"],
      defaultValue: "default entry",
    })

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()

    await clickAddEntryButton()
    const input = screen.queryByRole("textbox") as HTMLInputElement

    expect(input).toBeInTheDocument()
    expect(input).toHaveValue("default entry")
  })

  it("shows edit component for correct entry when edit button is clicked", async () => {
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      summaryComponent: JsonStringifySummary,
      modelValue: ["entry 1", "entry 2", "entry 3"],
    })

    await clickEditButtonOfEntry(1)

    expect(screen.queryByText('"entry 1"')).toBeVisible()
    expect(screen.queryByText('"entry 2"')).not.toBeInTheDocument()
    expect(screen.queryByText('"entry 3"')).toBeVisible()

    const input = screen.queryByRole("textbox") as HTMLInputElement

    expect(input).toBeInTheDocument()
    expect(input).toHaveValue("entry 2")
  })

  it("shows edit component for correct entry when entry is clicked", async () => {
    const user = userEvent.setup()
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      summaryComponent: JsonStringifySummary,
      modelValue: ["entry 1", "entry 2", "entry 3"],
    })

    await user.click(screen.getByText('"entry 2"'))

    expect(screen.queryByText('"entry 1"')).toBeVisible()
    expect(screen.queryByText('"entry 2"')).not.toBeInTheDocument()
    expect(screen.queryByText('"entry 3"')).toBeVisible()

    const input = screen.queryByRole("textbox") as HTMLInputElement

    expect(input).toBeInTheDocument()
    expect(input).toHaveValue("entry 2")
  })

  it("deletes correct entry when delete button is clicked", async () => {
    const modelValue = ["entry 1", "entry 2"]
    await renderComponent({
      summaryComponent: JsonStringifySummary,
      modelValue,
    })

    await clickDeleteButtonOfEntry(1)

    expect(screen.queryByText('"entry 1"')).toBeVisible()
    expect(screen.queryByText('"entry 2"')).not.toBeInTheDocument()
    expect(modelValue).toEqual(["entry 1"])
  })

  it("correctly maintains edit state if predecessor entry gets deleted", async () => {
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      summaryComponent: JsonStringifySummary,
      modelValue: ["entry 1", "entry 2", "entry 3"],
    })

    await clickEditButtonOfEntry(1)
    await clickDeleteButtonOfEntry(0)

    const input = screen.queryByRole("textbox") as HTMLInputElement
    expect(input).toBeInTheDocument()
    expect(input).toHaveValue("entry 2")
  })

  it("correctly maintains edit state if successor entry gets deleted", async () => {
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      summaryComponent: JsonStringifySummary,
      modelValue: ["entry 1", "entry 2", "entry 3"],
    })

    await clickEditButtonOfEntry(1)
    await clickDeleteButtonOfEntry(2)

    const input = screen.queryByRole("textbox") as HTMLInputElement
    expect(input).toBeInTheDocument()
    expect(input).toHaveValue("entry 2")
  })

  it("automatically adds a default entry in edit mode if list is empty on initial render", async () => {
    const modelValue: string[] = []
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue,
      defaultValue: "default entry",
    })

    const input = screen.queryByRole("textbox") as HTMLInputElement

    expect(input).toBeInTheDocument()
    expect(input).toHaveValue("default entry")
    expect(modelValue).toEqual(["default entry"])
  })

  it("automatically adds a default entry in edit mode if list is undefined", async () => {
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue: undefined,
      defaultValue: "default entry",
    })

    const input = screen.queryByRole("textbox") as HTMLInputElement

    expect(input).toBeInTheDocument()
    expect(input).toHaveValue("default entry")
  })

  it("automatically adds a default entry in edit mode if user deletes all entries", async () => {
    const modelValue: string[] = ["entry 1", "entry 2"]
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue,
      defaultValue: "default entry",
    })

    await clickDeleteButtonOfEntry(1)
    await clickDeleteButtonOfEntry(0)

    const input = screen.queryByRole("textbox") as HTMLInputElement
    expect(input).toBeInTheDocument()
    expect(input).toHaveValue("default entry")
    expect(modelValue).toEqual(["default entry"])
  })

  it("automatically focuses the first input element of the edit component", async () => {
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue: ["entry 1", "entry 2"],
    })

    await clickEditButtonOfEntry(0)

    expect(screen.getByRole("textbox")).toHaveFocus()
  })

  it("automatically focuses the first selected group member if the edit component starts with radios", async () => {
    await renderComponent({
      editComponent: SimpleRadioGroupComponent,
      modelValue: ["bar", "baz"],
    })

    await clickEditButtonOfEntry(0)

    expect(screen.getByRole("radio", { name: "Foo" })).not.toHaveFocus()
    expect(screen.getByRole("radio", { name: "Bar" })).toHaveFocus()
  })

  it("updates the model value entry on editing it", async () => {
    const user = userEvent.setup()
    const modelValue = ["fo", "bar"]
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue,
    })

    const input = await clickEditButtonOfEntry(0, user)
    await user.type(input, "o")

    expect(modelValue).toEqual(["foo", "bar"])
  })

  it("closes the editing component if user hits the enter key inside it", async () => {
    const user = userEvent.setup()
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      summaryComponent: JsonStringifySummary,
      modelValue: ["entry 1", "entry 2"],
    })

    const input = await clickEditButtonOfEntry(0, user)
    await user.type(input, "{enter}")

    expect(input).not.toBeInTheDocument()
    expect(screen.queryByText('"entry 1"')).toBeVisible()
  })

  it("renders the component without the multi entry capability", async () => {
    await renderComponent({
      disableMultiEntry: true,
    })

    expect(
      screen.queryByRole("button", { name: "Weitere Angabe" }),
    ).not.toBeInTheDocument()
  })

  it("does not extend the number of entries if adding only empty entries", async () => {
    const modelValue: string[] = []
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue,
    })

    await clickAddEntryButton()
    await clickAddEntryButton()
    await clickAddEntryButton()

    expect(modelValue).toEqual([""])
  })

  it("removes the current entry if it was cleared and the user adds a new one", async () => {
    const user = userEvent.setup()
    const modelValue = ["entry 1"]
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue,
    })

    const input = await clickEditButtonOfEntry(0, user)
    await user.clear(input)
    await clickAddEntryButton(user)

    expect(modelValue).toEqual([""])
  })

  it("removes the current entry if it was cleared and a different entry gets edited afterwards", async () => {
    const user = userEvent.setup()
    const modelValue = ["entry 1", "entry 2"]
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue,
    })

    const input = await clickEditButtonOfEntry(1)
    await user.clear(input)
    await clickEditButtonOfEntry(0)

    expect(modelValue).toEqual(["entry 1"])
  })

  it("removes the current entry if it was cleared and the user finishes the entry", async () => {
    const user = userEvent.setup()
    const modelValue = ["entry 1", "entry 2"]
    await renderComponent({
      editComponent: SimpleTextEditComponent,
      modelValue,
    })

    const input = await clickEditButtonOfEntry(1)
    await user.clear(input)
    await user.type(input, "{enter}")

    expect(modelValue).toEqual(["entry 1"])
  })
})
