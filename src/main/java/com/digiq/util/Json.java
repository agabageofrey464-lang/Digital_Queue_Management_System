package com.digiq.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small helpers for the JSON endpoints and the WebSocket payloads. */
public final class Json {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    private Json() {
    }

    public static String stringify(Object value) {
        return GSON.toJson(value);
    }

    /** Builds a mutable, insertion-ordered map - the usual shape of an event payload. */
    public static Map<String, Object> map(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Json.map() needs an even number of arguments");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
        }
        return map;
    }

    public static void write(HttpServletResponse response, Object payload) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(stringify(payload));
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        write(response, map("ok", false, "error", message));
    }
}
