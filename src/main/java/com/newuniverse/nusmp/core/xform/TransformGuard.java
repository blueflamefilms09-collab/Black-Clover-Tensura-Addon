package com.newuniverse.nusmp.core.xform;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.core.xform.TransformPolicy.Reason;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Why: every transformation of the mod kept its own life cycle, and a few of them never learned that a death is the end of a body (a Black Form kept in a skill's tag came back after respawn, a
 * form's flags in the player's persistent data could be copied into the new body, render layers outlived the dead player, a dead player could still press a form key). This is the ONE place
 * that reverts all of that, by the policy of {@link TransformPolicy}: on death (late, after every system's own handler), on the clone (the flags copied to the new body are stripped) and on
 * respawn (early, before any system rebuilds what should persist) it runs the registered {@link Hook}s, strips the registered persistent-data keys, removes the registered attribute modifiers BY ID,
 * and clears every render layer of the player with one small packet per running layer to the players who can see the body. Setting: {@code transformations.resetOnDeath}.
 *
 * <p>Cost: nothing runs per tick. A death, a clone and a respawn each do a few dozen map lookups (about 15 modifier ids, a few flags, none of them world work) and at most a handful of small
 * packets to the players tracking the body; no entity, no chunk, no allocation that grows. The guard holds no per-player map of its own (the state it clears lives in the player's persistent
 * data, attributes, skill tags and the aura table keyed by UUID), so there is nothing to clean at logout.
 */
public final class TransformGuard {
    private TransformGuard() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** A system that keeps form state outside persistent data and attributes (a skill's tag, a runtime record) and knows how to end it. Must be cheap, idempotent and never fail on a body with no state. */
    public interface Hook {
        String id();

        /** Ends this system's form state for the body; {@code why} is {@link Reason#DEATH} (the dying body) or {@link Reason#RESPAWN} (the new one). */
        void end(ServerPlayer p, Reason why);
    }

    /** One transient form modifier: the attribute it sits on and its fixed id. */
    public record Mod(Holder<Attribute> attribute, ResourceLocation id) {}

    private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();
    private static final Set<String> KEYS = ConcurrentHashMap.newKeySet();
    private static final List<Mod> MODIFIERS = new CopyOnWriteArrayList<>();
    private static boolean registered;

    // ------------------------------------------------------------------ registry
    /** Adds a hook once per id (a second one with the same id is ignored). */
    public static void hook(Hook h) {
        if (h == null || h.id() == null) return;
        for (Hook o : HOOKS) if (o.id().equals(h.id())) return;
        HOOKS.add(h);
    }

    /** A persistent-data flag that belongs to a form and must not outlive the body ({@code nusmp_} keys only). */
    public static void key(String k) { if (TransformPolicy.strippable(k)) KEYS.add(k); }

    /** A TRANSIENT form modifier (this mod's namespace only) removed when the forms end. Never register a permanent price or a racial bonus here. */
    public static void modifier(Holder<Attribute> attribute, ResourceLocation id) {
        if (attribute == null || !TransformPolicy.sweepable(id)) return;
        for (Mod m : MODIFIERS) if (m.id().equals(id) && m.attribute().equals(attribute)) return;
        MODIFIERS.add(new Mod(attribute, id));
    }

    public static List<String> hookIds() {
        List<String> out = new ArrayList<>();
        for (Hook h : HOOKS) out.add(h.id());
        return out;
    }

    public static Set<String> keys() { return Set.copyOf(KEYS); }

    public static List<Mod> modifiers() { return List.copyOf(MODIFIERS); }

    // ------------------------------------------------------------------ the events
    /** Registers the built-in registrations and the listeners once (called from NUSMP). */
    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        FormHooks.registerAll();
        bus.addListener(EventPriority.LOWEST, TransformGuard::onDeath);          // after every system's own death handler: a system that reads its state to end cleanly still can
        bus.addListener(EventPriority.HIGHEST, TransformGuard::onClone);
        bus.addListener(EventPriority.HIGHEST, TransformGuard::onRespawn);       // before any system rebuilds what should persist
    }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) end(p, Reason.DEATH, NUConfig.transformResetOnDeath());
    }

    static void onClone(PlayerEvent.Clone e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (TransformPolicy.stripsClone(e.isWasDeath(), NUConfig.transformResetOnDeath())) strip(p.getPersistentData());
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        end(p, e.isEndConquered() ? Reason.END_RETURN : Reason.RESPAWN, NUConfig.transformResetOnDeath());
    }

    // ------------------------------------------------------------------ the work
    /**
     * Applies the policy for one moment: when it says the forms end, runs the hooks, strips the flags, sweeps the modifiers and clears the render layers; otherwise does nothing. Public so a
     * self-test can drive the same entry the events call. Never throws.
     */
    public static void end(ServerPlayer p, Reason why, boolean resetOnDeath) {
        if (p == null || !TransformPolicy.endsForms(why, resetOnDeath)) return;
        for (Hook h : HOOKS) {
            try {
                h.end(p, why);
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("[anime_multiverse] transform guard: hook {} failed for {}", h.id(), why, ex);            // one broken system never stops the others
            }
        }
        strip(p.getPersistentData());
        sweepModifiers(p);
        try {
            PlayerAuras.clearAll(p);
        } catch (RuntimeException ex) {
            LOG.warn("[anime_multiverse] transform guard: could not clear the render layers", ex);
        }
    }

    /** Removes every registered flag from the tag; returns how many were there. */
    static int strip(CompoundTag tag) {
        int n = 0;
        for (String k : KEYS) {
            if (tag.contains(k)) {
                tag.remove(k);
                n++;
            }
        }
        return n;
    }

    /** Removes every registered modifier from the player; returns how many were on it. */
    static int sweepModifiers(ServerPlayer p) {
        int n = 0;
        for (Mod m : MODIFIERS) {
            AttributeInstance inst = p.getAttribute(m.attribute());
            if (inst != null && inst.hasModifier(m.id())) {
                inst.removeModifier(m.id());
                n++;
            }
        }
        return n;
    }
}
