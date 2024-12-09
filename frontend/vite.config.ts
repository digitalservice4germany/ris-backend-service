import path from "path"
import * as process from "process"
import { sentryVitePlugin } from "@sentry/vite-plugin"
import vue from "@vitejs/plugin-vue"
import Icons from "unplugin-icons/vite"
import { defineConfig as defineViteConfig, mergeConfig } from "vite"
import EnvironmentPlugin from "vite-plugin-environment"
import Pages from "vite-plugin-pages"
import vueDevTools from "vite-plugin-vue-devtools"
import { defineConfig as defineVitestConfig } from "vitest/config"

// https://vitejs.dev/config/
const viteConfig = defineViteConfig({
  build: {
    sourcemap: true, // Source map generation must be turned on
  },
  esbuild: {
    supported: {
      "top-level-await": true,
    },
  },
  server: {
    port: 3000, // Required for vite to be accessible when running as part of docker compose setup
  },
  preview: {
    port: 3000,
  },
  plugins: [
    vue(),
    Pages({
      dirs: "src/routes",
      // We disable lazy loading as it leads to problems when an old user sessions requests resources that were
      // removed with a new deployment. see https://stackoverflow.com/q/69300341/4694994
      importMode: "sync",
    }),
    EnvironmentPlugin({
      BACKEND_HOST: "",
    }),
    sentryVitePlugin({
      authToken: process.env.SENTRY_AUTH_TOKEN,
      org: "digitalservice",
      project: "ris-frontend",
      telemetry: process.env.VITEST !== "true",
    }),
    Icons({
      scale: 1.3333, // ~24px at the current default font size of 18px
    }),
    vueDevTools({ launchEditor: "idea" }),
  ],

  define: {
    "process.env": {},
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
      "~": path.resolve(__dirname, "test"),
    },
  },
})

const vitestConfig = defineVitestConfig({
  test: {
    setupFiles: ["test/setup.ts"],
    globals: true,
    environment: "jsdom",
    include: ["test/**/*.ts"],
    exclude: [
      "test/e2e/**/*.ts",
      "test/a11y/**/*.ts",
      "test/queries/**/*.ts",
      "test/test-helper/**/*.ts",
      "test/setup.ts",
    ],
    coverage: {
      reporter: ["lcov"],
      exclude: [
        // Configuration and generated outputs
        "**/[.]**",
        "coverage/**/*",
        "dist/**/*",
        "**/.*rc.{?(c|m)js,yml}",
        "*.config.{js,ts}",

        // Types
        "**/*.d.ts",

        // Tests
        "test/**/*",

        // App content we're not interested in covering with unit tests
        "src/routes/**/*",
        "src/kitchensink/**/*",
        "src/App.vue",
        "src/main.ts",
      ],
    },
  },
})

export default mergeConfig(viteConfig, vitestConfig)
