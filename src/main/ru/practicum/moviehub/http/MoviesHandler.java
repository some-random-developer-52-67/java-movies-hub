package ru.practicum.moviehub.http;

import com.google.gson.stream.MalformedJsonException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieValidator;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.List;
import java.util.Optional;

final class MoviesHandler extends BaseHttpHandler {

    static final String MOVIES_PATH = "/movies";
    private static final String VALIDATION_ERROR = "Ошибка валидации";
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

            handleMovies(exchange);
        }
    }

    private void handleMovies(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (HttpMethod.GET.name().equals(method)) {
            sendJson(exchange, 200, store.getAll());
        } else if (HttpMethod.POST.name().equals(method)) {
            createMovie(exchange);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void createMovie(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst(CONTENT_TYPE_HEADER);
        if (!JsonMediaType.supports(contentType)) {
            sendError(exchange, 415, "Ожидается Content-Type application/json с кодировкой UTF-8");
            return;
        }

        Optional<CreateMovieRequest> body;
        try {
            body = JsonRequestReader.readObject(exchange.getRequestBody(), CreateMovieRequest.class);
        } catch (EOFException | MalformedJsonException | CharacterCodingException exception) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        if (body.isEmpty()) {
            sendJson(exchange, 422, new ErrorResponse(VALIDATION_ERROR, List.of("Тело запроса должно быть объектом")));
            return;
        }

        CreateMovieRequest request = body.get();
        String title = request.readTitle();
        Integer year = request.readYear();

        List<String> details = MovieValidator.validate(title, year);
        if (!details.isEmpty()) {
            sendJson(exchange, 422, new ErrorResponse(VALIDATION_ERROR, details));
            return;
        }

        Movie movie = store.add(title, year);

        exchange.getResponseHeaders().set(LOCATION_HEADER, MOVIES_PATH + "/" + movie.id());
        sendJson(exchange, 201, movie);
    }

}
