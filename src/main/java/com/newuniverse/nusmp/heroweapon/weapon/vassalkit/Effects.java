package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Why: four of the five weapons take status effects off a creature or copy them from it (the Mirror's rebound cleanses and its Envy copies, the Musical
 * Instrument's Zero Note cleanses allies, the Harpoon's Zero strips a foe's helpful effects, the Sewing Set's mending cures one), and every one must do it the same
 * safe way: strongest effects first, a fixed maximum, an effect with no end ({@code duration -1}) given a real length when it is copied, and the copy taken BEFORE
 * the original is removed. One helper keeps those rules in one place. Server thread only.
 */
public final class Effects {
    private Effects() {}

    /** How strong an effect is for sorting: amplifier first, then time left (an infinite effect counts as very long). */
    private static int rank(MobEffectInstance i) {
        return i.getAmplifier() * 10_000 + (i.isInfiniteDuration() ? 9_999 : Math.min(i.getDuration(), 9_999));
    }

    /** The live effects of {@code e} that are helpful ({@code beneficial}) or harmful (otherwise), strongest first. Instant effects are skipped. */
    public static List<MobEffectInstance> strongest(LivingEntity e, boolean beneficial) {
        List<MobEffectInstance> out = new ArrayList<>();
        for (MobEffectInstance inst : e.getActiveEffects()) {
            MobEffect kind = inst.getEffect().value();
            if (kind.isInstantenous()) continue;
            if (beneficial ? kind.isBeneficial() : kind.getCategory() == MobEffectCategory.HARMFUL) out.add(inst);
        }
        out.sort(Comparator.comparingInt((MobEffectInstance i) -> -rank(i)));
        return out;
    }

    /** Removes up to {@code max} of the strongest helpful or harmful effects and returns copies of what was removed. */
    public static List<MobEffectInstance> strip(LivingEntity e, boolean beneficial, int max) {
        List<MobEffectInstance> taken = new ArrayList<>();
        for (MobEffectInstance inst : strongest(e, beneficial)) {
            if (taken.size() >= max) break;
            taken.add(new MobEffectInstance(inst));
            e.removeEffect(inst.getEffect());
        }
        return taken;
    }

    /** The length to give a copy of an effect: what is left, but at most {@code cap}, and a real length for one that never ends. */
    public static int ticks(MobEffectInstance i, int cap) {
        return Math.max(1, i.isInfiniteDuration() ? cap : Math.min(i.getDuration(), cap));
    }

    /** A mob effect by registry id ("tensura", "silence"), if the game has it (an optional mod may be absent); empty otherwise. */
    public static Optional<Holder<MobEffect>> named(String namespace, String path) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath(namespace, path)).map(h -> (Holder<MobEffect>) h);
    }

    /** The registry namespace of an effect ("minecraft", "tensura" ...), or an empty string. */
    public static String namespace(MobEffectInstance i) {
        return i.getEffect().unwrapKey().map(k -> k.location().getNamespace()).orElse("");
    }

    /** The registry path of an effect ("poison", "slowness" ...), or an empty string. */
    public static String path(MobEffectInstance i) {
        return i.getEffect().unwrapKey().map(k -> k.location().getPath()).orElse("");
    }
}
