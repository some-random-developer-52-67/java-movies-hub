package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final String CONTENT_TYPE_HEADER = "Content-Type";

    private static final Gson GSON = new Gson();

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        exchange.getResponseHeaders().set(CONTENT_TYPE_HEADER, JsonMediaType.UTF_8_CONTENT_TYPE);
        if (HttpMethod.HEAD.name().equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }

        byte[] bytes = GSON.toJson(body).getBytes(UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    protected void sendError(HttpExchange exchange, int status, String error) throws IOException {
        sendJson(exchange, status, new ErrorResponse(error));
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set(CONTENT_TYPE_HEADER, JsonMediaType.UTF_8_CONTENT_TYPE);
        exchange.sendResponseHeaders(204, -1);
    }

    protected void sendMethodNotAllowed(HttpExchange exchange, HttpMethod... allowedMethods) throws IOException {
        String allow = Arrays.stream(allowedMethods)
                .map(HttpMethod::name)
                .collect(Collectors.joining(", "));

        exchange.getResponseHeaders().set("Allow", allow);
        sendError(exchange, 405, "Метод не поддерживается");
    }

}
