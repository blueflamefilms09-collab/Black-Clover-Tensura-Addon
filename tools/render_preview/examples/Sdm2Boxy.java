package examples;

import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules.Models;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge2;

/** Preview helper of the Spirit Dive merge 2: merge 2 with the boxy spirits only. Listed AFTER the look helper of the scene. */
public final class Sdm2Boxy {
    private Sdm2Boxy() {}

    public static void register() {
        SpiritDiveMerge.setEnabled(true);
        SpiritDiveMerge2.set(Models.BOXY, true, true, true);
    }
}
