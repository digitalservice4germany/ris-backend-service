package utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import de.bund.digitalservice.ris.norms.domain.entity.FileReference
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller.EditNormFrameController
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller.LoadNormController
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.encodeLocalDate
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.encodeLocalDateTime
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime

fun convertEditNormRequestSchemaToJson(
    editNormRequestSchema: EditNormFrameController.NormFramePropertiesRequestSchema,
): String {
    return Gson().toJson(editNormRequestSchema)
}

fun convertNormToJson(norm: LoadNormController.NormResponseSchema): String {
    class LocalDateSerializer : JsonSerializer<LocalDate> {
        override fun serialize(
            src: LocalDate?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?,
        ): JsonElement {
            return JsonPrimitive(encodeLocalDate(src))
        }
    }
    val gson: Gson =
        GsonBuilder().registerTypeAdapter(LocalDate::class.java, LocalDateSerializer()).serializeNulls().create()
    return gson.toJson(norm)
}

fun convertFileReferenceToJson(fileReference: FileReference): String {
    class LocalDateTimeSerializer : JsonSerializer<LocalDateTime> {
        override fun serialize(
            src: LocalDateTime,
            typeOfSrc: Type?,
            context: JsonSerializationContext?,
        ): JsonElement {
            return JsonPrimitive(encodeLocalDateTime(src))
        }
    }
    val gson: Gson =
        GsonBuilder().registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer()).create()
    return gson.toJson(fileReference)
}
