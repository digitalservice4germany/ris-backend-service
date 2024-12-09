<script setup lang="ts">
import TextButton from "@/components/input/TextButton.vue"
import Tooltip from "@/components/Tooltip.vue"
import KitchensinkPage from "@/kitchensink/components/KitchensinkPage.vue"
</script>

<template>
  <KitchensinkPage name="Tooltip">
    <div class="flex gap-20">
      <Tooltip text="I'm a tooltip">
        <TextButton button-type="primary" label="Hover me" />
      </Tooltip>

      <Tooltip shortcut="Key1 + Key2" text="I'm a tooltip with shortcut">
        <TextButton button-type="primary" label="Hover me, I have a shortcut" />
      </Tooltip>

      <Tooltip
        text="Tooltips shouldn’t really exceed one line, but sometimes it’s unavoidable"
      >
        <TextButton
          button-type="primary"
          label="Hover me, I have a long tooltip"
        />
      </Tooltip>
    </div>
  </KitchensinkPage>
</template>
