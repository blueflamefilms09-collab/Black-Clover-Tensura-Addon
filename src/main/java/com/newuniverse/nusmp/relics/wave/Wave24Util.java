package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the small server-side helpers the twelve wave-24 relics share: the player's own saved numbers (persistent data, so timers survive relog and
 * death), effects by id, creatures around a point, an aim along the look direction, messages and Rukh particles. Everything tolerates a missing id.
 */
final class Wave24Util {
    private Wave24Util() {}

    private static final String ROOT = "nusmp_w24";
    private static final int MAX_FOES = 24;

    // ---------------------------------------------------------------- the player's own numbers
    static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }

    /** The same without making anything: null if this wave never kept a number for the player. */
    static CompoundTag peek(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) return null;
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        return root.contains(ROOT, Tag.TAG_COMPOUND) ? root.getCompound(ROOT) : null;
    }

    /** Is the timed state under this key still running? */
    static boolean running(Player p, String key) {
        CompoundTag d = peek(p);
        return d != null && d.getLong(key) > p.level().getGameTime();
    }

    static Wave24Rules.Dice dice(LivingEntity e) {
        net.minecraft.util.RandomSource r = e.getRandom();
        return r::nextInt;
    }

    // ---------------------------------------------------------------- stacks
    /** The first equipped stack of this relic id that works for the player (the real stack, so it can be used up), or empty. */
    static ItemStack stackOf(Player p, String relicId) {
        for (ItemStack s : Relics.equipped(p)) {
            var def = Relics.defOf(s);
            if (def != null && def.id().equals(relicId)) return s;
        }
        return ItemStack.EMPTY;
    }

    static String idOf(ItemStack s) {
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(s.getItem());
        return rl == null ? "" : rl.toString();
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

    /** An effect with no particles and no ambient swirl; skipped when the id does not exist. */
    static boolean give(LivingEntity e, String id, int ticks, int amp) {
        if (e == null || !e.isAlive() || e.isSpectator() || ticks <= 0) return false;
        Optional<Holder<MobEffect>> h = effect(id);
        if (h.isEmpty()) return false;
        e.addEffect(new MobEffectInstance(h.get(), ticks, Mth.clamp(amp, 0, 9), false, false, true));
        return true;
    }

    // ---------------------------------------------------------------- messages
    static void say(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(colour));
    }

    static void bar(ServerPlayer p, Component text) {
        if (p != null && text != null) p.displayClientMessage(text, true);
    }

    static void bar(ServerPlayer p, String text, ChatFormatting colour) { bar(p, Component.literal(text).withStyle(colour)); }

    // ---------------------------------------------------------------- particles (Rukh only)
    static FxTone align(boolean dark) { return FxTone.of(dark); }

    static void burst(ServerLevel l, Vec3 at, boolean dark, int count) { Fx.burst(l, FxThemes.RELIC_WAVE_24, at, align(dark), count); }

    static void trail(ServerLevel l, Vec3 from, Vec3 to, boolean dark) { Fx.stream(l, FxThemes.RELIC_WAVE_24, from, to, align(dark), 4); }

    // ---------------------------------------------------------------- creatures
    /** Is this a creature the player may hurt: hostile or hunting them, never an ally, a pet, a relic summon, a stand, or a creative or spectator player. */
    static boolean foe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
        if (e.getPersistentData().hasUUID("nusmp_relic_owner")) return false;
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** Anything an effect may touch: not the caster, not a stand, not a creative or spectating player, not a pet of the caster. */
    static boolean touchable(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        return !(e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID()));
    }

    /** Creatures within {@code r} blocks of a point that the filter accepts (at most 24, nearest first). */
    static List<LivingEntity> around(ServerLevel level, Vec3 at, double r, Predicate<LivingEntity> ok) {
        double rr = Mth.clamp(r, 0.5, 64.0);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(rr), e -> e.isAlive() && !e.isSpectator()
                && e.distanceToSqr(at) <= rr * rr && ok.test(e));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    // ---------------------------------------------------------------- aiming
    /** What a look along the player's sight found: the creature (or null) and where the line ended. */
    record Aim(LivingEntity target, Vec3 end) {}

    /** The nearest creature along the look direction within {@code range} among those {@code ok} accepts; a block stops the line. */
    static Aim aim(ServerPlayer p, double range, Predicate<LivingEntity> ok) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 far = eye.add(look.scale(range));
        BlockHitResult blocks = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (blocks.getType() != HitResult.Type.MISS) far = blocks.getLocation();
        AABB box = new AABB(eye, far).inflate(1.0);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        Vec3 bestAt = far;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive() && !x.isSpectator() && ok.test(x))) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eye, far);
            if (hit.isEmpty()) continue;
            double d = hit.get().distanceToSqr(eye);
            if (d < bestD) {
                bestD = d;
                best = e;
                bestAt = hit.get();
            }
        }
        return new Aim(best, best == null ? far : bestAt);
    }

    /** How many solid blocks lie on the straight line between two points (counted at half-block steps, each block once). */
    static int solidBetween(ServerLevel level, Vec3 from, Vec3 to) {
        double len = from.distanceTo(to);
        int steps = Mth.clamp((int) (len * 2), 1, 160);
        net.minecraft.core.BlockPos last = null;
        int solid = 0;
        for (int i = 1; i < steps; i++) {
            Vec3 v = from.lerp(to, i / (double) steps);
            net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(v);
            if (bp.equals(last)) continue;
            last = bp;
            if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) solid++;
        }
        return solid;
    }
}
