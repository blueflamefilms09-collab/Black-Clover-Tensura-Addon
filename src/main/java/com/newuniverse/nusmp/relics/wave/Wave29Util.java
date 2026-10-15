package com.newuniverse.nusmp.relics.wave;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Why: the four potions of wave 29 share a few server-side helpers: a creature's own timers (kept in persistent data, so they survive relog and death for
 * players), effects by id with no particles and no ten-minute cap, the "is this a foe" test, the magicule pool read, and the hand-back of an unused potion.
 * Everything tolerates a missing id.
 */
final class Wave29Util {
    private Wave29Util() {}

    private static final String ROOT = "nusmp_w29";

    // ---------------------------------------------------------------- a creature's own numbers
    /** The creature's wave 29 data (made if missing). Players keep it in the part of persistent data that survives death. */
    static CompoundTag data(LivingEntity e) {
        CompoundTag base = e.getPersistentData();
        if (e instanceof Player) {
            if (!base.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) base.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
            base = base.getCompound(Player.PERSISTED_NBT_TAG);
        }
        if (!base.contains(ROOT, Tag.TAG_COMPOUND)) base.put(ROOT, new CompoundTag());
        return base.getCompound(ROOT);
    }

    /** The same without making anything: null if wave 29 has never kept a number for this creature. */
    static CompoundTag peek(LivingEntity e) {
        CompoundTag base = e.getPersistentData();
        if (e instanceof Player) {
            if (!base.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) return null;
            base = base.getCompound(Player.PERSISTED_NBT_TAG);
        }
        return base.contains(ROOT, Tag.TAG_COMPOUND) ? base.getCompound(ROOT) : null;
    }

    /** Is the timed state under this key still running (an end time in the future)? */
    static boolean running(LivingEntity e, String key) {
        CompoundTag d = peek(e);
        return d != null && d.getLong(key) > e.level().getGameTime();
    }

    // ---------------------------------------------------------------- ids and effects
    static ResourceLocation rl(String id) {
        if (id == null || id.isBlank()) return null;
        return id.indexOf(':') >= 0 ? ResourceLocation.tryParse(id.trim()) : ResourceLocation.withDefaultNamespace(id.trim());
    }

    static Optional<Holder<MobEffect>> effect(String id) {
        ResourceLocation rl = rl(id);
        if (rl == null) return Optional.empty();
        return BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> (Holder<MobEffect>) h);
    }

    /** An effect with no particles, for as long as asked (not capped to ten minutes like {@code Fx.addEffect}); false if the id is not there. */
    static boolean give(LivingEntity e, String id, int ticks, int amp) {
        if (e == null || !e.isAlive() || e.isSpectator() || ticks <= 0) return false;
        Optional<Holder<MobEffect>> h = effect(id);
        if (h.isEmpty()) return false;
        e.addEffect(new MobEffectInstance(h.get(), ticks, Math.max(0, Math.min(amp, 9)), false, false, true));
        return true;
    }

    /** Removes the effect only if it is the one the potions give: the named id at this amplifier or more. */
    static void strip(LivingEntity e, String id, int minAmp) {
        Optional<Holder<MobEffect>> h = effect(id);
        if (h.isEmpty()) return;
        MobEffectInstance inst = e.getEffect(h.get());
        if (inst != null && inst.getAmplifier() >= minAmp) e.removeEffect(h.get());
    }

    // ---------------------------------------------------------------- words
    static void say(ServerPlayer p, String text) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD));
    }

    static void tell(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(colour));
    }

    // ---------------------------------------------------------------- potions handed back
    /** The consumable is used up after the act; this gives it back (the act refused or found nothing to work on). */
    static void keep(ServerPlayer p, ItemStack stack) {
        if (!p.isCreative()) stack.grow(1);
    }

    // ---------------------------------------------------------------- foes and the pool
    /** Is this a creature the player may hurt or call a foe: hostile or hunting them, never an ally, a pet, a relic summon, a stand, or a creative or spectator player. */
    static boolean foe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
        if (e.getPersistentData().hasUUID("nusmp_relic_owner")) return false;
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** The share of the player's maximum magicules they hold now (0 to 1); 0 where Tensura has nothing for them. */
    static double magiculeFraction(LivingEntity e) {
        if (!(e instanceof Player p)) return 0;
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = EnergyHelper.getMaxMagicule(p);
            return ex == null || max <= 0 ? 0 : Math.max(0, Math.min(1, ex.getMagicule() / max));
        } catch (RuntimeException | LinkageError none) {
            return 0;
        }
    }
}
