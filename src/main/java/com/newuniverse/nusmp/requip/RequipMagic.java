package com.newuniverse.nusmp.requip;

import com.newuniverse.nusmp.skill.ErzaRequipMagic;
import com.newuniverse.nusmp.skill.RequipMagicSelfTests;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Why: the one entry point of the Equip Magic (the pocket Armoury, Loadouts, the weapon modes, Partial Armour, the finishers) that {@link RequipSystem#init} calls: it registers the saved data
 * (the {@code nusmp:requip_armoury} attachment), the armoury window and its key packet, the server config, the skill-side listeners and the in-game self-test {@code skill.requip_magic}, and on the
 * client the screen and the key. The skill is still {@code skill.ErzaRequipSkill}; the new modes are appended after Dismiss.
 */
public final class RequipMagic {
    private RequipMagic() {}

    private static boolean started;

    /** Called once from the mod constructor (through {@link RequipSystem#init}), before the registers are frozen. */
    public static void init(IEventBus modBus) {
        if (started) return;
        started = true;
        RequipArmoury.ATTACHMENTS.register(modBus);
        RequipArmoury.MENUS.register(modBus);
        modBus.addListener(RequipArmoury::registerPayloads);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, RequipMagicConfig.SPEC, "nusmp-requip-server.toml");
        ErzaRequipMagic.init(modBus);
        RequipMagicSelfTests.register();
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.requip.RequipArmouryClient.init(modBus);
    }
}
