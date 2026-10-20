package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.NUConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 1.99.0: the strict limit of Devil Union, the FORM of Anti-Magic (Asta and the devil Liebe; the mod's Black Form, {@link AntiMagicBook#setForm}). After the owner's canon: the union
 * costs no mana (the Aura gauge of antimagic.aura is untouched), the body is coated in Anti-Magic, and it lasts at most five minutes (config {@code armour.devilFormLimitSeconds});
 * then the form shatters and the user is incapacitated by intense muscle pain for about thirty minutes (config {@code armour.devilFormAftermathSeconds}) and can not enter the union
 * again meanwhile. The pain is slowness, weakness and mining fatigue: never hunger, never anything that touches the food bar. Ending the form on purpose costs nothing.
 *
 * <p>The state is two numbers in the player's persistent data (when the union began, when the aftermath ends); neither survives death, which clears the debuffs too.
 */
public final class DevilFormLimit {
    private DevilFormLimit() {}

    static final String SINCE = "nusmp_devil_form_since", AFTER = "nusmp_devil_form_after_until";

    private static int limitSeconds() {
        try { return NUConfig.DEVIL_FORM_LIMIT_SECONDS.get(); } catch (RuntimeException | LinkageError e) { return ArmourRules.DEVIL_LIMIT_SECONDS; }
    }

    private static int aftermathSeconds() {
        try { return NUConfig.DEVIL_FORM_AFTERMATH_SECONDS.get(); } catch (RuntimeException | LinkageError e) { return ArmourRules.DEVIL_AFTERMATH_SECONDS; }
    }

    /** True (with a message) while the aftermath of a broken union lasts: the user can not enter the union. */
    public static boolean refuse(ServerPlayer p) {
        if (!ArmourRules.devilBlocked(p.getPersistentData().getLong(AFTER), p.level().getGameTime())) return false;
        GrimoireBook.fail(p, "Your muscles still burn from the last union. The devil will not answer yet.");
        return true;
    }

    /** The union began. */
    public static void begin(ServerPlayer p) { p.getPersistentData().putLong(SINCE, Math.max(1, p.level().getGameTime())); }

    /** The union ended on purpose or because the Aura ran dry: no price. */
    public static void clear(ServerPlayer p) { p.getPersistentData().remove(SINCE); }

    /** Has the union lasted the limit? */
    public static boolean expired(ServerPlayer p) {
        return ArmourRules.devilExpired(p.getPersistentData().getLong(SINCE), p.level().getGameTime(), ArmourRules.devilLimitTicks(limitSeconds()));
    }

    /** The limit broke the union: the price is paid (muscle pain, no hunger). The caller has already switched the form off. */
    public static void shatter(ServerPlayer p) {
        clear(p);
        long ticks = ArmourRules.devilAftermathTicks(aftermathSeconds());
        if (ticks <= 0 || p.isCreative()) {
            p.displayClientMessage(Component.literal("The union breaks.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        p.getPersistentData().putLong(AFTER, p.level().getGameTime() + ticks);
        int t = (int) Math.min(Integer.MAX_VALUE / 2, ticks);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 2));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, t, 2));
        p.displayClientMessage(Component.literal("The union shatters. Every muscle screams.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }
}
