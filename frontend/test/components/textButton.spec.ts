import { mount } from "@vue/test-utils"
import TextButton from "@/components/TextButton.vue"

describe("TextButton", () => {
  it("renders with label prop", () => {
    const wrapper = mount(TextButton, {
      props: {
        label: "foo",
      },
    })
    expect(wrapper.props().label).toBe("foo")
  })

  it("renders with default label prop", () => {
    const wrapper = mount(TextButton)
    expect(wrapper.props().label).toBe("Speichern")
  })
})
