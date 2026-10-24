package com.newuniverse.nusmp.mana.method;

/**
 * What True Magic does to the world beyond the old blocks (pure, tested; the placement itself is {@code TrueBlocks}). Two additions, both capped and both behind their own setting:
 * <ul>
 *   <li><b>True Plant: the cage.</b> The plant grows physical wood and leaves round the creature it struck: a 3x3 ring two blocks high (corner posts of LOG, the sides of LEAVES) and
 *       a LOG roof over the target's head, {@value #CAGE_CELLS} blocks at most. Only free cells, only where the caster may build, never over a creature, taken back after
 *       {@link #cageSeconds} (the leaves are breakable by hand, so a trapped player is never stuck). {@code trueMagic.plantCage} off: the old moss carpet.</li>
 *   <li><b>True Lightning: the bolt.</b> A real vanilla {@code LightningBolt} entity strikes the impact, at most ONE per cast and {@value #LIGHTNING_PER_SECOND} per second on the whole server.
 *       By default it is the visual-only kind (the flash and the thunder of the real entity, no vanilla damage, no fire, no mob changes) because the rune's own damage is done by the
 *       caster's attack; the full bolt ({@code trueMagic.lightningRealBolt}) needs a clear spot: no one but foes within {@value #SAFE_RADIUS} blocks, the game rule {@code doFireTick}
 *       and {@code trueMagic.placeFire}.</li>
 * </ul>
 */
public final class TrueWorldRules {
    private TrueWorldRules() {}

    public enum Material { LOG, LEAVES }

    /** The most cage blocks one cast places, one caster has standing, and the whole server has standing (the cage shares the ledger with the other True blocks). */
    public static final int CAGE_PER_CAST = 18, CAGE_PER_CASTER = 36, CAGE_GLOBAL = 240;
    public static final int CAGE_MAX_SECONDS = 30;
    public static final int LIGHTNING_PER_SECOND = 3;
    public static final double SAFE_RADIUS = 4.5;

    /** The cage's cells relative to the target's feet cell, walls first so a truncated cage is still a wall: (dx, dy, dz, material ordinal). Two rings of eight, then the roof. */
    private static final int[][] CAGE = buildCage();
    public static final int CAGE_CELLS = CAGE.length;

    private static int[][] buildCage() {
        int[][] c = new int[17][];
        int n = 0;
        for (int dy = 0; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    boolean corner = dx != 0 && dz != 0;
                    c[n++] = new int[]{dx, dy, dz, (corner ? Material.LOG : Material.LEAVES).ordinal()};
                }
        c[n++] = new int[]{0, 2, 0, Material.LOG.ordinal()};
        return c;
    }

    public static int[] cageCell(int i) { return CAGE[Math.floorMod(i, CAGE.length)]; }

    public static Material cageMaterial(int i) { return Material.values()[cageCell(i)[3]]; }

    /** How many of the cage's blocks may be placed given what the caster and the world already have standing (never negative). */
    public static int allowedCage(int casterLive, int globalLive) {
        int room = Math.min(CAGE_PER_CASTER - casterLive, CAGE_GLOBAL - globalLive);
        return Math.max(0, Math.min(Math.min(CAGE_CELLS, CAGE_PER_CAST), room));
    }

    /** The lifetime of a cage in seconds: the True block lifetime, but never more than {@link #CAGE_MAX_SECONDS}. */
    public static int cageSeconds(int blockSeconds) { return Math.min(CAGE_MAX_SECONDS, TrueBlockRules.seconds(blockSeconds)); }

    /**
     * Does a cage close round this creature? Only round a creature that was struck (a wall hit has nothing to trap), only if the setting is on, and only a foe: never the caster, an
     * ally or a bystander.
     */
    public static boolean cageWanted(boolean settingOn, boolean struckCreature, boolean isFoe) { return settingOn && struckCreature && isFoe; }

    // ---------------------------------------------------------------- lightning
    /**
     * Asks the server-wide allowance of lightning bolts: {@code window} is a two-element scratch ({@code [0]} = the second, {@code [1]} = the bolts used in it) that the caller keeps.
     * Returns true and counts the bolt while fewer than {@link #LIGHTNING_PER_SECOND} were used in this second.
     */
    public static boolean takeBolt(long gameTime, long[] window) {
        long second = Math.floorDiv(gameTime, 20L);
        if (window[0] != second) {
            window[0] = second;
            window[1] = 0;
        }
        if (window[1] >= LIGHTNING_PER_SECOND) return false;
        window[1]++;
        return true;
    }

    /** May the strike be the full vanilla bolt (damage, fire, mob changes)? Never with a bystander close, never without the fire rules. */
    public static boolean realBoltAllowed(boolean settingOn, boolean fireRuleOn, boolean fireSettingOn, int bystandersNear) {
        return settingOn && fireRuleOn && fireSettingOn && bystandersNear <= 0;
    }

    /** Is an entity a bystander of a lightning strike (anything living that is not a foe of the caster and not the caster's enemy-by-rule)? */
    public static boolean bystander(boolean isFoe) { return !isFoe; }
}
