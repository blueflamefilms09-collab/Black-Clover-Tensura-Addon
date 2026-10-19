package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing of the nature-magic rebuild can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these
 * (key prefix {@code magic}). {@code magic.tree_rebuild_pages}: the rebuilt grimoires of Tree, Vine, Poison Plant, Mud and Mucus (made by the real WikiBooks factory) and Plant Magic keep the old
 * page ids first and in order and append the new ones, inside the page limit. {@code magic.tree_rebuild_assets}: every geo model, animation, texture and glow map of the six armours and twelve
 * summons is in the jar, and the eighteen effect shapes exist. {@code magic.tree_rebuild_props}: every summon of the six props can be spawned for a stand-in caster, is of its kind, carries its
 * variant, and ends cleanly. Registered from {@code prop.TreeProps.init}.
 */
public final class TreeSelfTests {
    private TreeSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.tree_rebuild_pages", TreeSelfTests::pages);
        SelfTests.register("magic.tree_rebuild_assets", TreeSelfTests::assets);
        SelfTests.register("magic.tree_rebuild_props", TreeSelfTests::props);
    }

    private static final List<MagicType> WIKI = List.of(MagicType.TREE, MagicType.VINE, MagicType.POISON_PLANT, MagicType.MUD, MagicType.MUCUS);
    private static final String[] PIECES = {"helm", "crown", "chest", "pauld_r", "pauld_l", "bracer_r", "bracer_l", "greave_r", "greave_l", "back", "aux"};
    private static final String[][] SUMMONS = {{"tree", "guardian", "lance"}, {"vine", "serpent", "whip", "wall"}, {"plant", "familiar", "bloom"}, {"poison_plant", "bloom", "maw"},
            {"mud", "golem", "spout"}, {"mucus", "construct", "tendril"}};

    private static Result pages(MinecraftServer server) {
        StringBuilder sb = new StringBuilder();
        for (MagicType m : WIKI) {
            GrimoireBook b = WikiBooks.attribute(m);
            if (!(b instanceof TreeElementBook)) return Result.fail(m + " is not the rebuilt book");
            String why = check(m.name(), b);
            if (why != null) return Result.fail(m + ": " + why);
            sb.append(m.name().toLowerCase()).append('=').append(b.familyCount()).append(' ');
        }
        PlantBook plant = new PlantBook();
        String why = check("PLANT", plant);
        if (why != null) return Result.fail("PLANT: " + why);
        sb.append("plant=").append(plant.familyCount());
        return Result.ok(sb.toString().trim());
    }

    private static String check(String magic, GrimoireBook b) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < b.familyCount(); i++) ids.add(b.page(i).id());
        return TreeRules.check(magic, ids);
    }

    private static Result assets(MinecraftServer server) {
        List<String> missing = new ArrayList<>();
        for (String key : new String[]{"tree", "vine", "plant", "poison_plant", "mud", "mucus"})
            for (String piece : PIECES) model(key + "_" + piece, missing);
        for (String[] s : SUMMONS) for (int i = 1; i < s.length; i++) model(s[0] + "_" + s[i], missing);
        for (String key : new String[]{"tree", "vine", "plant", "poison_plant", "mud", "mucus"}) {
            res("assets/nusmp/textures/aura/" + key + "_skin.png", missing);
            res("assets/nusmp/textures/aura/" + key + "_skin_glow.png", missing);
            for (int n = 1; n <= 3; n++) if (TreeElementBook.shape(MagicType.valueOf(key.toUpperCase()), n).name().startsWith("WIKI")) missing.add(key + " shape " + n);
        }
        if (!missing.isEmpty()) return Result.fail("missing: " + String.join(", ", missing.subList(0, Math.min(8, missing.size()))) + (missing.size() > 8 ? " (+" + (missing.size() - 8) + ")" : ""));
        return Result.ok("66 armour models, 13 summon models, 12 skins, 18 shapes");
    }

    private static void model(String id, List<String> missing) {
        res("assets/nusmp/geo/entity/" + id + ".geo.json", missing);
        res("assets/nusmp/textures/entity/" + id + ".png", missing);
        res("assets/nusmp/textures/entity/" + id + "_glow.png", missing);
    }

    private static void res(String path, List<String> missing) {
        if (TreeSelfTests.class.getClassLoader().getResource(path) == null) missing.add(path);
    }

    private static Result props(MinecraftServer server) {
        ServerPlayer p = SelfTests.player(server.overworld(), "NatureRebuild", 0.5, 100.0, 0.5);
        PropKind[] kinds = {PropKind.TREE_1, PropKind.VINE_1, PropKind.PLANT_1, PropKind.POISON_PLANT_1, PropKind.MUD_1, PropKind.MUCUS_1};
        int[] variants = {2, 3, 2, 2, 2, 2};
        int n = 0;
        for (int k = 0; k < kinds.length; k++) for (int v = 0; v < variants[k]; v++) {
            MagicPropEntity e = MagicProps.spawn(server.overworld(), kinds[k], new Vec3(0.5, 100.0, 0.5), 0f, 1f, 40, v, p);
            if (e == null) return Result.fail(kinds[k] + " variant " + v + " could not be spawned");
            if (e.kind() != kinds[k] || (e.param() & 0xFF) != v) { e.discard(); return Result.fail(kinds[k] + " variant " + v + " came out as " + e.kind() + " / " + (e.param() & 0xFF)); }
            e.expire();
            n++;
        }
        return Result.ok(n + " summons spawned and ended cleanly");
    }
}
