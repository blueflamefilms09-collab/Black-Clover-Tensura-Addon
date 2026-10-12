package com.newuniverse.nusmp.dungeon.wild;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Who lives in the Living Dungeon: the creatures of the entity-type tags nusmp:dungeon/tier1, tier2, tier3 and legendary (minus
 * nusmp:dungeon/excluded), as plain entries so that choosing among them is tested without the game. The glue (Wildlife) reads the tags and
 * builds this; a modpack edits the tags, never this class. Weights make the common small things common and the strong ones rare, and an
 * entry that belongs to a tier of the Dungeon (a salamander to the Volcanic Vents, a sea serpent to the Deep Waterways) is likelier there.
 * Pure.
 */
public final class Roster {
    /** The tiers of the Dungeon by the ordinal of dungeon.DungeonTier. */
    public static final int UPPER = 0, DEEP = 1, VENTS = 2, CITY = 3;

    /** One member: its id ("tensura:slime"), its tier 1-3 (a legendary one is 3), and what the tags say about it. */
    public record Entry(String id, int tier, boolean legendary, boolean aquatic, boolean grazer) {
        public String path() {
            int i = id.indexOf(':');
            return i < 0 ? id : id.substring(i + 1);
        }

        public String namespace() {
            int i = id.indexOf(':');
            return i < 0 ? "minecraft" : id.substring(0, i);
        }
    }

    private final List<Entry> entries;

    public Roster(List<Entry> entries) { this.entries = List.copyOf(entries); }

    public List<Entry> all() { return entries; }

    /** How many ordinary (not legendary) members this tier has. */
    public int count(int tier) {
        int n = 0;
        for (Entry e : entries) if (e.tier == tier && !e.legendary) n++;
        return n;
    }

    public int legendCount() {
        int n = 0;
        for (Entry e : entries) if (e.legendary) n++;
        return n;
    }

    public int countNamespace(String namespace) {
        int n = 0;
        for (Entry e : entries) if (e.namespace().equals(namespace)) n++;
        return n;
    }

    /** Does this tier have a creature that lives in water? */
    public boolean hasAquatic(int tier) {
        for (Entry e : entries) if (e.tier == tier && !e.legendary && e.aquatic) return true;
        return false;
    }

    public boolean contains(int tier, String id) {
        for (Entry e : entries) if (e.tier == tier && !e.legendary && e.id.equals(id)) return true;
        return false;
    }

    // ---------------------------------------------------------------- tier arithmetic
    /** The tier a creature has from its tags: 0 if it is vetoed or in none, legendary counts as 3, the highest listed tier wins. */
    public static int tierFor(boolean tier1, boolean tier2, boolean tier3, boolean legendary, boolean excluded) {
        if (excluded) return 0;
        if (legendary || tier3) return 3;
        if (tier2) return 2;
        return tier1 ? 1 : 0;
    }

    // ---------------------------------------------------------------- choosing
    private static final int DEFAULT_WEIGHT = 10;
    private static final Map<String, Integer> WEIGHTS = Map.ofEntries(
            // tier 1: common, small
            Map.entry("slime", 30), Map.entry("metal_slime", 2), Map.entry("horned_rabbit", 14), Map.entry("hell_caterpillar", 8),
            Map.entry("goblin", 12), Map.entry("giant_bat", 12), Map.entry("cattledeer", 10), Map.entry("one_eyed_owl", 8),
            Map.entry("winged_cat", 6), Map.entry("aqua_frog", 10), Map.entry("landfish", 8), Map.entry("hover_lizard", 8),
            Map.entry("giant_cod", 8), Map.entry("giant_salmon", 8), Map.entry("pixie", 6), Map.entry("hippocampus", 6),
            Map.entry("hippogryph", 5), Map.entry("stymphalian_bird", 8), Map.entry("death_worm", 6), Map.entry("myrmex_worker", 10),
            Map.entry("dread_thrall", 6),
            // tier 2: armoured and middling
            Map.entry("armorsaurus", 14), Map.entry("lizardman", 10), Map.entry("orc", 10), Map.entry("hound_dog", 10),
            Map.entry("hell_moth", 8), Map.entry("giant_ant", 8), Map.entry("army_wasp", 8), Map.entry("horned_bear", 8),
            Map.entry("barghest", 6), Map.entry("leech_lizard", 7), Map.entry("salamander", 5), Map.entry("undine", 5),
            Map.entry("sylphide", 5), Map.entry("dragon_peacock", 5), Map.entry("feathered_serpent", 5), Map.entry("phantaspore", 5),
            Map.entry("akash", 5), Map.entry("supermassive_slime", 4), Map.entry("pegasus", 4), Map.entry("pegacorn", 3),
            Map.entry("unicorn", 3), Map.entry("spear_toro", 6), Map.entry("troll", 6), Map.entry("amphithere", 5),
            Map.entry("cockatrice", 5), Map.entry("siren", 5), Map.entry("ghost", 5), Map.entry("myrmex_soldier", 8),
            Map.entry("myrmex_royal", 3), Map.entry("dread_ghoul", 6), Map.entry("dread_beast", 5), Map.entry("dread_scuttler", 5),
            // tier 3: apex
            Map.entry("direwolf", 14), Map.entry("basilisk", 12), Map.entry("black_spider", 8), Map.entry("knight_spider", 4),
            Map.entry("blade_tiger", 8), Map.entry("giant_bear", 8), Map.entry("war_gnome", 4), Map.entry("beast_gnome", 4),
            Map.entry("lesser_daemon", 3), Map.entry("evil_centipede", 5), Map.entry("megalodon", 5), Map.entry("sissie", 2),
            Map.entry("sea_serpent", 3), Map.entry("gorgon", 3), Map.entry("cyclops", 3), Map.entry("hydra", 3),
            Map.entry("myrmex_sentinel", 5), Map.entry("myrmex_queen", 1), Map.entry("dread_knight", 3), Map.entry("dread_lich", 2));

    /** The tier of the Dungeon (see UPPER..CITY) a kind of creature belongs to, by the path of its id. Missing = it lives anywhere. */
    private static final Map<String, Integer> AFFINITY = Map.ofEntries(
            Map.entry("salamander", VENTS), Map.entry("hell_moth", VENTS), Map.entry("hell_caterpillar", VENTS), Map.entry("fire_dragon", VENTS),
            Map.entry("aqua_frog", DEEP), Map.entry("undine", DEEP), Map.entry("giant_cod", DEEP), Map.entry("giant_salmon", DEEP),
            Map.entry("megalodon", DEEP), Map.entry("sissie", DEEP), Map.entry("spear_toro", DEEP), Map.entry("landfish", DEEP),
            Map.entry("hippocampus", DEEP), Map.entry("siren", DEEP), Map.entry("sea_serpent", DEEP), Map.entry("ice_dragon", DEEP),
            Map.entry("giant_bat", UPPER), Map.entry("one_eyed_owl", UPPER), Map.entry("black_spider", UPPER), Map.entry("ghost", UPPER),
            Map.entry("dread_thrall", UPPER), Map.entry("dread_ghoul", UPPER),
            Map.entry("pegasus", CITY), Map.entry("pegacorn", CITY), Map.entry("unicorn", CITY), Map.entry("sylphide", CITY),
            Map.entry("lesser_daemon", CITY), Map.entry("lightning_dragon", CITY));

    public static int baseWeight(String path) { return WEIGHTS.getOrDefault(path, DEFAULT_WEIGHT); }

    /** The tier of the Dungeon a kind of creature belongs to, or -1 if it lives anywhere. */
    public static int affinity(String path) { return AFFINITY.getOrDefault(path, -1); }

    /** An entry's weight in a given tier of the Dungeon: four times as likely at home, half as likely in the wrong place. */
    public static double weightAt(Entry e, int place) {
        int home = affinity(e.path());
        double w = baseWeight(e.path());
        if (home < 0 || place < 0) return w;
        return home == place ? w * 4.0 : w * 0.5;
    }

    /**
     * One ordinary member of a tier, chosen by weight. {@code place} is the tier of the Dungeon the creature will stand in (or -1 if it does
     * not matter), {@code water} selects the aquatic members instead of the land ones, {@code roll} is a random number in [0, 1).
     * Null if the tier has none.
     */
    public Entry pick(int tier, int place, boolean water, double roll) {
        List<Entry> pool = new ArrayList<>();
        double total = 0;
        for (Entry e : entries) {
            if (e.tier != tier || e.legendary || e.aquatic != water) continue;
            pool.add(e);
            total += weightAt(e, place);
        }
        if (pool.isEmpty()) return null;
        double at = Math.max(0.0, Math.min(0.999999999, roll)) * total;
        for (Entry e : pool) {
            at -= weightAt(e, place);
            if (at < 0) return e;
        }
        return pool.get(pool.size() - 1);
    }

    /** One legendary member, all equally likely. Null if there is none. */
    public Entry pickLegend(double roll) {
        List<Entry> pool = new ArrayList<>();
        for (Entry e : entries) if (e.legendary) pool.add(e);
        if (pool.isEmpty()) return null;
        int i = (int) (Math.max(0.0, Math.min(0.999999999, roll)) * pool.size());
        return pool.get(i);
    }

    /** The tier of the Dungeon a legendary creature lives in: the fire dragon in the Volcanic Vents, the ice dragon in the Deep Waterways, anything else in the Golden City. */
    public static int legendHome(String path) {
        if (path.contains("fire")) return VENTS;
        if (path.contains("ice")) return DEEP;
        return CITY;
    }
}
