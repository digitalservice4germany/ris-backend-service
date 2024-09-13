<script setup lang="ts">
import FlexContainer from "@/components/FlexContainer.vue"
import { ResponseError } from "@/services/httpClient"

const props = defineProps<{ responseError?: ResponseError }>()
</script>
<template>
  <FlexContainer
    v-if="props.responseError"
    align-items="items-center"
    class="py-112"
    flex-direction="flex-col"
  >
    <span>{{ props.responseError.title }} </span>
    <span v-if="props.responseError.description"
      >{{ props.responseError.description }}
    </span>
    <slot />
  </FlexContainer>
</template>
