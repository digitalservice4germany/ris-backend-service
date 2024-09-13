<script lang="ts" setup>
import { computed, ref, watch } from "vue"
import { NodeHelperInterface } from "@/components/fieldOfLawNode"
import FlexContainer from "@/components/FlexContainer.vue"
import Checkbox from "@/components/input/CheckboxInput.vue"
import TokenizeText from "@/components/TokenizeText.vue"
import { FieldOfLaw } from "@/domain/fieldOfLaw"
import IconAdd from "~icons/ic/baseline-add"
import IconHorizontalRule from "~icons/ic/baseline-horizontal-rule"

interface Props {
  node: FieldOfLaw
  modelValue: FieldOfLaw[]
  showNorms: boolean
  nodeHelper: NodeHelperInterface
  expandValues: FieldOfLaw[]
  isRoot?: boolean
  rootChild?: boolean
  selectedNode?: FieldOfLaw
}

const props = defineProps<Props>()

const emit = defineEmits<{
  "node:select": [node: FieldOfLaw]
  "node:unselect": [node: FieldOfLaw]
  "node:expand": [node: FieldOfLaw]
  "node:collapse": [node: FieldOfLaw]
  "linked-field:select": [node: FieldOfLaw]
  "selected-node:reset": []
}>()

const isExpanded = ref(false)
const children = ref<FieldOfLaw[]>([])
const isSelected = computed({
  get: () =>
    props.modelValue.some(
      ({ identifier }) => identifier === props.node.identifier,
    ),
  set: (value) => {
    if (value) {
      emit("node:select", props.node)
    } else {
      emit("node:unselect", props.node)
    }
  },
})

function toggleExpanded() {
  isExpanded.value = !isExpanded.value
  if (isExpanded.value) {
    emit("node:expand", props.node)
  } else {
    emit("node:collapse", props.node)
    if (props.selectedNode && props.rootChild) {
      emit("selected-node:reset")
    }
  }
}

watch(
  props,
  () => {
    if (props.expandValues.length > 0) {
      isExpanded.value = props.expandValues.some(
        (expandedNode) => expandedNode.identifier == props.node.identifier,
      )
    }
  },
  { immediate: true },
)

watch(
  props,
  async () => {
    if (props.selectedNode && props.isRoot) {
      children.value = await props.nodeHelper.getFilteredChildren(
        props.node,
        props.expandValues,
      )
    } else if (props.isRoot) {
      children.value = await props.nodeHelper.getChildren(props.node)
    }
  },
  { immediate: true },
)
watch(
  isExpanded,
  async () => {
    if (isExpanded.value) {
      children.value = await props.nodeHelper.getChildren(props.node)
    }
  },
  { immediate: true },
)
</script>

<template>
  <FlexContainer flex-direction="flex-col">
    <FlexContainer class="min-h-[32px]" flex-direction="flex-row">
      <div v-if="node.hasChildren">
        <div v-if="isExpanded" class="w-[1.3333em] min-w-[1.3333em]">
          <button
            id="minus-button"
            :aria-label="node.text + ' einklappen'"
            class="w-icon rounded-full bg-blue-200 text-blue-800"
            @click="toggleExpanded"
          >
            <IconHorizontalRule />
          </button>
        </div>
        <div v-else>
          <button
            id="plus-button"
            :aria-label="node.text + ' aufklappen'"
            class="w-icon rounded-full bg-blue-200 text-blue-800"
            @click="toggleExpanded"
          >
            <IconAdd />
          </button>
        </div>
      </div>
      <span v-else class="pl-[1.3333em]" />
      <div v-if="!props.isRoot" data-testid>
        <Checkbox
          :id="`field-of-law-node-${node.identifier}`"
          v-model="isSelected"
          :aria-label="
            node.identifier +
            ' ' +
            node.text +
            (isSelected ? ' entfernen' : ' hinzufügen')
          "
          class="ds-checkbox-mini ml-8 bg-white"
          :data-testid="`field-of-law-node-${node.identifier}`"
        />
      </div>

      <div>
        <div class="flex flex-col">
          <div class="flex flex-row">
            <div
              v-if="!props.isRoot"
              class="whitespace-nowrap pl-8 text-[16px]"
            >
              {{ node.identifier }}
            </div>
            <div class="pl-6 pt-2 text-[14px] text-blue-800">
              <TokenizeText
                :keywords="props.node.linkedFields ?? []"
                :text="props.node.text"
                @linked-field:select="emit('linked-field:select', $event)"
              />
            </div>
          </div>
        </div>
        <FlexContainer
          v-if="showNorms"
          class="pb-6 pl-8"
          flex-direction="flex-col"
        >
          <FlexContainer
            class="text-[14px] text-[#66522e]"
            flex-wrap="flex-wrap"
          >
            <span v-for="(norm, idx) in node.norms" :key="idx">
              <strong>{{ norm.singleNormDescription }}</strong>
              {{ norm.abbreviation
              }}{{ idx < node.norms.length - 1 ? ",&nbsp;" : "" }}
            </span>
          </FlexContainer>
        </FlexContainer>
      </div>
    </FlexContainer>
    <div v-if="isExpanded">
      <FieldOfLawNodeComponent
        v-for="child in children"
        :key="child.identifier"
        class="pl-36"
        expand-if-selected
        :expand-values="expandValues"
        :model-value="modelValue"
        :node="child"
        :node-helper="nodeHelper"
        :root-child="props.isRoot"
        :selected-node="selectedNode"
        :show-norms="showNorms"
        @linked-field:select="emit('linked-field:select', $event)"
        @node:collapse="emit('node:collapse', $event)"
        @node:expand="emit('node:expand', $event)"
        @node:select="emit('node:select', $event)"
        @node:unselect="emit('node:unselect', $event)"
        @selected-node:reset="emit('selected-node:reset')"
      />
    </div>
  </FlexContainer>
</template>
