import { render, screen } from "@testing-library/vue"
import CategoryWrapper from "@/components/CategoryWrapper.vue"

describe("category wrapper", () => {
  test("should display button instead of category component", async () => {
    // Act
    render(CategoryWrapper, {
      props: {
        label: "Test label",
        shouldShowButton: true,
      },
    })

    // Assert
    expect(screen.getByTestId("category-wrapper-button")).toBeInTheDocument()
    expect(
      screen.queryByTestId("category-wrapper-component"),
    ).not.toBeInTheDocument()
  })

  test("should display category component instead of button", async () => {
    // Act
    render(CategoryWrapper, {
      props: {
        label: "Test label",
        shouldShowButton: false,
      },
      slots: {
        default: "Just any category component",
      },
    })

    // Assert
    expect(
      screen.queryByTestId("category-wrapper-button"),
    ).not.toBeInTheDocument()
    expect(screen.getByTestId("category-wrapper-component")).toBeInTheDocument()
  })
})
