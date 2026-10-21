package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the holder of the room data that flags a challenge room ({@code data/nusmp/mega/lockdown/rooms.json}, parsed by {@link LockdownTable}) and the reload listener
 * that reads it again whenever data reloads and never crashes on a bad file (the problems go to the log with the file's name, the snapshot keeps what was good). The
 * snapshot is swapped whole through a volatile field, so the server thread never sees a half-read table. With {@code lockdown.roomData = false}, or when the file is
 * missing or empty, {@link #MINIMUM} answers instead: treasure and set-piece rooms lock, nothing else, so the feature never silently does nothing. A datapack overrides
 * the file by shipping the same path; the highest-priority pack wins.
 */
public final class LockdownRooms {
    private LockdownRooms() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String FILE = "mega/lockdown/rooms.json";

    /** The built-in minimum: the rooms nobody would argue are challenge rooms. */
    public static final LockdownTable MINIMUM = new LockdownTable(
            Map.of("treasure", new LockdownRule(true, 1.0, Band.STANDARD, 1, 1, 100, "a guarded treasure room"),
                    "set", new LockdownRule(true, 1.0, Band.HARD, 2, 1, 100, "a set-piece hall")),
            Map.of(), List.of());

    private static volatile LockdownTable table = LockdownTable.EMPTY;
    private static volatile int version;

    /** The table in force: the file's, or the minimum when the file gave nothing or the config says to ignore it. */
    public static LockdownTable table() {
        LockdownTable t = table;
        return t.isEmpty() || !LockdownConfig.get(LockdownConfig.ROOM_DATA) ? MINIMUM : t;
    }

    /** Counts up on every reload. */
    public static int version() { return version; }

    /** The rule of a room (its template, a pattern, its kind). */
    public static LockdownRule ruleFor(String template, RoomKind kind) { return table().ruleFor(template, kind); }

    /** Replaces the snapshot (the reload listener and the tests of the glue). */
    static void install(LockdownTable t) {
        table = t == null ? LockdownTable.EMPTY : t;
        version++;
    }

    public static final class Reload implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            LockdownTable found = LockdownTable.EMPTY;
            try {
                Optional<Resource> res = manager.getResource(ResourceLocation.fromNamespaceAndPath("nusmp", FILE));
                if (res.isPresent()) {
                    String text = read(res.get());
                    if (text != null) {
                        LockdownTable.Parsed p = LockdownTable.parse(FILE, text);
                        for (String problem : p.problems()) LOG.warn("[nusmp] {}", problem);
                        found = p.table();
                    }
                } else {
                    LOG.warn("[nusmp] {} is missing: the built-in minimum applies (treasure and set-piece rooms lock)", FILE);
                }
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] The lockdown room data could not be read: {}", e.toString());
            }
            install(found);
            LOG.info("[nusmp] Lockdown: {} kind rules, {} template rules, {} name patterns", found.kinds().size(), found.templates().size(), found.patterns().size());
        }

        private static String read(Resource res) {
            try (InputStream in = res.open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException e) {
                LOG.warn("[nusmp] {} could not be read: {}", FILE, e.toString());
                return null;
            }
        }
    }
}
