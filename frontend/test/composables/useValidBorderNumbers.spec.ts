import { useValidBorderNumberLinks } from "@/composables/useValidBorderNumberLinks"

describe("useValidBorderNumbers", () => {
  it("add valid attribute true when border number is valid", () => {
    const value = useValidBorderNumberLinks(
      'lorem ipsum <border-number-link nr="1"></border-number-link>',
      ["1"],
    )

    expect(value).toEqual(
      'lorem ipsum <border-number-link nr="1" valid="true"></border-number-link>',
    )
  })

  it("add valid attribute false when border number is invalid", () => {
    const value = useValidBorderNumberLinks(
      'lorem ipsum <border-number-link nr="2"></border-number-link>',
      ["1"],
    )
    expect(value).toEqual(
      'lorem ipsum <border-number-link nr="2" valid="false"></border-number-link>',
    )
  })
})
