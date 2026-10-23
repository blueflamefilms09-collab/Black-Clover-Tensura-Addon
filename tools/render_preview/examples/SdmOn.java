package examples;

import com.newuniverse.nusmp.client.aura.SpiritDiveMerge;

/** Preview helper of the Spirit Dive merge: the merge layer on (the default). Listed AFTER the look helper of the scene. */
public final class SdmOn {
    private SdmOn() {}

    public static void register() { SpiritDiveMerge.setEnabled(true); }
}
