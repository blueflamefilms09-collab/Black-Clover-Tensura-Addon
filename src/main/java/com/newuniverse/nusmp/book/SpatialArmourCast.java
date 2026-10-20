package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.93.0 magic rebuild put the temporary armour of Spatial, Gravity, Permeation, Mirror and Slash on the caster for five seconds after every page. 1.99.0 (armour policy,
 * {@link ArmourPolicy}): none of the five has an armour in the anime, so a cast never equips. The two methods keep their names and signatures (the page lists and spells that use them
 * are unchanged): {@link #armoured} returns the cast as it is and {@link #on} does nothing. The five painters (client.aura.SpatialAura ...) and their models stay registered.
 */
public final class SpatialArmourCast {
    private SpatialArmourCast() {}

    /** Five seconds, the length the armour had after a cast (kept for the tests that pin the old rule). */
    public static final int TICKS = 100;

    /** 1.99.0: the cast as it is (it used to put the armour on after every success). */
    public static BookPage.Cast armoured(BookPage.Cast inner, Aura aura, int style) { return inner; }

    /** 1.99.0: does nothing (it used to put the armour on). */
    public static void on(ServerPlayer p, Aura aura, int ticks, int style) { }

    /** A line of text above the hotbar, from the lang file with an English fallback (the fragment tools/lang/*_rebuild.json carries the keys). */
    public static void say(ServerPlayer p, String key, String fallback) { p.displayClientMessage(Component.translatableWithFallback(key, fallback), true); }
}
