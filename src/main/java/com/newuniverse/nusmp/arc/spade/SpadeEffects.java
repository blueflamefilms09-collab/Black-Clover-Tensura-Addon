package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Spade bosses take away three things that a vanilla potion effect cannot take away cleanly, and a player must SEE what has been done to them
 * (an icon and a timer), be able to learn from it, and have it end by itself if the boss dies or the server stops. A real effect carries its attribute
 * modifiers (jump strength, gravity, speed), shows on the victim's screen, is cleared by the same things that clear any effect, and can never be left
 * behind on a player the way a hand-applied modifier could. Wire it in NUSMP: call {@link #init()} and {@code EFFECTS.register(modEventBus)}.
 * <ul>
 *   <li><b>Flattened Air</b> (Lucifero's Flatten the Air): the air is pressed flat. No jumping (jump strength -100%) and twice the gravity, so a fall is fast
 *       and a gliding player is brought down. Walking is untouched: the lanes and rings of the fight are meant to be walked out of.</li>
 *   <li><b>Kneeling</b> (a Bow not obeyed, Kneel Before the King, the grasp): on the knees. No walking, no jumping, no sideways drift. It lasts as long
 *       as the Balance Law allows a hard control (three seconds on a player at most) and is never put on a holder of anti-magic.</li>
 *   <li><b>Specimen</b> (Morris's Specimen Mark): informs, and slows a little. It marks the one the thread is drawn to, so the party knows who to
 *       free (an ally standing on the thread cuts it).</li>
 * </ul>
 */
public final class SpadeEffects {
    private SpadeEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    /** No jumping, double gravity: the air is pressed flat. */
    public static final DeferredHolder<MobEffect, MobEffect> FLATTENED_AIR = EFFECTS.register("flattened_air", () -> new Plain(MobEffectCategory.HARMFUL, 0x7A5CC8)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, id("flattened_air_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.GRAVITY, id("flattened_air_gravity"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** On the knees: cannot walk, jump or drift. */
    public static final DeferredHolder<MobEffect, MobEffect> KNEELING = EFFECTS.register("kneeling", Kneeling::new);

    /** Marked by Morris's thread. */
    public static final DeferredHolder<MobEffect, MobEffect> SPECIMEN = EFFECTS.register("specimen", () -> new Plain(MobEffectCategory.HARMFUL, 0xFF9B58)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("specimen_speed"), -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    /** An effect with nothing but attribute modifiers (MobEffect's constructor is protected). */
    private static final class Plain extends MobEffect {
        Plain(MobEffectCategory category, int color) { super(category, color); }
    }

    /** The knees: attribute modifiers cannot stop the sideways drift of a knock, so the tick takes it away too. */
    private static final class Kneeling extends MobEffect {
        Kneeling() {
            super(MobEffectCategory.HARMFUL, 0x3E2A66);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("kneeling_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.JUMP_STRENGTH, id("kneeling_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            var v = e.getDeltaMovement();
            e.setDeltaMovement(v.x * 0.2, Math.min(0.0, v.y), v.z * 0.2);
            return true;
        }
    }
}
