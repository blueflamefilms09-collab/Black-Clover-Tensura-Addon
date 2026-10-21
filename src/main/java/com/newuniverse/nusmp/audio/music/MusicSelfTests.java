package com.newuniverse.nusmp.audio.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing here can be heard by the author, so the in-game self-test {@code dungeon.music} (run by the CI server smoke test) checks what a server can: every track of the
 * catalogue has its sound event in the registry (and it is the event {@link MusicSounds} hands out), its entry in {@code sounds.json} names an Ogg file that is on the class path,
 * the music is within its size budget, every one of the Region Themes maps to a family whose explore and battle loops exist, the layer rules and the cross-fade director behave
 * (a fight escalates quickly, a calm waits, a fade ends with one voice), and the state packet survives a round trip through the codec. Never throws: a broken part is a failed result.
 */
public final class MusicSelfTests {
    private MusicSelfTests() {}

    /** The budget of the whole music set, in bytes. */
    public static final long BUDGET_BYTES = 25L * 1024 * 1024;

    public static void register() {
        SelfTests.register("dungeon.music", MusicSelfTests::music);
    }

    private static SelfTests.Result music(MinecraftServer server) throws Exception {
        List<String> bad = new ArrayList<>();
        int events = 0;
        for (String track : MusicCatalog.trackNames()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, MusicCatalog.eventId(track));
            SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(rl);
            if (registered == null || !BuiltInRegistries.SOUND_EVENT.containsKey(rl)) bad.add("not registered: " + rl);
            else if (registered != MusicSounds.event(track)) bad.add("MusicSounds hands out another event for " + rl);
            events++;
        }

        JsonObject doc;
        try (Reader in = new InputStreamReader(MusicSelfTests.class.getResourceAsStream("/assets/nusmp/sounds.json"), StandardCharsets.UTF_8)) {
            doc = JsonParser.parseReader(in).getAsJsonObject();
        } catch (NullPointerException | IllegalStateException ex) {
            return SelfTests.Result.fail("assets/nusmp/sounds.json is not on the class path or not an object");
        }
        long bytes = 0;
        for (String track : MusicCatalog.trackNames()) {
            String id = MusicCatalog.eventId(track);
            JsonElement entry = doc.get(id);
            if (entry == null || !entry.isJsonObject()) {
                bad.add("no sounds.json entry " + id);
                continue;
            }
            JsonArray sounds = entry.getAsJsonObject().getAsJsonArray("sounds");
            if (sounds == null || sounds.isEmpty()) {
                bad.add(id + ": no sounds");
                continue;
            }
            for (JsonElement s : sounds) {
                String name = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
                String path = "/assets/nusmp/sounds/" + name.substring(name.indexOf(':') + 1) + ".ogg";
                try (InputStream in = MusicSelfTests.class.getResourceAsStream(path)) {
                    if (in == null) {
                        bad.add(id + ": missing file " + path);
                        continue;
                    }
                    byte[] head = in.readNBytes(4);
                    long size = head.length + in.transferTo(java.io.OutputStream.nullOutputStream());
                    if (head.length < 4 || head[0] != 'O' || head[1] != 'g' || head[2] != 'g' || head[3] != 'S') bad.add(path + " is not an Ogg file");
                    if (size < 20_000) bad.add(path + " is only " + size + " bytes");
                    bytes += size;
                }
            }
        }
        if (bytes > BUDGET_BYTES) bad.add("the music takes " + bytes + " bytes, the budget is " + BUDGET_BYTES);

        for (RegionTheme t : RegionTheme.values()) {
            if (!MusicCatalog.themeTable().containsKey(t.id())) bad.add("theme " + t.id() + " has no family in the table");
            String ex = MusicCatalog.trackFor(MusicLayer.EXPLORE, t.id(), 1), ba = MusicCatalog.trackFor(MusicLayer.BATTLE, t.id(), 1);
            if (!MusicCatalog.trackNames().contains(ex) || !MusicCatalog.trackNames().contains(ba)) bad.add("theme " + t.id() + " maps to a track that does not exist");
        }

        if (MusicRules.decide(true, true, true, true, 9) != MusicLayer.HUB) bad.add("rules: the hub must win");
        if (MusicRules.decide(false, true, false, true, 0) != MusicLayer.BOSS) bad.add("rules: a boss must beat a safe room");
        if (MusicRules.decide(false, false, false, true, 3) != MusicLayer.SAFE) bad.add("rules: a safe room stays safe");
        if (MusicRules.decide(false, false, false, false, 1) != MusicLayer.BATTLE) bad.add("rules: a hunter means a fight");
        if (MusicRules.decide(false, false, false, false, 0) != MusicLayer.EXPLORE) bad.add("rules: nothing happening means explore");

        MusicDirector d = new MusicDirector(MusicDirector.Params.ofSeconds(2.0, 5.0));
        long t = 0;
        String calm = "stone_explore", fight = "stone_battle";
        for (int i = 0; i < 100; i++) d.tick(t++, calm, MusicLayer.EXPLORE.priority());
        if (d.voices().size() != 1 || d.voices().get(0).level() < 0.99f) bad.add("director: a lone track must settle at full level");
        for (int i = 0; i < 30; i++) d.tick(t++, fight, MusicLayer.BATTLE.priority());
        if (!fight.equals(d.currentTrack())) bad.add("director: a fight must take over within a second and a half");
        for (int i = 0; i < 60; i++) d.tick(t++, calm, MusicLayer.EXPLORE.priority());
        if (!fight.equals(d.currentTrack())) bad.add("director: the fight must linger after the last hunter");
        for (int i = 0; i < 400; i++) d.tick(t++, calm, MusicLayer.EXPLORE.priority());
        if (!calm.equals(d.currentTrack()) || d.voices().size() != 1) bad.add("director: the calm track must come back alone");
        for (int i = 0; i < 200; i++) d.tick(t++, null, 0);
        if (d.isActive()) bad.add("director: silence must end with no voice");

        MusicState in = new MusicState(MusicLayer.BOSS, "holy_church", -1);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MusicNet.write(buf, new MusicNet.State(in));
        MusicState out = MusicNet.read(buf).state();
        if (!in.equals(out)) bad.add("the state packet changed on the way: " + in + " -> " + out);

        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problem(s): " + bad.subList(0, Math.min(5, bad.size())));
        return SelfTests.Result.ok(events + " music events registered, files " + (bytes / 1024) + " KB, " + RegionTheme.values().length + " themes mapped, director and packet fine");
    }
}
