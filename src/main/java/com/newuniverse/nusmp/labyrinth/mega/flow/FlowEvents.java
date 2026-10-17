package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.budget.BossBudget;
import com.newuniverse.nusmp.boss.budget.BossBudgetConfig;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.UUID;

/**
 * Why: this is where the Rukh flow touches combat. A hit that a player lands inside a place with a flow, within the cast window of a cast that matches that
 * flow, is multiplied; a hit from a cast that opposes it is left alone (its price was already taken when the cast was made, see {@link FlowCasts}) or, if the
 * owner wants, cancelled. The hook is {@code LivingIncomingDamageEvent}, the first of NeoForge 1.21's three damage events: its amount is the raw hit before
 * any armour or resistance of the target, so "the target's own reductions still apply after the boost" is true with no extra code, and the old
 * {@code LivingHurtEvent} no longer exists. It runs at HIGH priority so that the mod's many mitigation handlers (shields, wards, caps) see the boosted number and
 * can answer it. Three limits keep the boost honest. (1) A boss with a damage-per-second budget ({@code BudgetedBoss}) has already drawn the ORIGINAL hit from
 * it, because its own {@code hurt} runs before this event; so only the EXTRA is drawn from what is left of the budget now, and a boost can never push a boss
 * past its cap (a boss without a budget, such as the Floor Warden family, takes the full multiplier). (2) A boosted hit on a player is held under the share of
 * their health that the balance law lets one hit take, because the law's own cap ran before this event. (3) Who counts: the attacker is the root cause of
 * the damage (the owner of an arrow or of a summon), the target is not the attacker, an ally or a creature the attacker owns, and the attacker is inside the
 * Mega-Dungeon at a place with a flow. {@link #adjust} is the whole decision with the polarity passed in, so the self-tests drive it without a generated
 * sector; {@link #onIncoming} only gathers the facts. Server thread; nothing here throws out of an event handler.
 */
public final class FlowEvents {
    private FlowEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;
    /** Ticks between two boost effects (and two notices) for one player, so a zone that hits ten times a second does not send ten bursts. */
    private static final int EFFECT_GAP = 4, NOTICE_GAP = 100;

    /** Why: the verdict on one hit in one piece: what happened, the damage to use, and whether the hit is cancelled. */
    public record Result(FlowRules.Outcome outcome, float amount, boolean cancel) {}

    // ---------------------------------------------------------------- where the flow is
    /**
     * The block an entity stands in, read from its coordinates. Used instead of {@code blockPosition()} because NeoForge's {@code FakePlayer}, the stand-in
     * the self-tests use for a person, may override {@code blockPosition()} and {@code position()}; the coordinates are always the real ones.
     */
    static BlockPos posOf(Entity e) { return BlockPos.containing(e.getX(), e.getY(), e.getZ()); }

    /** The exact point an entity stands at, read from its coordinates (see {@link #posOf}). */
    static Vec3 pointOf(Entity e) { return new Vec3(e.getX(), e.getY(), e.getZ()); }

    /** The flow of a place: the flow of the road whose sector holds the position, NEUTRAL outside every sector (Floor 0, the gaps, other dimensions). */
    public static Alignment flowAt(Level level, BlockPos pos) { return flowOf(MegaDungeon.polarityAt(level, pos)); }

    /** The flow of a road under the current config (a null road, Floor 0 or outside the dungeon, has none). */
    public static Alignment flowOf(Polarity road) {
        if (road == null) return Alignment.NEUTRAL;                                          // the common case of a hit outside every sector: no config text is parsed
        return FlowRules.flowOf(road, road == Polarity.HOLY ? FlowConfig.holyFlow() : Alignment.NEUTRAL, road == Polarity.ABYSSAL ? FlowConfig.abyssalFlow() : Alignment.NEUTRAL);
    }

    // ---------------------------------------------------------------- the event
    public static void onIncoming(LivingIncomingDamageEvent e) {
        try {
            Entity cause = e.getSource().getEntity();
            if (cause == null) return;                                                       // fall, fire, lava: nobody's hit
            LivingEntity target = e.getEntity();
            if (!(target.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;
            if (!FlowConfig.get(FlowConfig.ENABLED)) return;
            ServerPlayer attacker = playerOf(cause, level);
            if (attacker == null) return;
            float amount = e.getAmount();
            Result r = adjust(attacker, target, amount, MegaDungeon.polarityAt(level, posOf(attacker)));
            if (r.cancel()) e.setCanceled(true);
            else if (r.amount() != amount) e.setAmount(r.amount());
        } catch (RuntimeException ex) {
            warn(ex);
        }
    }

    /** The player a hit is credited to: the attacker itself, or the owner of the summon that struck; null for anything else. */
    static ServerPlayer playerOf(Entity cause, ServerLevel level) {
        if (cause instanceof ServerPlayer p) return p;
        if (cause instanceof OwnableEntity o) {
            UUID owner = o.getOwnerUUID();
            if (owner != null && level.getPlayerByUUID(owner) instanceof ServerPlayer sp) return sp;
        }
        return null;
    }

    /**
     * The decision for one hit of {@code attacker} on {@code target} worth {@code amount}, in a place on the road {@code road} (null = no road): the damage to
     * use and whether to cancel. Everything the flow needs is read here (the config, the tracker's last cast, the boss budget, the balance law's player cap);
     * the arithmetic is in {@link FlowRules}. Public so the in-game self-test can drive it with a made-up road.
     */
    public static Result adjust(ServerPlayer attacker, LivingEntity target, float amount, Polarity road) {
        Result unchanged = new Result(FlowRules.Outcome.NONE, amount, false);
        Alignment flow = flowOf(road);
        if (flow == Alignment.NEUTRAL || target == attacker || target.isAlliedTo(attacker) || ownedBy(target, attacker)) return unchanged;
        ServerLevel level = attacker.serverLevel();
        long now = level.getGameTime();
        Alignment recent = FlowTracker.shared().recentCast(attacker.getUUID(), now, FlowConfig.get(FlowConfig.CAST_WINDOW_TICKS));
        FlowRules.Decision d = FlowRules.decide(flow, recent, amount, FlowConfig.get(FlowConfig.HOLY_MULTIPLIER), FlowConfig.get(FlowConfig.REJECT_CANCELS_DAMAGE));
        if (d.outcome() != FlowRules.Outcome.BOOST) return new Result(d.outcome(), d.amount(), d.cancel());
        float boosted = d.amount();
        if (target instanceof BudgetedBoss boss) boosted = chargeBossBudget(boss, attacker, amount, boosted);
        if (target instanceof Player) boosted = FlowRules.capBoost(amount, boosted, BalanceLaw.cap(target, boosted));
        if (boosted > amount) celebrate(level, attacker, target, flow, now, boosted / amount);
        return new Result(FlowRules.Outcome.BOOST, boosted, false);
    }

    private static boolean ownedBy(LivingEntity target, ServerPlayer attacker) {
        return target instanceof OwnableEntity o && attacker.getUUID().equals(o.getOwnerUUID());
    }

    /** The extra damage a boosted hit on a budgeted boss may add: what its per-second budget still has, unless the budget is off or the attacker is exempt. */
    private static float chargeBossBudget(BudgetedBoss boss, ServerPlayer attacker, float original, float boosted) {
        if (!BossBudgetConfig.enabled() || (attacker.isCreative() && BossBudgetConfig.ignoreCreative())) return boosted;
        BossBudget budget = boss.dpsBudget();
        if (budget == null) return boosted;
        return FlowRules.chargeExtra(original, boosted, extra -> budget.raw().allow((float) extra));
    }

    /** The Rukh answer a boosted hit: a few Rukh of the flow's colour on the target (at most one burst per few ticks per player) and, now and then, a line on the action bar. */
    private static void celebrate(ServerLevel level, ServerPlayer attacker, LivingEntity target, Alignment flow, long now, float factor) {
        if (FlowTracker.shared().effectDue(attacker.getUUID(), now, EFFECT_GAP)) RukhFx.burst(level, target, Rejection.rukh(flow), 4);
        if (FlowTracker.shared().noticeDue(attacker.getUUID(), now, NOTICE_GAP)) {
            String word = flow == Alignment.DARK ? "Dark" : "Light";
            attacker.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.mfw.boost." + flow.id(),
                    "The " + word + " Rukh answer your spell: x%s damage.", String.format(Locale.ROOT, "%.2f", factor).replaceAll("\\.?0+$", "")), true);
        }
    }

    // ---------------------------------------------------------------- housekeeping
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        try { FlowTracker.shared().forget(e.getEntity().getUUID()); } catch (RuntimeException ex) { warn(ex); }
    }

    public static void onStopping(ServerStoppingEvent e) { FlowTracker.shared().clear(); }

    /** Logs the first failure of a flow handler and then stays quiet: a handler must never throw out of an event, and one bad hit must not flood the log. */
    static void warn(RuntimeException ex) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] Rukh flow handler failed (further failures are not logged)", ex);
    }
}
