package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.MagicType.HitEffect;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Everything a magic needs, with a SAFE DEFAULT for every part, so a magic that defines only some of it (or nothing) still casts, draws
 * and shows up. Only {@link #id()} is abstract. Every default is non-null and never throws; the registry ({@link MagicProfiles}) hands
 * one out for any {@link MagicType}, any id string, and any merged magic.
 *
 * <p>How to use it: call {@code MagicProfiles.of(type)} (or {@code type.profile()}) and read what you need; never switch over the enum
 * for presentation data. To give a magic something special, register a {@link ProfileOverride} for it; the rest keeps its defaults.</p>
 */
public interface MagicProfile {
    /** The enum constant name ("FLAME") or a merged id ("merged:flame+water"). */
    String id();

    // ---------------------------------------------------------------- names and rules
    default String displayName() { return MagicKeys.prettify(id()) + (isMerged() ? "" : " Magic"); }

    /** The prefix of spell names ("Flame" -> "Flame Bullet"). */
    default String word() { return MagicKeys.prettify(id()); }

    /** TR Nightmare soul family ("FLAME", ..., "EMPTY"). */
    default String soulType() { return "EMPTY"; }

    /** What a hit does besides damage. PIERCE is the no-rider value. */
    default HitEffect hit() { return HitEffect.PIERCE; }

    /** A second effect that rides on hits (merged magics); null = none. */
    default HitEffect secondaryHit() { return null; }

    /** Never rolled for a soul (devil, command or admin only). By name, see {@link MagicKeys#RESTRICTED}. */
    default boolean restricted() { return MagicKeys.isRestricted(id()); }

    default boolean isMerged() { return MagicKeys.isMerged(id()); }

    /** The enum names of the parents of a merged magic, in canonical order; empty for a plain magic. */
    default List<String> parentIds() { return List.of(); }

    // ---------------------------------------------------------------- look
    /** The magic's main colour, ARGB. */
    default int color() { return 0xFF000000 | com.newuniverse.nusmp.grimoire.BookPalette.glow(MagicKeys.enumKey(id())); }

    /** A particle that exists in every game version of the mod's target; the final fallback is GLOW. */
    default SimpleParticleType particle() { return ParticleTypes.GLOW; }

    /** The effect the magic's guard page grants; the final fallback is Resistance. */
    default Holder<MobEffect> guardEffect() { return MobEffects.DAMAGE_RESISTANCE; }

    default VfxStyle vfxStyle() { return MagicShapes.GENERIC.style(); }

    /** The three effects of the magic (bolt / trail, zone, impact); never null. The generic ones are the wiki layers. */
    default VfxShape castShape() { return MagicShapes.GENERIC.cast(); }
    default VfxShape fieldShape() { return MagicShapes.GENERIC.field(); }
    default VfxShape burstShape() { return MagicShapes.GENERIC.burst(); }

    /** The player render layer slot this magic has, or null (then casting never asks for one). */
    default Aura auraStyle() { return null; }

    /** Texture path inside assets/nusmp of the icon (it may not exist: use {@link #icon()}, which is checked by the audit). */
    default String iconPath() { return MagicKeys.iconPath(id()); }

    default ResourceLocation icon() { return ResourceLocation.fromNamespaceAndPath("nusmp", iconPath()); }

    // ---------------------------------------------------------------- safe numbers (used when a page does not state its own)
    /** Cost of a cast as a percent of max magicule. */
    default double safeCostPercent() { return 8; }
    default double safeCostFloor() { return 150; }
    /** Cooldown in ticks. */
    default int safeCooldownTicks() { return 80; }

    // ---------------------------------------------------------------- fallback
    /**
     * What happens instead of a cast that failed inside this magic's own code: a burst of the magic's colour at the aim point. It does
     * no damage and never throws. Returns true (the cast is spent: mana, cooldown) so the player is not stuck.
     */
    default boolean fallbackCast(ServerPlayer player) { return GenericFallbackFx.play(player, this); }
}
