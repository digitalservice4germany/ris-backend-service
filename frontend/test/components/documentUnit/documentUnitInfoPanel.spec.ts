import { createTestingPinia } from "@pinia/testing"
import { render, screen } from "@testing-library/vue"
import { nextTick } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnitInfoPanel from "@/components/DocumentUnitInfoPanel.vue"
import DocumentUnit, {
  CoreData,
  DuplicateRelation,
  DuplicationRelationStatus,
} from "@/domain/documentUnit"
import FeatureToggleService from "@/services/featureToggleService"
import routes from "~/test-helper/routes"

function renderComponent(options?: {
  heading?: string
  coreData?: CoreData
  duplicateRelations?: DuplicateRelation[]
  isExternalUser?: boolean
}) {
  const router = createRouter({
    history: createWebHistory(),
    routes: routes,
  })
  return {
    ...render(DocumentUnitInfoPanel, {
      props: { heading: options?.heading ?? "" },
      global: {
        plugins: [
          router,
          createTestingPinia({
            initialState: {
              session: {
                user: {
                  roles: [options?.isExternalUser ? "External" : "Internal"],
                },
              },
              docunitStore: {
                documentUnit: new DocumentUnit("foo", {
                  documentNumber: "1234567891234",
                  coreData: options?.coreData ?? {
                    court: {
                      type: "AG",
                      location: "Test",
                      label: "AG Test",
                    },
                  },
                  managementData: {
                    borderNumbers: [],
                    duplicateRelations: options?.duplicateRelations ?? [],
                  },
                }),
              },
            },
          }),
        ],
      },
    }),
  }
}

describe("documentUnit InfoPanel", () => {
  beforeAll(() => {
    vi.spyOn(FeatureToggleService, "isEnabled").mockResolvedValue({
      status: 200,
      data: true,
    })
  })

  it("renders heading if given", async () => {
    renderComponent({ heading: "test heading" })

    screen.getAllByText("test heading")
  })

  it("renders all given property infos in correct order", async () => {
    const coreData = {
      decisionDate: "2024-01-01",
      fileNumbers: ["AZ123"],
      court: {
        type: "AG",
        location: "Test",
        label: "AG Test",
      },
    }
    renderComponent({ coreData: coreData })

    expect(
      await screen.findByText("AG Test, AZ123, 01.01.2024"),
    ).toBeInTheDocument()
  })

  it("omits incomplete coredata fields from rendering", async () => {
    renderComponent()

    expect(await screen.findByText("AG Test")).toBeInTheDocument()
  })

  it("renders a duplicate warning with link if there are pending duplicates", async () => {
    renderComponent({
      duplicateRelations: [
        {
          status: DuplicationRelationStatus.PENDING,
          documentNumber: "doc",
          isJdvDuplicateCheckActive: true,
        },
      ],
    })

    // Wait for feature flag
    await nextTick()

    expect(await screen.findByText("Dublettenverdacht")).toBeInTheDocument()
    expect(screen.getByRole("link")).toHaveTextContent("Bitte prüfen")
  })

  it("renders a duplicate warning without link for external user", async () => {
    renderComponent({
      isExternalUser: true,
      duplicateRelations: [
        {
          status: DuplicationRelationStatus.PENDING,
          documentNumber: "doc",
          isJdvDuplicateCheckActive: true,
        },
      ],
    })

    // Wait for feature flag
    await nextTick()

    expect(await screen.findByText("Dublettenverdacht")).toBeInTheDocument()
    expect(screen.queryByRole("link")).not.toBeInTheDocument()
  })

  it("renders no duplicate warning if there are ignored duplicates", async () => {
    renderComponent({
      duplicateRelations: [
        {
          status: DuplicationRelationStatus.IGNORED,
          documentNumber: "doc",
          isJdvDuplicateCheckActive: true,
        },
      ],
    })

    // Wait for feature flag
    await nextTick()

    expect(screen.queryByText("Dublettenverdacht")).not.toBeInTheDocument()
  })
})
