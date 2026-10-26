package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Veldanava skills put six conditions on a creature that a player must SEE (an icon and a timer), that end by themselves, that a milk bucket or a death clears, and that other
 * code can ask about with one call. They are this mod's own effects, written from scratch under {@code nusmp}; what each one DOES lives in {@link JudgmentEvents} (the rules that need a
 * hook on the game) and here (what an attribute modifier or a tick can do alone). Wire it in NUSMP: call {@link #init()} and {@code EFFECTS.register(modEventBus)}.
 * <ul>
 *   <li><b>Temporal Stutter</b>: time catches. Slower to move and to swing, and for three ticks of every second the body does not move at all.</li>
 *   <li><b>Temporal Debt</b>: the seconds owed. The amplifier is the number of stacks (see {@link TemporalDebt}); the debt is what Final Judgment detonates, and each stack makes the creature
 *       1% frailer to magic (at most 50%). It does nothing else and expires by itself.</li>
 *   <li><b>Event Silence</b>: no skill, spell or grimoire page can be started (the same veto the Spell Seal uses), and no spatial ability can be used.</li>
 *   <li><b>Future Rejection</b>: no healing, no teleport, and no invulnerability frames after a hit: the next blow always lands.</li>
 *   <li><b>Movement Interference</b>: much slower, no jumping, no dashing in the air, and no teleport.</li>
 *   <li><b>Action Lock</b>: cannot attack, use an item, interact or cast; a mob loses its target. It can still walk.</li>
 * </ul>
 */
public final class JudgmentEffects {
    private JudgmentEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> TEMPORAL_STUTTER = EFFECTS.register("temporal_stutter", Stutter::new);

    public static final DeferredHolder<MobEffect, MobEffect> TEMPORAL_DEBT = EFFECTS.register("temporal_debt", () -> new Plain(MobEffectCategory.HARMFUL, 0x6A3FA0));

    public static final DeferredHolder<MobEffect, MobEffect> EVENT_SILENCE = EFFECTS.register("event_silence", () -> new Plain(MobEffectCategory.HARMFUL, 0x2B2F5E));

    public static final DeferredHolder<MobEffect, MobEffect> FUTURE_REJECTION = EFFECTS.register("future_rejection", () -> new Plain(MobEffectCategory.HARMFUL, 0xD8C25A));

    public static final DeferredHolder<MobEffect, MobEffect> MOVEMENT_INTERFERENCE = EFFECTS.register("movement_interference", Interference::new);

    public static final DeferredHolder<MobEffect, MobEffect> ACTION_LOCK = EFFECTS.register("action_lock", ActionLock::new);

    /** 3.14.0 (Israfil): a crushing weight. Falls fast, cannot fly (a flying player is brought down, a flying mob feels the pull). Lasts until its time is up, a milk bucket or death. */
    public static final DeferredHolder<MobEffect, MobEffect> GRAVITY_CRUSH = EFFECTS.register("gravity_crush", Crush::new);

    /** 3.14.0 (Israfil): Nihility Collapse - the mark of standing in the collapsed place. Slower, and no healing (the events read it). */
    public static final DeferredHolder<MobEffect, MobEffect> NIHILITY_COLLAPSE = EFFECTS.register("nihility_collapse", Collapse::new);

    /** 3.19.0 (Hercules): Shattered Armour - the armour and the armour toughness of the creature are gone while it lasts (World-Breaking Strike). */
    public static final DeferredHolder<MobEffect, MobEffect> ARMOR_BREAK = EFFECTS.register("armor_break", Shattered::new);

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    /** True when the creature carries the effect (false if the registry is not ready, so a unit test or an early tick never throws). */
    public static boolean has(LivingEntity e, DeferredHolder<MobEffect, MobEffect> effect) {
        try { return e.hasEffect(effect); } catch (RuntimeException notReady) { return false; }
    }

    /** Silences a creature: the effect (which vetoes skills and spatial moves, see {@link JudgmentEvents}) and the mod's own seal flag, which the grimoire casts and the ultimate casts already read. */
    public static void silence(LivingEntity e, int ticks) {
        if (e.level().isClientSide) return;
        e.addEffect(new MobEffectInstance(EVENT_SILENCE, ticks, 0, false, true, true));
        long until = e.level().getGameTime() + ticks;
        if (e.getPersistentData().getLong("nusmp_sealed_until") < until) e.getPersistentData().putLong("nusmp_sealed_until", until);
    }

    /** An effect with no behaviour of its own (MobEffect's constructor is protected); the events read it. */
    private static final class Plain extends MobEffect {
        Plain(MobEffectCategory category, int color) { super(category, color); }
    }

    /** Time catches: slower, and three ticks of every second the body stands still. */
    private static final class Stutter extends MobEffect {
        Stutter() {
            super(MobEffectCategory.HARMFUL, 0x9FB8E8);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("temporal_stutter_speed"), -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.ATTACK_SPEED, id("temporal_stutter_swing"), -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 20 < 3; }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (e.level().isClientSide) return true;
            var v = e.getDeltaMovement();
            e.setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
            e.hurtMarked = true;                                                                         // tells a player's client, which moves itself
            return true;
        }
    }

    /** Much slower, no jump, and no steering in the air (a dash cannot carry). */
    private static final class Interference extends MobEffect {
        Interference() {
            super(MobEffectCategory.HARMFUL, 0x44C2B0);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("movement_interference_speed"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.JUMP_STRENGTH, id("movement_interference_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 2 == 0; }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (e.level().isClientSide || e.onGround()) return true;
            var v = e.getDeltaMovement();
            e.setDeltaMovement(v.x * 0.6, v.y, v.z * 0.6);
            return true;
        }
    }

    /** Halts every action: the item in use is dropped, a mob forgets who it was fighting. Walking is untouched. */
    private static final class ActionLock extends MobEffect {
        ActionLock() {
            super(MobEffectCategory.HARMFUL, 0xE8E8F0);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("action_lock_speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 4 == 0; }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (e.level().isClientSide) return true;
            if (e.isUsingItem()) e.stopUsingItem();
            if (e instanceof Mob m && m.getTarget() != null) m.setTarget(null);
            return true;
        }
    }

    /** A weight on the body: gravity pulls harder, and anything that flies is pressed down. */
    private static final class Crush extends MobEffect {
        Crush() {
            super(MobEffectCategory.HARMFUL, 0x3A2F4A);
            addAttributeModifier(Attributes.GRAVITY, id("gravity_crush_pull"), 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("gravity_crush_speed"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 4 == 0; }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (e.level().isClientSide) return true;
            if (e instanceof net.minecraft.world.entity.player.Player p) {
                if (p.getAbilities().flying && !p.isCreative() && !p.isSpectator()) {
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                }
                if (p.isFallFlying()) p.stopFallFlying();
            }
            if (!e.onGround()) {
                var v = e.getDeltaMovement();
                e.setDeltaMovement(v.x, Math.min(v.y, 0.0) - 0.12, v.z);
                e.hurtMarked = true;
            }
            return true;
        }
    }

    /** The collapsed place: slower. (No healing there: {@code IsrafilEvents} reads the effect.) */
    private static final class Collapse extends MobEffect {
        Collapse() {
            super(MobEffectCategory.HARMFUL, 0x2A1E44);
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("nihility_collapse_speed"), -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    /** The armour is broken: every point of armour and of toughness is taken away, so a blow lands as if the creature wore nothing. */
    private static final class Shattered extends MobEffect {
        Shattered() {
            super(MobEffectCategory.HARMFUL, 0xB07A2A);
            addAttributeModifier(Attributes.ARMOR, id("armor_break_armor"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.ARMOR_TOUGHNESS, id("armor_break_toughness"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }
}
