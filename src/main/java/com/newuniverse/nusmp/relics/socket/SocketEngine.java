package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.djinn.Bonds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server half of the sockets, registered on the game bus with {@link #register}:
 * <ul>
 *   <li>the sneak-use flow: relic in the main hand, sneak and use. A Spirit or Devil Core in the off hand is socketed; an Empty Element Core
 *       ({@code tensura:element_core_empty}) in the off hand draws the last Core out again; otherwise the use fires a bound socket's ability
 *       ({@link SocketRules#castSlot});</li>
 *   <li>the passive gifts ({@link Boon}) once a second per player, offset from the relic engine's own pass, for the relics that work for the player now
 *       (equipped, attuned where needed): mob effects, lent Tensura skills (a 60-tick lease, like the engine's), attribute modifiers, resistances and life steal;</li>
 *   <li>nothing for spectators; nothing for creative players either (they take no damage, so the gifts would only be noise).</li>
 * </ul>
 * A socket whose kind conflicts with the player's own bond ({@link SocketRules#usable}) is silent until the conflict is gone. A relic-bound Spirit or
 * Devil is never counted by {@code Bonds}: it is the relic's, not the player's.
 */
public final class SocketEngine {
    private SocketEngine() {}

    private static final String MOD_NS = "nusmp";
    private static final int SKILL_LEASE = 60, PASS_PHASE = 5;
    private static final String SPENT = "nusmp_socket_ls", SPENT_T = "nusmp_socket_ls_t";

    private record Want(Holder<Attribute> attr, double amount, AttributeModifier.Operation op) {}

    private static final Map<UUID, Map<ResourceLocation, Holder<Attribute>>> APPLIED = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Holder<Attribute>>> ATTRS = new ConcurrentHashMap<>();
    private static final Map<String, ResourceKey<DamageType>> DAMAGE_KEYS = new ConcurrentHashMap<>();
    private static volatile Item emptyCore;

    public static void register(IEventBus bus) {
        bus.addListener(SocketEngine::onRightClick);
        bus.addListener(SocketEngine::onPlayerTick);
        bus.addListener(SocketEngine::onIncomingDamage);
        bus.addListener(SocketEngine::onDamaged);
        bus.addListener(SocketEngine::onLogout);
        bus.addListener(SocketEngine::onRespawn);
        bus.addListener(SocketEngine::onServerStopping);
    }

    // ---------------------------------------------------------------- sneak-use
    private static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getHand() != InteractionHand.MAIN_HAND || !p.isShiftKeyDown() || p.isSpectator()) return;
        ItemStack main = e.getItemStack();
        RelicDef def = Relics.defOf(main);
        if (def == null) return;
        ItemStack off = p.getOffhandItem();
        SocketDef core = SocketItems.defOf(off);
        if (core != null) {
            if (SocketService.socket(p, main, core, false) == SocketRules.Outcome.OK && !p.isCreative()) off.shrink(1);
            consume(e);
            return;
        }
        SocketData cur = SocketComponents.get(main);
        if (isEmptyCore(off) && !cur.isEmpty()) {
            ItemStack back = SocketService.unsocket(p, main, cur.size() - 1);
            if (!back.isEmpty()) {
                SocketService.give(p, back);
                if (!p.isCreative()) off.shrink(1);
            }
            consume(e);
            return;
        }
        int slot = SocketRules.castSlot(true, p.getXRot(), def.acts().size(), cur.size());
        if (slot >= 0 && slot < cur.size()) {
            cast(p, main, def, cur.entries().get(slot));
            consume(e);
        }
    }

    private static void consume(PlayerInteractEvent.RightClickItem e) {
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean isEmptyCore(ItemStack s) {
        if (s.isEmpty()) return false;
        Item it = emptyCore;
        if (it == null) {
            it = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("tensura", "element_core_empty")).orElse(null);
            emptyCore = it;
        }
        return it != null && s.is(it);
    }

    // ---------------------------------------------------------------- casting
    /** Fires the ability of one bound socket of a relic the player holds or wears. True if it ran. */
    public static boolean cast(ServerPlayer p, ItemStack relic, RelicDef def, SocketData.Entry entry) {
        SocketDef d = SocketCatalog.byId(entry.id());
        Act act = SocketActs.of(entry.id());
        if (d == null || act == null) return false;
        if (!SocketRules.usable(d.kind(), Bonds.mask(p), Bonds.multiBond(p))) {
            p.displayClientMessage(Component.translatable("socket.nusmp.msg.silenced").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return RelicEngine.activate(p, relic, def, act, p.isShiftKeyDown(), 0);
    }

    /**
     * Fires the {@code n}-th (1 based) bound socket across the relics that work for the player now, in the order they are worn. The target of the
     * {@code /multiverse relic socket cast} command and of a worn-relic key hook. True if it ran.
     */
    public static boolean castWorn(ServerPlayer p, int n) {
        int i = 0;
        for (Relics.Worn w : Relics.loadoutFresh(p).active()) {
            for (SocketData.Entry e : SocketComponents.get(w.stack()).entries()) {
                if (++i == Math.max(1, n)) return cast(p, w.stack(), w.def(), e);
            }
        }
        p.displayClientMessage(Component.translatable("socket.nusmp.msg.nothing_bound").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    // ---------------------------------------------------------------- the passive pass
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        if ((p.tickCount + p.getId()) % 20 != PASS_PHASE) return;
        pass(p);
    }

    static void pass(ServerPlayer p) {
        Map<ResourceLocation, Want> want = new LinkedHashMap<>();
        if (!p.isSpectator() && !p.isCreative()) {
            int mask = -1;
            boolean multi = false;
            for (Relics.Worn w : Relics.loadout(p).active()) {
                if (!SocketComponents.hasAny(w.stack())) continue;
                boolean offOnly = w.def().slot().isHand() && w.stack() == p.getOffhandItem() && w.stack() != p.getMainHandItem();
                for (SocketData.Entry en : SocketComponents.get(w.stack()).entries()) {
                    SocketDef d = SocketCatalog.byId(en.id());
                    if (d == null) continue;
                    if (mask < 0) {
                        mask = Bonds.mask(p);
                        multi = Bonds.multiBond(p);
                    }
                    if (!SocketRules.usable(d.kind(), mask, multi)) continue;
                    int i = 0;
                    for (Boon b : d.boons()) {
                        switch (b.type()) {
                            case EFFECT -> ambient(p, b);
                            case SKILL -> Fx.lendSkill(p, b.key(), SKILL_LEASE);
                            case ATTR -> {
                                Holder<Attribute> h = offOnly ? null : attribute(b.key());
                                if (h != null)
                                    want.put(ResourceLocation.fromNamespaceAndPath(MOD_NS, "socket/" + d.kind().prefix() + "_" + d.key() + "/" + i), new Want(h, b.amount(), operation(b.amp())));
                            }
                            default -> { }
                        }
                        i++;
                    }
                }
            }
        }
        apply(p, want);
    }

    private static AttributeModifier.Operation operation(int ordinal) {
        AttributeModifier.Operation[] all = AttributeModifier.Operation.values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }

    private static void ambient(ServerPlayer p, Boon b) {
        ResourceLocation rl = ResourceLocation.tryParse(b.key());
        if (rl == null) return;
        Optional<Holder.Reference<MobEffect>> h = BuiltInRegistries.MOB_EFFECT.getHolder(rl);
        if (h.isEmpty()) return;
        int ticks = b.key().endsWith("night_vision") ? 260 : 80;                      // night vision flickers under ten seconds
        p.addEffect(new MobEffectInstance(h.get(), ticks, b.amp(), true, false));
    }

    private static Holder<Attribute> attribute(String id) {
        return ATTRS.computeIfAbsent(id, k -> {
            ResourceLocation rl = ResourceLocation.tryParse(k);
            return rl == null ? Optional.<Holder<Attribute>>empty() : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
        }).orElse(null);
    }

    private static void apply(ServerPlayer p, Map<ResourceLocation, Want> want) {
        Map<ResourceLocation, Holder<Attribute>> old = want.isEmpty() ? APPLIED.get(p.getUUID()) : APPLIED.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        if (old == null) return;
        Iterator<Map.Entry<ResourceLocation, Holder<Attribute>>> it = old.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, Holder<Attribute>> e = it.next();
            if (want.containsKey(e.getKey())) continue;
            AttributeInstance inst = p.getAttribute(e.getValue());
            if (inst != null) inst.removeModifier(e.getKey());
            it.remove();
        }
        for (Map.Entry<ResourceLocation, Want> e : want.entrySet()) {
            Want w = e.getValue();
            AttributeInstance inst = p.getAttribute(w.attr());
            if (inst == null) continue;
            AttributeModifier cur = inst.getModifier(e.getKey());
            if (cur == null || cur.amount() != w.amount() || cur.operation() != w.op()) {
                if (cur != null) inst.removeModifier(e.getKey());
                inst.addTransientModifier(new AttributeModifier(e.getKey(), w.amount(), w.op()));
            }
            old.put(e.getKey(), w.attr());
        }
        if (old.isEmpty()) APPLIED.remove(p.getUUID());
    }

    // ---------------------------------------------------------------- resistances and life steal
    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) return;
        double keep = 1.0;
        String seen0 = null, seen1 = null, seen2 = null, seen3 = null;                 // a socket id counts once however many relics carry it
        for (Relics.Worn w : Relics.loadout(p).active()) {
            if (!SocketComponents.hasAny(w.stack())) continue;
            for (SocketData.Entry en : SocketComponents.get(w.stack()).entries()) {
                String id = en.id();
                if (id.equals(seen0) || id.equals(seen1) || id.equals(seen2) || id.equals(seen3)) continue;
                if (seen0 == null) seen0 = id; else if (seen1 == null) seen1 = id; else if (seen2 == null) seen2 = id; else seen3 = id;
                SocketDef d = SocketCatalog.byId(id);
                if (d == null) continue;
                for (Boon b : d.boons()) if (b.type() == Boon.Type.RESIST && matches(e.getSource(), b.key())) keep *= 1.0 - b.amount();
            }
        }
        if (keep < 1.0) e.setAmount((float) (e.getAmount() * Math.max(0.1, keep)));
    }

    private static boolean matches(DamageSource src, String id) {
        ResourceKey<DamageType> key = DAMAGE_KEYS.computeIfAbsent(id, k -> {
            ResourceLocation rl = ResourceLocation.tryParse(k);
            return ResourceKey.create(Registries.DAMAGE_TYPE, rl == null ? ResourceLocation.withDefaultNamespace("none") : rl);
        });
        return src.is(key);
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() == p || p.isCreative() || p.isSpectator() || !p.isAlive()) return;
        float dealt = e.getNewDamage();
        if (dealt <= 0f) return;
        double fraction = 0.0;
        for (Relics.Worn w : Relics.loadout(p).active()) {
            if (!SocketComponents.hasAny(w.stack())) continue;
            for (SocketData.Entry en : SocketComponents.get(w.stack()).entries()) {
                SocketDef d = SocketCatalog.byId(en.id());
                if (d == null) continue;
                for (Boon b : d.boons()) if (b.type() == Boon.Type.LIFESTEAL) fraction = Math.max(fraction, b.amount());
            }
        }
        if (fraction <= 0.0) return;
        long now = p.level().getGameTime();
        var data = p.getPersistentData();
        float spent = now - data.getLong(SPENT_T) >= 20 ? 0f : data.getFloat(SPENT);
        float give = Math.min(SocketRules.lifesteal(dealt, fraction), SocketRules.lifestealLeft(p.getMaxHealth(), spent));
        if (give <= 0f) return;
        if (now - data.getLong(SPENT_T) >= 20) data.putLong(SPENT_T, now);
        data.putFloat(SPENT, spent + give);
        p.heal(give);
    }

    // ---------------------------------------------------------------- housekeeping
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { APPLIED.remove(e.getEntity().getUUID()); }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { APPLIED.remove(e.getEntity().getUUID()); }

    private static void onServerStopping(ServerStoppingEvent e) {
        APPLIED.clear();
        emptyCore = null;
    }

    /** The sockets a relic stack has, with their defs, for tooltips and the commands (unknown ids are skipped). */
    public static List<SocketDef> boundDefs(ItemStack stack) {
        List<SocketDef> out = new ArrayList<>(2);
        for (SocketData.Entry e : SocketComponents.get(stack).entries()) {
            SocketDef d = SocketCatalog.byId(e.id());
            if (d != null) out.add(d);
        }
        return out;
    }
}
