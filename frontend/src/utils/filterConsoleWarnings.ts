export function filterConsoleWarnings() {
  const originalConsoleWarn = console.warn
  console.warn = (message, ...args) => {
    if (
      typeof message === "string" &&
      message.startsWith(
        "TextSelection endpoint not pointing into a node with inline content",
      )
    ) {
      /**
       * `TextSelection` is used in {@link @/editor/commands/handleSelection.ts} for a range
       * that spans both inline and block content. However, ProseMirror’s `TextSelection` only
       * natively supports inline content and issues a warning when block content is included.
       *
       * The complexity required to fully resolve this as a mixed-content selection isn't
       * justified for this case.
       *
       * Therefore, we intentionally ignore this warning.
       */
      return
    }
    originalConsoleWarn(message, args)
  }
}
