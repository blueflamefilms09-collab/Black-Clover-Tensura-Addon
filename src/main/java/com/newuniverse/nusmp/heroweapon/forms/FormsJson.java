package com.newuniverse.nusmp.heroweapon.forms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: {@link FormsSchema} must run in JUnit and in the game on the same input, and must not depend on a JSON library's types, so a file is first turned into
 * plain Java values here: {@code Map} (member order kept), {@code List}, {@code String}, {@code Boolean}, {@code Long} for a whole number, {@code Double} for any
 * other number, and {@code null}. The only library used is Gson, which Minecraft already ships and which {@code DialogueBook} and the aptitude loader use the same way.
 */
public final class FormsJson {
    private FormsJson() {}

    /**
     * Parses JSON text into plain values.
     *
     * @throws IllegalArgumentException with a readable message when the text is not JSON
     */
    public static Object parse(String text) {
        try {
            return convert(JsonParser.parseString(text == null ? "" : text));
        } catch (JsonParseException | IllegalStateException bad) {
            throw new IllegalArgumentException("not valid JSON: " + bad.getMessage());
        }
    }

    /**
     * The keys that appear twice in one object of the text, as readable messages. Gson keeps only the last of two equal keys without a word, so a duplicated
     * {@code "key1"} slot in a {@code skills} object would silently lose a technique; the schema reports these as warnings. Text that is not JSON gives no messages
     * (the parse reports that).
     */
    public static List<String> duplicateKeys(String text) {
        List<String> out = new ArrayList<>();
        try (JsonReader in = new JsonReader(new StringReader(text == null ? "" : text))) {
            in.setLenient(true);
            Deque<Set<String>> objects = new ArrayDeque<>();
            Deque<String> names = new ArrayDeque<>();
            String last = "";
            for (JsonToken t = in.peek(); t != JsonToken.END_DOCUMENT; t = in.peek()) {
                switch (t) {
                    case BEGIN_OBJECT -> {
                        in.beginObject();
                        objects.push(new HashSet<>());
                        names.push(last);
                    }
                    case END_OBJECT -> {
                        in.endObject();
                        objects.pop();
                        last = names.pop();
                    }
                    case BEGIN_ARRAY -> in.beginArray();
                    case END_ARRAY -> in.endArray();
                    case NAME -> {
                        last = in.nextName();
                        if (!objects.peek().add(last)) out.add("the key \"" + last + "\" appears twice in one object" + (names.peek().isEmpty() ? "" : " (inside \"" + names.peek() + "\")") + "; only the last one counts");
                    }
                    default -> in.skipValue();
                }
            }
        } catch (IOException | RuntimeException notJson) {
            // the parse itself reports invalid text
        }
        return out;
    }

    /** Converts a Gson tree (used by the loader, which reads the stream itself). */
    public static Object convert(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> m : o.entrySet()) out.put(m.getKey(), convert(m.getValue()));
            return out;
        }
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            List<Object> out = new ArrayList<>(a.size());
            for (JsonElement x : a) out.add(convert(x));
            return out;
        }
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isBoolean()) return p.getAsBoolean();
        if (p.isNumber()) return number(p.getAsNumber().toString());
        return p.getAsString();
    }

    /** A whole number that fits a long becomes a {@code Long}; anything else a {@code Double}. */
    static Object number(String text) {
        try {
            BigDecimal d = new BigDecimal(text);
            if (d.stripTrailingZeros().scale() <= 0 && d.abs().compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) < 0 && !text.contains(".") && !text.contains("e") && !text.contains("E"))
                return d.longValueExact();
            return d.doubleValue();
        } catch (NumberFormatException | ArithmeticException odd) {
            return Double.NaN;
        }
    }
}
