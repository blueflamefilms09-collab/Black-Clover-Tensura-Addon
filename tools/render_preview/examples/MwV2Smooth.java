package examples;

/** Wind Spirit Dive overlay v2 of Yuno with the SMOOTH arms (the first v2: rounded sleeves, cuffs and domed mantles; client option modeArmor.windArms = SMOOTH). */
public final class MwV2Smooth {
    private MwV2Smooth() {}

    public static void register() { MwV2Common.install(true, true, com.newuniverse.nusmp.book.ModeArmorWindV2Rules.ArmStyle.SMOOTH); }
}
