package examples;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules.Models;
import com.newuniverse.nusmp.client.aura.PlayerAuraClient;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge2;
import com.newuniverse.nusmp.client.aura.SpiritDiveMergeFirstPerson;

/** Preview helper of the Spirit Dive merge 2: the wearer's FIRST PERSON view of the merge alone (the camera of the scene sits at the eyes; the painter of Aura.SPIRIT_DIVE is replaced by the first person merge). Preview only. */
public final class Sdm2Fp {
    private Sdm2Fp() {}

    public static void register() {
        SpiritDiveMerge.setEnabled(true);
        SpiritDiveMerge2.set(Models.SLIM, true, true, true);
        PlayerAuraClient.register(Aura.SPIRIT_DIVE, SpiritDiveMergeFirstPerson::paintHarness);
    }
}
