const routes = [
  {
    path: "/caselaw/documentUnit/new",
    name: "new",
    component: {},
  },
  {
    path: "/",
    name: "home",
    component: {},
  },
  {
    path: "/caselaw/documentUnit/:documentNumber/categories",
    name: "caselaw-documentUnit-documentNumber-categories",
    component: {},
  },
  {
    path: "/caselaw/documentUnit/:documentNumber/preview",
    name: "caselaw-documentUnit-documentNumber-preview",
    component: {},
  },
  {
    path: "/caselaw/documentUnit/:documentNumber/files",
    name: "caselaw-documentUnit-documentNumber-files",
    component: {},
  },
  {
    path: "/caselaw/periodical-evaluation/:editionId",
    name: "caselaw-periodical-evaluation-editionId",
    component: {},
  },
  {
    path: "/caselaw/periodical-evaluation/:editionId/references",
    name: "caselaw-periodical-evaluation-editionId-references",
    component: {},
  },
  {
    path: "/caselaw/periodical-evaluation/:editionId/edition",
    name: "caselaw-periodical-evaluation-editionId-edition",
    component: {},
  },
  {
    path: "/caselaw/periodical-evaluation/:editionId/handover",
    name: "caselaw-periodical-evaluation-editionId-handover",
    component: {},
  },
]

export default routes
