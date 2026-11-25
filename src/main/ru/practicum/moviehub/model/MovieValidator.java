package ru.practicum.moviehub.model;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public final class MovieValidator {

    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    private MovieValidator() {
    }

    public static List<String> validate(String title, Integer year) {
        List<String> details = new ArrayList<>();
        validateTitle(title, details);
        validateYear(year, details);

        return details;
    }

    private static void validateTitle(String title, List<String> details) {
        if (title == null) {
            details.add("Название должно быть строкой");
        } else if (title.isBlank()) {
            details.add("Название не должно быть пустым");
        } else if (title.codePointCount(0, title.length()) > MAX_TITLE_LENGTH) {
            details.add("Название не должно быть длиннее %d символов".formatted(MAX_TITLE_LENGTH));
        }
    }

    private static void validateYear(Integer year, List<String> details) {
        int maxYear = Year.now().getValue() + 1;
        if (year == null || year < MIN_YEAR || year > maxYear) {
            details.add("Год должен быть целым числом от %d до %d".formatted(MIN_YEAR, maxYear));
        }
    }

}
