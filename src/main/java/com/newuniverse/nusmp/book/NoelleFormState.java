package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.NoelleFormRules.Form;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.grimoire.CanonBook;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Why: the server half of Noelle Silva's armour forms (docs/magic/noelle_forms.md): the armour layer itself is put on and taken off by {@link ArmourCast} / {@link ArmourState} (the armour
 * policy), this class adds what a form needs on top, per player and cheaply:
 * <ul>
 *   <li>the TIER of the look: the character (full quality) look belongs to the bound grimoire of Noelle Silva ({@link #characterTier}); every other Water mage draws the standard tier;</li>
 *   <li>the buffs of the form (Mermaid: water breathing, swift in water, slowed on land; Sea Dragon God and Black Dragon: strength, resistance, speed), refreshed once a second as short hidden
 *       effects so they end by themselves two seconds after the form;</li>
 *   <li>the limits: the Sea Dragon God Form lasts five minutes, the Black Dragon Form ninety seconds and leaves three minutes of exhaustion (slowness and weakness; never hunger) in which it cannot be
 *       entered again; it is also refused while Devil Union (or its aftermath, {@link DevilFormLimit}) is on, because the dragon's scales and the devil's pact do not share a body.</li>
 * </ul>
 * Cost: one persistent-data read per player per tick (the armour key is absent for nearly everyone); the rest runs once a second, and only for a player who wears one of the new forms. No packet is sent
 * from here (the layer's lease of the armour policy is the only traffic), no entity is spawned, no world is scanned.
 */
public final class NoelleFormState {
    private NoelleFormState() {}

    static final String AFTER = "nusmp_noelle_black_after";
    private static boolean registered;

    /** Registers the per-player tick once (called from NUSMP). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        AttributeEvents.playerTick(NoelleFormState::tick);
    }

    // ------------------------------------------------------------------ who
    /** The caster's grimoire is the canon book of Noelle Silva: the full-quality look and the two dragon forms. */
    public static boolean characterTier(ServerPlayer p) {
        try { return CharacterSpells.characterOf(p) == CanonBook.NOELLE; } catch (RuntimeException | LinkageError e) { return false; }
    }

    /** The form of the Water armour the player wears right now (null when it is not one of the three new forms). */
    public static Form worn(ServerPlayer p) {
        ArmourRules.Held h = ArmourState.held(p);
        if (h == null || h.form() || !"WATER".equals(h.magic())) return null;
        Form f = NoelleFormRules.form(h.style());
        return f.ordinal() >= Form.MERMAID.ordinal() ? f : null;
    }

    // ------------------------------------------------------------------ entering
    /** Black Dragon Form: refused (true, with a message) during its own exhaustion, during Devil Union and during the aftermath of a broken union. */
    public static boolean refuseBlackDragon(ServerPlayer p) {
        if (NoelleFormRules.blocked(p.getPersistentData().getLong(AFTER), p.level().getGameTime()) && !p.isCreative()) {
            GrimoireBook.fail(p, "The dragon's exhaustion still holds you. The black scales will not close yet.");
            return true;
        }
        if (GrimoireBook.inDevilState(p)) {
            GrimoireBook.fail(p, "A devil's pact is on you. The black scales will not share your body with it.");
            return true;
        }
        return DevilFormLimit.refuse(p);
    }

    // ------------------------------------------------------------------ the tick
    private static void tick(ServerPlayer p) {
        CompoundTag t = p.getPersistentData();
        if (!t.contains(ArmourState.KEY, 10) || p.tickCount % 20 != 3) return;                      // nearly everyone stops here: no armour, or not this tick of the second
        Form f = worn(p);
        if (f == null) return;
        long now = p.level().getGameTime(), since = t.getCompound(ArmourState.KEY).getLong("T");
        if (NoelleFormRules.expired(f, since, now)) {
            ArmourState.takeOff(p);
            if (f == Form.BLACK_DRAGON) exhaust(p, now);
            else p.displayClientMessage(Component.translatableWithFallback("nusmp.noelle.limit", "The form ends.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean water = p.isInWaterOrBubble();
        for (NoelleFormRules.Buff b : NoelleFormRules.buffs(f)) {
            if (b.waterOnly() && !water || b.landOnly() && water) continue;
            Holder<MobEffect> e = effect(b.effect());
            if (e != null) p.addEffect(new MobEffectInstance(e, 60, b.amplifier(), true, false, false));
        }
    }

    /** The Black Dragon Form ran its course: slowness and weakness (never hunger) and no re-entry for the aftermath. Creative players pay nothing. */
    static void exhaust(ServerPlayer p, long now) {
        if (p.isCreative()) { p.displayClientMessage(Component.translatableWithFallback("nusmp.noelle.black_end", "The black scales crumble.").withStyle(ChatFormatting.GRAY), true); return; }
        int ticks = (int) NoelleFormRules.BLACK_AFTERMATH_TICKS;
        p.getPersistentData().putLong(AFTER, now + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        p.displayClientMessage(Component.translatableWithFallback("nusmp.noelle.black_exhausted", "The black scales crumble. The dragon leaves you hollow.").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), true);
    }

    private static Holder<MobEffect> effect(String name) {
        return switch (name) {
            case "water_breathing" -> MobEffects.WATER_BREATHING;
            case "dolphins_grace" -> MobEffects.DOLPHINS_GRACE;
            case "regeneration" -> MobEffects.REGENERATION;
            case "movement_slowdown" -> MobEffects.MOVEMENT_SLOWDOWN;
            case "damage_boost" -> MobEffects.DAMAGE_BOOST;
            case "damage_resistance" -> MobEffects.DAMAGE_RESISTANCE;
            case "movement_speed" -> MobEffects.MOVEMENT_SPEED;
            case "night_vision" -> MobEffects.NIGHT_VISION;
            default -> null;
        };
    }
}
