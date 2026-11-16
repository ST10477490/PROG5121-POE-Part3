package com.chatapp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes and reads message records in a JSON file (an array of objects).
 * Written by hand so that no external library is needed.
 */
public final class JsonStorage {

    private JsonStorage() {
    }

    /**
     * Writes records to a JSON file. A String value is written in quotes and
     * any other value (such as an Integer) is written as a plain number.
     *
     * @param filePath the file to write to
     * @param records  the records to write
     * @return true if the file was written successfully
     */
    public static boolean write(String filePath, List<Map<String, Object>> records) {
        StringBuilder json = new StringBuilder("[\n");

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            json.append("  {\n");

            int field = 0;
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                json.append("    \"").append(escape(entry.getKey())).append("\": ");
                if (entry.getValue() instanceof String) {
                    json.append("\"").append(escape((String) entry.getValue())).append("\"");
                } else {
                    json.append(entry.getValue());
                }
                field++;
                json.append(field < record.size() ? ",\n" : "\n");
            }

            json.append("  }").append(i < records.size() - 1 ? ",\n" : "\n");
        }
        json.append("]\n");

        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, json.toString().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reads a JSON file written by {@link #write} into a list of records.
     * Every value is returned as a String.
     *
     * @param filePath the file to read
     * @return the records, or an empty list if the file is missing or unreadable
     */
    public static List<Map<String, String>> read(String filePath) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            return parse(json);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Parses a JSON array of flat objects (string and number values only).
     */
    static List<Map<String, String>> parse(String json) {
        List<Map<String, String>> records = new ArrayList<>();
        int[] end = new int[1];
        int i = 0;
        int length = json.length();

        while (i < length) {
            if (json.charAt(i) == '{') {
                Map<String, String> record = new LinkedHashMap<>();
                i++;

                while (i < length && json.charAt(i) != '}') {
                    if (json.charAt(i) == '"') {
                        // Read the field name
                        String key = readString(json, i, end);
                        i = end[0];

                        // Skip to the value after the colon
                        while (i < length && json.charAt(i) != ':') {
                            i++;
                        }
                        i++;
                        while (i < length && Character.isWhitespace(json.charAt(i))) {
                            i++;
                        }

                        // Read the value (a string or a plain number)
                        String value;
                        if (json.charAt(i) == '"') {
                            value = readString(json, i, end);
                            i = end[0];
                        } else {
                            int start = i;
                            while (i < length && json.charAt(i) != ','
                                    && json.charAt(i) != '}'
                                    && !Character.isWhitespace(json.charAt(i))) {
                                i++;
                            }
                            value = json.substring(start, i);
                        }
                        record.put(key, value);
                    } else {
                        i++;
                    }
                }
                records.add(record);
            }
            i++;
        }
        return records;
    }

    /**
     * Reads a quoted JSON string starting at the opening quote, converting
     * escape sequences back into normal characters.
     *
     * @param json  the full JSON text
     * @param start the position of the opening quote
     * @param end   receives the position just after the closing quote
     * @return the text inside the quotes
     */
    private static String readString(String json, int start, int[] end) {
        StringBuilder text = new StringBuilder();
        int i = start + 1;

        while (i < json.length() && json.charAt(i) != '"') {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                i++;
                char next = json.charAt(i);
                switch (next) {
                    case 'n': text.append('\n'); break;
                    case 'r': text.append('\r'); break;
                    case 't': text.append('\t'); break;
                    case 'u':
                        text.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                        i += 4;
                        break;
                    default: text.append(next);
                }
            } else {
                text.append(c);
            }
            i++;
        }
        end[0] = i + 1;
        return text.toString();
    }

    /**
     * Escapes special characters so text is valid inside a JSON string.
     */
    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':  escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\n': escaped.append("\\n");  break;
                case '\r': escaped.append("\\r");  break;
                case '\t': escaped.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }
}
