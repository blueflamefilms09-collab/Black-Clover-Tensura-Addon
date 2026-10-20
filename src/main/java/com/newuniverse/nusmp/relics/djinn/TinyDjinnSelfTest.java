package com.newuniverse.nusmp.relics.djinn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.relics.djinn.tiny.TinyDjinnActor;
import com.newuniverse.nusmp.client.relics.djinn.tiny.TinyDjinnAssets;
import com.newuniverse.nusmp.client.relics.djinn.tiny.TinyDjinnMath;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * The self-test {@code djinn.tiny} (run by the CI server smoke test in the real game): every one of the 36 Djinn has its tiny model on the class path (the geo with
 * the 25 shared bones and a sane cube count, the texture and glow of the light Djinn and of the Dark Djinn), the shared animation file has the six clips and moves only
 * bones the models have, the {@link DjinnNet.Cue} packet survives its codec, and the motion rules hold (the cap keeps the nearest figures, the shoulder anchor is on the
 * owner's right, an actor walks cast, dash, hop and exit and says what happened).
 */
public final class TinyDjinnSelfTest {
    private TinyDjinnSelfTest() {}

    public static void register() { SelfTests.register("djinn.tiny", TinyDjinnSelfTest::run); }

    private static JsonObject json(String path) throws Exception {
        try (InputStream in = TinyDjinnSelfTest.class.getResourceAsStream("/assets/nusmp/" + path)) {
            if (in == null) throw new IllegalStateException("missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static boolean exists(String path) {
        try (InputStream in = TinyDjinnSelfTest.class.getResourceAsStream("/assets/nusmp/" + path)) {
            return in != null;
        } catch (Exception e) {
            return false;
        }
    }

    static SelfTests.Result run(MinecraftServer server) throws Exception {
        Set<String> bones = new HashSet<>(java.util.List.of(TinyDjinnAssets.BONES));
        int cubes = 0, models = 0;
        for (Djinn d : Djinn.values()) {
            JsonObject g = json(TinyDjinnAssets.model(d)).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject desc = g.getAsJsonObject("description");
            if (desc.get("texture_width").getAsInt() != TinyDjinnAssets.TEXTURE_SIZE || desc.get("texture_height").getAsInt() != TinyDjinnAssets.TEXTURE_SIZE)
                return SelfTests.Result.fail(d.id() + ": the texture is not " + TinyDjinnAssets.TEXTURE_SIZE + " square");
            Set<String> have = new HashSet<>();
            int n = 0;
            for (JsonElement be : g.getAsJsonArray("bones")) {
                JsonObject b = be.getAsJsonObject();
                have.add(b.get("name").getAsString());
                n += b.getAsJsonArray("cubes").size();
            }
            if (!have.equals(bones)) return SelfTests.Result.fail(d.id() + ": the bones differ from the shared set: " + have);
            if (n < TinyDjinnAssets.MIN_CUBES || n > TinyDjinnAssets.MAX_CUBES) return SelfTests.Result.fail(d.id() + ": " + n + " cubes");
            for (boolean dark : new boolean[]{false, true}) {
                if (!exists(TinyDjinnAssets.texture(d, dark))) return SelfTests.Result.fail(d.id() + ": no " + TinyDjinnAssets.texture(d, dark));
                if (!exists(TinyDjinnAssets.glow(d, dark))) return SelfTests.Result.fail(d.id() + ": no " + TinyDjinnAssets.glow(d, dark));
            }
            cubes += n;
            models++;
        }
        JsonObject clips = json(TinyDjinnAssets.ANIMATION).getAsJsonObject("animations");
        for (String c : TinyDjinnAssets.CLIPS) {
            if (!clips.has(c)) return SelfTests.Result.fail("the animation file has no clip " + c);
            for (String bone : clips.getAsJsonObject(c).getAsJsonObject("bones").keySet())
                if (!bones.contains(bone)) return SelfTests.Result.fail("clip " + c + " moves the unknown bone " + bone);
        }
        // the packet
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DjinnNet.Cue.STREAM_CODEC.encode(buf, new DjinnNet.Cue(1234, 56789));
        DjinnNet.Cue back = DjinnNet.Cue.STREAM_CODEC.decode(buf);
        if (back.owner() != 1234 || back.foe() != 56789) return SelfTests.Result.fail("the cue packet changed on the way: " + back);
        // the rules
        double[] d2 = {400, 4, 100, 9, 25};
        boolean[] keep = new boolean[5];
        if (TinyDjinnMath.cap(d2, 5, 2, keep) != 2 || !keep[1] || !keep[3] || keep[0] || keep[2] || keep[4]) return SelfTests.Result.fail("the cap does not keep the two nearest");
        float[] a = new float[3];
        TinyDjinnMath.anchor(a, 0, 64, 0, 0f, 1.8f, false, false);                  // facing south: the owner's right is -x
        if (!(a[0] < -0.3f && a[1] > 64f + 1.4f)) return SelfTests.Result.fail("the shoulder anchor is not on the owner's right and above: " + a[0] + ", " + a[1]);
        String play = TinyDjinnSelfTestSim.play();
        if (play != null) return SelfTests.Result.fail(play);
        return SelfTests.Result.ok(models + " tiny models (" + cubes / models + " cubes on average), 6 clips, the cue packet and the motion rules hold");
    }

    /** The actor's walk through its states, on its own so the test above stays readable. */
    private static final class TinyDjinnSelfTestSim {
        static String play() {
            TinyDjinnActor act = new TinyDjinnActor(0.3f);
            float[] anchor = {1f, 65f, 2f}, vessel = {0f, 65f, 0f}, foe = {6f, 65f, 2f};
            act.spawn(0f, anchor, 0f);
            int seen = act.takeEvents();
            if ((seen & TinyDjinnActor.EV_POOF_IN) == 0) return "spawning gave no poof";
            float t = 0f;
            for (; t < 12f; t += 0.5f) act.update(t, anchor, vessel, null, 0f);
            if (act.state != TinyDjinnActor.State.IDLE) return "not idle after the entrance: " + act.state;
            if (!act.cue(t, 7)) return "an idle figure refused a cast";
            int ev = 0;
            for (float end = t + 30f; t < end; t += 0.5f) {
                act.update(t, anchor, vessel, foe, 0f);
                ev |= act.takeEvents();
                if (act.killCheckDue(t)) act.markKill(t);
            }
            if ((ev & TinyDjinnActor.EV_CAST) == 0 || (ev & TinyDjinnActor.EV_STRIKE) == 0) return "the cast gave no burst or no strike: " + ev;
            if ((ev & TinyDjinnActor.EV_HOP) == 0) return "a kill gave no hop";
            act.exit(t);
            for (float end = t + 20f; t < end; t += 0.5f) act.update(t, anchor, vessel, null, 0f);
            if (act.alive()) return "the figure did not leave";
            if ((act.takeEvents() & TinyDjinnActor.EV_POOF_OUT) == 0) return "the exit gave no last puff";
            return null;
        }
    }
}
