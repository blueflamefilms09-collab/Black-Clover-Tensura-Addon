package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.93.0 (the Water / Dark / Lightning / Flame rebuild): how a cast puts the temporary armour on the caster. Every page of the four rebuilt books is wrapped by {@link #wrap}: when the
 * cast succeeded the caster wears the magic's armour (client.aura.WaterAura / DarkAura / LightningAura / FlameAura: a set of pieces forged in by the Djinn full-equip animation) for
 * {@link #CAST_TICKS} ticks (5 s); the armour pages (Valkyrie Dress, Thunder Fiend, Flame Armour ...) keep it for their whole duration with {@link #put}. A longer armour that is already
 * on is never shortened by a later short cast. Nothing is put in an inventory or an armour slot: the armour is a render layer on the player's model.
 */
public final class WaterArmour {
    private WaterArmour() {}

    /** An ordinary cast: 5 seconds. */
    public static final int CAST_TICKS = WaterArmourRule.CAST_TICKS;

    private static String key(Aura a) { return "nusmp_rb_armour_" + a.name().toLowerCase(java.util.Locale.ROOT); }

    /** The game time until which the caster's armour of this kind runs (0 = none). */
    public static long until(ServerPlayer p, Aura a) { return p.getPersistentData().getLong(key(a)); }

    /** True while the caster wears the armour (a mechanic that is tied to the look, e.g. Flame Armour's burning blows). */
    public static boolean worn(ServerPlayer p, Aura a) { return until(p, a) > p.level().getGameTime(); }

    /**
     * The pure rule behind {@link #put}: the new end time of the armour, or -1 when the armour that is on already lasts at least as long and has the same style (it is left alone).
     * A different style always replaces what is on.
     */
    public static long nextEnd(long now, long currentEnd, int ticks, boolean running, boolean sameStyle) { return WaterArmourRule.nextEnd(now, currentEnd, ticks, running, sameStyle); }

    /** Puts the armour on for 'ticks' (style = the painter's variant); a longer armour of the same style that is already on stays. */
    public static void put(ServerPlayer p, Aura a, int ticks, int style) {
        long now = p.level().getGameTime();
        boolean running = until(p, a) > now && PlayerAuras.active(p, a);
        long end = nextEnd(now, until(p, a), ticks, running, running && PlayerAuras.style(p, a) == style);
        if (end < 0) return;
        p.getPersistentData().putLong(key(a), end);
        PlayerAuras.set(p, a, (int) (end - now), style);
    }

    /** The page with the same id, name, cost and cooldown whose cast, when it succeeds, puts the armour on for 'ticks'. */
    public static BookPage wrap(BookPage page, Aura a, int ticks, int style) {
        BookPage.Cast inner = page.cast();
        BookPage.Cast armed = (b, i, p, mode) -> {
            boolean ok = inner.cast(b, i, p, mode);
            if (ok) put(p, a, ticks, worn(p, a) && PlayerAuras.active(p, a) ? PlayerAuras.style(p, a) : style);       // an armour that is already on keeps its style
            return ok;
        };
        return new BookPage(page.id(), page.name(), page.incantation(), page.costPercent(), page.costFloor(), page.cooldown(), armed, page.anim(), page.weaponTag());
    }

    /** {@link #wrap} for an ordinary cast (5 s, style 0). */
    public static BookPage wrap(BookPage page, Aura a) { return wrap(page, a, CAST_TICKS, 0); }
}
