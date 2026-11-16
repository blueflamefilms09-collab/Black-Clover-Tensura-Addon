package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: everything the slot does on its own (not on a key press) is event driven and bounded. Once a second per bearer (staggered by entity id): read the slot, apply or remove the passives, sample for the
 * rewind (Time only), pulse the Soulworld, a rare whisper. Per hit: Foresight, the Soul anchor and soul resistance on the bearer's incoming damage. Per effect: the Mind Stone's mental shield, the Time
 * Stone's immunity to slowing, the Reality Stone's one in five debuffs that do not happen. Per container close: a stone is not left in a chest. Per death, logout and dimension change: the bearer's
 * tables are emptied. Once a minute: the away rule. A player who wears no stone costs one cached Curios read a second (shared with the relic scan) and nothing else. Server only.
 */
public final class StoneEvents {
    private StoneEvents() {}

    private static final Set<String> MIND_SHIELD = Set.of("tensura:confusion", "tensura:fear", "minecraft:nausea", "tensura:insanity", "tensura:mind_control");
    private static final Set<String> TIME_SHIELD = Set.of("minecraft:slowness", "tensura:slowness");
    private static final long ANCHOR_COOLDOWN = 24000L, FORESIGHT_COOLDOWN = 1200L;

    /** Three whispers per stone (the lang fragment has the same lines; the code fallback is the English one). */
    static final String[][] WHISPERS = {
            {"Space bends towards the places you have not been.", "Every door is a door you may open.", "Distance is only a habit."},
            {"I hear them all. They hear nothing of me.", "A thought is a door left open.", "You need not take a will to know it."},
            {"There is more of me than you can hold.", "Strength asks nothing. It only waits.", "Spend me, and I will not be less."},
            {"It has already happened. Do it again.", "A moment is a place you can return to.", "Patience is the first power."},
            {"Say it, and it is so; unsay it, and it never was.", "Nothing is fixed that you do not fix.", "The world is a draft."},
            {"The ones you lost are here. They are quiet.", "Every soul has a weight. Mine is yours.", "What is given is never returned."}};

    public static void register(IEventBus bus) {
        bus.addListener(StoneEvents::onPlayerTick);
        bus.addListener(StoneEvents::onIncoming);
        bus.addListener(StoneEvents::onEffect);
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, StoneEvents::onDrops);        // after Curios has put the worn items into the drops
        bus.addListener(StoneEvents::onDeath);
        bus.addListener(StoneEvents::onRespawn);
        bus.addListener(StoneEvents::onLogout);
        bus.addListener(StoneEvents::onDimension);
        bus.addListener(StoneEvents::onContainerClose);
        bus.addListener(StoneEvents::onServerTick);
        bus.addListener(StoneEvents::onStopping);
    }

    // ================================================================ once a second per bearer
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator()) return;
        if ((p.tickCount + p.getId()) % 20 != 0) return;
        StoneSlot.State before = StoneSlot.refresh(p);
        StoneSlot.State now = StoneSlot.state(p);
        if (before.stone() != now.stone()) changed(p, before, now);
        if (now.working()) {
            if (StoneConfig.passives()) StonePassives.apply(p, now.stone(), StoneSlot.scale(p));
            else StonePassives.clear(p);
            if (now.stone() == InfinityStone.TIME) StoneState.sample(p);
            ambient(p, now.stone());
            if (StoneConfig.whispers() && p.getRandom().nextInt(900) == 0) whisper(p, now.stone());
        } else if (before.stone() != null) {
            StonePassives.clear(p);
        }
        zonePulse(p);
    }

    /** A stone appeared in or left the slot. A new one that the gate does not allow goes back to the pack (once the player has been in the world a few seconds, so a login is never an ejection). */
    private static void changed(ServerPlayer p, StoneSlot.State before, StoneSlot.State now) {
        if (now.stone() == null) {
            StonePassives.clear(p);
            StoneState.forgetMotion(p.getUUID());
            return;
        }
        if (!now.working()) {
            if (p.tickCount > 100 && StoneConfig.enabled() && !StoneSlot.gateOk(p)) eject(p);
            return;
        }
        ServerLevel l = p.serverLevel();
        Fx.ring(l, p.position(), 3.0, now.stone().rgb());
        Fx.burst(l, p.position(), now.stone().rgb());
        float recoil = StoneRules.recoil(p.getMaxHealth(), p.getHealth(), 0.03);
        if (recoil > 0f) p.setHealth(p.getHealth() - recoil);
        p.displayClientMessage(Component.translatableWithFallback("message.anime_multiverse.stone.surge", "The stone wakes in you."), true);
    }

    /** Moves the stone out of the slot into the player's pack with a word. Not destroyed. */
    private static void eject(ServerPlayer p) {
        IItemHandler h = CuriosLink.slotHandlers(p).get(StoneRules.SLOT);
        if (!(h instanceof IItemHandlerModifiable m)) return;
        for (int i = 0; i < m.getSlots(); i++) {
            ItemStack s = m.getStackInSlot(i);
            if (s.isEmpty() || StoneSlot.stoneOf(s) == null) continue;
            m.setStackInSlot(i, ItemStack.EMPTY);
            if (!p.getInventory().add(s)) p.drop(s, false);
            Relics.invalidate(p);
            CuriosLink.forget(p.getUUID());
            StoneSlot.forget(p.getUUID());
            p.displayClientMessage(Component.translatableWithFallback("message.anime_multiverse.stone.gate", "The stone will not wake: it needs %s base maximum EP.", StoneActs.short_(StoneConfig.epGate())), false);
            return;
        }
    }

    private static void ambient(ServerPlayer p, InfinityStone s) {
        long now = p.level().getGameTime();
        if (now % 60 != 0 || !PerfBudgets.effectPacket(now)) return;
        Fx.burst(p.serverLevel(), p.position().add(0, 0.6, 0), s.rgb());
    }

    private static void whisper(ServerPlayer p, InfinityStone s) {
        int n = p.getRandom().nextInt(3);
        p.displayClientMessage(Component.translatableWithFallback("message.anime_multiverse.stone.whisper." + s.id() + "." + (n + 1), WHISPERS[s.ordinal()][n]), true);
    }

    /** The open Soulworld of this bearer: heals the bearer and allied players inside it a few percent a second (at most 45% in all), closes at the end or when the bearer leaves the dimension. */
    private static void zonePulse(ServerPlayer p) {
        StoneState.Zone z = StoneState.zone(p.getUUID());
        if (z == null) return;
        ServerLevel l = p.serverLevel();
        if (l.getGameTime() > z.until() || !z.dim().equals(l.dimension().location().toString())) {
            StoneState.closeZone(p.getUUID());
            return;
        }
        float pulse = (float) StoneRules.soulworldPulse(StoneSlot.scale(p));
        Vec3 c = new Vec3(z.x(), z.y(), z.z());
        for (Player o : l.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(8.0),
                q -> q.isAlive() && !q.isSpectator() && (q == p || q.isAlliedTo(p)) && q.position().distanceToSqr(c) <= 64.0)) {
            o.heal(o.getMaxHealth() * pulse);
        }
        if (PerfBudgets.effectPacket(l.getGameTime())) Fx.ring(l, c, 8.0, InfinityStone.SOUL.rgb());
    }

    // ================================================================ per hit
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer v) || v.level().isClientSide()) return;
        InfinityStone s = StoneSlot.active(v);
        if (s == null) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        long now = v.level().getGameTime();
        UUID id = v.getUUID();
        if (s == InfinityStone.TIME && StoneRules.foresightDodges(e.getAmount(), v.getMaxHealth(), now, StoneState.foresightReady(id))) {
            StoneState.foresightUsed(id, now + FORESIGHT_COOLDOWN);
            e.setCanceled(true);
            v.displayClientMessage(Component.translatableWithFallback("message.anime_multiverse.stone.foresight", "You saw it coming."), true);
            Fx.burst(v.serverLevel(), v.position(), s.rgb());
            return;
        }
        if (s == InfinityStone.SOUL) {
            if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)) e.setAmount(e.getAmount() * 0.6f);
            if (StoneRules.soulAnchors(e.getAmount(), v.getHealth() + v.getAbsorptionAmount(), now, StoneState.anchorReady(id))) {
                StoneState.anchorUsed(id, now + ANCHOR_COOLDOWN);
                e.setCanceled(true);
                v.setHealth(StoneRules.anchorHealth(v.getMaxHealth()));
                v.displayClientMessage(Component.translatableWithFallback("message.anime_multiverse.stone.anchor", "A soul holds you here."), true);
                Fx.burst(v.serverLevel(), v.position(), s.rgb());
            }
        }
    }

    static void onEffect(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer v) || v.level().isClientSide()) return;
        InfinityStone s = StoneSlot.active(v);
        MobEffectInstance inst = e.getEffectInstance();
        if (s == null || inst == null) return;
        String id = inst.getEffect().unwrapKey().map(k -> k.location().toString()).orElse("");
        boolean no = switch (s) {
            case MIND -> MIND_SHIELD.contains(id);
            case TIME -> TIME_SHIELD.contains(id);
            case REALITY -> inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && v.getRandom().nextDouble() < 0.20;
            default -> false;
        };
        if (no) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    // ================================================================ death, respawn, logout, dimension
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !p.level().isClientSide()) StoneState.forgetMotion(p.getUUID());
    }

    /** "keep": the stone that was worn in the slot is taken out of the drops and waits for the respawn (or for a logout from the death screen, when it falls where the bearer died). */
    static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide() || !StoneConfig.enabled() || !StoneConfig.keepOnDeath()) return;
        InfinityStone worn = StoneSlot.state(p).stone();
        if (worn == null) return;
        ItemStack[] kept = {ItemStack.EMPTY};
        e.getDrops().removeIf(ie -> {
            ItemStack s = ie.getItem();
            if (kept[0].isEmpty() && StoneSlot.stoneOf(s) == worn) {
                kept[0] = s.copy();
                return true;
            }
            return false;
        });
        if (!kept[0].isEmpty()) StoneState.keep(p.getUUID(), new StoneState.Pending(kept[0], p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ()));
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) return;
        StoneSlot.forget(p.getUUID());
        StoneState.Pending pd = StoneState.takePending(p.getUUID());
        if (pd == null) return;
        if (!p.getInventory().add(pd.stack())) p.drop(pd.stack(), false);
        p.displayClientMessage(Component.translatableWithFallback("message.anime_multiverse.stone.returned", "The stone finds you again."), true);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        StoneState.Pending pd = StoneState.takePending(id);
        if (pd != null && e.getEntity().getServer() != null) dropPending(e.getEntity().getServer(), pd);
        StoneState.forgetAll(id);
        StoneSlot.forget(id);
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        StoneState.forgetMotion(e.getEntity().getUUID());
    }

    private static void dropPending(MinecraftServer server, StoneState.Pending pd) {
        ResourceLocation rl = ResourceLocation.tryParse(pd.dim());
        ServerLevel l = rl == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
        if (l == null) l = server.overworld();
        l.addFreshEntity(new ItemEntity(l, pd.x(), pd.y(), pd.z(), pd.stack()));
    }

    static void onStopping(ServerStoppingEvent e) {
        for (Map.Entry<UUID, StoneState.Pending> en : new ArrayList<>(StoneState.allPending().entrySet())) dropPending(e.getServer(), en.getValue());
        StoneState.clear();
        StoneSlot.clear();
    }

    // ================================================================ containers and the away rule
    /** A stone left in a chest, barrel, ender chest or shulker box goes back to the player who closes it. */
    static void onContainerClose(PlayerContainerEvent.Close e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide() || !StoneConfig.enabled() || !StoneConfig.containerRule()) return;
        AbstractContainerMenu menu = e.getContainer();
        if (!(menu instanceof ChestMenu) && !(menu instanceof ShulkerBoxMenu)) return;
        List<ItemStack> back = new ArrayList<>(2);
        for (Slot sl : menu.slots) {
            if (sl.container == p.getInventory()) continue;
            ItemStack s = sl.getItem();
            if (!s.isEmpty() && StoneSlot.stoneOf(s) != null) {
                back.add(s.copy());
                sl.set(ItemStack.EMPTY);
            }
        }
        for (ItemStack s : back) {
            if (!p.getInventory().add(s)) p.drop(s, false);
            p.displayClientMessage(Component.translatableWithFallback("message.anime_multiverse.stone.container", "The stone will not stay in a chest."), true);
        }
    }

    /** Once a minute: a stone whose holder has been away longer than the setting returns to the world through the Order's ledger. */
    static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int days = StoneConfig.awayDays();
        if (!StoneConfig.enabled() || days <= 0 || server.getTickCount() % 1200 != 600) return;
        List<String> ids = new ArrayList<>(6);
        for (InfinityStone s : InfinityStone.values()) ids.add(s.relicId());
        UniqueLedger.loseAway(server, ids, days * 86_400_000L);
    }
}
