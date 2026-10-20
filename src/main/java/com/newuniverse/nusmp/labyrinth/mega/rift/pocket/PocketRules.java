package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Why: every number and decision of the exclusive pockets that does not need a world, in one pure class so a unit test pins it: how hard a trap may hit (a share of the victim's health,
 * capped, never lethal), what gravity a zone gives, how the seal of the treasury is broken (by a swing's attack damage, a part of it per blow, or unravelled by a mage whose magic is on the
 * list), when each room of a collapsing tomb falls (the deepest first, so the delvers run for the entry), how long an empty tomb is kept, which tomb a rift leads to (a weighted pick by depth,
 * the same for the same seed), how many monsters a room holds, and the HARD GATE of the relics: a loot roll may give a pocket's relic only to someone standing inside that very pocket, at the
 * stage the table names (the boss dead, the seal open).
 */
public final class PocketRules {
    private PocketRules() {}

    // ---------------------------------------------------------------- traps
    /** Ticks a victim is safe from the same trap tile after it fires. */
    public static final int TRAP_COOLDOWN = 60;
    /** The most a trap takes in one blow, as a share of maximum health (before the never-lethal rule). */
    public static final double TRAP_CAP = 0.30;
    /** The horizontal reach of a trap tile's trigger: a tile is 3 blocks across, the trigger is a little more than its middle block. */
    public static final double TRAP_REACH = 1.55;

    /** The damage of a trap blow: its share of the victim's maximum health, capped, and never more than leaves the victim one half heart. */
    public static float trapDamage(float maxHealth, float health, double share) {
        double d = Math.min(Math.max(0.0, share), TRAP_CAP) * Math.max(0f, maxHealth);
        return (float) Math.max(0.0, Math.min(d, health - 1.0f));
    }

    /** Is a player at (px, pz) on the trap tile whose middle is (tx, tz)? */
    public static boolean onTile(double px, double pz, double tx, double tz) {
        double dx = px - (tx + 0.5), dz = pz - (tz + 0.5);
        return dx * dx + dz * dz <= TRAP_REACH * TRAP_REACH;
    }

    // ---------------------------------------------------------------- gravity
    /** Normal gravity, and what an inverted zone and a weightless zone make of it (the attribute's value; the pocket's base multiplies the normal one). */
    public static final double G_NORMAL = 0.08, G_INVERTED = -0.06, G_NEGATED = 0.02;

    /** The gravity attribute value to apply for a zone kind (null = no zone) in a pocket whose gravity is {@code base} times normal. */
    public static double gravity(PocketLayout.Kind zone, double base) {
        if (zone == PocketLayout.Kind.INVERTED) return G_INVERTED;
        if (zone == PocketLayout.Kind.NEGATED) return Math.min(G_NEGATED, G_NORMAL * base);
        return G_NORMAL * Math.max(0.25, Math.min(1.0, base));
    }

    /** The modifier amount (added to the base 0.08) that makes the attribute {@code value}. */
    public static double gravityModifier(double value) { return value - G_NORMAL; }

    /** The fall distance (blocks) the pocket forgives its delvers: the mana of the place cushions every fall, so a drop from the ceiling of a tall corridor never hurts. */
    public static final double SAFE_FALL = 24.0;

    // ---------------------------------------------------------------- the seal
    /** A swing counts for the seal only when the attack is this charged. */
    public static final double CHARGE_MIN = 0.9;

    /** The force a swing puts into the seal: its attack damage, at most a quarter of the seal's strength (so no single blow opens it), nothing if the swing was not charged. */
    public static double sealForce(double attackDamage, double charge, int sealHp) {
        if (charge < CHARGE_MIN || attackDamage <= 0.0) return 0.0;
        return Math.min(attackDamage, sealHp * 0.25);
    }

    /** What holding the attack button on the seal adds each tick: a hundredth of the attack damage (a quarter of a second of holding is worth about a fortieth of one charged swing). */
    public static double sealHold(double attackDamage) { return Math.max(0.0, attackDamage) * 0.01; }

    /** The crack stage (0 to 9) the seal shows at {@code left} points of strength out of {@code total}. */
    public static int crackStage(int left, int total) {
        if (total <= 0 || left >= total) return -1;
        return Math.max(0, Math.min(9, (int) Math.floor(10.0 * (total - Math.max(0, left)) / total)));
    }

    /** Does a mage whose grimoire magic is {@code magic} (any case; "" = none) unravel the seal that answers to {@code magics}? */
    public static boolean unravels(String magic, List<String> magics) {
        if (magic == null || magic.isEmpty()) return false;
        String m = magic.toUpperCase(Locale.ROOT);
        for (String s : magics) if (s.equalsIgnoreCase(m)) return true;
        return false;
    }

    // ---------------------------------------------------------------- the collapse
    /** The ticks of warning a room gets before it falls, and the share of the total time before the first room falls and after which the last (nearest the entry) does. */
    public static final int ROOM_WARN_TICKS = 100;
    public static final double FIRST_FALL = 0.18, LAST_FALL = 0.85;

    /**
     * The tick (counted from the start of the collapse) at which the room at {@code depth} falls, of {@code total} ticks: the deepest room first, the shallowest last; the entry (depth 0) never
     * falls before the end, when the whole tomb is cleared. {@code maxDepth} is the depth of the deepest room.
     */
    public static int roomFallsAt(int depth, int maxDepth, int total) {
        if (depth <= 0 || maxDepth <= 0) return total;
        double k = 1.0 - Math.min(1.0, depth / (double) maxDepth);
        return (int) Math.round(total * (FIRST_FALL + (LAST_FALL - FIRST_FALL) * k));
    }

    /** Is a room in its warning (it falls within {@link #ROOM_WARN_TICKS} and has not fallen)? */
    public static boolean roomWarned(int elapsed, int fallsAt) { return elapsed >= fallsAt - ROOM_WARN_TICKS && elapsed < fallsAt; }

    /** The most health a delver caught in a falling room loses (a share of the maximum; never lethal either, see {@link #trapDamage}). */
    public static final double FALL_SHARE = 0.20;

    /** Seconds an empty tomb is kept before it is cleared, and the grace right after it opened (the party may still be coming). */
    public static final int EMPTY_KEEP_SECONDS = 120, FIRST_GRACE_SECONDS = 150;

    /** Does an empty tomb collapse now? {@code sinceEmpty}: ticks nobody has been inside; {@code sinceCreated}: ticks since it was made. */
    public static boolean emptyCollapse(boolean empty, long sinceEmpty, long sinceCreated, boolean collapsing) {
        if (!empty) return false;
        if (collapsing) return true;
        return sinceCreated > FIRST_GRACE_SECONDS * 20L && sinceEmpty > EMPTY_KEEP_SECONDS * 20L;
    }

    // ---------------------------------------------------------------- which tomb
    /** The index of the tomb a rift of this seed leads to, among those whose minimum depth is reached (weighted by their weight); -1 when none qualifies. */
    public static int pick(List<PocketDef> defs, long seed, int depth) {
        int total = 0;
        for (PocketDef d : defs) if (d.minDepth() <= depth) total += d.weight();
        if (total <= 0) return -1;
        long r = Math.floorMod(RiftRules.mix(seed, 0x7011L), (long) total);
        for (int i = 0; i < defs.size(); i++) {
            PocketDef d = defs.get(i);
            if (d.minDepth() > depth) continue;
            r -= d.weight();
            if (r < 0) return i;
        }
        return -1;
    }

    // ---------------------------------------------------------------- the monsters
    /** The number of monsters a room of this kind and size holds for a party of {@code players} (1 to 6). */
    public static int monsterCount(PocketLayout.Kind kind, int half, int players) {
        int base = switch (kind) {
            case COMBAT -> 4 + half / 3;
            case TRAP, RUIN -> 3;
            case KEY -> 3;
            case INVERTED, NEGATED -> 3;
            default -> 0;
        };
        int extra = Math.max(0, Math.min(5, players - 1));
        return base == 0 ? 0 : base + (extra + 1) / 2;
    }

    /** A weighted choice among the mobs allowed by the room (a flier room takes only the fliers; any other room only the walkers; no walker at all: none); returns an index or -1. */
    public static int pickMob(List<PocketDef.Mob> mobs, boolean airRoom, Random rnd) {
        int total = 0;
        for (PocketDef.Mob m : mobs) if (m.air() == airRoom && !m.elite()) total += m.weight();
        if (total <= 0) return -1;
        int r = rnd.nextInt(total);
        for (int i = 0; i < mobs.size(); i++) {
            PocketDef.Mob m = mobs.get(i);
            if (m.air() != airRoom || m.elite()) continue;
            r -= m.weight();
            if (r < 0) return i;
        }
        return -1;
    }

    /** The floor a tomb's monsters are made for: the rift's floor, but never below the tomb's own minimum. */
    public static int effectiveFloor(int riftFloor, int floorMin) { return Math.max(Math.max(1, Math.abs(riftFloor)), Math.max(1, floorMin)); }

    /** Floors per tier of the monster tables (the Mob Foundry's), and the number of tiers. */
    public static final int FLOORS_PER_TIER = 200, TIERS = 5;

    /** The tier (1 to 5) of a floor, the same rule as the Foundry's. */
    public static int tierOf(int floor) { return Math.min(TIERS, (Math.max(1, Math.abs(floor)) - 1) / FLOORS_PER_TIER + 1); }

    /**
     * How much a tomb's monster is stronger than its base would be: the tomb's monsters stand one tier above the floor (capped at the top tier), so the factor is the table's value at that tier over
     * its value at the base's own tier, clamped to {@code 0.5 .. 8} so a datapack cannot ask for a monster that is a speck or a wall.
     */
    public static double tierFactor(double[] table, int baseTier, int floor) {
        int target = Math.min(table.length, tierOf(floor) + 1), base = Math.max(1, Math.min(table.length, baseTier));
        return Math.max(0.5, Math.min(8.0, table[target - 1] / table[base - 1]));
    }

    /** How many tiers of threat above its floor a tomb's mega-boss stands: it is a Decennial boss with a larger body and a harder hand. */
    public static final int BOSS_THREAT_BONUS = 8;

    /** The threat the mega-boss is scaled for (1 to 1000). */
    public static int bossThreat(int effectiveFloor) { return Math.min(1000, Math.max(1, effectiveFloor) + BOSS_THREAT_BONUS); }

    /** The most mega-bosses' court (adds) that may stand at once, however a profile asks. */
    public static final int ADDS_CAP = 8;

    // ---------------------------------------------------------------- the hard gate of the relics
    /** The stage a loot table of a pocket is for: the boss's death, the treasury's pedestals and chests, or anywhere inside the tomb. */
    public enum Stage { BOSS, TREASURY, ANY }

    public static Stage stageOf(String name) {
        if (name == null) return Stage.ANY;
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "boss" -> Stage.BOSS;
            case "treasury" -> Stage.TREASURY;
            default -> Stage.ANY;
        };
    }

    /**
     * May a loot roll give a relic of the pocket {@code required}? Only when the roll happens INSIDE an exclusive pocket (a live one: {@code insideId} is its dungeon id, empty when the roll's
     * place is in no pocket), that pocket is the required one, and the stage holds: the boss table only after the boss died, the treasury table only with the seal open.
     */
    public static boolean gate(String insideId, String required, Stage stage, boolean bossDead, boolean sealOpen) {
        if (insideId == null || insideId.isEmpty() || required == null || !insideId.equals(required)) return false;
        return switch (stage) {
            case BOSS -> bossDead;
            case TREASURY -> sealOpen;
            case ANY -> true;
        };
    }

    // ---------------------------------------------------------------- ancient magic (the grimoire's resonance)
    /** Chance that an ancient spell of the tomb inscribes itself into the grimoire of a resonant mage when the treasury is opened. */
    public static final double RESONANCE_CHANCE = 0.45;

    /** Does a mage with this magic resonate with the tomb (its list of magics, any case)? */
    public static boolean resonates(String magic, List<String> resonance) { return unravels(magic, resonance); }

    // ---------------------------------------------------------------- messages
    /** The numbers of a collapse clock as the whole seconds a warning names (60, 30, 10, 5, 3, 2, 1) once each; 0 otherwise. */
    public static int warnSeconds(long ticksLeft, long previousTicksLeft) { return RiftRules.warnSeconds(ticksLeft, previousTicksLeft); }

    /** The rooms (ids) in the order they fall: deepest first; the entry is not among them. */
    public static List<Integer> fallOrder(PocketLayout l) {
        int[] depth = depthsAll(l);
        List<Integer> ids = new ArrayList<>();
        for (PocketLayout.Room r : l.rooms()) if (r.id() != l.entryId()) ids.add(r.id());
        ids.sort((a, b) -> depth[b] != depth[a] ? Integer.compare(depth[b], depth[a]) : Integer.compare(a, b));
        return ids;
    }

    /** Depths from the entry over every door, the sealed one included (the collapse reaches the treasury through the lair). */
    public static int[] depthsAll(PocketLayout l) {
        int n = l.rooms().size();
        int[] depth = new int[n];
        java.util.Arrays.fill(depth, -1);
        depth[l.entryId()] = 0;
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        q.add(l.entryId());
        while (!q.isEmpty()) {
            int c = q.poll();
            for (int o : l.neighbours(c, true)) if (depth[o] < 0) {
                depth[o] = depth[c] + 1;
                q.add(o);
            }
        }
        return depth;
    }
}
