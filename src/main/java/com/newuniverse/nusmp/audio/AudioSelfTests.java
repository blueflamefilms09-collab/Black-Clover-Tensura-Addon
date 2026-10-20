package com.newuniverse.nusmp.audio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryData;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryDef;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing here can be heard by the author, so the in-game self-test {@code audio.mob_sounds} (run by the CI server smoke test) checks what a server can: every event of the
 * catalogue ({@link MobSoundSet}) is in the sound-event registry and is the very event {@link ModSounds} hands out; {@code sounds.json} is on the class path with an entry for each of them
 * whose files are all there ({@code assets/nusmp/sounds/mob/...}); every mob of the Foundry roster resolves to a family of the catalogue and every shipped boss profile to one too;
 * and the vanilla-voice remap reads the sounds it must (a zombie's groan, a wolf's howl) and leaves the ones it must not (a bow, a door). Never throws: a broken part is a failed result.
 */
public final class AudioSelfTests {
    private AudioSelfTests() {}

    public static void register() {
        SelfTests.register("audio.mob_sounds", AudioSelfTests::mobSounds);
    }

    private static SelfTests.Result mobSounds(MinecraftServer server) throws Exception {
        List<String> bad = new ArrayList<>();
        int events = 0, files = 0;
        for (MobSoundSet.Family f : MobSoundSet.families()) {
            for (Cue c : Cue.values()) {
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, f.event(c));
                SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(rl);
                if (registered == null || !BuiltInRegistries.SOUND_EVENT.containsKey(rl)) bad.add("not registered: " + rl);
                else if (registered != ModSounds.event(f.id(), c)) bad.add("ModSounds hands out another event for " + rl);
                events++;
            }
        }
        if (events < 160) bad.add("only " + events + " events (the spec asks for at least 8 cues x 20 families)");

        JsonObject doc;
        try (Reader in = new InputStreamReader(AudioSelfTests.class.getResourceAsStream("/assets/nusmp/sounds.json"), StandardCharsets.UTF_8)) {
            doc = JsonParser.parseReader(in).getAsJsonObject();
        } catch (NullPointerException | IllegalStateException ex) {
            return SelfTests.Result.fail("assets/nusmp/sounds.json is not on the class path or not an object");
        }
        for (String id : MobSoundSet.allEventIds()) {
            JsonElement entry = doc.get(id);
            if (entry == null || !entry.isJsonObject()) {
                bad.add("sounds.json has no entry " + id);
                continue;
            }
            JsonArray sounds = entry.getAsJsonObject().getAsJsonArray("sounds");
            if (sounds == null || sounds.isEmpty()) {
                bad.add("sounds.json: " + id + " has no sounds");
                continue;
            }
            for (JsonElement s : sounds) {
                String name = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
                String path = "/assets/" + name.replace(':', '/').replaceFirst("/", "/sounds/") + ".ogg";
                if (AudioSelfTests.class.getResource(path) == null) bad.add("missing file " + path);
                files++;
            }
        }

        Set<String> usedFamilies = new LinkedHashSet<>();
        int mobs = 0;
        try {
            for (FoundryDef d : FoundryData.roster().all()) {
                String fam = MobSoundSet.forFoundry(d.faction(), d.id());
                if (!MobSoundSet.has(fam)) bad.add("Foundry mob " + d.id() + " has no family (" + fam + ")");
                usedFamilies.add(fam);
                mobs++;
            }
        } catch (RuntimeException ex) {
            bad.add("the Foundry roster could not be read: " + ex.getMessage());
        }
        int bosses = 0;
        for (DndProfile p : DndProfiles.all()) {
            String fam = BossSoundSets.family(p.id(), p.look(), p.name(), p.dnd(), p.side());
            if (!MobSoundSet.has(fam)) bad.add("boss profile " + p.id() + " has no family (" + fam + ")");
            bosses++;
        }

        MobSoundSet.Voice zombie = MobSoundSet.parseVanillaVoice("entity.zombie.ambient");
        if (zombie == null || zombie.cue() != Cue.AMBIENT) bad.add("the remap does not read entity.zombie.ambient");
        if (MobSoundSet.parseVanillaVoice("entity.wolf.howl") == null) bad.add("the remap does not read entity.wolf.howl");
        if (MobSoundSet.parseVanillaVoice("entity.arrow.shoot") != null || MobSoundSet.parseVanillaVoice("block.wooden_door.open") != null) bad.add("the remap reads a sound that is not a voice");

        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problem(s): " + bad.get(0));
        return SelfTests.Result.ok(events + " events registered, " + files + " files on the class path, " + mobs + " Foundry mobs in " + usedFamilies.size() + " families, " + bosses
                + " boss profiles resolved, the vanilla-voice remap reads what it must");
    }
}
