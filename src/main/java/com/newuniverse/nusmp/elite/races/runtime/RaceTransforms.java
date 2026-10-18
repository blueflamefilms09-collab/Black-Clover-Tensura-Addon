package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.net.RaceNet;
import com.newuniverse.nusmp.elite.races.race.EliteRace;
import com.newuniverse.nusmp.elite.races.state.Coexistence;
import com.newuniverse.nusmp.elite.races.state.EntryCondition;
import com.newuniverse.nusmp.elite.races.state.EntryCondition.Reason;
import com.newuniverse.nusmp.elite.races.state.StateModifier;
import com.newuniverse.nusmp.elite.races.state.TransformMachine;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import com.newuniverse.nusmp.elite.races.state.Zenkai;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the server side of the race transformations, the one place that turns the pure {@link TransformMachine} into things that happen to a player. It answers a request (the
 * hotkey, a command), runs the machine every few ticks (expiry, the dawn, the ki upkeep, the automatic triggers), and applies the current form as TRANSIENT attribute modifiers
 * (never the base value; removed and re-added whenever the form, the Zenkai stacks or the Kaioken overlap change, and re-checked once every five seconds), with the aura and the
 * Rukh (RaceAura) and a message on the action bar. The machine is the authority: this class never decides a rule, it asks {@code check} and obeys.
 *
 * <p>Tick cost: a player who is not an elite race costs one race lookup every 20 ticks; a transformed one is stepped every 5 ticks.
 */
public final class RaceTransforms {
    private RaceTransforms() {}

    public enum Mode { AUTO, CYCLE, OFF, STATE }

    /** The answer to a request: whether it worked, what to tell the player, and the form they are in now. */
    public record Outcome(boolean ok, Component message, TransformState state) {}

    private static final Map<UUID, String> APPLIED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TICK = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------- who is an elite race
    /** The player's elite race, or null (a Tensura race, no race, or any error reading it). */
    public static EliteRace raceOf(LivingEntity e) {
        try {
            var opt = RaceAPI.getRaceFrom(e).getRace();
            if (opt.isPresent() && opt.get().getRace() instanceof EliteRace r) return r;
        } catch (RuntimeException | LinkageError ignored) {
            // no race storage on this entity: not an elite race
        }
        return null;
    }

    private static Outcome fail(Component c) { return new Outcome(false, c, null); }

    private static Outcome no(Reason r) { return fail(RaceMessages.why(r)); }

    // ---------------------------------------------------------------- requests
    /** A request from the hotkey (force false) or an administrator (force true, which skips the entry conditions but not the race's own list of forms). */
    public static Outcome request(ServerPlayer p, Mode mode, TransformState wanted, boolean force) {
        if (!EliteRacesConfig.transformations()) return no(Reason.DISABLED);
        EliteRace race = raceOf(p);
        RaceTemplate t = race == null ? null : race.template();
        if (t == null) return fail(RaceMessages.msg("norace", "You are not one of the elite races (Saiyan or Primordial)."));
        Map<TransformState, TransformSpec> specs = RaceSpecs.effective(t);
        TransformMachine m = RaceLedger.machine(p);
        PlayerTransformCtx ctx = new PlayerTransformCtx(p, t.tier());
        TransformState target = null;
        switch (mode) {
            case OFF -> {
                return m.active() ? leave(p, t, m, specs) : fail(RaceMessages.msg("notactive", "You are not transformed."));
            }
            case AUTO -> {
                if (m.active()) return leave(p, t, m, specs);
                target = m.best(specs, ctx);
                if (target == null) {
                    TransformMachine.Result r = m.check(specs.get(TransformState.BOOSTED), ctx);
                    return no(r.reason() == null ? Reason.NOT_AVAILABLE : r.reason());
                }
            }
            case CYCLE -> {
                target = m.cycleTarget(specs, ctx);
                if (target == null) return m.active() ? leave(p, t, m, specs) : fail(RaceMessages.msg("noform", "No form is open to you right now."));
            }
            case STATE -> {
                if (wanted == null || wanted == TransformState.BASE) return m.active() ? leave(p, t, m, specs) : fail(RaceMessages.msg("notactive", "You are not transformed."));
                target = wanted;
            }
        }
        TransformSpec spec = specs.get(target);
        if (spec == null) return no(Reason.NOT_AVAILABLE);
        if (force) return enter(p, t, m, specs, spec, null, false);
        TransformMachine.Result r = m.check(spec, ctx);
        if (!r.ok()) return no(r.reason());
        return enter(p, t, m, specs, spec, r.entry(), false);
    }

    private static Outcome enter(ServerPlayer p, RaceTemplate t, TransformMachine m, Map<TransformState, TransformSpec> specs, TransformSpec spec, EntryCondition entry, boolean auto) {
        TransformSpec leaving = m.active() ? specs.get(m.state()) : null;
        long now = p.serverLevel().getGameTime();
        if (leaving != null) RaceAura.leave(p, leaving);
        m.forceEnter(spec, leaving, now);
        if (entry != null && entry.consume() && !entry.item().isEmpty()) PlayerRaceCtx.take(p, entry.item(), Math.max(1, entry.itemCount()));
        LAST_TICK.put(p.getUUID(), now);
        RaceLedger.saveMachine(p);
        sync(p, t, m, specs, true);
        RaceAura.enter(p, spec);
        p.serverLevel().playSound(null, p.blockPosition(), spec.state() == TransformState.GREAT_APE ? SoundEvents.ENDER_DRAGON_GROWL : SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.8f, spec.state() == TransformState.GREAT_APE ? 0.8f : 1.4f);
        Component msg = RaceMessages.msg(auto ? "awaken" : "enter", auto ? "%s rises from within you!" : "You enter the %s.", RaceMessages.stateName(spec.state()));
        announce(p, msg);
        pushState(p, m, specs);
        return new Outcome(true, msg, spec.state());
    }

    private static Outcome leave(ServerPlayer p, RaceTemplate t, TransformMachine m, Map<TransformState, TransformSpec> specs) {
        TransformState was = m.state();
        TransformSpec spec = specs.get(was);
        m.exit(spec, p.serverLevel().getGameTime());
        LAST_TICK.remove(p.getUUID());
        RaceLedger.saveMachine(p);
        sync(p, t, m, specs, true);
        RaceAura.leave(p, spec);
        Component msg = RaceMessages.msg("leave", "You drop out of the %s.", RaceMessages.stateName(was));
        announce(p, msg);
        pushState(p, m, specs);
        return new Outcome(true, msg, TransformState.BASE);
    }

    private static void announce(ServerPlayer p, Component c) {
        if (EliteRacesConfig.announce()) p.displayClientMessage(c, true);
    }

    private static void pushState(ServerPlayer p, TransformMachine m, Map<TransformState, TransformSpec> specs) {
        TransformSpec spec = specs.get(m.state());
        long left = m.remaining(p.serverLevel().getGameTime());
        RaceNet.sendState(p, m.state().id, m.endsAt() == Long.MAX_VALUE ? 0 : (int) Math.min(Integer.MAX_VALUE, left), spec == null ? 0xFFFFFFFF : spec.color());
    }

    // ---------------------------------------------------------------- the tick
    /** Called for every server player every tick by {@link RaceRuntime}; does its work every 5 or 20 ticks. */
    public static void tick(ServerPlayer p) {
        TransformMachine m = RaceLedger.machine(p);
        boolean hurt = p.getHealth() < p.getMaxHealth() * 0.6f;
        int period = m.active() || hurt ? 5 : 20;
        if ((p.tickCount + p.getId()) % period != 0) return;
        EliteRace race = EliteRacesConfig.enabled() ? raceOf(p) : null;
        RaceTemplate t = race == null ? null : race.template();
        long now = p.serverLevel().getGameTime();
        if (t == null || !EliteRacesConfig.transformations()) {
            if (m.active()) {
                m.exit(null, now);
                LAST_TICK.remove(p.getUUID());
                RaceLedger.saveMachine(p);
                RaceNet.sendState(p, TransformState.BASE.id, 0, 0xFFFFFFFF);
            }
            if (APPLIED.remove(p.getUUID()) != null) clear(p);
            return;
        }
        Map<TransformState, TransformSpec> specs = RaceSpecs.effective(t);
        if (!t.id().equals(RaceLedger.template(p))) {   // a new race: the old form, and its modifiers, are gone
            RaceLedger.setTemplate(p, t.id());
            if (m.active()) {
                m.exit(null, now);
                LAST_TICK.remove(p.getUUID());
                RaceLedger.saveMachine(p);
            }
            APPLIED.remove(p.getUUID());
            pushState(p, m, specs);
        }
        PlayerTransformCtx ctx = new PlayerTransformCtx(p, t.tier());
        TransformState was = m.state();
        TransformSpec wasSpec = specs.get(was);
        TransformMachine.Event ev = m.tick(specs, ctx);
        if (ev != TransformMachine.Event.NONE) {
            LAST_TICK.remove(p.getUUID());
            RaceLedger.saveMachine(p);
            RaceAura.leave(p, wasSpec);
            announce(p, RaceMessages.end(ev, was));
            sync(p, t, m, specs, true);
            pushState(p, m, specs);
        } else if (m.active()) {
            TransformSpec spec = specs.get(m.state());
            Long last = LAST_TICK.put(p.getUUID(), now);
            double seconds = last == null ? 0.0 : Math.max(0, Math.min(now - last, 40L)) / 20.0;
            if (spec != null) {
                RaceEnergy.drainAura(p, spec.auraDrainPerSecond() * seconds);
                if (p.tickCount % 20 < period) RaceAura.pulse(p, spec);
            }
            if (p.tickCount % 40 < period) pushState(p, m, specs);
        } else if (EliteRacesConfig.autoTriggers()) {
            TransformState auto = m.autoTrigger(specs, ctx);
            if (auto != null) {
                TransformSpec spec = specs.get(auto);
                TransformMachine.Result r = m.check(spec, ctx, true);
                if (r.ok()) enter(p, t, m, specs, spec, r.entry(), true);
            }
        }
        sync(p, t, m, specs, false);
    }

    // ---------------------------------------------------------------- modifiers
    private static ResourceLocation id(String prefix, String attribute) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "elite_" + prefix + "_" + attribute.replace(':', '_'));
    }

    private static boolean zenkaiOn(RaceTemplate t) { return t.hasTrait("zenkai") && EliteRacesConfig.zenkai(); }

    /** Makes the player's transient modifiers match their form, the Zenkai stacks and the Kaioken overlap. Cheap when nothing changed. */
    static void sync(ServerPlayer p, RaceTemplate t, TransformMachine m, Map<TransformState, TransformSpec> specs, boolean force) {
        TransformSpec spec = m.active() ? specs.get(m.state()) : null;
        boolean kaioken = spec != null && spec.kiScaled() && RaceSkills.kaiokenActive(p);
        Zenkai.State z = zenkaiOn(t) ? Zenkai.clampTo(RaceLedger.zenkai(p), t.tier()) : Zenkai.State.NONE;
        String key = t.id() + "|" + m.state().id + "|" + kaioken + "|" + z.stacks() + "|" + EliteRacesConfig.kaiokenOverlap();
        boolean same = key.equals(APPLIED.get(p.getUUID()));
        if (!force && same && (p.tickCount % 100 != 0 || stillThere(p, spec, z))) return;
        clear(p);
        if (spec != null) {
            for (StateModifier mod : Coexistence.effective(spec, kaioken, EliteRacesConfig.kaiokenOverlap())) apply(p, "state", mod);
        }
        if (z.stacks() > 0) {
            apply(p, "zenkai", new StateModifier("attack_damage", Zenkai.attackBonus(z.stacks()), StateModifier.Op.MULT_BASE));
            apply(p, "zenkai", new StateModifier("max_health", Zenkai.healthBonus(z.stacks()), StateModifier.Op.ADD));
        }
        APPLIED.put(p.getUUID(), key);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static boolean stillThere(ServerPlayer p, TransformSpec spec, Zenkai.State z) {
        if (spec != null && !spec.modifiers().isEmpty()) {
            StateModifier first = spec.modifiers().get(0);
            Holder<Attribute> h = RaceAttributes.get(first.attribute());
            AttributeInstance inst = h == null ? null : p.getAttribute(h);
            if (inst != null && !inst.hasModifier(id("state", first.attribute()))) return false;
        }
        if (z.stacks() > 0) {
            AttributeInstance inst = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            if (inst != null && !inst.hasModifier(id("zenkai", "attack_damage"))) return false;
        }
        return true;
    }

    private static void apply(ServerPlayer p, String prefix, StateModifier mod) {
        Holder<Attribute> h = RaceAttributes.get(mod.attribute());
        AttributeInstance inst = h == null ? null : p.getAttribute(h);
        if (inst == null) return;
        ResourceLocation id = id(prefix, mod.attribute());
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, mod.amount(), RaceAttributes.operation(mod.op())));
    }

    /** Removes every modifier this class ever adds (state and Zenkai), for every attribute the table knows. */
    static void clear(ServerPlayer p) {
        List<String> keys = new ArrayList<>(RaceAttributes.keys());
        for (String k : keys) {
            Holder<Attribute> h = RaceAttributes.get(k);
            AttributeInstance inst = h == null ? null : p.getAttribute(h);
            if (inst == null) continue;
            inst.removeModifier(id("state", k));
            inst.removeModifier(id("zenkai", k));
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    // ---------------------------------------------------------------- lifecycle (called by RaceRuntime)
    /** A new body (respawn): the old form is over, with no rest, and every modifier will be rebuilt. */
    public static void onRespawn(ServerPlayer p) {
        TransformMachine m = RaceLedger.machine(p);
        m.reset();
        RaceLedger.saveMachine(p);
        APPLIED.remove(p.getUUID());
        LAST_TICK.remove(p.getUUID());
        RaceNet.sendState(p, TransformState.BASE.id, 0, 0xFFFFFFFF);
    }

    /** Login: the modifiers are gone with the old body, rebuild them and tell the client. */
    public static void onLogin(ServerPlayer p) {
        APPLIED.remove(p.getUUID());
        LAST_TICK.remove(p.getUUID());
        TransformMachine m = RaceLedger.machine(p);
        EliteRace race = raceOf(p);
        RaceTemplate t = race == null ? null : race.template();
        if (t != null) pushState(p, m, RaceSpecs.effective(t));
        else RaceNet.sendState(p, TransformState.BASE.id, 0, 0xFFFFFFFF);
    }

    public static void onLogout(ServerPlayer p) {
        RaceLedger.forget(p);
        RaceNet.forget(p.getUUID());
        APPLIED.remove(p.getUUID());
        LAST_TICK.remove(p.getUUID());
    }

    /** A Zenkai stack changed: rebuild the modifiers on the next tick. */
    public static void invalidate(ServerPlayer p) { APPLIED.remove(p.getUUID()); }
}
