import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { describe } from "vitest"
import { Component, markRaw, ref, h } from "vue"
import { ComponentExposed } from "vue-component-type-helpers"
import { withSummarizer } from "@/components/DataSetSummary.vue"
import EditableList from "@/components/EditableList.vue"
import EditableListItem from "@/domain/editableListItem"
import DummyInputGroupVue from "@/kitchensink/components/DummyInputGroup.vue"
import DummyListItem from "@/kitchensink/domain/dummyListItem"

const listWithEntries = ref<DummyListItem[]>([
  new DummyListItem({ text: "foo", uuid: "123" }),
  new DummyListItem({ text: "bar", uuid: "124" }),
])

function summerizer(dataEntry: EditableListItem) {
  return h("div", { class: ["ds-label-01-reg"] }, dataEntry.renderDecision)
}

const SummaryComponent = withSummarizer(summerizer)

type EditableListProps<T extends EditableListItem> = ComponentExposed<
  typeof EditableList<T>
  //@ts-expect-error("wrong type")
>["$props"]

async function renderComponent<T>(options?: {
  editComponent?: Component
  summaryComponent?: Component
  modelValue?: T[]
  defaultValue?: T
  disableMultiEntry?: boolean
}) {
  const props: EditableListProps<T> = {
    editComponent: markRaw(options?.editComponent ?? DummyInputGroupVue),
    summaryComponent: markRaw(options?.summaryComponent ?? SummaryComponent),
    modelValue: options?.modelValue ?? listWithEntries.value,
    defaultValue: options?.defaultValue ?? new DummyListItem(),
    disableMultiEntry: options?.disableMultiEntry ?? false,
  }

  const user = userEvent.setup()
  return {
    user,
    ...render(EditableList, { props }),
  }
}

describe("EditableList", () => {
  it("renders a summary per model entry on initial render with entries", async () => {
    await renderComponent()

    expect(screen.getByText("foo")).toBeVisible()
    expect(screen.getByText("bar")).toBeVisible()

    expect(screen.getByLabelText("Weitere Angabe")).toBeVisible()
  })

  it("shows edit component for default value when adding new new entry via button click", async () => {
    const { user } = await renderComponent()
    expect(screen.queryByLabelText("Editier Input")).not.toBeInTheDocument()
    expect(screen.getByText("foo")).toBeVisible()
    expect(screen.getByText("bar")).toBeVisible()
    await user.click(screen.getByLabelText("Weitere Angabe"))
    expect(screen.getByLabelText("Editier Input")).toBeVisible()
  })

  it("shows edit component when list item is clicked", async () => {
    const { user } = await renderComponent()

    await user.click(screen.getByTestId("list-entry-0"))

    expect(screen.getByLabelText("Editier Input")).toHaveValue("foo")
    expect(screen.getByLabelText("Listeneintrag speichern")).toBeVisible()
    expect(screen.getByLabelText("Abbrechen")).toBeVisible()
    expect(screen.getByLabelText("Eintrag löschen")).toBeVisible()
  })

  it("delete button emits modelValue without the deleted entry", async () => {
    const { user, emitted } = await renderComponent()
    await user.click(screen.getByTestId("list-entry-0"))
    await user.click(screen.getByLabelText("Eintrag löschen"))

    expect(listWithEntries.value.length).toEqual(2)

    expect(emitted()["update:modelValue"]).toEqual([
      [
        [
          {
            text: "bar",
            uuid: "124",
          },
        ],
      ],
    ])

    expect(
      screen.getByLabelText("Weitere Angabe"),
      "Deleting did not reset edit entry",
    ).toBeVisible()
  })

  it("automatically adds a default entry in edit mode if list is empty on initial render", async () => {
    await renderComponent({ modelValue: [] })

    expect(screen.getByLabelText("Editier Input")).toBeVisible()
    expect(screen.queryByLabelText("Weitere Angabe")).not.toBeInTheDocument()
    expect(screen.getByLabelText("Listeneintrag speichern")).toBeVisible()
    expect(screen.getByLabelText("Listeneintrag speichern")).toBeDisabled()

    //with no inputs, there is no cancel or delete button
    expect(screen.queryByLabelText("Abbrechen")).not.toBeInTheDocument()
    expect(screen.queryByLabelText("Eintrag löschen")).not.toBeInTheDocument()
  })

  it("updates the model value entry on editing it", async () => {
    const { emitted, user } = await renderComponent()

    await user.click(screen.getByTestId("list-entry-0"))
    await user.type(screen.getByLabelText("Editier Input"), "1")
    await user.click(screen.getByLabelText("Listeneintrag speichern"))

    expect(emitted()["update:modelValue"]).toEqual([
      [
        [
          {
            text: "foo1",
            uuid: "123",
          },
          {
            text: "bar",
            uuid: "124",
          },
        ],
      ],
    ])
  })

  it("closes the editing component if user clicks cancel button, changes not saved", async () => {
    const { user } = await renderComponent()

    await user.click(screen.getByTestId("list-entry-0"))

    expect(screen.getByLabelText("Editier Input")).toBeVisible()
    await user.type(screen.getByLabelText("Editier Input"), "1")
    await user.click(screen.getByLabelText("Abbrechen"))
    expect(screen.queryByText("foo1")).not.toBeInTheDocument()
    expect(screen.getByText("foo")).toBeVisible()
  })

  it("removes the current entry if no inputs made and a different entry gets edited afterwards", async () => {
    const { user } = await renderComponent()

    expect(screen.getAllByLabelText("Listen Eintrag").length).toEqual(2)
    await user.click(screen.getByLabelText("Weitere Angabe"))

    //no inputs made, click in other entry
    await user.click(screen.getByTestId("list-entry-0"))

    expect(screen.getAllByLabelText("Listen Eintrag").length).toEqual(2)
  })

  describe("Scrolling behavior", () => {
    it("scrolls to the item being edited after cancel", async () => {
      // Arrange
      const { user } = await renderComponent()
      const scrollIntoViewMock = vi.fn()
      const item = screen.getByTestId("list-entry-0")
      window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock
      await user.click(item)

      // Act
      await user.click(screen.getByLabelText("Abbrechen"))

      // Assert
      expect(scrollIntoViewMock).toHaveBeenCalledTimes(1)
    })

    it("scrolls to the item being edited after 'übernehmen''", async () => {
      // Arrange
      const { user } = await renderComponent()
      const scrollIntoViewMock = vi.fn()
      const item = screen.getByTestId("list-entry-0")
      window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock
      await user.click(item)
      expect(screen.getByLabelText("Editier Input")).toBeVisible()
      await user.type(screen.getByLabelText("Editier Input"), "1")
      const button = screen.getByLabelText("Listeneintrag speichern")

      // Act
      await user.click(button)

      // Assert
      expect(scrollIntoViewMock).toHaveBeenCalledTimes(1)
    })

    it("scrolls to editable list container if an item has been deleted", async () => {
      // Arrange
      const { user } = await renderComponent()
      const scrollIntoViewMock = vi.fn()
      const item = screen.getByTestId("list-entry-0")
      window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock
      await user.click(item)

      // Act
      await user.click(screen.getByLabelText("Eintrag löschen"))

      // Assert
      expect(scrollIntoViewMock).toHaveBeenCalledTimes(1)
    })
  })
})
