package com.newuniverse.nusmp.ultimate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the charge animation of an Ultimate is chosen by the caster's kingdom (pure {@link UltimateAnim}) and played from clips in cast.animation.json. This pins the mapping (every kingdom has its own start / loop /
 * release, the setting off and a caster without a kingdom give the old single loop), and reads the REAL animation file with the repo's own loader ({@link GeoAnim}) to check that every clip exists, drives only real
 * bones, closes its loop, hands over from start to loop without a jump, stays inside the time of the cast, and is not frozen. Pure, no game.
 */
class UltimateAnimTest {
    private static final Set<String> BONES = Set.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg");
    private static final String[] DRIVEN = {"left_arm", "right_arm", "left_leg", "right_leg"};     // the bones client/CastAnimClient applies (the body is kept in the clips for the Blockbench master)
    private static final String FILE = "src/main/resources/assets/nusmp/animations/player/cast.animation.json";

    private static JsonObject json() throws Exception {
        try (Reader r = new FileReader(new File(FILE))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static GeoAnim anims() throws Exception { return GeoAnim.parse(json()); }

    private static float[] rot(GeoAnim.Clip c, String bone, float t) {
        GeoAnim.BoneAnim b = c.bones.get(bone);
        float[] v = b == null || b.rotation == null ? null : b.rotation.sample(t);
        return v == null ? new float[]{0, 0, 0} : v;
    }

    // ------------------------------------------------------------------ the mapping
    @Test
    void everyKingdomHasItsOwnThreeClips() {
        Set<String> seen = new HashSet<>();
        for (Kingdom k : Kingdom.values()) {
            UltimateAnim.Clips c = UltimateAnim.forKingdom(k);
            assertTrue(c != UltimateAnim.DEFAULT, k + " has no animation of its own");
            assertNotNull(c.start());
            assertNotNull(c.loop());
            assertNotNull(c.release());
            String id = k.name().toLowerCase(java.util.Locale.ROOT);
            assertEquals("ult_" + id + "_start", c.start());
            assertEquals("ult_" + id + "_loop", c.loop());
            assertEquals("ult_" + id + "_release", c.release());
            for (String s : List.of(c.start(), c.loop(), c.release())) assertTrue(seen.add(s), "clip named twice: " + s);
        }
        assertEquals(Kingdom.values().length * 3, seen.size());
        assertEquals(seen, new HashSet<>(UltimateAnim.ids()));
        assertEquals(List.of(Kingdom.values()), UltimateAnim.styled());
    }

    @Test
    void noKingdomAndTheSettingOffGiveTheOldSingleLoop() {
        assertSame(UltimateAnim.DEFAULT, UltimateAnim.forKingdom(null));
        assertSame(UltimateAnim.DEFAULT, UltimateAnim.choose(true, null));
        for (Kingdom k : Kingdom.values()) {
            assertSame(UltimateAnim.DEFAULT, UltimateAnim.choose(false, k), "setting off must restore the old id for " + k);
            assertTrue(UltimateAnim.choose(true, k) != UltimateAnim.DEFAULT);
        }
        UltimateAnim.Clips d = UltimateAnim.DEFAULT;
        assertEquals("cast_mana_zone", d.loop(), "the default charge is the old Mana Zone loop");
        assertEquals("cast_signature", d.release(), "the default release is the old Signature finisher");
        assertNull(d.start());
        assertEquals("cast_mana_zone", d.first(), "without a start clip the loop begins with the cast, as before");
    }

    @Test
    void theHoldBeginsOnceAtTheEndOfTheStartPoseAndNeverForTheDefault() {
        UltimateAnim.Clips c = UltimateAnim.forKingdom(Kingdom.CLOVER);
        assertEquals(c.start(), c.first());
        int due = 0;
        for (int age = 0; age < 400; age++) if (c.loopDue(age)) { due++; assertEquals(UltimateAnim.START_TICKS, age); }
        assertEquals(1, due, "the hold is sent exactly once per cast");
        for (int age = 0; age < 400; age++) assertFalse(UltimateAnim.DEFAULT.loopDue(age), "the default plays its one loop from the start: nothing to swap");
        assertEquals(10, UltimateAnim.START_TICKS);
    }

    @Test
    void theKingdomIsTheHeritageThenTheCover() {
        assertEquals(Kingdom.SPADE, UltimateAnim.resolveKingdom(Kingdom.SPADE, Kingdom.HEART));
        assertEquals(Kingdom.HEART, UltimateAnim.resolveKingdom(null, Kingdom.HEART));
        assertNull(UltimateAnim.resolveKingdom(null, null));
    }

    // ------------------------------------------------------------------ the real clips
    @Test
    void everyClipExistsAndDrivesRealBonesOnTheTickGrid() throws Exception {
        GeoAnim a = anims();
        for (String name : UltimateAnim.ids()) {
            GeoAnim.Clip c = a.clip(name);
            assertNotNull(c, name + " is not in cast.animation.json (run tools/gen_ultimate_anims.py)");
            assertTrue(c.length > 0.4f && c.length <= 10f, name + " length " + c.length);
            assertFalse(c.bones.isEmpty(), name);
            for (String bone : c.bones.keySet()) assertTrue(BONES.contains(bone), name + " bone " + bone);
            for (String bone : DRIVEN) assertTrue(c.bones.containsKey(bone), name + " does not drive " + bone);
        }
        // the keys are on the 20 per second grid and the angles are sane
        JsonObject all = json().getAsJsonObject("animations");
        for (String name : UltimateAnim.ids()) {
            for (var be : all.getAsJsonObject(name).getAsJsonObject("bones").entrySet()) {
                for (var ch : be.getValue().getAsJsonObject().entrySet()) {
                    for (var key : ch.getValue().getAsJsonObject().entrySet()) {
                        double ticks = Double.parseDouble(key.getKey()) * 20.0;
                        assertEquals(Math.rint(ticks), ticks, 1e-6, name + "/" + be.getKey() + " key " + key.getKey() + " is not on a game tick");
                        for (var v : key.getValue().getAsJsonArray()) assertTrue(Math.abs(v.getAsDouble()) <= 200, name + " angle " + v);
                    }
                }
            }
        }
    }

    @Test
    void theStartIsTenTicksOfMotionThatNeverWrapsAndEndsWhereTheLoopBegins() throws Exception {
        GeoAnim a = anims();
        for (Kingdom k : Kingdom.values()) {
            UltimateAnim.Clips ids = UltimateAnim.forKingdom(k);
            GeoAnim.Clip start = a.clip(ids.start()), loop = a.clip(ids.loop());
            assertTrue(start.loop, ids.start() + ": the start clip loops on purpose (the client only blends a new clip out of a looping one)");
            assertTrue(start.length >= 1.0f, ids.start() + ": long enough that the swap at tick " + UltimateAnim.START_TICKS + " (plus the 3 tick blend) never wraps it");
            float end = UltimateAnim.START_TICKS / 20f;
            for (String bone : DRIVEN) {
                float[] s = rot(start, bone, end), l = rot(loop, bone, 0f);
                for (int i = 0; i < 3; i++) assertEquals(l[i], s[i], 0.51f, ids.start() + " ends away from the loop at " + bone + "[" + i + "]");
                float[] hold = rot(start, bone, end + 3 / 20f);
                for (int i = 0; i < 3; i++) assertEquals(s[i], hold[i], 0.51f, ids.start() + " still moves while the hold blends in at " + bone);
            }
            // it really moves during the ten ticks (not a static pose)
            float moved = 0f;
            for (String bone : DRIVEN) {
                float[] p = rot(start, bone, 0f), q = rot(start, bone, end);
                for (int i = 0; i < 3; i++) moved = Math.max(moved, Math.abs(p[i] - q[i]));
            }
            assertTrue(moved >= 20f, ids.start() + " moves only " + moved + " degrees");
        }
    }

    @Test
    void theLoopClosesIsLongAndIsNeverFrozen() throws Exception {
        GeoAnim a = anims();
        for (Kingdom k : Kingdom.values()) {
            GeoAnim.Clip loop = a.clip(UltimateAnim.forKingdom(k).loop());
            assertTrue(loop.loop);
            assertTrue(loop.length >= 4f && loop.length <= 8f, loop.name + " length " + loop.length + ": a loop of 4 to 8 seconds, repeated for a 5 to 20 second hold");
            float frozen = 0f;
            for (String bone : DRIVEN) {
                float[] first = rot(loop, bone, 0f), last = rot(loop, bone, loop.length);
                for (int i = 0; i < 3; i++) assertEquals(first[i], last[i], 0.06f, loop.name + " does not close at " + bone + "[" + i + "]");
                for (int n = 0; n < 200; n++) {
                    float[] v = rot(loop, bone, loop.length * n / 200f);
                    for (int i = 0; i < 3; i++) frozen = Math.max(frozen, Math.abs(v[i] - first[i]));
                }
            }
            assertTrue(frozen >= 2.5f, loop.name + " moves only " + frozen + " degrees: it would look frozen");
            assertTrue(frozen <= 45f, loop.name + " moves " + frozen + " degrees: that is not a hold");
            // no sudden jump inside the loop: at most 12 degrees between two game ticks
            for (String bone : DRIVEN) {
                float[] prev = rot(loop, bone, 0f);
                for (int tick = 1; tick <= Math.round(loop.length * 20); tick++) {
                    float[] v = rot(loop, bone, tick / 20f);
                    for (int i = 0; i < 3; i++) assertTrue(Math.abs(v[i] - prev[i]) <= 12f, loop.name + " jumps at tick " + tick + " " + bone);
                    prev = v;
                }
            }
        }
    }

    @Test
    void theReleaseFitsInTheTwentyTicksOfThePulsesAndEndsRelaxed() throws Exception {
        GeoAnim a = anims();
        for (Kingdom k : Kingdom.values()) {
            GeoAnim.Clip rel = a.clip(UltimateAnim.forKingdom(k).release());
            assertFalse(rel.loop, rel.name + " is a one-shot");
            assertTrue(rel.length <= 1.0f && rel.length >= 0.6f, rel.name + " length " + rel.length + ": the stop comes 20 ticks after the release");
            for (String bone : new String[]{"left_arm", "right_arm"}) {
                float[] end = rot(rel, bone, rel.length);
                assertTrue(Math.abs(end[0]) <= 8f && Math.abs(end[1]) <= 8f && Math.abs(end[2]) <= 12f, rel.name + " ends with " + bone + " not relaxed");
            }
            // the contact (the biggest move) is in the first third of the clip
            float biggest = 0f, at = 0f;
            for (int tick = 1; tick <= Math.round(rel.length * 20); tick++) {
                float t0 = (tick - 1) / 20f, t1 = tick / 20f, d = 0f;
                for (String bone : DRIVEN) {
                    float[] p = rot(rel, bone, t0), q = rot(rel, bone, t1);
                    for (int i = 0; i < 3; i++) d += Math.abs(q[i] - p[i]);
                }
                if (d > biggest) { biggest = d; at = t1; }
            }
            assertTrue(at <= 0.35f, rel.name + ": the fastest move is at " + at + " s, it should land at the release");
        }
    }

    @Test
    void theFourKingdomsLookDifferent() throws Exception {
        GeoAnim a = anims();
        List<Kingdom> ks = List.of(Kingdom.values());
        for (int i = 0; i < ks.size(); i++) {
            for (int j = i + 1; j < ks.size(); j++) {
                GeoAnim.Clip p = a.clip(UltimateAnim.forKingdom(ks.get(i)).loop()), q = a.clip(UltimateAnim.forKingdom(ks.get(j)).loop());
                float d = 0f;
                for (String bone : new String[]{"left_arm", "right_arm"}) {
                    float[] u = rot(p, bone, 0f), v = rot(q, bone, 0f);
                    for (int n = 0; n < 3; n++) d += Math.abs(u[n] - v[n]);
                }
                assertTrue(d >= 40f, ks.get(i) + " and " + ks.get(j) + " hold nearly the same arms (" + d + " degrees apart)");
            }
        }
    }

    @Test
    void theOldClipsAreStillThere() throws Exception {
        GeoAnim a = anims();
        for (String name : new String[]{"cast_chant", "cast_release_thrust", "cast_release_sweep", "cast_release_side", "cast_release_up", "cast_release_slam", "cast_mana_zone", "cast_signature", "cast_fail",
                "seal_crush", "cast_weapon_sword", "cast_weapon_heavy", "cast_weapon_katana", "cast_weapon_dance", "dance_sway", "dance_spin", "dance_ripple", "dance_crouch_wave", "dance_lunge_feint", "dance_fold",
                "dance_drift", "dance_rise", "fire_bat_swing", "fire_punch_a", "fire_punch_b", "fire_punch_finisher", "fire_charge", "fire_charge_release"})
            assertNotNull(a.clip(name), "the old clip " + name + " was lost");
        assertTrue(a.clip("cast_mana_zone").loop);
        assertTrue(new File("tools/gen_ultimate_anims.py").isFile());
        assertTrue(new File("tools/ultimate_anim_preview.py").isFile());
    }
}
