package com.newuniverse.nusmp.arc.tier;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.early.EarlyMoves;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the game side of {@link TierTrait} (docs/bosses/tier_powers.md): what each anime power of a light boss does in the world. {@code TierBoss} calls these hooks, nothing else does; the
 * numbers are the constants of {@link TierTrait} multiplied by the server setting {@code tierPowerScale} (never a duration), and every hook is a no-op when {@code tierPowers} is off, on
 * the client, or for a boss without traits.
 *
 * <p>Cost (worst case, per boss): one entity query a second for the slow field or the sight (at most 24 foes, {@link EarlyMoves#MAX_FOES}), one attribute check a second, a few
 * tracked players in {@link TierState} (at most 24 per map, pruned a second), one small particle ring a second while a field or stance is up, at most 4 delayed pulls and 1 delayed echo per
 * use of a bound move, no entity is spawned and no block is touched. A hook that throws is caught by the caller and logged; it never takes the tick down.
 */
public final class TierTraits {
    private TierTraits() {}

    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "tier_trait_speed");

    private static boolean active(TierBoss b) {
        return !b.level().isClientSide && ArcConfig.tierPowers() && !b.row().traits().isEmpty();
    }

    private static double scale() { return ArcConfig.tierPowerScale(); }

    private static long now(TierBoss b) { return b.level().getGameTime(); }

    private static double healthFraction(TierBoss b) { return b.getHealth() / Math.max(1f, b.getMaxHealth()); }

    /** Tells one player, on the action bar (nothing for a summon or a mob). */
    private static void notice(LivingEntity e, String text) {
        if (e instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal(text), true);
    }

    /** The player a hit belongs to: the player, or the owner of their summon (null for anything else). */
    private static UUID fighterOf(Entity attacker) {
        if (attacker instanceof ServerPlayer p) return p.getUUID();
        if (attacker instanceof OwnableEntity o && o.getOwnerUUID() != null) return o.getOwnerUUID();
        return null;
    }

    // ================================================================ every second
    /** Called by the boss every tick; does its work once a second (spread over the bosses by their ids). */
    public static void tick(TierBoss b) {
        if (b.level().isClientSide || (b.tickCount + b.getId()) % 20 != 0) return;
        if (!(b.level() instanceof ServerLevel sl)) return;
        updateSpeed(b);
        if (!active(b)) return;
        TierState st = b.traitState();
        long now = sl.getGameTime();
        st.prune(now);
        TierRow row = b.row();
        double hp = healthFraction(b);
        announceFusion(b, sl, row, hp);
        if (!b.fightActive() || !b.isAlive()) return;
        int rgb = b.styleColor();
        if (row.has(TierTrait.SLOW_FIELD)) {
            double r = Math.min(TierTrait.MAX_RADIUS, (b.gatesOpen() ? TierTrait.SLOW_RADIUS_GATES : TierTrait.SLOW_RADIUS) * scale());
            for (LivingEntity e : EarlyMoves.inCircle(b, b.position(), r)) EarlyMoves.slow(e, TierTrait.SLOW_TICKS, 0);
            Tells.ring(sl, b.position().add(0, 0.1, 0), r, rgb, 0.8f, 16);
        }
        if (row.has(TierTrait.SIGHT)) {
            double r = Math.min(TierTrait.MAX_RADIUS, TierTrait.SIGHT_REACH * scale());
            for (LivingEntity e : EarlyMoves.inCircle(b, b.position(), r))
                if (e.isInvisible() || e.isCrouching()) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, TierTrait.SIGHT_TICKS, 0, false, false));
        }
        if (st.mirror(now) || st.guard(now)) Tells.ring(sl, b.position().add(0, 0.1, 0), 1.8, b.styleAccent(), 1.0f, 12);
    }

    /** The climb and the fusion make him faster: one transient modifier, replaced only when the number changes (and removed when the powers are off). */
    private static void updateSpeed(TierBoss b) {
        AttributeInstance a = b.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) return;
        double bonus = active(b) ? TierTrait.selfSpeed(b.row().traits(), b.power(), healthFraction(b), scale()) : 0.0;
        AttributeModifier cur = a.getModifier(SPEED_ID);
        if (bonus < 1e-6) {
            if (cur != null) a.removeModifier(SPEED_ID);
            return;
        }
        if (cur != null && Math.abs(cur.amount() - bonus) < 1e-6) return;
        if (cur != null) a.removeModifier(SPEED_ID);
        a.addTransientModifier(new AttributeModifier(SPEED_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void announceFusion(TierBoss b, ServerLevel sl, TierRow row, double hp) {
        if (!row.has(TierTrait.FUSION)) return;
        TierState st = b.traitState();
        int steps = TierTrait.fusionSteps(hp);
        if (steps > st.fusionSeen() && b.fightActive()) {
            st.fusionSeen(steps);
            b.announce(b.bossName() + " fuses with the power he holds - stronger, faster (" + steps + "/" + TierTrait.FUSION_AT.length + ").");
            Tells.ring(sl, b.position().add(0, 0.1, 0), 3.5, b.styleColor(), 1.5f, 24);
            Tells.burst(sl, b.position().add(0, 1.0, 0), b.styleAccent(), 12);
        } else if (steps != st.fusionSeen() && !b.fightActive()) {
            st.fusionSeen(steps);           // a world that loads mid-fight, or a reset: no announcement, just the count
        } else if (steps < st.fusionSeen()) {
            st.fusionSeen(steps);
        }
    }

    // ================================================================ what his own hits do
    /** The base of a strike on this victim (the Karma mark makes his blows hit harder). */
    public static float strikeBase(TierBoss b, LivingEntity victim, float base) {
        if (victim == null || !active(b) || !b.row().has(TierTrait.KARMA)) return base;
        if (!b.traitState().karma(victim.getUUID(), now(b))) return base;
        return (float) (base * (1.0 + (TierTrait.KARMA_DAMAGE - 1.0) * scale()));
    }

    /** After a strike landed: poison, soul marks, system lock, shield break. {@code blocking} = the victim had a raised shield when the blow came. */
    public static void afterStrike(TierBoss b, LivingEntity victim, boolean blocking) {
        if (victim == null || !victim.isAlive() || !active(b)) return;
        TierRow row = b.row();
        TierState st = b.traitState();
        long now = now(b);
        if (row.has(TierTrait.SPORE_POISON)) victim.addEffect(new MobEffectInstance(MobEffects.POISON, TierTrait.POISON_TICKS, 0));
        if (row.has(TierTrait.SOUL_MARK)) {
            int n = st.addMark(victim.getUUID(), now);
            if (n >= TierTrait.MARK_MAX) {
                st.clearMarks(victim.getUUID());
                EarlyMoves.weaken(victim, 100, 0);
                st.startFrenzy(now);
                notice(victim, "Three soul marks: you are weakened and " + b.bossName() + " is in a frenzy!");
                if (b.level() instanceof ServerLevel sl) Tells.burst(sl, victim.position().add(0, 1.0, 0), b.styleColor(), 10);
            } else {
                notice(victim, "Soul mark " + n + "/" + TierTrait.MARK_MAX);
            }
        }
        if (row.has(TierTrait.SYSTEM_LOCK) && st.lockReady(victim.getUUID(), now) && strip(victim, false) > 0) notice(victim, "System lock: an effect was stripped.");
        if (row.has(TierTrait.SHIELD_BREAK) && blocking) breakShield(victim, TierTrait.SHIELD_TICKS);
    }

    /** Disables the raised shield of a player for {@code ticks} (the cooldown of the item in use, so a modded shield works too). */
    public static void breakShield(LivingEntity e, int ticks) {
        if (!ArcConfig.tierPowers() || ticks <= 0 || !(e instanceof Player p) || !p.isBlocking()) return;
        ItemStack use = p.getUseItem();
        p.getCooldowns().addCooldown(use.getItem(), ticks);
        p.stopUsingItem();
        notice(p, "Your shield is broken!");
    }

    /** Removes one beneficial, timed effect (or every one when {@code all}); returns how many were removed. */
    public static int strip(LivingEntity e, boolean all) {
        if (e == null || !ArcConfig.tierPowers()) return 0;
        List<Holder<MobEffect>> found = null;
        for (MobEffectInstance i : e.getActiveEffects()) {
            if (!i.getEffect().value().isBeneficial() || i.isInfiniteDuration()) continue;
            if (found == null) found = new ArrayList<>(3);
            found.add(i.getEffect());
            if (!all) break;
        }
        if (found == null) return 0;
        int n = 0;
        for (Holder<MobEffect> h : found) if (e.removeEffect(h)) n++;
        return n;
    }

    // ================================================================ what happens to a hit he takes
    /** Armour and the guard cut the hit (not applied while he is exposed: the framework only calls this when he is not). */
    public static float mitigate(TierBoss b, DamageSource source, float amount) {
        if (!active(b)) return amount;
        TierRow row = b.row();
        double keep = 1.0;
        if (row.has(TierTrait.ARMOUR) && isMelee(source)) keep *= 1.0 - Math.min(TierTrait.MAX_CUT, TierTrait.ARMOUR_CUT * scale());
        if (row.has(TierTrait.UNTOUCHABLE) && b.traitState().guard(now(b))) keep *= 1.0 - Math.min(TierTrait.MAX_CUT, TierTrait.UNTOUCHABLE_CUT * scale());
        keep = Math.max(1.0 - TierTrait.MAX_CUT, keep);
        if (keep < 1.0 && b.level() instanceof ServerLevel sl) Tells.burst(sl, b.position().add(0, b.getBbHeight() * 0.6, 0), b.styleAccent(), 2);
        return (float) (amount * keep);
    }

    /** A blow of a body: the attacker is the thing that hits, no projectile, no magic, nothing that goes round armour. */
    static boolean isMelee(DamageSource s) {
        Entity direct = s.getDirectEntity();
        return direct instanceof LivingEntity && direct == s.getEntity() && !s.is(DamageTypeTags.BYPASSES_ARMOR) && !s.is(DamageTypeTags.IS_PROJECTILE);
    }

    /** After the boss took {@code dealt} health from {@code source}: the Balance's window, the steam, the mirror. */
    public static void afterHurt(TierBoss b, DamageSource source, float dealt) {
        if (dealt <= 0f || !active(b)) return;
        TierRow row = b.row();
        TierState st = b.traitState();
        long now = now(b);
        Entity attacker = source.getEntity();
        if (row.has(TierTrait.BALANCE)) {
            UUID who = fighterOf(attacker);
            if (who != null) st.addWindow(who, dealt, now);
        }
        if (!(attacker instanceof LivingEntity le) || !b.isFoe(le)) return;
        if (row.has(TierTrait.STEAM) && isMelee(source) && b.distanceTo(le) <= Math.min(8.0, TierTrait.STEAM_REACH * scale()) && st.steamReady(le.getUUID(), now)) {
            le.setRemainingFireTicks(Math.max(le.getRemainingFireTicks(), TierTrait.STEAM_FIRE_TICKS));
            if (b.level() instanceof ServerLevel sl) Tells.burst(sl, le.position().add(0, 1.0, 0), b.styleAccent(), 4);
        }
        if (row.has(TierTrait.MIRROR) && st.mirror(now)) {
            float give = (float) Math.min(TierTrait.MIRROR_CAP_HP, dealt * TierTrait.MIRROR_SHARE * scale());
            if (le instanceof Player) {
                give = Math.min(give, le.getHealth() - 1f);                                                           // the mirror never kills
                give = Math.min(give, (float) (le.getMaxHealth() * NUGameRules.pvpHitCap(b.level())));                // and obeys the balance law's cap per hit
            }
            if (give >= 0.5f) {
                le.hurt(b.damageSources().indirectMagic(b, b), give);
                if (b.level() instanceof ServerLevel sl) Tells.burst(sl, le.position().add(0, 1.0, 0), b.styleColor(), 4);
            }
        }
    }

    /** The multiplier on every blow of his that his own traits give right now (climb, fusion, frenzy). */
    public static float damageMultiplier(TierBoss b) {
        if (!active(b)) return 1f;
        return (float) TierTrait.selfDamage(b.row().traits(), b.power(), healthFraction(b), scale(), b.traitState().frenzy(now(b)));
    }

    // ================================================================ the moves the traits hang on
    /** A telegraph of a move was sent: the Balance's window opens with the wind-up of its move. */
    public static void onTell(TierBoss b, TierMove m) {
        if (!active(b) || !b.row().has(TierTrait.BALANCE) || !m.id().equals(b.row().moveOf(TierTrait.BALANCE))) return;
        b.traitState().openWindow(now(b), m.windUp());
    }

    /** A move has run: the stances start, the Karma mark is laid, the pull and the echo are scheduled, the Balance is weighed. */
    public static void afterMove(TierBoss b, TierMove m, LivingEntity target) {
        if (!active(b) || !(b.level() instanceof ServerLevel sl)) return;
        TierState st = b.traitState();
        long now = sl.getGameTime();
        for (TierTraitSpec s : b.row().traits()) {
            if (!s.trait().needsMove() || !m.id().equals(s.moveId())) continue;
            switch (s.trait()) {
                case MIRROR -> {
                    st.startMirror(now);
                    Tells.ring(sl, b.position().add(0, 0.1, 0), 1.8, b.styleAccent(), 1.2f, 16);
                }
                case UNTOUCHABLE -> {
                    st.startGuard(now);
                    Tells.ring(sl, b.position().add(0, 0.1, 0), 1.8, b.styleAccent(), 1.2f, 16);
                }
                case KARMA -> {
                    if (target != null && target.isAlive() && b.isFoe(target)) {
                        st.markKarma(target.getUUID(), now);
                        notice(target, "Karma marks you: " + b.bossName() + " hits harder for 15 s.");
                        Tells.burst(sl, target.position().add(0, 1.0, 0), b.styleColor(), 8);
                    }
                }
                case PULL -> pull(b, sl, target);
                case ECHO -> echo(b, m, target);
                case BALANCE -> judge(b, sl);
                default -> { }
            }
        }
    }

    private static void pull(TierBoss b, ServerLevel sl, LivingEntity target) {
        if (target == null || !target.isAlive() || !b.isFoe(target)) return;
        double speed = Math.min(0.9, TierTrait.PULL_STRENGTH * scale());
        Tells.line(sl, b.getEyePosition(), target.getBoundingBox().getCenter(), b.styleAccent());
        for (int i = 0; i < TierTrait.PULL_IMPULSES; i++) {
            b.later(i * TierTrait.PULL_GAP_TICKS, () -> {
                if (!target.isAlive() || target.level() != b.level() || target.distanceToSqr(b) > 48.0 * 48.0) return;
                Vec3 d = target.position().subtract(b.position());
                if (d.x * d.x + d.z * d.z < 2.0 * 2.0) return;                          // already at his feet: never dragged through him
                EarlyMoves.pull(target, b.position(), speed);
            });
        }
    }

    /** The echo: the same move, once more, {@link TierTrait#ECHO_DELAY_TICKS} later, telegraphed again and with no bonus. It calls the move's body directly, so it cannot echo itself. */
    private static void echo(TierBoss b, TierMove m, LivingEntity target) {
        if (target == null) return;
        b.later(Math.max(1, TierTrait.ECHO_DELAY_TICKS - 20), () -> {
            if (b.fightActive() && target.isAlive()) TierMoves.tellSafe(m, b, target);
        });
        b.later(TierTrait.ECHO_DELAY_TICKS, () -> {
            if (b.fightActive() && target.isAlive()) TierMoves.runSafe(m, b, target);
        });
    }

    /** The Balance falls: whoever dealt most of the window (over half) is weakened, a lone fighter takes part of it back. */
    private static void judge(TierBoss b, ServerLevel sl) {
        TierState.Window w = b.traitState().closeWindow();
        if (w.empty() || w.top() == null) return;
        ServerPlayer top = sl.getServer().getPlayerList().getPlayer(w.top());
        if (top == null || top.level() != sl || top.distanceToSqr(b) > 64.0 * 64.0 || !top.isAlive()) return;
        int rgb = b.styleColor();
        if (w.share() >= TierTrait.BALANCE_TOP_SHARE) {
            EarlyMoves.weaken(top, TierTrait.BALANCE_WEAKEN_TICKS, 0);
            notice(top, "The Balance weighs your blows: weakened.");
            Tells.burst(sl, top.position().add(0, 1.0, 0), rgb, 8);
        }
        if (w.fighters() == 1) {
            double cap = b.getAttributeValue(Attributes.ATTACK_DAMAGE) * TierTrait.BALANCE_BACK_CAP_ATTACKS;
            float back = (float) Math.min(w.total() * TierTrait.BALANCE_BACK * scale(), cap);
            if (back >= 1f && b.plainStrike(top, back, true)) notice(top, "The Balance returns what you dealt.");
        }
    }
}
