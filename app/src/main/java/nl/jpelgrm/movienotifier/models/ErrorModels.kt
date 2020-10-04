package nl.jpelgrm.movienotifier.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Errors(val errors: List<String>? = null)

@JsonClass(generateAdapter = true)
data class Message(val message: String? = null)