package com.newuniverse.nusmp.boss.rework3;

import com.newuniverse.nusmp.arc.tier.TierMove;
import com.newuniverse.nusmp.arc.tier.TierMove.Basis;
import com.newuniverse.nusmp.arc.tier.TierMove.Effect;
import com.newuniverse.nusmp.arc.tier.TierMove.Kind;

import java.util.List;
import java.util.Map;

/**
 * Boss rework wave 3 (docs/bosses/rework_wave3.md), part 2: one new signature move for each of 16 bosses of the early, midnight and Seven Deadly Sins arcs that fought with
 * plain sprays, as plain data. A row is a {@link TierMove} (a bounded shape: at most 24 foes looked at, a bolt lives at most 40 ticks, at most 5 bolts or marks, no block
 * changed, no entity spawned, no heal) plus the name of the themed {@code VfxShape} that goes out as it lands and its colour. Moves are appended at the end of the boss's move
 * list (the client reads the casting move by index), one per boss, and leave the list again with the server setting {@code bossRework3Moves} off. Damage is the boss's own
 * attack times {@code damage}, through the framework's damage law (never over 45 percent of a victim's maximum health in a hit, never over 6 percent a second).
 *
 * <p>Sources: the wiki could not be reached from the build (rule 10), so the move names are NAMED (the name as the series knows it, from memory) or THEME / FILLER (named and
 * shaped here); none is wiki-verified. Pure: no game types (the shape is a name, checked against the enum by the tests).
 *
 * @param boss  the character id (the arc roster id, {@code ArcCharacter#id})
 * @param shape the name of an existing {@code VfxShape} constant
 */
public final class Rework3Rows {
    private Rework3Rows() {}

    public record Row(String boss, TierMove move, String shape, int rgb, double fxSize) {}

    /** All rows, in the order of the arcs. */
    public static final List<Row> ROWS = List.of(
            // ---- the early arcs
            row("heath_grice", TierMove.of("glacier_bloom", "Glacier Bloom", Kind.WAVE).power(25).wind(26).cd(160).range(0, 14).size(7).dmg(1.1).fx(Effect.FROST, 60).magic().theme(),
                    "ICE_FX1", 0x80DEEA, 1.6),
            row("vetto", TierMove.of("bound_hail", "Bound Hail", Kind.MARKED).power(30).wind(30).cd(170).range(3, 22).size(3).count(3).dmg(1.0).fx(Effect.SLOW, 50).theme(),
                    "SEAL_CHAINS", 0xB0BEC5, 1.5),
            row("lotus_whomalt", TierMove.of("poison_lotus_bloom", "Poison Lotus Bloom", Kind.WAVE).power(25).wind(24).cd(150).range(0, 12).size(6).dmg(1.0).fx(Effect.POISON, 80).magic().theme(),
                    "POISON_CURTAIN", 0x9CCC65, 1.6),
            row("mars", TierMove.of("war_drum_quake", "War Drum Quake", Kind.SLAM).power(30).wind(30).cd(180).range(0, 10).size(6).dmg(1.4).knock(0.9).filler(),
                    "EARTH_RISE", 0xBF8040, 1.6),
            row("revchi_salik", TierMove.of("storm_anvil", "Storm Anvil", Kind.VOLLEY).power(30).wind(26).cd(170).range(4, 26).size(30).count(4).dmg(0.9).magic().filler(),
                    "LIGHTNING_FIEND", 0x90CAF9, 1.5),
            // ---- the Midnight Sun elves
            row("fana", TierMove.of("radiant_verdict", "Radiant Verdict", Kind.BEAM).power(35).wind(34).cd(200).range(4, 30).size(20).dmg(1.4).magic().theme(),
                    "DIVINE_RAY", 0xFFF59D, 1.7),
            row("rhya", TierMove.of("rooted_dirge", "Rooted Dirge", Kind.MARKED).power(35).wind(30).cd(190).range(3, 22).size(3).count(3).dmg(1.1).fx(Effect.SLOW, 60).magic().theme(),
                    "TREE_ROOTS", 0x66BB6A, 1.5),
            row("rades_spirito", TierMove.of("thrall_tide", "Thrall Tide", Kind.SWARM).power(35).wind(28).cd(200).range(0, 20).size(3).count(4).dmg(0.9).magic().theme(),
                    "SOUL_VORTEX", 0x7E57C2, 1.6),
            row("patolli", TierMove.of("loaded_dice", "Loaded Dice", Kind.MARKED).power(30).wind(28).cd(180).range(3, 22).size(3).count(4).dmg(1.0).magic().theme(),
                    "DICE_D20", 0xEF5350, 1.5),
            // ---- the Seven Deadly Sins
            row("meliodas", TierMove.of("hellblaze", "Hellblaze", Kind.WAVE).power(40).wind(30).cd(200).range(0, 14).size(8).dmg(1.5).fx(Effect.BURN, 60).magic().named(),
                    "DEMON_FIRE_FX1", 0x6A1B9A, 1.8),
            row("diane", TierMove.of("foundation", "Foundation", Kind.SLAM).power(35).wind(32).cd(190).range(0, 12).size(7).dmg(1.5).knock(1.0).named(),
                    "EARTH_FISSURE", 0xA1887F, 1.8),
            row("king", TierMove.of("spirit_spear_barrage", "Spirit Spear Barrage", Kind.VOLLEY).power(35).wind(28).cd(180).range(4, 28).size(34).count(5).dmg(0.9).magic().theme(),
                    "FX_SPIRIT_NOVA", 0xFFD54F, 1.5),
            row("escanor", TierMove.of("sunshine", "Sunshine", Kind.BEAM).power(45).wind(36).cd(220).range(4, 30).size(22).dmg(1.6).fx(Effect.BURN, 60).magic().named(),
                    "FIRE_PILLAR", 0xFFB300, 1.8),
            row("ban", TierMove.of("hunter_fest", "Hunter Fest", Kind.CLEAVE).power(35).wind(18).cd(160).range(0, 6).size(5).dmg(1.5).named(),
                    "SLASH_BLADES", 0xE53935, 1.6),
            row("gowther", TierMove.of("nightmare_teller", "Nightmare Teller", Kind.MARKED).power(35).wind(32).cd(190).range(3, 22).size(4).count(2).dmg(1.0).fx(Effect.BLIND, 50).magic().named(),
                    "DREAM_DOME", 0xCE93D8, 1.6),
            row("merlin", TierMove.of("infinity", "Infinity", Kind.MARKED).power(40).wind(32).cd(200).range(3, 24).size(3).count(5).dmg(1.0).magic().theme(),
                    "SPACE_RINGGATE", 0x4FC3F7, 1.6));

    private static Row row(String boss, TierMove.Builder m, String shape, int rgb, double size) { return new Row(boss, m.build(), shape, rgb, size); }

    /** The rows by boss id. */
    public static final Map<String, Row> BY_BOSS = ROWS.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Row::boss, r -> r));

    /** The row of a boss, or null. */
    public static Row of(String bossId) { return bossId == null ? null : BY_BOSS.get(bossId); }

    /** Is this id one of the appended moves (they leave the list with {@code bossRework3Moves} off)? */
    public static boolean is(String moveId) { return ROWS.stream().anyMatch(r -> r.move().id().equals(moveId)); }

    /** Where a row's numbers must stay (read by the tests; the {@code TierMove} caps plus the 6 percent a second and 45 percent a hit rules of the damage law). */
    public static String problem(Row r) {
        TierMove m = r.move();
        if (m.damage() <= 0 || m.damage() > TierMove.MAX_DAMAGE) return r.boss() + ": damage " + m.damage();
        double maxSize = m.kind() == Kind.VOLLEY ? TierMove.MAX_SPREAD : TierMove.MAX_SIZE * 2.0;       // a volley's size is its spread in degrees
        if (m.size() <= 0 || m.size() > maxSize) return r.boss() + ": size " + m.size();
        if (m.count() < 1 || m.count() > TierMove.MAX_COUNT) return r.boss() + ": count " + m.count();
        if (m.effectTicks() > TierMove.MAX_EFFECT_TICKS) return r.boss() + ": effect ticks " + m.effectTicks();
        if (m.knock() > TierMove.MAX_KNOCK) return r.boss() + ": knock " + m.knock();
        if (m.windUp() < 12) return r.boss() + ": no time to see it coming (wind-up " + m.windUp() + ")";
        if (m.cooldown() < 100) return r.boss() + ": cooldown " + m.cooldown();
        if (m.ultimate()) return r.boss() + ": a signature move here is never an ultimate";
        if (m.basis() == Basis.FILLER && r.shape().isEmpty()) return r.boss() + ": no shape";
        return null;
    }
}
