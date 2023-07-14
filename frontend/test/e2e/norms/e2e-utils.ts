import fs from "fs"
import { tmpdir } from "os"
import path from "path"
import internal from "stream"
import { APIRequestContext, expect } from "@playwright/test"
import jsZip from "jszip"
import { Page } from "playwright"

const VERSION_TAG = "v0.15.0"
const REMOTE_JURIS_TEST_FILE_FOLDER_URL = `raw.githubusercontent.com/digitalservicebund/ris-norms-juris-converter/${VERSION_TAG}/src/test/resources/juris`

async function getLocalJurisTestFileFolderPath(): Promise<string> {
  const folderPath = path.join(
    tmpdir(),
    "ris-norms_juris-test-files",
    VERSION_TAG,
  )
  await fs.promises.mkdir(folderPath, { recursive: true })
  return folderPath
}

async function downloadJurisTestFile(
  request: APIRequestContext,
  fileName: string,
  localPath: string,
): Promise<void> {
  const username = process.env.GH_PACKAGES_REPOSITORY_USER
  const password = process.env.GH_PACKAGES_REPOSITORY_TOKEN
  const remoteUrl = `https://${username}:${password}@${REMOTE_JURIS_TEST_FILE_FOLDER_URL}/${fileName}`
  const response = await request.get(remoteUrl)

  if (!response.ok()) {
    console.error(`Download of the following Juris file failed: ${fileName}`, {
      status: response.status(),
      text: await response.text(),
    })
  }

  expect(response.ok()).toBeTruthy()

  const body = await response.body()
  await fs.promises.writeFile(localPath, body)
}

export async function loadJurisTestFile(
  request: APIRequestContext,
  fileName: string,
): Promise<{ filePath: string; fileContent: Buffer }> {
  const folderPath = await getLocalJurisTestFileFolderPath()
  const filePath = path.join(folderPath, fileName)

  if (!fs.existsSync(filePath)) {
    await downloadJurisTestFile(request, fileName, filePath)
  }

  const fileContent = await fs.promises.readFile(filePath)
  return { filePath, fileContent }
}

export async function importNormViaApi(
  request: APIRequestContext,
  fileContent: Buffer,
  fileName: string,
): Promise<{ guid: string }> {
  const response = await request.post(`/api/v1/norms`, {
    headers: { "Content-Type": "application/zip", "X-Filename": fileName },
    data: fileContent,
  })

  expect(response.ok()).toBeTruthy()

  const body = await response.text()
  return JSON.parse(body)
}

export const openNorm = async (
  page: Page,
  officialLongTitle: string,
  guid: string,
) => {
  await page.goto("/norms")
  const listEntry = page.locator(`a[href="/norms/norm/${guid}"]`)
  await expect(listEntry).toBeVisible()
  await expect(listEntry).toHaveText(officialLongTitle)
  await listEntry.click()
}

export async function getDownloadedFileContent(page: Page, filename: string) {
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.locator('a:has-text("Zip Datei speichern")').click(),
  ])

  expect(download.suggestedFilename()).toBe(filename)
  expect(
    (await fs.promises.stat((await download.path()) as string)).size,
  ).toBeGreaterThan(0)
  const readable = await download.createReadStream()
  const chunks = []
  for await (const chunk of readable as internal.Readable) {
    chunks.push(chunk)
  }

  return Buffer.concat(chunks)
}

export async function getMetaDataFileAsString(
  content: Buffer,
): Promise<string> {
  return jsZip.loadAsync(content).then(function (zip) {
    const metadataFileName = Object.keys(zip.files)
      .filter(
        (filename) => filename.endsWith(".xml") && !filename.includes("BJNE"),
      )
      .pop()
    return zip.files[metadataFileName as string]
      .async("string")
      .then((xmlContent) => xmlContent.replace(/ {2}|\r\n|\n|\r/gm, ""))
  })
}
