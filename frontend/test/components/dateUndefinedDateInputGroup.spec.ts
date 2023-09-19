import { userEvent } from "@testing-library/user-event"
import { render, screen } from "@testing-library/vue"
import { createPinia, setActivePinia } from "pinia"
import DateUndefinedDateInputGroup from "@/components/DateUndefinedDateInputGroup.vue"
import { MetadatumType } from "@/domain/norm"

function renderComponent(overrides = {}) {
  // eslint-disable-next-line testing-library/await-async-events
  const user = userEvent.setup()
  const props = {
    dateId: "1",
    dateInputFieldLabel: "dateInputFieldLabel",
    dateInputAriaLabel: "dateInputAriaLabel",
    undefinedDateId: "3",
    undefinedDateInputFieldLabel: "undefinedDateInputFieldLabel",
    undefinedDateDropdownAriaLabel: "undefinedDateDropdownAriaLabel",
    selectedInputType: MetadatumType.DATE,
    dateValue: "01.01.2022",
    undefinedDateStateValue: "undefinedDateStateValue",
    ...overrides,
  }
  const utils = render(DateUndefinedDateInputGroup, { props })
  return { user, props, ...utils }
}

describe("Date or UndefinedDate InputGroup Component", () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
  })

  it('renders DateInput when selectedInputType is "date" and the other input is not in the document', async () => {
    renderComponent()

    const dateInput = screen.getByLabelText("dateInputAriaLabel")
    expect(dateInput).toHaveAttribute("id", "1")
    expect(dateInput).toBeInTheDocument()

    const undefinedDateDropdown = screen.queryByLabelText(
      "undefinedDateDropdownAriaLabel",
    )
    expect(undefinedDateDropdown).not.toBeInTheDocument()
  })

  it('renders DropdownInput when selectedInputType is "undefined_date" and the other input is not in the document', () => {
    renderComponent({
      selectedInputType: MetadatumType.UNDEFINED_DATE,
    })

    const undefinedDateDropdown = screen.getByLabelText(
      "undefinedDateDropdownAriaLabel",
    )
    expect(undefinedDateDropdown).toHaveAttribute("id", "3")
    expect(undefinedDateDropdown).toBeInTheDocument()

    const dateInput = screen.queryByLabelText("dateInputAriaLabel")
    expect(dateInput).not.toBeInTheDocument()
  })
})
