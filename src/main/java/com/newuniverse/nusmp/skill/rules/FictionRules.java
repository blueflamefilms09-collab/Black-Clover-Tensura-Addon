package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Book Maker (Kumagawa) pins a target with screws and drags it down to your level; All Fiction, its Ultimate, makes harm "never have
 * happened" (Undo), erases a target (Fiction) and refuses one death. The five-second health memory behind Undo, the pin numbers and the
 * erase-or-hurt decision are pure, so they are tested without a world.
 */
public final class FictionRules {
    private FictionRules() {}

    // ---- Book Maker ----
    public static final int PIN = 0, BARRAGE = 1;
    public static double pinCost() { return 400; }
    public static int pinCooldown() { return 80; }
    public static double pinDamage(Tier t) { return t == Tier.BASE ? 10 : 16; }
    public static int pinSlowTicks() { return 120; }
    public static int pinWeakTicks() { return 200; }
    public static int pinWeakAmp(Tier t) { return t == Tier.BASE ? 1 : 2; }
    public static double pinRange() { return 24; }
    /** Barrage: pins everything in a cone in front of you (mastered only). */
    public static double barrageCost() { return 1200; }
    public static int barrageCooldown() { return 240; }
    public static double barrageRange() { return 10; }
    public static double barrageCone() { return 0.55; }
    public static boolean barrageUnlocked(Tier t) { return t != Tier.BASE; }
    public static int modes() { return 2; }

    // ---- All Fiction ----
    public static final int UNDO = 0, ERASE = 1, FICTION_DEATH = 2;
    public static final int HISTORY_SECONDS = 5;
    public static double undoCost() { return 1500; }
    public static int undoCooldown() { return 400; }
    public static double eraseCost() { return 4000; }
    public static int eraseCooldown() { return 1200; }
    public static double eraseRange() { return 24; }
    /** Share of max health a boss (or protected player) loses instead of being erased. */
    public static double eraseBossShare() { return 0.25; }
    public static int deathCooldown() { return 18000; }

    /** Index into the health history of the sample to restore: the oldest one remembered. */
    public static int oldestIndex() { return HISTORY_SECONDS - 1; }

    /** The health Undo restores: the remembered value if it is higher than now, never above the maximum. */
    public static float undoHealth(float remembered, float current, float max) {
        if (remembered <= current) return current;
        return Math.min(remembered, max);
    }

    /** Shifts a new sample into a history array (newest first), dropping the oldest. */
    public static void push(float[] history, float sample) {
        for (int i = history.length - 1; i > 0; i--) history[i] = history[i - 1];
        if (history.length > 0) history[0] = sample;
    }

    /** Whether the target is erased outright (true) or only weakened (false). */
    public static boolean erases(boolean fullyAffected) { return fullyAffected; }

    // ---- All Fiction rebuilt (3.46.0): ten modes. Undo (0) and Fiction (1) keep their places and numbers; the passive that refuses a death is unchanged. ----
    public static final int ANNUL = 2, REWIND = 3, UNPICK = 4, NULLIFY = 5, UNMAKE = 6, SHARE = 7, STAND_UP = 8, WORLD = 9;
    public static final int ALL_MODES = 10;
    /** The ids of the ten modes (lang keys anime_multiverse.skill.mode.&lt;id&gt;; the first two are the old ones). */
    public static final String[] ALL_IDS = {"fiction.undo", "fiction.erase", "fiction.annul", "fiction.rewind", "fiction.unpick", "fiction.nullify", "fiction.unmake", "fiction.share", "fiction.standup", "fiction.world"};

    // ---- All Fiction god class (modes 10-15, appended after the ten of 3.46.0; server setting allFictionGod) ----
    // Canon: "All Fiction" (Kumagawa) makes a thing not have happened, so it can be an injury, an effect, a skill, a blow or the time spent. Class design: docs/all_fiction_rework.md.
    public static final int DENY = 10, RULE = 11, REVERSAL = 12, MASS = 13, SKIN = 14, REWRITE_TIME = 15;
    public static final int GOD_MODES = 16;
    /** The ids of the sixteen modes: the ten above first and unchanged. */
    public static final String[] GOD_IDS = {"fiction.undo", "fiction.erase", "fiction.annul", "fiction.rewind", "fiction.unpick", "fiction.nullify", "fiction.unmake", "fiction.share", "fiction.standup", "fiction.world",
            "fiction.deny", "fiction.rule", "fiction.reversal", "fiction.mass", "fiction.skin", "fiction.rewritetime"};

    /** The wheel: sixteen with the god-class modes on, ten with only the rebuild (allFictionGod = false), the old two with the rebuild off too. */
    public static int wheel(boolean rebuilt, boolean god) { return rebuilt ? (god ? GOD_MODES : ALL_MODES) : 2; }

    /** Deny Skill: effects stripped, magic silenced (through the balance law's control time, so a boss is held one second and a player three). */
    public static double denyRange() { return 24.0; }
    public static int denySilenceTicks() { return 300; }
    public static double denyDamage() { return 40.0; }

    /** Overwrite Rule: a zone where a rule is written for 12 s; once a second foes in it are weakened and silenced, allies cleansed, hostile shots erased; the owner takes half in it. */
    public static double ruleRadius() { return 10.0; }
    public static int ruleTicks() { return 240; }
    public static double ruleOwnerTakenMultiplier() { return 0.5; }
    public static int ruleWeakTicks() { return 40; }
    public static int ruleSilenceTicks() { return 40; }
    public static int rulePulseMaxProjectiles() { return 16; }

    /** True when {@code distSq} (blocks squared from the zone centre) is inside the zone. */
    public static boolean inZone(double distSq, double radius) { return distSq <= radius * radius; }

    /** Reversal: for eight seconds up to three hits are made not to have happened and are written back at the striker (through the balance law). */
    public static int reversalTicks() { return 160; }
    public static int reversalCharges() { return 3; }
    public static double reversalMultiplier() { return 1.5; }
    public static double reversalRawCap() { return 60.0; }
    public static double reversalRaw(double hit) { return Math.max(0.0, Math.min(reversalRawCap(), hit * reversalMultiplier())); }
    /** A charge is spent by a hit while the window is open and charges are left. */
    public static boolean reversalOpen(long until, long now, int charges) { return windowOpen(until, now) && charges > 0; }

    /** Mass Fiction (mastered only): every foe within the radius (at most the 24 SkillTargets allows). Ordinary creatures are erased; bosses and players lose their effects and take a hit. */
    public static double massRadius() { return 14.0; }
    public static double massDamage() { return 60.0; }
    public static int massSilenceTicks() { return 200; }
    public static boolean massUnlocked(boolean mastered) { return mastered; }
    public static boolean massErases(boolean isPlayer, boolean fullyAffected) { return !isPlayer && fullyAffected; }

    /** Nothing Sticks: for twenty seconds every second your harmful effects are cleansed. */
    public static int skinTicks() { return 400; }

    /** Rewrite Time: the time spent on these modes never was. The big modes (Fiction, World, Mass, Overwrite Rule, Rewrite Time itself) are not on the list. */
    public static boolean resettable(int mode) {
        return switch (mode) { case UNDO, ANNUL, REWIND, UNPICK, NULLIFY, UNMAKE, SHARE, DENY, REVERSAL, SKIN -> true; default -> false; };
    }

    /** Passive Retcon (once a second): how many harmful effects of the owner are made not to have happened, never more than this. */
    public static int retconPerSecond() { return 1; }

    // ---- Book Maker leads in (modes 2-3, appended; server setting allFictionGod) ----
    public static final int EQUALIZE = 2, GLIMPSE = 3;
    /** Book Maker's wheel: four with the lead-in modes on, the old two (see {@link #modes()}) off. */
    public static int bookMakerModes(boolean god) { return god ? 4 : modes(); }
    public static double equalizeCost() { return 900; }
    public static int equalizeCooldown() { return 160; }
    public static double equalizeRange() { return 24; }
    public static int equalizeStripMax() { return 6; }
    public static int equalizeWeakAmp(Tier t) { return t == Tier.BASE ? 2 : 3; }
    public static int equalizeWeakTicks() { return 200; }
    /** Mastered Book Maker also silences the pinned (through the balance law's control time). */
    public static boolean equalizeSilences(Tier t) { return t != Tier.BASE; }
    /** Glimpse of the Fiction (mastered only): for five seconds a quarter of your maximum health of harm is made not to have happened. */
    public static double glimpseCost() { return 1800; }
    public static int glimpseCooldown() { return 1200; }
    public static int glimpseTicks() { return 100; }
    public static double glimpsePool(double maxHealth) { return Math.max(0.0, maxHealth) * 0.25; }
    public static boolean glimpseUnlocked(Tier t) { return t != Tier.BASE; }

    // ---- Canon pass (Medaka Box wiki, docs/all_fiction_rework.md; server setting allFictionCanon, needs allFictionGod). Appended after everything above. ----
    /** Erase Presence (16: Kumagawa erases his own presence) and April Fiction (17: the erase that lasts three minutes). */
    public static final int PRESENCE = 16, APRIL = 17;
    public static final int CANON_MODES = 18;
    /** The ids of the eighteen modes: the sixteen above first and unchanged. */
    public static final String[] CANON_IDS = {"fiction.undo", "fiction.erase", "fiction.annul", "fiction.rewind", "fiction.unpick", "fiction.nullify", "fiction.unmake", "fiction.share", "fiction.standup", "fiction.world",
            "fiction.deny", "fiction.rule", "fiction.reversal", "fiction.mass", "fiction.skin", "fiction.rewritetime", "fiction.presence", "fiction.april"};

    /** The wheel with the canon pass: eighteen when the rebuild, the god modes and the canon pass are all on; otherwise as {@link #wheel(boolean, boolean)}. */
    public static int wheel(boolean rebuilt, boolean god, boolean canon) { return rebuilt && god && canon ? CANON_MODES : wheel(rebuilt, god); }

    /** Canon: All Fiction cannot erase another Minus (it only removes the unwanted part). With the canon pass off the old rule (the target only has to be fully affected) stands. */
    public static boolean canErase(boolean fullyAffected, boolean canon, boolean targetIsMinus) { return fullyAffected && !(canon && targetIsMinus); }

    /** Canon: nothing real to erase, nothing happens (Ezumachi's trick). Undo has work when it would return health or there is a harmful effect to remove. */
    public static boolean undoHasWork(float remembered, float current, float max, boolean harmfulEffect) { return harmfulEffect || undoHealth(remembered, current, max) > current; }

    /** Erase Presence: invisible for 20 s, and creatures (not bosses, at most the 24 SkillTargets allows) hunting you lose you. */
    public static int presenceTicks() { return 400; }
    public static double presenceRadius() { return 24.0; }

    /** April Fiction: the erase that comes back by itself. A creature's strength is erased (Weakness) for three minutes (canon), a player's for 30 s, a boss's for 20 s; then it returns on its own. */
    public static int aprilTicks(boolean boss, boolean player) { return boss ? 400 : player ? 600 : 3600; }
    public static int aprilMaxTicks() { return 3600; }
    public static int aprilWeakAmp() { return 2; }

    // ---- Book Maker canon pass: the level-down screw ----
    public static final int FOREST = 4;
    /** Book Maker's wheel: five with the canon pass on (Pin, Barrage, Equalize, Glimpse, Leg Eating Forest), else as {@link #bookMakerModes(boolean)}. */
    public static int bookMakerModes(boolean god, boolean canon) { return god && canon ? 5 : bookMakerModes(god); }
    /** Canon: the screw does almost no damage; it only soils the heart. */
    public static double pinDamageCanon() { return 1.0; }
    /** How much of the gap between the target and you is closed: base 60 %, mastered 100 % (all the way down to your level), half of that on a boss. */
    public static double levelStrength(Tier t, boolean boss) { double s = t == Tier.BASE ? 0.6 : 1.0; return boss ? s * 0.5 : s; }
    /** Seconds of the level-down (it reverts when it wears off): 12 s base, 20 s mastered, 5 s on a boss. */
    public static int levelTicks(Tier t, boolean boss) { return boss ? 100 : t == Tier.BASE ? 240 : 400; }
    /** The amount taken off the target's value: the gap above the caster's value times the strength, never below the caster's value and never negative. */
    public static double levelDrop(double targetValue, double casterValue, double strength) { return targetValue > casterValue ? (targetValue - casterValue) * Math.max(0.0, Math.min(1.0, strength)) : 0.0; }
    /** Pin's screw closes half of what Equalize closes. */
    public static double pinLevelScale() { return 0.5; }
    /** Leg Eating Forest (mastered only): every foe within the radius (at most the 24 SkillTargets allows) is pinned by a hidden screw. */
    public static double forestCost() { return 2400; }
    public static int forestCooldown() { return 600; }
    public static double forestRadius() { return 8.0; }
    public static boolean forestUnlocked(Tier t) { return t != Tier.BASE; }

    /** The number of modes on the wheel: ten when the rebuild is on (setting allFictionRebuilt), the old two when it is off. */
    public static int wheel(boolean rebuilt) { return rebuilt ? ALL_MODES : 2; }

    public static double cost(int mode) {
        return switch (mode) {
            case UNDO -> undoCost(); case ERASE -> eraseCost(); case ANNUL -> 2500; case REWIND -> 2000; case UNPICK -> 1500;
            case NULLIFY -> 3000; case UNMAKE -> 3500; case SHARE -> 3000; case WORLD -> 6000;
            case DENY -> 4000; case RULE -> 7000; case REVERSAL -> 5000; case MASS -> 9000; case SKIN -> 4000; case REWRITE_TIME -> 8000;
            case PRESENCE -> 3000; case APRIL -> 5000; default -> 0;
        };
    }

    public static int cooldown(int mode) {
        return switch (mode) {
            case UNDO -> undoCooldown(); case ERASE -> eraseCooldown(); case ANNUL -> 900; case REWIND -> 600; case UNPICK -> 300;
            case NULLIFY -> 1200; case UNMAKE -> 900; case SHARE -> 900; case WORLD -> 3600;
            case DENY -> 600; case RULE -> 3600; case REVERSAL -> 1800; case MASS -> 2400; case SKIN -> 2400; case REWRITE_TIME -> 6000;
            case PRESENCE -> 1200; case APRIL -> 2400; default -> 0;
        };
    }

    /** Annul Damage: for six seconds, hits are made not to have happened until a pool of half your maximum health is used up. */
    public static int annulTicks() { return 120; }
    public static double annulPool(double maxHealth) { return Math.max(0.0, maxHealth) * 0.5; }

    /** How much of a hit the pool takes (never more than the hit or the pool). */
    public static double annulAbsorb(double pool, double hit) { return Math.max(0.0, Math.min(pool, hit)); }

    /** A window (Annul, Nullify, World) stored as the game time it ends at is open while that time is in the future. */
    public static boolean windowOpen(long until, long now) { return until > now; }

    /** Nullify Strike: the next hit within ten seconds never happened (hits that bypass invulnerability, the void and kill, are not Fiction's to refuse). */
    public static int nullifyTicks() { return 200; }

    /** Rewind Step: the place you stood five seconds ago, if it is between 1.5 and 48 blocks away. */
    public static double rewindMin() { return 1.5; }
    public static double rewindMax() { return 48.0; }
    public static boolean rewindOk(double distance) { return distance >= rewindMin() && distance <= rewindMax(); }

    /** Erase Effects: range of the look. */
    public static double unpickRange() { return 24.0; }

    /** Unmake: hostile projectiles within this radius are erased, at most this many. */
    public static double unmakeRadius() { return 10.0; }
    public static int unmakeMax() { return 32; }

    /** Share the Fiction: allies within this radius (at most eight, you included) are cleansed and healed this share of their maximum health. */
    public static double shareRadius() { return 8.0; }
    public static double shareHeal() { return 0.25; }
    public static int shareMax() { return 8; }

    /** World Fiction: a zone around where you stood, pulsing once a second for six seconds; each pulse cleanses allies in it and erases hostile projectiles (at most 16). */
    public static double worldRadius() { return 8.0; }
    public static int worldTicks() { return 120; }
    public static int worldPulseMaxProjectiles() { return 16; }

    /** True when a pulse of the zone is due: it is open and this is a whole second of the owner's tick count. */
    public static boolean worldPulseDue(long until, long now, int tickCount) { return until > now && tickCount % 20 == 0; }

    /** Seconds until the refused death is ready again (0 = ready now). */
    public static int standUpSeconds(long readyAt, long now) { return readyAt <= now ? 0 : (int) Math.min(Integer.MAX_VALUE, (readyAt - now + 19) / 20); }

    /** Shifts a sample into a history of doubles (newest first), dropping the oldest. */
    public static void pushD(double[] history, double sample) {
        for (int i = history.length - 1; i > 0; i--) history[i] = history[i - 1];
        if (history.length > 0) history[0] = sample;
    }
}
