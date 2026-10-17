package com.newuniverse.nusmp.arc.arena;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why: the arenas and the Chronicle are drawn with vanilla blocks only, and a block with a facing, a half or an axis cannot be named by its id alone. A shape
 * (pure, no Minecraft types) therefore names every block as TEXT in the form the game's own commands use, {@code stone_brick_stairs[facing=north,half=bottom]},
 * and the game side turns the text into a block state in one place. Keeping the names as text is what lets a unit test read the whole palette of every arena
 * and check each id and each property value against the table of vanilla block states of the game version
 * ({@code tools/labyrinth/vanilla_block_states_1_21_1.json}), so a block that does not exist, or a typo in a property, fails a test and not the first arrival of a
 * player. The namespace is optional and means {@code minecraft}.
 */
public record BlockSpec(String namespace, String path, Map<String, String> properties) {
    private static final Pattern FORM = Pattern.compile("^(?:([a-z0-9_.-]+):)?([a-z0-9_./-]+)(?:\\[([a-z0-9_=,]*)\\])?$");

    public BlockSpec {
        properties = properties == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    /** The spec of a text such as {@code minecraft:lantern[hanging=false]} or {@code stone_bricks}. Throws {@link IllegalArgumentException} for text that is not one. */
    public static BlockSpec parse(String text) {
        if (text == null) throw new IllegalArgumentException("a block spec is not null");
        Matcher m = FORM.matcher(text.trim());
        if (!m.matches()) throw new IllegalArgumentException("not a block spec: " + text);
        Map<String, String> props = new LinkedHashMap<>();
        String body = m.group(3);
        if (body != null && !body.isEmpty()) {
            for (String pair : body.split(",")) {
                int eq = pair.indexOf('=');
                if (eq <= 0 || eq == pair.length() - 1) throw new IllegalArgumentException("not a property in " + text + ": " + pair);
                if (props.put(pair.substring(0, eq), pair.substring(eq + 1)) != null) throw new IllegalArgumentException("property listed twice in " + text);
            }
        }
        return new BlockSpec(m.group(1) == null ? "minecraft" : m.group(1), m.group(2), props);
    }

    /** {@code namespace:path}, the registry id of the block. */
    public String id() { return namespace + ":" + path; }

    /** The text form again, with the namespace spelled out and the properties in the order they were written. */
    @Override
    public String toString() {
        if (properties.isEmpty()) return id();
        StringBuilder b = new StringBuilder(id()).append('[');
        boolean first = true;
        for (Map.Entry<String, String> e : properties.entrySet()) {
            if (!first) b.append(',');
            b.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return b.append(']').toString();
    }
}
