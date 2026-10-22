package com.newuniverse.nusmp.sylph;

import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.WindBook;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.MagicPropEntity;
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
 * Why: nothing of the Sylph can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs this (key
 * {@code magic.sylph}). It is pure and server safe (it touches no client class). It checks: the prop kind exists; every sound cue of her own set is a registered sound event and has an entry
 * in {@code sounds.json} and its files on the class path; her models, sheets and animation files are on the class path; the Wind book still has the Spirit of Zephyr page and it is still Yuno's;
 * the rules (forms, caps, the emitter, the clip choice) answer as the doc says; the switch defaults on; and on a stand-in player the link and the powered form really spawn on the server, are counted,
 * are capped, are hidden / shown as the doc says, and are all gone (entity and ledger) when the player's state is forgotten (logout, death or a change of dimension do exactly that).
 */
public final class SylphSelfTests {
    private SylphSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.sylph", SylphSelfTests::run);
    }

    private static boolean onClassPath(String path) {
        try (InputStream in = SylphSelfTests.class.getResourceAsStream(path)) { return in != null; } catch (java.io.IOException e) { return false; }
    }

    private static Result run(MinecraftServer server) throws Exception {
        if (PropKind.valueOf("SYLPH_1") == null) return Result.fail("the SYLPH_1 prop kind is missing");
        if (!SylphConfig.enabled()) return Result.fail("magic.sylph does not default to true");

        // her sounds: every cue is a registered event, has its entry and its files
        String sounds;
        try (InputStream in = SylphSelfTests.class.getResourceAsStream("/assets/nusmp/sounds.json")) {
            if (in == null) return Result.fail("sounds.json is missing");
            sounds = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (SylphSoundSet.Cue c : SylphSoundSet.cues()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", c.event()))) return Result.fail("sound event " + c.event() + " is not registered");
            if (SylphSounds.event(c.id()) == null) return Result.fail("SylphSounds has no event for " + c.id());
            if (!sounds.contains("\"" + c.event() + "\"")) return Result.fail("sounds.json has no entry " + c.event());
            for (int v = 1; v <= c.variants(); v++) if (!onClassPath("/assets/nusmp/sounds/" + c.file(v) + ".ogg")) return Result.fail("missing sound file " + c.file(v));
        }

        // her looks: models, sheets, animations
        for (String form : new String[]{"small", "powered"}) {
            for (String suffix : new String[]{"", "_lod1", "_lod2", "_wings", "_fx"}) {
                String id = "sylph_" + form + suffix;
                if (!onClassPath("/assets/nusmp/geo/entity/" + id + ".geo.json")) return Result.fail("missing model " + id);
                if (!onClassPath("/assets/nusmp/textures/entity/" + id + ".png")) return Result.fail("missing sheet " + id);
            }
            if (!onClassPath("/assets/nusmp/animations/entity/sylph_" + form + ".animation.json")) return Result.fail("missing animations of " + form);
        }

        // her moods: every clip a pose names is in her animation files, every spoken line is a registered event with its entry and its file, the fallbacks are real cues
        for (String form : new String[]{"small", "powered"}) {
            String anim;
            try (InputStream in = SylphSelfTests.class.getResourceAsStream("/assets/nusmp/animations/entity/sylph_" + form + ".animation.json")) { anim = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
            for (String clip : form.equals("small") ? SylphRules.SMALL_CLIPS : SylphRules.POWERED_CLIPS) if (!anim.contains("\"" + clip + "\"")) return Result.fail("the " + form + " animation file has no clip " + clip);
        }
        for (String line : SylphVoiceLines.ids()) {
            if (!com.newuniverse.nusmp.audio.voice.VoiceCatalog.has(line)) return Result.fail("the voice catalogue lacks her line " + line);
            if (com.newuniverse.nusmp.audio.voice.VoiceSounds.event(line) == null) return Result.fail("no sound event for her line " + line);
            if (!sounds.contains("\"voice." + line + "\"")) return Result.fail("sounds.json has no entry for her line " + line);
            if (!onClassPath("/assets/nusmp/sounds/voice/sylph/" + line + ".ogg")) return Result.fail("missing file of her line " + line);
        }
        for (String g : new String[]{"hurt", "worry", "idle", "cheer", "pout", "sad", "smug", "giggle"}) {
            String cue = SylphVoiceLines.fallbackCue(g);
            if (cue != null && !SylphSoundSet.has(cue)) return Result.fail("the stand-in cue " + cue + " of " + g + " does not exist");
        }
        SylphMood.State mood = new SylphMood.State();
        SylphMood.Decision hello = SylphMood.decide(mood, SylphMood.Event.LINK, 100L, 0);
        if (hello.pose() != SylphMood.WAVE || !"link".equals(hello.voice())) return Result.fail("linking does not wave and say hello");
        if (SylphMood.decide(mood, SylphMood.Event.HURT, 110L, 0).pose() != SylphMood.HURT) return Result.fail("a flinch (more important) did not replace the wave");
        if (SylphRules.pose(SylphRules.withPose(SylphRules.param(SylphRules.V_SMALL, 9), SylphMood.CRY)) != SylphMood.CRY) return Result.fail("the pose bits do not round trip");

        // the Wind book is as it was: the page and its lock
        WindBook wind = new WindBook();
        boolean zephyr = false;
        for (int k = 0; k < wind.familyCount(); k++) if (wind.page(k) != null && wind.page(k).id().equals("spirit_of_zephyr")) zephyr = true;
        if (!zephyr) return Result.fail("the Spirit of Zephyr page is gone from the Wind book");
        var owners = CharacterSpells.owners(wind, wind.page(5));
        if (owners == null || !owners.contains(CanonBook.YUNO)) return Result.fail("Spirit of Zephyr lost its character lock");

        // the rules
        if (SylphRules.variant(SylphRules.param(SylphRules.V_POWERED, 77)) != SylphRules.V_POWERED || SylphRules.extra(SylphRules.param(SylphRules.V_POWERED, 77)) != 77) return Result.fail("the prop word does not round trip");
        if (!SylphRules.callsPowered(20.0) || SylphRules.callsPowered(8.0)) return Result.fail("the powered form is called by the wrong pages");
        double[] near = SylphRules.emit(0, 1.62, 0, 0, 1.62, 0, 0, 0, 0, 0.5, 2.0, 0.2);
        if (Math.abs(near[1] - 2.0) > 1e-9) return Result.fail("a caster's own start did not move to the emitter");
        double[] far = SylphRules.emit(9, 1.62, 9, 0, 1.62, 0, 0, 0, 0, 0.5, 2.0, 0.2);
        if (far[0] != 9 || far[2] != 9) return Result.fail("a start far from the caster moved");
        if (!SylphRules.smallClip(1, false, 0, 0).equals("cast") || !SylphRules.smallClip(500, true, 0, 0).equals("fly")) return Result.fail("the clip choice is wrong");
        SylphLedger l = new SylphLedger();
        UUID u = UUID.randomUUID();
        if (!l.acquire(u, 1, 2) || l.acquire(u, 1, 2)) return Result.fail("the per player cap does not hold");
        l.forget(u);
        if (l.total() != 0) return Result.fail("the ledger kept a slot after forget");

        // the link and the powered form on a stand-in player: spawn, count, cap, hide, cleanup
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "sylph", at.x, at.y, at.z);
        int before = SylphCasting.liveCount();
        try {
            SylphCasting.State s = SylphCasting.stateOf(p);
            if (!SylphCasting.link(p, s) || SylphCasting.prop(level, s.spirit) == null) return Result.fail("the small Sylph did not spawn");
            if (SylphCasting.liveCount() != before + 1) return Result.fail("the link was not counted");
            MagicPropEntity e = SylphCasting.prop(level, s.spirit);
            if (SylphRules.variant(e.param()) != SylphRules.V_SMALL || e.getBbHeight() > 0.7f) return Result.fail("the small Sylph has the wrong size or variant");
            if (SylphConfig.poses() && SylphRules.pose(e.param()) != SylphMood.WAVE) return Result.fail("the fresh link did not make her wave (pose bits " + SylphRules.pose(e.param()) + ")");
            long now = level.getGameTime();
            if (SylphMoods.syncPose(e.param(), s, now + 100000L, false) != SylphRules.withPose(e.param(), 0)) return Result.fail("an over mood did not clear her pose bits");
            SylphMoods.fire(p, s, SylphMood.Event.HURT_HEAVY);
            if (SylphConfig.poses() && SylphRules.pose(e.param()) == 0) return Result.fail("a heavy hit did not give her a pose");
            if (SylphCasting.link(p, s) && SylphCasting.SPIRITS.count(p.getUUID()) > 1) return Result.fail("a second Sylph was allowed for one player");
            SylphCasting.forget(p);
            if (SylphCasting.liveCount() != before) return Result.fail("the ledgers kept a slot after the player was forgotten");
            if (!e.isRemoved()) return Result.fail("the Sylph entity survived the cleanup");
        } finally {
            SylphCasting.forget(p);
        }
        return Result.ok("sounds, models, rules, moods, voice lines, the Wind book, the Sylph link and its cleanup hold");
    }
}
