import { createTestingPinia } from "@pinia/testing"
import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { setActivePinia } from "pinia"
import { beforeEach } from "vitest"
import DocumentUnitCoreData from "@/components/DocumentUnitCoreData.vue"
import DocumentUnit, { CoreData } from "@/domain/documentUnit"

type CoreDataProps = InstanceType<typeof DocumentUnitCoreData>["$props"]

function renderComponent(props?: Partial<CoreDataProps>) {
  const user = userEvent.setup()

  let modelValue: CoreData | undefined = props?.modelValue ?? {}

  const effectiveProps: CoreDataProps = {
    modelValue,
    "onUpdate:modelValue":
      props?.["onUpdate:modelValue"] ??
      ((val: CoreData | undefined) => (modelValue = val)),
  }

  return { user, ...render(DocumentUnitCoreData, { props: effectiveProps }) }
}

describe("Core Data", () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia())
  })
  test("renders correctly with given documentUnitId", async () => {
    const documentUnit = new DocumentUnit("1", {
      coreData: {
        fileNumbers: ["one", "two"],
        ecli: "abc123",
      },
      documentNumber: "ABCD2022000001",
    })

    renderComponent({ modelValue: documentUnit.coreData })

    const chipList = screen.getAllByRole("listitem")
    expect(chipList.length).toBe(2)
    expect(chipList[0]).toHaveTextContent("one")
    expect(chipList[1]).toHaveTextContent("two")

    expect(screen.getByLabelText("ECLI")).toHaveValue("abc123")
  })

  test("renders deviating decision date", async () => {
    const documentUnit = new DocumentUnit("1", {
      coreData: {
        deviatingDecisionDates: ["2021-02-01", "2022-02-01"],
      },
      documentNumber: "ABCD2022000001",
    })

    renderComponent({ modelValue: documentUnit.coreData })
    await screen.findByLabelText("Abweichendes Entscheidungsdatum anzeigen")
    screen.getByLabelText("Abweichendes Entscheidungsdatum anzeigen").click()
    expect(await screen.findByTestId("deviating-decision-dates")).toBeVisible()

    const chipList = screen.getAllByRole("listitem")
    expect(chipList.length).toBe(2)
    expect(chipList[0]).toHaveTextContent("01.02.2021")
    expect(chipList[1]).toHaveTextContent("01.02.2022")
  })

  test("updates deviating decision date", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      "onUpdate:modelValue": onUpdate,
    })

    await screen.findByLabelText("Abweichendes Entscheidungsdatum anzeigen")
    screen.getByLabelText("Abweichendes Entscheidungsdatum anzeigen").click()
    await user.type(
      screen.getByLabelText("Abweichendes Entscheidungsdatum"),
      "02.02.2022{enter}",
    )
    expect(onUpdate).toHaveBeenCalledWith({
      deviatingDecisionDates: ["2022-02-02"],
    })
  })

  test("renders year of dispute", async () => {
    const documentUnit = new DocumentUnit("1", {
      coreData: {
        yearsOfDispute: ["2021", "2022"],
      },
      documentNumber: "ABCD2022000001",
    })

    renderComponent({ modelValue: documentUnit.coreData })
    await screen.findByText("Streitjahr")
    expect(screen.getByTestId("year-of-dispute")).toBeVisible()

    const chipList = screen.getAllByRole("listitem")
    expect(chipList.length).toBe(2)
    expect(chipList[0]).toHaveTextContent("2021")
    expect(chipList[1]).toHaveTextContent("2022")
  })

  test("updates year of dispute", async () => {
    const onUpdate = vi.fn()
    const { user } = renderComponent({
      "onUpdate:modelValue": onUpdate,
    })

    await screen.findByText("Streitjahr")
    await user.type(screen.getByLabelText("Streitjahr"), "2023{enter}")
    expect(onUpdate).toHaveBeenCalledWith({ yearsOfDispute: ["2023"] })
  })
})
