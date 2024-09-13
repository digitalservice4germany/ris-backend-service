import { createTestingPinia } from "@pinia/testing"
import { setActivePinia } from "pinia"
import { beforeEach, describe, expect } from "vitest"
import { useInternalUser } from "@/composables/useInternalUser"
import useSessionStore from "@/stores/sessionStore"

function mockUser(role: string) {
  const mockedSessionStore = useSessionStore()
  mockedSessionStore.user = {
    documentationOffice: undefined,
    email: undefined,
    name: "",
    roles: [role],
  }
  return mockedSessionStore
}
describe("useExternalUser", () => {
  beforeEach(() => {
    setActivePinia(createTestingPinia())
  })

  it("should return false if user has role external", async () => {
    mockUser("External")
    const result = useInternalUser()

    expect(result).equals(false)
  })

  it("should return true if user has role internal", async () => {
    mockUser("Internal")
    const result = useInternalUser()

    expect(result).equals(true)
  })
})
