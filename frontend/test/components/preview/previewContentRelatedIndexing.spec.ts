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
    })

    expect(await screen.findByText("Schlagwörter")).toBeInTheDocument()
    expect(await screen.findByText("Normen")).toBeInTheDocument()
    expect(await screen.findByText("Aktivzitierung")).toBeInTheDocument()
    expect(await screen.findByText("Sachgebiete")).toBeInTheDocument()
    expect(await screen.findByText("Berufsbild")).toBeInTheDocument()
  })

  test("renders multiple keywords and nothing else", async () => {
    renderComponent({
      keywords: ["foo", "bar"],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
    })

    expect(await screen.findByText("Schlagwörter")).toBeInTheDocument()
    expect(await screen.findByText("foo")).toBeInTheDocument()
    expect(await screen.findByText("bar")).toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
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
    })

    expect(await screen.findByText("Normen")).toBeInTheDocument()
    expect(await screen.findByText("ABC")).toBeInTheDocument()
    expect(await screen.findByText("DEF - §1")).toBeInTheDocument()
    expect(await screen.findByText("DEF - §2")).toBeInTheDocument()
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
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
  })

  test("renders multiple job profiles and nothing else", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
      jobProfiles: ["foo", "bar"],
    })

    expect(await screen.findByText("Berufsbild")).toBeInTheDocument()
    expect(await screen.findByText("foo")).toBeInTheDocument()
    expect(await screen.findByText("bar")).toBeInTheDocument()
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
  })

  test("renders nothing when elements are empty", async () => {
    renderComponent({
      keywords: [],
      norms: [],
      activeCitations: [],
      fieldsOfLaw: [],
      jobProfiles: [],
    })
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
  })

  test("renders nothing when elements are undefined", async () => {
    renderComponent({
      keywords: undefined,
      norms: undefined,
      activeCitations: undefined,
      fieldsOfLaw: undefined,
      jobProfiles: undefined,
    })
    expect(screen.queryByText("Schlagwörter")).not.toBeInTheDocument()
    expect(screen.queryByText("Normen")).not.toBeInTheDocument()
    expect(screen.queryByText("Aktivzitierung")).not.toBeInTheDocument()
    expect(screen.queryByText("Sachgebiete")).not.toBeInTheDocument()
    expect(screen.queryByText("Berufsbild")).not.toBeInTheDocument()
  })
})
