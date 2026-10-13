package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.dungeon.MealSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The helpers every relic wave calls. All of them run on the server, never throw, skip creative and spectator players as targets, skip a missing
 * effect or entity id silently (an optional mod may be absent), and are capped: at most 24 foes, a radius of 32, bounded durations and damage.
 * Damage goes through the mod's own {@link BalanceLaw} (EP scaling, the PvP hit cap) and then through the relics' stricter cap on players (30% of
 * their maximum health per call, 45% for {@link #damageHeavy}, which belongs to abilities that cost existence points).
 */
public final class Fx {
    private Fx() {}

    private static final double MAX_RADIUS = 32.0;
    private static final int MAX_FOES = 24, MAX_TICKS = 20 * 60 * 10, MAX_AMP = 9;
    /** The persistent-data key that marks an entity summoned by a relic, with its owner's UUID and the end of its life. */
    static final String OWNER = "nusmp_relic_owner", UNTIL = "nusmp_relic_until";

    // ---------------------------------------------------------------- healing and effects
    /** Heals the player through the balance law (50% of maximum health at most per cast, less if healed a moment ago). */
    public static void heal(ServerPlayer p, float amount) {
        if (p == null || !p.isAlive() || p.isSpectator() || amount <= 0f) return;
        BalanceLaw.heal(p, Math.min(amount, 200f));
    }

    /** A mob effect by id ("minecraft:slowness", "tensura:fear"); no particles in the icon row. Skipped if the effect does not exist. */
    public static void addEffect(LivingEntity e, String mobEffectId, int ticks, int amp) {
        if (e == null || !e.isAlive() || e.isSpectator() || mobEffectId == null || ticks <= 0) return;
        Optional<? extends net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> h = effect(mobEffectId);
        if (h.isEmpty()) return;
        e.addEffect(new MobEffectInstance(h.get(), Math.min(ticks, MAX_TICKS), Mth.clamp(amp, 0, MAX_AMP)));
    }

    static Optional<? extends net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> effect(String id) {
        ResourceLocation rl = id(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(rl);
    }

    static ResourceLocation id(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return s.indexOf(':') >= 0 ? ResourceLocation.parse(s.trim()) : ResourceLocation.withDefaultNamespace(s.trim());
        } catch (RuntimeException bad) {
            return null;
        }
    }

    /** Absorption hearts (about {@code absorb} health) that fade away when the time is up. */
    public static void shield(ServerPlayer p, float absorb, int ticks) {
        if (p == null || !p.isAlive() || absorb <= 0f) return;
        addEffect(p, "minecraft:absorption", ticks, Mth.clamp((int) Math.ceil(absorb / 4.0) - 1, 0, 4));
    }

    // ---------------------------------------------------------------- Tensura skills
    /**
     * Lends a Tensura skill for {@code ticks} (at most half an hour), through the Dungeon meals' lease ({@link MealSkills#grant}): the lease is saved,
     * taken back when the time is up, and survives relog and death; a skill the player owns for good is left alone. True if the skill is now the
     * player's (lent, extended, or owned already); false if the id is not in the skill registry or Tensura's skill list is not there.
     */
    public static boolean lendSkill(ServerPlayer p, String skillId, int ticks) {
        if (p == null || skillId == null || !p.isAlive() || p.isSpectator()) return false;
        try {
            MealSkills.Outcome o = MealSkills.grant(p, skillId, Mth.clamp(ticks, 20, 20 * 60 * 30), "Your relic");   // neutral wording: not "The meal lends you"
            return o == MealSkills.Outcome.GRANTED || o == MealSkills.Outcome.EXTENDED || o == MealSkills.Outcome.OWNED;
        } catch (RuntimeException | LinkageError none) {
            return false;
        }
    }

    /** The merge helper: lends several skills at once for the same time (ids the registry does not know are skipped); returns how many the player has now. */
    public static int lendSkills(ServerPlayer p, int ticks, String... skillIds) {
        int n = 0;
        if (skillIds != null) for (String id : skillIds) if (lendSkill(p, id, ticks)) n++;
        return n;
    }

    // ---------------------------------------------------------------- foes and damage
    /** Hostile and non-allied living things within the radius (at most 24, nearest first). Never creative or spectator players, allies, pets or summons of the caster. */
    public static List<LivingEntity> foesAround(ServerPlayer p, double radius) {
        if (p == null || !p.isAlive()) return List.of();
        double r = Mth.clamp(radius, 0.5, MAX_RADIUS);
        Vec3 c = p.position();
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> isFoe(p, e) && e.distanceToSqr(c) <= r * r);
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
        if (e.getPersistentData().hasUUID(OWNER)) return false;                       // a relic summon (of anyone) is not a target
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** A hit on {@code target} with this damage type id ("minecraft:magic", "minecraft:lightning_bolt"; unknown ids become a plain player attack). */
    public static void damage(ServerPlayer p, LivingEntity target, float amount, String damageTypeId) {
        hit(p, target, amount, damageTypeId, 0.30);
    }

    /** The same with a higher cap on players (45% of their maximum health): for abilities that cost existence points. */
    public static void damageHeavy(ServerPlayer p, LivingEntity target, float amount, String damageTypeId) {
        hit(p, target, amount, damageTypeId, 0.45);
    }

    private static void hit(ServerPlayer p, LivingEntity t, float amount, String typeId, double playerCap) {
        if (p == null || t == null || t == p || !t.isAlive() || amount <= 0f || t.isAlliedTo(p)) return;
        if (t instanceof Player o && (o.isCreative() || o.isSpectator())) return;
        float dmg = BalanceLaw.damage(p, t, Math.min(amount, 400f), 0.6);
        if (t instanceof Player) dmg = (float) Math.min(dmg, t.getMaxHealth() * playerCap);
        t.hurt(source(p, typeId), Math.min(dmg, 400f));
    }

    private static DamageSource source(ServerPlayer p, String typeId) {
        ResourceLocation rl = id(typeId);
        if (rl != null) {
            try {
                var holder = p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, rl));
                if (holder.isPresent()) return new DamageSource(holder.get(), p, p);
            } catch (RuntimeException ignored) {
                // an unknown type: fall through to a plain hit
            }
        }
        return p.damageSources().playerAttack(p);
    }

    /** Damage and an effect on every foe within the radius (nearest 24). A damage of 0 or a null effect id skips that half. */
    public static void aoe(ServerPlayer p, double radius, float damage, String damageTypeId, String mobEffectId, int ticks, int amp) {
        for (LivingEntity t : foesAround(p, radius)) {
            if (damage > 0f) damage(p, t, damage, damageTypeId);
            if (mobEffectId != null) addEffect(t, mobEffectId, ticks, amp);
        }
    }

    // ---------------------------------------------------------------- movement
    /** A burst of speed along the look direction (about {@code blocks} blocks, at most 12); no fall damage from it. */
    public static void dash(ServerPlayer p, double blocks) {
        if (p == null || !p.isAlive() || p.isPassenger() || p.isFallFlying()) return;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? look : flat.normalize();
        double v = Mth.clamp(blocks, 0.0, 12.0) * 0.45;
        p.setDeltaMovement(dir.x * v, Math.max(p.getDeltaMovement().y, 0.12), dir.z * v);
        p.hurtMarked = true;
        p.fallDistance = 0;
    }

    /** Blinks along the look direction up to {@code range} blocks (at most 32) to a spot with room to stand and ground below; false if there is none. */
    public static boolean teleportLook(ServerPlayer p, double range) {
        if (p == null || !p.isAlive() || p.isPassenger()) return false;
        double reach = Mth.clamp(range, 1.0, 32.0);
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double dist = reach;
        HitResult hit = p.pick(reach, 1.0f, false);
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) dist = Math.max(0.0, Math.sqrt(b.getLocation().distanceToSqr(eye)) - 0.6);
        for (double d = dist; d >= 1.0; d -= 0.5) {                                    // from the far end back towards the player: the first safe spot wins
            Vec3 at = eye.add(look.scale(d));
            Vec3 spot = safe(level, p, at.x, at.y - p.getEyeHeight(), at.z);
            if (spot != null) {
                p.teleportTo(spot.x, spot.y, spot.z);
                p.fallDistance = 0;
                return true;
            }
        }
        return false;
    }

    private static Vec3 safe(ServerLevel level, ServerPlayer p, double x, double y, double z) {
        for (int dy : new int[]{0, 1, -1, 2, -2}) {
            double yy = y + dy;
            AABB box = p.getBoundingBox().move(x - p.getX(), yy - p.getY(), z - p.getZ());
            if (!level.noCollision(p, box)) continue;
            BlockPos feet = BlockPos.containing(x, yy, z);
            boolean ground = false;
            for (int i = 1; i <= 3 && !ground; i++) {                                   // something solid to land on within three blocks
                BlockState below = level.getBlockState(feet.below(i));
                if (below.is(net.minecraft.world.level.block.Blocks.LAVA) || below.is(net.minecraft.world.level.block.Blocks.FIRE)) return null;
                ground = !below.getCollisionShape(level, feet.below(i)).isEmpty();
            }
            if (ground && !level.getFluidState(feet).is(net.minecraft.tags.FluidTags.LAVA)) return new Vec3(x, yy, z);
        }
        return null;
    }

    // ---------------------------------------------------------------- particles and words
    /** A ring of coloured dust on the ground plus a grimoire circle (0xRRGGBB). */
    public static void ring(ServerLevel l, Vec3 at, double radius, int colorRgb) {
        if (l == null || at == null) return;
        double r = Mth.clamp(radius, 0.5, MAX_RADIUS);
        DustParticleOptions dust = dust(colorRgb, 1.2f);
        int n = Mth.clamp((int) (r * 8), 12, 64);
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            l.sendParticles(dust, at.x + Math.cos(a) * r, at.y + 0.1, at.z + Math.sin(a) * r, 1, 0, 0.02, 0, 0);
        }
        VfxSpawn.send(l, VfxShape.MAGIC_CIRCLE, at.add(0, 0.08, 0), at.add(0, 1, 0), 0xFF000000 | (colorRgb & 0xFFFFFF), 24, (float) Math.min(6.0, r * 0.7));
    }

    /** A puff of coloured dust and sparks (0xRRGGBB). */
    public static void burst(ServerLevel l, Vec3 at, int colorRgb) {
        if (l == null || at == null) return;
        l.sendParticles(dust(colorRgb, 1.5f), at.x, at.y + 0.6, at.z, 32, 0.5, 0.5, 0.5, 0.05);
        l.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.6, at.z, 12, 0.4, 0.4, 0.4, 0.08);
    }

    static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** A line in chat from the relic (gold). */
    public static void say(ServerPlayer p, String text) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD));
    }

    // ---------------------------------------------------------------- allies
    /**
     * A time-limited ally: the entity type by id (Tensura or vanilla; skipped silently if absent), tamed to the player where it can be, gone after
     * {@code lifeTicks} (at most five minutes) and at most six per player at once. Returns it, or null.
     */
    public static LivingEntity summon(ServerPlayer p, String entityTypeId, int lifeTicks) {
        ResourceLocation rl = id(entityTypeId);
        if (p == null || rl == null || !p.isAlive()) return null;
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        if (type.isEmpty()) return null;
        ServerLevel level = p.serverLevel();
        try {
            net.minecraft.world.entity.Entity made = type.get().create(level);
            if (!(made instanceof LivingEntity living)) return null;
            RelicSummons.makeRoom(p);
            Vec3 look = p.getLookAngle();
            living.moveTo(p.getX() + look.x * 2, p.getY(), p.getZ() + look.z * 2, p.getYRot() + 180f, 0f);
            if (living instanceof Mob m) m.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            if (living instanceof TamableAnimal t) t.tame(p);
            living.getPersistentData().putUUID(OWNER, p.getUUID());
            living.getPersistentData().putLong(UNTIL, level.getGameTime() + Mth.clamp(lifeTicks, 20, 6000));
            if (!level.addFreshEntity(living)) return null;
            RelicSummons.track(p, living);
            burst(level, living.position(), 0xB48CFF);
            return living;
        } catch (RuntimeException | LinkageError failed) {
            return null;
        }
    }
}
