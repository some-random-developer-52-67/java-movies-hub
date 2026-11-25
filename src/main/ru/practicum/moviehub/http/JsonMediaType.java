package ru.practicum.moviehub.http;

import static java.nio.charset.StandardCharsets.UTF_8;

final class JsonMediaType {

    private static final String APPLICATION_JSON = "application/json";
    static final String UTF_8_CONTENT_TYPE = APPLICATION_JSON + "; charset=" + UTF_8.name();

    private JsonMediaType() {
    }

    static boolean supports(String contentType) {
        if (contentType == null) {
            return false;
        }

        String[] parts = contentType.split(";", 2);
        if (!APPLICATION_JSON.equalsIgnoreCase(parts[0].strip())) {
            return false;
        }
        if (parts.length == 1) {
            return true;
        }

        String[] parameter = parts[1].split("=", 2);
        if (parameter.length != 2 || !"charset".equalsIgnoreCase(parameter[0].strip())) {
            return false;
        }

        String charset = parameter[1].strip();

        return UTF_8.name().equalsIgnoreCase(charset) || ("\"" + UTF_8.name() + "\"").equalsIgnoreCase(charset);
    }

}
