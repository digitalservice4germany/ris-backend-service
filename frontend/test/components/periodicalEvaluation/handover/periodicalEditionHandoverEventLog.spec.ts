import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import PeriodicalEditionHandoverEventLog from "@/components/periodical-evaluation/handover/PeriodicalEditionHandoverEventLog.vue"

import { EventRecordType, HandoverMail } from "@/domain/eventRecord"

function renderComponent(props = {}) {
  const user = userEvent.setup()

  return {
    user,
    ...render(PeriodicalEditionHandoverEventLog, {
      props: {
        ...props,
      },
    }),
  }
}

describe("Periodical Edition Handover Event Log", () => {
  test("renders earlier handover events", async () => {
    renderComponent({
      eventLog: [
        new HandoverMail({
          type: EventRecordType.HANDOVER,
          attachments: [
            {
              fileContent:
                '<?xml version="1.0"?>\n<!DOCTYPE juris-r SYSTEM "juris-r.dtd">\n<xml>content</xml>',
              fileName: "file.xml",
            },
          ],
          statusMessages: ["success"],
          success: true,
          receiverAddress: "receiver address",
          mailSubject: "mail subject",
          date: "01.02.2000",
        }),
      ],
    })

    expect(screen.getByLabelText("Letzte Ereignisse")).toHaveTextContent(
      `Letzte EreignisseXml Email Abgabe - 02.01.2000 um 00:00 UhrÜBERE-Mail an: receiver address Betreff: mail subjectALSfile.xml1<?xml version="1.0"?>2<!DOCTYPE juris-r SYSTEM "juris-r.dtd">3<xml>content</xml>`,
    )
  })

  it("renders event log error", async () => {
    renderComponent({
      eventLogError: {
        title: "Fehler beim Laden",
        description: "Event Log konnte nicht geladen werden",
      },
    })

    const modal = await screen.findByLabelText(
      "Fehler beim Laden des Event Logs",
    )
    expect(modal).toBeInTheDocument()
    expect(modal).toHaveTextContent("Fehler beim Laden")
  })

  it("shows message for first-time handover when no event log is available", async () => {
    renderComponent()

    expect(screen.queryByTestId("expandable-content")).not.toBeInTheDocument()
    expect(
      screen.getByText("Diese Ausgabe wurde bisher nicht an die jDV übergeben"),
    ).toBeInTheDocument()
  })
})
