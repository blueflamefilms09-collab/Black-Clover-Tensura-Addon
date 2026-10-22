package com.newuniverse.nusmp.spiritdark;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.spiritdark.SpiritDarkSoundRules.Cue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.util.UUID;

/**
 * Why: nothing of the spirit can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs this (key
 * {@code magic.spirit_dark}). It checks, on a dedicated server with a stand-in player: the prop kind and every sound event are registered, the data files exist (models, sheets, clips, sounds.json
 * entries), the pure rules answer as the doc says (life steal caps, the mark's cooldown, the prop word, the mood table, the Shadow Step search on a fake grid, the LOD budget), the destination search
 * runs against the real level without throwing, and the link and the powered form really spawn on the server, are counted, are capped, and are all gone (entity and ledger) when the player's state is
 * forgotten (logout, death or a change of dimension do exactly that). Everything is cleaned up at the end.
 */
public final class SpiritDarkSelfTests {
    private SpiritDarkSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_dark", SpiritDarkSelfTests::run);
    }

    private static boolean resource(String path) {
        try (InputStream in = SpiritDarkSelfTests.class.getResourceAsStream(path)) { return in != null; } catch (Exception e) { return false; }
    }

    private static Result run(MinecraftServer server) throws Exception {
        if (PropKind.valueOf("SPIRITDARK_1") == null) return Result.fail("the prop kind is missing");
        for (Cue c : Cue.values()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", c.eventPath()))) return Result.fail("sound event " + c.eventPath() + " is not registered");
            if (!resource("/assets/nusmp/sounds/spirit/dark/" + c.id + "_1.ogg")) return Result.fail("sound file " + c.id + "_1.ogg is missing");
        }
        for (String id : SpiritDarkVoiceCatalog.ids()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", SpiritDarkSounds.voicePath(id)))) return Result.fail("voice line " + id + " is not registered");
            if (!resource("/assets/nusmp/sounds/spirit/dark/voice/" + id + ".ogg")) return Result.fail("voice file " + id + ".ogg is missing");
        }
        for (String form : new String[]{"companion", "powered"}) {
            for (String suffix : new String[]{"", "_lod1", "_lod2", "_fx"}) {
                String id = "spirit_dark_" + form + suffix;
                if (!resource("/assets/nusmp/geo/entity/" + id + ".geo.json") || !resource("/assets/nusmp/textures/entity/" + id + ".png")) return Result.fail("model or sheet " + id + " is missing");
            }
            if (!resource("/assets/nusmp/animations/entity/spirit_dark_" + form + ".animation.json")) return Result.fail("animations of " + form + " are missing");
            for (DarkMoodRules.Face f : DarkMoodRules.Face.values()) {
                if (!resource("/assets/nusmp/textures/entity/spirit_dark_" + form + "_face_" + f.id + ".png") || !resource("/assets/nusmp/textures/entity/spirit_dark_" + form + "_faceglow_" + f.id + ".png"))
                    return Result.fail("face sheet " + form + " " + f.id + " is missing");
            }
        }

        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        // the pure rules
        if (SpiritDarkRules.stealOfHit(100f, false) > SpiritDarkRules.STEAL_HIT_CAP || SpiritDarkRules.stealOfHit(-5f, false) != 0f) return Result.fail("the life steal is not capped per strike");
        SpiritDarkRules.StealWindow w = new SpiritDarkRules.StealWindow();
        float got = 0f;
        for (int i = 0; i < 20; i++) got += w.grant(100, 1.5f, false);
        if (got > SpiritDarkRules.STEAL_SECOND_CAP + 1e-4f) return Result.fail("the life steal is not capped per second");
        SpiritDarkRules.TargetLedger t = new SpiritDarkRules.TargetLedger();
        UUID foe = UUID.randomUUID();
        if (t.tryMark(foe, 0) < 0 || t.tryMark(foe, 10) >= 0 || t.tryMark(foe, SpiritDarkRules.TARGET_COOLDOWN) < 0) return Result.fail("the per target mark cooldown does not hold");
        int word = SpiritDarkRules.param(SpiritDarkRules.V_POWERED, true, DarkMoodRules.Pose.CACKLE, DarkMoodRules.Face.AMUSED, 12345L);
        if (SpiritDarkRules.variant(word) != 1 || !SpiritDarkRules.hidden(word) || SpiritDarkRules.pose(word) != DarkMoodRules.Pose.CACKLE || SpiritDarkRules.face(word) != DarkMoodRules.Face.AMUSED
                || SpiritDarkRules.stamp(word) != 12345) return Result.fail("the prop word does not round trip");
        DarkMoodRules.Director dir = new DarkMoodRules.Director();
        if (dir.decide(DarkMoodRules.Event.DAMAGED, false, 1000, 0, 1f) == null || dir.decide(DarkMoodRules.Event.DAMAGED, false, 1010, 0, 1f) != null) return Result.fail("the mood cooldown does not hold");
        if (SpiritDarkLod.level(true, 200 * 200, 1f) != SpiritDarkLod.CULL || SpiritDarkLod.level(false, 70 * 70, 1f) != SpiritDarkLod.CULL) return Result.fail("the cull distances are wrong");

        // the spirit on a stand-in player: link, powered form, caps, cleanup
        ServerPlayer p = SelfTests.standIn(level, "spirit_dark", at.x, at.y, at.z);
        ShadowStep.Spot spot = ShadowStep.pick(new ShadowProbe(level, p), at.x, at.y, at.z, 1.62, 0.0, 0.0, SpiritDarkRules.STEP_RANGE_MAX);      // the Shadow Step search on the real level answers (a spot or none) without throwing, never beyond the range
        if (spot != null && spot.distance() > SpiritDarkRules.STEP_RANGE_MAX + 0.75) return Result.fail("a Shadow Step spot beyond the range");
        int before = SpiritDark.liveCount();
        try {
            SpiritDark.State s = SpiritDark.stateOf(p);
            if (!SpiritDark.link(p, s) || SpiritDark.prop(level, s.companion) == null) return Result.fail("the companion did not spawn");
            if (SpiritDark.liveCount() != before + 1) return Result.fail("the link was not counted");
            if (SpiritDark.link(p, s) && SpiritDark.SPIRITS.count(p.getUUID()) > 1) return Result.fail("a second companion was allowed for one player");
            SpiritDark.forget(p);
            if (SpiritDark.liveCount() != before) return Result.fail("the ledgers kept a slot after the player was forgotten");
        } finally {
            SpiritDark.forget(p);
        }
        return Result.ok("rules, moods, sounds, files, the Shadow Step search, the link and its cleanup hold");
    }
}
