package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.EarthBook;
import com.newuniverse.nusmp.book.EarthFamilyBook;
import com.newuniverse.nusmp.book.EarthFamilyBooks;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.WikiBooks;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.93.0 Self-tests of the earth-family rebuild for the CI server smoke test ({@code /multiverse mega selftest magic.earth_family_rebuild.*}):
 * <ul>
 *   <li>{@code magic.earth_family_rebuild.books}: the five wiki books are the rebuilt {@link EarthFamilyBook} (through the registry's own entry point), keep the five generic page
 *       ids first, and Earth Magic keeps its six pages ahead of the three new ones.</li>
 *   <li>{@code magic.earth_family_rebuild.constructs}: every construct type spawns as a prop, carries its type in the synced word, gets a hitbox, and ends cleanly (no exception
 *       from its init or end effect, nothing left in the world).</li>
 * </ul>
 */
public final class EarthFamilySelfTests {
    private EarthFamilySelfTests() {}

    private static final String[] GENERIC = {"attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard"};
    private static final String[] EARTH = {"spike_line", "wall", "mother_earth_split", "boulder_shot", "witch_hunter_claws", "rampaging_mother_earth", "earth_golem", "earth_armor", "sinkhole"};

    /** Called from {@link EarthConstructProps#init()}. */
    public static void register() {
        SelfTests.register("magic.earth_family_rebuild.books", EarthFamilySelfTests::books);
        SelfTests.register("magic.earth_family_rebuild.constructs", EarthFamilySelfTests::constructs);
    }

    private static SelfTests.Result books(MinecraftServer server) {
        for (MagicType m : new MagicType[]{MagicType.SANDSTONE, MagicType.ROCK, MagicType.RED_OCHRE, MagicType.STONE, MagicType.MINERAL}) {
            if (!EarthFamilyBooks.owns(m)) return SelfTests.Result.fail(m + " is not owned by EarthFamilyBooks");
            GrimoireBook b = WikiBooks.attribute(m);
            if (!(b instanceof EarthFamilyBook)) return SelfTests.Result.fail(m + ": WikiBooks.attribute did not return the rebuilt book");
            List<BookPage> pages = b.pages();
            for (int k = 0; k < GENERIC.length; k++)
                if (!pages.get(k).id().equals(GENERIC[k])) return SelfTests.Result.fail(m + ": page " + k + " is " + pages.get(k).id() + ", not " + GENERIC[k]);
            if (b.familyCount() < 7 || b.familyCount() > 24) return SelfTests.Result.fail(m + ": family page count " + b.familyCount());
            for (int k = 0; k < b.familyCount(); k++)
                for (int j = k + 1; j < b.familyCount(); j++)
                    if (pages.get(k).id().equals(pages.get(j).id())) return SelfTests.Result.fail(m + ": duplicate page id " + pages.get(k).id());
        }
        List<BookPage> earth = new EarthBook().pages();
        for (int k = 0; k < EARTH.length; k++)
            if (!earth.get(k).id().equals(EARTH[k])) return SelfTests.Result.fail("Earth: page " + k + " is " + earth.get(k).id() + ", not " + EARTH[k]);
        return SelfTests.Result.ok("5 wiki books rebuilt with the generic ids first, Earth keeps its six and adds three");
    }

    private static SelfTests.Result constructs(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        Vec3 at = new Vec3(spawn.getX() + 0.5, spawn.getY() + 2, spawn.getZ() + 0.5);
        int made = 0;
        for (int type = 0; type < EarthConstruct.TYPES; type++) {
            MagicPropEntity e = EarthConstructProps.spawn(level, type, at, 0f, 1f, 40, null);
            if (e == null) return SelfTests.Result.fail("type " + type + " (" + EarthConstruct.name(type) + ") did not spawn");
            if (EarthConstruct.type(e.param()) != type) return SelfTests.Result.fail("type " + type + " lost its type in the synced word");
            if (e.kind() != PropKind.EARTH_CONSTRUCT) return SelfTests.Result.fail("type " + type + " is not an EARTH_CONSTRUCT prop");
            if (e.getBbHeight() < 0.5f) return SelfTests.Result.fail(EarthConstruct.name(type) + " has no hitbox (" + e.getBbHeight() + ")");
            e.expire();
            if (!e.isRemoved()) return SelfTests.Result.fail(EarthConstruct.name(type) + " did not end");
            made++;
        }
        return SelfTests.Result.ok(made + " constructs spawned, sized and ended cleanly");
    }
}
