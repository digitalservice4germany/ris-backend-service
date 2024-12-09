# Frontend

Built with Vue

## Prerequisites

The repository contains a `.node-version` file to set up the required Node.js version automatically
with [nodenv](https://github.com/nodenv/nodenv).

Install dependencies:

```bash
npm install
```

For E2E and a11y testing with [Playwright](https://playwright.dev/docs/intro) you will need to install the supported
browsers, if you don't have them already:

```bash
npx playwright install chrome firefox
```

## Icons

All icons in the [Google Material](https://icon-sets.iconify.design/ic), [Material Symbol](https://icon-sets.iconify.design/material-symbols)
and [Material Design Icons](https://icon-sets.iconify.design/mdi/) sets can be used.

To make the icon available in your code, select the icon, choose "Unplugin Icons" as component and copy-paste the import statement. Example:

```typescript
import VerifiedIcon from "~icons/material-symbols/verified-user"
```

## Development

### Dev server

The project uses [Vite](https://vitejs.dev/guide/) to provide a fast bundler-less [dev server](http://127.0.0.1/).

You have two options:

1. Run all services together:

```bash
./run.sh dev
```

2. Start the frontend dev server in isolation:

```bash
npm run dev
```

### Testing

The application has

- unit tests (using [Vitest](https://github.com/vitest-dev/vitest))
- end-to-end tests (using [Playwright](https://playwright.dev/docs/intro))
- accessibility tests (using [Axe](https://github.com/abhinaba-ghosh/axe-playwright#readme)
  and [Playwright](https://playwright.dev/docs/intro))
- performance tests for the backend search endpoints (using [Playwright](https://playwright.dev/docs/intro))

**To run the unit tests:**

```bash
npm test
```

**To run one unit test:**

```bash
npm test -- chipsDateInput.spec.ts
```

**Run unit tests with watcher:**

```bash
npm run test:unit:watch
```

**Gather coverage:**

```bash
npm run coverage
```

**To run the E2E tests:**

(Requires the application to run locally.)

```bash
# run all
npm run test:e2e

# run all from a specific file
npm run test:e2e filename.spec.ts

# run a specific test in a specific file
npx playwright test filename.spec.ts -g "test name"

# for a less cluttered terminal during dev, you can comment out a browser in playwright.config.ts
```

**To run the a11y tests:**

```bash
npm run test:a11y
```

**To run the performance tests:**

```bash
npm run test:queries -- --workers=1
```

**To run playwright tests against a different environment:**

Add the URL of the environment like this

```bash
E2E_BASE_URL='<ENV_URL>' npm run <your_test>
```

### Style (linting & formatting)

Check our [Frontend Styleguide](FRONTEND_STYLEGUIDE.md) document.

Linting is done via [ESLint](https://eslint.org/docs/user-guide/getting-started); consistent formatting for a variety of
source code files is being enforced using [Prettier](https://prettier.io/docs/en/index.html). ESLint and Prettier work
in conjunction.

**Check style:**

```bash
npm run style:check
```

**Autofix issues:**

```bash
npm run style:fix
```

(Some problems might not autofix.)

### Debugging

**Debug Frontend in IntelliJ with Chrome**

To debug frontend in IntelliJ IDEA, such as setting watchers and breakpoints:

1. **Run Chrome in Debug Mode:**

   Start your Chrome browser in remote debugging mode to allow IntelliJ IDEA to connect to it by:

   - Locate your Chromium-based browser:
     ```bash
     cd /Applications/Brave\ Browser.app/Contents/MacOS/
     ```
   - Then run it with debugging mode:
     ```bash
     ./Brave\ Browser --remote-debugging-port=9222
     ```

2. **Configure IntelliJ IDEA:**
   - Open IntelliJ IDEA and navigate to `Run` > `Edit Configurations`.
   - Click on the `+` button and select `Attach to Node.js/Chrome`.
   - Make sure you are setting the host to `localhost` and the port to `9222`.
   - Run your project in IntelliJ IDEA and select the frontend tab to debug.

More info can be found
in [Debug JavaScript in Chrome](https://www.jetbrains.com/help/idea/debugging-javascript-in-chrome.html)
