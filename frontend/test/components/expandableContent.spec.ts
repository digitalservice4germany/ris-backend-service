import userEvent from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import ExpandableContentnt from "@/components/ExpandableContent.vue"

function renderComponent(options?: {
  header?: string
  isExpanded?: boolean
  defaultSlot?: string
  headerSlot?: string
  iconsOnLeft?: boolean
  marginLeft?: number
}) {
  const slots = {
    default: options?.defaultSlot ?? "",
    header: options?.headerSlot ?? "",
  }
  const props = {
    header: options?.header,
    isExpanded: options?.isExpanded,
    iconsOnLeft: options?.iconsOnLeft,
    marginLeft: options?.marginLeft,
  }
  const utils = render(ExpandableContentnt, { slots, props })
  const user = userEvent.setup()
  return { user, ...utils }
}

describe("ExpandableContent", () => {
  global.ResizeObserver = require("resize-observer-polyfill")
  it("displays given header property as regular text", () => {
    renderComponent({ header: "test header" })
    const header = screen.queryByText("test header")

    expect(header).toBeInTheDocument()
  })

  it("renders given header slot", () => {
    renderComponent({
      headerSlot: "<span>test header</span>",
    })
    const header = screen.queryByText("test header")

    expect(header).toBeInTheDocument()
  })

  it("renders default slot into content", () => {
    renderComponent({ defaultSlot: "test content", isExpanded: true })
    const content = screen.queryByText("test content")

    expect(content).toBeInTheDocument()
  })

  it("hides content per default", () => {
    renderComponent({ defaultSlot: "test content" })
    const content = screen.queryByText("test content")

    expect(content).not.toBeInTheDocument()
  })

  it("can open content per default with property", () => {
    renderComponent({
      isExpanded: true,
      defaultSlot: "test content",
    })
    const content = screen.getByText("test content")

    expect(content).toBeVisible()
  })

  it("body can be toggled by clicking", async () => {
    const { user } = renderComponent({
      defaultSlot: "test content",
    })
    const header = screen.getByRole("button")

    expect(screen.queryByText("test content")).not.toBeInTheDocument()

    await user.click(header)

    expect(screen.getByText("test content")).toBeInTheDocument()

    await user.click(header)

    expect(screen.queryByText("test content")).not.toBeInTheDocument()
  })

  it("emits update event when content gets toggled", async () => {
    const { emitted, user } = renderComponent()
    const header = screen.getByRole("button")

    await user.click(header)
    await user.click(header)
    await user.click(header)

    expect(emitted()["update:isExpanded"]).toHaveLength(3)
    expect(emitted()["update:isExpanded"]).toEqual([[true], [false], [true]])
  })

  it("renders default 0 margin left", () => {
    renderComponent()
    const button = screen.getByRole("button")

    expect(button).toHaveClass("ml-[0px]")
  })

  it("renders a non-default margin left for the button", () => {
    renderComponent({ marginLeft: 40 })
    const button = screen.getByRole("button")

    expect(button).toHaveClass("ml-[40px]")
  })

  it("renders default icons on the right side", () => {
    renderComponent()

    const icon = screen.getByTestId("icons-open-close")

    expect(icon).toHaveClass("icon")
  })

  it("renders icons on the left side", () => {
    renderComponent({ iconsOnLeft: true })

    const icon = screen.getByTestId("icons-open-close")

    expect(icon).toHaveClass("icon")
  })
})
