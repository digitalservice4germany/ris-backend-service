// See https://github.com/import-js/eslint-plugin-import
const moduleImportRules = {
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
}

module.exports = {
  root: true,
  env: {
    node: true,
    es6: true,
  },
  parser: "@typescript-eslint/parser",
  parserOptions: {
    ecmaVersion: 2022,
  },
  ignorePatterns: ["dist/", "Dockerfile.prod", "playwright.config.ts"],
  overrides: [
    // Avoid linting JavaScript config files with TypeScript rules...
    {
      files: ["**/*.ts"],
      extends: [
        "plugin:import/recommended",
        "plugin:import/typescript",
        "plugin:@typescript-eslint/recommended",
      ],
      rules: {
        ...moduleImportRules,
        "@typescript-eslint/no-floating-promises": ["error"],
        "@typescript-eslint/no-unused-vars": [
          "error",
          { ignoreRestSiblings: true },
        ],
      },
      parserOptions: {
        project: ["./tsconfig.json"],
      },
    },
    {
      files: ["**/*.vue"],
      parser: "vue-eslint-parser",
      parserOptions: {
        parser: "@typescript-eslint/parser",
      },
      extends: [
        "plugin:import/recommended",
        "plugin:import/typescript",
        "plugin:vue/vue3-recommended",
        "plugin:vuejs-accessibility/recommended",
        "plugin:vue-scoped-css/vue3-recommended",
        "@vue/typescript/recommended",
        "@vue/prettier",
        "@vue/eslint-config-prettier",
      ],
      rules: {
        ...moduleImportRules,
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
    }, // ...and avoid linting TypeScript files with ES rules for JavaScript config files!
    {
      files: ["**/*.js"],
      extends: ["eslint:recommended", "plugin:import/recommended"],
      rules: { ...moduleImportRules },
    },
    {
      files: ["**/test/components/**/*.ts"],
      extends: ["plugin:testing-library/vue"],
      rules: { ...moduleImportRules },
    },
    {
      files: ["test/**/*.ts"],
      extends: ["plugin:jest-dom/recommended"],
    },
    {
      files: ["**/e2e/**/*.ts", "**/a11y/**/*.ts"],
      extends: ["plugin:playwright/playwright-test"],
      rules: {
        ...moduleImportRules,
        "playwright/prefer-to-contain": 2,
        "playwright/prefer-to-have-count": 2,
        "playwright/prefer-comparison-matcher": 2,
        "playwright/prefer-equality-matcher": 2,
        "playwright/prefer-to-have-length": 2,
      },
    },
  ],
}
