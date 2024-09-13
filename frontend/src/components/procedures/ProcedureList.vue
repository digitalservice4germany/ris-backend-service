<script lang="ts" setup>
import dayjs from "dayjs"
import { computed, onMounted, ref, watch } from "vue"
import ProcedureDetail from "./ProcedureDetail.vue"
import { InfoStatus } from "@/components/enumInfoStatus"
import ExpandableContent from "@/components/ExpandableContent.vue"
import InfoModal from "@/components/InfoModal.vue"
import DropdownInput from "@/components/input/DropdownInput.vue"
import InputField from "@/components/input/InputField.vue"
import TextInput from "@/components/input/TextInput.vue"
import { DropdownItem } from "@/components/input/types"
import Pagination, { Page } from "@/components/Pagination.vue"
import { useInternalUser } from "@/composables/useInternalUser"
import useQuery, { Query } from "@/composables/useQueryFromRoute"
import { Procedure } from "@/domain/documentUnit"
import DocumentUnitListEntry from "@/domain/documentUnitListEntry"
import { UserGroup } from "@/domain/userGroup"
import documentationUnitService from "@/services/documentUnitService"
import FeatureToggleService from "@/services/featureToggleService"
import { ResponseError, ServiceResponse } from "@/services/httpClient"
import service from "@/services/procedureService"
import userGroupsService from "@/services/userGroupsService"
import IconBaselineDescription from "~icons/ic/baseline-description"
import IconExpandLess from "~icons/ic/baseline-expand-less"
import IconExpandMore from "~icons/ic/baseline-expand-more"
import IconFolderOpen from "~icons/material-symbols/folder-open"

const itemsPerPage = 10
const currentPage = ref<Page<Procedure>>()
const procedures = computed(() => currentPage.value?.content)
const currentlyExpanded = ref<number[]>([])
const { getQueryFromRoute, pushQueryToRoute, route } = useQuery<"q">()
const query = ref(getQueryFromRoute())
const responseError = ref()
const userGroups = ref<UserGroup[]>([])
const featureToggle = ref()
const isInternalUser = useInternalUser()
const assignError = ref<ResponseError>()

/**
 * Loads all procedures
 * @param {number} page - page to be updated
 * @param {Query<string>} queries - parameters from route to filter search results
 */
async function updateProcedures(page: number, queries?: Query<string>) {
  const response = await service.get(itemsPerPage, page, queries?.q)
  if (response.data) {
    currentPage.value = response.data
  }
}

/**
 * Get all external user groups for the current user and documentation office.
 */
async function getUserGroups() {
  const response = await userGroupsService.get()
  if (response.data) {
    userGroups.value = response.data
  }
}

/**
 * Loads document units and adds to local value
 */
async function loadDocumentUnits(loadingProcedure: Procedure) {
  if (responseError.value) {
    responseError.value = undefined
  }
  if (!currentPage.value?.content) return
  if (loadingProcedure.documentationUnitCount == 0) return
  if (loadingProcedure.documentUnits) return

  const response = await service.getDocumentUnits(loadingProcedure.id)

  if (response.error) {
    responseError.value = response.error
    return
  }

  const procedureIndex = currentPage.value.content.findIndex(
    (procedure) => procedure.label == loadingProcedure.label,
  )
  if (procedureIndex > -1) {
    currentPage.value.content[procedureIndex].documentUnits = response.data
  }
}

/**
 * Lazily loads the document units, only when the procedure list is expanded.
 * When the isExpanded state changes, the procedure is added / removed from
 * the currentlyExpanded procedures list.
 */
async function handleIsExpanded(
  isExpanded: boolean,
  procedure: Procedure,
  index: number,
) {
  if (isExpanded) {
    currentlyExpanded.value.push(index)
    await loadDocumentUnits(procedure)
  } else {
    currentlyExpanded.value = currentlyExpanded.value.filter(
      (item) => item !== index,
    )
  }
}

/**
 * Handles a change in the {@link DropdownInput}.
 * When the user selected an option with a procedureId and a userGroupId
 * the assign api is called.
 * When the user selected an option which contains only the procedureId the unassign api is called.
 **/
async function handleAssignUserGroup(
  procedureId: string | undefined,
  userGroupId: string | undefined,
) {
  let response: ServiceResponse<unknown> | undefined
  if (procedureId && userGroupId) {
    response = await service.assignUserGroup(procedureId, userGroupId)
  } else if (procedureId) {
    response = await service.unassignUserGroup(procedureId)
  }
  if (response?.error) {
    assignError.value = response.error
    alert(response.error.title)
  }
}

async function handleDeleteDocumentationUnit(
  deletedDocumentationUnit: DocumentUnitListEntry,
  updatedProcedure: Procedure,
) {
  const { error } = await documentationUnitService.delete(
    deletedDocumentationUnit.uuid!,
  )
  if (!error) {
    const owningProcedure =
      currentPage.value!.content[
        currentPage.value!.content.indexOf(updatedProcedure)
      ]

    owningProcedure.documentationUnitCount -= 1

    owningProcedure.documentUnits = updatedProcedure.documentUnits?.filter(
      (documentationUnit) =>
        documentationUnit.uuid != deletedDocumentationUnit.uuid,
    )
  }
}

/**
 * Transforms a pathName of a user group (e.g. "/caselaw/BGH/Extern/Agentur1") into
 * the name of the last subgroup (e.g. "Agentur1")
 **/
const getLastSubgroup = (userGroupPathName: string) => {
  return userGroupPathName.substring(userGroupPathName.lastIndexOf("/") + 1)
}

/**
 * Returns a list of {@link DropdownItem} containing the external user groups
 * of the documentation office.
 **/
const getDropdownItems = (): DropdownItem[] => {
  const dropdownItems = userGroups.value.map(({ userGroupPathName, id }) => ({
    label: getLastSubgroup(userGroupPathName),
    value: id,
  }))
  dropdownItems.push({ label: "Nicht zugewiesen", value: "" })
  return dropdownItems
}

/**
 * Sets a timeout before pushing the search query to the route,
 * in order to only change the url params when the user input pauses.
 */
const debouncedPushQueryToRoute = (() => {
  let timeoutId: number | null = null

  return (currentQuery: Query<string>) => {
    if (timeoutId != null) window.clearTimeout(timeoutId)

    timeoutId = window.setTimeout(() => pushQueryToRoute(currentQuery), 500)
  }
})()

/**
 * Get display text for the date the procedure had been created.
 * If the date is missing a default text is displayed.
 */
const getCreatedAtDisplayText = (procedure: Procedure): string => {
  if (procedure.createdAt) {
    return "erstellt am " + dayjs(procedure.createdAt).format("DD.MM.YYYY")
  }
  return "Erstellungsdatum unbekannt"
}

/**
 * Get query from url and set local query value
 */
watch(route, () => {
  const currentQuery = getQueryFromRoute()
  if (JSON.stringify(query.value) != JSON.stringify(currentQuery))
    query.value = currentQuery
})

/**
 * Update procedures with local query value und update url after timeout
 */
watch(
  query,
  async () => {
    await updateProcedures(0, query.value)
    debouncedPushQueryToRoute(query.value)
  },
  { deep: true },
)

/**
 * Switching pages closes all expanded items
 */
watch(currentPage, (newPage) => {
  if (newPage?.number !== currentPage.value) {
    currentlyExpanded.value = []
  }
})

onMounted(() => {
  updateProcedures(0, query.value)
  getUserGroups()
})
onMounted(async () => {
  featureToggle.value = (
    await FeatureToggleService.isEnabled("neuris.assign-procedure")
  ).data
})
</script>

<template>
  <div class="flex h-full flex-col space-y-24 bg-gray-100 px-16 py-16">
    <h1 class="ds-heading-02-reg">Vorgänge</h1>
    <div class="mt-24 w-480" role="search">
      <InputField id="procedureFilter" label="Vorgang" visually-hide-label>
        <TextInput
          id="procedureFilter"
          v-model="query.q"
          aria-label="Nach Vorgängen suchen"
          class="ds-input-medium"
          placeholder="Nach Vorgängen suchen"
        ></TextInput>
      </InputField>
    </div>
    <InfoModal
      v-if="assignError"
      :description="assignError.description"
      :status="InfoStatus.ERROR"
      :title="assignError.title"
    />

    <div v-if="procedures" class="flex-1">
      <Pagination
        v-if="currentPage"
        navigation-position="bottom"
        :page="currentPage"
        @update-page="(page) => updateProcedures(page, query)"
      >
        <ExpandableContent
          v-for="(procedure, index) in procedures"
          :key="index"
          aria-label="Vorgang Listenelement"
          class="border-b-1 border-blue-300 bg-white px-24 py-20"
          :class="{
            'my-24': currentlyExpanded.includes(index),
            'hover:bg-blue-100': !currentlyExpanded.includes(index),
          }"
          :is-expanded="currentlyExpanded.includes(index)"
          @update:is-expanded="
            (isExpanded) => handleIsExpanded(isExpanded, procedure, index)
          "
        >
          <template #open-icon>
            <IconExpandMore class="text-blue-800" />
          </template>

          <template #close-icon>
            <IconExpandLess class="text-blue-800" />
          </template>

          <template #header>
            <div class="flex w-full justify-between gap-24">
              <div class="flex flex-row items-center gap-16">
                <IconFolderOpen />
                <span class="ds-label-01-reg" :title="procedure.label">{{
                  procedure.label
                }}</span>
                <div
                  class="ds-label-02-reg flex flex-row items-center gap-4 rounded-full bg-blue-300 px-8 py-2 outline-none"
                >
                  <IconBaselineDescription class="w-16 text-blue-800" />
                  <span>
                    {{ procedure.documentationUnitCount }}
                  </span>
                </div>
              </div>
              <DropdownInput
                v-if="featureToggle && isInternalUser"
                v-model="procedure.userGroupId"
                aria-label="dropdown input"
                class="ml-auto w-auto"
                is-small
                :items="getDropdownItems()"
                @click.stop
                @update:model-value="
                  (value: string | undefined) =>
                    handleAssignUserGroup(procedure.id, value)
                "
              />
              <span class="mr-24 w-224 content-center text-center">{{
                getCreatedAtDisplayText(procedure)
              }}</span>
            </div>
          </template>

          <ProcedureDetail
            :procedure="procedure"
            :response-error="responseError"
            @delete-document-unit="handleDeleteDocumentationUnit"
          />
        </ExpandableContent>
      </Pagination>
    </div>
    <div
      v-else
      class="flex h-full w-full flex-1 items-center justify-center bg-white"
    >
      <div class="flex flex-col items-center space-y-24">
        <span
          class="flex h-128 w-128 items-center justify-center rounded-full bg-blue-200"
        >
          <IconFolderOpen class="text-64 text-blue-700" />
        </span>
        <span>Es wurden noch keine Vorgänge angelegt.</span>
      </div>
    </div>
  </div>
</template>
