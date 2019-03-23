package nl.jpelgrm.movienotifier.util;

import android.util.Patterns;

import java.util.regex.Pattern;

public class UserValidation {
    private static Pattern validName = Pattern.compile("^[a-z]{4}?[a-z0-9]{0,12}$");

    public static boolean validateName(String name) {
        return validName.matcher(name).matches();
    }

    public static boolean validateEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean validatePassword(String password) {
        return password.length() >= 8;
    }
}
