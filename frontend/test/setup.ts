import "@testing-library/jest-dom"
import failOnConsole from "jest-fail-on-console"
import { useFeatureToggleServiceMock } from "./test-helper/useFeatureToggleServiceMock"
import { useResizeObserverMock } from "./test-helper/useResizeObserverMock"

failOnConsole({
  shouldFailOnAssert: true,
  shouldFailOnDebug: true,
  shouldFailOnError: true,
  shouldFailOnInfo: true,
  shouldFailOnLog: true,
  shouldFailOnWarn: true,
})

useResizeObserverMock()

useFeatureToggleServiceMock()
