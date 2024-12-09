import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import ListInputDisplay from "@/components/input/listInput/ListInputDisplay.vue"

function renderComponent(
  options: {
    props?: {
      label: string
      modelValue: string[]
    }
  } = {
    props: {
      label: "Liste",
      modelValue: [],
    },
  },
) {
  const user = userEvent.setup()

  return {
    user,
    ...render(ListInputDisplay, {
      props: options.props,
    }),
  }
}

describe("List input display mode", () => {
  test("renders modelValue as chips", async () => {
    renderComponent({
      props: {
        label: "Liste",
        modelValue: ["one", "two", "three"],
      },
    })

    expect(screen.getAllByTestId("chip")).toHaveLength(3)

    // Assert that the correct values are rendered in each chip
    const chips = screen.getAllByTestId("chip").map((chip) => chip.textContent)
    expect(chips[0]).toBe("one")
    expect(chips[1]).toBe("two")
    expect(chips[2]).toBe("three")
  })

  test("click on edit button emits toggle event", async () => {
    const { emitted, user } = renderComponent({
      props: {
        label: "Liste",
        modelValue: ["one", "two"],
      },
    })

    // Click the button
    const button = screen.getByRole("button", { name: /Liste bearbeiten/i })
    await user.click(button)

    // Assert that the toggle event is emitted
    expect(emitted().toggle).toBeTruthy()
    expect(emitted().toggle).toHaveLength(1)
  })
})
