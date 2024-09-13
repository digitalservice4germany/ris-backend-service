import dayjs from "dayjs"

export default class DateUtil {
  public static formatDate(
    date: string | Date | undefined,
  ): string | undefined {
    const parsedDate = dayjs(date, "YYYY-MM-DD", true)
    return parsedDate.isValid() ? parsedDate.format("DD.MM.YYYY") : undefined
  }
}
