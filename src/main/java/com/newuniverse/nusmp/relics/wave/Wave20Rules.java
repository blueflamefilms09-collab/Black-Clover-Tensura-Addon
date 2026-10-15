package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;

/**
 * Why: the pure rules (no Minecraft imports) behind the twelve weapons of wave 20 of the Relics of Many Worlds, part 4, so they can be tested
 * without the game. Tabletop becomes Minecraft here: a "long rest" is a Minecraft day (24000 ticks), a short rest a couple of minutes, one round
 * of combat six seconds, 5 ft one block, and a die roll is a fixed average or a plain chance. Every counter lives in a {@link Bag} the game side backs
 * with the stack's custom data or the player's persistent data.
 */
public final class Wave20Rules {
    private Wave20Rules() {}

    public static final long DAY = 24000L;

    /** A long-valued key/value store; an absent key reads as 0. */
    public interface Bag {
        long get(String key);

        void put(String key, long value);
    }

    // ---------------------------------------------------------------- the elements of the Dragoon's Halberd
    /** The element a drop attack carries: the wielder's choice, cycled with the first act. */
    public enum Element {
        FIRE("Fire", "minecraft:in_fire"), FROST("Frost", "minecraft:freeze"), LIGHTNING("Lightning", "minecraft:lightning_bolt"), WIND("Wind", "minecraft:magic");

        private final String label, damageType;

        Element(String label, String damageType) {
            this.label = label;
            this.damageType = damageType;
        }

        public String label() { return label; }

        public String damageType() { return damageType; }

        public Element next() { return values()[(ordinal() + 1) % values().length]; }

        public static Element of(long ordinal) { return values()[(int) Math.floorMod(ordinal, (long) values().length)]; }
    }

    /** A drop attack: the wielder is falling (more than one and a half blocks) when the blow lands. */
    public static boolean isDropAttack(double fallDistance, boolean onGround) {
        return !onGround && fallDistance >= 1.5;
    }

    /** The extra 1d6 elemental damage of a drop attack (3.5) and the radius it reaches (10 ft is two blocks). */
    public static final float DROP_DAMAGE = 3.5f;
    public static final double DROP_RADIUS = 2.0;

    /** "You can kill a dragon with a pointy stick": the blow against anything draconic is stronger. */
    public static final float DRAGON_BONUS = 4.0f;

    /** Draconic creatures by registry path (a dragon, a wyvern, a drake, a wyrm). */
    public static boolean isDraconic(String path) {
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return p.contains("dragon") || p.contains("wyvern") || p.contains("drake") || p.contains("wyrm");
    }

    // ---------------------------------------------------------------- Efreeti's Kusarigama: the grapple
    public static final int GRAPPLE_TICKS = 120, GRAPPLE_STEP = 40;
    public static final float GRAPPLE_DAMAGE = 3.5f;
    public static final float BOSS_HEALTH = 200.0f;

    /** Fire damage at the start of the grapple and again every 40 ticks it lasts ("at the end of its turn"). */
    public static boolean grappleDamageDue(long now, long nextAt) { return now >= nextAt; }

    public static boolean grappleOver(long now, long until) { return now >= until; }

    // ---------------------------------------------------------------- Ilmare Gin: the thrown katana and the step
    public static final int STEP_WINDOW = 100;

    /** The bonus action to teleport to the impact stays open for five seconds after the throw. */
    public static boolean stepOpen(long now, long thrownAt) { return thrownAt > 0 && now >= thrownAt && now - thrownAt <= STEP_WINDOW; }

    // ---------------------------------------------------------------- Katana of Carnage
    public static final int CARNAGE_WINDOW = 160, CARNAGE_MAX = 5;

    /** The stacks after a kill: they keep growing while kills come within eight seconds of each other, and start again after a gap. */
    public static int carnageAfterKill(int stacks, long lastKillAt, long now) {
        int kept = lastKillAt > 0 && now - lastKillAt <= CARNAGE_WINDOW ? stacks : 0;
        return Math.min(CARNAGE_MAX, kept + 1);
    }

    /** The stacks still standing: they fade eight seconds after the last kill. */
    public static int carnageNow(int stacks, long lastKillAt, long now) {
        return lastKillAt > 0 && now - lastKillAt <= CARNAGE_WINDOW ? Math.min(CARNAGE_MAX, Math.max(0, stacks)) : 0;
    }

    public static float carnageBonus(int stacks) { return Math.min(CARNAGE_MAX, Math.max(0, stacks)) * 1.0f; }

    /** "Of sharpness": one blow in twenty severs: a deeper cut and a bleeding wound. */
    public static final double SEVER_CHANCE = 0.05;

    public static boolean severs(double roll) { return roll < SEVER_CHANCE; }

    /** Bloodlust: each stack buys three seconds of speed and haste, so five stacks give a quarter of a minute. */
    public static int bloodlustTicks(int stacks) { return Math.max(0, stacks) * 60; }

    // ---------------------------------------------------------------- Katana of the Crimson Shogun
    public static final int DEFLECT_COOLDOWN = 60;
    public static final double DEFLECT_CHANCE = 0.45;
    public static final float COUNTER_DAMAGE = 6.0f;

    /** The reaction: an attack roll against the attacker's. Here a plain chance, once every three seconds. */
    public static boolean deflects(double roll, long now, long lastAt) { return now - lastAt >= DEFLECT_COOLDOWN && roll < DEFLECT_CHANCE; }

    // ---------------------------------------------------------------- Lightworker's Scepter
    public static final int WARD_HEAL_PER_DAY = 20, WARD_HEAL_PER_HIT = 2;

    /** Hit points gained by resisting a magical blow: two per blow, twenty a day. */
    public static int wardGain(long usedToday) { return (int) Math.max(0, Math.min(WARD_HEAL_PER_HIT, WARD_HEAL_PER_DAY - usedToday)); }

    /** Soulgate: a blow that would kill is turned aside once a day (the wielder slips to the plane of life at a few hearts). */
    public static boolean soulgateSaves(float health, float amount, boolean usedToday) { return !usedToday && amount >= health; }

    public static final float SOULGATE_HEALTH = 8.0f;
    public static final int AURA_TICKS = 1200;

    // ---------------------------------------------------------------- Mjolnir
    public static final double THROW_RANGE = 24.0;

    /** Distance from a point to a ray (origin, unit direction), -1 if the point is behind the origin or beyond {@code length}. */
    public static double distToRay(double px, double py, double pz, double ox, double oy, double oz, double dx, double dy, double dz, double length) {
        double rx = px - ox, ry = py - oy, rz = pz - oz;
        double along = rx * dx + ry * dy + rz * dz;
        if (along < 0 || along > length) return -1.0;
        double cx = rx - dx * along, cy = ry - dy * along, cz = rz - dz * along;
        return Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    /** The bolt is stronger in a thunderstorm. */
    public static float boltDamage(float base, boolean thundering) { return thundering ? base * 1.5f : base; }

    // ---------------------------------------------------------------- Overprotective Halberd
    public static final int HOVER_TICKS = 3600, HOVER_COOLDOWN = 40;
    public static final double HOVER_REACH = 3.0;

    /** The hovering halberd strikes back once per turn (two seconds here) at a foe that struck the wielder in melee from within three blocks. */
    public static boolean hoverStrikes(long now, long hoverUntil, long lastAt, double distance) {
        return hoverUntil > now && now - lastAt >= HOVER_COOLDOWN && distance <= HOVER_REACH;
    }

    // ---------------------------------------------------------------- Phoenix Flame
    /** 2d6 fire damage while the halberd is ablaze. */
    public static final float BLAZE_DAMAGE = 7.0f;

    // ---------------------------------------------------------------- Primal Chaos
    public static final float RADIANT_DAMAGE = 2.5f, VOID_BONUS = 6.0f;

    /** The creatures of the void: the End's folk and everything whose id says void. */
    public static boolean isVoidCreature(String namespace, String path) {
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        String n = namespace == null ? "" : namespace.toLowerCase(Locale.ROOT);
        return p.contains("void") || p.contains("ender") || p.contains("shulker") || p.contains("warden") || p.contains("vex") || p.contains("phantom")
                || (n.equals("tensura") && (p.contains("spatial") || p.contains("abyss")));
    }

    // ---------------------------------------------------------------- Raven's Wing
    public static final double SAVE_CHANCE = 0.40;
    public static final float TOLL_DAMAGE = 4.5f;

    /** Toll the Dead: 1d8, 2d8 against a creature that is already hurt; a creature that holds its ground takes half. */
    public static float tollDamage(float health, float max, boolean saved) {
        float d = health < max ? TOLL_DAMAGE * 2.0f : TOLL_DAMAGE;
        return saved ? d * 0.5f : d;
    }

    public static boolean saveHolds(double roll) { return roll < SAVE_CHANCE; }

    /** Spare the Dying: a creature at half its health or less counts as dying and is the one steadied. */
    public static boolean dying(float health, float max) { return max > 0 && health / max <= 0.5f; }

    // ---------------------------------------------------------------- uses per day (used by the passive daily allowances)
    public static long day(long now) { return now / DAY; }

    /** How much of a daily allowance has been spent today (the counter resets when the day changes). */
    public static long spentToday(Bag b, String key, long now) { return b.get("d_" + key) == day(now) + 1 ? b.get("s_" + key) : 0L; }

    public static void spend(Bag b, String key, long now, long amount) {
        long used = spentToday(b, key, now);
        b.put("d_" + key, day(now) + 1);
        b.put("s_" + key, used + amount);
    }
}
