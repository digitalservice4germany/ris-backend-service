<script lang="ts" setup>
import { sanitizeUrl } from "@braintree/sanitize-url"
import type { Component } from "vue"
import { computed, h } from "vue"

interface Props {
  label?: string
  icon?: Component
  ariaLabel?: string
  buttonType?: string
  disabled?: boolean
  href?: string
  download?: boolean | string
  size?: "large" | "medium" | "small"
  target?: "_self" | "_blank" | "_parent" | "_top"
  iconPosition?: "left" | "right"
}

const props = withDefaults(defineProps<Props>(), {
  label: undefined,
  icon: undefined,
  ariaLabel: undefined,
  buttonType: "primary",
  disabled: false,
  href: undefined,
  download: undefined,
  size: "medium",
  target: undefined,
  iconPosition: "left",
})

const buttonClasses = computed(() => ({
  "ds-button-large": !props.size || props.size === "large",
  "ds-button-medium": props.size === "medium",
  "ds-button-small": props.size === "small",
  "ds-button-primary": props.buttonType == "primary",
  "ds-button-secondary": props.buttonType == "secondary",
  "ds-button-ghost hover:bg-blue-200 hover:shadow-none focus:shadow-none focus:bg-blue-200":
    props.buttonType == "ghost",
  "ds-button-tertiary": props.buttonType == "tertiary",
  "shadow-button shadow-red-900 text-red-900 bg-transparent hover:bg-red-200 focus:bg-red-200 active:bg-red-200 active:border-none active:text-red-900 disabled:text-gray-600 disabled:shadow-gray-600 disabled:bg-transparent":
    props.buttonType == "destructive",
  "ds-button-with-icon": props.icon,
  "ds-button-with-icon-only": props.icon && !props.label,
  "is-disabled": props.href && props.disabled,
  "pl-16":
    props.icon &&
    props.label &&
    props.buttonType !== "ghost" &&
    props.iconPosition === "left",
  "pr-16":
    props.icon &&
    props.label &&
    props.buttonType !== "ghost" &&
    props.iconPosition === "right",
}))

const isLink = computed(() => !!props.href)
const sanitizedUrl = computed(() => sanitizeUrl(props.href))

const renderIcon = () =>
  props.icon ? h(props.icon, { class: "ds-button-icon" }) : undefined

const renderLabel = () =>
  props.label ? h("span", { class: "ds-button-label" }, props.label) : undefined

const render = () => {
  const { disabled, href, download, target } = props
  let tag = "button"
  if (isLink.value) {
    tag = disabled ? "div" : "a"
  }

  return h(
    tag,
    {
      class: ["ds-button", buttonClasses.value],
      ariaLabel: props.ariaLabel,
      disabled: !href && disabled,
      href: sanitizedUrl.value,
      download,
      target,
    },
    [
      props.iconPosition === "right"
        ? [renderLabel(), renderIcon()]
        : [renderIcon(), renderLabel()],
    ],
  )
}
</script>

<template>
  <div class="w-max" data-testid>
    <render />
  </div>
</template>

<style lang="scss" scoped>
// The ignoring of the rule is necessary because we use manual render functions
// instead of a rich template.
// eslint-disable vue-scoped-css/no-unused-selector
.ds-button-large > .ds-button-icon {
  font-size: 2rem;
}

.ds-button.ds-button-ghost {
  padding-right: 12px;
  padding-left: 12px;
}
</style>
