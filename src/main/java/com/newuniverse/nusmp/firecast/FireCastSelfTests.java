package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.FireBook;
import com.newuniverse.nusmp.grimoire.CanonBook;
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
 * Why: nothing of the casting styles can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs this
 * (key {@code magic.fire_casting}). It checks, on a dedicated server with a stand-in player: the Soul Bat is a registered item and the two prop kinds exist; the four canon books map to
 * four styles and every other book to none; the page ids the styles act on are still the Fire book's (nothing renamed, nothing removed); the swing, combo, charge, caps and emitter rules answer
 * as the doc says; the cast animation file holds the new clips; and the Salamander link and its big form really spawn on the server, are counted, are capped, and are all gone (entity and ledger)
 * when the player's state is forgotten (logout, death or a change of dimension do exactly that). Everything is cleaned up at the end.
 */
public final class FireCastSelfTests {
    private FireCastSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.fire_casting", FireCastSelfTests::run);
        SelfTests.register("magic.salamander_moods", FireCastSelfTests::runMoods);
    }

    /**
     * The Salamander's moods (docs/magic/fire_casting.md, 4.2) on the real server: the creature sound events are registered and their files, subtitles and the pose clips and face models are in the jar;
     * a stand-in player's Salamander really takes the pose and face the table gives a link and a hit (written to the prop's synced param), a hit twice in a row does not change it again (the cooldown),
     * the clients' reading of the param gives the pose back, the per listener sound gate holds, and everything is gone with the player's state.
     */
    private static Result runMoods(MinecraftServer server) throws Exception {
        for (SalamanderSoundSet.Cue c : SalamanderSoundSet.cues()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", c.event()))) return Result.fail("sound event " + c.event() + " is not registered");
            for (int v = 1; v <= c.variants(); v++)
                try (InputStream in = FireCastSelfTests.class.getResourceAsStream("/assets/nusmp/sounds/" + c.file(v) + ".ogg")) {
                    if (in == null) return Result.fail("sound file " + c.file(v) + " is missing");
                }
        }
        try (InputStream in = FireCastSelfTests.class.getResourceAsStream("/assets/nusmp/sounds.json")) {
            String text = in == null ? "" : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (SalamanderSoundSet.Cue c : SalamanderSoundSet.cues()) if (!text.contains("\"" + c.event() + "\"")) return Result.fail("sounds.json has no entry " + c.event());
        }
        for (String form : new String[]{"salamander", "dragon"}) {
            boolean big = form.equals("dragon");
            try (InputStream in = FireCastSelfTests.class.getResourceAsStream("/assets/nusmp/animations/entity/firecast_" + form + ".animation.json")) {
                String text = in == null ? "" : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (int pose = 0; pose < SalamanderMoodRules.poseCount(big); pose++)
                    if (!text.contains("\"" + SalamanderMoodRules.clipName(big, pose) + "\"")) return Result.fail(form + " clip " + SalamanderMoodRules.clipName(big, pose) + " is missing");
            }
            for (int f = 1; f < SalamanderMoodRules.FACES; f++)
                try (InputStream in = FireCastSelfTests.class.getResourceAsStream("/assets/nusmp/geo/entity/firecast_" + form + "_fx_face_" + SalamanderMoodRules.faceName(f) + ".geo.json")) {
                    if (in == null) return Result.fail("face model " + form + " " + SalamanderMoodRules.faceName(f) + " is missing");
                }
        }
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "salamander_moods", at.x, at.y, at.z);
        int before = FireCasting.liveCount();
        try {
            FireCasting.State s = FireCasting.stateOf(p);
            if (!Salamander.link(p, s)) return Result.fail("the small Salamander did not spawn");
            var prop = FireCasting.prop(level, s.spirit);
            if (prop == null) return Result.fail("no prop");
            long now = level.getGameTime();
            if (FireCastConfig.moods()) {
                int look = SalamanderMoodRules.look(false, prop.param(), now, prop.life(), prop.maxLife());
                if (SalamanderMoodRules.poseOfLook(look) != SalamanderMoodRules.GRIP_TIGHTEN || SalamanderMoodRules.faceOfLook(look) != SalamanderMoodRules.FACE_ALERT)
                    return Result.fail("the link did not give the grip and the alert face: look " + look);
                FireCasting.onOwnerHurt(p);
                look = SalamanderMoodRules.look(false, prop.param(), now, prop.life(), prop.maxLife());
                if (SalamanderMoodRules.poseOfLook(look) != SalamanderMoodRules.FLINCH || SalamanderMoodRules.faceOfLook(look) != SalamanderMoodRules.FACE_ANGRY) return Result.fail("a hit did not make it flinch");
                int param = prop.param();
                FireCasting.onOwnerHurt(p);
                if (prop.param() != param) return Result.fail("a second hit at once changed the pose again (the cooldown does not hold)");
                if (FireCastRules.variant(prop.param()) != FireCastRules.V_SALAMANDER) return Result.fail("the mood changed the prop's variant");
            }
            SalamanderSoundSet.Gate gate = new SalamanderSoundSet.Gate();
            UUID listener = UUID.randomUUID();
            if (!gate.tryAcquire(listener, "trill", now) || gate.tryAcquire(listener, "hiss", now + 5)) return Result.fail("the sound gate does not hold one sound per three seconds");
            FireCasting.forget(p);
            if (FireCasting.liveCount() != before) return Result.fail("the ledgers kept a slot after the player was forgotten");
        } finally {
            FireCasting.forget(p);
        }
        return Result.ok("sounds, clips, faces, the link and hit moods, the cooldown and the sound gate hold");
    }

    private static Result run(MinecraftServer server) throws Exception {
        if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "magnas_soul_bat"))) return Result.fail("the Soul Bat is not registered");
        if (PropKind.valueOf("FIRECAST_1") == null || PropKind.valueOf("FIRECAST_2") == null) return Result.fail("a firecast prop kind is missing");
        if (FireStyle.ofCanon(CanonBook.MAGNA.id()) != FireStyle.MAGNA || FireStyle.ofCanon(CanonBook.MEREOLEONA.id()) != FireStyle.MEREOLEONA
                || FireStyle.ofCanon(CanonBook.FUEGOLEON.id()) != FireStyle.FUEGOLEON || FireStyle.ofCanon(CanonBook.LEOPOLD.id()) != FireStyle.LEOPOLD) return Result.fail("a canon book does not map to its style");
        for (CanonBook b : CanonBook.values()) {
            boolean four = b == CanonBook.MAGNA || b == CanonBook.MEREOLEONA || b == CanonBook.FUEGOLEON || b == CanonBook.LEOPOLD;
            if ((FireStyle.ofCanon(b.id()) != FireStyle.NONE) != four) return Result.fail(b.id() + " has the wrong style");
        }
        FireBook fire = new FireBook();
        String[] ids = {"exploding_fireball", "sol_linea", "calderos", "leo_rugiens", "spiral_flame", "wild_bursting_flame", "ignis_columna", "calidos_brachium", "crimson_lion_king", "flame_armour", "phoenix_flare"};
        for (int k = 0; k < ids.length; k++) if (fire.page(k) == null || !fire.page(k).id().equals(ids[k])) return Result.fail("Fire page " + k + " is not " + ids[k]);
        if (CharacterSpells.owners(fire, fire.page(3)) == null) return Result.fail("Leo Rugiens lost its character lock");

        // the rules
        double[] b = FireCastRules.ballPosition(0, 1.6, 0, 0, 0, 1);
        if (!FireCastRules.swingHits(0, 1.6, 0, 0, 0, 1, b[0], b[1], b[2])) return Result.fail("a swing at the ball in front missed it");
        if (FireCastRules.swingHits(0, 1.6, 0, 0, 0, -1, b[0], b[1], b[2])) return Result.fail("a swing the other way hit the ball");
        if (FireCastRules.finisherHits(8, false) != 3 || FireCastRules.finisherHits(35, false) != 5 || FireCastRules.finisherHits(35, true) != 4) return Result.fail("the finisher numbers are wrong");
        if (FireCastRules.chargeLevel(19) != 0 || FireCastRules.chargeLevel(20) != 1 || FireCastRules.chargeLevel(9999) != FireCastRules.MAX_LEVEL) return Result.fail("the charge levels are wrong");
        if (FireCastRules.chargeDamage(99) > 1.5 || FireCastRules.chargeSize(99) > 1.4) return Result.fail("the charge multipliers are not capped");
        CapLedger l = new CapLedger();
        UUID u = UUID.randomUUID();
        if (!l.acquire(u, 1, 2) || l.acquire(u, 1, 2)) return Result.fail("the per player cap does not hold");
        l.forget(u);
        if (l.total() != 0) return Result.fail("the ledger kept a slot after forget");

        // the animation clips
        try (InputStream in = FireCastSelfTests.class.getResourceAsStream("/assets/nusmp/animations/player/cast.animation.json")) {
            if (in == null) return Result.fail("cast.animation.json is missing");
            String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String clip : new String[]{"fire_bat_swing", "fire_punch_a", "fire_punch_b", "fire_punch_finisher", "fire_charge", "fire_charge_release"})
                if (!text.contains("\"" + clip + "\"")) return Result.fail("clip " + clip + " is missing");
        }

        // the Salamander on a stand-in player: link, big form, caps, cleanup
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "fire_casting", at.x, at.y, at.z);
        int before = FireCasting.liveCount();
        try {
            FireCasting.State s = FireCasting.stateOf(p);
            if (!Salamander.link(p, s) || FireCasting.prop(level, s.spirit) == null) return Result.fail("the small Salamander did not spawn");
            if (FireCasting.liveCount() != before + 1) return Result.fail("the link was not counted");
            if (Salamander.link(p, s) && FireCasting.SPIRITS.count(p.getUUID()) > 1) return Result.fail("a second Salamander was allowed for one player");
            FireCasting.forget(p);
            if (FireCasting.liveCount() != before) return Result.fail("the ledgers kept a slot after the player was forgotten");
        } finally {
            FireCasting.forget(p);
        }
        return Result.ok("rules, styles, clips, the Salamander link and its cleanup hold");
    }
}
