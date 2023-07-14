import { computed } from "vue"
import type { Ref } from "vue"
import type { RouteLocationNormalizedLoaded } from "vue-router"

export function useNormMenuItems(
  normGuid: Ref<string>,
  route: RouteLocationNormalizedLoaded,
  exportIsEnabled?: Ref<boolean>,
) {
  const baseRoute = {
    params: { guid: normGuid.value },
    query: route.query,
  }

  const getChildItem = (label: string, id: string) => ({
    label: label,
    route: {
      ...baseRoute,
      name: "norms-norm-:normGuid-frame",
      hash: `#${id}`,
    },
  })

  return computed(() => [
    {
      label: "Normenkomplex",
      route: {
        ...baseRoute,
        name: "norms-norm-:normGuid",
      },
    },
    {
      label: "Rahmen",
      route: {
        ...baseRoute,
        name: "norms-norm-:normGuid-frame",
      },
      children: [
        getChildItem("Allgemeine Angaben", "officialLongTitle"),
        getChildItem("Dokumenttyp", "documentTypes"),
        getChildItem("Normgeber", "normProviders"),
        getChildItem("Mitwirkende Organe", "participatingInstitutions"),
        getChildItem("Federführung", "leads"),
        getChildItem("Sachgebiet", "subjectAreas"),
        getChildItem("Überschriften und Abkürzungen", "officialShortTitle"),
        getChildItem("Inkrafttreten", "entryIntoForces"),
        getChildItem("Außerkrafttreten", "expirations"),
        getChildItem("Verkündungsdatum", "announcementDate"),
        getChildItem("Zitierdatum", "citationDates"),
        getChildItem("Amtliche Fundstelle", "officialReferences"),
        getChildItem("Nichtamtliche Fundstelle", "unofficialReferences"),
        getChildItem("Vollzitat", "completeCitation"),
        getChildItem("Stand-Angabe", "statusIndication"),
        getChildItem(
          "Stand der dokumentarischen Bearbeitung",
          "documentStatus",
        ),
        getChildItem("Aktivverweisung", "categorizedReferences"),
        getChildItem("Fußnoten", "footnotes"),
        getChildItem("Gültigkeitsregelung", "validityRules"),
        getChildItem("Elektronischer Nachweis", "digitalEvidence"),
        getChildItem("Aktenzeichen", "referenceNumbers"),
        getChildItem("ELI", "eli"),
        getChildItem("CELEX-Nummer", "celexNumber"),
        getChildItem("Altersangabe", "ageIndications"),
        getChildItem("Definition", "definitions"),
        getChildItem("Angaben zur Volljährigkeit", "ageOfMajorityIndications"),
        getChildItem("Text", "text"),
      ],
    },
    {
      label: "Bestand",
      route: {
        ...baseRoute,
        name: "norms",
      },
    },
    {
      label: "Export",
      route: {
        ...baseRoute,
        name: "norms-norm-:normGuid-export",
      },
      isDisabled: !(exportIsEnabled?.value ?? false),
    },
  ])
}
