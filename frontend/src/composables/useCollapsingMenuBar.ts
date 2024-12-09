import { computed, Ref } from "vue"
import { EditorButton } from "@/components/input/TextEditorButton.vue"

function setIsLast(buttonList: EditorButton[]) {
  for (let i = 1; i < buttonList.length; i++) {
    if (buttonList[i].group !== buttonList[i - 1].group) {
      buttonList[i - 1].isLast = true
    }
  }
}

export function useCollapsingMenuBar(
  buttons: Ref<EditorButton[]>,
  maxBarEntries: Ref<number>,
) {
  const collapsedButtons = computed(() => {
    let buttonList = new Array(...buttons.value)
    while (buttonList.length > maxBarEntries.value) {
      const collapsableButtonsWithGroup = buttonList.filter(
        (button) => button.group != undefined && button.isCollapsable === true,
      )

      if (collapsableButtonsWithGroup.length == 0) {
        setIsLast(buttonList)
        return buttonList
      }

      const lastGroupName =
        collapsableButtonsWithGroup[collapsableButtonsWithGroup.length - 1]
          .group

      const buttonsOfGroup = buttonList.filter(
        (button) => button.group == lastGroupName,
      )

      const menuButtonIndex = buttonList.indexOf(buttonsOfGroup[0])

      buttonList = buttonList.filter(
        (button) => !buttonsOfGroup.includes(button),
      )
      const activeButton = buttonsOfGroup.find((button) => button.isActive)
      const menuButton = {
        type: "menu",
        icon: activeButton ? activeButton.icon : buttonsOfGroup[0].icon,
        ariaLabel: buttonsOfGroup[0].group || "menu",
        group: buttonsOfGroup[0].group,
        isCollapsable: false,
        childButtons: buttonsOfGroup,
      }

      buttonList.splice(menuButtonIndex, 0, menuButton)
    }
    setIsLast(buttonList)
    return buttonList
  })
  return { collapsedButtons }
}
