package com.newuniverse.nusmp.celestial;

import java.util.List;

/**
 * Why: the Celestial Spirit King is the capstone of the collection, and the rules of the choice at the Gate and of the fight are numbers: who may open the Gate, who may be
 * recognised instead of fighting, how the king scales with the party and with the EP of the people in the Spirit World, which phase he is in, how many legendary-action
 * points he spends each round, how long a player must wait to challenge him again, and what each outcome gives. They are written without a game class so they can be
 * tested; {@link SpiritKingEntity} and {@link SpiritWorld} only ask. The shape of the fight (phases, legendary actions that refill each round, lair actions, a few
 * legendary resistances) follows the Decennial bosses of the Mega-Dungeon, written for a single arena.
 */
public final class KingRules {
    private KingRules() {}

    /** Ticks of the rounds in which the legendary points refill, of the lair action, and the longest the fight may last before the world collapses. */
    public static final int ROUND_TICKS = 100, LAIR_TICKS = 240, REALM_TICKS = 20 * 60 * 20;
    /** The window in which the choice after the Gate opens may be made. */
    public static final int CHOICE_TICKS = 20 * 90;
    /** Waiting times (ticks) before the same player may face him again: an hour after a victory, five minutes after a defeat or a retreat. */
    public static final long REFIGHT_WIN = 20L * 60 * 60, REFIGHT_LOSS = 20L * 60 * 5;
    /** The most players counted for the king's scaling, and the legendary resistances he shrugs a harmful effect with. */
    public static final int MAX_PARTY = 6, RESISTANCES = 3;

    public enum Choice { CHALLENGE, RECOGNITION }

    public enum Outcome { VICTORY, RECOGNITION }

    // ================================================================ who may do what
    public enum Gate { OK, NOT_ALL_KEYS, NOT_BOUND, COOLDOWN, NO_GATE_KEY, ALREADY_OPEN }

    /** Whether the Gate may be opened now. All twelve gold keys must be in the pack, bound to the player, and the gate key in hand. */
    public static Gate canOpen(int heldMask, int boundMask, boolean gateKey, long now, long cooldownUntil, boolean alreadyOpen) {
        if (alreadyOpen) return Gate.ALREADY_OPEN;
        if (!gateKey) return Gate.NO_GATE_KEY;
        if (!CelestialRules.completes(heldMask)) return Gate.NOT_ALL_KEYS;
        if (!CelestialRules.completes(boundMask)) return Gate.NOT_BOUND;
        if (now < cooldownUntil) return Gate.COOLDOWN;
        return Gate.OK;
    }

    /**
     * Recognition without a fight: the king acknowledges a summoner whose twelve spirits trust them. Every gold spirit must be bound with a bond of at least 2, the skill's mastery
     * must reach 50%, and the player must not already be recognised. (Option C of the brief: an Ultimate-tier mode opens without a battle, with the smaller reward.)
     */
    public static boolean recognitionEarned(int boundMask, int[] bondXp, double masteryFrac, boolean alreadyRecognised) {
        if (alreadyRecognised || !CelestialRules.completes(boundMask) || masteryFrac < 0.5) return false;
        for (CelestialKey k : CelestialKey.gold()) if (bondXp == null || bondXp.length <= k.ordinal() || CelestialRules.bondLevel(bondXp[k.ordinal()]) < 2) return false;
        return true;
    }

    /** Ticks until the same player may challenge again. */
    public static long refightCooldown(boolean victory) { return victory ? REFIGHT_WIN : REFIGHT_LOSS; }

    // ================================================================ scaling
    /** The king's health: 600 half hearts per point of EP scale (1..12), plus 75% for every player past the first (at most {@link #MAX_PARTY}), at least 1,200. */
    public static double health(double epScale, int party) {
        double scale = Math.max(1.0, Math.min(12.0, epScale));
        return Math.max(1200.0, 600.0 * scale * partyHealth(party));
    }

    public static double partyHealth(int party) { return 1.0 + 0.75 * (clampParty(party) - 1); }

    /** Damage multiplier on the king's blows: +12% per extra player, so a crowd is hit harder but never per head. */
    public static double partyDamage(int party) { return 1.0 + 0.12 * (clampParty(party) - 1); }

    /** The king's base blow (HP) from the EP scale: 14 at scale 1 up to 14 * sqrt(12). */
    public static double attack(double epScale) { return 14.0 * Math.sqrt(Math.max(1.0, Math.min(12.0, epScale))); }

    /** A blow that lands on a player is at most this share of the player's maximum health ("no one-shots"). */
    public static final double PLAYER_HIT_CAP = 0.45;

    public static float cap(float damage, float targetMaxHealth) { return Math.min(damage, targetMaxHealth * (float) PLAYER_HIT_CAP); }

    private static int clampParty(int n) { return Math.max(1, Math.min(MAX_PARTY, n)); }

    // ================================================================ phases and actions
    /** The phase from the health fraction: 1 above two thirds, 2 above one third, 3 under it. */
    public static int phase(double healthFrac) { return healthFrac > 2.0 / 3.0 ? 1 : healthFrac > 1.0 / 3.0 ? 2 : 3; }

    /** Legendary action points per round: 3, 4 from phase 2, 5 in phase 3, one more for a party of three or more. */
    public static int points(int phase, int party) { return 2 + Math.max(1, Math.min(3, phase)) + (party >= 3 ? 1 : 0); }

    /** One legendary action: id, cost in points, the phase it opens in. */
    public record Action(String id, int cost, int fromPhase) {}

    public static final List<Action> ACTIONS = List.of(
            new Action("star_fall", 1, 1),             // a rain of stars on three marked spots
            new Action("gate_slam", 2, 1),             // the Gate's door slams: a ring that throws everyone back
            new Action("constellation_bind", 2, 2),    // a zodiac chain binds the farthest player to a star
            new Action("zodiac_beam", 2, 2),           // the twelve spirits' beams, one after another around the arena
            new Action("judgement", 3, 3));            // the Spirit King's Judgement: a wide blast with a long telegraph, survivable in the safe sphere

    public static final List<String> LAIR_ACTIONS = List.of("star_tremor", "gate_light", "spirit_wind");

    /** The actions he may use in this phase, in the order of the table. */
    public static List<Action> available(int phase) {
        java.util.ArrayList<Action> out = new java.util.ArrayList<>();
        for (Action a : ACTIONS) if (a.fromPhase() <= phase) out.add(a);
        return List.copyOf(out);
    }

    /** The first action in {@code available(phase)} that {@code points} can pay, starting at {@code start} (wraps), or null. */
    public static Action pick(int phase, int points, int start) {
        List<Action> av = available(phase);
        if (av.isEmpty()) return null;
        for (int i = 0; i < av.size(); i++) {
            Action a = av.get(Math.floorMod(start + i, av.size()));
            if (a.cost() <= points) return a;
        }
        return null;
    }

    /** Whether he shrugs the effect off with a legendary resistance: true while some are left (the caller counts them down). */
    public static boolean resists(int left) { return left > 0; }

    // ================================================================ rewards
    /** What an outcome gives. {@code hp} is half hearts of permanent maximum health, the fractions are permanent shares of maximum magicules and aura. */
    public record Reward(int hp, double magicule, double aura, int bond, boolean sceptre, boolean blessingMode, boolean summonMode, double refill) {}

    public static Reward reward(Outcome o) {
        return switch (o) {
            case VICTORY -> new Reward(8, 0.30, 0.30, 2, true, true, true, 1.0);
            case RECOGNITION -> new Reward(4, 0.12, 0.12, 1, false, true, false, 0.5);
        };
    }

    /** Rewards stack at most once per outcome; the best one wins when both were earned. */
    public static Reward total(boolean won, boolean recognised) {
        if (won && recognised) {
            Reward w = reward(Outcome.VICTORY), r = reward(Outcome.RECOGNITION);
            return new Reward(w.hp() + r.hp(), w.magicule() + r.magicule(), w.aura() + r.aura(), w.bond() + r.bond(), true, true, true, 1.0);
        }
        if (won) return reward(Outcome.VICTORY);
        if (recognised) return reward(Outcome.RECOGNITION);
        return new Reward(0, 0, 0, 0, false, false, false, 0);
    }

    /** The cost of the king's summon (the Ultimate-tier mode) as a share of the maximum magicules, its cooldown and how long he stays. */
    public static final int SUMMON_COST_PERMILLE = 400, SUMMON_COOLDOWN = 20 * 60 * 20, SUMMON_STAY = 20 * 14;
}
