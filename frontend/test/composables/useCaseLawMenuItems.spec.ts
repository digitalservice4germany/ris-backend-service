import { vi } from "vitest"
import { useCaseLawMenuItems } from "@/composables/useCaseLawMenuItems"

let isInternalUser = false
vi.mock("@/composables/useInternalUser", () => {
  return {
    useInternalUser: () => isInternalUser,
  }
})

describe("useCaseLawMenuItems", () => {
  it("adds document identifier as route parameter to each menu item", () => {
    isInternalUser = true
    const documentNumber = "fake-number"

    const menuItems = useCaseLawMenuItems(documentNumber, {})

    for (const menuItem of menuItems) {
      expect(menuItem.route.params).toMatchObject({
        documentNumber: "fake-number",
      })
    }
  })

  it("clones current route query to menu item", () => {
    isInternalUser = true
    const route = {
      query: { foo: "bar" },
    }

    const menuItems = useCaseLawMenuItems("", route.query)

    for (const menuItem of menuItems) {
      expect(menuItem.route.query).toEqual({ foo: "bar" })
    }
  })

  it("lists all expected menu items", () => {
    isInternalUser = true
    const menuItems = useCaseLawMenuItems("", {})

    const topLabelNames = menuItems.map((item) => item.label)
    expect(topLabelNames).toContain("Rubriken")
    expect(topLabelNames).toContain("Dokumente")
    expect(topLabelNames).toContain("Übergabe an jDV")
  })

  it("lists all expected menu items for external user", () => {
    isInternalUser = false
    const menuItems = useCaseLawMenuItems("", {})

    const topLabelNames = menuItems.map((item) => item.label)
    expect(topLabelNames).toContain("Rubriken")
    expect(topLabelNames).not.toContain("Dokumente")
    expect(topLabelNames).toContain("Übergabe an jDV")

    const categoriesSubMenu = menuItems.find(
      (menu) => menu.label === "Rubriken",
    )?.children
    expect(categoriesSubMenu?.map((menu) => menu.label)).toEqual([
      "Rechtszug",
      "Inhaltliche Erschließung",
      "Kurz- & Langtexte",
    ])
  })
})
