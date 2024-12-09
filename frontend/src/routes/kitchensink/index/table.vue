<script setup lang="ts">
import { ref } from "vue"
import CellHeaderItem from "@/components/CellHeaderItem.vue"
import CellItem from "@/components/CellItem.vue"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import SearchResultStatus from "@/components/SearchResultStatus.vue"
import TableHeader from "@/components/TableHeader.vue"
import TableRow from "@/components/TableRow.vue"
import TableView from "@/components/TableView.vue"
import KitchensinkPage from "@/kitchensink/components/KitchensinkPage.vue"
import KitchensinkStory from "@/kitchensink/components/KitchensinkStory.vue"

const data = [
  {
    name: "Lea",
    date: new Date().toDateString(),
  },
  { name: "Roy", date: new Date(8.64e15).toDateString() },
]
const searchResultString = "Keine Ergebnisse gefunden."

const isLoading = ref(true)
</script>

<template>
  <KitchensinkPage name="Table">
    <KitchensinkStory name="">
      <TableView class="w-full">
        <TableHeader>
          <CellHeaderItem>Name</CellHeaderItem>
          <CellHeaderItem>Datum</CellHeaderItem>
        </TableHeader>
        <TableRow v-for="item in data" :key="item.name">
          <CellItem>{{ item.name }}</CellItem>
          <CellItem>{{ item.date }}</CellItem>
        </TableRow>
      </TableView>
    </KitchensinkStory>

    <KitchensinkStory name="No search result">
      <div class="flex h-full flex-col bg-white">
        <TableView class="w-full">
          <TableHeader>
            <CellHeaderItem>Name</CellHeaderItem>
            <CellHeaderItem>Datum</CellHeaderItem>
          </TableHeader>
          <TableRow />
        </TableView>
        <SearchResultStatus :text="searchResultString"></SearchResultStatus>
      </div>
    </KitchensinkStory>

    <KitchensinkStory name="Loading ">
      <div class="flex h-full flex-col bg-white">
        <TableView class="w-full">
          <TableHeader>
            <CellHeaderItem>Name</CellHeaderItem>
            <CellHeaderItem>Datum</CellHeaderItem>
          </TableHeader>
          <TableRow />
        </TableView>
        <SearchResultStatus
          v-if="!isLoading"
          :text="searchResultString"
        ></SearchResultStatus>
        <div
          v-if="isLoading"
          class="grid justify-items-center bg-white bg-opacity-60 py-112"
        >
          <LoadingSpinner />
        </div>
      </div>
    </KitchensinkStory>
  </KitchensinkPage>
</template>
