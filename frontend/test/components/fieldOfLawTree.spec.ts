/* eslint-disable jest-dom/prefer-in-document */
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import FieldOfLawTreeVue from "@/components/FieldOfLawTree.vue"
import { FieldOfLawNode } from "@/domain/fieldOfLaw"
import FieldOfLawService from "@/services/fieldOfLawService"

function renderComponent(
  options: {
    modelValue?: FieldOfLawNode[]
  } = {},
) {
  return render(FieldOfLawTreeVue, {
    props: {
      modelValue: options.modelValue ?? [],
      clickedIdentifier: "",
      showNorms: false,
    },
  })
}

describe("FieldOfLawTree", () => {
  const user = userEvent.setup()

  const fetchSpy = vi
    .spyOn(FieldOfLawService, "getChildrenOf")
    .mockImplementation(() =>
      Promise.resolve({
        status: 200,
        data: [
          {
            identifier: "AB-01",
            text: "Text for AB",
            children: [],
            norms: [],
            isExpanded: false,
            hasChildren: false,
          },
          {
            identifier: "CD-02",
            text: "And text for CD with link to AB-01",
            children: [],
            norms: [],
            linkedFields: ["AB-01"],
            isExpanded: false,
            hasChildren: false,
          },
        ],
      }),
    )

  it("Tree is fully closed upon at start", async () => {
    renderComponent()
    expect(fetchSpy).toBeCalledTimes(1)
    expect(screen.getByText("Alle Sachgebiete")).toBeInTheDocument()
    expect(
      screen.getByLabelText("Alle Sachgebiete aufklappen"),
    ).toBeInTheDocument()
    expect(screen.queryByText("Text for AB")).not.toBeInTheDocument()
    expect(screen.queryByText("And text for CD")).not.toBeInTheDocument()
  })

  it("Tree opens top level nodes upon root click", async () => {
    renderComponent()

    await user.click(screen.getByLabelText("Alle Sachgebiete aufklappen"))

    expect(fetchSpy).toBeCalledTimes(3)
    expect(screen.getByText("Text for AB")).toBeInTheDocument()
    expect(screen.getByText("And text for CD with link to")).toBeInTheDocument()
    expect(screen.getByText("Alle Sachgebiete")).toBeInTheDocument()
  })

  it("Linked node gets displayed as link in stext", async () => {
    const { emitted } = renderComponent()
    await user.click(screen.getByLabelText("Alle Sachgebiete aufklappen"))
    const node1ids = screen.getAllByText("AB-01")
    const nonLinkText = screen.getByText("And text for CD with link to")

    expect(node1ids).toHaveLength(2)
    expect(node1ids[1] as HTMLElement).toHaveClass("linked-field")
    expect(nonLinkText as HTMLElement).not.toHaveClass("linked-field")
    await user.click(node1ids[1] as HTMLElement)
    expect(emitted()["linkedField:clicked"]).toBeTruthy()
  })
})
