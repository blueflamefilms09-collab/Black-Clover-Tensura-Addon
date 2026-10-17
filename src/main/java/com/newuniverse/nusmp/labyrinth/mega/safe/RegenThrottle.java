package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: "Players cannot passively regenerate health/mana easily; they must secure and defend Safe Zones". In the Mega-Dungeon, outside a Landing or
 * a Safe Zone, natural health regeneration and Tensura's magicule and aura regeneration are scaled by {@code passiveRegenScale} (default a
 * quarter), and inside a safe room regeneration is vanilla, so a Landing is a place to catch your breath even before you rest. Health goes
 * through {@code LivingHealEvent} with the heuristic documented in {@link RegenMath} (vanilla's natural regeneration heals in steps of at most
 * one point and the Regeneration effect is exempt; bigger heals such as potions, spells and rests are not touched). Mana cannot be intercepted
 * the same way, so a TRANSIENT attribute modifier ({@code ADD_MULTIPLIED_TOTAL}, {@code scale - 1}, a fixed id) is put on
 * {@code MAGICULE_REGENERATION_MULTIPLIER} and {@code AURA_REGENERATION_MULTIPLIER} while the player is in the dimension outside a safe zone, and
 * taken off when they enter a safe zone, leave the dimension or change the config; a transient modifier is not saved, so a crash or a logout can
 * never leave a player throttled in the Overworld. The check runs once a second, not per tick. Creative and spectator players are exempt.
 */
public final class RegenThrottle {
    private RegenThrottle() {}

    static final ResourceLocation MAGICULE_MODIFIER = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "mega_regen_magicule");
    static final ResourceLocation AURA_MODIFIER = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "mega_regen_aura");

    /** Scales a natural-sized heal of a player outside the safe zones of the Mega-Dungeon; cancels it when it rounds to nothing. */
    public static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        float before = e.getAmount();
        float after = scaledHeal(p, before);
        if (after == before) return;
        if (after <= 0f) e.setCanceled(true);
        else e.setAmount(after);
    }

    /**
     * What a heal of {@code amount} should come to for this player right now: scaled when they are in the Mega-Dungeon, outside a Landing or Safe Zone,
     * not exempt and not under the Regeneration effect (and the heal is natural-sized), else unchanged; 0 means "cancel". The heal event calls this, and
     * so do the in-game self-tests.
     */
    public static float scaledHeal(ServerPlayer p, float amount) {
        ServerLevel level = p.serverLevel();
        if (!Labyrinth.inMega(level)) return amount;
        double scale = SafeConfig.get(SafeConfig.PASSIVE_REGEN_SCALE);
        if (scale >= 1.0) return amount;                                                       // vanilla: nothing to look up
        boolean exempt = p.isCreative() || p.isSpectator();
        boolean zone = !exempt && MegaDungeon.inSafeZone(level, p.blockPosition());
        if (!RegenMath.applies(true, zone, exempt, scale)) return amount;
        return RegenMath.scaleHeal(amount, scale, p.hasEffect(MobEffects.REGENERATION));
    }

    /** Once a second per player: puts the regeneration modifiers on or takes them off. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;       // offset from the other once-a-second handlers
        refresh(p);
    }

    /** Puts the magicule and aura regeneration modifiers on {@code p} when the throttle applies to them now, and takes them off when it does not. */
    public static void refresh(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        double scale = SafeConfig.get(SafeConfig.PASSIVE_REGEN_SCALE);
        boolean inMega = Labyrinth.inMega(level);
        boolean exempt = p.isCreative() || p.isSpectator();
        boolean zone = inMega && MegaDungeon.inSafeZone(level, p.blockPosition());
        boolean on = RegenMath.applies(inMega, zone, exempt, scale);
        double amount = RegenMath.modifierAmount(scale);
        set(p, TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, MAGICULE_MODIFIER, on, amount);
        set(p, TensuraAttributes.AURA_REGENERATION_MULTIPLIER, AURA_MODIFIER, on, amount);
    }

    /** Leaving the dimension takes the modifiers off at once, not up to a second later. */
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        set(p, TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, MAGICULE_MODIFIER, false, 0.0);
        set(p, TensuraAttributes.AURA_REGENERATION_MULTIPLIER, AURA_MODIFIER, false, 0.0);
    }

    /** Is the throttle's modifier on the player right now (for the admin command)? */
    public static boolean throttled(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);
        return inst != null && inst.hasModifier(MAGICULE_MODIFIER);
    }

    private static void set(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id, boolean on, double amount) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;                                                              // an entity type Tensura gave no such attribute: nothing to throttle
        AttributeModifier current = inst.getModifier(id);
        if (!on) {
            if (current != null) inst.removeModifier(id);
            return;
        }
        if (current != null && current.amount() == amount && current.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
