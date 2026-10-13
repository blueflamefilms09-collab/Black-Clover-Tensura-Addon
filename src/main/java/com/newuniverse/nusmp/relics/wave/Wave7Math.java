package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 7 (the ten chest armours): every number and rule that needs no Minecraft class, so it can be tested on its own.
 * The glue in {@link Wave7Events} only reads game state and hands it to these functions.
 */
public final class Wave7Math {
    private Wave7Math() {}

    // ---------------------------------------------------------------- Tensura stat gates (the cards' class and ability-score requirements)
    /** A Rogue is trained in agility and nerve: the Golden Shadow wants this much maximum aura (Tensura's physical energy). */
    public static final double GATE_ROGUE_AURA = 500;
    /** A spellcaster has a pool to cast from: the Arcane Conduction Armor wants this much maximum magicules. */
    public static final double GATE_CASTER_MAGICULE = 1000;
    /** Strength 15: Delphyne's plate wants this much maximum existence (EP) or it drags. */
    public static final double GATE_STRENGTH_EP = 3000;
    public static final double TRAIN_MIN = 0.5, TRAIN_MAX = 1.5;

    /**
     * How well a wearer fits a requirement: 1.0 at the gate, half as good a tenth of the way there (or with nothing), one and a half times
     * as good ten times over (log scale, like Tensura's own EP scaling). Never a lock-out: the armour still works, only less or more.
     */
    public static double training(double value, double gate) {
        if (!(value > 0) || !(gate > 0)) return TRAIN_MIN;
        return Math.max(TRAIN_MIN, Math.min(TRAIN_MAX, 1.0 + 0.5 * Math.log10(value / gate)));
    }

    /** The "Intelligence modifier" of the Arcane armour: extra damage reduction from the magicule pool (0 below the gate, at most 12%). */
    public static double intBonus(double maxMagicule) {
        if (!(maxMagicule >= GATE_CASTER_MAGICULE)) return 0.0;
        return Math.min(0.12, 0.04 * (1.0 + Math.log10(maxMagicule / GATE_CASTER_MAGICULE)));
    }

    public static boolean isCaster(double maxMagicule) { return maxMagicule >= GATE_CASTER_MAGICULE; }

    /** Base Mode is for everyone; Fortified, Stealth and Burst need a spellcaster's pool. */
    public static boolean modeAllowed(int mode, boolean caster) { return caster || Math.floorMod(mode, 4) == 0; }

    /** Plate that is too heavy for the wearer's existence (unknown EP counts as fine). */
    public static boolean weighedDown(double ep) { return ep > 0 && ep < GATE_STRENGTH_EP; }

    // ---------------------------------------------------------------- Armour of the Stone Sentinel
    /** Ticks between two position samples of a wearer (the player tick is gated to this period). */
    public static final int SAMPLE = 10;
    /** Standing still and quiet this long (8 s; the card says 10 minutes) puts the wearer into the trance. */
    public static final int TRANCE_TICKS = 160;
    private static final int STILL_CAP = 100_000;
    /** A sample that moved more than this (squared blocks) breaks the stillness. */
    public static final double MOVE_EPS_SQ = 0.0036;
    /** The Sentinel needs half the food: only this share of the exhaustion it sees is kept. */
    public static final float HUNGER_KEEP = 0.5f;
    /** A hunger sample older than this (ticks) is not trusted (the armour was off in between). */
    public static final int HUNGER_STALE = 45;

    /** Still counter after one more sample: reset by movement or noise, otherwise it grows (capped). */
    public static int stillNext(int still, boolean movedOrNoisy) {
        if (movedOrNoisy || still < 0) return 0;
        return still >= STILL_CAP ? STILL_CAP : Math.min(still + SAMPLE, STILL_CAP);
    }

    public static boolean tranced(int still) { return still >= TRANCE_TICKS; }

    /** The card's literal ten minutes of stillness: the deep trance (the card's easy-to-overlook state at full strength). */
    public static final int DEEP_TRANCE_TICKS = 12_000;

    /** 0 none, 1 trance (8 s), 2 deep trance (10 minutes). */
    public static int tranceLevel(int still) { return still >= DEEP_TRANCE_TICKS ? 2 : still >= TRANCE_TICKS ? 1 : 0; }

    /**
     * The exhaustion a Sentinel wearer ends up with: of everything gained since the last sample, only
     * {@link #HUNGER_KEEP} stays. A drop (the vanilla wrap-around) or no gain leaves the value alone.
     */
    public static float halfHunger(float last, float now) {
        if (!(now > last) || last < 0f) return now;
        return last + (now - last) * HUNGER_KEEP;
    }

    /** True while food level and saturation are unchanged since the last sample (so the exhaustion delta is trustworthy). */
    public static boolean hungerSynced(int lastFood, int food, float lastSat, float sat, long ageTicks) {
        return lastFood == food && lastSat == sat && ageTicks >= 0 && ageTicks <= HUNGER_STALE;
    }

    // ---------------------------------------------------------------- stealth (Starstudded, Golden Shadow, Arcane Stealth, trance; heavy armours are noisy)
    public static final double VIS_STAR = 0.75, VIS_GOLD = 0.6, VIS_ARCANE = 0.65, VIS_TRANCE = 0.2, VIS_DEEP = 0.1, VIS_SNEAK_EXTRA = 0.7, VIS_HEAVY = 1.25;
    public static final double VIS_MIN = 0.1, VIS_MAX = 1.6;

    /**
     * How visible the wearer is to mobs compared with normal (the product of every worn modifier, clamped).
     * stealthy armours get an extra factor while sneaking; the heavy armours with "Stealth: Disadvantage" are easier to notice.
     * {@code goldTraining} (see {@link #training}) deepens or weakens the Golden Shadow's own factor.
     */
    public static double visibility(boolean sneaking, int tranceLevel, boolean star, boolean gold, double goldTraining, boolean arcaneStealth, boolean heavy) {
        double f = 1.0;
        if (tranceLevel >= 2) f *= VIS_DEEP;
        else if (tranceLevel == 1) f *= VIS_TRANCE;
        if (star) f *= VIS_STAR;
        if (gold) f *= Math.max(0.15, Math.min(1.0, 1.0 - (1.0 - VIS_GOLD) * goldTraining));
        if (arcaneStealth) f *= VIS_ARCANE;
        if (sneaking && (star || gold || arcaneStealth)) f *= VIS_SNEAK_EXTRA;
        if (heavy) f *= VIS_HEAVY;
        return Math.max(VIS_MIN, Math.min(VIS_MAX, f));
    }

    // ---------------------------------------------------------------- Starstudded Leathers
    public static final int STAR_TICKS = 1200;               // 1 minute
    public static final double STAR_DAY_RADIUS = 5.0;        // 10 feet
    public static final double STAR_NIGHT_RADIUS = 7.0;      // 15 feet between dusk and dawn
    public static final float STAR_HIT = 5f, STAR_HIT_PLAYER = 3f, STAR_BOLT = 5f;

    /** Dusk to dawn (the game's dusk at 12500 to dawn at 23500). */
    public static boolean isNight(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 12500 && t < 23500;
    }

    public static double starRadius(long dayTime) { return isNight(dayTime) ? STAR_NIGHT_RADIUS : STAR_DAY_RADIUS; }

    /** Radiant damage the starscape deals to whoever hits the wearer (the Dexterity save becomes a softer hit on players). */
    public static float starRetaliation(boolean attackerIsPlayer) { return attackerIsPlayer ? STAR_HIT_PLAYER : STAR_HIT; }

    // ---------------------------------------------------------------- Coat of the Duelist
    public static final float DUEL_TAKEN = 0.78f;           // "double Dexterity to AC" with one weapon or none: less melee damage taken
    public static final int GUARD_TICKS = 120;               // En Garde stance

    /** Share of a melee blow the Duelist takes: only with at most one weapon (off hand empty) against a melee hit. */
    public static float duelistTaken(boolean offhandEmpty, boolean melee) { return offhandEmpty && melee ? DUEL_TAKEN : 1f; }

    // ---------------------------------------------------------------- Suit of the Golden Shadow
    public static final int FADE_TICKS = 100;               // Fade from View after a kill (until the end of the next turn)
    public static final int UNSEEN_TICKS = 600;             // Unseen: 30 s of invisibility, ends on attack
    public static final float SNEAK_MIN = 3f, SNEAK_MAX = 8f, SNEAK_SHARE = 0.4f;

    /** The sneak attack bonus: 40% of the blow, never below 3 (the "reroll the 1" rule), never above 8. */
    public static float sneakBonus(float damage) {
        return Math.max(SNEAK_MIN, Math.min(SNEAK_MAX, damage * SNEAK_SHARE));
    }

    /** The same scaled by how well the wearer fits the Rogue requirement (half as much when untrained, half as much again when expert). */
    public static float sneakBonus(float damage, double training) {
        return sneakBonus(damage) * (float) Math.max(TRAIN_MIN, Math.min(TRAIN_MAX, training));
    }

    /** Unseen lasts 30 s for a wearer at the gate, 15 s untrained, 45 s as an expert. */
    public static int unseenTicks(double training) {
        return (int) Math.round(UNSEEN_TICKS * Math.max(TRAIN_MIN, Math.min(TRAIN_MAX, training)));
    }

    /**
     * A sneak attack lands from the Unseen / Fade from View cloak, or on a mob that is not fighting the attacker.
     * Players are only ever hit by the cloaked version (never a free bonus in PvP).
     */
    public static boolean isSneakAttack(boolean cloaked, boolean victimIsMob, boolean targetIsOnYou) {
        return cloaked || (victimIsMob && !targetIsOnYou);
    }

    // ---------------------------------------------------------------- Armor of the Lionheart
    public static final double PRIDE_RADIUS = 10.0;         // 30 feet
    public static final int ROAR_TICKS = 600;               // the advantage lasts a minute: half a minute of buffs
    public static final int RESOLVE_TICKS = 100;
    public static final float RESOLVE_PROTECT = 0.75f, RESOLVE_EXPOSED = 1.25f;   // +-5 AC

    /** Damage share a creature takes while a Lion's Resolve swap is on it. */
    public static float resolveFactor(boolean protectedAlly, boolean exposedSelf) {
        float f = 1f;
        if (protectedAlly) f *= RESOLVE_PROTECT;
        if (exposedSelf) f *= RESOLVE_EXPOSED;
        return f;
    }

    // ---------------------------------------------------------------- Armor of the Beheaded
    public static final int HEAD_TICKS = 900;               // 45 s with the head detached
    public static final float HEAD_PROJECTILE = 0.6f, HEADLESS_PROJECTILE = 0.85f;

    /** Share of a projectile hit taken by the headless knight (nothing for the flying axe to hit). */
    public static float headlessProjectile(boolean detached) { return detached ? HEAD_PROJECTILE : HEADLESS_PROJECTILE; }

    /** What cannot be used while the head is off (no mouth). */
    public static boolean headlessBlocks(String useAnimation) { return "EAT".equals(useAnimation) || "DRINK".equals(useAnimation); }

    // ---------------------------------------------------------------- Dispel Magic (the Beheaded)
    public static final double DISPEL_RADIUS = 6.0;
    public static final int DISPEL_SENSES_TICKS = 1200;      // the card: Dispel Magic disables the magical senses for 1 minute
    public static final int JAM_SECONDS = 6, JAM_SECONDS_BOSS = 2, DISPEL_FOES = 8;
    public static final float JAM_MISFIRE = 0.5f;

    /** Are the head's magical senses working? Off while a dispel (the armour's own, or another one's) lasts. */
    public static boolean sensesOn(boolean detached, long dispelUntil, long now) { return detached && !active(dispelUntil, now); }

    // ---------------------------------------------------------------- False Dragon Armor
    public static final int WINGS_TICKS = 2400;             // 2 minutes (the card says an hour)
    public static final int WINGS_FALL_TICKS = 200;         // slow falling when the wings burn out

    public static boolean active(long until, long now) { return until > now; }

    // ---------------------------------------------------------------- Soul Armor
    public static final float SOUL_CAP = 8f;                 // 4 hearts: twice a proficiency modifier of +4
    public static final long SOUL_FADE = 24000L;             // a day without a kill ("after a long rest")

    /** The escaping life energy that returns to the wearer as magicules and aura (share of the maximums per kill). */
    public static float soulEnergyShare(boolean firstKill) { return firstKill ? 0.10f : 0.03f; }

    /** 1d4 from a roll in 0..3. */
    public static float soulRoll(int roll0to3) { return 1 + Math.floorMod(roll0to3, 4); }

    /**
     * Applies a kill: the tracked temporary hit points first shrink to what is left of the absorption (blows eat them),
     * then grow by the gain up to the cap. Returns {tracked, absorption}.
     */
    public static float[] soulApply(float tracked, float absorption, float gain, float cap) {
        float live = Math.max(0f, Math.min(tracked, absorption));
        float grown = Math.min(cap, live + Math.max(0f, gain));
        return new float[]{grown, Math.max(0f, absorption) + (grown - live)};
    }

    /** Absorption left after the tracked temporary hit points fade (armour removed, a day passed, a rest). */
    public static float soulFade(float tracked, float absorption) {
        return Math.max(0f, absorption - Math.max(0f, Math.min(tracked, absorption)));
    }

    // ---------------------------------------------------------------- Arcane Conduction Armor
    public static final int MODE_BASE = 0, MODE_FORTIFIED = 1, MODE_STEALTH = 2, MODE_BURST = 3, MODES = 4;
    public static final float FORCE_TAKEN = 0.65f, FORTIFIED_TAKEN = 0.88f, BURST_TAKEN = 1.10f, BURST_DEALT = 1.15f;

    public static int nextMode(int mode) { return Math.floorMod(mode + 1, MODES); }

    public static String modeName(int mode) {
        switch (Math.floorMod(mode, MODES)) {
            case MODE_FORTIFIED: return "Fortified Mode";
            case MODE_STEALTH: return "Stealth Mode";
            case MODE_BURST: return "Burst Mode";
            default: return "Base Mode";
        }
    }

    /** Share of a blow the wearer takes in a mode (Base resists force and magic, Fortified is tougher, Burst is fragile). */
    public static float arcaneTaken(int mode, boolean forceLike, double intBonus) {
        float base;
        switch (Math.floorMod(mode, MODES)) {
            case MODE_FORTIFIED: base = FORTIFIED_TAKEN; break;
            case MODE_BURST: base = BURST_TAKEN; break;
            case MODE_BASE: base = forceLike ? FORCE_TAKEN : 1f; break;
            default: base = 1f; break;
        }
        return base * (float) (1.0 - Math.max(0.0, Math.min(0.2, intBonus)));
    }

    /** Burst Mode: the wearer's spell damage is raised. */
    public static float arcaneDealt(int mode, boolean magicLike) { return mode == MODE_BURST && magicLike ? BURST_DEALT : 1f; }

    /** Force-like damage (magic, sonic, spatial and the like) by damage type path. */
    public static boolean isForceLike(String damageTypePath) {
        if (damageTypePath == null) return false;
        String s = damageTypePath.toLowerCase(java.util.Locale.ROOT);
        return s.contains("magic") || s.contains("force") || s.contains("sonic") || s.contains("spatial") || s.contains("arcane");
    }

    // ---------------------------------------------------------------- Delphyne's Cursed Armor
    public static final float DRAGON_TAKEN = 0.75f;
    public static final double TAIL_RADIUS = 4.0;
    public static final float TAIL_HIT = 4f;

    /** How hard the tail shoves: scaled by how well the wearer fits the Strength requirement. */
    public static double tailPush(double strengthTraining) { return 1.1 * Math.max(TRAIN_MIN, Math.min(TRAIN_MAX, strengthTraining)); }

    /** A dragon (ender dragon, Tensura dragons, drakes) by entity type path. */
    public static boolean isDragonish(String entityTypePath) {
        if (entityTypePath == null) return false;
        String s = entityTypePath.toLowerCase(java.util.Locale.ROOT);
        return s.contains("dragon") || s.contains("drake");
    }

    /** Status effects the Lionheart / Delphyne wearers cannot suffer: Frightened. */
    public static boolean isFear(String namespace, String path) { return "tensura".equals(namespace) && "fear".equals(path); }

    /** Status effects of the head: the Beheaded has no head to blind, sicken or darken. */
    public static boolean isHeadEffect(String namespace, String path) {
        return "minecraft".equals(namespace) && ("blindness".equals(path) || "nausea".equals(path) || "darkness".equals(path));
    }
}
