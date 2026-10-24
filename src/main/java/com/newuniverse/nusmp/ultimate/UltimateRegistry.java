package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.ultimate.UltimateDef.Debuff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the data-driven table of every Ultimate Magic (docs/magic/ultimate_magic.md): ONE per grimoire. Pure (no Minecraft types). A book appears at most once, ids are unique, and the page of an entry is
 * appended as the LAST family page of its book by {@link UltimatePages}. New entries are only ever appended at the END (the order is the order of the pages across books and the order of the docs); the
 * unbuilt proposals for every other attribute are listed in docs/magic/ultimate_audit.md for the owner to approve.
 *
 * <p>Origin marks: Black Clover names an "Ultimate Magic" only for a few mages (Mereoleona's Excelitius Leonum, Luck's Lightning Battle Fiend, Gadjah's Apocalypra Astrauza; the mod already has those as
 * character pages of the Fire and Lightning books), so the eight flagship Ultimates below are ORIGINAL pages named in the style of the show; the existing canon pages stay untouched (rule 2).
 * Shapes are the existing {@code VfxShape} constants: nothing new is drawn, nothing of the Time Magic effects is used.
 */
public final class UltimateRegistry {
    private UltimateRegistry() {}

    private static List<Debuff> d(Debuff... l) { return List.of(l); }

    private static Debuff slow(int t, int a) { return new Debuff("minecraft:slowness", t, a); }

    private static Debuff weak(int t, int a) { return new Debuff("minecraft:weakness", t, a); }

    private static Debuff fatigue(int t, int a) { return new Debuff("minecraft:mining_fatigue", t, a); }

    private static final List<UltimateDef> CORE = List.of(
            new UltimateDef("ultimate_fire", "book_fire", "FLAME", "Ultimate Flame Magic: Crimson Sun Descent", "fire", "original", 160, 16.0, 40f,
                    d(slow(120, 2), weak(300, 1), new Debuff("minecraft:hunger", 300, 1)), 12, 0, 1.3, 0.35, 16f, 6f, 5f, "FLAMEFX_ZONE", "FIRE_PILLAR", "FLAMEFX_BURST"),
            new UltimateDef("ultimate_water", "book_water", "WATER", "Ultimate Water Magic: Sea God's Deluge", "water", "original", 180, 18.0, 36f,
                    d(slow(160, 2), fatigue(200, 2), weak(200, 1), new Debuff("minecraft:nausea", 120, 0)), 0, 0, 1.6, 0.25, 18f, 6f, 5f, "WATERFX_ZONE", "WATER_BURST", "WATERFX_BURST"),
            new UltimateDef("ultimate_wind", "book_wind", "WIND", "Ultimate Wind Magic: Heavenfall Tempest", "wind", "original", 140, 20.0, 32f,
                    d(slow(80, 1), weak(240, 1), new Debuff("minecraft:levitation", 30, 0)), 0, 0, 2.0, 0.8, 2.5f, 8f, 2.5f, "WIND_RING", "WIND_TORNADO", "WIND_RING"),
            new UltimateDef("ultimate_earth", "book_earth", "EARTH", "Ultimate Earth Magic: World-Splitting Titan Quake", "earth", "original", 200, 14.0, 44f,
                    d(slow(160, 3), fatigue(300, 2), weak(240, 2)), 0, 0, 0.9, 0.5, 14f, 6f, 4f, "EARTHFAM_ZONE", "EARTHFAM_BURST", "EARTHFAM_BURST"),
            new UltimateDef("ultimate_lightning", "book_lightning", "LIGHTNING", "Ultimate Lightning Magic: Thunder Sovereign's Verdict", "lightning", "original", 120, 18.0, 38f,
                    d(slow(100, 2), weak(240, 1), new Debuff("minecraft:blindness", 60, 0)), 6, 0, 0.8, 0.15, 18f, 4f, 4f, "LIGHTNINGFX_ZONE", "LIGHTNING_GOD", "LIGHTNINGFX_BURST"),
            new UltimateDef("ultimate_light", "book_light", "LIGHT", "Ultimate Light Magic: Dawn of Judgment", "light", "original", 180, 20.0, 34f,
                    d(new Debuff("minecraft:blindness", 100, 0), weak(300, 2), new Debuff("minecraft:glowing", 300, 0), slow(80, 1)), 0, 0, 1.0, 0.3, 16f, 8f, 1.5f, "DIVINE_SIGIL", "DIVINE_PILLAR", "LIGHT_FLARE"),
            new UltimateDef("ultimate_dark", "book_dark", "DARK", "Ultimate Dark Magic: Black Moon Eclipse", "dark", "original", 180, 18.0, 36f,
                    d(new Debuff("minecraft:wither", 160, 1), new Debuff("minecraft:darkness", 200, 0), new Debuff("minecraft:blindness", 80, 0), slow(100, 1)), 0, 0, -1.2, 0.0, 18f, 8f, 5f, "DARKFX_ZONE", "DARK_BLACK_MOON", "DARKFX_BURST"),
            new UltimateDef("ultimate_spatial", "book_spatial", "SPATIAL", "Ultimate Spatial Magic: World's Edge Collapse", "space", "original", 170, 18.0, 34f,
                    d(slow(100, 2), new Debuff("minecraft:nausea", 200, 0), weak(200, 1)), 0, 0, -1.5, 0.2, 12f, 18f, 10f, "SPACE_CRACK", "SPACE_SHATTER", "SPACE_SHATTER"));

    /** The core eight, then the four groups of the remaining attributes (3.3.0, each group is its own file so builders never touch the same lines); the order never changes, new rows are appended inside a group. */
    private static final List<UltimateDef> ALL;

    static {
        List<UltimateDef> l = new ArrayList<>(CORE);
        l.addAll(UltimateDefs1.all());
        l.addAll(UltimateDefs2.all());
        l.addAll(UltimateDefs3.all());
        l.addAll(UltimateDefs4.all());
        ALL = Collections.unmodifiableList(l);
    }

    private static final Map<String, UltimateDef> BY_ID, BY_BOOK;

    static {
        Map<String, UltimateDef> id = new LinkedHashMap<>(), book = new LinkedHashMap<>();
        for (UltimateDef u : ALL) {
            id.put(u.id(), u);
            book.put(u.bookPath(), u);
        }
        BY_ID = Collections.unmodifiableMap(id);
        BY_BOOK = Collections.unmodifiableMap(book);
    }

    /** Every Ultimate, in registry order (never changes order, new ones are appended). */
    public static List<UltimateDef> all() { return ALL; }

    /** The Ultimate of a grimoire (by its registry path), or null when that book has none. */
    public static UltimateDef forBook(String bookPath) { return bookPath == null ? null : BY_BOOK.get(bookPath); }

    public static UltimateDef byId(String pageId) { return pageId == null ? null : BY_ID.get(pageId); }

    public static boolean isUltimatePage(String pageId) { return pageId != null && BY_ID.containsKey(pageId); }

    /** Every problem of the table: a bad row, a repeated id or a repeated book. Empty when the table is sound. */
    public static List<String> problems() {
        List<String> out = new ArrayList<>();
        Map<String, Boolean> ids = new LinkedHashMap<>(), books = new LinkedHashMap<>();
        for (UltimateDef u : ALL) {
            String p = u.problem();
            if (p != null) out.add(p);
            if (ids.put(u.id(), true) != null) out.add("id used twice: " + u.id());
            if (books.put(u.bookPath(), true) != null) out.add("two Ultimates for " + u.bookPath());
        }
        return out;
    }
}
