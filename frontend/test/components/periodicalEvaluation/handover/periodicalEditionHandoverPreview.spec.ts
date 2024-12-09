import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import PeriodicalEditionHandoverPreview from "@/components/periodical-evaluation/handover/PeriodicalEditionHandoverPreview.vue"

import { Preview } from "@/domain/eventRecord"

function renderComponent(props = {}) {
  const user = userEvent.setup()

  return {
    user,
    ...render(PeriodicalEditionHandoverPreview, {
      props: {
        ...props,
      },
      global: {
        stubs: {
          CodeSnippet: {
            template: '<div data-testid="code-snippet"/>',
          },
        },
      },
    }),
  }
}

describe("Periodical edition handover XML preview", () => {
  test("renders ExpandableContent with preview data", async () => {
    const { user } = renderComponent({
      preview: [
        new Preview({
          xml: "<xml>Preview Content</xml>",
          success: true,
        }),
      ],
    })
    expect(screen.getByLabelText("XML Vorschau")).toBeInTheDocument()
    await user.click(screen.getByLabelText("Aufklappen"))
    const codeSnippet = screen.queryByTestId("code-snippet")

    expect(codeSnippet).toBeInTheDocument()

    expect(codeSnippet).toHaveAttribute("XML")
    expect(codeSnippet?.getAttribute("xml")).toBe("<xml>Preview Content</xml>")
  })

  test("renders InfoModal with preview error", async () => {
    renderComponent({
      previewError: {
        title: "Fehler beim Laden",
        description: "Preview konnte nicht geladen werden",
      },
    })

    const modal = await screen.findByLabelText("Fehler beim Laden der Preview")
    expect(modal).toBeInTheDocument()
    expect(modal).toHaveTextContent("Fehler beim Laden")
    expect(modal).toHaveTextContent("Preview konnte nicht geladen werden")
  })

  test("does not render InfoModal or ExpandableContent when there is no preview or error", async () => {
    renderComponent()

    expect(screen.queryByLabelText("XML Vorschau")).not.toBeInTheDocument()
    expect(
      screen.queryByLabelText("Fehler beim Laden der Preview"),
    ).not.toBeInTheDocument()
  })
})
