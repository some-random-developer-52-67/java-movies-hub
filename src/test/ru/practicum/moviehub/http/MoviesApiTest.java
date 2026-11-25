package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void postMovies_whenTitleLengthAndYearsAreOnInclusiveLimits_createsMovies() {
        String title = "я".repeat(100);
        int lastAllowedYear = Year.now().getValue() + 1;


        HttpResponse<String> first = postMovie(title, 1888);
        HttpResponse<String> second = postMovie(title, lastAllowedYear);


        assertEquals(201, first.statusCode());
        assertEquals(201, second.statusCode());
        assertEquals(title, readMovie(first).title());
        assertEquals(1888, readMovie(first).year());
        assertEquals(lastAllowedYear, readMovie(second).year());
    }

    @Test
    void postMovies_whenTitleIsTooLong_returns422() {
        HttpResponse<String> response = postMovie("я".repeat(101), 2016);


        assertValidationError(response, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "\"   \"", "42", "null"})
    void postMovies_whenTitleIsInvalid_returns422(String titleJson) {
        String body = "{\"title\": %s, \"year\": 2016}".formatted(titleJson);

        HttpResponse<String> response = postJson(body, JSON_UTF_8);

        assertValidationError(response, 1);
    }

    @Test
    void postMovies_whenYearIsOutsideLimits_returns422() {
        int firstDisallowedYear = Year.now().getValue() + 2;


        HttpResponse<String> beforeLimit = postMovie("Прибытие", 1887);
        HttpResponse<String> afterLimit = postMovie("Прибытие", firstDisallowedYear);


        assertValidationError(beforeLimit, 1);
        assertValidationError(afterLimit, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"2016\"", "2016.5", "null", "true", "2147483648"})
    void postMovies_whenYearIsNotAnInt_returns422(String yearJson) {
        String body = "{\"title\": \"Прибытие\", \"year\": %s}".formatted(yearJson);

        HttpResponse<String> response = postJson(body, JSON_UTF_8);

        assertValidationError(response, 1);
    }

    @Test
    void postMovies_whenBothFieldsAreMissing_returnsBothDetailsAndDoesNotAddAMovie() {
        HttpResponse<String> invalid = postJson("{}", JSON_UTF_8);
        HttpResponse<String> movies = send("GET", MOVIES_PATH);

        assertValidationError(invalid, 2);
        assertEquals(200, movies.statusCode());
        assertEquals(List.of(), readMovies(movies));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "null", "2016"})
    void postMovies_whenBodyIsNotAnObject_returns422(String body) {
        HttpResponse<String> response = postJson(body, JSON_UTF_8);

        assertValidationError(response, 1);
    }

    @Test
    void postMovies_whenJsonIsIncomplete_returns400() {
        HttpResponse<String> response = postJson("{\"title\":", JSON_UTF_8);

        assertError(response, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":TRUE}",
            "{\"title\":\"A\nB\"}",
            "{\"title\":\"A\\'B\"}"
    })
    void postMovies_whenJsonContainsInvalidStringOrLiteral_returns400(String body) {
        HttpResponse<String> response = postJson(body, JSON_UTF_8);

        assertError(response, 400);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"text/plain", "application/jsonp", "application/json; charset=ISO-8859-1"})
    void postMovies_whenContentTypeIsUnsupported_returns415(String contentType) {
        String body = "{\"title\": \"Прибытие\", \"year\": 2016}";

        HttpResponse<String> response = postJson(body, contentType);

        assertError(response, 415);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "Application/JSON; charset=\"utf-8\""})
    void postMovies_whenContentTypeIsJson_createsMovie(String contentType) {
        String body = "{\"title\": \"Прибытие\", \"year\": 2016}";

        HttpResponse<String> response = postJson(body, contentType);

        assertEquals(201, response.statusCode());
        assertJsonContentType(response);
    }

    @Test
    void getMovie_whenIdExists_returnsMovie() {
        Movie movie = createMovie("Прибытие", 2016);
        String path = MOVIES_PATH + "/" + movie.id();


        HttpResponse<String> response = send("GET", path);


        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        assertEquals(movie, readMovie(response));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "DELETE"})
    void movieById_whenMovieIsMissing_returns404(String method) {
        HttpResponse<String> response = send(method, MOVIES_PATH + "/42");


        assertError(response, 404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "9223372036854775808"})
    void movieById_whenIdIsInvalid_returns400ForGetAndDelete(String id) {
        String path = MOVIES_PATH + "/" + id;


        HttpResponse<String> get = send("GET", path);
        HttpResponse<String> delete = send("DELETE", path);


        assertError(get, 400);
        assertError(delete, 400);
    }

    @Test
    void deleteMovie_whenIdExists_returns204AndRemovesMovie() {
        Movie movie = createMovie("Прибытие", 2016);
        Movie remainingMovie = createMovie("Сталкер", 1979);
        String path = MOVIES_PATH + "/" + movie.id();


        HttpResponse<String> first = send("DELETE", path);
        HttpResponse<String> second = send("DELETE", path);
        HttpResponse<String> get = send("GET", path);
        HttpResponse<String> movies = send("GET", MOVIES_PATH);


        assertEquals(204, first.statusCode());
        assertJsonContentType(first);
        assertEquals("", first.body());
        assertError(second, 404);
        assertError(get, 404);
        assertEquals(200, movies.statusCode());
        assertEquals(List.of(remainingMovie), readMovies(movies));
    }

    @Test
    void getMovies_whenYearMatches_returnsOnlyMoviesFromThatYear() {
        Movie first = createMovie("Прибытие", 2016);
        Movie second = createMovie("Ла-Ла Ленд", 2016);
        createMovie("Сталкер", 1979);


        HttpResponse<String> response = send("GET", MOVIES_PATH + "?year=2016");


        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        List<Movie> movies = readMovies(response);
        assertEquals(2, movies.size());
        assertTrue(movies.containsAll(List.of(first, second)));
    }

    @Test
    void getMovies_whenYearValueIsPercentEncoded_filtersByDecodedValue() {
        Movie movie = createMovie("Прибытие", 2016);
        createMovie("Сталкер", 1979);


        HttpResponse<String> response = send("GET", MOVIES_PATH + "?year=201%36");


        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        assertEquals(List.of(movie), readMovies(response));
    }

    @ParameterizedTest
    @ValueSource(ints = {1800, 2017})
    void getMovies_whenYearDoesNotMatch_returnsEmptyArray(int year) {
        createMovie("Прибытие", 2016);


        HttpResponse<String> response = send("GET", MOVIES_PATH + "?year=" + year);


        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        assertEquals(List.of(), readMovies(response));
    }

    @ParameterizedTest
    @ValueSource(strings = {"year", "year=", "year=abc", "year=2016.0", "year=2147483648", "year=2016&year=2017"})
    void getMovies_whenYearIsInvalid_returns400(String query) {
        HttpResponse<String> response = send("GET", MOVIES_PATH + "?" + query);


        assertError(response, 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/unknown", "/movies-other", "/movies/", "/movies/1/other"})
    void get_whenPathIsUnknown_returns404(String path) {
        HttpResponse<String> response = send("GET", path);


        assertError(response, 404);
    }

    @Test
    void putMovies_whenMethodIsUnsupported_returns405AndAllowedMethods() {
        HttpResponse<String> response = send("PUT", MOVIES_PATH);


        assertError(response, 405);
        assertEquals("GET, POST", response.headers().firstValue("Allow").orElseThrow());
    }

    @Test
    void putMovie_whenMethodIsUnsupported_returns405AndAllowedMethods() {
        HttpResponse<String> response = send("PUT", MOVIES_PATH + "/42");


        assertError(response, 405);
        assertEquals("GET, DELETE", response.headers().firstValue("Allow").orElseThrow());
    }

    @Test
    void headMovies_whenMethodIsUnsupported_returns405WithNoBody() {
        HttpResponse<String> response = send("HEAD", MOVIES_PATH);


        assertEquals(405, response.statusCode());
        assertJsonContentType(response);
        assertEquals("GET, POST", response.headers().firstValue("Allow").orElseThrow());
        assertEquals("", response.body());
    }

    private static void assertJsonContentType(HttpResponse<String> response) {
        assertEquals(JSON_UTF_8, response.headers().firstValue("Content-Type").orElseThrow());
    }

    private static ErrorResponse assertError(HttpResponse<String> response, int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode());
        assertJsonContentType(response);
        ErrorResponse error = GSON.fromJson(response.body(), ErrorResponse.class);
        assertNotNull(error.error());
        assertFalse(error.error().isBlank());

        return error;
    }

    private static void assertValidationError(HttpResponse<String> response, int expectedDetailsCount) {
        ErrorResponse error = assertError(response, 422);
        assertNotNull(error.details());
        assertEquals(expectedDetailsCount, error.details().size());
        assertTrue(error.details().stream().noneMatch(String::isBlank));
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

        return postJson(json, JSON_UTF_8);
    }

    private HttpResponse<String> postJson(String json, String contentType) {
        HttpRequest.Builder builder = request(MOVIES_PATH);
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }

        return send(builder.POST(HttpRequest.BodyPublishers.ofString(json, UTF_8)).build());
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
