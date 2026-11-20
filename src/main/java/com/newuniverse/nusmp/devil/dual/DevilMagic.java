package com.newuniverse.nusmp.devil.dual;

import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilStore;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the one place that says "this is devil magic" (the owner's rule: Adaptability never adapts to it and the law-domination effect has no effect on it). A blow is devil magic when its damage type
 * is in the tag {@code anime_multiverse:devil_magic} (the arts of the skill Devil Magic hurt with the type {@code anime_multiverse:devil_magic}, and a data pack or another mod can add its own types to the
 * tag), or when the player who struck has let a devil in. Adaptability asks {@link #adaptationBlocked}; any rule that removes, bars or rewrites what hits a body asks {@link #lawDominationApplies} first.
 * The devil effects the arts put on a foe are remembered on the foe for as long as they last ({@link #markEffect}), so a rule that strips effects can ask {@link #isDevilEffect} per effect.
 * The decision itself is the pure {@link DevilMagicRules}. Cost: a tag lookup and one attachment lookup per question; one long per effect id on the foe (at most 16).
 *
 * <p>Law Domination itself is not in this mod: it is a skill of third-party Tensura addons (it removes and bars effects from its holder). It can not be edited from here, so the immunity is this API and the
 * tag, which such a mechanic (or a mixin of it) consults.
 */
public final class DevilMagic {
    private DevilMagic() {}

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "devil_magic");
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ID);
    public static final TagKey<DamageType> DAMAGE_TAG = TagKey.create(Registries.DAMAGE_TYPE, ID);
    private static final String FX = "nusmp_devil_fx";
    private static final int MAX_FX = 16;

    /** The damage source of an art: the devil_magic type with the caster as the cause, or plain indirect magic if the type is missing (a broken data pack). */
    public static DamageSource source(ServerPlayer p) {
        return p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(DAMAGE)
                .<DamageSource>map(h -> new DamageSource(h, null, p))
                .orElseGet(() -> p.damageSources().indirectMagic(p, p));
    }

    /** Has the entity who struck let a devil in (so its blows are devil magic)? */
    public static boolean hostCarries(Entity attacker) {
        if (!(attacker instanceof ServerPlayer p)) return false;
        DevilContract c = DevilStore.peek(p);
        return DevilMagicRules.hostCarries(DevilConfig.enabled(), c != null && c.hasContract(), c == null ? 0f : c.assimilation);
    }

    public static boolean isDevilMagic(DamageSource s) {
        return s != null && DevilMagicRules.isDevilMagic(s.is(DAMAGE_TAG), hostCarries(s.getEntity()));
    }

    /** True when Adaptability must neither learn from this blow nor soften it. */
    public static boolean adaptationBlocked(DamageSource s) { return !DevilMagicRules.mayAdaptTo(isDevilMagic(s), DualConfig.immuneAdaptability()); }

    /** False for devil magic while the setting is on: a law-domination effect must leave this blow alone. */
    public static boolean lawDominationApplies(DamageSource s) { return DevilMagicRules.lawDominationApplies(isDevilMagic(s), DualConfig.immuneLaw()); }

    // ---------------------------------------------------------------- time
    /** The origins of the player's Time magic: the TIME grimoire (Julius) and/or the devil attribute TIME. */
    public static TimeOrigin timeOrigin(ServerPlayer p, DevilContract c) {
        boolean g;
        try {
            g = com.newuniverse.nusmp.blackclover.GrimoirePages.grimoireOf(p).map(com.newuniverse.nusmp.blackclover.GrimoirePages::magicOf).filter(m -> m == com.newuniverse.nusmp.blackclover.MagicType.TIME).isPresent();
        } catch (RuntimeException noSkills) {
            g = false;
        }
        return TimeOrigin.of(g, c != null && c.hasContract() && DualRules.isTime(c.attribute));
    }

    // ---------------------------------------------------------------- effects
    /** Remembers that the effect {@code effectId} on this entity is devil magic until {@code untilTick} (game time). */
    public static void markEffect(LivingEntity target, String effectId, long untilTick) {
        if (target == null || effectId == null || effectId.isEmpty()) return;
        CompoundTag root = target.getPersistentData();
        CompoundTag fx = root.getCompound(FX);
        if (fx.size() >= MAX_FX && !fx.contains(effectId)) fx = new CompoundTag();
        fx.putLong(effectId, untilTick);
        root.put(FX, fx);
    }

    /** Is this effect on the entity one the devil put there (and still running)? */
    public static boolean isDevilEffect(LivingEntity target, String effectId) {
        if (target == null || effectId == null) return false;
        CompoundTag fx = target.getPersistentData().getCompound(FX);
        return fx.contains(effectId) && fx.getLong(effectId) > target.level().getGameTime();
    }

    /** False for a devil effect while the setting is on: a law-domination effect must not remove or bar it. */
    public static boolean lawDominationApplies(LivingEntity target, String effectId) {
        return DevilMagicRules.lawDominationApplies(isDevilEffect(target, effectId), DualConfig.immuneLaw());
    }
}
