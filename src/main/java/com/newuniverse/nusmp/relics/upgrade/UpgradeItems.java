package com.newuniverse.nusmp.relics.upgrade;

import java.util.List;
import java.util.zip.CRC32;

/**
 * Why: the one list of the weapons that have an upgraded look (mesh, passes, ambient layers, act signatures), shared by the client (which looks the specs up by id) and the server (which
 * sends an act signature only for these). Pure: no Minecraft types. The signature id of an act is the crc32 of "item:act", masked to 31 bits, so the client finds the recipe of a packet
 * without a table that both sides would have to keep in step.
 * <p>
 * Slice god_melee_a (1.102.0): the twelve God grade hand weapons of wave 71. Other slices append their ids to {@link #IDS_OTHER} the same way.
 */
public final class UpgradeItems {
    private UpgradeItems() {}

    /** Slice god_melee_a. */
    public static final List<String> GOD_MELEE_A = List.of("staff_of_the_magi", "wizard_kings_vow", "wand_of_the_wisdom_king", "hammer_of_thunderbolts", "gungnir_the_unerring",
            "kusanagi_the_grass_cutter", "leviathans_crown_trident", "calamity_wave_aegis", "hellblaze_the_demon_sin", "minotaur_kings_labrys", "dragon_force_twin_fangs", "dawnpiercer_crossbow");

    /** Where the other slices of the visual upgrade add theirs (append-only). */
    public static final List<String> IDS_OTHER = List.of();

    /** Every upgraded id. */
    public static final List<String> ALL = java.util.stream.Stream.concat(GOD_MELEE_A.stream(), IDS_OTHER.stream()).toList();

    /** Ticks the effect that carries a signature lives (every recipe's last component ends before it; a test checks it). */
    public static final int SIGNATURE_DURATION = 52;

    private static final java.util.regex.Pattern STATE_SUFFIX = java.util.regex.Pattern.compile("_(pull[123]|charged|spent|genesis|5leaf)$");

    public static boolean has(String id) { return id != null && ALL.contains(id); }

    /** The signature id of an act of an item (the server puts it in the packet's seed, the client finds the recipe by it). */
    public static int signatureId(String item, String act) {
        CRC32 c = new CRC32();
        c.update((item + ":" + act).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (int) (c.getValue() & 0x7FFFFFFFL);
    }

    /** Packs a signature id and a random part into the seed of an effect packet. */
    public static long packSeed(int signature, int random) { return ((long) signature << 32) | (random & 0xFFFFFFFFL); }

    /** The signature id carried by an effect packet's seed. */
    public static int signatureOf(long seed) { return (int) (seed >>> 32); }

    /** The id of the base weapon of a state mesh (dawnpiercer_crossbow_pull2 -> dawnpiercer_crossbow); other ids are returned as they are. */
    public static String baseId(String meshId) {
        return meshId == null ? null : STATE_SUFFIX.matcher(meshId).replaceFirst("");
    }
}
