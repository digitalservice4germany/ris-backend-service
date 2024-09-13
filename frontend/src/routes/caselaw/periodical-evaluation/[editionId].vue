<script lang="ts" setup>
import { computed, onMounted, ref } from "vue"
import { useRoute } from "vue-router"
import NavbarSide from "@/components/NavbarSide.vue"
import ErrorPage from "@/components/PageError.vue"
import PeriodicalEditionInfoPanel from "@/components/periodical-evaluation/PeriodicalInfoPanel.vue"
import { usePeriodicalEvaluationMenuItems } from "@/composables/usePeriodicalEvaluationMenuItems"
import { ResponseError } from "@/services/httpClient"
import { useEditionStore } from "@/stores/editionStore"
import StringsUtil from "@/utils/stringsUtil"

const store = useEditionStore()
const responseError = ref<ResponseError>()
const route = useRoute()

const infoSubtitle = computed(() =>
  StringsUtil.mergeNonBlankStrings(
    [store.edition?.legalPeriodical?.abbreviation, store.edition?.name],
    " ",
  ),
)

onMounted(async () => {
  const response = await store.loadEdition()
  if (response.error) {
    responseError.value = response.error
  }
})

const menuItems = usePeriodicalEvaluationMenuItems(
  store.edition?.id,
  route.query,
)
</script>

<template>
  <div class="flex w-screen grow">
    <div
      v-if="store.edition"
      class="sticky top-0 z-50 flex flex-col border-r-1 border-solid border-gray-400 bg-white"
    >
      <NavbarSide :is-child="false" :menu-items="menuItems" :route="route" />
    </div>
    <div v-if="store.edition" class="flex w-full min-w-0 flex-col bg-gray-100">
      <PeriodicalEditionInfoPanel :subtitle="infoSubtitle" />

      <div class="flex grow flex-col items-start">
        <router-view />
      </div>
    </div>
    <ErrorPage
      v-if="responseError"
      :error="responseError"
      :title="responseError?.title"
    />
  </div>
</template>
