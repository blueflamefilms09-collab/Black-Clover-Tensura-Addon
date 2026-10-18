package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.heroweapon.HeroKind;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: {@code /multiverse hero gem holders} wants to name the holders of hero weapons who are offline, and the only record of them is the core's world file
 * {@code nusmp_hero_registry.dat}. Its layout belongs to the core builder; when this was written it was not known, and the core's own class (read later) saves
 * {@code {holders: {<weapon id>: [<uuid text>, ...]}}}. So the reader is deliberately tolerant, and because "tolerant" is exactly the kind of logic that hides a
 * wrong answer, it is pure (it reads a tiny {@link Node} view of the saved tree, not the game's NBT classes) and tested with every layout it accepts and with noise
 * it must not read. It walks the whole tree (to a depth limit) and accepts: a compound that maps a weapon id to a holder or to a list of holders
 * ({@code {legendary_shield: <uuid>}}, {@code {legendary_shield: [<uuid>, ...]}}), a compound that maps a holder's uuid to a weapon id
 * ({@code {<uuid>: "legendary_shield"}}), and a compound that has a weapon id under {@code kind} (or {@code weapon}, {@code id}, {@code item}) next to a holder
 * under {@code uuid} (or {@code holder}, {@code owner}, {@code player}, {@code wielder}). A holder is a uuid written as four ints or as the canonical 36-character
 * text (the game's own parser also accepts odd lengths, which would misread a weapon id as a uuid); an entry of a list that is not a uuid is skipped. Every distinct
 * holder of a weapon is kept once, in the order found. Anything else adds nothing.
 */
public final class RegistryScan {
    private RegistryScan() {}

    /** The few things the reader needs to know about one value of a saved tree; each accessor is null when the value is not of that kind. */
    public interface Node {
        /** The entries when this is a compound. */
        Map<String, Node> compound();

        /** The elements when this is a list. */
        List<Node> list();

        /** The text when this is a string. */
        String text();

        /** The ints when this is an int array. */
        int[] ints();
    }

    private static final Set<String> KIND_KEYS = Set.of("kind", "weapon", "id", "item");
    private static final Set<String> HOLDER_KEYS = Set.of("uuid", "holder", "owner", "player", "wielder");
    private static final int MAX_DEPTH = 8;

    /** The holders of each weapon found anywhere in the tree (a weapon nobody holds is absent). */
    public static Map<HeroKind, List<UUID>> parse(Node root) {
        Map<HeroKind, List<UUID>> out = new EnumMap<>(HeroKind.class);
        walk(root, out, 0);
        return out;
    }

    private static void add(Map<HeroKind, List<UUID>> out, HeroKind kind, UUID holder) {
        List<UUID> l = out.computeIfAbsent(kind, k -> new ArrayList<>());
        if (!l.contains(holder)) l.add(holder);
    }

    private static void walk(Node node, Map<HeroKind, List<UUID>> out, int depth) {
        if (node == null || depth > MAX_DEPTH) return;
        Map<String, Node> entries = node.compound();
        if (entries != null) {
            HeroKind named = null;
            UUID who = null;
            for (Map.Entry<String, Node> e : entries.entrySet()) {
                String key = e.getKey();
                Node v = e.getValue();
                HeroKind byKey = HeroKind.parse(key);
                List<UUID> holders = byKey == null ? List.of() : holdersOf(v);
                if (!holders.isEmpty()) {                                                        // {legendary_shield: <uuid>} or {legendary_shield: [<uuid>, ...]}
                    for (UUID h : holders) add(out, byKey, h);
                    continue;
                }
                UUID keyHolder = uuidOf(key);
                HeroKind valueKind = v != null && v.text() != null ? HeroKind.parse(v.text()) : null;
                if (keyHolder != null && valueKind != null) {                                    // {<uuid>: "legendary_shield"}
                    add(out, valueKind, keyHolder);
                    continue;
                }
                String lower = key.toLowerCase(Locale.ROOT);
                if (valueKind != null && KIND_KEYS.contains(lower)) named = valueKind;           // {kind: "legendary_shield", uuid: <uuid>}
                UUID holder = uuidOf(v);
                if (holder != null && HOLDER_KEYS.contains(lower)) who = holder;
                walk(v, out, depth + 1);
            }
            if (named != null && who != null) add(out, named, who);
            return;
        }
        List<Node> items = node.list();
        if (items != null) for (Node item : items) walk(item, out, depth + 1);
    }

    /** The holders a value gives to a weapon: one uuid, or every uuid of a list (entries that are not uuids are skipped). */
    private static List<UUID> holdersOf(Node v) {
        UUID one = uuidOf(v);
        if (one != null) return List.of(one);
        List<Node> items = v == null ? null : v.list();
        if (items == null) return List.of();
        List<UUID> out = new ArrayList<>();
        for (Node item : items) {
            UUID u = uuidOf(item);
            if (u != null && !out.contains(u)) out.add(u);
        }
        return out;
    }

    /** The uuid a value spells (four ints, or the canonical text), or null. */
    static UUID uuidOf(Node v) {
        if (v == null) return null;
        int[] ints = v.ints();
        if (ints != null && ints.length == 4) return new UUID(((long) ints[0] << 32) | (ints[1] & 0xFFFFFFFFL), ((long) ints[2] << 32) | (ints[3] & 0xFFFFFFFFL));
        return v.text() == null ? null : uuidOf(v.text());
    }

    /** The uuid a text spells, or null: only the canonical 36-character form counts. */
    static UUID uuidOf(String text) {
        if (text == null || text.length() != 36) return null;
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }
}
