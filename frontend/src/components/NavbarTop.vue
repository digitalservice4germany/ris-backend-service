<script lang="ts" setup>
import { ref, computed } from "vue"
import { useRoute } from "vue-router"
import FlexContainer from "@/components/FlexContainer.vue"
import FlexItem from "@/components/FlexItem.vue"
import IconBadge from "@/components/IconBadge.vue"
import useSessionStore from "@/stores/sessionStore"
import IconPermIdentity from "~icons/ic/baseline-perm-identity"

const route = useRoute()
const session = useSessionStore()
const fontColor = ref<string>()

const badge = computed(() => {
  const docOffice = session.user?.documentationOffice
    ? session.user.documentationOffice.abbreviation
    : undefined
  if (session.env == "staging") {
    return {
      color: "bg-red-300",
      label: docOffice ? `${docOffice} | Staging` : "",
    }
  } else if (session.env == "uat") {
    return {
      color: "bg-yellow-300",
      label: docOffice ? `${docOffice} | UAT` : "",
    }
  } else {
    return {
      color: "bg-blue-300",
      label: docOffice ?? "",
    }
  }
})
</script>

<template>
  <nav
    class="flex items-center justify-between border-y border-gray-400 py-16 pe-16 print:hidden"
  >
    <div class="flex items-center gap-44">
      <div class="flex items-center">
        <span class="px-[1rem] text-14 font-bold uppercase leading-16">
          <span aria-hidden="true" :style="{ color: fontColor }">
            Rechtsinformationen</span
          >
          <br />
          <span aria-hidden="true">des Bundes</span>
        </span>
      </div>

      <router-link
        class="ds-label-01-reg p-8 hover:bg-yellow-500 hover:underline"
        :class="{
          underline:
            route.path === '/caselaw' ||
            route.path.includes('caselaw/documentunit'),
        }"
        data-testid="search-navbar-button"
        :to="{ name: 'caselaw' }"
        >Suche
      </router-link>
      <router-link
        class="ds-label-01-reg p-8 hover:bg-yellow-500 hover:underline"
        :class="{ underline: route.path.includes('procedures') }"
        :to="{ name: 'caselaw-procedures' }"
        >Vorgänge
      </router-link>
      <router-link
        class="ds-label-01-reg p-8 hover:bg-yellow-500 hover:underline"
        :class="{
          underline: route.path.includes('periodical-evaluation'),
        }"
        :to="{ name: 'caselaw-periodical-evaluation' }"
        >Periodika
      </router-link>
    </div>

    <div v-if="session.user" class="grid grid-cols-[auto,1fr] gap-10">
      <IconPermIdentity />
      <div>
        <div class="ds-label-01-reg">
          <router-link :to="{ name: 'settings' }">
            <FlexContainer>
              <FlexItem class="pe-8">{{ session.user?.name }}</FlexItem>
              <FlexItem>
                <IconBadge
                  v-if="session.user.documentationOffice"
                  :background-color="badge.color"
                  color="text-black"
                  :label="badge.label"
                />
              </FlexItem>
            </FlexContainer>
          </router-link>
        </div>
      </div>
    </div>
  </nav>
</template>
