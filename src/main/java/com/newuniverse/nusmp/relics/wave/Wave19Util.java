package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import io.github.manasmods.tensura.storage.TensuraStorages;
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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Wave 19: the small server-side helpers its consumables share: the player's own numbers (kept in persistent data, so they survive relog and
 * death), effects by id, the foes around a point, dust. Everything tolerates a missing id.
 */
final class Wave19Util {
    private Wave19Util() {}

    private static final String ROOT = "nusmp_w19";
    private static final int MAX_FOES = 24;

    // ---------------------------------------------------------------- the player's own numbers
    static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }

    /** The same without making anything: null if this wave has never kept a number for the player. */
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

    /** An effect with no particles and no ambient swirl, for as long as asked (not capped to ten minutes like {@code Fx.addEffect}). */
    static boolean give(LivingEntity e, String id, int ticks, int amp) {
        if (e == null || !e.isAlive() || e.isSpectator() || ticks <= 0) return false;
        Optional<Holder<MobEffect>> h = effect(id);
        if (h.isEmpty()) return false;
        e.addEffect(new MobEffectInstance(h.get(), ticks, Math.max(0, Math.min(amp, 9)), false, false, true));
        return true;
    }

    // ---------------------------------------------------------------- messages
    static void say(ServerPlayer p, String text) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD));
    }

    static void tell(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(colour));
    }

    // ---------------------------------------------------------------- particles
    static net.minecraft.core.particles.SimpleParticleType dust(int rgb, float size) {
        return align(rgb).type();                    // Rukh replaces vanilla dust: the colour only picks the alignment
    }

    /** Rukh alignment for a legacy dust colour: dark colours (relative luminance below 0.33) become dark Rukh, the rest light. */
    static com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment align(int rgb) {
        double lum = (0.2126 * (rgb >> 16 & 255) + 0.7152 * (rgb >> 8 & 255) + 0.0722 * (rgb & 255)) / 255.0;
        return com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.of(lum < 0.33);
    }

    // ---------------------------------------------------------------- foes
    /** Is this a creature the player may hurt: hostile or hunting them, never an ally, a pet, a relic summon, a stand, or a creative or spectator player. */
    static boolean foe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
        if (e.getPersistentData().hasUUID("nusmp_relic_owner")) return false;
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** Foes within {@code r} blocks of a point (at most 24, nearest first). */
    static List<LivingEntity> foesAt(ServerPlayer p, Vec3 at, double r) {
        double rr = Math.max(0.5, Math.min(r, 24.0));
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(rr), e -> foe(p, e) && e.distanceToSqr(at) <= rr * rr);
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    // ---------------------------------------------------------------- magicules (Tensura)
    /** The share of the player's maximum magicules they hold now (0 to 1); 0 where Tensura has nothing for them. */
    static double magiculeFraction(ServerPlayer p) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = EnergyHelper.getMaxMagicule(p);
            return ex == null || max <= 0 ? 0 : Math.max(0, Math.min(1, ex.getMagicule() / max));
        } catch (RuntimeException | LinkageError none) {
            return 0;
        }
    }

    /** Adds a share of the player's maximum magicules (never past the maximum). */
    static void addMagicules(ServerPlayer p, double share) {
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = EnergyHelper.getMaxMagicule(p);
            if (ex == null || max <= 0) return;
            ex.setMagicule(Math.min(max, ex.getMagicule() + max * share));
            ex.markDirty();
        } catch (RuntimeException | LinkageError none) {
            // no Tensura data: nothing to add
        }
    }
}
