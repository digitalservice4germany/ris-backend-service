import { getFavicon } from "@/utils/getFavicon"

describe("getFavicon", () => {
  it.each(["staging", "uat", "production"])(
    "returns correct favicon for %s",
    (environment) => {
      vi.stubEnv("MODE", "production")
      const favicon = getFavicon(environment)

      expect(favicon).toBe(`/src/assets/favicon-${environment}.svg`)
      vi.unstubAllEnvs()
    },
  )

  it("falls back to production favicon", () => {
    const favicon = getFavicon("foo")

    expect(favicon).toBe(`/src/assets/favicon-production.svg`)
  })

  it("returns correct favicon for local", () => {
    vi.stubEnv("MODE", "development")

    const favicon = getFavicon("staging")

    expect(favicon).toBe(`/src/assets/favicon-local.svg`)
    vi.unstubAllEnvs()
  })
})
