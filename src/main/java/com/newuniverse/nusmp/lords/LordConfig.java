package com.newuniverse.nusmp.lords;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: whether the four Mortal Sin weapons are one-in-the-world, how their drawback and decree work and how strong they hit change gameplay, so they are SERVER settings ({@code nusmp-lords-server.toml}, registered by
 * {@link LordBoot}). {@code enabled = false} restores the old behaviour exactly: the weapons are the soul-bound-by-carrier items of {@code sinweapons} (many may exist, no forms, no burden, no mastery). The look of the
 * effects is a client matter ({@code nusmp-lords-client.toml}).
 */
public final class LordConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, BURDEN, INACTIVITY_RELEASE, HUNGER_OF_THE_BLADE, NEW_VFX;
    public static final ModConfigSpec.IntValue DAMAGE_PERCENT, BURDEN_PERCENT, MASTERY_PERCENT, HALF_UNLOCK_PERCENT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("lords");
        ENABLED = b.comment("The Lords of the Mortal Sins: the Black Axe of Sloth, Gunblade of Envy, Spear of Vanity and the holy sword Seifort are ONE in the world each, soul-bound, with three forms, a mastery and a Lord's decree. false = the old behaviour (carrier-bound, many may exist, one form).")
                .define("lordsEnabled", true);
        BURDEN = b.comment("The Burden: using a Lord weapon builds its named drawback (Weight of Sloth, Hollow of Envy, Cracked Mirror, Penance), which strains the wielder from 50 and overloads at 100 for 6 s. It never touches the food bar and always recovers. false = no drawback.")
                .define("burden", true);
        INACTIVITY_RELEASE = b.comment("A wielder who has not been online for 14 days loses the weapon (it leaves the world and may enter again). false = it waits for ever.").define("inactivityRelease", true);
        HUNGER_OF_THE_BLADE = b.comment("The Black Sword Greed's starvation is the Hunger of the Blade: below 20 satiation the blade is Ravenous (heals you less, drinks 1.5% of your magicule a second, lends 70% of its bonuses), at 0 it is Dormant (its bonuses sleep until it is fed). It never damages you, never abandons you and never touches the food bar. false = the old starvation (magic damage and abandonment).")
                .define("hungerOfTheBlade", true);
        NEW_VFX = b.comment("true: the arts, the decrees and the states of the Lord weapons and of Greed's hunger are drawn by their own effect (one small effect packet each, drawn by the client). false = only the old particle puffs. Looks only; the client setting lords.classicVfx also hides them for one player.")
                .define("themedVfx", true);
        DAMAGE_PERCENT = b.comment("Strength of the arts and decrees in percent (100 = as listed).").defineInRange("damagePercent", 100, 10, 1000);
        BURDEN_PERCENT = b.comment("How fast the burden builds, in percent (100 = as listed).").defineInRange("burdenPercent", 100, 0, 1000);
        MASTERY_PERCENT = b.comment("How fast the mastery tracks grow, in percent (100 = as listed; the Half-Starved Mastery of Greed too).").defineInRange("masteryPercent", 100, 0, 1000);
        HALF_UNLOCK_PERCENT = b.comment("Half-Starvation (the second mode of Lord of Gluttonous Greed) can be switched ON once the skill's own mastery reaches this many percent (canon: Fate had unlocked half of his Gluttony skill). Switching it OFF is always allowed. 0 = always available, as before.")
                .defineInRange("halfStarvedUnlockPercent", 50, 0, 100);
        b.pop();
        SPEC = b.build();
    }

    private LordConfig() {}

    public static boolean enabled() { return ENABLED.get(); }
    public static boolean burden() { return BURDEN.get(); }
    public static boolean hungerOfTheBlade() { return HUNGER_OF_THE_BLADE.get(); }
    public static boolean themedVfx() { return NEW_VFX.get(); }
    public static double damage() { return DAMAGE_PERCENT.get() / 100.0; }
    public static double burdenScale() { return BURDEN_PERCENT.get() / 100.0; }
    public static int halfUnlockPercent() { return HALF_UNLOCK_PERCENT.get(); }
    public static double masteryScale() { return MASTERY_PERCENT.get() / 100.0; }
}
