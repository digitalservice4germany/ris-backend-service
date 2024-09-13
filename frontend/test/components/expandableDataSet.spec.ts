import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { Component, defineComponent, markRaw } from "vue"
import ExpandableDataSet from "@/components/ExpandableDataSet.vue"

const JsonStringifySummary = defineComponent({
  props: {
    data: {
      default: undefined,
      validator: () => true,
    },
  },
  template: "<span>{{ JSON.stringify(data) }}</span>",
})

function renderComponent(options?: {
  title?: string
  dataSet?: any // eslint-disable-line @typescript-eslint/no-explicit-any
  summaryComponent?: Component
  defaultSlot?: string
}) {
  const props = {
    title: options?.title ?? "title",
    dataSet: options?.dataSet ?? [],
    summaryComponent: markRaw(
      options?.summaryComponent ?? JsonStringifySummary,
    ),
  }

  const slots = {
    default: options?.defaultSlot ?? "",
  }

  render(ExpandableDataSet, { props, slots })
}

describe("ExpandableDataSet", () => {
  beforeAll(() => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    global.ResizeObserver = require("resize-observer-polyfill")
  })

  vi.spyOn(window, "scrollTo").mockImplementation(() => vi.fn())

  it("shows the given title", () => {
    renderComponent({ title: "test title" })

    const title = screen.queryByText("test title")

    expect(title).toBeVisible()
  })

  it("shows a summary with the given dataset on initial render", () => {
    renderComponent({
      dataSet: ["foo", "bar"],
      summaryComponent: JsonStringifySummary,
    })

    const summary = screen.queryByText('["foo","bar"]')

    expect(summary).toBeVisible()
  })

  it("hides the default slot and close button on initial render", () => {
    renderComponent({ defaultSlot: "special" })

    const slot = screen.queryByText("special")
    const button = screen.queryByText("Fertig")

    expect(slot).not.toBeInTheDocument()
    expect(button).not.toBeInTheDocument()
  })

  it("toggles the default slot and close button when clicking on title", async () => {
    const user = userEvent.setup()
    renderComponent({ title: "test title", defaultSlot: "content" })
    const title = screen.getByText("test title")

    await user.click(title)

    expect(screen.queryByText("content")).toBeVisible()
    expect(screen.queryByText("Fertig")).toBeVisible()

    await user.click(title)

    expect(screen.queryByText("content")).not.toBeInTheDocument()
    expect(screen.queryByText("Fertig")).not.toBeInTheDocument()
  })

  it("shows the default slot and close button when clicking on the summary", async () => {
    const user = userEvent.setup()
    renderComponent({
      dataSet: ["foo", "bar"],
      summaryComponent: JsonStringifySummary,
      defaultSlot: "content",
    })
    const summary = screen.getByText('["foo","bar"]')

    await user.click(summary)

    expect(screen.queryByText("content")).toBeVisible()
    expect(screen.queryByText("Fertig")).toBeVisible()
  })

  it("hides the summary when opening data set", async () => {
    const user = userEvent.setup()
    renderComponent({
      dataSet: ["foo", "bar"],
      summaryComponent: JsonStringifySummary,
      defaultSlot: "content",
    })

    const summary = screen.getByText('["foo","bar"]')
    await user.click(summary)

    expect(summary).not.toBeVisible()
  })

  it("it hides the default slot and close button when clicking the latter", async () => {
    const user = userEvent.setup()
    renderComponent({ title: "test title", defaultSlot: "content" })
    const title = screen.getByText("test title")
    await user.click(title)
    const button = screen.getByText("Fertig")

    await user.click(button)
    const slot = screen.queryByText("content")

    expect(button).not.toBeInTheDocument()
    expect(slot).not.toBeInTheDocument()
  })
})
