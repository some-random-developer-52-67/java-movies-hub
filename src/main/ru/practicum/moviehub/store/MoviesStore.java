package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {

    private final Map<Long, Movie> movies = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized Movie add(String title, int year) {
        Movie movie = new Movie(nextId++, title, year);
        movies.put(movie.id(), movie);

        return movie;
    }

    public synchronized List<Movie> getAll() {
        return List.copyOf(movies.values());
    }

}
