<script lang="ts" setup>
import { computed, watchEffect, watch } from "vue"
import { useRouter } from "vue-router"
import NavbarSide from "@/components/NavbarSide.vue"
import MenuItem from "@/domain/menuItem"

const router = useRouter()

// Generates the navigation items based on the children within the kitchensink-
// index route. This will:
//
// - Filter out all routes that are not children of kitchensink-index
// - Guess the name by removing the prefix and converting the rest to title case
// - Sort the items alphabetically
//
// To add a new page to the kitchensink, simply add a new page in /kitchensink/index.
const navigation = computed<MenuItem[]>(() =>
  router
    .getRoutes()
    .filter(({ name }) => name?.toString().startsWith("kitchensink-index-"))
    .map<MenuItem>((route) => {
      const label = route.name
        ?.toString()
        .replace("kitchensink-index-", "")
        .replace(/(^\w|[A-Z])/g, (match) => ` ${match.toUpperCase()}`)
        .trim()

      return {
        label: label ?? "Unnamed component",
        route: { name: route.name?.toString() as string },
      }
    })
    .sort((a, b) => (a.label < b.label ? -1 : 1)),
)

// Redirect to the first page in the kitchensink if the user navigates to the
// kitchensink index.
watchEffect(async () => {
  if (
    router.currentRoute.value.name === "kitchensink" &&
    navigation.value.length > 0
  ) {
    await router.replace(navigation.value[0].route)
  }
})

// Scroll to the top of the page when the user navigates to a new page. (because
// the navigation is quite long).
watch(router.currentRoute, () => {
  window.scrollTo(0, 0)
})
</script>

<template>
  <div class="flex bg-gray-100">
    <NavbarSide
      class="flex-none border-r border-gray-400 bg-white"
      go-back-label="Zur Anwendung"
      :go-back-route="{ name: 'index' }"
      :menu-items="navigation"
    />

    <main class="w-full flex-1 p-48">
      <RouterView />
    </main>
  </div>
</template>
