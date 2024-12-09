<script lang="ts" setup>
import { computed, ref, watch } from "vue"
import FieldOfLawTreeNode from "./FieldOfLawTreeNode.vue"
import {
  NodeHelper,
  NodeHelperInterface,
} from "@/components/field-of-law/fieldOfLawNode"
import CheckboxInput from "@/components/input/CheckboxInput.vue"
import InputField, { LabelPosition } from "@/components/input/InputField.vue"
import { buildRoot, FieldOfLaw } from "@/domain/fieldOfLaw"

const props = defineProps<{
  modelValue: FieldOfLaw[]
  nodeOfInterest?: FieldOfLaw
  searchResults?: FieldOfLaw[]
  showNorms: boolean
}>()

const emit = defineEmits<{
  "node:add": [node: FieldOfLaw]
  "node:remove": [node: FieldOfLaw]
  "node-of-interest:reset": []
  "linked-field:select": [node: FieldOfLaw]
  "toggle-show-norms": []
}>()

const root = ref(buildRoot())
const nodeHelper = ref<NodeHelperInterface>(new NodeHelper())
const expandedNodes = ref<FieldOfLaw[]>([])
const showNormsModelValue = computed({
  get: () => props.showNorms,
  set: () => emit("toggle-show-norms"),
})

async function expandNode(node: FieldOfLaw) {
  const itemsToReturn = new Map<string, FieldOfLaw>()
  if (props.nodeOfInterest) {
    itemsToReturn.set(node.identifier, node)
    const response = await nodeHelper.value.getAncestors(
      props.nodeOfInterest.identifier,
    )
    for (const node of response) {
      itemsToReturn.set(node.identifier, node)
    }
    itemsToReturn.set(root.value.identifier, root.value)
  }
  expandedNodes.value = Array.from(itemsToReturn.values())
}

async function expandNodesUpTo(node: FieldOfLaw) {
  const itemsToReturn = new Map<string, FieldOfLaw>()

  if (node.identifier == "root") {
    itemsToReturn.set(node.identifier, node)
    for (const selected of props.modelValue) {
      const response = await nodeHelper.value.getAncestors(selected.identifier)
      for (const node of response) {
        itemsToReturn.set(node.identifier, node)
      }
    }
    expandedNodes.value = Array.from(addExpandedNodes(itemsToReturn).values())
  } else {
    expandedNodes.value.push(node)
  }
}

function collapseNode(collapsedNode: FieldOfLaw) {
  expandedNodes.value = expandedNodes.value.filter(
    (node) => node.identifier !== collapsedNode.identifier,
  )
}

function addExpandedNodes(
  map: Map<string, FieldOfLaw>,
): Map<string, FieldOfLaw> {
  expandedNodes.value.forEach((node) => {
    map.set(node.identifier, node)
  })
  return map
}

function collapseTree() {
  expandedNodes.value = []
}

watch(
  () => props.nodeOfInterest,
  async (newValue, oldValue) => {
    if (newValue !== oldValue) {
      if (props.nodeOfInterest) await expandNode(props.nodeOfInterest)
    }
  },
  { immediate: true },
)

defineExpose({ collapseTree })
</script>

<template>
  <div class="flex flex-1 flex-col bg-blue-200 p-16">
    <div class="mb-20 flex w-full flex-row justify-between">
      <div class="flex">
        <p class="ds-label-01-reg">Sachgebietsbaum</p>
      </div>
      <div class="flex">
        <InputField
          id="showNorms"
          aria-label="Normen anzeigen"
          label="Mit Normen"
          label-class="ds-label-02-reg"
          :label-position="LabelPosition.RIGHT"
        >
          <CheckboxInput
            id="showNorms"
            v-model="showNormsModelValue"
            class="ds-checkbox-mini bg-white"
            size="small"
          />
        </InputField>
      </div>
    </div>

    <FieldOfLawTreeNode
      :key="root.identifier"
      :expand-values="expandedNodes"
      is-root
      :model-value="modelValue"
      :node="root"
      :node-helper="nodeHelper"
      :node-of-interest="nodeOfInterest"
      :search-results="searchResults"
      :show-norms="showNorms"
      @node-of-interest:reset="emit('node-of-interest:reset')"
      @node:add="emit('node:add', $event)"
      @node:collapse="collapseNode"
      @node:expand="expandNodesUpTo"
      @node:remove="emit('node:remove', $event)"
    />
  </div>
</template>
