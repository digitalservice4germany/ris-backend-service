export default class TextEditorUtil {
  /**
   * Checks if the provided HTML string contains meaningful content.
   *
   * @param value - The HTML string or undefined.
   * @returns The string if it contains text, images, or tables; otherwise undefined.
   *
   * 1. Example: "<p>&nbsp;</p>" will return undefined.
   * 2. Example: "<p></p><p></p><p></p>" will return undefined.
   * 3. Example: "<p>            </p>" will return undefined.
   */
  public static getEditorContentIfPresent(
    value: string | undefined,
  ): string | undefined {
    if (value == undefined || value.trim().length == 0) return undefined
    const divElem = document.createElement("div")
    divElem.innerHTML = value
    const hasImgElem = divElem.getElementsByTagName("img").length > 0
    const hasTable = divElem.getElementsByTagName("table").length > 0
    const hasInnerText = divElem.innerText.trim().length > 0
    return hasInnerText || hasImgElem || hasTable ? value : undefined
  }
}
