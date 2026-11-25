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
    private static final String MOVIE_PATH_PREFIX = MOVIES_PATH + "/";
    private static final String MOVIE_NOT_FOUND = "Фильм не найден";
    private static final String VALIDATION_ERROR = "Ошибка валидации";
    private static final String LOCATION_HEADER = "Location";

    private final MoviesStore store;

    MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if (MOVIES_PATH.equals(path)) {
                handleMovies(exchange);
            } else if (isMoviePath(path)) {
                handleMovie(exchange, path.substring(MOVIE_PATH_PREFIX.length()));
            } else {
                sendError(exchange, 404, "Маршрут не найден");
            }
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

    private void handleMovie(HttpExchange exchange, String pathId) throws IOException {
        long id;
        try {
            id = Long.parseLong(pathId);
        } catch (NumberFormatException exception) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        String method = exchange.getRequestMethod();
        if (HttpMethod.GET.name().equals(method)) {
            getMovie(exchange, id);
        } else if (HttpMethod.DELETE.name().equals(method)) {
            deleteMovie(exchange, id);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void getMovie(HttpExchange exchange, long id) throws IOException {
        Optional<Movie> movie = store.findById(id);
        if (movie.isPresent()) {
            sendJson(exchange, 200, movie.get());
        } else {
            sendError(exchange, 404, MOVIE_NOT_FOUND);
        }
    }

    private void deleteMovie(HttpExchange exchange, long id) throws IOException {
        if (store.deleteById(id)) {
            sendNoContent(exchange);
        } else {
            sendError(exchange, 404, MOVIE_NOT_FOUND);
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

    private static boolean isMoviePath(String path) {
        int prefixLength = MOVIE_PATH_PREFIX.length();

        return path.startsWith(MOVIE_PATH_PREFIX) && path.length() > prefixLength && path.indexOf('/', prefixLength) < 0;
    }

}
