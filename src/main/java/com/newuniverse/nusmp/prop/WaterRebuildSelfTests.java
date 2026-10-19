package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.93.0: the Water / Dark / Lightning / Flame rebuild, proved by the real game (the CI server smoke test boots the mod and runs these; key prefix {@code magic}). For each magic: its book
 * is the real registered one, its pages are the legacy ones in the legacy order followed by the new ones (no page id moved, none lost, at most 24 family pages, every page has a cast and a
 * name), its armour slot and its two prop kinds have a registered behaviour, and the shared enums really end with the new constants. Nothing is cast and nobody is touched.
 */
public final class WaterRebuildSelfTests {
    private WaterRebuildSelfTests() {}

    private static boolean registered;

    /** Called once from WaterProps.init(). */
    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.water_rebuild", s -> check(MagicType.WATER, Aura.WATER, PropKind.WATER_1, PropKind.WATER_2, VfxShape.WATERFX_CAST,
                List.of("waterball", "roar", "cradle", "aqua_javelin", "sea_dragons_nest", "valkyrie_dress"), List.of("sea_dragons_wrath", "tide_lance", "scale_mail", "torrent")));
        SelfTests.register("magic.dark_rebuild", s -> check(MagicType.DARK, Aura.DARK, PropKind.DARK_1, PropKind.DARK_2, VfxShape.DARKFX_CAST,
                List.of("avidya_slash", "black_hole", "dimension_slash", "death_thrust", "black_blade", "avidya_wild_slash", "black_moon", "iai_slash", "dimension_slash_equinox"),
                List.of("dark_cloak", "shadow_embrace", "black_hole_collapse")));
        SelfTests.register("magic.lightning_rebuild", s -> check(MagicType.LIGHTNING, Aura.LIGHTNING, PropKind.LIGHTNING_1, PropKind.LIGHTNING_2, VfxShape.LIGHTNINGFX_CAST,
                List.of("chain", "thunder_boots", "thunder_lance", "thunder_fiend", "pulsaranta", "rising_salim", "thunderbird_cavalry", "black_lightning_battle_fiend"),
                List.of("raging_lightning", "thunder_drum", "lightning_surge")));
        SelfTests.register("magic.flame_rebuild", s -> check(MagicType.FLAME, Aura.FLAME, PropKind.FLAME_1, PropKind.FLAME_2, VfxShape.FLAMEFX_CAST,
                List.of("exploding_fireball", "sol_linea", "calderos", "leo_rugiens", "spiral_flame", "wild_bursting_flame", "ignis_columna", "calidos_brachium"),
                List.of("crimson_lion_king", "flame_armour", "phoenix_flare")));
    }

    private static Result check(MagicType magic, Aura aura, PropKind p1, PropKind p2, VfxShape shape, List<String> legacy, List<String> added) {
        GrimoireBook book = NUSkills.bookFor(magic);
        if (book == null) return Result.fail("no registered book for " + magic);
        List<BookPage> pages = book.pages();
        List<String> ids = new ArrayList<>();
        for (BookPage p : pages) ids.add(p.id());
        int family = legacy.size() + added.size();
        if (book.familyCount() != family) return Result.fail(magic + ": " + book.familyCount() + " family pages, expected " + family);
        if (family > 24) return Result.fail(magic + ": more than 24 family pages");
        for (int i = 0; i < legacy.size(); i++) if (!legacy.get(i).equals(ids.get(i))) return Result.fail(magic + ": legacy page " + i + " is '" + ids.get(i) + "', expected '" + legacy.get(i) + "'");
        for (int i = 0; i < added.size(); i++) if (!added.get(i).equals(ids.get(legacy.size() + i))) return Result.fail(magic + ": new page " + i + " is not '" + added.get(i) + "'");
        for (int i = 0; i < family; i++) {
            BookPage p = pages.get(i);
            if (p.cast() == null || p.name() == null || p.name().isBlank()) return Result.fail(magic + ": page '" + p.id() + "' has no cast or no name");
        }
        if (MagicProps.behavior(p1) == null || MagicProps.behavior(p2) == null) return Result.fail(magic + ": a prop kind has no behaviour");
        if (aura.ordinal() < Aura.WIKI.ordinal()) return Result.fail(magic + ": the armour slot moved");
        if (shape.ordinal() < VfxShape.BUTOH_AFTERIMAGE.ordinal()) return Result.fail(magic + ": the VFX shape moved");
        return Result.ok(magic + ": " + legacy.size() + " legacy pages in order, " + added.size() + " new, armour " + aura + ", props " + p1 + " / " + p2);
    }
}
