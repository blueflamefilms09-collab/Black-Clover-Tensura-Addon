package com.newuniverse.nusmp.heroweapon.curse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Why: the Curse Series (a weapon's answer to despair, tied to the seven sins) touches the other hero systems at a few points only, and this is that small door.
 * The core asks whether a curse form is on (the look, the HUD) and whether the weapon holds its wielder (the loan refuses a dismiss, the forms refuse a change of
 * form); the weapon logics and the forms ask for the corruption; the events that make a person despair report here; and the dispatcher tells the curse about each
 * technique ({@link #onTechnique}) and asks whether the curse forbids one ({@link #refuses}). Every question works for a player on either side: a server player is
 * read from the bond, the local player of a client from the last sync packet ({@link CurseView}), so the HUD and the look need no packet of their own. A player who
 * never despaired is answered "no curse, no corruption", and every report about a player without a hero weapon, in creative or in spectator, is ignored. The state,
 * the rules and the effects are in the classes behind this one; nothing here throws.
 */
public final class HeroCurse {
    private HeroCurse() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    /** A report or a hook of another system must never break that system: a failure is logged once and the call is dropped. */
    private static void failed(String what, RuntimeException e) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] HeroCurse.{} failed and was ignored (further failures are not logged): {}", what, e.toString(), e);
    }

    // ------------------------------------------------------------------ questions (both sides)
    /** True while the player's active form is a Curse Series form. */
    public static boolean active(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide) return CurseView.active() != null;
        HeroBond b = HeroBonds.of(player);
        if (!b.isBound()) return false;
        HeroForm f = HeroForms.active(b);
        return f != null && f.curse();
    }

    /** True while the weapon holds its wielder (an Overrun): the weapon cannot be dismissed and the form cannot be changed. */
    public static boolean locked(Player player) {
        if (player == null) return false;
        long now = player.level().getGameTime();
        if (player.level().isClientSide) return CurseView.locked(now);
        HeroBond b = HeroBonds.of(player);
        return b.isBound() && CurseStore.overrunUntil(b) > now;
    }

    /** The corruption of the player, 0 to 1000 (1000 is an Overrun). */
    public static int corruption(Player player) {
        if (player == null) return 0;
        if (player.level().isClientSide) return CurseView.corruption();
        return CurseStore.corruption(HeroBonds.of(player));
    }

    /** Despair of the player, 0 to 1000; at the unlock threshold the weapon offers the Curse Series. */
    public static int despair(Player player) {
        if (player == null) return 0;
        if (player.level().isClientSide) return CurseView.despair();
        return CurseStore.despair(HeroBonds.of(player));
    }

    /** True when the player has embraced the Curse Series of this sin. */
    public static boolean hasSeries(Player player, Sin sin) {
        if (player == null || sin == null) return false;
        if (player.level().isClientSide) return CurseView.hasSeries(sin);
        return CurseStore.hasSeries(HeroBonds.of(player), sin);
    }

    /** The sin of the curse form the player wears, or null. */
    public static Sin activeSin(Player player) {
        if (player == null) return null;
        if (player.level().isClientSide) return CurseView.active();
        HeroBond b = HeroBonds.of(player);
        if (!b.isBound()) return null;
        HeroForm f = HeroForms.active(b);
        return f != null && f.curse() ? f.sin() : null;
    }

    /** The hearts of maximum health the player's Curse Series have taken for good (known on the server; 0 on a client). */
    public static int tax(Player player) { return player == null || player.level().isClientSide ? 0 : CurseStore.tax(HeroBonds.of(player)); }

    // ------------------------------------------------------------------ reports (server)
    /**
     * A cause of despair happened to this player (an ally died, a betrayal, a near-death). {@code amount} is in despair points; {@code why} a short key for the log
     * and for the pause between two reports of the same kind (the keys of {@link Cause}; an unknown key is counted with no pause and at most 500 points).
     * Ignored for a player with no hero weapon, in creative or spectator, or while the Curse Series is switched off.
     */
    public static void addDespair(ServerPlayer player, int amount, String why) {
        if (player == null || amount <= 0 || !CurseConfig.enabled() || CurseService.exempt(player)) return;
        try {
            Cause cause = Cause.byKey(why);
            CurseService.addDespair(player, cause, cause == Cause.OTHER ? Math.min(amount, 500) : amount, CurseService.now(player), CurseEvents.tuning(player.getServer()));
        } catch (RuntimeException e) {
            failed("addDespair", e);
        }
    }

    /** Corruption from outside the curse forms themselves (a skill that costs its wielder a piece of their mind). A negative amount lowers it. */
    public static void addCorruption(ServerPlayer player, int amount) {
        if (player == null || amount == 0 || !CurseConfig.enabled() || CurseService.exempt(player)) return;
        try {
            CurseService.addCorruption(player, amount, CurseService.now(player), CurseEvents.tuning(player.getServer()));
        } catch (RuntimeException e) {
            failed("addCorruption", e);
        }
    }

    // ------------------------------------------------------------------ the dispatcher's two calls (server)
    /**
     * The dispatcher cast a technique successfully: with a curse form worn it adds corruption (an ultimate more), Greed charges its surcharge and Sloth starts its
     * lock. Call it right after {@code FormMastery.skill}.
     */
    public static void onTechnique(ServerPlayer player, HeroSkill skill) {
        if (player == null || skill == null || !CurseConfig.enabled()) return;
        try {
            CurseService.onTechnique(player, skill, CurseService.now(player), CurseEvents.tuning(player.getServer()));
        } catch (RuntimeException e) {
            failed("onTechnique", e);
        }
    }

    /**
     * Asks whether the curse forbids this technique right now: the weapon holds the wielder (when the config says techniques stop), Envy has taken it, or Sloth
     * holds the hand back. Returns the line to show the wielder, or null when the technique may go on. Call it before the cost is paid.
     */
    public static Component refuses(ServerPlayer player, HeroSkill skill) {
        if (player == null || skill == null || !CurseConfig.enabled()) return null;
        try {
            return CurseService.refuses(player, skill, CurseService.now(player), CurseEvents.tuning(player.getServer()));
        } catch (RuntimeException e) {
            failed("refuses", e);
            return null;
        }
    }
}
