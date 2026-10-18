package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.93.0 magic rebuild: the CI self-test every rebuilt magic registers ({@code magic.<key>_rebuild}, run by the server smoke test). It can only check what a
 * server can see, which is exactly what a rebuild must not break: the old page ids are still there, in their old order and at their old mode numbers; the
 * new pages are appended after them; ids are unique; the armour has an Aura constant, the summon models their PropKind, the effects their VfxShape; and every
 * geo model of the armour and summons is in the jar with its texture and glow map. The pure rule ({@link SpatialPageOrder}) is also unit tested.
 */
public final class SpatialRebuildChecks {
    private SpatialRebuildChecks() {}

    /** Registers the self-test of one magic (called from the magic's Props.init()). */
    public static void register(String key, MagicType magic, List<String> oldIds, List<String> newIds, String auraName, List<String> models, List<String> propKinds, List<String> shapes) {
        SelfTests.register("magic." + key + "_rebuild", server -> {
            GrimoireBook book = NUSkills.bookFor(magic);
            if (book == null) return SelfTests.Result.fail("no book for " + magic);
            List<String> ids = new ArrayList<>();
            for (BookPage p : book.pages()) ids.add(p.id());
            List<String> bad = SpatialPageOrder.problems(ids, oldIds, newIds);
            for (String n : List.of(auraName)) if (!enumHas(Aura.values(), n)) bad.add("no Aura." + n);
            for (String n : propKinds) if (!enumHas(PropKind.values(), n)) bad.add("no PropKind." + n);
            for (String n : shapes) if (!enumHas(VfxShape.values(), n)) bad.add("no VfxShape." + n);
            for (String m : models) for (String suffix : new String[]{".png", "_glow.png"}) {
                if (SpatialRebuildChecks.class.getResource("/assets/nusmp/textures/entity/" + m + suffix) == null) bad.add("missing texture " + m + suffix);
            }
            for (String m : models) if (SpatialRebuildChecks.class.getResource("/assets/nusmp/geo/entity/" + m + ".geo.json") == null) bad.add("missing geo " + m);
            return bad.isEmpty() ? SelfTests.Result.ok(key + ": " + ids.size() + " pages, " + models.size() + " models") : SelfTests.Result.fail(String.join("; ", bad));
        });
    }

    private static boolean enumHas(Enum<?>[] all, String name) {
        for (Enum<?> e : all) if (e.name().equals(name)) return true;
        return false;
    }
}
