import { fileURLToPath, URL } from "node:url"
import { includeIgnoreFile } from "@eslint/compat"
import js from "@eslint/js"
import prettierConfig from "@vue/eslint-config-prettier"
import vueTsEslintConfig from "@vue/eslint-config-typescript"
import importPlugin from "eslint-plugin-import"
import playwrightPlugin from "eslint-plugin-playwright"
import vuePlugin from "eslint-plugin-vue"
import vueScopedCssPlugin from "eslint-plugin-vue-scoped-css"
import vueA11yPlugin from "eslint-plugin-vuejs-accessibility"
import globals from "globals"
import {
  config as defineConfig,
  configs as tsEslintConfigs,
} from "typescript-eslint"
import vueEslintParser from "vue-eslint-parser"
import testingLibraryPlugin from "eslint-plugin-testing-library"
import jestDomPlugin from "eslint-plugin-jest-dom"

export default defineConfig(
  // Files
  {
    ignores: [
      "dist/**",
      "Dockerfile.prod",
      "playwright.config.ts",
      "tailwind.config.js",
      "prettier.config.js",
      "postcss.config.js",
      "eslint.config.js",
      "**/node_modules/**",
    ],
  },

  includeIgnoreFile(fileURLToPath(new URL(".gitignore", import.meta.url))),
  {
    files: ["**/*.ts", "**/*.js", "**/*.vue"],
  },

  // Basic rules
  js.configs.recommended,
  ...tsEslintConfigs.recommended,
  importPlugin.flatConfigs.recommended,
  importPlugin.flatConfigs.typescript,
  ...vuePlugin.configs["flat/recommended"],
  ...vueA11yPlugin.configs["flat/recommended"],
  ...vueTsEslintConfig(),

  {
    files: ["**/*.js"],
    extends: [tsEslintConfigs.disableTypeChecked],
  },

  {
    rules: {
      "import/exports-last": 2,
      "import/first": 2,
      "import/newline-after-import": 2,
      "import/no-duplicates": 2,
      "import/order": [
        "error",
        {
          alphabetize: {
            order: "asc",
            caseInsensitive: true,
          },
        },
      ],
      "import/no-unresolved": 0,
      "@typescript-eslint/array-type": "error",
      /*
       * This rules restricts our flexibility to write hierarchically separated
       * components with labels and inputs as it sometimes "can't see" the relation.
       * We decided to rather depend on the end-to-end accessibility tests which test
       * this relation too, but are code independent.
       */
      "vuejs-accessibility/label-has-for": "off",
      "@typescript-eslint/no-floating-promises": ["error"],
      "@typescript-eslint/no-unused-vars": [
        "error",
        { ignoreRestSiblings: true },
      ],
    },
  },

  {
    languageOptions: {
      globals: { ...globals.node },
      parser: vueEslintParser,
      ecmaVersion: 2022,
      sourceType: "module",
      parserOptions: {
        project: true,
        parser: "@typescript-eslint/parser",
        tsconfigRootDir: import.meta.dirname,
      },
    },

    settings: {
      "import/resolver": { node: true, typescript: true },
    },
  },

  {
    files: ["src/**/*.vue"],
    plugins: {
      "vue-scoped-css": vueScopedCssPlugin,
    },
    rules: {
      "vue/no-static-inline-styles": "error",
      "vue/component-tags-order": [
        "error",
        {
          order: ["script", "template", "style"],
        },
      ],
      "vue/component-name-in-template-casing": [
        "error",
        "PascalCase",
        { registeredComponentsOnly: true },
      ],
      "vue/attributes-order": ["error", { alphabetical: true }],
      "vue/component-api-style": ["error", ["script-setup"]],
      "vue/define-props-declaration": "error",
      "vue/define-emits-declaration": "error",
      "vue/define-macros-order": "error",
      "vue/no-boolean-default": ["error", "default-false"],
      "vue/prefer-true-attribute-shorthand": "error",
      "vue/no-ref-object-destructure": "error",
      "vue/no-restricted-call-after-await": "error",
      "vue/no-undef-properties": "error",
      "vue/no-unused-refs": "error",
      "vue/no-useless-v-bind": "error",
      "vue/prefer-separate-static-class": "error",
      "vue/v-for-delimiter-style": "error",
      "vue/v-on-function-call": "error",
      "vue-scoped-css/enforce-style-type": [
        "warn",
        { allows: ["module", "scoped"] },
      ],
      "@typescript-eslint/no-unused-vars": [
        "error",
        { ignoreRestSiblings: true },
      ],
    },
  },

  {
    files: ["src/routes/**/*.vue"],
    rules: {
      "vue/multi-word-component-names": "off",
    },
  },

  {
    files: ["test/components/**/*.ts"],
    ...testingLibraryPlugin.configs["flat/vue"],
  },

  {
    files: ["test/**/*.ts"],
    ...jestDomPlugin.configs["flat/recommended"],
  },

  // Additional rules for E2E tests
  {
    ...playwrightPlugin.configs["flat/recommended"],
    files: ["test/{e2e,a11y}/**/*.ts"],
    rules: {
      ...playwrightPlugin.configs["flat/recommended"].rules,
      "playwright/prefer-to-contain": 2,
      "playwright/prefer-to-have-count": 2,
      "playwright/prefer-comparison-matcher": 2,
      "playwright/prefer-equality-matcher": 2,
      "playwright/prefer-to-have-length": 2,
    },
  },

  prettierConfig,
)
