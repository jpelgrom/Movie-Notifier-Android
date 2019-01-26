package nl.jpelgrm.movienotifier.util;

import java.util.Random;

// https://github.com/SijmenHuizenga/Movie-Notifier/blob/efa7183e847e95c2b7826a016012475b0039d96f/src/main/java/it/sijmen/movienotifier/util/ApiKeyHelper.java
public class ApiKeyHelper {

    private final char[] symbols;

    private final Random random;

    public ApiKeyHelper() {
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch)
            tmp.append(ch);
        for (char ch = 'a'; ch <= 'z'; ++ch)
            tmp.append(ch);
        for (char ch = 'A'; ch <= 'Z'; ++ch)
            tmp.append(ch);
        symbols = tmp.toString().toCharArray();

        random = new Random();
    }

    public String randomAPIKey() {
        char[] buf = new char[64];

        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }

}