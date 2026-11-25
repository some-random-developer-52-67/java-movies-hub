package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final Gson GSON = new Gson();
    private static final ListOfMoviesTypeToken MOVIES_TYPE = new ListOfMoviesTypeToken();
    private static final String MOVIES_PATH = "/movies";
    private static final String JSON_UTF_8 = "application/json; charset=UTF-8";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    private static HttpClient client;

    private MoviesServer server;

    @BeforeAll
    static void beforeAll() {
        client = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server = new MoviesServer(new MoviesStore(), 0);
        server.start();
    }

    @AfterEach
    void afterEach() {
        if (server != null) {
            server.stop();
        }
    }

    @AfterAll
    static void afterAll() {
        client.close();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() {
        HttpResponse<String> response = send("GET", MOVIES_PATH);


        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        assertEquals(List.of(), readMovies(response));
    }

    @Test
    void postMovies_whenMovieIsValid_returnsCreatedMovieAndLocation() {
        String title = "Прибытие";
        int year = 2016;


        HttpResponse<String> response = postMovie(title, year);


        assertEquals(201, response.statusCode());
        assertJsonContentType(response);
        Movie movie = readMovie(response);
        assertTrue(movie.id() > 0);
        assertEquals(title, movie.title());
        assertEquals(year, movie.year());
        assertEquals("/movies/" + movie.id(), response.headers().firstValue("Location").orElseThrow());
    }

    @Test
    void getMovies_whenMoviesHaveBeenAdded_returnsBothMoviesWithDifferentIds() {
        Movie first = createMovie("Прибытие", 2016);
        Movie second = createMovie("Сталкер", 1979);


        HttpResponse<String> response = send("GET", MOVIES_PATH);


        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        List<Movie> movies = readMovies(response);
        assertEquals(2, movies.size());
        assertTrue(movies.containsAll(List.of(first, second)));
        assertNotEquals(first.id(), second.id());
    }

    private static void assertJsonContentType(HttpResponse<String> response) {
        assertEquals(JSON_UTF_8, response.headers().firstValue("Content-Type").orElseThrow());
    }

    private static List<Movie> readMovies(HttpResponse<String> response) {
        return GSON.fromJson(response.body(), MOVIES_TYPE);
    }

    private static Movie readMovie(HttpResponse<String> response) {
        return GSON.fromJson(response.body(), Movie.class);
    }

    private Movie createMovie(String title, int year) {
        HttpResponse<String> response = postMovie(title, year);
        assertEquals(201, response.statusCode());

        return readMovie(response);
    }

    private HttpResponse<String> postMovie(String title, int year) {
        String json = GSON.toJson(Map.of("title", title, "year", year));

        HttpRequest request = request(MOVIES_PATH)
                .header("Content-Type", JSON_UTF_8)
                .POST(HttpRequest.BodyPublishers.ofString(json, UTF_8))
                .build();

        return send(request);
    }

    private HttpRequest.Builder request(String path) {
        URI uri = URI.create("http://localhost:" + server.getPort() + path);

        return HttpRequest.newBuilder(uri).timeout(HTTP_TIMEOUT);
    }

    private HttpResponse<String> send(String method, String path) {
        return send(request(path).method(method, HttpRequest.BodyPublishers.noBody()).build());
    }

    private HttpResponse<String> send(HttpRequest request) {
        return assertDoesNotThrow(() -> client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8)));
    }

}
