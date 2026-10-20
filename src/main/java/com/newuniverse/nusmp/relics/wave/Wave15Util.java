package com.newuniverse.nusmp.relics.wave;


import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 15: the small Minecraft helpers the seventeen cards share: where a player's own numbers are kept, effects and attributes by id (a missing id
 * is skipped), who an aimed ability may touch, particle lines. All of it runs on the server.
 */
final class Wave15Util {
    private Wave15Util() {}

    private static final String ROOT = "nusmp_w15";

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

    /** The first of these mob effects that exists is put on the entity ("tensura:chill" first, a vanilla stand-in after it). True if one was. */
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

    static Optional<Holder<Attribute>> attribute(String id) {
        ResourceLocation rl = rl(id);
        if (rl == null) return Optional.empty();
        return BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
    }

    // ---------------------------------------------------------------- attribute modifiers of this wave (transient: gone with a relog or a death)
    static ResourceLocation modId(String name) { return ResourceLocation.fromNamespaceAndPath("nusmp", "w15/" + name); }

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
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        return true;
    }

    /** A creature that is not a player and not a boss: the ones a stare may turn aside. */
    static boolean softMob(ServerPlayer p, LivingEntity e) {
        return e instanceof Mob && target(p, e) && e.getMaxHealth() < 150f;
    }

    /** The saving throw as Tensura existence: true if the target's maximum EP is so far above the caster's that it shrugs the effect off. */
    static boolean saves(ServerPlayer p, LivingEntity t) {
        try {
            return EnergyHelper.getMaxEP(t) > EnergyHelper.getMaxEP(p) * 2.0;
        } catch (RuntimeException | LinkageError noEnergy) {
            return false;
        }
    }

    /** Someone on the caster's side: the caster, a pet, a summon, a team mate. */
    static boolean friend(ServerPlayer p, LivingEntity e) {
        return e == p || (e.isAlive() && !e.isSpectator() && e.isAlliedTo(p));
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

    static Aim aim(ServerPlayer p, double range) { return aim(p, range, e -> target(p, e)); }

    static Aim aim(ServerPlayer p, double range, Predicate<LivingEntity> who) {
        Vec3 start = p.getEyePosition(), end = start.add(p.getViewVector(1f).scale(range));
        HitResult wall = p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, start, end,
                p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), e -> e instanceof LivingEntity le && who.test(le));
        if (hit != null) return new Aim((LivingEntity) hit.getEntity(), hit.getLocation());
        return new Aim(null, end);
    }

    // ---------------------------------------------------------------- particles and words
    static net.minecraft.core.particles.ParticleOptions dust(int rgb, float size) {
        return FxRelic.dust(rgb, size);                    // Rukh replaces vanilla dust: the colour only picks the alignment
    }

    /** Rukh alignment for a legacy dust colour: dark colours (relative luminance below 0.33) become dark Rukh, the rest light. */
    static FxTone align(int rgb) {
        double lum = (0.2126 * (rgb >> 16 & 255) + 0.7152 * (rgb >> 8 & 255) + 0.0722 * (rgb & 255)) / 255.0;
        return FxTone.of(lum < 0.33);
    }

    /** A line of particles (at most 48 points) from one point to another. */
    static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.1) return;
        int n = (int) Math.max(2, Math.min(48, len * 2.0));
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(d.scale(i / (double) n));
            Fx.spray(level, FxThemes.RELIC_WAVE_15, FxTone.of(Fx.toneOf(particle) == FxTone.DARK), at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /** A line on the action bar (the chat stays quiet for the small things). */
    static void tell(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.displayClientMessage(Component.literal(text).withStyle(colour), true);
    }

    static void tell(ServerPlayer p, String text) { tell(p, text, ChatFormatting.GOLD); }
}
