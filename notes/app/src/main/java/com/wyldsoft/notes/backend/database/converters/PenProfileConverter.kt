package com.wyldsoft.notes.backend.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.wyldsoft.notes.backend.database.entities.StoredPenProfile

class PenProfileConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromPenProfile(profile: StoredPenProfile?): String? {
        return if (profile == null) null else gson.toJson(profile)
    }

    @TypeConverter
    fun toPenProfile(data: String?): StoredPenProfile? {
        return if (data == null) null else gson.fromJson(data, StoredPenProfile::class.java)
    }
}