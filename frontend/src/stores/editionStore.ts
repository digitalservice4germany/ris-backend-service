import { UUID } from "crypto"
import { defineStore } from "pinia"
import { ref } from "vue"
import { useRoute } from "vue-router"
import LegalPeriodicalEdition from "@/domain/legalPeriodicalEdition"
import { ServiceResponse } from "@/services/httpClient"
import LegalPeriodicalEditionService from "@/services/legalPeriodicalEditionService"

export const useEditionStore = defineStore("editionStore", () => {
  const edition = ref<LegalPeriodicalEdition | undefined>(undefined)
  const route = useRoute()

  async function loadEdition(): Promise<
    ServiceResponse<LegalPeriodicalEdition>
  > {
    const uuid = route.params.editionId
    const response = await LegalPeriodicalEditionService.get(
      uuid.toString() as UUID,
    )

    if (response.data) {
      edition.value = new LegalPeriodicalEdition({
        ...response.data,
      })
    }

    return response
  }

  async function updateEdition(): Promise<
    ServiceResponse<LegalPeriodicalEdition>
  > {
    const response = await LegalPeriodicalEditionService.save(
      edition.value as LegalPeriodicalEdition,
    )

    return response
  }

  return {
    edition,
    loadEdition,
    updateEdition,
  }
})
