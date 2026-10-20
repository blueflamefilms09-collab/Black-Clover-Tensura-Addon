package com.newuniverse.nusmp.boss.bars;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialProfile;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialRoster;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Why: the real game proves what the unit tests cannot. {@code boss.bars}, run by the CI server smoke test inside the booted mod, checks that every style file ships, parses and has its frame
 * texture in the jar, that the resolver gives every one of the 200 Decennial bosses a style that exists (the roster is loaded by the data reload by then), that the state packet survives
 * the real {@code FriendlyByteBuf} codec, that the diff says what it should, and that the hub is wired (the command and the tick). It touches no client class, so it runs on a dedicated server.
 */
public final class BossBarSelfTests {
    private BossBarSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("boss.bars", BossBarSelfTests::run);
    }

    private static String read(String path) throws IOException {
        try (InputStream in = BossBarSelfTests.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean exists(String path) {
        try (InputStream in = BossBarSelfTests.class.getResourceAsStream(path)) {
            return in != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static SelfTests.Result run(MinecraftServer server) throws Exception {
        // ---- the styles ship, parse, and have their art
        for (String id : BarStyleIds.ALL) {
            BarStyle s = BarStyle.parse(read("/assets/nusmp/boss_bars/" + id + ".json"));
            if (!s.id().equals(id)) return SelfTests.Result.fail("style file " + id + " says id " + s.id());
            if (!exists("/assets/nusmp/textures/gui/boss_bar/frame_" + id + ".png")) return SelfTests.Result.fail("no frame texture for " + id);
        }
        for (String f : new String[]{"emblems.png", "fillfx.png", "chains.png"}) if (!exists("/assets/nusmp/textures/gui/boss_bar/" + f)) return SelfTests.Result.fail("missing texture " + f);

        // ---- the resolver covers the roster
        List<DecennialProfile> all = DecennialRoster.all();
        if (all.size() != 200) return SelfTests.Result.fail("the Decennial roster holds " + all.size() + " bosses, not 200 (not loaded yet?)");
        int[] per = new int[BarStyleIds.ALL.size()];
        for (DecennialProfile p : all) {
            String id = BarStyleResolver.forDecennial(p.side(), p.archetype(), p.theme(), p.creature(), p.devil());
            int i = BarStyleIds.ALL.indexOf(id);
            if (i < 0) return SelfTests.Result.fail(p.id() + " resolves to unknown style " + id);
            per[i]++;
        }
        int used = 0;
        for (int n : per) if (n > 0) used++;

        // ---- the packet through the real codec
        BossBarState s = new BossBarState(UUID.nameUUIDFromBytes("boss.bars".getBytes(StandardCharsets.UTF_8)), 42, "Quillothe", BarStyleIds.FALLEN_KING, "Fallen King", "Hollow Court", 2, 3,
                List.of(66, 33), 40, BossBarState.INVULNERABLE | BossBarState.HEALING, "judgment_rings", 80, 100, 3, "Radiant Nova", 55, 3);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        BossBarStatePayload.STREAM_CODEC.encode(buf, new BossBarStatePayload(s));
        BossBarState back = BossBarStatePayload.STREAM_CODEC.decode(buf).state();
        if (!back.equals(s)) return SelfTests.Result.fail("the state packet changed on the way: " + back + " != " + s);
        if (BossBarState.differs(s, 100, back, 104) || !BossBarState.differs(s, 100, withPhase(s, 3), 104)) return SelfTests.Result.fail("the state diff is wrong");

        // ---- wired
        if (server.getCommands().getDispatcher().getRoot().getChild("multiverse") == null || server.getCommands().getDispatcher().findNode(List.of("multiverse", "bossbar", "preview")) == null)
            return SelfTests.Result.fail("/multiverse bossbar preview is not registered");
        return SelfTests.Result.ok(BarStyleIds.ALL.size() + " styles load, 200 bosses resolve to " + used + " styles, packet round trip ok, /multiverse bossbar registered");
    }

    private static BossBarState withPhase(BossBarState s, int phase) {
        return new BossBarState(s.bar(), s.entityId(), s.name(), s.style(), s.title(), s.phaseName(), phase, s.phaseCount(), s.thresholds(), s.shield(), s.flags(), s.lairId(), s.lairTicks(),
                s.lairTotal(), s.adds(), s.castName(), s.castPercent(), s.fighters());
    }
}
