package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class BaseHttpHandler implements HttpHandler {

    private static final String JSON_UTF_8 = "application/json; charset=UTF-8";
    private static final Gson GSON = new Gson();

    protected <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(exchange.getRequestBody(), UTF_8.newDecoder()))) {
            T json = GSON.getAdapter(type).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new MalformedJsonException("Expected one JSON value");
            }

            return json;
        }
    }

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", JSON_UTF_8);

        byte[] bytes = GSON.toJson(body).getBytes(UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

}
