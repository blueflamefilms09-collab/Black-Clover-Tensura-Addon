package com.newuniverse.nusmp.undine;

import com.newuniverse.nusmp.book.WaterBook;
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
 * Why: nothing of the Undine can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs this (key
 * {@code magic.undine}). It checks, on a dedicated server with a stand-in player: the prop kind and the sound events exist and are the catalogue's; the sound files and the animation file are on the
 * class path; the Water grimoire's page ids are still the ones the casting acts on (nothing renamed, nothing removed); the rules, caps and emitter answer as the doc says; the companion link and the
 * full form really spawn on the server, are counted, are capped, and are all gone (entity and ledger) when the player's state is forgotten (logout, death or a change of dimension do exactly that); and a
 * sound goes through the budget without touching a client class. Pure and server safe: everything is cleaned up at the end.
 */
public final class UndineSelfTests {
    private UndineSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.undine", UndineSelfTests::run);
    }

    private static Result run(MinecraftServer server) throws Exception {
        if (PropKind.valueOf("UNDINE_1") == null) return Result.fail("the UNDINE_1 prop kind is missing");
        for (UndineSoundSet.Cue c : UndineSoundSet.Cue.values()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", c.event()))) return Result.fail("the sound event " + c.event() + " is not registered");
            for (int i = 1; i <= c.variants(); i++) {
                try (InputStream in = UndineSelfTests.class.getResourceAsStream("/assets/nusmp/sounds/undine/" + c.id() + "_" + i + ".ogg")) {
                    if (in == null) return Result.fail("the sound file " + c.id() + "_" + i + ".ogg is missing");
                }
            }
        }
        try (InputStream in = UndineSelfTests.class.getResourceAsStream("/assets/nusmp/animations/entity/undine_body.animation.json")) {
            if (in == null) return Result.fail("undine_body.animation.json is missing");
            String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String clip : new String[]{"idle", "full_idle", "cast", "open"}) if (!text.contains("\"" + clip + "\"")) return Result.fail("clip " + clip + " is missing");
        }
        WaterBook water = new WaterBook();
        String[] ids = {"waterball", "roar", "cradle", "aqua_javelin", "sea_dragons_nest", "valkyrie_dress", "sea_dragons_wrath", "tide_lance", "scale_mail", "torrent"};
        for (int k = 0; k < ids.length; k++) if (water.page(k) == null || !water.page(k).id().equals(ids[k])) return Result.fail("Water page " + k + " is not " + ids[k]);
        if (!UndineConfig.enabled()) return Result.fail("magic.undine must default to true");

        // the rules
        if (UndineRules.variant(UndineRules.param(UndineRules.V_FULL, 77)) != UndineRules.V_FULL || UndineRules.extra(UndineRules.param(UndineRules.V_FULL, 77)) != 77) return Result.fail("the prop word does not round-trip");
        if (UndineRules.callsFull(8.0) || !UndineRules.callsFull(20.0) || !UndineRules.callsFull(35.0)) return Result.fail("the full form is called by the wrong pages");
        double[] palm = UndineRules.fullPalms(0, 0, 0, 0);
        double[] eyeShift = UndineRules.emit(0, 1.6, 0.2, 0, 1.62, 0, 0, 0, 0, palm[0], palm[1], palm[2]);
        if (Math.hypot(eyeShift[0], Math.hypot(eyeShift[1] - 1.62, eyeShift[2])) > UndineRules.EMITTER_MAX_SHIFT + 1e-6) return Result.fail("the emitter moved a spell too far from the eye");
        UndineLedger l = new UndineLedger();
        UUID u = UUID.randomUUID();
        if (!l.acquire(u, 1, 2) || l.acquire(u, 1, 2)) return Result.fail("the per player cap does not hold");
        l.forget(u);
        if (l.total() != 0) return Result.fail("the ledger kept a slot after forget");
        UndineVoiceBudget b = new UndineVoiceBudget();
        if (!b.tryAcquire(u, "cast", 100, 8, 12) || b.tryAcquire(u, "cast", 101, 8, 12)) return Result.fail("the sound budget does not hold");

        // the companion and the full form on a stand-in player: link, full, caps, cleanup
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "undine", at.x, at.y, at.z);
        int before = UndineCasting.liveCount();
        try {
            UndineCasting.State s = UndineCasting.stateOf(p);
            if (!UndineCasting.link(p, s) || UndineCasting.prop(level, s.spirit) == null) return Result.fail("the companion did not spawn");
            if (UndineCasting.liveCount() != before + 1) return Result.fail("the link was not counted");
            if (UndineCasting.link(p, s) && UndineCasting.SPIRITS.count(p.getUUID()) > 1) return Result.fail("a second companion was allowed for one player");
            if (!UndineCasting.summonFull(p, s, level.getGameTime()) || UndineCasting.prop(level, s.full) == null) return Result.fail("the full form did not spawn");
            if (UndineCasting.summonFull(p, s, level.getGameTime() + 1)) return Result.fail("a second full form was allowed for one player");
            UndineAudio.play(level, p.getUUID(), at.x, at.y, at.z, UndineSoundSet.Cue.CAST, false);
            UndineCasting.forget(p);
            if (UndineCasting.liveCount() != before) return Result.fail("the ledgers kept a slot after the player was forgotten");
        } finally {
            UndineCasting.forget(p);
        }
        return Result.ok("rules, sounds, clips, the companion link, the full form and their cleanup hold");
    }
}
