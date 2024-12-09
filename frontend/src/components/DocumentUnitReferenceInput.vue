<script lang="ts" setup>
import { computed, ref, watch } from "vue"
import ComboboxInput from "@/components/ComboboxInput.vue"
import InputField, { LabelPosition } from "@/components/input/InputField.vue"
import RadioInput from "@/components/input/RadioInput.vue"
import TextButton from "@/components/input/TextButton.vue"
import TextInput from "@/components/input/TextInput.vue"
import { useValidationStore } from "@/composables/useValidationStore"
import LegalPeriodical from "@/domain/legalPeriodical"
import Reference from "@/domain/reference"
import ComboboxItemService from "@/services/comboboxItemService"

const props = defineProps<{
  modelValue?: Reference
  modelValueList?: Reference[]
}>()

const emit = defineEmits<{
  "update:modelValue": [value: Reference]
  addEntry: [void]
  cancelEdit: [void]
  removeEntry: [value?: boolean]
}>()

const reference = ref(new Reference({ ...props.modelValue }))
const lastSavedModelValue = ref(new Reference({ ...props.modelValue }))

const validationStore = useValidationStore<(typeof Reference.fields)[number]>()

const legalPeriodical = computed({
  get: () =>
    reference?.value?.legalPeriodical?.abbreviation
      ? {
          label: reference.value.legalPeriodical.abbreviation,
          value: reference.value.legalPeriodical,
          additionalInformation: reference.value.legalPeriodical.subtitle,
        }
      : undefined,
  set: (newValue) => {
    const legalPeriodical = { ...newValue } as LegalPeriodical
    if (newValue) {
      reference.value.legalPeriodical = legalPeriodical
    } else {
      reference.value.legalPeriodical = undefined
    }
  },
})

const fieldsMissing = computed(
  () =>
    (reference.value.referenceType === "caselaw" &&
      reference.value.hasMissingRequiredFieldsForDocunit) ||
    (reference.value.referenceType === "literature" &&
      reference.value.hasMissingRequiredLiteratureFields),
)

async function validateRequiredInput() {
  validationStore.reset()
  reference.value.missingRequiredFields.forEach((missingField) =>
    validationStore.add("Pflichtfeld nicht befüllt", missingField),
  )

  reference.value.missingRequiredLiteratureFields.forEach((missingField) =>
    validationStore.add("Pflichtfeld nicht befüllt", missingField),
  )
  validationStore.remove("referenceSupplement")
}

async function addReference() {
  await validateRequiredInput()

  if (!fieldsMissing.value) {
    emit("update:modelValue", reference.value as Reference)
    emit("addEntry")
  }
}

/**
 * This updates the local reference with the updated model value from the props. It also stores a copy of the last saved
 * model value, because the local reference might change in between.
 */
watch(
  () => props.modelValue,
  () => {
    reference.value = new Reference({ ...props.modelValue })
    lastSavedModelValue.value = new Reference({ ...props.modelValue })
    if (lastSavedModelValue.value.isEmpty) {
      validationStore.reset()
    } else if (
      !lastSavedModelValue.value.legalPeriodical &&
      !!lastSavedModelValue.value.legalPeriodicalRawValue
    ) {
      validationStore.add("Mehrdeutiger Verweis", "legalPeriodical")
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="flex flex-col gap-24">
    <div v-if="lastSavedModelValue.isEmpty" class="flex items-center gap-16">
      <div class="flex items-center">
        <InputField
          id="caselaw"
          class="flex items-center"
          label="Rechtsprechung"
          :label-position="LabelPosition.RIGHT"
        >
          <RadioInput
            v-model="reference.referenceType"
            aria-label="Rechtsprechung Fundstelle"
            name="referenceType"
            size="medium"
            value="caselaw"
            @click="validationStore.reset()"
          />
        </InputField>
      </div>

      <div class="flex items-center">
        <InputField
          id="literature"
          class="flex items-center"
          label="Literatur"
          :label-position="LabelPosition.RIGHT"
        >
          <RadioInput
            v-model="reference.referenceType"
            aria-label="Literatur Fundstelle"
            name="referenceType"
            size="medium"
            value="literature"
            @click="validationStore.reset()"
          />
        </InputField>
      </div>
    </div>
    <InputField
      id="legalPeriodical"
      v-slot="slotProps"
      label="Periodikum *"
      :validation-error="validationStore.getByField('legalPeriodical')"
    >
      <ComboboxInput
        id="legalPeriodical"
        v-model="legalPeriodical"
        aria-label="Periodikum"
        clear-on-choosing-item
        :has-error="slotProps.hasError"
        :item-service="ComboboxItemService.getLegalPeriodicals"
        @focus="validationStore.remove('legalPeriodical')"
      ></ComboboxInput>
    </InputField>

    <div class="flex flex-col gap-24">
      <div class="flex justify-between gap-24">
        <div class="flex w-full flex-col">
          <InputField
            id="citation"
            v-slot="slotProps"
            label="Zitatstelle *"
            :validation-error="validationStore.getByField('citation')"
          >
            <TextInput
              id="citation"
              v-model="reference.citation"
              aria-label="Zitatstelle"
              :has-error="slotProps.hasError"
              size="medium"
              @focus="validationStore.remove('citation')"
            ></TextInput>
          </InputField>
          <span v-if="legalPeriodical" class="ds-label-03-reg pt-4"
            >Zitierbeispiel: {{ legalPeriodical.value.citationStyle }}</span
          >
        </div>
        <InputField
          v-if="reference.referenceType === 'caselaw'"
          id="referenceSupplement"
          v-slot="slotProps"
          label="Klammernzusatz"
          :validation-error="validationStore.getByField('referenceSupplement')"
        >
          <TextInput
            id="referenceSupplement"
            v-model="reference.referenceSupplement"
            aria-label="Klammernzusatz"
            :has-error="slotProps.hasError"
            size="medium"
            @focus="validationStore.remove('referenceSupplement')"
          ></TextInput>
        </InputField>
        <InputField
          v-if="reference.referenceType === 'literature'"
          id="literatureReferenceDocumentType"
          v-slot="slotProps"
          label="Dokumenttyp *"
          :validation-error="validationStore.getByField('documentType')"
        >
          <ComboboxInput
            id="literatureReferenceDocumentType"
            v-model="reference.documentType"
            aria-label="Dokumenttyp Literaturfundstelle"
            :has-error="slotProps.hasError"
            :item-service="
              ComboboxItemService.getDependentLiteratureDocumentTypes
            "
            @focus="validationStore.remove('documentType')"
          ></ComboboxInput>
        </InputField>
      </div>
      <div
        v-if="reference.referenceType === 'literature'"
        class="w-[calc(50%-10px)]"
      >
        <InputField
          id="literatureReferenceDocumentType"
          v-slot="slotProps"
          label="Autor *"
          :validation-error="validationStore.getByField('author')"
        >
          <TextInput
            id="literatureReferenceDocumentType"
            v-model="reference.author"
            aria-label="Autor Literaturfundstelle"
            :has-error="slotProps.hasError"
            size="medium"
            @focus="validationStore.remove('author')"
          ></TextInput>
        </InputField>
      </div>
    </div>
    <div class="flex w-full flex-row justify-between">
      <div>
        <div class="flex gap-16">
          <TextButton
            aria-label="Fundstelle speichern"
            button-type="tertiary"
            :disabled="reference.isEmpty"
            label="Übernehmen"
            size="small"
            @click.stop="addReference"
          />
          <TextButton
            v-if="!lastSavedModelValue.isEmpty"
            aria-label="Abbrechen"
            button-type="ghost"
            label="Abbrechen"
            size="small"
            @click.stop="emit('cancelEdit')"
          />
        </div>
      </div>
      <TextButton
        v-if="!lastSavedModelValue.isEmpty"
        aria-label="Eintrag löschen"
        button-type="destructive"
        label="Eintrag löschen"
        size="small"
        @click.stop="emit('removeEntry', true)"
      />
    </div>
  </div>
</template>
