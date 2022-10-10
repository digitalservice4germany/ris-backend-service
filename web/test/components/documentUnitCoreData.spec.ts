import { mount } from "@vue/test-utils"
import DocumentUnit from "../../src/domain/documentUnit"
import DocumentUnitCoreData from "@/components/DocumentUnitCoreData.vue"

// vitest run --testNamePattern CoreData
describe("Core Data", () => {
  test("renders correctly with given documentUnitId", async () => {
    const documentUnit = new DocumentUnit("1", {
      coreData: {
        fileNumber: "abc",
      },
      documentNumber: "ABCD2022000001",
    })
    const wrapper = mount(DocumentUnitCoreData, {
      props: {
        modelValue: documentUnit.coreData,
        updateStatus: 0,
      },
    })

    expect(
      (wrapper.find("#fileNumber").element as HTMLInputElement).value
    ).toBe("abc")
    const buttons = wrapper.findAll("button")
    expect(buttons[buttons.length - 1].text()).toBe("Speichern")
    expect(wrapper.text()).toContain("* Pflichtfelder zum Veröffentlichen")
    // what else? TODO
  })
})
