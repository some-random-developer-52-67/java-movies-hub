package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

final class MoviesHandler extends BaseHttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if ("GET".equals(exchange.getRequestMethod()) && "/movies".equals(exchange.getRequestURI().getPath())) {
                sendJson(exchange, 200, List.of());
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }

}
