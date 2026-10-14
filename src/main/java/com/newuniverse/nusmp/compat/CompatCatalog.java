package com.newuniverse.nusmp.compat;

import java.util.List;
import java.util.function.Predicate;

/**
 * What this mod works with, as data (pure, unit tested): one entry per mod, with an honest one-line statement of what the code or the
 * data really does with it. The Compatibility Codex ({@link CompatBook}) and {@code /multiverse compat status} are both built from this
 * list; the live Loaded / Not installed part comes from the caller's mod-list predicate.
 * <p>
 * Rule for editing: an entry may only say what a class or a data file of this repo does. Required mods mirror
 * {@code META-INF/neoforge.mods.toml} (a test compares them).
 */
public final class CompatCatalog {
    private CompatCatalog() {}

    /** The mod id of the addon this pack and the codex section are about. */
    public static final String TNI_MODID = "tensuranoindex";
    /** The id of the built-in data pack that {@code CompatPacks} switches on. */
    public static final String PACK_ID = "nusmp_tni_compat";

    public enum Kind { REQUIRED, VIA_TENSURA, OPTIONAL, BRIDGE }

    /** One mod: its id, the name shown to players, where it sits, and what we do with it. */
    public record Entry(String modId, String name, Kind kind, String line) {}

    /** A mod with the answer to "is it installed right now". */
    public record Status(Entry entry, boolean loaded) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry("neoforge", "NeoForge 21.1", Kind.REQUIRED, "The mod loader this build targets."),
            new Entry("tensura", "Tensura Reincarnated", Kind.REQUIRED,
                    "The base mod. Our skills, evolutions and many mobs are Tensura's, found by id."),
            new Entry("manascore_skill", "ManasCore Skill", Kind.REQUIRED, "The skill framework every power here is registered in."),
            new Entry("manascore_storage", "ManasCore Storage", Kind.REQUIRED, "Required by the mod file (ManasCore's storage module)."),
            new Entry("architectury", "Architectury API", Kind.VIA_TENSURA,
                    "Its event results are used to veto skills in boss fights."),
            new Entry("geckolib", "GeckoLib", Kind.VIA_TENSURA,
                    "Our robe items are GeckoLib items."),
            new Entry("curios", "Curios API", Kind.OPTIONAL,
                    "Weapons and relics fit its slots (data tags), plus our 2 dungeon_relic slots. Worn weapons give a small passive. Read by reflection."),
            new Entry("bettercombat", "Better Combat", Kind.OPTIONAL,
                    "12 weapons inherit its attack presets. Data only."),
            new Entry("ftbteams", "FTB Teams", Kind.OPTIONAL,
                    "Magic Knight Squads run on its parties. Without it the squad commands do nothing."),
            new Entry("trnightmare", "TR Nightmare", Kind.OPTIONAL,
                    "Its soul type picks the starter grimoire magic. Without it a default is used."),
            new Entry("iceandfire", "Ice and Fire", Kind.OPTIONAL,
                    "Its creatures are optional ids in the Living Dungeon tags."),
            new Entry(TNI_MODID, "Tensura No Index", Kind.BRIDGE,
                    "Built-in data pack nusmp_tni_compat: extra loot, boss tags. See the bridge pages."));

    /** Every entry, in book order. */
    public static List<Entry> entries() { return ENTRIES; }

    /** The entry of a mod id, or null. */
    public static Entry entry(String modId) {
        for (Entry e : ENTRIES) if (e.modId().equals(modId)) return e;
        return null;
    }

    /** The entries of one kind, in book order. */
    public static List<Entry> of(Kind kind) { return ENTRIES.stream().filter(e -> e.kind() == kind).toList(); }

    /** Every entry with its live status; {@code loaded} answers for a mod id (a null predicate answers "not installed"). */
    public static List<Status> statuses(Predicate<String> loaded) {
        Predicate<String> p = loaded == null ? id -> false : loaded;
        return ENTRIES.stream().map(e -> new Status(e, p.test(e.modId()))).toList();
    }

    /** Is the built-in pack active? It is switched on exactly when the other mod is loaded. */
    public static boolean packActive(Predicate<String> loaded) { return loaded != null && loaded.test(TNI_MODID); }
}
