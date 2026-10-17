package com.newuniverse.nusmp.arc.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: everything a character says lives in one data file ({@code data/nusmp/arc/dialogue/<character_id>.json}), so the people who write the characters
 * write words, not Java, and a datapack can replace any of it. This class is the pure half: it parses such a file into a book of {@link DialogueLine}s per
 * {@link DialogueContext} and picks the line a player hears. It never throws on a bad file: every problem (a typo in a context name, a line without text,
 * a number that is not one) is returned as a readable message and the rest of the file is still used, so one wrong line does not make a character mute.
 *
 * <p>The file:
 * <pre>
 * { "character": "yami_sukehiro", "voice": "#C8C8D8",
 *   "lines": {
 *     "greeting": [ "Plain text is a line with no conditions.",
 *                   { "text": "Hello, {player}.", "key": "arc.dialogue.yami_sukehiro.2", "first": true } ],
 *     "hint":     [ { "text": "Heath Grice is down. Good.", "defeated": ["heath_grice"], "minStage": 1, "maxStage": 5,
 *                    "saga": ["elf_reincarnation"], "notDefeated": ["zagred"], "minQuest": 0, "maxQuest": 1,
 *                    "priority": 3, "weight": 2 } ] } }
 * </pre>
 * Choosing: of the lines of the context whose conditions hold, the highest {@code priority} wins (the default priority is the number of conditions, so a
 * line that reacts to the story beats a generic one; give a conditional line {@code "priority": 0} to let it mix with the generic ones); among the
 * winners the choice is random, weighted by {@code weight}, and the line shown last time is skipped when there is another.
 */
public final class DialogueBook {
    /** The id of the file every character without a file of its own says (data/nusmp/arc/dialogue/_fallback.json). */
    public static final String FALLBACK_ID = "_fallback";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_]+)}");

    /** The result of reading a file: a book (null when nothing usable was found) and every problem met on the way. */
    public record Parsed(DialogueBook book, List<String> problems) {
        public boolean ok() { return book != null; }
    }

    /** A chosen line and its position in its context's list (so the caller can avoid repeating it next time). */
    public record Pick(DialogueLine line, int index) {}

    private final String characterId;
    private final int voice;
    private final Map<DialogueContext, List<DialogueLine>> lines;

    private DialogueBook(String characterId, int voice, Map<DialogueContext, List<DialogueLine>> lines) {
        this.characterId = characterId;
        this.voice = voice;
        this.lines = lines;
    }

    public String characterId() { return characterId; }

    /** The chat colour 0xRRGGBB the file asks for, or -1 to use the character's own colour. */
    public int voice() { return voice; }

    public List<DialogueLine> lines(DialogueContext c) {
        List<DialogueLine> l = lines.get(c);
        return l == null ? List.of() : l;
    }

    /** Every line, in the canonical order of the contexts then the order in the file (the order the language keys are numbered in). */
    public List<DialogueLine> all() {
        List<DialogueLine> out = new ArrayList<>();
        for (DialogueContext c : DialogueContext.values()) out.addAll(lines(c));
        return out;
    }

    public int size() { return all().size(); }

    /** Is there at least one line for this context that the situation allows? */
    public boolean has(DialogueContext c, DialogueSituation s) {
        for (DialogueLine l : lines(c)) if (l.matches(s)) return true;
        return false;
    }

    /**
     * Chooses a line, or null when none fits.
     *
     * @param avoidIndex the index of the line shown last time in this context ({@link Pick#index()}), or -1; skipped when another line also fits
     * @param rnd        gives a number in {@code [0, bound)}
     */
    public Pick pick(DialogueContext c, DialogueSituation s, int avoidIndex, IntUnaryOperator rnd) {
        List<DialogueLine> all = lines(c);
        List<Integer> fit = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) if (all.get(i).matches(s)) fit.add(i);
        if (fit.isEmpty()) return null;
        int best = Integer.MIN_VALUE;
        for (int i : fit) best = Math.max(best, all.get(i).priority());
        List<Integer> top = new ArrayList<>();
        for (int i : fit) if (all.get(i).priority() == best) top.add(i);
        if (top.size() > 1 && avoidIndex >= 0) top.remove(Integer.valueOf(avoidIndex));        // only among equals: a more specific line is never dropped for variety
        int total = 0;
        for (int i : top) total += all.get(i).weight();
        int roll = total <= 1 ? 0 : Math.max(0, Math.min(total - 1, rnd.applyAsInt(total)));
        for (int i : top) {
            roll -= all.get(i).weight();
            if (roll < 0) return new Pick(all.get(i), i);
        }
        return new Pick(all.get(top.get(top.size() - 1)), top.get(top.size() - 1));
    }

    // ================================================================ parsing
    /** Reads a dialogue file. {@code characterId} is the file's name (the id the book belongs to). */
    public static Parsed parse(String characterId, String json) {
        List<String> problems = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (JsonParseException | IllegalStateException e) {
            problems.add("not valid JSON: " + e.getMessage());
            return new Parsed(null, problems);
        }
        if (root == null || !root.isJsonObject()) {
            problems.add("the file must be a JSON object");
            return new Parsed(null, problems);
        }
        JsonObject obj = root.getAsJsonObject();
        String id = characterId == null ? "" : characterId.toLowerCase(Locale.ROOT);
        if (obj.has("character") && obj.get("character").isJsonPrimitive()
                && !obj.get("character").getAsString().equalsIgnoreCase(id)) {
            problems.add("the \"character\" field says '" + obj.get("character").getAsString() + "' but the file belongs to '" + id + "'");
        }
        int voice = -1;
        if (obj.has("voice")) {
            voice = colour(obj.get("voice"));
            if (voice < 0) problems.add("\"voice\" must be a colour like \"#C8C8D8\"");
        }
        if (!obj.has("lines") || !obj.get("lines").isJsonObject()) {
            problems.add("the file needs a \"lines\" object");
            return new Parsed(null, problems);
        }
        Map<DialogueContext, List<DialogueLine>> out = new EnumMap<>(DialogueContext.class);
        for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("lines").entrySet()) {
            DialogueContext ctx = DialogueContext.fromId(e.getKey());
            if (ctx == null) {
                problems.add("unknown situation '" + e.getKey() + "' (known: " + knownContexts() + ")");
                continue;
            }
            if (!e.getValue().isJsonArray()) {
                problems.add(e.getKey() + ": must be an array of lines");
                continue;
            }
            JsonArray arr = e.getValue().getAsJsonArray();
            List<DialogueLine> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                DialogueLine l = line(arr.get(i), e.getKey() + "[" + i + "]", problems);
                if (l != null) list.add(l);
            }
            if (!list.isEmpty()) out.put(ctx, Collections.unmodifiableList(list));
        }
        if (out.isEmpty()) {
            problems.add("the file has no usable line");
            return new Parsed(null, problems);
        }
        return new Parsed(new DialogueBook(id, voice, out), problems);
    }

    private static String knownContexts() {
        List<String> ids = new ArrayList<>();
        for (DialogueContext c : DialogueContext.values()) ids.add(c.id());
        return String.join(", ", ids);
    }

    private static int colour(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) return -1;
        String s = e.getAsString().trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (!s.matches("[0-9A-Fa-f]{6}")) return -1;
        return Integer.parseInt(s, 16);
    }

    private static DialogueLine line(JsonElement e, String where, List<String> problems) {
        if (e == null || e.isJsonNull()) {
            problems.add(where + ": empty entry");
            return null;
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            String t = e.getAsString();
            if (t.isBlank()) {
                problems.add(where + ": blank text");
                return null;
            }
            checkPlaceholders(t, where, problems);
            return DialogueLine.unconditional("", t);
        }
        if (!e.isJsonObject()) {
            problems.add(where + ": a line is a string or an object");
            return null;
        }
        JsonObject o = e.getAsJsonObject();
        String text = string(o, "text");
        if (text == null || text.isBlank()) {
            problems.add(where + ": a line needs a non-blank \"text\"");
            return null;
        }
        checkPlaceholders(text, where, problems);
        String key = string(o, "key");
        int minStage = integer(o, "minStage", DialogueLine.NO_MIN, where, problems);
        int maxStage = integer(o, "maxStage", DialogueLine.NO_MAX, where, problems);
        int minQuest = integer(o, "minQuest", DialogueLine.NO_MIN, where, problems);
        int maxQuest = integer(o, "maxQuest", DialogueLine.NO_MAX, where, problems);
        Set<String> sagas = ids(o, "saga", where, problems);
        Set<String> defeated = ids(o, "defeated", where, problems);
        Set<String> notDefeated = ids(o, "notDefeated", where, problems);
        int first = DialogueLine.FIRST_ANY;
        if (o.has("first")) {
            JsonElement f = o.get("first");
            if (f.isJsonPrimitive() && f.getAsJsonPrimitive().isBoolean()) first = f.getAsBoolean() ? DialogueLine.FIRST_ONLY : DialogueLine.FIRST_NEVER;
            else problems.add(where + ": \"first\" must be true or false");
        }
        if (minStage != DialogueLine.NO_MIN && maxStage != DialogueLine.NO_MAX && minStage > maxStage)
            problems.add(where + ": minStage " + minStage + " is above maxStage " + maxStage + ", the line can never be said");
        if (minQuest != DialogueLine.NO_MIN && maxQuest != DialogueLine.NO_MAX && minQuest > maxQuest)
            problems.add(where + ": minQuest " + minQuest + " is above maxQuest " + maxQuest + ", the line can never be said");
        for (String d : defeated) if (notDefeated.contains(d)) problems.add(where + ": '" + d + "' is both required and forbidden, the line can never be said");
        int priority = integer(o, "priority", DialogueLine.countConditions(minStage, maxStage, sagas, defeated, notDefeated, minQuest, maxQuest, first), where, problems);
        int weight = integer(o, "weight", 1, where, problems);
        return new DialogueLine(key, text, minStage, maxStage, sagas, defeated, notDefeated, minQuest, maxQuest, first, priority, weight);
    }

    private static void checkPlaceholders(String text, String where, List<String> problems) {
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) {
            if (!DialogueLine.PLACEHOLDERS.contains(m.group(1)))
                problems.add(where + ": unknown placeholder {" + m.group(1) + "} (known: " + String.join(", ", DialogueLine.PLACEHOLDERS) + ")");
        }
    }

    private static String string(JsonObject o, String name) {
        JsonElement e = o.get(name);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static int integer(JsonObject o, String name, int dflt, String where, List<String> problems) {
        if (!o.has(name)) return dflt;
        JsonElement e = o.get(name);
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            try {
                return e.getAsInt();
            } catch (NumberFormatException bad) {
                problems.add(where + ": \"" + name + "\" is not a whole number");
                return dflt;
            }
        }
        problems.add(where + ": \"" + name + "\" must be a number");
        return dflt;
    }

    /** A condition that lists ids: one string or an array of strings, lower-cased. */
    private static Set<String> ids(JsonObject o, String name, String where, List<String> problems) {
        Set<String> out = new LinkedHashSet<>();
        if (!o.has(name)) return out;
        JsonElement e = o.get(name);
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            out.add(e.getAsString().trim().toLowerCase(Locale.ROOT));
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) {
                if (x.isJsonPrimitive() && x.getAsJsonPrimitive().isString()) out.add(x.getAsString().trim().toLowerCase(Locale.ROOT));
                else problems.add(where + ": \"" + name + "\" must list ids as strings");
            }
        } else {
            problems.add(where + ": \"" + name + "\" must be an id or an array of ids");
        }
        out.removeIf(String::isEmpty);
        return out;
    }
}
