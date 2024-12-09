import { createTestingPinia } from "@pinia/testing"
import { render, screen } from "@testing-library/vue"
import { setActivePinia } from "pinia"
import { vi } from "vitest"
import { ref, Ref } from "vue"
import OtherCategories from "@/components/OtherCategories.vue"
import DocumentUnit, { ContentRelatedIndexing } from "@/domain/documentUnit"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"

let courtTypeRef: Ref | undefined = undefined
vi.mock("@/composables/useCourtType", () => {
  return {
    useInjectCourtType: () => courtTypeRef,
  }
})
function mockSessionStore(contentRelatedIndexing: ContentRelatedIndexing) {
  const mockedSessionStore = useDocumentUnitStore()
  mockedSessionStore.documentUnit = new DocumentUnit("q834", {
    contentRelatedIndexing,
  })

  return mockedSessionStore
}
describe("other categories", () => {
  beforeEach(() => {
    vi.resetModules()
    vi.resetAllMocks()
    setActivePinia(createTestingPinia())
  })
  describe("LegislativeMandate", () => {
    test("should not display legislative mandate when it is false and courtType is non-constitutional", async () => {
      // Arrange
      courtTypeRef = ref("BAG")
      mockSessionStore({ hasLegislativeMandate: false })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.getByText("Berufsbild")).toBeInTheDocument()
      expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
      expect(
        screen.queryByText("Gesetzgebungsauftrag vorhanden"),
      ).not.toBeInTheDocument()
    })

    test("should display legislative mandate button when it is false and courtType is constitutional", async () => {
      // Arrange
      courtTypeRef = ref("BVerfG")
      mockSessionStore({ hasLegislativeMandate: false })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.getByText("Berufsbild")).toBeInTheDocument()
      expect(screen.getByText("Gesetzgebungsauftrag")).toBeInTheDocument()
      expect(
        screen.queryByText("Gesetzgebungsauftrag vorhanden"),
      ).not.toBeInTheDocument()
    })

    test("should display checked legislative mandate when it is true and has constitutional courtType", async () => {
      // Arrange
      courtTypeRef = ref("BVerfG")
      mockSessionStore({ hasLegislativeMandate: true })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.getByText("Berufsbild")).toBeInTheDocument()
      expect(screen.getByText("Gesetzgebungsauftrag")).toBeInTheDocument()
      expect(
        screen.getByText("Gesetzgebungsauftrag vorhanden"),
      ).toBeInTheDocument()
    })

    test("should display checked legislative mandate when it is true and has non-constitutional courtType", async () => {
      // Arrange
      courtTypeRef = ref("BAG")
      mockSessionStore({ hasLegislativeMandate: true })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.getByText("Berufsbild")).toBeInTheDocument()
      expect(screen.getByText("Gesetzgebungsauftrag")).toBeInTheDocument()
      expect(
        screen.getByText("Gesetzgebungsauftrag vorhanden"),
      ).toBeInTheDocument()
    })
  })

  describe("Dismissal Inputs", () => {
    test("should not display dismissal inputs/button when inputs are empty and courtType is non-labor", async () => {
      // Arrange
      courtTypeRef = ref("BGH")
      mockSessionStore({ dismissalGrounds: [], dismissalTypes: [] })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.queryByText("Kündigung")).not.toBeInTheDocument()
      expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
      expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    })

    test("should display dismissal button when inputs are empty and courtType is labor", async () => {
      // Arrange
      courtTypeRef = ref("BAG")
      mockSessionStore({ dismissalGrounds: [], dismissalTypes: [] })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.getByText("Kündigung")).toBeInTheDocument()
      expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
      expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    })

    test("should display dismissal inputs when ground is non-empty and courtType is non-labor", async () => {
      // Arrange
      courtTypeRef = ref("BGH")
      mockSessionStore({ dismissalGrounds: ["ground"], dismissalTypes: [] })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.queryByText("Kündigung")).not.toBeInTheDocument()
      expect(screen.getByText("Kündigungsarten")).toBeInTheDocument()
      expect(screen.getByText("Kündigungsgründe")).toBeInTheDocument()
    })

    test("should display dismissal inputs when ground is non-empty and courtType is non-labor", async () => {
      // Arrange
      courtTypeRef = ref("BGH")
      mockSessionStore({ dismissalGrounds: [], dismissalTypes: ["type"] })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.queryByText("Kündigung")).not.toBeInTheDocument()
      expect(screen.getByText("Kündigungsarten")).toBeInTheDocument()
      expect(screen.getByText("Kündigungsgründe")).toBeInTheDocument()
    })

    test("should display dismissal inputs when inputs are non-empty and courtType is labor", async () => {
      // Arrange
      courtTypeRef = ref("BAG")
      mockSessionStore({
        dismissalGrounds: ["ground"],
        dismissalTypes: ["type"],
      })

      // Act
      render(OtherCategories)

      // Assert
      expect(screen.queryByText("Kündigung")).not.toBeInTheDocument()
      expect(screen.getByText("Kündigungsarten")).toBeInTheDocument()
      expect(screen.getByText("Kündigungsgründe")).toBeInTheDocument()
    })
  })

  describe("CollectiveAgreements", () => {
    test("should not display collective agreements button when it is empty and not a labor court", async () => {
      // Arrange
      courtTypeRef = ref("BVerfG")
      mockSessionStore({ collectiveAgreements: [] })

      // Act
      render(OtherCategories)

      // Assert
      expect(
        screen.queryByRole("button", { name: "Tarifvertrag" }),
      ).not.toBeInTheDocument()
      expect(
        screen.queryByRole("textbox", { name: "Tarifvertrag Input" }),
      ).not.toBeInTheDocument()
    })

    test("should display collective agreements button when it is empty and labor court", async () => {
      // Arrange
      courtTypeRef = ref("LArbG")
      mockSessionStore({ collectiveAgreements: [] })

      // Act
      render(OtherCategories)

      // Assert
      expect(
        screen.getByRole("button", { name: "Tarifvertrag" }),
      ).toBeInTheDocument()
      expect(
        screen.queryByRole("textbox", { name: "Tarifvertrag Input" }),
      ).not.toBeInTheDocument()
    })

    test("should display collective agreements when it is not empty without labor court", async () => {
      // Arrange
      courtTypeRef = ref("BVerfG")
      mockSessionStore({ collectiveAgreements: ["Stehende Bühnen"] })

      // Act
      render(OtherCategories)

      // Assert
      expect(
        screen.getByRole("textbox", { name: "Tarifvertrag Input" }),
      ).toHaveValue("Stehende Bühnen")

      expect(
        screen.queryByRole("button", { name: "Tarifvertrag" }),
      ).not.toBeInTheDocument()
    })
  })
})
