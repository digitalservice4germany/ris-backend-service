<script lang="ts" setup>
import { computed } from "vue"
import TextEditor from "../components/input/TextEditor.vue"
import TextAreaInput from "@/components/input/TextAreaInput.vue"
import TextInput from "@/components/input/TextInput.vue"
import { useInternalUser } from "@/composables/useInternalUser"
import { useValidBorderNumbers } from "@/composables/useValidBorderNumbers"
import { Texts } from "@/domain/documentUnit"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import StringsUtil from "@/utils/stringsUtil"

const store = useDocumentUnitStore()
const isInternalUser = useInternalUser()

function isEditableByUser(item: { name: string }): boolean {
  switch (item.name) {
    case "tenor":
    case "reasons":
    case "caseFacts":
    case "decisionReasons":
    case "dissentingOpinion":
    case "otherLongText":
    case "outline":
      return isInternalUser
    default:
      return true
  }
}

const data = computed(() => {
  if (store.documentUnit == undefined) return null
  return useValidBorderNumbers(
    store.documentUnit.texts,
    store.documentUnit?.borderNumbers,
  ).filter(isEditableByUser)
})

const updateValueByTextId = async (id: keyof Texts, updatedText?: string) => {
  if (StringsUtil.isEmpty(updatedText)) {
    store.documentUnit!.texts[id] = undefined
  } else {
    const divElem = document.createElement("div")
    if (updatedText == undefined) updatedText = ""
    divElem.innerHTML = updatedText
    const hasImgElem = divElem.getElementsByTagName("img").length > 0
    const hasTable = divElem.getElementsByTagName("table").length > 0
    const hasInnerText = divElem.innerText.length > 0
    store.documentUnit!.texts[id] =
      hasInnerText || hasImgElem || hasTable ? updatedText : ""
  }
}
</script>

<template>
  <div class="core-data mb-16 flex flex-col gap-24 bg-white p-32">
    <h2 class="ds-heading-03-bold">Kurz- & Langtexte</h2>

    <div class="flex flex-col gap-24">
      <div v-for="item in data" :key="item.id" class="">
        <label class="ds-label-02-reg mb-4" :for="item.id">{{
          item.label
        }}</label>

        <TextEditor
          v-if="item.fieldType == TextAreaInput"
          :id="item.id"
          :aria-label="item.aria"
          class="shadow-blue focus-within:shadow-focus hover:shadow-hover"
          editable
          :field-size="item.fieldSize"
          :value="item.value"
          @update-value="updateValueByTextId(item.id, $event)"
        />

        <TextInput
          v-else-if="item.fieldType == TextInput"
          :id="item.id"
          :aria-label="item.aria"
          :model-value="item.value"
          size="medium"
          @update:model-value="updateValueByTextId(item.id, $event)"
        />
      </div>
    </div>
  </div>
</template>
