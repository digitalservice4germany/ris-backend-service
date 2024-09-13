import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { expect } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import DocumentUnitSearchEntryForm from "@/components/DocumentUnitSearchEntryForm.vue"

async function renderComponent(options?: { isLoading: boolean }) {
  const props = {
    isLoading: options?.isLoading ? options?.isLoading : false,
  }

  const router = createRouter({
    history: createWebHistory(),
    routes: [
      {
        path: "/",
        name: "home",
        component: {},
      },
    ],
  })

  return {
    ...render(DocumentUnitSearchEntryForm, {
      props,
      global: { plugins: [router] },
    }),
    user: userEvent.setup(),
    router: router,
  }
}

describe("Documentunit search form", () => {
  test("renders correctly", async () => {
    await renderComponent()
    ;[
      "Aktenzeichen Suche",
      "Gerichtstyp Suche",
      "Gerichtsort Suche",
      "Entscheidungsdatum Suche",
      "Dokumentnummer Suche",
      "Status Suche",
      "Nur meine Dokstelle Filter",
    ].forEach((label) => expect(screen.getByLabelText(label)).toBeVisible())
  })

  test("click on 'Suche zurücksetzen' emits 'resetSearchResults'", async () => {
    const { emitted, user } = await renderComponent()
    await user.type(
      screen.getByLabelText("Entscheidungsdatum Suche"),
      "22.02.2001",
    )
    await user.click(screen.getByLabelText("Suche zurücksetzen"))
    expect(emitted().resetSearchResults).toBeTruthy()
  })

  test("click on 'Ergebnisse anzeigen' emits search event", async () => {
    const { emitted, user } = await renderComponent()

    await user.type(
      screen.getByLabelText("Entscheidungsdatum Suche"),
      "22.02.2001",
    )
    await user.click(
      screen.getByLabelText("Nach Dokumentationseinheiten suchen"),
    )

    expect(emitted().search).toBeTruthy()
  })

  test("click on 'Ergebnisse anzeigen' with the same search entry, emits search event again", async () => {
    const { emitted, user } = await renderComponent()

    await user.type(
      screen.getByLabelText("Entscheidungsdatum Suche"),
      "23.03.2003",
    )
    await user.click(
      screen.getByLabelText("Nach Dokumentationseinheiten suchen"),
    )
    expect(emitted().search.length).toBe(1)

    await user.click(
      screen.getByLabelText("Nach Dokumentationseinheiten suchen"),
    )

    expect(emitted().search.length).toBe(2)
  })

  test("click on 'Nur meine Dokstelle' renders 'Nur fehlerhafte Dokumentationseinheiten' checkbox", async () => {
    const { user } = await renderComponent()
    expect(
      screen.queryByLabelText("Nur fehlerhafte Dokumentationseinheiten"),
    ).not.toBeInTheDocument()
    expect(screen.getByLabelText("Nur meine Dokstelle Filter")).toBeVisible()
    await user.click(screen.getByLabelText("Nur meine Dokstelle Filter"))
    expect(
      screen.getByLabelText("Nur fehlerhafte Dokumentationseinheiten"),
    ).toBeVisible()
  })

  test("click on 'Nur meine Dokstelle' renders 'Nur fehlerhafte Dokumentationseinheiten' checkbox", async () => {
    const { user } = await renderComponent()
    expect(
      screen.queryByLabelText("Nur fehlerhafte Dokumentationseinheiten"),
    ).not.toBeInTheDocument()
    expect(screen.getByLabelText("Nur meine Dokstelle Filter")).toBeVisible()
    await user.click(screen.getByLabelText("Nur meine Dokstelle Filter"))
    expect(
      screen.getByLabelText("Nur fehlerhafte Dokumentationseinheiten"),
    ).toBeVisible()
  })

  test("search by pressing ctrl & enter", async () => {
    const { emitted, router, user } = await renderComponent()
    await router.push("/") // reset for whatever reason

    await user.type(
      screen.getByLabelText("Entscheidungsdatum Suche"),
      "22.02.2002",
    )

    await user.keyboard("{Control>}{Enter}")

    expect(emitted("search")).toBeTruthy()
  })
})
