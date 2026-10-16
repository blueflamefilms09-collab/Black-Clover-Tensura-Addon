package com.newuniverse.nusmp.quests;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: two behaviours of the quest side are the operator's call: whether the bundled questline installs itself the first time a server
 * starts without any quest folder, and whether a boss kill or arena victory counts for the whole FTB team around the killer. Own spec file
 * ({@code nusmp-quests-server.toml}, SERVER); the lead registers it with
 * {@code modContainer.registerConfig(ModConfig.Type.SERVER, QuestsConfig.SPEC, "nusmp-quests-server.toml")}. Plain {@code define} only;
 * reads fall back to the defaults before the config is loaded.
 */
public final class QuestsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue AUTO_INSTALL_ON_FIRST_RUN;
    public static final ModConfigSpec.BooleanValue SHARE_SQUAD_CREDIT;
    public static final ModConfigSpec.IntValue SHARE_RADIUS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("quests");
        AUTO_INSTALL_ON_FIRST_RUN = b.comment("When FTB Quests is installed and the folder config/ftbquests/quests does not exist at all, install the bundled",
                        "Multiverse of Anime questline on server start. Never touches an existing quest folder. (Use /nusmp quests install for the rest.)")
                .define("autoInstallOnFirstRun", false);
        SHARE_SQUAD_CREDIT = b.comment("A boss defeat or arena victory is also reported for the online members of the killer's FTB team who are close by,",
                        "so a squad kill counts for the whole squad's advancements and quests. false = only the killer.")
                .define("shareSquadCredit", true);
        SHARE_RADIUS = b.comment("How close (blocks, same dimension) a team member must be to share the credit.")
                .defineInRange("shareSquadCreditRadius", 128, 8, 1024);
        b.pop();
        SPEC = b.build();
    }

    private QuestsConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean autoInstall() { return get(AUTO_INSTALL_ON_FIRST_RUN); }

    public static boolean shareSquadCredit() { return get(SHARE_SQUAD_CREDIT); }

    public static int shareRadius() { return get(SHARE_RADIUS); }
}
