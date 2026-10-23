package examples;

import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules.Models;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge2;

/** Preview helper of the Spirit Dive merge 2: everything of merge 2 switched back (boxy spirits, the previous entry, the previous crowns). Listed AFTER the look helper of the scene. */
public final class Sdm2Old {
    private Sdm2Old() {}

    public static void register() {
        SpiritDiveMerge.setEnabled(true);
        SpiritDiveMerge2.set(Models.BOXY, false, true, false);
    }
}
