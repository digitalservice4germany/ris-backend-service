import { render, screen } from "@testing-library/vue"
import { previewLayoutInjectionKey } from "@/components/preview/constants"
import PreviewLongTexts from "@/components/preview/PreviewLongTexts.vue"
import { LongTexts } from "@/domain/documentUnit"
import ParticipatingJudge from "@/domain/participatingJudge"

function renderComponent(longTexts: LongTexts) {
  return render(PreviewLongTexts, {
    props: {
      longTexts: longTexts,
    },
    global: {
      provide: {
        [previewLayoutInjectionKey as symbol]: "wide",
      },
    },
  })
}
describe("preview long texts", () => {
  test("renders all long texts", async () => {
    renderComponent({
      tenor: "tenor",
      reasons: "reasons",
      caseFacts: "casefacts",
      decisionReasons: "decision reasons",
      dissentingOpinion: "dissenting opinion",
      participatingJudges: [
        new ParticipatingJudge({
          name: "Mustermann",
          referencedOpinions: "abweichende Meinung",
        }),
      ],
      otherLongText: "other long text",
      outline: "outline",
    })

    expect(await screen.findByText("Tenor")).toBeInTheDocument()
    expect(await screen.findByText("Gründe")).toBeInTheDocument()
    expect(await screen.findByText("Tatbestand")).toBeInTheDocument()
    expect(await screen.findByText("Entscheidungsgründe")).toBeInTheDocument()
    expect(await screen.findByText("Abweichende Meinung")).toBeInTheDocument()
    expect(await screen.findByText("Mitwirkende Richter")).toBeInTheDocument()
    expect(await screen.findByText("Sonstiger Langtext")).toBeInTheDocument()
    expect(await screen.findByText("Gliederung")).toBeInTheDocument()
  })

  it.each([
    [
      "Tenor",
      { tenor: "tenor" },
      [
        "Gründe",
        "Tatbestand",
        "Entscheidungsgründe",
        "Abweichende Meinung",
        "Mitwirkende Richter",
        "Sonstiger Langtext",
        "Gliederung",
      ],
    ],
    [
      "Gründe",
      { reasons: "reasons" },
      [
        "Tenor",
        "Tatbestand",
        "Entscheidungsgründe",
        "Abweichende Meinung",
        "Mitwirkende Richter",
        "Sonstiger Langtext",
        "Gliederung",
      ],
    ],
    [
      "Tatbestand",
      { caseFacts: "caseFacts" },
      [
        "Tenor",
        "Gründe",
        "Entscheidungsgründe",
        "Abweichende Meinung",
        "Mitwirkende Richter",
        "Sonstiger Langtext",
        "Gliederung",
      ],
    ],
    [
      "Entscheidungsgründe",
      { decisionReasons: "decisionReasons" },
      [
        "Tenor",
        "Gründe",
        "Tatbestand",
        "Abweichende Meinung",
        "Mitwirkende Richter",
        "Sonstiger Langtext",
        "Gliederung",
      ],
    ],
    [
      "Sonstiger Langtext",
      { otherLongText: "other long text" },
      [
        "Tenor",
        "Gründe",
        "Tatbestand",
        "Entscheidungsgründe",
        "Abweichende Meinung",
        "Mitwirkende Richter",
        "Gliederung",
      ],
    ],
    [
      "Gliederung",
      { outline: "outline" },
      [
        "Tenor",
        "Gründe",
        "Tatbestand",
        "Entscheidungsgründe",
        "Abweichende Meinung",
        "Mitwirkende Richter",
        "Sonstiger Langtext",
      ],
    ],
    [
      "Abweichende Meinung",
      { dissentingOpinion: "dissenting opinion" },
      [
        "Tenor",
        "Gründe",
        "Tatbestand",
        "Entscheidungsgründe",
        "Mitwirkende Richter",
        "Sonstiger Langtext",
        "Gliederung",
      ],
    ],
    [
      "Mitwirkende Richter",
      {
        participatingJudges: [
          new ParticipatingJudge({
            name: "Mustermann",
            referencedOpinions: "abweichende Meinung",
          }),
        ],
      },
      [
        "Tenor",
        "Gründe",
        "Tatbestand",
        "Entscheidungsgründe",
        "Abweichende Meinung",
        "Sonstiger Langtext",
        "Gliederung",
      ],
    ],
  ])(
    `renders %s and nothing else`,
    async (expected: string, texts: LongTexts, notExpected: string[]) => {
      renderComponent(texts)
      expect(await screen.findByText(expected)).toBeInTheDocument()
      notExpected.forEach((it) => {
        expect(screen.queryByText(it)).not.toBeInTheDocument()
      })
    },
  )
})
