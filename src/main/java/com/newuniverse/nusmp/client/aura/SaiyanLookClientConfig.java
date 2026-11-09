package com.newuniverse.nusmp.client.aura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the CLIENT config of the Saiyan form looks ({@code config/nusmp-saiyan-look-client.toml}, 3.69.0): {@code saiyan.looks} (default true) draws the eyes, flame aura, armour and charge-up of a
 * transformed Saiyan; false restores the old look (only the server's motes). {@code saiyan.armour}, {@code saiyan.chargeUp} and {@code saiyan.fullDetail} switch single parts off for slow computers.
 * Looks only: nothing on the server depends on any of them. The values are copied into volatile fields when the config loads or reloads.
 */
public final class SaiyanLookClientConfig {
    private SaiyanLookClientConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LOOKS, ARMOUR, CHARGE_UP, FULL_DETAIL, CHARGE_ANIM;
    private static volatile boolean looks = true, armour = true, chargeUp = true, full = true, chargeAnim = true;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("saiyan");
        LOOKS = b.comment("true: a transformed Saiyan is drawn with its form's glowing eyes, flame aura and power-up. false: only the plain particles of 3.65.0. Looks only.")
                .define("looks", true);
        ARMOUR = b.comment("true: a transformed Saiyan wears the Saiyan battle armour (forged on when the form starts, shattered when it ends). Looks only.")
                .define("armour", true);
        CHARGE_UP = b.comment("true: a form starts with the charge-up (rocks lift, energy streams in, the ground cracks, a column of light and shockwave rings). Looks only.")
                .define("chargeUp", true);
        FULL_DETAIL = b.comment("true: nearby Saiyans also get fur, a tail, lightning and rock chips in the forms that have them. false: the light version everywhere (about a fifth of the quads). Looks only.")
                .define("fullDetail", true);
        CHARGE_ANIM = b.comment("true: the body of a Saiyan powering up is animated (crouch, trembling flex, the snap open at the burst). false: the body keeps its normal pose. Looks only (3.71.0).")
                .define("chargeAnimation", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean looks() { return looks; }
    public static boolean armour() { return armour; }
    public static boolean chargeUp() { return chargeUp; }
    public static boolean fullDetail() { return full; }
    public static boolean chargeAnimation() { return chargeAnim; }

    /** Registers the file and the reload hook (client only; called from NUSMP in the client block). */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-saiyan-look-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
    }

    private static void apply() {
        try {
            looks = LOOKS.get(); armour = ARMOUR.get(); chargeUp = CHARGE_UP.get(); full = FULL_DETAIL.get(); chargeAnim = CHARGE_ANIM.get();
        } catch (IllegalStateException unloaded) {
            looks = true; armour = true; chargeUp = true; full = true; chargeAnim = true;
        }
    }
}
