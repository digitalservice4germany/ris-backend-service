import httpClient from "@/services/httpClient"
import {
  editNormFrame,
  getAllNorms,
  getNormByGuid,
} from "@/services/normsService"

vi.mock("@/services/httpClient")

describe("normsService", () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  describe("list norms", () => {
    it("queries the backend with the correct parameters", async () => {
      const httpClientGet = vi
        .mocked(httpClient)
        .get.mockResolvedValueOnce({ status: 200, data: {} })

      await getAllNorms()

      expect(httpClientGet).toHaveBeenCalledOnce()
      expect(httpClientGet).toHaveBeenLastCalledWith("norms")
    })

    it("returns data entry of body if server connection was successful", async () => {
      vi.mocked(httpClient).get.mockResolvedValueOnce({
        status: 200,
        data: { data: ["fake-norm"] },
      })

      const response = await getAllNorms()

      expect(response.data).toEqual(["fake-norm"])
    })

    it("responds with correct error message if server response status is above 300", async () => {
      vi.mocked(httpClient).get.mockResolvedValueOnce({
        status: 300,
        data: {},
      })

      const response = await getAllNorms()

      expect(response.error?.title).toBe(
        "Dokumentationseinheiten konnten nicht geladen werden."
      )
    })

    it("responds with correct error message if connection failed", async () => {
      vi.mocked(httpClient).get.mockResolvedValueOnce({
        status: 500,
        error: { title: "error" },
      })

      const response = await getAllNorms()

      expect(response.error?.title).toBe(
        "Dokumentationseinheiten konnten nicht geladen werden."
      )
    })
  })

  describe("load norm", () => {
    it("queries the backend with the correct parameters", async () => {
      const httpClientGet = vi
        .mocked(httpClient)
        .get.mockResolvedValueOnce({ status: 200, data: "" })

      await getNormByGuid("fake-guid")

      expect(httpClientGet).toHaveBeenCalledOnce()
      expect(httpClientGet).toHaveBeenLastCalledWith("norms/fake-guid")
    })

    it("returns response body if server connection was successful", async () => {
      vi.mocked(httpClient).get.mockResolvedValue({
        status: 200,
        data: "fake-norm",
      })

      const response = await getNormByGuid("fake-guid")

      expect(response.data).toBe("fake-norm")
    })

    it("responds with correct error message if server response status is above 300", async () => {
      vi.mocked(httpClient).get.mockResolvedValueOnce({
        status: 300,
        data: "",
      })

      const response = await getNormByGuid("fake-guid")

      expect(response.error?.title).toBe(
        "Dokumentationseinheit konnte nicht geladen werden."
      )
    })

    it("responds with correct error message if connection failed", async () => {
      vi.mocked(httpClient).get.mockResolvedValueOnce({
        status: 500,
        error: { title: "error" },
      })

      const response = await getNormByGuid("fake-guid")

      expect(response.error?.title).toBe(
        "Dokumentationseinheit konnte nicht geladen werden."
      )
    })
  })

  describe("edit norm frame", () => {
    it("sends command to the backend with the correct parameters", async () => {
      const httpClientPut = vi
        .mocked(httpClient)
        .put.mockResolvedValueOnce({ status: 204, data: "" })

      await editNormFrame("fake-guid", {
        officialLongTitle: "new title",
        officialShortTitle: "",
        officialAbbreviation: undefined,
        referenceNumber: "",
        publicationDate: "2022-11-14T23:00:00.000Z",
        announcementDate: "",
        citationDate: undefined,
        frameKeywords: "",
        providerEntity: "new provider entity",
        providerDecidingBody: undefined,
        providerIsResolutionMajority: undefined,
        leadJurisdiction: undefined,
        leadUnit: undefined,
        participationType: undefined,
        participationInstitution: undefined,
      })

      expect(httpClientPut).toHaveBeenCalledOnce()
      expect(httpClientPut).toHaveBeenLastCalledWith(
        "norms/fake-guid",
        {
          headers: {
            Accept: "application/json",
            "Content-Type": "application/json",
          },
        },
        {
          announcementDate: null,
          documentTemplateName: null,
          citationDate: null,
          frameKeywords: null,
          officialAbbreviation: null,
          officialLongTitle: "new title",
          participationType: null,
          providerDecidingBody: null,
          providerEntity: "new provider entity",
          providerIsResolutionMajority: null,
          publicationDate: "2022-11-15",
          referenceNumber: null,
          officialShortTitle: null,
          isExpirationDateTemp: null,
          leadJurisdiction: null,
          risAbbreviation: null,
          leadUnit: null,
          participationInstitution: null,
          subjectBgb3: null,
          ageIndicationEnd: null,
          ageIndicationStart: null,
          ageOfMajorityIndication: null,
          applicationScopeArea: null,
          applicationScopeEndDate: null,
          applicationScopeStartDate: null,
          categorizedReference: null,
          celexNumber: null,
          completeCitation: null,
          definition: null,
          digitalAnnouncementDate: null,
          digitalAnnouncementArea: null,
          digitalAnnouncementAreaNumber: null,
          digitalAnnouncementEdition: null,
          digitalAnnouncementExplanations: null,
          digitalAnnouncementInfo: null,
          digitalAnnouncementMedium: null,
          digitalAnnouncementPage: null,
          digitalAnnouncementYear: null,
          digitalEvidenceAppendix: null,
          digitalEvidenceExternalDataNote: null,
          digitalEvidenceLink: null,
          digitalEvidenceRelatedData: null,
          divergentDocumentNumber: null,
          divergentEntryIntoForceDate: null,
          divergentEntryIntoForceDateState: null,
          divergentExpirationDate: null,
          divergentExpirationDateState: null,
          documentCategory: null,
          documentNormCategory: null,
          documentNumber: null,
          documentStatusDate: null,
          documentStatusDescription: null,
          documentStatusEntryIntoForceDate: null,
          documentStatusProof: null,
          documentStatusReference: null,
          documentStatusWorkNote: null,
          documentTextProof: null,
          documentTypeName: null,
          entryIntoForceDate: null,
          entryIntoForceDateState: null,
          euAnnouncementExplanations: null,
          euAnnouncementGazette: null,
          euAnnouncementInfo: null,
          euAnnouncementNumber: null,
          euAnnouncementPage: null,
          euAnnouncementSeries: null,
          euAnnouncementYear: null,
          europeanLegalIdentifier: null,
          expirationDate: null,
          expirationDateState: null,
          expirationNormCategory: null,
          otherDocumentNote: null,
          otherFootnote: null,
          otherOfficialAnnouncement: null,
          otherStatusNote: null,
          principleEntryIntoForceDate: null,
          principleEntryIntoForceDateState: null,
          principleExpirationDate: null,
          principleExpirationDateState: null,
          printAnnouncementExplanations: null,
          printAnnouncementGazette: null,
          printAnnouncementInfo: null,
          printAnnouncementNumber: null,
          printAnnouncementPage: null,
          printAnnouncementYear: null,
          reissueArticle: null,
          reissueDate: null,
          reissueNote: null,
          reissueReference: null,
          repealArticle: null,
          repealDate: null,
          repealNote: null,
          repealReferences: null,
          risAbbreviationInternationalLaw: null,
          statusDate: null,
          statusDescription: null,
          statusNote: null,
          statusReference: null,
          subjectFna: null,
          subjectGesta: null,
          subjectPreviousFna: null,
          text: null,
          unofficialAbbreviation: null,
          unofficialLongTitle: null,
          unofficialReference: null,
          unofficialShortTitle: null,
          validityRule: null,
        }
      )
    })

    it("responds with correct error message if server response status is above 300", async () => {
      vi.mocked(httpClient).put.mockResolvedValueOnce({
        status: 300,
        data: "",
      })

      const response = await editNormFrame("fake-guid", {
        officialLongTitle: "new title",
      })

      expect(response.error?.title).toBe(
        "Dokumentationseinheit konnte nicht bearbeitet werden."
      )
    })

    it("responds with correct error message if connection failed", async () => {
      vi.mocked(httpClient).put.mockResolvedValueOnce({
        status: 500,
        error: { title: "error" },
      })

      const response = await editNormFrame("fake-guid", {
        officialLongTitle: "new title",
      })

      expect(response.error?.title).toBe(
        "Dokumentationseinheit konnte nicht bearbeitet werden."
      )
    })
  })
})
