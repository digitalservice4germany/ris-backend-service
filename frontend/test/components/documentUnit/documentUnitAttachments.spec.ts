import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnitAttachments from "@/components/DocumentUnitAttachments.vue"
import Attachment from "@/domain/attachment"
import DocumentUnit from "@/domain/documentUnit"
import routes from "~/test-helper/routes"

function renderComponent(attachments?: Attachment[]) {
  const user = userEvent.setup()

  const router = createRouter({
    history: createWebHistory(),
    routes: routes,
  })
  return {
    user,
    ...render(DocumentUnitAttachments, {
      global: {
        plugins: [
          createTestingPinia({
            initialState: {
              docunitStore: {
                documentUnit: new DocumentUnit("foo", {
                  documentNumber: "1234567891234",
                  attachments: attachments ?? [],
                }),
              },
            },
          }),
          [router],
        ],
      },
    }),
  }
}

describe("Document Unit Categories", () => {
  test("renders without attachments", async () => {
    renderComponent()

    expect(screen.getByTestId("title")).toBeVisible()
    expect(screen.queryByTestId("attachment-list")).not.toBeInTheDocument()
    expect(
      screen.queryByTestId("attachment-view-side-panel"),
    ).not.toBeInTheDocument()
  })

  test("renders file list if has attachments", async () => {
    const name = "this-is-a-file-name.docx"
    const format = "docx"
    const attachment: Attachment = {
      name: name,
      format: format,
      s3path: "./path.docx",
      uploadTimestamp: "11.04.2024",
    }
    renderComponent([attachment])

    expect(screen.getByTestId("title")).toBeVisible()
    expect(screen.queryByTestId("attachment-list")).toBeVisible()
    expect(screen.queryByText(name)).toBeVisible()
    expect(screen.queryByText(format)).toBeVisible()
    expect(screen.getByTestId("uploaded-at-cell")).toBeInTheDocument()
  })
})
