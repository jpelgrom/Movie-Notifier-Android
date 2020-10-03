package nl.jpelgrm.movienotifier.util

import android.content.Context
import com.google.gson.GsonBuilder
import nl.jpelgrm.movienotifier.R
import nl.jpelgrm.movienotifier.models.Errors
import nl.jpelgrm.movienotifier.models.Message
import retrofit2.Response
import java.io.IOException

object ErrorUtil {
    @JvmStatic
    fun getErrorMessage(context: Context, response: Response<*>?, login401: Boolean): String {
        return if (response != null) {
            val gson = GsonBuilder().create()
            val errorBuilder = StringBuilder()
            if (login401 && response.code() == 401) {
                context.getString(R.string.error_login_401)
            } else if (response.code() == 400) {
                if (response.errorBody() != null) {
                    try {
                        val errors = gson.fromJson(response.errorBody()!!.string(), Errors::class.java)
                        errors.errors?.forEach {
                            if (errorBuilder.toString() != "") {
                                errorBuilder.append("\n")
                            }
                            errorBuilder.append(it)
                        }
                    } catch (e: IOException) {
                        errorBuilder.append(context.getString(R.string.error_general_server, "I400"))
                    }
                } else {
                    errorBuilder.append(context.getString(R.string.error_general_server, "N400"))
                }
                errorBuilder.toString()
            } else if (response.code() == 500) {
                if (response.errorBody() != null) {
                    try {
                        val message = gson.fromJson(response.errorBody()!!.string(), Message::class.java)
                        errorBuilder.append(message.message)
                    } catch (e: IOException) {
                        errorBuilder.append("I500")
                    }
                } else {
                    errorBuilder.append("N500")
                }
                context.getString(R.string.error_general_message, errorBuilder.toString())
            } else {
                context.getString(R.string.error_general_server, "H" + response.code())
            }
        } else {
            context.getString(R.string.error_general_exception)
        }
    }

    @JvmStatic
    fun getErrorMessage(context: Context, response: Response<*>?): String {
        return getErrorMessage(context, response, false)
    }
}