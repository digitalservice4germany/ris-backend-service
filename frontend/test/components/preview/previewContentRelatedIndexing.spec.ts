import { createTestingPinia } from "@pinia/testing"
import { render, screen } from "@testing-library/vue"
import { previewLayoutInjectionKey } from "@/components/preview/constants"
import PreviewContentRelatedIndexing from "@/components/preview/PreviewContentRelatedIndexing.vue"
import ActiveCitation from "@/domain/activeCitation"
import DocumentUnit, { ContentRelatedIndexing } from "@/domain/documentUnit"
import NormReference from "@/domain/normReference"
import SingleNorm from "@/domain/singleNorm"

function renderComponent(contentRelatedIndexing?: ContentRelatedIndexing) {
  return {
    ...render(PreviewContentRelatedIndexing, {
      global: {
        plugins: [
          [
            createTestingPinia({
              initialState: {
                docunitStore: {
                  documentUnit: new DocumentUnit("123", {
                    documentNumber: "foo",
                    contentRelatedIndexing: contentRelatedIndexing ?? {},
                  }),
                },
              },
            }),
          ],
        ],
        provide: {
          [previewLayoutInjectionKey as symbol]: "wide",
        },
      },
    }),
  }
}

describe("preview content related indexing", () => {
  test("renders all content related indexing", async () => {
    renderComponent({
      keywords: ["keyword"],
      norms: [
        new NormReference({
          normAbbreviation: { abbreviation: "ABC" },
        }),
      ],
      activeCitations: [
        new ActiveCitation({
          court: {
            type: "type1",
            location: "location1",
            label: "label1",
          },
          documentType: {
            jurisShortcut: "documentTypeShortcut1",
            label: "documentType1",
          },
          fileNumber: "test fileNumber1",
        }),
      ],
      fieldsOfLaw: [
        {
          identifier: "AB-01",
          text: "Text for AB",
          children: [],
          hasChildren: false,
          norms: [],
        },
      ],
      jobProfiles: ["Handwerker", "Elektriker"],
      dismissalGrounds: ["Betriebsbedingte Kündigung"],
      dismissalTypes: ["Einführung neuer Technologien"],
      collectiveAgreements: ["Normalvertrag Chor", "Stehende Bühnen"],
      hasLegislativeMandate: true,
    })

    expect(await screen.findByText("Schlagwörter")).toBeInTheDocument()
    expect(await screen.findByText("Normen")).toBeInTheDocument()
    expect(await screen.findByText("Aktivzitierung")).toBeInTheDocument()
    expect(await screen.findByText("Sachgebiete")).toBeInTheDocument()
    expect(await screen.findByText("Kündigungsgründe")).toBeInTheDocument()
    expect(await screen.findByText("Kündigungsarten")).toBeInTheDocument()
    expect(await screen.findByText("Berufsbild")).toBeInTheDocument()
    expect(await screen.findByText("Gesetzgebungsauftrag")).toBeInTheDocument()
    expect(await screen.findByText("Tarifvertrag")).toBeInTheDocument()
  })

  test("renders multiple keywords and nothing else", async () => {
    renderComponent({
      keywords: ["foo", "bar"],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
      dismissalGrounds: [],
      dismissalTypes: [],
      jobProfiles: [],
      hasLegislativeMandate: false,
      collectiveAgreements: [],
    })

    expect(await screen.findByText("Schlagwörter")).toBeInTheDocument()
    expect(await screen.findByText("foo")).toBeInTheDocument()
    expect(await screen.findByText("bar")).toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
  })

  test("renders multiple norms and single norms and nothing else", async () => {
    renderComponent({
      keywords: [],
      norms: [
        new NormReference({
          normAbbreviation: { abbreviation: "ABC" },
        }),
        new NormReference({
          normAbbreviation: { abbreviation: "DEF" },
          singleNorms: [
            new SingleNorm({
              singleNorm: "§1",
            }),
            new SingleNorm({
              singleNorm: "§2",
            }),
          ],
        }),
      ],
      activeCitations: [],
      fieldsOfLaw: [],
      jobProfiles: [],
      dismissalGrounds: [],
      dismissalTypes: [],
      hasLegislativeMandate: false,
      collectiveAgreements: [],
    })

    expect(await screen.findByText("Normen")).toBeInTheDocument()
    expect(await screen.findByText("ABC")).toBeInTheDocument()
    expect(await screen.findByText("DEF - §1")).toBeInTheDocument()
    expect(await screen.findByText("DEF - §2")).toBeInTheDocument()
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
  })

  test("renders multiple active citations and nothing else", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [
        new ActiveCitation({
          court: {
            type: "AG",
            location: "Aachen",
            label: "AG Aachen",
          },
          documentType: {
            jurisShortcut: "Bes",
            label: "Beschluss",
          },
          fileNumber: "ABC.123",
        }),
        new ActiveCitation({
          court: {
            type: "BVerfG",
            label: "BVerfG Karlsruhe",
          },
          documentType: {
            jurisShortcut: "Urt",
            label: "Urteil",
          },
          fileNumber: "DEF.456",
        }),
      ],
      fieldsOfLaw: [],
      jobProfiles: [],
      dismissalGrounds: [],
      dismissalTypes: [],
      hasLegislativeMandate: false,
      collectiveAgreements: [],
    })

    expect(await screen.findByText("Aktivzitierung")).toBeInTheDocument()
    expect(
      await screen.findByText("AG Aachen, ABC.123, Beschluss"),
    ).toBeInTheDocument()
    expect(
      await screen.findByText("BVerfG Karlsruhe, DEF.456, Urteil"),
    ).toBeInTheDocument()
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
  })

  test("renders multiple fields of law and nothing else", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [
        {
          identifier: "AB-01-01",
          text: "Text for AB-01-01",
          children: [],
          hasChildren: false,
          norms: [],
          parent: {
            identifier: "AB-01",
            text: "Text for AB-01",
            children: [],
            hasChildren: false,
            norms: [],
            parent: {
              identifier: "AB",
              text: "Text for AB",
              children: [],
              hasChildren: false,
              norms: [],
            },
          },
        },
        {
          identifier: "CD-01-05",
          text: "Text for CD-01-05",
          children: [],
          hasChildren: false,
          norms: [],
          parent: {
            identifier: "CD-01",
            text: "Text for CD-01",
            children: [],
            hasChildren: false,
            norms: [],
            parent: {
              identifier: "CD",
              text: "Text for CD",
              children: [],
              hasChildren: false,
              norms: [],
            },
          },
        },
      ],
      jobProfiles: [],
      dismissalGrounds: [],
      dismissalTypes: [],
      hasLegislativeMandate: false,
      collectiveAgreements: [],
    })

    expect(await screen.findByText("Sachgebiete")).toBeInTheDocument()

    expect(await screen.findByText("Text for AB-01-01")).toBeInTheDocument()
    expect(await screen.findByText("Text for AB-01")).toBeInTheDocument()
    expect(await screen.findByText("Text for AB")).toBeInTheDocument()

    expect(await screen.findByText("Text for CD-01-05")).toBeInTheDocument()
    expect(await screen.findByText("Text for CD-01")).toBeInTheDocument()
    expect(await screen.findByText("Text for CD")).toBeInTheDocument()

    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
  })

  test("renders multiple job profiles and nothing else", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
      jobProfiles: ["foo", "bar"],
      dismissalGrounds: [],
      collectiveAgreements: [],
      dismissalTypes: [],
      hasLegislativeMandate: false,
    })

    expect(await screen.findByText("Berufsbild")).toBeInTheDocument()
    expect(await screen.findByText("foo")).toBeInTheDocument()
    expect(await screen.findByText("bar")).toBeInTheDocument()
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
  })

  test("renders multiple collective agreements and nothing else", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
      jobProfiles: [],
      collectiveAgreements: ["Normalvertrag Chor", "Stehende Bühnen"],
      dismissalGrounds: [],
      dismissalTypes: [],
    })

    expect(await screen.findByText("Tarifvertrag")).toBeInTheDocument()
    expect(await screen.findByText("Normalvertrag Chor")).toBeInTheDocument()
    expect(await screen.findByText("Stehende Bühnen")).toBeInTheDocument()
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
  })

  test("renders legislative mandate and nothing else", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
      jobProfiles: [],
      dismissalGrounds: [],
      dismissalTypes: [],
      collectiveAgreements: [],
      hasLegislativeMandate: true,
    })

    expect(await screen.findByText("Gesetzgebungsauftrag")).toBeInTheDocument()
    expect(await screen.findByText("Ja")).toBeInTheDocument()
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
  })

  test("renders dismissal inputs and nothing else", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
      jobProfiles: [],
      dismissalGrounds: ["ground"],
      dismissalTypes: ["type"],
      collectiveAgreements: [],
      hasLegislativeMandate: false,
    })

    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
    expect(await screen.findByText("Kündigungsgründe")).toBeInTheDocument()
    expect(await screen.findByText("Kündigungsarten")).toBeInTheDocument()
  })

  test("renders no legislative mandate when it is false", async () => {
    renderComponent({
      hasLegislativeMandate: false,
    })

    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
  })

  test("renders nothing when elements are empty", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
      jobProfiles: [],
      dismissalGrounds: [],
      dismissalTypes: [],
      collectiveAgreements: [],
      hasLegislativeMandate: undefined,
    })
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
  })

  test("renders nothing when elements are undefined", async () => {
    renderComponent({
      keywords: undefined,
      norms: undefined,
      activeCitations: undefined,
      fieldsOfLaw: undefined,
      jobProfiles: undefined,
      hasLegislativeMandate: undefined,
      dismissalGrounds: undefined,
      dismissalTypes: undefined,
      collectiveAgreements: undefined,
    })
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
    expect(screen.queryByText("Gesetzgebungsauftrag")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsgründe")).not.toBeInTheDocument()
    expect(screen.queryByText("Kündigungsarten")).not.toBeInTheDocument()
    expect(screen.queryByText("Tarifvertrag")).not.toBeInTheDocument()
  })
})
