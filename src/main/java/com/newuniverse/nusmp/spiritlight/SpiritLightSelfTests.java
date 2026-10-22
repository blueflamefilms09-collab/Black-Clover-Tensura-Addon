package com.newuniverse.nusmp.spiritlight;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.util.UUID;

/**
 * Why: nothing of the Spirit of Light can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs this (key
 * {@code magic.spirit_light}). It checks, on a dedicated server with a stand-in player: the two prop kinds and the six sound events exist and every model and sound file is on the class path; the
 * rules answer as the doc says (caps, pierce, pulse pick, prop words); and the link really spawns the cub on the server, is counted, gives Swiftness III (and only while linked), the powered form rises
 * and ends, one beam and one glow pulse spawn their props within the per player cap and the cooldowns, a second beam in the same tick is refused, and everything (entities, ledgers, the speed effect)
 * is gone when the player's state is forgotten (logout, death or a change of dimension do exactly that). Pure and server safe: no client class is touched. Everything is cleaned up at the end.
 */
public final class SpiritLightSelfTests {
    private SpiritLightSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_light", SpiritLightSelfTests::run);
    }

    private static boolean resource(String path) {
        try (InputStream in = SpiritLightSelfTests.class.getResourceAsStream("/assets/nusmp/" + path)) { return in != null; } catch (java.io.IOException e) { return false; }
    }

    private static Result run(MinecraftServer server) throws Exception {
        if (PropKind.valueOf("SPIRIT_LIGHT_1") == null || PropKind.valueOf("SPIRIT_LIGHT_2") == null) return Result.fail("a spirit light prop kind is missing");
        for (String id : SpiritLightRules.SOUND_IDS)
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", id))) return Result.fail("sound event " + id + " is not registered");
        for (String form : new String[]{"cub", "seraph"}) {
            for (String suffix : new String[]{"", "_lod1", "_lod2", "_fx"}) if (!resource("geo/entity/spiritlight_" + form + suffix + ".geo.json")) return Result.fail("model spiritlight_" + form + suffix + " is missing");
            if (!resource("animations/entity/spiritlight_" + form + ".animation.json")) return Result.fail("animation file of " + form + " is missing");
        }
        if (!SpiritLightConfig.enabled()) return Result.fail("magic.spiritLight is not on by default");

        // the rules
        if (SpiritLightRules.beamRaw(true, 1.0) > SpiritLightRules.BEAM_RAW_CAP || SpiritLightRules.beamRaw(false, 5.0) > SpiritLightRules.BEAM_RAW_CAP) return Result.fail("the beam damage is not capped");
        if (SpiritLightRules.beamPierce(true) > SpiritLightRules.MAX_PIERCE || SpiritLightRules.beamRange(true) > SpiritLightRules.MAX_RANGE) return Result.fail("the beam pierce or range is not capped");
        if (SpiritLightRules.pierce(new double[]{9, 3, -1, 5, 1, 7}, 6, 3).length != 3 || SpiritLightRules.pierce(new double[]{9, 3, -1, 5, 1, 7}, 6, 3)[0] != 4) return Result.fail("the beam does not pierce the nearest three");
        if (SpiritLightRules.pulseTargets(true) > SpiritLightRules.MAX_PULSE_TARGETS || SpiritLightRules.pulseRadius(true) > SpiritLightRules.MAX_PULSE_RADIUS) return Result.fail("the pulse is not capped");
        if (SpiritLightRules.variant(SpiritLightRules.beamParam(true, 3, -20, 17.5)) != SpiritLightRules.V_BEAM) return Result.fail("the beam prop word is wrong");
        SpiritLightLedger.Counted l = new SpiritLightLedger.Counted();
        UUID u = UUID.randomUUID();
        if (!l.acquire(u, 1, 2) || l.acquire(u, 1, 2)) return Result.fail("the per player cap does not hold");
        l.forget(u);
        if (l.total() != 0) return Result.fail("the ledger kept a slot after forget");

        // the mood rules: every pose of both forms has a clip file entry, the cub never speaks, a line without a file is never registered
        for (boolean seraph : new boolean[]{false, true})
            for (int pose = 0; pose < LightMoodRules.poseCount(seraph); pose++)
                if (LightMoodRules.poseId(seraph, LightMoodRules.poseName(seraph, pose)) != pose) return Result.fail("the pose table is not consistent at " + pose);
        for (LightMoodRules.Situation sit : LightMoodRules.Situation.values()) if (LightMoodRules.cueFor(false, sit, 0) != null) return Result.fail("the cub speaks on " + sit);
        for (String id : SpiritLightVoiceLines.AVAILABLE) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", SpiritLightSounds.voiceEventId(id)))) return Result.fail("the spoken line " + id + " is not registered");
            if (!resource("sounds/spirit_light/voice/" + id + ".ogg")) return Result.fail("the spoken line " + id + " has no file");
        }
        for (String id : SpiritLightRules.SOUND_IDS) if (!resource("sounds/" + id.replace("spirit_light.", "spirit_light/") + "_1.ogg")) return Result.fail("no file for the sound " + id);

        // the spirit on a stand-in player: link, Swiftness III, powered form, beam, pulse, caps, cleanup
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "spirit_light", at.x, at.y, at.z);
        int before = SpiritLight.liveCount();
        try {
            SpiritLight.State s = SpiritLight.stateOf(p);
            if (!SpiritLight.link(p, s, false) || SpiritLight.prop(level, s.cub) == null) return Result.fail("the cub did not spawn");
            if (SpiritLight.liveCount() != before + 1) return Result.fail("the link was not counted");
            MobEffectInstance sw = p.getEffect(MobEffects.MOVEMENT_SPEED);
            if (sw == null || sw.getAmplifier() != SpiritLightRules.SWIFT_AMPLIFIER || sw.getDuration() > SpiritLightRules.SWIFT_TICKS) return Result.fail("Swiftness III is not a short effect while linked");
            if (!SpiritLight.link(p, s, false) || SpiritLight.SPIRITS.count(p.getUUID()) != 1) return Result.fail("a second spirit was allowed for one player");
            SpiritLight.onOwnerHurt(p);
            int hurtPose = LightMoodRules.poseId(false, "hurt");
            net.minecraft.world.entity.Entity cubProp = SpiritLight.prop(level, s.cub);
            if (!(cubProp instanceof com.newuniverse.nusmp.prop.MagicPropEntity cp) || SpiritLightRules.pose(cp.param()) != hurtPose) return Result.fail("the owner's hurt did not make the cub flinch");
            if (SpiritLightVoice.say(p, "no_such_line", 0) != 0) return Result.fail("an unknown line was spoken");
            SpiritLight.togglePower(p, s);
            if (s.seraph == null || SpiritLight.prop(level, s.seraph) == null) return Result.fail("the seraph did not rise");
            if (SpiritLight.POWERED.count(p.getUUID()) != 1) return Result.fail("the powered form was not counted");
            long now = level.getGameTime();
            s.nextBeam = 0;
            SpiritLight.fireBeam(p, s);
            if (SpiritLight.EFFECTS.count(p.getUUID(), now) != 1 || s.nextBeam <= now) return Result.fail("the beam was not counted or has no cooldown");
            SpiritLight.fireBeam(p, s);
            if (SpiritLight.EFFECTS.count(p.getUUID(), now) != 1) return Result.fail("a second beam in the same tick was allowed");
            s.nextPulse = 0;
            SpiritLight.pulse(p, s);
            if (SpiritLight.EFFECTS.count(p.getUUID(), now) != 2 || s.nextPulse < now + SpiritLightRules.MIN_PULSE_COOLDOWN) return Result.fail("the pulse was not counted or has a short cooldown");
            SpiritLight.endPower(p, s, true, false);
            if (s.seraph != null || SpiritLight.POWERED.count(p.getUUID()) != 0) return Result.fail("the seraph did not end");
            SpiritLight.unlink(p, s, false);
            if (p.hasEffect(MobEffects.MOVEMENT_SPEED)) return Result.fail("Swiftness III stayed after the unlink");
            if (SpiritLight.liveCount() != before) return Result.fail("the ledgers kept a slot after the unlink");
            SpiritLight.link(p, s, false);
            SpiritLight.forget(p);
            if (SpiritLight.liveCount() != before || SpiritLight.STATES.containsKey(p.getUUID()) || p.hasEffect(MobEffects.MOVEMENT_SPEED)) return Result.fail("something of the spirit stayed after the player was forgotten");
        } finally {
            SpiritLight.forget(p);
        }
        return Result.ok("rules, caps, moods, sounds, models, the link, Swiftness III, the seraph, the beam, the pulse and their cleanup hold");
    }
}
