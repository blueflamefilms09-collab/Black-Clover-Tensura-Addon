package com.newuniverse.nusmp.client.heroweapon;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.heroweapon.bond.HeroNet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the client does not own the bond, the server does; but the HUD, the tooltip and the Weapon Book need to read it every frame, so the last copy the server
 * sent ({@link HeroNet.Sync}) is kept here, with the cooldown bars of the techniques the server said were cast. Everything is static and read-only for the
 * rest of the client (the Weapon Book, the curse HUD and the tooltip read it too); the only writers are the two payload handlers and the client tick that
 * counts the clock the cooldown bars run on. The class imports nothing of the game's client, so common code (the item's tooltip) may read it and a dedicated
 * server never loads anything it cannot. It is forgotten when the player leaves the world.
 */
public final class ClientHeroBond {
    private ClientHeroBond() {}

    private static HeroNet.Sync state = HeroNet.Sync.none();
    private static HeroKind kind;

    /** The cooldown of a technique: the client tick it ends at, and its whole length. */
    private record Cool(long readyAt, int total) {}

    private static final Map<String, Cool> COOL = new HashMap<>();
    private static long clock;

    // ================================================================ writers
    /** A bond copy arrived from the server. */
    public static void receive(HeroNet.Sync s) {
        state = s;
        kind = s.isNone() ? null : HeroKind.byItemId(s.weapon());
        if (kind == null) COOL.clear();
    }

    /** A technique was cast: start its bar. */
    public static void cooldown(HeroNet.Cooldown c) { COOL.put(c.skill(), new Cool(clock + Math.max(0, c.ticks()), Math.max(1, c.ticks()))); }

    /** Counts the clock the bars run on; called once per client tick (not while the game is paused). */
    public static void tick() { clock++; }

    /** The player left the world. */
    public static void clear() {
        state = HeroNet.Sync.none();
        kind = null;
        COOL.clear();
    }

    // ================================================================ readers
    /** The bound weapon, or null when the player has none (or the server has not told us yet). */
    public static HeroKind kind() { return kind; }

    public static boolean bound() { return kind != null; }

    public static long exp() { return state.exp(); }

    public static int level() { return HeroLevels.level(state.exp()); }

    /** The id of the active form. */
    public static String form() { return state.form(); }

    /** The display name of the active form (empty when the server did not say). */
    public static String formName() { return state.formName(); }

    public static boolean summoned() { return state.summoned(); }

    /** Another Holy wielder is within the repel radius: no experience. */
    public static boolean repelled() { return (state.flags() & HeroNet.FLAG_REPELLED) != 0; }

    /** A curse form is active. */
    public static boolean cursed() { return (state.flags() & HeroNet.FLAG_CURSED) != 0; }

    /** The weapon holds its wielder (an Overrun). */
    public static boolean locked() { return (state.flags() & HeroNet.FLAG_LOCKED) != 0; }

    /** The forms learned, in the order they were learned (the base form first). */
    public static List<String> unlocked() { return state.unlocked(); }

    /** Mastery points per form id. */
    public static Map<String, Integer> mastery() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (HeroNet.Entry e : state.mastery()) out.put(e.key(), e.value());
        return out;
    }

    /** The largest few absorbed items (item id and count). */
    public static List<HeroNet.Entry> absorbed() { return state.absorbed(); }

    /** How many different items the gem has absorbed, and how many in all. */
    public static int absorbedKinds() { return state.absorbedKinds(); }

    public static long absorbedTotal() { return state.absorbedTotal(); }

    /** The skill ids on the active form's five inputs: use, sneak, key one, key two, key three (empty text for none). */
    public static List<String> slots() { return state.slots(); }

    /** Ticks left of a technique's cooldown (0 when ready). */
    public static int cooldownLeft(String skillId) {
        Cool c = COOL.get(skillId);
        if (c == null) return 0;
        long left = c.readyAt() - clock;
        if (left <= 0L) {
            COOL.remove(skillId);
            return 0;
        }
        return (int) Math.min(left, Integer.MAX_VALUE);
    }

    /** The whole length of the technique's last cooldown in ticks (1 when it never ran). */
    public static int cooldownTotal(String skillId) {
        Cool c = COOL.get(skillId);
        return c == null ? 1 : c.total();
    }
}
