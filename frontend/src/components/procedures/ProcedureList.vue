<script lang="ts" setup>
import { debouncedWatch } from "@vueuse/core"
import { useRouteQuery } from "@vueuse/router"
import dayjs from "dayjs"
import { computed, onBeforeMount, ref, watch } from "vue"
import ProcedureDetail from "./ProcedureDetail.vue"
import { InfoStatus } from "@/components/enumInfoStatus"
import ExpandableContent from "@/components/ExpandableContent.vue"
import InfoModal from "@/components/InfoModal.vue"
import DropdownInput from "@/components/input/DropdownInput.vue"
import InputField from "@/components/input/InputField.vue"
import TextInput from "@/components/input/TextInput.vue"
import { DropdownItem } from "@/components/input/types"
import LoadingSpinner from "@/components/LoadingSpinner.vue"
import Pagination from "@/components/Pagination.vue"
import { useInternalUser } from "@/composables/useInternalUser"
import { Procedure } from "@/domain/documentUnit"
import DocumentUnitListEntry from "@/domain/documentUnitListEntry"
import { UserGroup } from "@/domain/userGroup"
import documentationUnitService from "@/services/documentUnitService"
import { ResponseError, ServiceResponse } from "@/services/httpClient"
import service from "@/services/procedureService"
import userGroupsService from "@/services/userGroupsService"
import IconBaselineDescription from "~icons/ic/baseline-description"
import IconExpandLess from "~icons/ic/baseline-expand-less"
import IconExpandMore from "~icons/ic/baseline-expand-more"
import IconFolderOpen from "~icons/material-symbols/folder-open"

const itemsPerPage = 10
const currentPage = ref(0)
const currentlyExpanded = ref<number[]>([])

const filter = useRouteQuery<string>("q")
const debouncedFilter = ref(filter.value)

const userGroups = ref<UserGroup[]>([])
const isInternalUser = useInternalUser()
const assignError = ref<ResponseError>()
const docUnitsForProcedure = ref<{
  [procedureId: string]: DocumentUnitListEntry[]
}>({})

const {
  data: procedurePage,
  error: responseError,
  isFetching: isFetchingProcedures,
  abort: abortFetchingProcedures,
  canAbort: canAbortFetchingProcedures,
  execute: fetchProcedures,
} = service.get(itemsPerPage, currentPage, debouncedFilter)
const procedures = computed(() => procedurePage.value?.content)

async function updateProcedures() {
  if (canAbortFetchingProcedures.value) {
    // If another request is still running, we cancel it.
    abortFetchingProcedures()
  }

  await fetchProcedures()
  // When fetching new procedures, we reset the currently expanded procedures
  currentlyExpanded.value = []
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
  if (!procedurePage.value?.content) return
  if (loadingProcedure.documentationUnitCount == 0) return
  if (!loadingProcedure.id) return

  const response = await service.getDocumentUnits(loadingProcedure.id)

  if (response.error) {
    responseError.value = response.error
    return
  }

  docUnitsForProcedure.value[loadingProcedure.id] = response.data
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
      procedures.value?.[procedures.value?.indexOf(updatedProcedure)]

    if (owningProcedure) {
      owningProcedure.documentationUnitCount -= 1

      docUnitsForProcedure.value[updatedProcedure.id!] =
        docUnitsForProcedure.value[updatedProcedure.id!].filter(
          (documentationUnit) =>
            documentationUnit.uuid != deletedDocumentationUnit.uuid,
        )
    }
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
 * Get display text for the date the procedure had been created.
 * If the date is missing a default text is displayed.
 */
const getCreatedAtDisplayText = (procedure: Procedure): string => {
  if (procedure.createdAt) {
    return "erstellt am " + dayjs(procedure.createdAt).format("DD.MM.YYYY")
  }
  return "Erstellungsdatum unbekannt"
}

// Wait for the user input to be finished before requesting. (debounce after last keystroke)
debouncedWatch(
  filter,
  () => {
    debouncedFilter.value = filter.value
    currentPage.value = 0
  },
  { debounce: 500 },
)

watch([currentPage, debouncedFilter], () => updateProcedures())

onBeforeMount(async () => {
  await getUserGroups()
})
</script>

<template>
  <div class="flex h-full flex-col space-y-24 bg-gray-100 px-16 py-16">
    <h1 class="ds-heading-02-reg">Vorgänge</h1>
    <div class="mt-24 flex flex-row items-center gap-4">
      <div class="w-480" role="search">
        <InputField id="procedureFilter" label="Vorgang" visually-hide-label>
          <TextInput
            id="procedureFilter"
            v-model="filter"
            aria-label="Nach Vorgängen suchen"
            class="ds-input-medium"
            placeholder="Nach Vorgängen suchen"
          ></TextInput>
        </InputField>
      </div>
      <LoadingSpinner v-if="isFetchingProcedures" size="small" />
    </div>
    <InfoModal
      v-if="assignError"
      :description="assignError.description"
      :status="InfoStatus.ERROR"
      :title="assignError.title"
    />

    <div v-if="procedures" class="flex-1">
      <Pagination
        v-if="procedurePage"
        navigation-position="bottom"
        :page="procedurePage"
        @update-page="(page) => (currentPage = page)"
      >
        <ExpandableContent
          v-for="(procedure, index) in procedures"
          :key="index"
          aria-label="Vorgang Listenelement"
          class="border-b-1 border-blue-300 bg-white p-24"
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
                v-if="isInternalUser"
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
            class="mt-24"
            :doc-units="docUnitsForProcedure[procedure.id ?? ''] ?? []"
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
