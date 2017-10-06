package nl.jpelgrm.movienotifier.util;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.models.error.Errors;
import nl.jpelgrm.movienotifier.models.error.Message;
import retrofit2.Response;

public class ErrorUtil {
    public static String getErrorMessage(Context context, Response response, boolean login401) {
        if(response != null) {
            Gson gson = new GsonBuilder().create();
            StringBuilder errorBuilder = new StringBuilder();

            if(login401 && response.code() == 401) {
                return context.getString(R.string.error_login_401);
            } else if(response.code() == 400) {
                if(response.errorBody() != null) {
                    try {
                        Errors errors = gson.fromJson(response.errorBody().string(), Errors.class);
                        for(String errorString : errors.getErrors()) {
                            if(!errorBuilder.toString().equals("")) {
                                errorBuilder.append("\n");
                            }
                            errorBuilder.append(errorString);
                        }
                    } catch(IOException e) {
                        errorBuilder.append(context.getString(R.string.error_general_server, "I400"));
                    }
                } else {
                    errorBuilder.append(context.getString(R.string.error_general_server, "N400"));
                }

                return errorBuilder.toString();
            } else if(response.code() == 500) {
                if(response.errorBody() != null) {
                    try {
                        Message message = gson.fromJson(response.errorBody().string(), Message.class);
                        errorBuilder.append(message.getMessage());
                    } catch(IOException e) {
                        errorBuilder.append("I500");
                    }
                } else {
                    errorBuilder.append("N500");
                }

                return context.getString(R.string.error_general_message, errorBuilder.toString());
            } else {
                return context.getString(R.string.error_general_server, "H" + response.code());
            }
        } else {
            return context.getString(R.string.error_general_exception);
        }
    }

    public static String getErrorMessage(Context context, Response response) {
        return getErrorMessage(context, response, false);
    }
}
