package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: seven bosses stood in this mod long before the roster of the arcs existed (the Dark Triad, Lucius, the Devil Avatar, the Goliath and the Charybdis
 * Spawn), they are the first bosses a player meets and the prerequisites of the characters the other builders add (the Spade Kingdom's Morris and Lucifero
 * follow "the Triad", the wizard-king saga follows it too), and since the old Living Dungeon went away (1.58.0) the Goliath and the Charybdis Spawn have no
 * home at all. They are not {@code ArcBoss} classes and must not become one (their fights are tested and balanced as they are: "no new fight code"), so
 * each gets only its pure {@link ArcCharacter} record here, registered in the roster under the SAME id the boss registry and the Charter table already use,
 * and the game side ({@code ExistingBossTypes}) maps that id to the entity type that was always registered. Everything the roster wants is read from the
 * boss's own data: stats from the attributes its class registers, the Charter Grimoire from {@link CharterTable} (so the two can never disagree), colours
 * from the grimoire's cover.
 *
 * <p>Left out on purpose: the God of Order (a visit of the Order's ledger: its fight ends by its own Erasure after 75 seconds, it leaves instead of dying, so a
 * story that waits for its defeat could never go on), the Blood-Red Commander (its Monarch's Vault went with the old dungeon, no entity is left), the Floor
 * Warden and the Labyrinth Guardian (the dungeon's own wardens, not characters), and Zagred (the ZAG builder replaces his class under the same entity id
 * and registers him itself). A boss that stands here is still free to be summoned by its old command and egg.
 *
 * <p>The story order is the old dungeon's pair first (open from the start), the Avatar after both, the Triad after Zagred (an id nobody may have registered
 * yet: the roster skips it, so the Triad is open from the start until the Zagred remake is merged), Lucius after the Triad.
 */
public final class ExistingBosses {
    private ExistingBosses() {}

    /** The id of the character whose fight lives in the arena of the Final Hour (the Hourglass's dimension), not in a story arena of the Chronicle. */
    public static final String FINAL_HOUR_ARENA = "zogratis_arena";

    // ---- the old dungeon: the Living Dungeon's floor bosses, floors 10 to 30 (a boss floor is every tenth), open from the start
    public static final ArcCharacter GOLIATH = ArcCharacter.boss("goliath", "Goliath")
            .title("the Giant of the Pit").source("DanMachi").saga(ArcSaga.DANMACHI).tier(2).colors(0x6B5A3C, 0xE3C98A)
            .magic("earth").stats(1800, 32, 0.20, 13).floors(null, 10, 30).themes("gravity", "flame", "dwarven_forgehold", "crystal_geode")
            .arena("orario_pit").charter(0.40, charterSkills("goliath")).build();

    public static final ArcCharacter CHARYBDIS_SPAWN = ArcCharacter.boss("charybdis_spawn", "Spawn of Charybdis")
            .title("the Whirlpool").source("Tensura").saga(ArcSaga.DANMACHI).tier(2).colors(0x1D4F9C, 0x9AD8FF)
            .magic("water").stats(1350, 25, 0.30, 7).floors(null, 10, 30).themes("frost", "storm", "sunken_temple", "drowned_crypt")
            .arena("orario_pit").charter(0.40, charterSkills("charybdis_spawn")).build();

    /** The devil's Avatar from the Underworld Gate: it follows both pit bosses and belongs to the devil arc of the Spade saga. */
    public static final ArcCharacter DEVIL_AVATAR = ArcCharacter.boss("devil_avatar", "Devil Avatar")
            .title("the Walking Devil").source("Black Clover").saga(ArcSaga.SPADE_RAID).tier(3).colors(0x2A2024, 0xFF6A1A)
            .devil(Devil.LUCIFERO).magic("gravity").stats(1100, 15, 0.34, 9).floors(Polarity.ABYSSAL, 20, 50)
            .themes("flame", "blood", "abyss", "infernal_foundry").after("goliath", "charybdis_spawn")
            .arena("spade_throne").charter(0.40, charterSkills("devil_avatar")).build();

    // ---- the Dark Triad, rulers of the Spade Kingdom: tier 4 by their saga, whatever their raw health says (the framework's power curve does the rest)
    public static final ArcCharacter DANTE = ArcCharacter.boss("dante", "Dante Zogratis")
            .title("Gravity of Lucifero").source("Black Clover").saga(ArcSaga.SPADE_RAID).tier(4).colors(0x4B2D8A, 0xC9A7FF)
            .devil(Devil.LUCIFERO).magic("gravity").stats(1700, 18, 0.30, 15).floors(null, 50, 90)
            .themes("gravity", "storm", "sky_citadel").after("zagred")
            .arena("spade_throne").charter(0.25, charterSkills("dante")).build();

    public static final ArcCharacter VANICA = ArcCharacter.boss("vanica", "Vanica Zogratis")
            .title("Curse of Megicula").source("Black Clover").saga(ArcSaga.SPADE_RAID).tier(4).colors(0x9B1230, 0xFF8FA3)
            .devil(Devil.MEGICULA).magic("curse").stats(1300, 13, 0.36, 9).floors(Polarity.ABYSSAL, 50, 90)
            .themes("blood", "shadow", "abyss", "necropolis", "vampire_castle", "haunted_manor", "cursed_archive").after("zagred")
            .arena("spade_throne").charter(0.25, charterSkills("vanica")).build();

    public static final ArcCharacter ZENON = ArcCharacter.boss("zenon", "Zenon Zogratis")
            .title("Space of Beelzebub").source("Black Clover").saga(ArcSaga.SPADE_RAID).tier(4).colors(0x1C7C8C, 0x8CF2FF)
            .devil(Devil.BEELZEBUB).magic("spatial").stats(1350, 15, 0.34, 8).floors(Polarity.HOLY, 50, 90)
            .themes("spatial", "astral", "radiant", "crystal_geode", "celestial_court").after("zagred")
            .arena("spade_throne").charter(0.25, charterSkills("zenon")).build();

    /**
     * Lucius, the Ultimate Wizard King's shadow: never a floor boss (he answers only in the arena of the Final Hour, {@code luciusArenaOnly}), so he has no
     * floors; the Chronicle sends the party to that arena through the Hourglass's own travel instead of building another.
     */
    public static final ArcCharacter LUCIUS = ArcCharacter.boss("lucius", "Lucius Zogratis")
            .title("the Supreme Time Devil's Vessel").source("Black Clover").saga(ArcSaga.WIZARD_KING).tier(5).colors(0xD9B43A, 0xF2E6B0)
            .devil(Devil.ASTAROTH).magic("time").stats(4800, 22, 0.36, 24).after("dante", "vanica", "zenon")
            .arena(FINAL_HOUR_ARENA).charter(0.25, charterSkills("lucius")).build();

    /** The seven, in story order (the order the roster should hold them in). */
    private static final List<ArcCharacter> ALL = List.of(GOLIATH, CHARYBDIS_SPAWN, DEVIL_AVATAR, DANTE, VANICA, ZENON, LUCIUS);

    /** Roster id to the registry path of the entity type the old class has always been registered under. */
    private static final Map<String, String> PATHS = new LinkedHashMap<>();
    static {
        PATHS.put("goliath", "goliath");
        PATHS.put("charybdis_spawn", "charybdis_spawn");
        PATHS.put("devil_avatar", "devil_avatar");
        PATHS.put("dante", "dante_zogratis");
        PATHS.put("vanica", "vanica_zogratis");
        PATHS.put("zenon", "zenon_zogratis");
        PATHS.put("lucius", "lucius_zogratis");
    }

    /** The skills of the Charter row of a boss that already has one (the record never keeps a second copy: a typo here cannot make the grimoire differ). */
    private static String[] charterSkills(String id) {
        CharterTable.Boss row = CharterTable.byId(id);
        if (row == null) throw new IllegalStateException("the Canon table has no row for " + id);
        return row.skills().toArray(new String[0]);
    }

    /** The seven characters, in story order. */
    public static List<ArcCharacter> all() { return new ArrayList<>(ALL); }

    /** Is this id one of the seven (a boss that stands on an old class, not on {@code ArcBoss})? Null-safe. */
    public static boolean owns(String id) { return id != null && PATHS.containsKey(id); }

    /** The entity type path ({@code nusmp:<path>}) of an existing boss, or null. */
    public static String entityPath(String id) { return id == null ? null : PATHS.get(id); }

    /** The roster id of the boss whose entity type has this path, or null. */
    public static String idOfPath(String path) {
        if (path == null) return null;
        for (Map.Entry<String, String> e : PATHS.entrySet()) if (e.getValue().equals(path)) return e.getKey();
        return null;
    }

    /** Puts the seven into the roster, in story order (registering the same instance twice is harmless). The lead calls this from {@code ArcCharacters.bootstrap()}. */
    public static void registerAll() {
        for (ArcCharacter c : ALL) ArcRoster.register(c);
    }
}
