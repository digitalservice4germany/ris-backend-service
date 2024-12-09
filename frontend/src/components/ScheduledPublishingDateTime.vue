<script setup lang="ts">
import dayjs from "dayjs"
import customParseFormat from "dayjs/plugin/customParseFormat"
import dayjsTimezone from "dayjs/plugin/timezone"
import dayjsUtc from "dayjs/plugin/utc"
import { computed, ref } from "vue"
import { ValidationError } from "./input/types"
import InfoModal from "@/components/InfoModal.vue"
import DateInput from "@/components/input/DateInput.vue"
import InputField from "@/components/input/InputField.vue"
import TextButton from "@/components/input/TextButton.vue"
import TimeInput from "@/components/input/TimeInput.vue"
import { ResponseError } from "@/services/httpClient"
import { useDocumentUnitStore } from "@/stores/documentUnitStore"
import useSessionStore from "@/stores/sessionStore"
import IconErrorOutline from "~icons/ic/baseline-error-outline"

const { isPublishable } = defineProps<{ isPublishable: boolean }>()

dayjs.extend(dayjsUtc)
dayjs.extend(dayjsTimezone)
dayjs.extend(customParseFormat)

const documentUnitStore = useDocumentUnitStore()
const sessionStore = useSessionStore()

const storedScheduledPublicationDateTime = computed({
  get: () =>
    documentUnitStore.documentUnit!.managementData.scheduledPublicationDateTime,
  set: (newDate?: string) =>
    (documentUnitStore.documentUnit!.managementData.scheduledPublicationDateTime =
      newDate),
})

// initialize local values with stored date-time
/* eslint-disable vue/no-ref-object-destructure */
const scheduledPublishingDate = ref<string | undefined>(
  storedScheduledPublicationDateTime.value &&
    dayjs
      .utc(storedScheduledPublicationDateTime.value)
      .tz("Europe/Berlin")
      .format("YYYY-MM-DD"),
)
const scheduledPublishingTime = ref<string | undefined>(
  (storedScheduledPublicationDateTime.value &&
    dayjs
      .utc(storedScheduledPublicationDateTime.value)
      .tz("Europe/Berlin")
      .format("HH:mm")) ||
    "05:00",
)
/* eslint-enable vue/no-ref-object-destructure */

const scheduledDateTimeInput = computed(() =>
  dayjs.tz(
    scheduledPublishingDate.value + "T" + scheduledPublishingTime.value,
    "Europe/Berlin",
  ),
)

const isScheduled = computed<boolean>(
  () => !!storedScheduledPublicationDateTime.value,
)

const isDateInFuture = computed<boolean>(
  () =>
    !scheduledPublishingDate.value ||
    !scheduledPublishingTime.value ||
    scheduledDateTimeInput.value.isAfter(new Date()),
)

const isDateValid = computed<boolean>(() =>
  dayjs(scheduledPublishingDate.value, "YYYY-MM-DD", true).isValid(),
)

const isTimeValid = computed<boolean>(() =>
  dayjs(scheduledPublishingTime.value, "HH:mm", true).isValid(),
)

const isValidDateTime = computed<boolean>(
  () =>
    isDateInFuture.value &&
    isDateValid.value &&
    isTimeValid.value &&
    !dateValidationError.value,
)

const docUnitSaveError = ref<ResponseError | null>(null)

const saveScheduling = async () => {
  docUnitSaveError.value = null
  if (dateValidationError.value || !isDateValid.value || !isTimeValid.value) {
    // This is needed as the DateInput does not update its modelValue when the date is incomplete.
    // First input correct date "01.01.2080", then delete last char "01.01.208". Without blurring input, click on button.
    return
  }

  storedScheduledPublicationDateTime.value =
    scheduledDateTimeInput.value.toISOString()

  documentUnitStore.documentUnit!.managementData.scheduledByEmail =
    sessionStore.user?.email

  const { error } = await documentUnitStore.updateDocumentUnit()

  if (error) {
    storedScheduledPublicationDateTime.value = undefined
    docUnitSaveError.value = error
  } else {
    scheduledPublishingDate.value = dayjs(
      storedScheduledPublicationDateTime.value,
    )
      .tz("Europe/Berlin")
      .format("YYYY-MM-DD")
  }
}

const removeScheduling = async () => {
  docUnitSaveError.value = null
  const previousDate = storedScheduledPublicationDateTime.value
  storedScheduledPublicationDateTime.value = undefined
  documentUnitStore.documentUnit!.managementData.scheduledByEmail = undefined

  const { error } = await documentUnitStore.updateDocumentUnit()

  if (error) {
    storedScheduledPublicationDateTime.value = previousDate
    docUnitSaveError.value = error
  } else {
    scheduledPublishingDate.value = undefined
    scheduledPublishingTime.value = "05:00"
  }
}

const dateValidationError = ref<ValidationError | undefined>()
</script>

<template>
  <div class="flex flex-col gap-16">
    <p>Oder für später terminieren:</p>
    <div class="flex max-w-640 flex-row items-end gap-8">
      <InputField id="scheduledPublishingDate" label="Datum *">
        <DateInput
          id="publishingDate"
          v-model="scheduledPublishingDate"
          aria-label="Terminiertes Datum"
          class="ds-input-medium"
          :disabled="!isPublishable"
          :has-error="!!dateValidationError"
          is-future-date
          :read-only="isScheduled"
          @update:validation-error="(error) => (dateValidationError = error)"
        ></DateInput>
      </InputField>
      <InputField id="publishingTime" label="Uhrzeit *">
        <TimeInput
          id="publishingTime"
          v-model="scheduledPublishingTime"
          aria-label="Terminierte Uhrzeit"
          class="ds-input-medium"
          :disabled="!isPublishable"
          :read-only="isScheduled"
        ></TimeInput>
      </InputField>
      <TextButton
        v-if="!isScheduled"
        aria-label="Termin setzen"
        button-type="primary"
        class="w-fit"
        :disabled="!isPublishable || !isValidDateTime"
        label="Termin&nbsp;setzen"
        size="medium"
        @click="saveScheduling"
      />
      <TextButton
        v-if="isScheduled"
        aria-label="Termin löschen"
        button-type="destructive"
        class="w-fit shrink-0"
        label="Termin&nbsp;löschen"
        size="medium"
        @click="removeScheduling"
      />
    </div>
    <div
      v-if="
        (!isScheduled && !isDateInFuture) ||
        dateValidationError ||
        !isTimeValid ||
        (!isPublishable && scheduledPublishingDate)
      "
      class="flex flex-row items-center"
      data-testid="scheduledPublishingDate_errors"
    >
      <IconErrorOutline class="pr-4 text-14 text-red-800" />

      <div class="flex-col">
        <div
          v-if="!isDateInFuture"
          class="lex-row ds-label-03-reg mt-2 text-red-800"
        >
          Der Terminierungszeitpunkt muss in der Zukunft liegen.
        </div>
        <div
          v-if="dateValidationError"
          class="lex-row ds-label-03-reg mt-2 text-red-800"
        >
          {{ dateValidationError.message }}.
        </div>
        <div
          v-if="!isTimeValid"
          class="lex-row ds-label-03-reg mt-2 text-red-800"
        >
          Unvollständige Uhrzeit.
        </div>
        <div
          v-if="!isPublishable && scheduledPublishingDate"
          class="lex-row ds-label-03-reg mt-2 text-red-800"
        >
          Die terminierte Abgabe kann aufgrund von Fehlern in der
          Plausibilitätsprüfung nicht durchgeführt werden.
        </div>
      </div>
    </div>
    <InfoModal
      v-if="docUnitSaveError"
      aria-label="Fehler bei der Terminierung"
      class="mt-8"
      :description="docUnitSaveError.description"
      :title="docUnitSaveError.title"
    />
  </div>
</template>
