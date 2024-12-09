import { AxeBuilder } from "@axe-core/playwright"
import { Page } from "@playwright/test"

export const useAxeBuilder = (page: Page) =>
  // dev tool is only enabled locally, can be ignored
  new AxeBuilder({ page }).exclude(".vue-devtools__panel")
