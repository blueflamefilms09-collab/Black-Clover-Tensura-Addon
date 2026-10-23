package examples;

import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules.Models;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge;
import com.newuniverse.nusmp.client.aura.SpiritDiveMerge2;

/** Preview helper of the Spirit Dive merge 2: the merge 2 as shipped (slim spirits, calm entry, first person, small crowns). Listed AFTER the look helper of the scene. */
public final class Sdm2New {
    private Sdm2New() {}

    public static void register() {
        SpiritDiveMerge.setEnabled(true);
        SpiritDiveMerge2.set(Models.SLIM, true, true, true);
    }
}
