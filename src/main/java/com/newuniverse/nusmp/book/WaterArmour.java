package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.93.0 (the Water / Dark / Lightning / Flame rebuild) wrapped every page of the four rebuilt books so that a cast put the magic's temporary armour on the caster for 5 s. 1.99.0
 * (armour policy, {@link ArmourPolicy}): a cast never equips. The methods keep their names and signatures so the page lists and spells that call them are unchanged, but:
 * <ul>
 *   <li>{@link #wrap} returns the page as it is: the armour is not a side effect of a bolt;</li>
 *   <li>{@link #put} does nothing: the armour of Water (Valkyrie Dress, Sea Dragon's Scale Mail), Lightning (Thunder Fiend ...), Dark (Dark Cloaked Guard) and Flame (Flame Armour) is put
 *       on by its own page, through {@link ArmourState} (the policy table names the pages), and stays until toggled off;</li>
 *   <li>{@link #worn} asks {@link ArmourState}: mechanics tied to the look (Flame Armour's burning blows) follow the armour that is really on.</li>
 * </ul>
 */
public final class WaterArmour {
    private WaterArmour() {}

    /** The length of the old per-cast armour (kept for the pure rule and its tests). */
    public static final int CAST_TICKS = WaterArmourRule.CAST_TICKS;

    /** The game time until which the caster's armour of this kind runs: effectively unbounded while it is worn (it has no end of its own), 0 when it is not. */
    public static long until(ServerPlayer p, Aura a) { return worn(p, a) ? Long.MAX_VALUE : 0L; }

    /** True while the caster wears the armour (a mechanic that is tied to the look, e.g. Flame Armour's burning blows): the armour of this layer is on, as armour or as the form. */
    public static boolean worn(ServerPlayer p, Aura a) {
        ArmourRules.Held h = ArmourState.held(p);
        if (h == null) return false;
        ArmourPolicy.Entry e = ArmourPolicy.get().entry(h.magic());
        return e != null && a.name().equals(e.aura());
    }

    /** The pure rule of the old per-cast armour (kept, and tested, as it was). */
    public static long nextEnd(long now, long currentEnd, int ticks, boolean running, boolean sameStyle) { return WaterArmourRule.nextEnd(now, currentEnd, ticks, running, sameStyle); }

    /** 1.99.0: does nothing. A spell no longer puts the armour on; the armour pages do, through ArmourState. */
    public static void put(ServerPlayer p, Aura a, int ticks, int style) { }

    /** 1.99.0: the page as it is. Every successful cast used to put the armour on for 5 s; casting never equips any more. */
    public static BookPage wrap(BookPage page, Aura a, int ticks, int style) { return page; }

    /** {@link #wrap} for an ordinary cast: the page as it is. */
    public static BookPage wrap(BookPage page, Aura a) { return page; }
}
