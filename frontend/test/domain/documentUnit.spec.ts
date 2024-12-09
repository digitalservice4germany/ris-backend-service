import DocumentUnit, {
  CoreData,
  LongTexts,
  ShortTexts,
} from "../../src/domain/documentUnit"

describe("DocumentUnit", () => {
  it("instantiates with uuid", () => {
    const documentUnit = new DocumentUnit("foo")
    expect(documentUnit.uuid).toEqual("foo")
    expect(documentUnit.id).toEqual(undefined)
  })

  it("returns core Data as object", () => {
    const documentUnit = new DocumentUnit("foo")
    documentUnit.coreData.fileNumbers = ["bar"]
    documentUnit.coreData.court = {
      type: "baz",
      location: "baz",
      label: "baz",
    }

    const coreData: CoreData = documentUnit.coreData
    expect(coreData.fileNumbers).toStrictEqual(["bar"])
    expect(coreData.court?.location).toBe("baz")
  })

  it("returns short texts as object", () => {
    const documentUnit = new DocumentUnit("foo")
    documentUnit.shortTexts.headnote = "baz"

    const documentUnitTexts: ShortTexts = documentUnit.shortTexts
    expect(documentUnitTexts.headnote).toBe("baz")
  })

  it("returns long texts as object", () => {
    const documentUnit = new DocumentUnit("foo")
    documentUnit.longTexts.reasons = "bar"

    const documentUnitTexts: LongTexts = documentUnit.longTexts
    expect(documentUnitTexts.reasons).toBe("bar")
  })

  it("returns false if no file is attached", () => {
    const documentUnit = new DocumentUnit("foo")
    expect(documentUnit.hasAttachments).toBeFalsy()

    const documentUnit2 = new DocumentUnit("foo", { attachments: [] })
    expect(documentUnit2.hasAttachments).toBeFalsy()
  })

  it("returns true if file is attached", () => {
    const documentUnit = new DocumentUnit("foo", {
      attachments: [{ s3path: "foo-path" }],
    })
    expect(documentUnit.hasAttachments).toBeTruthy()
  })

  it("returns all missing required fields", () => {
    const documentUnit = new DocumentUnit("foo", {
      attachments: [{ s3path: "foo-path" }],
    })
    expect(documentUnit.missingRequiredFields).toStrictEqual([
      "fileNumbers",
      "court",
      "decisionDate",
      "legalEffect",
      "documentType",
    ])
  })

  it("identify missing fields correctly if one field is set", () => {
    const documentUnit = new DocumentUnit("foo", {
      attachments: [{ s3path: "foo-path" }],
      coreData: { legalEffect: "foo" },
    })
    expect(documentUnit.missingRequiredFields).toStrictEqual([
      "fileNumbers",
      "court",
      "decisionDate",
      "documentType",
    ])
  })

  it("indentify empty fileNumbers as missing", () => {
    const documentUnit = new DocumentUnit("foo", {
      attachments: [{ s3path: "foo-path" }],
      coreData: { fileNumbers: [] },
    })
    expect(documentUnit.missingRequiredFields).toStrictEqual([
      "fileNumbers",
      "court",
      "decisionDate",
      "legalEffect",
      "documentType",
    ])
  })

  it("indentify empty decisionDate as missing", () => {
    const documentUnit = new DocumentUnit("foo", {
      attachments: [{ s3path: "foo-path" }],
      coreData: { decisionDate: "" },
    })
    expect(documentUnit.missingRequiredFields).toStrictEqual([
      "fileNumbers",
      "court",
      "decisionDate",
      "legalEffect",
      "documentType",
    ])
  })

  it("indentify all missing required fields", () => {
    const documentUnit = new DocumentUnit("foo", {
      attachments: [{ s3path: "foo-path" }],
      coreData: { legalEffect: "foo" },
    })
    expect(documentUnit.missingRequiredFields).toStrictEqual([
      "fileNumbers",
      "court",
      "decisionDate",
      "documentType",
    ])
  })

  it("returns true when field is required", () => {
    expect(DocumentUnit.isRequiredField("fileNumbers")).toBeTruthy()
  })

  it("returns true when field is required", () => {
    expect(DocumentUnit.isRequiredField("ECLI")).toBeFalsy()
  })
})
