package examples;

import com.newuniverse.nusmp.client.aura.SpiritDiveMerge;

/** Preview helper of the Spirit Dive merge: the merge layer off (the same dive as before the merge existed), to compare. */
public final class SdmOff {
    private SdmOff() {}

    public static void register() { SpiritDiveMerge.setEnabled(false); }
}
