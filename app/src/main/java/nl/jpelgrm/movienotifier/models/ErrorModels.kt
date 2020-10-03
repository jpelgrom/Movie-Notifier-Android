package nl.jpelgrm.movienotifier.models

import com.google.gson.annotations.Expose

data class Errors(@Expose val errors: List<String>? = null)

data class Message(@Expose val message: String? = null)