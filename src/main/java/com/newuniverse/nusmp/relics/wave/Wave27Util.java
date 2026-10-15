package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Why: the small Minecraft helpers the twelve cards of wave 27 share: where a player's own numbers are kept, effects by id (a missing id is skipped),
 * who an aimed ability may touch, a pickpocket's reach. All of it runs on the server and keeps no static state.
 */
final class Wave27Util {
    private Wave27Util() {}

    private static final String ROOT = "nusmp_w27";

    // ---------------------------------------------------------------- the player's own numbers
    /** The persisted compound where this wave keeps its per-player numbers (survives relog and death). */
    static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }

    /** The same without making anything: null if the player has never had a number of this wave kept. */
    static CompoundTag peek(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) return null;
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        return root.contains(ROOT, Tag.TAG_COMPOUND) ? root.getCompound(ROOT) : null;
    }

    /** A pool of dawn charges kept under {@code key}: read (and refreshed to today), with {@code gain} charges per dawn gone. */
    static Wave27Rules.Charges charges(ServerPlayer p, String key, int max, int gain) {
        CompoundTag d = data(p);
        Wave27Rules.Charges c = d.contains(key + "_n") ? new Wave27Rules.Charges(max, d.getInt(key + "_n"), d.getLong(key + "_day")) : Wave27Rules.Charges.fresh(max);
        Wave27Rules.Charges now = c.at(Wave27Rules.dayOf(p.level().getDayTime()), gain);
        store(p, key, now);
        return now;
    }

    static void store(ServerPlayer p, String key, Wave27Rules.Charges c) {
        CompoundTag d = data(p);
        d.putInt(key + "_n", c.left());
        d.putLong(key + "_day", c.day());
    }

    // ---------------------------------------------------------------- ids
    static ResourceLocation rl(String id) {
        if (id == null || id.isBlank()) return null;
        return id.indexOf(':') >= 0 ? ResourceLocation.tryParse(id.trim()) : ResourceLocation.withDefaultNamespace(id.trim());
    }

    static Optional<Holder<MobEffect>> effect(String id) {
        ResourceLocation rl = rl(id);
        if (rl == null) return Optional.empty();
        return BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> (Holder<MobEffect>) h);
    }

    /** The first of these mob effects that exists is put on the entity ("tensura:webbed" first, a vanilla stand-in after it). True if one was. */
    static boolean give(LivingEntity e, int ticks, int amp, String... ids) {
        if (e == null || !e.isAlive() || e.isSpectator() || ticks <= 0) return false;
        for (String id : ids) {
            Optional<Holder<MobEffect>> h = effect(id);
            if (h.isPresent()) {
                e.addEffect(new MobEffectInstance(h.get(), ticks, Math.max(0, Math.min(amp, 9))));
                return true;
            }
        }
        return false;
    }

    /** The same, quiet (no particles): for a state that is kept alive every second. */
    static boolean giveQuiet(LivingEntity e, int ticks, int amp, String id) {
        Optional<Holder<MobEffect>> h = effect(id);
        if (e == null || h.isEmpty() || ticks <= 0 || !e.isAlive()) return false;
        e.addEffect(new MobEffectInstance(h.get(), ticks, Math.max(0, Math.min(amp, 9)), true, false));
        return true;
    }

    /** The amplifier of an effect on the entity, or -1 if it is not there (or the id does not exist). */
    static int amp(LivingEntity e, String id) {
        Optional<Holder<MobEffect>> h = effect(id);
        if (h.isEmpty()) return -1;
        MobEffectInstance i = e.getEffect(h.get());
        return i == null ? -1 : i.getAmplifier();
    }

    // ---------------------------------------------------------------- who is a target
    /** Anything living that an aimed ability may hurt or bend: not the caster, not an ally or a pet, not a creative or spectator player. */
    static boolean target(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        return true;
    }

    /** A creature that is not a player and not a boss: the ones a hand may rob. */
    static boolean softMob(ServerPlayer p, LivingEntity e) {
        return e instanceof Mob && target(p, e) && e.getMaxHealth() < 150f;
    }

    /** Someone who would fight the wearer: a monster, or anything already after them. */
    static boolean hostile(ServerPlayer p, LivingEntity e) {
        return e instanceof Enemy || (e instanceof Mob m && m.getTarget() == p);
    }

    /** The saving throw as Tensura existence: true if the target's maximum EP is so far above the caster's that it shrugs the effect off. */
    static boolean saves(ServerPlayer p, LivingEntity t) {
        try {
            return Wave27Rules.tooDeep(EnergyHelper.getMaxEP(t), EnergyHelper.getMaxEP(p));
        } catch (RuntimeException | LinkageError noEnergy) {
            return false;
        }
    }

    /** Someone on the caster's side: the caster, a pet, a summon, a team mate, or any player who is not an enemy. */
    static boolean friend(ServerPlayer p, LivingEntity e) {
        return e == p || (e.isAlive() && !e.isSpectator() && (e.isAlliedTo(p) || e instanceof Player || e instanceof Villager));
    }

    /** Living things within a radius of a point (at most 24, nearest first) that pass the test. */
    static List<LivingEntity> near(ServerLevel level, Vec3 c, double r, Predicate<LivingEntity> test) {
        List<LivingEntity> out = level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(c) <= r * r && test.test(e));
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        return out.size() > 24 ? new ArrayList<>(out.subList(0, 24)) : out;
    }

    // ---------------------------------------------------------------- aiming
    /** Where a ray from the eyes ends and whom it hits first (null if nobody): stops at the first solid block. */
    record Aim(LivingEntity hit, Vec3 end) {}

    static Aim aim(ServerPlayer p, double range, Predicate<LivingEntity> who) {
        Vec3 start = p.getEyePosition(), end = start.add(p.getViewVector(1f).scale(range));
        HitResult wall = p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, start, end,
                p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), e -> e instanceof LivingEntity le && who.test(le));
        if (hit != null) return new Aim((LivingEntity) hit.getEntity(), hit.getLocation());
        return new Aim(null, end);
    }

    static Aim aim(ServerPlayer p, double range) { return aim(p, range, e -> target(p, e)); }

    // ---------------------------------------------------------------- pickpocketing
    /**
     * Takes one thing a creature holds (a villager: something from its pockets; any other mob: what is in its hands) and puts it in the thief's
     * bags (or at their feet). Returns the stack taken, or empty if there was nothing or the creature is a boss.
     */
    static ItemStack steal(ServerPlayer p, LivingEntity t) {
        if (!softMob(p, t)) return ItemStack.EMPTY;
        ItemStack got = ItemStack.EMPTY;
        if (t instanceof Villager v) {
            for (int i = 0; i < v.getInventory().getContainerSize() && got.isEmpty(); i++) {
                ItemStack s = v.getInventory().getItem(i);
                if (!s.isEmpty()) got = v.getInventory().removeItem(i, 1);
            }
        }
        if (got.isEmpty()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                ItemStack s = t.getItemBySlot(slot);
                if (!s.isEmpty()) {
                    got = s.copy();
                    t.setItemSlot(slot, ItemStack.EMPTY);
                    break;
                }
            }
        }
        if (got.isEmpty()) return got;
        ItemStack out = got.copy();
        if (!p.getInventory().add(got)) p.drop(got, false);
        return out;
    }

    /** True if the creature holds anything in its hands (what a melee fumble can steal). */
    static boolean holdsSomething(LivingEntity t) {
        return !t.getMainHandItem().isEmpty() || !t.getOffhandItem().isEmpty();
    }

    // ---------------------------------------------------------------- words
    /** A line on the action bar (the chat stays quiet for the small things). */
    static void tell(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.displayClientMessage(Component.literal(text).withStyle(colour), true);
    }

    /** A line in the chat: for what the wearer wants to keep (a read soul, an aura report). */
    static void say(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(colour));
    }

    static void puff(ServerLevel lv, RukhFx.Alignment a, LivingEntity e, int count) {
        RukhFx.spray(lv, a, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), count, 0.3, 0.4, 0.3, 0.02);
    }
}
