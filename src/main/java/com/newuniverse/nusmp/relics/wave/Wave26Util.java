package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
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
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/**
 * Why: the twelve head relics of wave 26 share the same small Minecraft chores (where a player's own numbers are kept, effects by id with a
 * vanilla stand-in, attribute modifiers of the wave's own, who a spell may touch, what a player carries), so they live once here instead of
 * in every relic. All of it runs on the server and tolerates a missing Tensura id.
 */
final class Wave26Util {
    private Wave26Util() {}

    private static final String ROOT = "nusmp_w26";
    /** The key {@link Fx#summon} puts on a relic ally: such a creature is never a target. */
    static final String OWNER_KEY = "nusmp_relic_owner";
    /** The wave's own marker of a charmed creature: its charmer and the game time its charm ends. */
    static final String CHARM_OWNER = "nusmp_w26_charm_owner", CHARM_UNTIL = "nusmp_w26_charm_until";

    // ---------------------------------------------------------------- the player's own numbers (persisted: survives relog and death)
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

    static IntUnaryOperator rng(LivingEntity e) { return bound -> e.getRandom().nextInt(Math.max(1, bound)); }

    static long today(ServerLevel level) { return Wave26Rules.dayOf(level.getDayTime()); }

    // ---------------------------------------------------------------- ids, effects, attributes
    static ResourceLocation rl(String id) {
        if (id == null || id.isBlank()) return null;
        return id.indexOf(':') >= 0 ? ResourceLocation.tryParse(id.trim()) : ResourceLocation.withDefaultNamespace(id.trim());
    }

    static Optional<Holder<MobEffect>> effect(String id) {
        ResourceLocation rl = rl(id);
        if (rl == null) return Optional.empty();
        return BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> (Holder<MobEffect>) h);
    }

    /** The first of these mob effects that exists is put on the entity (a Tensura id first, a vanilla stand-in after it). True if one was. */
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

    /** The same without particles: for the long effects a relic leaves on its wearer. */
    static boolean giveQuiet(LivingEntity e, int ticks, int amp, String id) {
        Optional<Holder<MobEffect>> h = effect(id);
        if (e == null || h.isEmpty() || ticks <= 0) return false;
        e.addEffect(new MobEffectInstance(h.get(), ticks, Math.max(0, Math.min(amp, 9)), false, true));
        return true;
    }

    static void remove(LivingEntity e, String... ids) {
        if (e == null) return;
        for (String id : ids) effect(id).ifPresent(e::removeEffect);
    }

    static boolean has(LivingEntity e, String... ids) {
        if (e == null) return false;
        for (String id : ids) {
            Optional<Holder<MobEffect>> h = effect(id);
            if (h.isPresent() && e.hasEffect(h.get())) return true;
        }
        return false;
    }

    static Optional<Holder<Attribute>> attribute(String id) {
        ResourceLocation rl = rl(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
    }

    private static ResourceLocation modId(String name) { return ResourceLocation.fromNamespaceAndPath("nusmp", "w26/" + name); }

    static void setMod(LivingEntity e, String attrId, String name, double amount, AttributeModifier.Operation op) {
        Optional<Holder<Attribute>> a = attribute(attrId);
        if (e == null || a.isEmpty()) return;
        AttributeInstance inst = e.getAttribute(a.get());
        if (inst == null) return;
        ResourceLocation id = modId(name);
        AttributeModifier cur = inst.getModifier(id);
        if (cur != null && cur.amount() == amount && cur.operation() == op) return;
        if (cur != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    static void dropMod(LivingEntity e, String attrId, String name) {
        Optional<Holder<Attribute>> a = attribute(attrId);
        if (e == null || a.isEmpty()) return;
        AttributeInstance inst = e.getAttribute(a.get());
        if (inst != null) inst.removeModifier(modId(name));
    }

    // ---------------------------------------------------------------- who is a target
    /** Anything living that a spell of ours may hurt or bend: not the caster, not an ally or a pet, not a creative or spectator player. */
    static boolean target(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e.isAlliedTo(p)) return false;
        if (e.getPersistentData().hasUUID(OWNER_KEY)) return false;                       // a relic summon is not a target
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null) return false;     // somebody's pet
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        return true;
    }

    /** A boss, by Tensura's or the common boss tag, or anything with a boss-sized health bar: no spell of ours bends it. */
    static boolean boss(LivingEntity e) {
        return e.getType().is(Tags.EntityTypes.BOSSES) || e.getMaxHealth() >= Wave26Rules.DOMINATE_MAX_HEALTH;
    }

    /** The saving throw as Tensura existence (see {@link Wave26Rules#epSaves}). */
    static boolean epSaves(ServerPlayer p, LivingEntity t) {
        try {
            return Wave26Rules.epSaves(EnergyHelper.getMaxEP(t), EnergyHelper.getMaxEP(p));
        } catch (RuntimeException | LinkageError noEnergy) {
            return false;
        }
    }

    static boolean friend(ServerPlayer p, LivingEntity e) {
        return e == p || (e.isAlive() && !e.isSpectator() && (e.isAlliedTo(p) || e.getPersistentData().hasUUID(OWNER_KEY)
                || (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID()))));
    }

    static boolean charmed(LivingEntity e, long now) {
        return e.getPersistentData().hasUUID(CHARM_OWNER) && now < e.getPersistentData().getLong(CHARM_UNTIL);
    }

    /** Living things within a radius of a point (at most 24, nearest first) that pass the test. */
    static List<LivingEntity> near(ServerLevel level, Vec3 c, double r, Predicate<LivingEntity> test) {
        List<LivingEntity> out = level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(c) <= r * r && test.test(e));
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        return out.size() > 24 ? new ArrayList<>(out.subList(0, 24)) : out;
    }

    // ---------------------------------------------------------------- aiming
    record Aim(LivingEntity hit, Vec3 end) {}

    /** Where a beam from the eyes ends and whom it hits first among those that pass the test (null if nobody): stops at the first solid block. */
    static Aim aim(ServerPlayer p, double range, Predicate<LivingEntity> test) {
        Vec3 start = p.getEyePosition(), end = start.add(p.getViewVector(1f).scale(range));
        HitResult wall = p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, start, end,
                p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), e -> e instanceof LivingEntity le && le != p && le.isAlive() && !le.isSpectator() && test.test(le));
        if (hit != null) return new Aim((LivingEntity) hit.getEntity(), hit.getLocation());
        return new Aim(null, end);
    }

    /** The creature a beam or a nearby foe offers: whoever the wearer looks at, or else the nearest foe within the range. */
    static LivingEntity pick(ServerPlayer p, double range) {
        Aim a = aim(p, range, e -> target(p, e));
        if (a.hit() != null) return a.hit();
        List<LivingEntity> foes = Fx.foesAround(p, range);
        return foes.isEmpty() ? null : foes.get(0);
    }

    /** The friendly creature a touch reaches (looked at within 4.5 blocks), or the caster. */
    static LivingEntity touchedFriend(ServerPlayer p) {
        Aim a = aim(p, 4.5, e -> friend(p, e) || e instanceof Player);
        return a.hit() != null ? a.hit() : p;
    }

    // ---------------------------------------------------------------- what a player carries
    static final int C_FRANK = 1, C_HELM = 2, C_DIFF = 4, C_INDIFF = 8;

    /** A bit mask of the wave's relics the player has on or in the pack AND is attuned to (the curses reach the carrier, worn or not). */
    static int carried(Player p) {
        int mask = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            RelicDef d = Relics.defOf(s);
            if (d == null || !Attune.isMine(s, p)) continue;
            switch (d.id()) {
                case "frank" -> mask |= C_FRANK;
                case "helm_of_whispers" -> mask |= C_HELM;
                case "mask_of_difference" -> mask |= C_DIFF;
                case "mask_of_indifference" -> mask |= C_INDIFF;
                default -> { }
            }
        }
        return mask;
    }

    // ---------------------------------------------------------------- particles and words
    static RukhFx.Alignment align(boolean dark) { return RukhFx.Alignment.of(dark); }

    static void tell(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.displayClientMessage(Component.literal(text).withStyle(colour), true);
    }

    static void tell(ServerPlayer p, String text) { tell(p, text, ChatFormatting.GOLD); }

    /** A line in chat for what a player should read twice. */
    static void say(ServerPlayer p, String text, ChatFormatting colour) {
        if (p != null && text != null) p.sendSystemMessage(Component.literal(text).withStyle(colour));
    }
}
