package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.93.0 magic rebuild: the shared server side of the five rebuilt temporary armours (Spatial, Gravity, Permeation, Mirror, Slash). A page cast wrapped in
 * {@link #armoured} puts the magic's armour on the caster for five seconds after the cast went off (nothing when the cast did nothing), so every page of a
 * rebuilt book forges the set in on the caster and shatters it out again, exactly as the Djinn's full equip does (client.aura.EquipAnimation). The style is
 * the painter's variant: 0 is the plain set, 1 the signature set (a heavier page). Nothing is ever put in an inventory or armour slot.
 */
public final class SpatialArmourCast {
    private SpatialArmourCast() {}

    /** Five seconds, the length of the armour after a cast (the last 18 ticks are the shatter). */
    public static final int TICKS = 100;

    public static BookPage.Cast armoured(BookPage.Cast inner, Aura aura, int style) {
        return (b, i, p, mode) -> {
            boolean ok = inner.cast(b, i, p, mode);
            if (ok) on(p, aura, TICKS, style);
            return ok;
        };
    }

    public static void on(ServerPlayer p, Aura aura, int ticks, int style) { PlayerAuras.set(p, aura, ticks, style); }

    /** A line of text above the hotbar, from the lang file with an English fallback (the fragment tools/lang/*_rebuild.json carries the keys). */
    public static void say(ServerPlayer p, String key, String fallback) { p.displayClientMessage(Component.translatableWithFallback(key, fallback), true); }
}
