package ai.promptscan.sdk.graphql

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import java.util.UUID
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

val UUIDAdapter = object : Adapter<UUID> {
    override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: UUID) {
        writer.value(value.toString())
    }

    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): UUID {
        return UUID.fromString(reader.nextString())
    }
}

val DateTimeAdapter = object : Adapter<OffsetDateTime> {
    override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: OffsetDateTime) {
        writer.value(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): OffsetDateTime {
        return OffsetDateTime.parse(reader.nextString()!!)
    }
}