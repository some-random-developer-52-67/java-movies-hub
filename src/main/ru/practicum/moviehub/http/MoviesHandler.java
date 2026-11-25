package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

final class MoviesHandler extends BaseHttpHandler {

    static final String MOVIES_PATH = "/movies";
    private static final String LOCATION_HEADER = "Location";

    private final MoviesStore store;

    MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!MOVIES_PATH.equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String method = exchange.getRequestMethod();
            if (HttpMethod.GET.name().equals(method)) {
                sendJson(exchange, 200, store.getAll());
            } else if (HttpMethod.POST.name().equals(method)) {
                createMovie(exchange);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }

    private void createMovie(HttpExchange exchange) throws IOException {
        CreateMovieRequest request = readJson(exchange, CreateMovieRequest.class);

        Movie movie = store.add(request.title(), request.year());

        exchange.getResponseHeaders().set(LOCATION_HEADER, MOVIES_PATH + "/" + movie.id());
        sendJson(exchange, 201, movie);
    }

}
