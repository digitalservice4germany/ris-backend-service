const validateBorderNumberLinks = (
  divElem: HTMLDivElement,
  validBorderNumbers: string[],
) => {
  const linkTags = Array.from(
    divElem.getElementsByTagName("border-number-link"),
  )

  linkTags.forEach((linkTag) => {
    const borderNumber = linkTag.getAttribute("nr")
    const hasBorderNumber = borderNumber
      ? validBorderNumbers.includes(borderNumber)
      : false
    linkTag.setAttribute("valid", hasBorderNumber.toString())
  })
  return divElem
}

export function useValidBorderNumberLinks(
  text: string,
  validBorderNumbers: string[],
) {
  const divElem = document.createElement("div")
  divElem.innerHTML = text
  return validateBorderNumberLinks(divElem, validBorderNumbers).innerHTML
}
