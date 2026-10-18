package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroColors;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.bond.HeroSync;
import com.newuniverse.nusmp.heroweapon.forms.FormMastery;
import com.newuniverse.nusmp.heroweapon.rules.HeroXp;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the dispatcher. One entry point turns an input (the use key, sneak + use, a hero key) into a skill cast: it finds the bond and the ACTIVE form, takes the
 * skill the form lists on that input, refuses when the weapon is not summoned or the skill is on cooldown or the wielder cannot pay, waits out the wind-up, casts,
 * and only then starts the cooldown. Doing all of this in one place is what keeps the owner's balance rules true for every weapon (a skill can never forget its own
 * cooldown, nor cast for free): the cost is paid first and given back if the technique then refuses, fails or is cancelled; the cooldown (shortened a little by
 * the bond level, never below the configured floor) and a short global lock between two techniques start only when the technique really happened; a wind-up is
 * a server-side timer that is cancelled when the wielder dies, puts the weapon away, leaves the dimension or logs out. A technique that throws is caught, logged
 * once, refunded and treated as a refusal. After a good cast the dispatcher tells the forms system (mastery), grants the experience, fires the quest hook
 * {@code hero.skill}, and tells the client (the HUD's cooldown bar). Cooldown state is not saved. Server thread only.
 */
public final class HeroUse {
    private HeroUse() {}

    private static final CooldownBook BOOK = new CooldownBook();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static long nextTicket = 1L;

    /** A technique waiting out its wind-up: which timer is current, what was paid, where the wielder stood. */
    private record Pending(long ticket, String skill, ResourceKey<Level> dimension, HeroPay.Receipt receipt) {}

    // ================================================================ the entry point
    /** Fires the skill on {@code trigger} for the player; true when a skill was cast (or its wind-up began). Never throws. */
    public static boolean fire(ServerPlayer player, HeroTrigger trigger) {
        boolean[] done = {false};
        SafeCast.tick("hero.use", () -> done[0] = fireInner(player, trigger));
        return done[0];
    }

    private static boolean fireInner(ServerPlayer player, HeroTrigger trigger) {
        if (!HeroConfig.enabled()) return false;
        HeroCtx c = HeroCtx.of(player);
        if (c == null) return false;
        String id = c.form.skill(trigger);
        HeroSkill skill = id == null ? null : HeroLogics.skill(id);
        if (skill == null) {
            if (id != null) SafeCast.fault("hero.use.missing." + id, new IllegalStateException("the form " + c.form.id() + " of " + c.kind + " lists the skill " + id + ", which no logic registered"));
            if (trigger.ordinal() >= HeroTrigger.KEY_1.ordinal() && HeroConfig.showMessages()) player.displayClientMessage(Component.translatable("hero.msg.no_skill"), true);
            return false;
        }
        boolean ok = run(player, c, skill);
        if (ok && trigger.ordinal() >= HeroTrigger.KEY_1.ordinal()) player.swing(InteractionHand.MAIN_HAND, true);
        return ok;
    }

    /**
     * The dispatcher for a skill already chosen: every check, the payment, the wind-up, the cast. It is also the door of the self-test, which runs a private test
     * skill through exactly this path. The context's stack must be the weapon in the main hand.
     */
    public static boolean run(ServerPlayer player, HeroCtx c, HeroSkill skill) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        UUID id = player.getUUID();
        boolean summoned = player.isCreative() || c.bond.summoned();
        boolean held = !c.stack.isEmpty();
        DispatchRules.Refusal why = DispatchRules.check(summoned, held, true, BOOK.left(id, skill.id(), now), BOOK.lockLeft(id, now), PENDING.containsKey(id));
        if (why != DispatchRules.Refusal.NONE) {
            refuse(player, skill, why, now);
            return false;
        }
        HeroPay.Receipt receipt = HeroPay.pay(c, skill.magoiShare());
        if (receipt == null) {
            say(player, Component.translatable("hero.msg.no_mana", skillName(skill)));
            return false;
        }
        int wind = Math.max(0, skill.windUpTicks());
        if (wind <= 0) return cast(player, c, skill, receipt, now);
        long ticket = nextTicket++;
        PENDING.put(id, new Pending(ticket, skill.id(), level.dimension(), receipt));
        windUpTell(player, c, skill, wind);
        SpellRuntime.later(level, wind, () -> SafeCast.tick("hero.windup", () -> finishWindUp(player, ticket, skill)));
        return true;
    }

    // ================================================================ wind-up
    private static void windUpTell(ServerPlayer player, HeroCtx c, HeroSkill skill, int wind) {
        ServerLevel level = player.serverLevel();
        VfxSpawn.sendFollowing(level, VfxShape.MANA_CHARGE, player, player.position().add(0, 1.0, 0), HeroColors.argb(c.kind), Math.max(6, wind), 1.0f);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 0.7f + Math.min(0.8f, wind / 40f));
        say(player, Component.empty().append(skillName(skill)).append("..."));
    }

    private static void finishWindUp(ServerPlayer player, long ticket, HeroSkill skill) {
        Pending p = PENDING.get(player.getUUID());
        if (p == null || p.ticket() != ticket) return;                          // cancelled meanwhile
        PENDING.remove(player.getUUID());
        HeroCtx c = player.isAlive() ? HeroCtx.of(player) : null;
        boolean ok = c != null && !player.hasDisconnected() && player.serverLevel().dimension() == p.dimension()
                && (player.isCreative() || c.bond.summoned()) && !c.stack.isEmpty();
        if (!ok) {
            HeroPay.refund(player, p.receipt());
            return;
        }
        cast(player, c, skill, p.receipt(), player.serverLevel().getGameTime());
    }

    /** Cancels the wielder's wind-up, if one is running (death, dismiss, release, dimension change, logout). {@code refund}: give the cost back. */
    public static void cancelPending(ServerPlayer player, boolean refund) {
        Pending p = PENDING.remove(player.getUUID());
        if (p != null && refund) HeroPay.refund(player, p.receipt());
    }

    /** Is a wind-up running for this player? */
    public static boolean windingUp(ServerPlayer player) { return PENDING.containsKey(player.getUUID()); }

    // ================================================================ the cast
    private static boolean cast(ServerPlayer player, HeroCtx c, HeroSkill skill, HeroPay.Receipt receipt, long now) {
        boolean ok;
        try {
            ok = skill.cast(c);
        } catch (RuntimeException | LinkageError broken) {
            SafeCast.fault("hero.skill." + skill.id(), broken);
            ok = false;
        }
        if (!ok) {
            HeroPay.refund(player, receipt);
            return false;
        }
        int base;
        try {
            base = skill.cooldownTicks(c);
        } catch (RuntimeException broken) {
            SafeCast.fault("hero.skill.cooldown." + skill.id(), broken);
            base = 200;
        }
        int ticks = DispatchRules.cooldownTicks(base, c.bondLevel(), HeroConfig.minCooldownTicks());
        int lock = HeroConfig.skillGlobalLockTicks();
        if (player.isCreative()) {
            ticks = DispatchRules.creativeTicks(ticks, 10);
            lock = Math.min(lock, 10);
        }
        UUID id = player.getUUID();
        BOOK.start(id, skill.id(), now, ticks);
        BOOK.lock(id, now, lock);
        HeroSync.sendCooldown(player, skill.id(), ticks);
        afterCast(player, skill);
        return true;
    }

    private static void afterCast(ServerPlayer player, HeroSkill skill) {
        SafeCast.tick("hero.mastery", () -> FormMastery.skill(player, skill));
        SafeCast.tick("hero.xp", () -> HeroXp.gain(player, 2));
        NusmpEvents.fire(player, "hero.skill", skill.id());
    }

    // ================================================================ the drawn use
    /**
     * The release of a drawn or charged use (a Bow's string let go): hands it to the weapon's logic; true when the logic handled it. It costs the dispatcher
     * nothing and starts no cooldown: the shot is the weapon's own, its techniques are the dispatcher's.
     */
    public static boolean release(ServerPlayer player, int chargeTicks) {
        boolean[] handled = {false};
        SafeCast.tick("hero.release", () -> {
            if (!HeroConfig.enabled()) return;
            HeroCtx c = HeroCtx.of(player);
            if (c == null || c.stack.isEmpty() || !(player.isCreative() || c.bond.summoned())) return;
            handled[0] = HeroLogics.of(c.kind).onRelease(c, Math.max(0, chargeTicks));
        });
        return handled[0];
    }

    // ================================================================ refusals
    private static void refuse(ServerPlayer player, HeroSkill skill, DispatchRules.Refusal why, long now) {
        UUID id = player.getUUID();
        switch (why) {
            case NOT_SUMMONED -> say(player, Component.translatable("hero.msg.not_out"));
            case NOT_HELD -> say(player, Component.translatable("hero.msg.not_held"));
            case COOLDOWN -> say(player, Component.translatable("hero.msg.cooldown", skillName(skill), seconds(BOOK.left(id, skill.id(), now))));
            case LOCK -> say(player, Component.translatable("hero.msg.lock", seconds(BOOK.lockLeft(id, now))));
            default -> { }
        }
    }

    private static Component skillName(HeroSkill skill) { return Component.translatableWithFallback("hero.skill." + skill.id() + ".name", skill.name()); }

    private static String seconds(int ticks) { return String.format(Locale.ROOT, "%.1f", ticks / 20.0); }

    private static void say(ServerPlayer player, Component text) {
        if (HeroConfig.showMessages()) player.displayClientMessage(text, true);
    }

    // ================================================================ state for the status command, the tests and the housekeeping
    /** Ticks left of a technique's cooldown for a player (0 when ready). */
    public static int cooldownLeft(ServerPlayer player, String skillId) { return BOOK.left(player.getUUID(), skillId, player.level().getGameTime()); }

    /** Ticks left of the lock between two techniques of a player. */
    public static int lockLeft(ServerPlayer player) { return BOOK.lockLeft(player.getUUID(), player.level().getGameTime()); }

    /** The cooldowns still running for a player, skill id to ticks left. */
    public static Map<String, Integer> running(ServerPlayer player) { return BOOK.running(player.getUUID(), player.level().getGameTime()); }

    /** Clears a player's cooldowns and lock (a self-test cleaning up, or the weapon released). */
    public static void clearCooldowns(UUID player) { BOOK.forget(player); }

    /** Forgets a player entirely (they left). */
    public static void forget(UUID player) {
        BOOK.forget(player);
        PENDING.remove(player);
    }

    /** Forgets everything (the server stopped). */
    public static void clear() {
        BOOK.clear();
        PENDING.clear();
    }
}
