package ru.practicum.moviehub.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

record CreateMovieRequest(
        JsonElement title,
        JsonElement year
) {

    String readTitle() {
        return title instanceof JsonPrimitive value && value.isString()
                ? value.getAsString()
                : null;
    }

    Integer readYear() {
        if (!(year instanceof JsonPrimitive value) || !value.isNumber()) {
            return null;
        }

        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

}
