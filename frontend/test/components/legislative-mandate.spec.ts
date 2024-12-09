import { createTestingPinia } from "@pinia/testing"
import { render, screen } from "@testing-library/vue"
import { setActivePinia } from "pinia"
import LegislativeMandate from "@/components/LegislativeMandate.vue"
import DocumentUnit from "@/domain/documentUnit"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

function mockSessionStore(value: { legislativeMandate: boolean }) {
  const mockedSessionStore = useDocumentUnitStore()
  mockedSessionStore.documentUnit = new DocumentUnit("q834", {
    contentRelatedIndexing: {
      hasLegislativeMandate: value.legislativeMandate,
    },
  })

  return mockedSessionStore
}
describe("legislative mandate", () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia())
  })

  afterEach(() => void vi.resetAllMocks())
  test("should have checkbox unchecked when legislative mandate is false", async () => {
    // Arrange
    const headline = "Gesetzgebungsauftrag"
    const label = "Gesetzgebungsauftrag vorhanden"
    mockSessionStore({ legislativeMandate: false })

    // Act
    render(LegislativeMandate, {
      props: {
        headline: headline,
        label: label,
      },
    })

    // Assert
    expect(screen.getByText(headline)).toBeInTheDocument()
    expect(screen.getByText(label)).toBeInTheDocument()
    expect(screen.getByTestId("legislative-mandate")).not.toBeChecked()
  })

  test("should have checkbox checked when legislative mandate is true", async () => {
    // Arrange
    const headline = "Gesetzgebungsauftrag"
    const label = "Gesetzgebungsauftrag vorhanden"
    mockSessionStore({ legislativeMandate: true })

    // Act
    render(LegislativeMandate, {
      props: {
        headline: "Gesetzgebungsauftrag",
        label: label,
      },
    })

    // Assert
    expect(screen.getByText(headline)).toBeInTheDocument()
    expect(screen.getByText(label)).toBeInTheDocument()
    expect(screen.getByTestId("legislative-mandate")).toBeChecked()
  })
})
