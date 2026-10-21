package com.newuniverse.nusmp.audio.voice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.NUSMP;
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
import java.util.UUID;

/**
 * Why: nothing here can be heard by the author, so the in-game self-test {@code audio.voices} (run by the CI server smoke test) checks what a server can: every line of the catalogue has
 * its sound event in the registry (and it is the event {@link VoiceSounds} hands out), its entry in {@code sounds.json} names an Ogg file that is on the class path, the payload survives
 * a round trip through the codec (and a forged id reads as empty), the picker answers lines that exist, and the limiter holds its cooldowns and its per-second budget. Never throws: a
 * broken part is a failed result.
 */
public final class VoiceSelfTests {
    private VoiceSelfTests() {}

    public static void register() {
        SelfTests.register("audio.voices", VoiceSelfTests::voices);
    }

    private static SelfTests.Result voices(MinecraftServer server) throws Exception {
        List<String> bad = new ArrayList<>();
        JsonObject doc;
        try (Reader in = new InputStreamReader(VoiceSelfTests.class.getResourceAsStream("/assets/nusmp/sounds.json"), StandardCharsets.UTF_8)) {
            doc = JsonParser.parseReader(in).getAsJsonObject();
        } catch (NullPointerException | IllegalStateException ex) {
            return SelfTests.Result.fail("assets/nusmp/sounds.json is not on the class path or not an object");
        }
        long bytes = 0;
        int files = 0;
        for (String line : VoiceCatalog.ids()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, VoiceCatalog.eventId(line));
            SoundEvent registered = BuiltInRegistries.SOUND_EVENT.get(rl);
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(rl)) bad.add("not registered: " + rl);
            else if (registered != VoiceSounds.event(line)) bad.add("VoiceSounds hands out another event for " + rl);
            JsonElement entry = doc.get(VoiceCatalog.eventId(line));
            if (entry == null || !entry.isJsonObject()) {
                bad.add("no sounds.json entry " + VoiceCatalog.eventId(line));
                continue;
            }
            String path = "/assets/nusmp/sounds/" + VoiceCatalog.filePath(line) + ".ogg";
            try (InputStream in = VoiceSelfTests.class.getResourceAsStream(path)) {
                if (in == null) {
                    bad.add("missing file " + path);
                    continue;
                }
                byte[] head = in.readNBytes(4);
                long size = head.length + in.transferTo(java.io.OutputStream.nullOutputStream());
                if (head.length < 4 || head[0] != 'O' || head[1] != 'g' || head[2] != 'g' || head[3] != 'S') bad.add(path + " is not an Ogg file");
                if (size < 2_000) bad.add(path + " is only " + size + " bytes");
                bytes += size;
                files++;
            }
        }

        for (String line : new String[]{VoiceCatalog.ids().get(0), VoiceCatalog.ids().get(VoiceCatalog.ids().size() - 1)}) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            VoiceNet.write(buf, new VoiceNet.Say(line));
            if (!line.equals(VoiceNet.read(buf).line())) bad.add("the payload changed on the way: " + line);
        }
        FriendlyByteBuf forged = new FriendlyByteBuf(Unpooled.buffer());
        forged.writeUtf("not_a_line", VoiceNet.MAX_ID);
        if (!VoiceNet.read(forged).line().isEmpty()) bad.add("a forged id must read as empty");

        if (VoicePicker.boss("holy", "intro") == null || VoicePicker.boss("not_a_style", "death") == null) bad.add("picker: a boss style must always find a family");
        if (VoicePicker.djinn("Glasya-Labolas", "summon") == null) bad.add("picker: Glasya-Labolas must resolve");
        if (VoicePicker.djinn("nobody", "summon") != null) bad.add("picker: an unknown Djinn must have no line");
        if (VoicePicker.system("floor_enter") == null || VoicePicker.npc(VoicePicker.NpcAct.GREET, true) == null) bad.add("picker: system / npc lines must resolve");

        VoiceLimiter lim = new VoiceLimiter(100);
        UUID a = UUID.nameUUIDFromBytes("a".getBytes(StandardCharsets.UTF_8)), b = UUID.nameUUIDFromBytes("b".getBytes(StandardCharsets.UTF_8));
        if (!lim.tryAcquire(a, "system_floor_enter", 0, 2)) bad.add("limiter: the first line must pass");
        if (lim.tryAcquire(a, "system_floor_enter", 50, 2)) bad.add("limiter: the same line inside its cooldown must be held back");
        if (!lim.tryAcquire(b, "system_floor_enter", 50, 2)) bad.add("limiter: another player is not held back by a first player's cooldown");
        if (!lim.tryAcquire(a, "system_floor_enter", 200, 2)) bad.add("limiter: the line must pass again after its cooldown");
        if (lim.tryAcquire(a, "system_rest_safe", 201, 0)) bad.add("limiter: maxPerSecond 0 means no lines");

        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problem(s): " + bad.subList(0, Math.min(5, bad.size())));
        return SelfTests.Result.ok(VoiceCatalog.ids().size() + " lines registered, " + files + " files present (" + (bytes / 1024) + " KB), payload, picker and limiter fine");
    }
}
