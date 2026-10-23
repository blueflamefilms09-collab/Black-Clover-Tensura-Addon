package examples;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.PlayerAuraClient;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge;

/** Preview helper of the Spirit Dive merge: the merge ALONE (the painter of Aura.SPIRIT_DIVE is replaced by the merge layer), so the flow of the spirit can be read without the look under it. Preview only. */
public final class SdmSolo {
    private SdmSolo() {}

    public static void register() {
        SpiritDiveMerge.setEnabled(true);
        PlayerAuraClient.register(Aura.SPIRIT_DIVE, SpiritDiveMerge::paintSolo);
    }
}
