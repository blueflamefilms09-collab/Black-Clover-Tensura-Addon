package com.newuniverse.nusmp.arc.tier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Items;

/**
 * Why: the end of the Death Note gag is a REAL death, and it must go through the ordinary death flow so keepInventory, respawn, the death screen, the Totem of Undying and every mod's death hook
 * behave exactly as for any other death. The blow is one hit of the custom damage type {@code nusmp:death_note} (data pack entry; its death message is "&lt;player&gt; died of a heart attack"), listed
 * in the vanilla "bypasses armour / effects / enchantments / shield / cooldown" tags so armour and Resistance cannot soften it, and NOT in "bypasses invulnerability", which is the tag that would
 * let a death skip the Totem. Creative and spectator players are never hit (the boss and {@link LightScene} check before this is called, and it checks again).
 */
public final class LightDeathNote {
    private LightDeathNote() {}

    /** What the blow did. */
    public enum Result { KILLED, SAVED_BY_TOTEM, RESISTED, SKIPPED }

    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "death_note"));

    /** The damage source (plain magic damage if the data pack entry is missing, so a broken pack never crashes the scene). */
    public static DamageSource source(ServerPlayer victim) {
        return victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(TYPE)
                .<DamageSource>map(DamageSource::new)
                .orElseGet(() -> victim.damageSources().magic());
    }

    /** Is the player one the Death Note may write? Survival or adventure, alive, not spectating. */
    public static boolean eligible(ServerPlayer p) { return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() && !p.hasDisconnected(); }

    /** The blow itself. A Totem of Undying in either hand saves the player once, as for any lethal damage. */
    public static Result strike(ServerPlayer victim) {
        if (!eligible(victim)) return Result.SKIPPED;
        boolean totem = victim.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || victim.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        victim.hurt(source(victim), Float.MAX_VALUE);
        if (!victim.isAlive() || victim.isDeadOrDying()) return Result.KILLED;
        return totem ? Result.SAVED_BY_TOTEM : Result.RESISTED;
    }
}
