import { ref } from "vue"
import { ValidationError } from "@/components/input/types"

type ValidationStore<T> = {
  getAll: () => ValidationError[]
  getByField: (field: T) => ValidationError | undefined
  getByMessage: (message: string) => ValidationError[]
  add: (message: string, instance: T) => void
  remove: (field: T) => void
  reset: () => void
  isValid: () => boolean
}

export function useValidationStore<T extends string>(): ValidationStore<T> {
  const validationErrors = ref<ValidationError[]>([])

  function getAll() {
    return validationErrors.value
  }

  function add(message: string, instance: T) {
    remove(instance)
    validationErrors.value?.push({ message, instance })
  }

  function getByField(field: T): ValidationError | undefined {
    return validationErrors.value.find((error) => error.instance == field)
  }

  function getByMessage(message: string): ValidationError[] {
    return validationErrors.value.filter((error) => error.message == message)
  }

  function remove(field: T) {
    if (getByField(field))
      validationErrors.value.splice(
        validationErrors.value.findIndex((error) => error.instance == field),
        1,
      )
  }

  function isValid(): boolean {
    return getAll().length == 0
  }

  function reset() {
    validationErrors.value = []
  }

  return {
    getAll,
    getByField,
    getByMessage,
    add,
    remove,
    reset,
    isValid,
  }
}
