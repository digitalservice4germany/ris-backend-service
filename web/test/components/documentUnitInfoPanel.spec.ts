import { render } from "@testing-library/vue"
import DocumentUnitInfoPanel from "@/components/DocumentUnitInfoPanel.vue"
import DocumentUnit from "@/domain/documentUnit"

describe("documentUnit InfoPanel", () => {
  it("renders documentNumber if given", async () => {
    const { getAllByText } = render(DocumentUnitInfoPanel, {
      props: {
        documentUnit: new DocumentUnit("123", { documentnumber: "foo" }),
      },
    })

    getAllByText("foo")
  })

  it("renders aktenzeichen if given", async () => {
    const { getAllByText } = render(DocumentUnitInfoPanel, {
      props: {
        documentUnit: new DocumentUnit("123", {
          coreData: {
            fileNumber: "foo",
          },
        }),
      },
    })

    getAllByText((_content, node) => {
      return !!node?.textContent?.match(/Aktenzeichen foo/)
    })
  })

  it("renders placeholder for aktenzeichen if not given", async () => {
    const { getAllByText } = render(DocumentUnitInfoPanel, {
      props: {
        documentUnit: new DocumentUnit("123", {
          coreData: {
            decisionDate: "bar",
            courtType: "baz",
            fileNumber: undefined,
          },
          documentnumber: "qux",
        }),
      },
    })

    getAllByText((_content, node) => {
      return !!node?.textContent?.match(/Aktenzeichen -/)
    })
  })

  it("renders Entscheidungsdatum if given", async () => {
    const { getAllByText } = render(DocumentUnitInfoPanel, {
      props: {
        documentUnit: new DocumentUnit("123", {
          coreData: {
            decisionDate: "foo",
          },
        }),
      },
    })

    getAllByText((_content, node) => {
      return !!node?.textContent?.match(/Entscheidungsdatum foo/)
    })
  })

  it("renders placeholder for Entscheidungsdatum if not given", async () => {
    const { getAllByText } = render(DocumentUnitInfoPanel, {
      props: {
        documentUnit: new DocumentUnit("123", {
          coreData: {
            fileNumber: "foo",
            courtType: "baz",
          },
          documentnumber: "qux",
        }),
      },
    })

    getAllByText((_content, node) => {
      return !!node?.textContent?.match(/Entscheidungsdatum -/)
    })
  })

  it("renders Gerichtstyp if given", async () => {
    const { getAllByText } = render(DocumentUnitInfoPanel, {
      props: {
        documentUnit: new DocumentUnit("123", {
          coreData: {
            courtType: "foo",
          },
        }),
      },
    })
    getAllByText((_content, node) => {
      return !!node?.textContent?.match(/Gerichtstyp foo/)
    })
  })

  it("renders placeholder for Gerichtstyp if not given", async () => {
    const { getAllByText } = render(DocumentUnitInfoPanel, {
      props: {
        documentUnit: new DocumentUnit("123", {
          coreData: {
            fileNumber: "foo",
            decisionDate: "bar",
          },
          documentnumber: "qux",
        }),
      },
    })

    getAllByText((_content, node) => {
      return !!node?.textContent?.match(/Gerichtstyp -/)
    })
  })
})
