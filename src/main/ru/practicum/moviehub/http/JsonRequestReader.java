package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

final class JsonRequestReader {

    private static final Gson GSON = new Gson();
    private static final Pattern JSON_STRINGS_AND_LITERALS = Pattern.compile(
            "\"(?:[^\"\\\\\\x00-\\x1F]|\\\\(?:[\"\\\\/bfnrt]|u\\p{XDigit}{4}))*+\""
                    + "|true|false|null|([\"tTfFnN\\uFEFF])"
    );

    private JsonRequestReader() {
    }

    static <T> Optional<T> readObject(InputStream input, Class<T> type) throws IOException {
        byte[] bytes = input.readAllBytes();
        String body = UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        if (JSON_STRINGS_AND_LITERALS.matcher(body).results().anyMatch(token -> token.start(1) >= 0)) {
            throw new MalformedJsonException("Invalid JSON string or literal");
        }

        try (JsonReader reader = new JsonReader(new StringReader(body))) {
            JsonElement json = GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new MalformedJsonException("Expected one JSON value");
            }

            return json.isJsonObject()
                    ? Optional.of(GSON.fromJson(json, type))
                    : Optional.empty();
        }
    }

}
