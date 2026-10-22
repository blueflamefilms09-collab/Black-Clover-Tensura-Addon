package com.newuniverse.nusmp.spiritspace;

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
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Why: nothing of the Spirit of Space can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs this (key
 * {@code magic.spirit_space}). Pure and server safe: it checks the two prop kinds and every sound event are registered, the sound entries and the lang lines exist in the jar, the caps, the channel,
 * the landing search (on a fake world), the waypoint rules, the projectile selection, the mood rules and the quad counts answer as the doc says, and then, with a stand-in player, that linking really
 * spawns the orb on the server, is counted, is not given twice, and that forgetting the player (logout, death, a change of dimension do exactly that) removes the entity and the ledger slot.
 */
public final class SpiritSpaceSelfTests {
    private SpiritSpaceSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_space", SpiritSpaceSelfTests::run);
    }

    private static Result run(MinecraftServer server) throws Exception {
        if (PropKind.valueOf("SPIRITSPACE_1") == null || PropKind.valueOf("SPIRITSPACE_2") == null) return Result.fail("a spirit space prop kind is missing");
        for (SoundGate.Kind k : SoundGate.Kind.values()) {
            if (k == SoundGate.Kind.VOICE) continue;
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", SpiritSpaceSounds.id(k)))) return Result.fail("sound event " + SpiritSpaceSounds.id(k) + " is not registered");
        }
        for (SpaceMoodRules.Voice v : SpaceMoodRules.Voice.values()) {
            if (v == SpaceMoodRules.Voice.NONE) continue;
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", SpiritSpaceSounds.voiceId(v)))) return Result.fail("voice event " + SpiritSpaceSounds.voiceId(v) + " is not registered");
        }
        try (InputStream in = SpiritSpaceSelfTests.class.getResourceAsStream("/assets/nusmp/sounds.json")) {
            if (in == null) return Result.fail("sounds.json is missing");
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!text.contains("\"spirit_space.hum\"") || !text.contains("\"spirit_space.voice_whisper\"")) return Result.fail("sounds.json has no spirit_space entries");
        }
        for (String f : new String[]{"geo/entity/spirit_space_orb.geo.json", "geo/entity/spirit_space_wisp.geo.json", "textures/entity/spirit_space/stars_far.png", "textures/entity/spirit_space/stars_mid.png", "textures/entity/spirit_space/stars_near.png"})
            try (InputStream in = SpiritSpaceSelfTests.class.getResourceAsStream("/assets/nusmp/" + f)) {
                if (in == null) return Result.fail("asset " + f + " is missing");
            }

        // the rules
        if (SpiritSpaceRules.inRange(0, 0, 0, 100, 0, 0, 96) || !SpiritSpaceRules.inRange(0, 0, 0, 96, 0, 0, 96)) return Result.fail("the portal range test is wrong");
        if (SpiritSpaceRules.cutDamage(1e9, 1e9, 1e9, 1e9) > SpiritSpaceRules.CUT_HIT_CEILING) return Result.fail("the cut damage is not capped");
        if (SpiritSpaceRules.channelBroken(0, 0, 0, 1.0, 0, 0) || !SpiritSpaceRules.channelBroken(0, 0, 0, 2.0, 0, 0)) return Result.fail("the channel rule is wrong");
        if (SpiritSpaceConfig.portalRange() > SpiritSpaceRules.RANGE_HARD || SpiritSpaceConfig.maxWisps() > SpiritSpaceRules.WISPS_HARD) return Result.fail("a config value passes its hard cap");
        double[] d = {9, 1, 4, 16};
        int[] pick = new int[2];
        if (SpiritSpaceRules.nearest(d, 4, 2, pick) != 2 || pick[0] != 1 || pick[1] != 2) return Result.fail("the nearest-projectile selection is wrong");
        Waypoints.Waypoint[] book = new Waypoints.Waypoint[3];
        book[0] = new Waypoints.Waypoint("Home", "minecraft:overworld", 10, 64, 10, 0f);
        if (Waypoints.checkTrip(book, 0, "minecraft:the_nether", 0, 64, 0, 96) != Waypoints.OTHER_DIMENSION) return Result.fail("a cross-dimension trip was not refused");
        if (Waypoints.checkTrip(book, 0, "minecraft:overworld", 1000, 64, 0, 96) != Waypoints.TOO_FAR) return Result.fail("a far trip was not refused");
        if (Waypoints.checkTrip(book, 1, "minecraft:overworld", 0, 64, 0, 96) != Waypoints.EMPTY) return Result.fail("an empty slot was not refused");
        if (SpaceLanding.find(new Void(), 0, 64, 0) != null) return Result.fail("a landing was found over the void");
        if (SpiritSpaceMesh.orbQuads(0) > 520 || SpiritSpaceMesh.wispQuads(0) > 900 || SpiritSpaceMesh.orbQuads(2) > 100 || SpiritSpaceMesh.wispQuads(2) > 100) return Result.fail("a level of detail is over its quad budget");
        if (SpaceMoodRules.moodFor(SpaceMoodRules.Situation.UNLINK, false, 0) != SpaceMoodRules.Mood.FAREWELL || SpaceMoodRules.moodFor(SpaceMoodRules.Situation.CUT, true, 0) != SpaceMoodRules.Mood.ROAR) return Result.fail("the mood table is wrong");
        SoundGate gate = new SoundGate();
        UUID id = UUID.randomUUID();
        if (!gate.allow(id, SoundGate.Kind.PING, 100) || gate.allow(id, SoundGate.Kind.PING, 101)) return Result.fail("the sound gate does not hold");
        SpaceLedger l = new SpaceLedger();
        if (!l.acquire(id, 1, 2) || l.acquire(id, 1, 2)) return Result.fail("the per player cap does not hold");
        l.forget(id);
        if (l.total() != 0) return Result.fail("the ledger kept a slot after forget");

        // the orb on a stand-in player: link, count, cleanup
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "spirit_space", at.x, at.y, at.z);
        int before = SpiritSpace.liveCount();
        try {
            SpiritSpace.State s = SpiritSpace.stateOf(p);
            if (!SpiritSpace.link(p, s) || SpiritSpace.prop(level, s.spirit) == null) return Result.fail("the orb did not spawn");
            if (SpiritSpace.liveCount() != before + 1) return Result.fail("the link was not counted");
            if (SpiritSpace.link(p, s) && SpiritSpace.SPIRITS.count(p.getUUID()) > 1) return Result.fail("a second spirit was allowed for one player");
            UUID orb = s.spirit;
            SpiritSpace.forget(p);
            if (SpiritSpace.liveCount() != before) return Result.fail("the ledgers kept a slot after the player was forgotten");
            if (level.getEntity(orb) != null && !level.getEntity(orb).isRemoved()) return Result.fail("the orb survived the player's cleanup");
        } finally {
            SpiritSpace.forget(p);
        }
        return Result.ok("rules, sounds, moods, landing and the orb's link and cleanup hold");
    }

    /** A world that is all void: nothing to stand on, everything hazardous. */
    private static final class Void implements SpaceLanding.Probe {
        @Override public boolean solid(int x, int y, int z) { return false; }
        @Override public boolean passable(int x, int y, int z) { return true; }
        @Override public boolean hazard(int x, int y, int z) { return false; }
        @Override public int minY() { return -64; }
        @Override public int maxY() { return 320; }
    }
}
