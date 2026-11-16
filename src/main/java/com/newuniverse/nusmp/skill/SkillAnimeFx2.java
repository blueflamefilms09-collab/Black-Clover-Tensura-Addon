package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.heroweapon.HeroColors;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules;
import com.newuniverse.nusmp.skill.rules.DbRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

/**
 * Why: the one place wave 4 of the skill effects is sent from (docs/vfx_remake/skill_anime_wave4.md): the four style-driven shapes drawn by {@code SkillAnime2Layer} (ANIME2_SKILL, ANIME2_BEAST, ANIME2_HERO,
 * ANIME2_GRIMOIRE) for the wave 3 leftovers, the 27 Beast Will beasts, the 21 hero weapons and the weakest grimoires. Same rules as {@link SkillAnimeFx}: ONE packet per cast through {@link VfxSpawn} (the per-tick
 * effect budget, players within 96 blocks only), sent next to the old effects, which stay (they are what the client setting {@code skills.classicVfx2} shows), never per tick, never per target, never throws.
 * The repeating senders (Hawk Eye / Spirit Card pulses, Hati's hits, a grimoire's impacts, Book Maker's screws, a hero skill) go through {@link Throttle}: at most N packets per window per player, the
 * table is bounded and a player's entry is just a pair of numbers. The power numbers below are read back by {@code vfx.client.layer.SkillAnime2Vfx} (this class may not name it: it is client code);
 * SkillAnime2VfxAssetTest pins the two against each other. Worst case: one packet per cast, at most {@code 4} screws per press, one packet per 3 seconds per pulsing aura.
 */
public final class SkillAnimeFx2 {
    private SkillAnimeFx2() {}

    /** A power carries a mode in steps of this much (SkillAnime2Vfx.STEP). */
    public static final float STEP = 20f;
    /** ANIME2_SKILL's styles, in the order of SkillAnime2Vfx.SKILL_STYLES (power = style + 1). */
    public static final int LIARIS = 0, HATI = 1, HAWK = 2, SPIRIT_CARD = 3, SUMMON = 4, SHISHOU = 5, GATE_ARMOR = 6, GATE_VIMANA = 7, GATE_KING = 8, GATE_ELIXIR = 9, DEVIL_KICK = 10, ALMIGHTY = 11,
            VISION_STAR = 12, VISION_VOID = 13, VISION_SELF = 14;
    /** ANIME2_BEAST's families, in the order of SkillAnime2Vfx.BEAST_FAMILIES. */
    public static final int LINE = 0, RUSH = 1, BURST = 2, VOLLEY = 3, SEAL = 4, AURA = 5;
    /** ANIME2_HERO's motifs, in the order of SkillAnime2Vfx.HERO_MOTIFS. */
    public static final int SLASH = 0, THRUST = 1, SHOCK = 2, WARD = 3, CAST = 4, THREAD = 5, SHARD = 6;
    /** ANIME2_GRIMOIRE's attributes, in the order of SkillAnime2Vfx.GRIMOIRE_STYLES. */
    public static final int COTTON = 0, MIRROR = 1, PLANT = 2, SEALING = 3, THREAD_BOOK = 4;
    /** The beast elements, in the order of SkillAnime2Vfx.ELEMENTS. */
    public static final String[] ELEMENTS = {"FIRE", "WATER", "ICE", "LIGHTNING", "EARTH", "WIND", "DARK", "LIGHT", "POISON", "NATURE", "SPIRIT", "VOID"};
    /** No effect lasts longer than this (ticks). */
    static final int MAX_TICKS = 200;

    // ---------------------------------------------------------------- the power encodings
    /** ANIME2_SKILL: style + 1, plus a radius (0..49) as hundredths. */
    public static float skillPower(int style, double extra) { return style + 1 + (float) Math.max(0.0, Math.min(49.0, extra)) / 100f; }

    /** ANIME2_BEAST: family * 20 + element, plus a half for a finisher. */
    public static float beastPower(int family, int element, boolean finisher) { return family * STEP + Math.max(0, Math.min(ELEMENTS.length - 1, element)) + (finisher ? 0.5f : 0f); }

    /** ANIME2_HERO: motif * 20 + variant * 2 + ultimate. */
    public static float heroPower(int motif, int variant, boolean ultimate) { return motif * STEP + Math.max(0, Math.min(8, variant)) * 2f + (ultimate ? 1f : 0f); }

    /** ANIME2_GRIMOIRE: attribute * 20 + phase * 8 + scale (0..3). */
    public static float grimPower(int attribute, boolean impact, double scale) { return attribute * STEP + (impact ? 8f : 0f) + (float) Math.max(0.0, Math.min(3.0, scale)); }

    /** The beast's element (index of {@link #ELEMENTS}) by the key of its Beast Will spec. An unknown key reads as the last element (VOID). */
    public static int beastElement(String key) {
        if (key == null) return 11;
        return switch (key) {
            case "dragon", "flame_serpent", "cerberus" -> 0;
            case "jormungandr", "kraken", "hafgufa", "scylla_charybdis", "yamata_no_orochi" -> 1;
            case "fenrir" -> 2;
            case "thunder_fang_chimera", "nue" -> 3;
            case "crested_boar", "minotaur" -> 4;
            case "wind_skimmer", "typhon" -> 5;
            case "shadow_panther", "ammit" -> 6;
            case "lion", "pegasus" -> 7;
            case "hydra", "nidhoggr" -> 8;
            case "elderwood_guardian", "cu_sith" -> 9;
            case "ridge_wolf", "kitsune" -> 10;
            default -> 11;                                                                                          // sphinx, apophis (and any beast a later build adds)
        };
    }

    /** The family of a Dragon Ball primitive's move (the Beast Will moves use the same primitives). */
    public static int beastFamily(DbRules.Prim prim) {
        if (prim == null) return LINE;
        return switch (prim) {
            case RUSH -> RUSH;
            case BLAST, DETONATE -> BURST;
            case BARRAGE -> VOLLEY;
            case STOP, SEAL, PSYCHIC -> SEAL;
            case GUARD, VEIL, SCAN, CAPSULE -> AURA;
            default -> LINE;                                                                                        // BEAM, DISC (and a passive, which never reaches here)
        };
    }

    /** The motif of a hero weapon and its variant: [motif, variant]. */
    public static int[] heroLook(HeroKind kind) {
        return switch (kind) {
            case SHIELD -> new int[]{WARD, 0};
            case SWORD, AXE -> new int[]{SLASH, 0};
            case CLAW -> new int[]{SLASH, 1};
            case FAN -> new int[]{SLASH, 2};
            case SCYTHE -> new int[]{SLASH, 3};
            case KATANA -> new int[]{SLASH, 4};
            case SPEAR -> new int[]{THRUST, 0};
            case HARPOON -> new int[]{THRUST, 1};
            case BOW -> new int[]{THRUST, 2};
            case PROJECTILE -> new int[]{THRUST, 3};
            case HAMMER -> new int[]{SHOCK, 0};
            case GAUNTLET -> new int[]{SHOCK, 1};
            case CARRIAGE -> new int[]{SHOCK, 2};
            case STAFF -> new int[]{CAST, 0};
            case BOOK -> new int[]{CAST, 1};
            case INSTRUMENT -> new int[]{CAST, 2};
            case WHIP -> new int[]{THREAD, 0};
            case CHAIN -> new int[]{THREAD, 1};
            case SEWING_SET -> new int[]{THREAD, 2};
            case MIRROR -> new int[]{SHARD, 0};
            default -> new int[]{SLASH, 0};                                                                         // a weapon a later build appends reads as a blade until it gets its own row
        };
    }

    static int ticks(int t) { return Math.max(1, Math.min(MAX_TICKS, t)); }

    // ---------------------------------------------------------------- the throttle
    /** At most {@code max} packets per {@code window} ticks per player; the table never holds more than {@link #CAP} players (it is cleared, then refills with the active ones). */
    static final class Throttle {
        static final int CAP = 512;
        private final HashMap<UUID, long[]> seen = new HashMap<>();

        boolean allow(UUID who, long now, int window, int max) {
            if (who == null) return false;
            if (seen.size() > CAP) seen.clear();
            long[] e = seen.get(who);
            if (e == null || now < e[0] || now - e[0] >= window) {
                seen.put(who, new long[]{now, 1});
                return true;
            }
            if (e[1] >= max) return false;
            e[1]++;
            return true;
        }
    }

    private static final Throttle LIARIS_T = new Throttle(), HATI_T = new Throttle(), HAWK_T = new Throttle(), SPIRIT_T = new Throttle(), GRIM_CAST_T = new Throttle(), GRIM_HIT_T = new Throttle(),
            HERO_T = new Throttle(), SCREW_T = new Throttle(), BEAST_T = new Throttle(), SHISHOU_T = new Throttle();

    // ---------------------------------------------------------------- sending
    static void send(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int argb, int duration, float power) {
        if (level == null || shape == null || from == null || to == null) return;
        try {
            com.newuniverse.nusmp.vfx.VfxSpawn.send(level, shape, from, to, argb, ticks(duration), power);
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only: a closed connection must not break the cast
        }
    }

    static void follow(ServerLevel level, VfxShape shape, net.minecraft.world.entity.Entity who, Vec3 to, int argb, int duration, float power) {
        if (level == null || shape == null || who == null || to == null) return;
        try {
            com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(level, shape, who, to, argb, ticks(duration), power);
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only
        }
    }

    /** An ANIME2_SKILL style at {@code at}; the radius rides on the from -> to distance. */
    public static void skill(ServerLevel level, int style, Vec3 at, double radius, int argb, int duration) {
        if (at == null) return;
        send(level, VfxShape.ANIME2_SKILL, at, at.add(Math.max(0.8, Math.min(24.0, radius)), 0, 0), argb, duration, skillPower(style, 0));
    }

    // ---------------------------------------------------------------- wave 3 leftovers
    public static void liaris(ServerPlayer p) {
        if (p == null || !LIARIS_T.allow(p.getUUID(), p.level().getGameTime(), 20, 1)) return;
        skill(p.serverLevel(), LIARIS, p.position(), 2.0, 0xFFFF5080, 30);
    }

    public static void hati(ServerLevel level, LivingEntity owner) {
        if (level == null || owner == null || !HATI_T.allow(owner.getUUID(), level.getGameTime(), 20, 1)) return;
        skill(level, HATI, owner.position(), 2.0, 0xFFFF8A2A, 24);
    }

    /** Hawk Eye's pulse: the scanning ring and the eye, at most once every 60 ticks (the pulse itself is every 20). */
    public static void hawk(ServerPlayer p, double radius) {
        if (p == null || !HAWK_T.allow(p.getUUID(), p.level().getGameTime(), 60, 1)) return;
        skill(p.serverLevel(), HAWK, p.position(), Math.max(4.0, Math.min(24.0, radius)), 0xFFFFE070, 40);
    }

    /** Spirit Card's pulse: the spinning card and the healing rings, at most once every 60 ticks. */
    public static void spiritCard(ServerPlayer p, double radius) {
        if (p == null || !SPIRIT_T.allow(p.getUUID(), p.level().getGameTime(), 60, 1)) return;
        skill(p.serverLevel(), SPIRIT_CARD, p.position(), Math.max(2.0, Math.min(16.0, radius)), 0xFF70FFA0, 36);
    }

    /** A summon card: gold for the beast and stone cards, violet for the wraith, orange for the legion; one packet per card cast. */
    public static void summon(ServerPlayer p, boolean wraith, boolean legion) {
        if (p == null) return;
        skill(p.serverLevel(), SUMMON, p.position(), legion || wraith ? 3.5 : 2.5, wraith ? 0xFFB070FF : legion ? 0xFFFF9050 : 0xFFFFC860, 34);
    }

    /** The Teacher's analysis rings; {@code mode} is the Shishou mode (one pip per mode, at most 12). */
    public static void shishou(ServerPlayer p, int mode) {
        if (p == null || !SHISHOU_T.allow(p.getUUID(), p.level().getGameTime(), 20, 1)) return;
        skill(p.serverLevel(), SHISHOU, p.position(), 1 + Math.max(0, Math.min(12, mode)), 0xFF70C8FF, 26);
    }

    public static void gateArmor(ServerPlayer p) { if (p != null) skill(p.serverLevel(), GATE_ARMOR, p.position(), 2.0, 0xFFFFD060, 30); }

    /** The Vimana's flight from the caster's chest along the look vector (8 blocks). */
    public static void gateVimana(ServerPlayer p) {
        if (p == null) return;
        Vec3 from = p.position().add(0, 1.0, 0), to = from.add(p.getViewVector(1.0f).scale(8.0));
        send(p.serverLevel(), VfxShape.ANIME2_SKILL, from, to, 0xFFFFD060, 30, skillPower(GATE_VIMANA, 0));
    }

    public static void gateKing(ServerPlayer p, double radius) { if (p != null) skill(p.serverLevel(), GATE_KING, p.position(), Math.max(2.0, Math.min(24.0, radius)), 0xFFFFD060, 34); }

    public static void gateElixir(ServerPlayer p) { if (p != null) skill(p.serverLevel(), GATE_ELIXIR, p.position(), 2.0, 0xFFC8FF80, 30); }

    /** Shinra's Devil's Kick: from the hip to the reach of the foot. */
    public static void devilKick(ServerPlayer p, double reach) { devilKick(p, reach, 0xFFFF5A1E); }

    /** The same kick in another flame colour (Hysterical Strength: blue). */
    public static void devilKick(ServerPlayer p, double reach, int argb) {
        if (p == null) return;
        Vec3 hip = p.position().add(0, 1.0, 0), foot = p.position().add(p.getViewVector(1.0f).scale(Math.max(1.5, Math.min(6.0, reach)))).add(0, 0.4, 0);
        send(p.serverLevel(), VfxShape.ANIME2_SKILL, hip, foot, argb, 16, skillPower(DEVIL_KICK, 0));
    }

    public static void almighty(ServerPlayer p) { if (p != null) skill(p.serverLevel(), ALMIGHTY, p.position(), 3.0, 0xFFFFE0A0, 44); }

    /** The Visionary's meteor from the sky to the ground; the radius rides as the style's extra. */
    public static void visionStar(ServerLevel level, Vec3 sky, Vec3 ground, double radius) {
        send(level, VfxShape.ANIME2_SKILL, sky, ground, 0xFFFFA0E0, 34, skillPower(VISION_STAR, radius));
    }

    public static void visionVoid(ServerLevel level, Vec3 at, double radius) { skill(level, VISION_VOID, at, Math.max(2.0, Math.min(16.0, radius)), 0xFF8A4FD8, 50); }

    public static void visionSelf(ServerPlayer p) { if (p != null) skill(p.serverLevel(), VISION_SELF, p.position(), 2.0, 0xFFFFD0F0, 36); }

    /** One of Book Maker's screws on a pinned target: at most 4 per press (a barrage can pin a whole cone). Re-uses the wave 3 screw. */
    public static void screw(ServerPlayer p, LivingEntity target) {
        if (p == null || target == null || !SCREW_T.allow(p.getUUID(), p.level().getGameTime(), 10, 4)) return;
        SkillAnimeFx.screwShot(p.serverLevel(), p.getEyePosition().add(0, -0.3, 0), target.position().add(0, target.getBbHeight() * 0.5, 0));
    }

    // ---------------------------------------------------------------- Beast Will
    /** One Beast Will move: the move's primitive picks the family, the beast's key picks the element. {@code finisher}: the mastery move (bigger, with a rune halo). */
    public static void beast(ServerPlayer p, BeastSkillRules.Spec spec, DbRules.Move mv, boolean finisher) {
        if (p == null || spec == null || mv == null || !BEAST_T.allow(p.getUUID(), p.level().getGameTime(), 6, 1)) return;
        try {
            beastSend(p, spec, mv, finisher);
        } catch (RuntimeException ignored) {
            // cosmetic only
        }
    }

    private static void beastSend(ServerPlayer p, BeastSkillRules.Spec spec, DbRules.Move mv, boolean finisher) {
        ServerLevel level = p.serverLevel();
        int family = beastFamily(mv.prim());
        float power = beastPower(family, beastElement(spec.key()), finisher);
        double range = Math.max(6.0, Math.min(32.0, mv.range() > 0 ? mv.range() : 12.0));
        Vec3 chest = p.getEyePosition().add(0, -0.3, 0), aim = DMUtil.lookPoint(p, range);
        int duration = finisher ? 36 : 30;
        int rgb = spec.rgb() | 0xFF000000;
        if (family == LINE || family == RUSH || family == VOLLEY) {
            send(level, VfxShape.ANIME2_BEAST, chest, aim, rgb, duration, power);
        } else {
            double radius = Math.max(1.5, Math.min(family == AURA ? 12.0 : 10.0, mv.radius() > 0 ? mv.radius() : 3.0));
            Vec3 centre = family == AURA || mv.range() <= 3.0 ? p.position() : aim;
            send(level, VfxShape.ANIME2_BEAST, centre, centre.add(radius, 0, 0), rgb, duration, power);
        }
    }

    // ---------------------------------------------------------------- hero weapons
    /** One hero weapon technique: the motif of the weapon, tinted in its colour; {@code ultimate}: the form's strongest technique. */
    public static void hero(ServerPlayer p, HeroKind kind, boolean ultimate) {
        if (p == null || kind == null || !HERO_T.allow(p.getUUID(), p.level().getGameTime(), 4, 1)) return;
        try {
            int[] look = heroLook(kind);
            Vec3 chest = p.position().add(0, 1.3, 0);
            send(p.serverLevel(), VfxShape.ANIME2_HERO, chest, DMUtil.lookPoint(p, 10.0), HeroColors.argb(kind), ultimate ? 30 : 26, heroPower(look[0], look[1], ultimate));
        } catch (RuntimeException ignored) {
            // cosmetic only
        }
    }

    // ---------------------------------------------------------------- grimoires
    /** The cast: the attribute's sigil under the caster (follows); at most 2 per 6 ticks per player. */
    public static void grimCast(ServerPlayer p, int attribute, int argb, float scale) {
        if (p == null || !GRIM_CAST_T.allow(p.getUUID(), p.level().getGameTime(), 6, 2)) return;
        follow(p.serverLevel(), VfxShape.ANIME2_GRIMOIRE, p, p.position().add(0, 1, 0), argb, 22, grimPower(attribute, false, scale));
    }

    /** The impact where the spell lands; at most 3 per 6 ticks per player (an area spell may call it for every target). */
    public static void grimImpact(ServerPlayer p, int attribute, int argb, Vec3 at, float scale) {
        if (p == null || at == null || !GRIM_HIT_T.allow(p.getUUID(), p.level().getGameTime(), 6, 3)) return;
        send(p.serverLevel(), VfxShape.ANIME2_GRIMOIRE, at, at.add(0, 1, 0), argb, 20, grimPower(attribute, true, scale));
    }

    // ------------------------------------------------------------------ CI server smoke test (prefix skill.vfx2 in SMOKE_EXPECT)
    private static boolean registered;

    public static void registerSelfTests() {
        if (registered) return;
        registered = true;
        SelfTests.register("skill.vfx2", SkillAnimeFx2::selfTest);
    }

    /** Every ANIME2_ shape goes out through the effect budget on a dedicated server without throwing, the four shapes follow SKILL_SIGIL in order, the encodings and tables hold. */
    private static SelfTests.Result selfTest(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        int sent = 0;
        for (VfxShape s : VfxShape.values()) {
            if (!s.name().startsWith("ANIME2_")) continue;
            send(level, s, at, at.add(0, 0, 6), 0xFFFFE7A0, 10, 1f);
            sent++;
        }
        if (sent != 4) return SelfTests.Result.fail("expected the 4 ANIME2_ shapes, found " + sent);
        VfxShape[] all = VfxShape.values();
        int skillIndex = VfxShape.ANIME2_SKILL.ordinal();
        if (skillIndex <= VfxShape.SKILL_SIGIL.ordinal() || VfxShape.ANIME2_BEAST.ordinal() != skillIndex + 1 || VfxShape.ANIME2_HERO.ordinal() != skillIndex + 2 || VfxShape.ANIME2_GRIMOIRE.ordinal() != skillIndex + 3
                || all.length <= VfxShape.ANIME2_GRIMOIRE.ordinal()) return SelfTests.Result.fail("the wave 4 shapes moved (they must follow SKILL_SIGIL, in order)");
        if (Math.abs(skillPower(VISION_STAR, 9) - 13.09f) > 1e-4f || beastPower(BURST, 4, true) != 44.5f || heroPower(THRUST, 2, true) != 25f || grimPower(PLANT, true, 1.0) != 49f) return SelfTests.Result.fail("the power encoding drifted");
        if (beastElement("dragon") != 0 || beastElement("apophis") != 11 || beastElement("kraken") != 1 || beastElement(null) != 11) return SelfTests.Result.fail("the beast element table drifted");
        if (beastFamily(DbRules.Prim.BEAM) != LINE || beastFamily(DbRules.Prim.RUSH) != RUSH || beastFamily(DbRules.Prim.BLAST) != BURST || beastFamily(DbRules.Prim.GUARD) != AURA) return SelfTests.Result.fail("the beast family table drifted");
        for (HeroKind k : HeroKind.values()) {
            int[] look = heroLook(k);
            if (look[0] < 0 || look[0] > SHARD || look[1] < 0 || look[1] > 4) return SelfTests.Result.fail("the hero weapon look of " + k + " is out of range");
        }
        for (BeastSkillRules.Spec s : BeastSkillRules.SPECS) {
            int e = beastElement(s.key());
            if (e < 0 || e >= ELEMENTS.length) return SelfTests.Result.fail("the beast " + s.key() + " has no element");
        }
        Throttle t = new Throttle();
        UUID who = new UUID(1L, 2L);
        if (!t.allow(who, 100, 10, 2) || !t.allow(who, 101, 10, 2) || t.allow(who, 102, 10, 2) || !t.allow(who, 110, 10, 2)) return SelfTests.Result.fail("the throttle drifted");
        skill(level, SHISHOU, at, 3, 0xFF70C8FF, 10);
        visionStar(level, at.add(8, 40, 0), at, 9);
        return SelfTests.Result.ok("the 4 wave 4 shapes sent through VfxSpawn, appended after SKILL_SIGIL; the encodings, the beast and hero tables and the throttle hold");
    }
}
