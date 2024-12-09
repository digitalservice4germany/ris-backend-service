import { render, screen, fireEvent } from "@testing-library/vue"
import { describe, it, expect } from "vitest"
import Tooltip from "@/components/Tooltip.vue"

describe("Tooltip", () => {
  it("tooltip is hidden by default when isVisible is false", () => {
    render(Tooltip, {
      props: { text: "This is a tooltip" },
    })

    // Tooltip should not be visible initially
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument()
  })

  it("opens and closes tooltip on hover and mouseleave", async () => {
    render(Tooltip, {
      props: { text: "Hovered tooltip" },
      slots: { default: "Hover over me" },
    })

    const target = screen.getByText("Hover over me")

    // Initially the tooltip should not be visible
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument()

    // Fire mouseenter event to show the tooltip
    await fireEvent.mouseEnter(target)

    // Tooltip should now be visible
    expect(screen.getByRole("tooltip")).toBeInTheDocument()

    // Fire mouseleave event to hide the tooltip
    await fireEvent.mouseLeave(target)

    // Tooltip should now be hidden
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument()
  })

  it("opens and closes tooltip on focus and blur", async () => {
    render(Tooltip, {
      props: { text: "Focused tooltip" },
      slots: { default: "Hover over me" },
    })

    const target = screen.getByText("Hover over me")

    // Initially the tooltip should not be visible
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument()

    // Fire focus event to show the tooltip
    await fireEvent.focus(target)

    // Tooltip should now be visible
    expect(screen.getByRole("tooltip")).toBeInTheDocument()

    // Fire blur event to hide the tooltip
    await fireEvent.blur(target)

    // Tooltip should now be hidden
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument()
  })

  it("closes the tooltip when the Escape key is pressed", async () => {
    render(Tooltip, {
      props: { text: "Escape tooltip" },
      slots: { default: "Hover over me" },
    })

    const target = screen.getByText("Hover over me")

    // Initially the tooltip should not be visible
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument()

    // Fire focus event to show the tooltip
    await fireEvent.focus(target)

    // Tooltip should now be visible
    expect(screen.getByRole("tooltip")).toBeInTheDocument()

    // Fire Escape key event to hide the tooltip
    await fireEvent.keyDown(window, { key: "Escape" })

    // Tooltip should now be hidden
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument()
  })

  it("renders the shortcut text when provided", async () => {
    render(Tooltip, {
      props: {
        text: "Tooltip with shortcut",
        shortcut: "Ctrl+C",
      },
      slots: { default: "Hover over me" },
    })

    const target = screen.getByText("Hover over me")

    // Initially the tooltip should not be visible
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument()

    // Fire focus event to show the tooltip
    await fireEvent.focus(target)

    // Tooltip should now be visible
    expect(screen.getByRole("tooltip")).toBeInTheDocument()

    // Check both the tooltip text and shortcut
    expect(screen.getByRole("tooltip")).toHaveTextContent(
      "Tooltip with shortcut",
    )
    expect(screen.getByRole("tooltip")).toHaveTextContent("Ctrl+C")
  })
})
