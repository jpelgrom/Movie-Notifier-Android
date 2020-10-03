package nl.jpelgrm.movienotifier.util

import android.util.Patterns
import java.util.regex.Pattern

object UserValidation {
    private val validName = Pattern.compile("^[a-z]{4}?[a-z0-9]{0,12}$")

    @JvmStatic
    fun validateName(name: String?): Boolean = validName.matcher(name ?: "").matches()

    @JvmStatic
    fun validateEmail(email: String?): Boolean = Patterns.EMAIL_ADDRESS.matcher(email ?: "").matches()

    @JvmStatic
    fun validatePassword(password: String): Boolean = password.length >= 8
}