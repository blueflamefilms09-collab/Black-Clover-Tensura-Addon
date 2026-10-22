package com.newuniverse.nusmp.gnome;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.EarthBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.util.UUID;

/**
 * Why: nothing of the Gnome can be seen by its author in a game, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs this (key
 * {@code magic.gnome}). It checks, on a dedicated server with a stand-in player: the prop kind and the seven sound events are registered and their files are on the class path; the nine pages of the Earth
 * book are still the old ones in the old order (nothing renamed, nothing removed); the caps, the emitter, the clip and the sound rules answer as the doc says; and the companion and the full form really
 * spawn on the server, are counted, are capped, and are all gone (entity and ledger) when the player's state is forgotten (logout, death and a change of dimension do exactly that). Pure and server safe:
 * no client class is touched. Everything is cleaned up at the end.
 */
public final class GnomeSelfTests {
    private GnomeSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.gnome", GnomeSelfTests::run);
    }

    private static final String[] EARTH_PAGES = {"spike_line", "wall", "mother_earth_split", "boulder_shot", "witch_hunter_claws", "rampaging_mother_earth", "earth_golem", "earth_armor", "sinkhole"};

    private static Result run(MinecraftServer server) throws Exception {
        if (PropKind.valueOf("GNOME_1") == null) return Result.fail("the GNOME_1 prop kind is missing");
        for (GnomeSoundRules.Cue c : GnomeSoundRules.CUES) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", c.event()))) return Result.fail("sound event not registered: " + c.event());
            for (int v = 1; v <= c.variants(); v++)
                try (InputStream in = GnomeSelfTests.class.getResourceAsStream("/assets/nusmp/sounds/gnome/" + c.id() + "_" + v + ".ogg")) {
                    if (in == null) return Result.fail("missing sound file " + c.id() + "_" + v + ".ogg");
                }
        }
        try (InputStream in = GnomeSelfTests.class.getResourceAsStream("/assets/nusmp/sounds.json")) {
            if (in == null) return Result.fail("sounds.json is missing");
            String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (GnomeSoundRules.Cue c : GnomeSoundRules.CUES) if (!text.contains("\"" + c.event() + "\"")) return Result.fail("sounds.json has no entry " + c.event());
        }
        for (String f : new String[]{"gnome_full", "gnome_full_lod1", "gnome_full_lod2", "gnome_full_fx", "gnome_companion", "gnome_companion_lod1", "gnome_companion_lod2", "gnome_companion_fx"})
            try (InputStream in = GnomeSelfTests.class.getResourceAsStream("/assets/nusmp/geo/entity/" + f + ".geo.json")) {
                if (in == null) return Result.fail("missing model " + f);
            }

        // the Earth book is the old book: nine pages, same ids, same order; the wrapped casts are the pages' own
        EarthBook earth = new EarthBook();
        for (int k = 0; k < EARTH_PAGES.length; k++) {
            BookPage pg = earth.page(k);
            if (pg == null || !pg.id().equals(EARTH_PAGES[k])) return Result.fail("Earth page " + k + " is not " + EARTH_PAGES[k]);
        }
        if (earth.page(2).costPercent() < GnomeRules.FULL_COST_PERCENT || earth.page(0).costPercent() >= GnomeRules.FULL_COST_PERCENT) return Result.fail("the cost tiers of the Earth pages moved");

        // the rules
        if (GnomeRules.variant(GnomeRules.param(GnomeRules.V_FULL, 77)) != GnomeRules.V_FULL || GnomeRules.stamp(GnomeRules.param(GnomeRules.V_FULL, 77)) != 77) return Result.fail("the prop word does not round trip");
        if (GnomeRules.fullClip(3, 120, -1, false) != GnomeRules.CLIP_RISE || GnomeRules.fullClip(110, 120, -1, false) != GnomeRules.CLIP_SINK || GnomeRules.fullClip(40, 120, 5, false) != GnomeRules.CLIP_CAST) return Result.fail("the clip choice is wrong");
        GnomeLedger l = new GnomeLedger();
        UUID u = UUID.randomUUID();
        if (!l.acquire(u, 1, 2) || l.acquire(u, 1, 2)) return Result.fail("the per player cap does not hold");
        l.forget(u);
        if (l.total() != 0) return Result.fail("the ledger kept a slot after forget");
        GnomeSoundRules.Gate gate = new GnomeSoundRules.Gate();
        if (!gate.allow(u, "rise", 100) || gate.allow(u, "rise", 101)) return Result.fail("the sound gate does not hold a cue's cooldown");
        double[] e = GnomeRules.emit(0, 1.6, 0, 0, 1.6, 0, 0, 0, 0, 9, 9, 9);
        if (Math.sqrt((e[0]) * (e[0]) + (e[1] - 1.6) * (e[1] - 1.6) + e[2] * e[2]) > GnomeRules.EMITTER_MAX_SHIFT + 1e-6) return Result.fail("the emitter is not bounded");

        // the Gnome on a stand-in player: companion, full form, caps, cleanup
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "gnome", at.x, at.y, at.z);
        int before = Gnome.liveCount();
        try {
            Gnome.State s = Gnome.state0(p);
            if (!Gnome.link(p, s) || Gnome.prop(level, s.spirit) == null) return Result.fail("the companion did not spawn");
            if (Gnome.liveCount() != before + 1) return Result.fail("the link was not counted");
            if (Gnome.link(p, s) && Gnome.SPIRITS.count(p.getUUID()) > 1) return Result.fail("a second companion was allowed for one player");
            double[] f = GnomeRules.full(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
            if (!Gnome.summonFull(p, s, level.getGameTime(), f) || Gnome.prop(level, s.full) == null) return Result.fail("the full form did not spawn");
            if (Gnome.summonFull(p, s, level.getGameTime(), f) && Gnome.FULLS.count(p.getUUID()) > 1) return Result.fail("a second giant was allowed for one player");
            Gnome.forget(p);
            if (Gnome.liveCount() != before) return Result.fail("the ledgers kept a slot after the player was forgotten");
        } finally {
            Gnome.forget(p);
        }
        return Result.ok("rules, sounds, the Earth book, the companion and the giant, and their cleanup hold");
    }
}
