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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
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
 * Why: every card of the wave needs the same few safe lookups (a mob effect that may not exist, an attribute that may not be there, a target that must
 * never be a creative player), so they live once here instead of being copied into each ability.
 * <p>
 * Wave 28: the small Minecraft helpers the twelve cards share: where a player's own numbers are kept, effects and attributes by id (a missing id is
 * skipped), who an aimed ability may touch, aiming, particles and the line of words on the action bar. All of it runs on the server.
 */
final class Wave28Util {
    private Wave28Util() {}

    private static final String ROOT = "nusmp_w28";

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

    /** The first of these mob effects that exists is put on the entity (a Tensura one first, a vanilla stand-in after it). True if one was. */
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

    static void remove(LivingEntity e, String... ids) {
        for (String id : ids) effect(id).ifPresent(h -> e.removeEffect(h));
    }

    static boolean has(LivingEntity e, String id) {
        Optional<Holder<MobEffect>> h = effect(id);
        return h.isPresent() && e.hasEffect(h.get());
    }

    static Optional<Holder<Attribute>> attribute(String id) {
        ResourceLocation rl = rl(id);
        if (rl == null) return Optional.empty();
        return BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
    }

    // ---------------------------------------------------------------- attribute modifiers of this wave (transient: gone with a relog or a death)
    static ResourceLocation modId(String name) { return ResourceLocation.fromNamespaceAndPath("nusmp", "w28/" + name); }

    static boolean hasMod(LivingEntity e, String attrId, String name) {
        Optional<Holder<Attribute>> h = attribute(attrId);
        AttributeInstance inst = h.isEmpty() ? null : e.getAttribute(h.get());
        return inst != null && inst.getModifier(modId(name)) != null;
    }

    /** Puts the modifier on (once; calling it again while it is there does nothing). */
    static void ensureMod(LivingEntity e, String attrId, String name, double amount, AttributeModifier.Operation op) {
        Optional<Holder<Attribute>> h = attribute(attrId);
        AttributeInstance inst = h.isEmpty() ? null : e.getAttribute(h.get());
        if (inst == null || inst.getModifier(modId(name)) != null) return;
        inst.addTransientModifier(new AttributeModifier(modId(name), amount, op));
    }

    static void dropMod(LivingEntity e, String attrId, String name) {
        Optional<Holder<Attribute>> h = attribute(attrId);
        AttributeInstance inst = h.isEmpty() ? null : e.getAttribute(h.get());
        if (inst != null && inst.getModifier(modId(name)) != null) inst.removeModifier(modId(name));
    }

    // ---------------------------------------------------------------- who is a target
    /** Anything living that an aimed ability may hurt or bend: not the caster, not an ally or a pet, not a creative or spectator player. */
    static boolean target(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e.isAlliedTo(p)) return false;
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        return true;
    }

    /** A hostile thing: a monster, a player who may be fought, or a creature that is hunting the caster. Never a farm animal. */
    static boolean foe(ServerPlayer p, LivingEntity e) {
        if (!target(p, e)) return false;
        if (e instanceof Player || e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** Someone on the caster's side: the caster, a pet, a summon, a team mate, a player the caster may not fight. */
    static boolean friend(ServerPlayer p, LivingEntity e) {
        if (e == p) return e.isAlive();
        if (!e.isAlive() || e.isSpectator()) return false;
        if (e.isAlliedTo(p)) return true;
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return true;
        return e instanceof Player o && !o.isCreative() && !p.canHarmPlayer(o);
    }

    /** The saving throw as Tensura existence: true if the target's maximum EP is so far above the caster's that it shrugs the effect off. */
    static boolean saves(ServerPlayer p, LivingEntity t) {
        try {
            return Wave28Rules.charmSaves(EnergyHelper.getMaxEP(t), EnergyHelper.getMaxEP(p));
        } catch (RuntimeException | LinkageError noEnergy) {
            return false;
        }
    }

    /** Living things within a radius of a point (at most 24, nearest first) that pass the test. */
    static List<LivingEntity> near(ServerLevel level, Vec3 c, double r, Predicate<LivingEntity> test) {
        List<LivingEntity> out = level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(c) <= r * r && test.test(e));
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

    /** Foes in front of the caster: inside the radius and inside the cone ({@code minDot} is the cosine of the half angle). */
    static List<LivingEntity> cone(ServerPlayer p, double range, double minDot) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        return near(p.serverLevel(), p.position(), range, e -> {
            if (!foe(p, e)) return false;
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double len = to.length();
            return len < 0.5 || to.scale(1.0 / len).dot(look) >= minDot;
        });
    }

    // ---------------------------------------------------------------- particles, words, items
    /** A line of Rukh (at most 40 points) from one point to another. */
    static void trail(ServerLevel level, Vec3 from, Vec3 to, RukhFx.Alignment a) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.1) return;
        int n = (int) Math.max(2, Math.min(40, len * 2.0));
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(d.scale(i / (double) n));
            RukhFx.spray(level, a, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    static void burst(ServerLevel level, Vec3 at, RukhFx.Alignment a, int count) { RukhFx.burst(level, at, a, count, 0.5); }

    /** A line on the action bar (the chat stays quiet for the small things). */
    static void tell(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.displayClientMessage(Component.literal(text).withStyle(colour), true);
    }

    static void tell(ServerPlayer p, String text) { tell(p, text, ChatFormatting.GOLD); }

    /** A line in the chat (for the things worth keeping on screen: the potions' stories). */
    static void say(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(colour));
    }

    /** The item with this registry id (empty if the id does not exist). */
    static ItemStack stack(String id, int count) {
        ResourceLocation rl = rl(id);
        Optional<Item> it = rl == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(rl);
        return it.map(i -> new ItemStack(i, Math.max(1, Math.min(count, 16)))).orElse(ItemStack.EMPTY);
    }

    /** Spits an item out in front of the player's mouth (no pick-up delay trouble: a short one). */
    static void spit(ServerPlayer p, ItemStack s) {
        if (s == null || s.isEmpty()) return;
        Vec3 mouth = p.getEyePosition().add(p.getViewVector(1f).scale(0.4)).add(0, -0.25, 0);
        ItemEntity ie = new ItemEntity(p.serverLevel(), mouth.x, mouth.y, mouth.z, s);
        Vec3 v = p.getViewVector(1f).scale(0.25);
        ie.setDeltaMovement(v.x, 0.15, v.z);
        ie.setPickUpDelay(20);
        p.serverLevel().addFreshEntity(ie);
    }
}
