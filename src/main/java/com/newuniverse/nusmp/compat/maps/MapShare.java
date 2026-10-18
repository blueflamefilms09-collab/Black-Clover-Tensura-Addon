package com.newuniverse.nusmp.compat.maps;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubKeys;
import com.newuniverse.nusmp.labyrinth.mega.rift.Waypoint;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the server side of waypoint sharing. At the moments a place matters (a Gate found, the Hub's gates, a Return Waypoint saved, a rift opened, a safe room found) it prints ONE
 * chat line that does three jobs: readable text with the coordinates, a click that copies the plain coordinates (for a player without any map mod), and, after it, the raw
 * Xaero's waypoint string ({@link WaypointShare}) which a client with Xaero's Minimap / World Map turns into an "add waypoint" button. The server cannot know which map mod a
 * client has, so the line goes to everybody who has sharing on. Opt-in twice: the server config {@code compat.maps.shareWaypoints} and the player's own switch
 * ({@code /multiverse waypoint share on|off}, a byte in the player's persistent data, which survives death). Automatic lines are rate limited ({@link ShareLimiter}: at most
 * {@code maxLinesPerMinute} per player) and a place is told once per player ({@link #KNOWN}). The explicit commands ({@code here}, {@code gate}, {@code return}) always answer.
 * Every automatic entry point swallows its own failure: a chat line must never break a gate, a rift or a rest. Server thread only.
 */
public final class MapShare {
    private MapShare() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** Player persistent data: byte, 0 = the player switched sharing off (absent = on). */
    public static final String PREF = "nusmp_maps_share";
    /** Player persistent data: list of strings, the places already told (oldest dropped past {@link #KNOWN_CAP}). */
    public static final String KNOWN = "nusmp_maps_known";
    public static final int KNOWN_CAP = 96;

    private static volatile ShareLimiter limiter = new ShareLimiter(4, 1200);

    /** One place to tell. {@code dim} is a dimension id such as {@code nusmp:mega_dungeon}. */
    public record Place(WaypointShare.Kind kind, String name, String dim, int x, int y, int z) {
        public String xaero() { return WaypointShare.xaero(name, x, y, z, kind, dim); }
    }

    // ---------------------------------------------------------------- switches
    public static boolean prefOn(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        return !d.contains(PREF) || d.getBoolean(PREF);
    }

    public static void setPref(ServerPlayer p, boolean on) { p.getPersistentData().putBoolean(PREF, on); }

    /** Does this player receive automatic lines? (Server config on, and the player's own switch on.) */
    public static boolean autoEnabled(ServerPlayer p) { return MapsConfig.get(MapsConfig.SHARE_WAYPOINTS) && prefOn(p); }

    // ---------------------------------------------------------------- the line
    /** The readable name of a dimension for a fallback (the translation key {@code dimension.<ns>.<path>} wins when the lang file has it). */
    static String dimFallback(String id) {
        return switch (id == null ? "" : id) {
            case "nusmp:mega_dungeon" -> "the Mega-Dungeon";
            case "nusmp:sanctuary" -> "Floor 0, the Hub";
            case "nusmp:labyrinth" -> "the Labyrinth";
            case "minecraft:overworld" -> "the Overworld";
            case "minecraft:the_nether" -> "the Nether";
            case "minecraft:the_end" -> "the End";
            default -> id == null ? "?" : id;
        };
    }

    private static Component dimLabel(String id) {
        int c = id == null ? -1 : id.indexOf(':');
        if (c <= 0) return Component.literal(String.valueOf(id));
        return Component.translatableWithFallback("dimension." + id.substring(0, c) + "." + id.substring(c + 1).replace('/', '.'), dimFallback(id));
    }

    /** The chat line for a place, as described in the class comment. */
    public static Component line(Place pl) {
        String coords = pl.x() + " " + pl.y() + " " + pl.z();
        Component hover = Component.translatableWithFallback("message.nusmp.maps.hover",
                "Click to copy the coordinates (%1$s). With Xaero's Minimap or World Map, click the grey text to add the waypoint.", coords);
        MutableComponent text = Component.translatableWithFallback("message.nusmp.maps.line", "%1$s at %2$s, %3$s, %4$s (%5$s)", pl.name(), pl.x(), pl.y(), pl.z(), dimLabel(pl.dim()))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coords))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        MutableComponent out = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatableWithFallback("message.nusmp.maps.tag", "Map").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(text);
        if (MapsConfig.get(MapsConfig.RAW_XAERO_STRING)) {
            String raw = pl.xaero();
            out.append(Component.literal(" ")).append(Component.literal(raw).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, raw))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatableWithFallback("message.nusmp.maps.hover_raw", "Click to copy the Xaero's waypoint text.")))));
        }
        return out;
    }

    /** The optional second line in JourneyMap's form, or null when that is switched off. */
    public static Component journeyMapLine(Place pl) {
        if (!MapsConfig.get(MapsConfig.SHARE_JOURNEYMAP_FORM)) return null;
        String jm = WaypointShare.journeyMap(pl.name(), pl.x(), pl.y(), pl.z(), pl.dim());
        return Component.literal(jm).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, jm)));
    }

    /** Sends the line for a place right now (no switch, no limit): what the commands use. */
    public static void send(ServerPlayer p, Place pl) {
        if (p.connection == null) return;
        p.sendSystemMessage(line(pl));
        Component jm = journeyMapLine(pl);
        if (jm != null) p.sendSystemMessage(jm);
    }

    // ---------------------------------------------------------------- the automatic path
    private static ShareLimiter limiter() {
        int max = MapsConfig.get(MapsConfig.MAX_LINES_PER_MINUTE);
        ShareLimiter l = limiter;
        if (l.max() != max) { l = new ShareLimiter(max, 1200); limiter = l; }
        return l;
    }

    public static void resetLimiter() { limiter().clear(); }

    private static List<String> knownList(ServerPlayer p) {
        ListTag lt = p.getPersistentData().getList(KNOWN, Tag.TAG_STRING);
        List<String> out = new ArrayList<>(lt.size());
        for (int i = 0; i < lt.size(); i++) out.add(lt.getString(i));
        return out;
    }

    static boolean isKnown(ServerPlayer p, String key) { return key != null && knownList(p).contains(key); }

    static boolean markKnown(ServerPlayer p, String key) {
        List<String> list = knownList(p);
        boolean fresh = ShareLimiter.remember(list, key, KNOWN_CAP);
        if (fresh) {
            ListTag out = new ListTag();
            for (String s : list) out.add(StringTag.valueOf(s));
            p.getPersistentData().put(KNOWN, out);
        }
        return fresh;
    }

    /**
     * The automatic path: sends the line when the server and the player allow it, the place has not been told before (when {@code rememberKey} is not null), and the player is under
     * the rate limit. True when a line was sent. Never throws.
     */
    public static boolean offer(ServerPlayer p, Place pl, String rememberKey) {
        try {
            if (p == null || p.connection == null || !autoEnabled(p)) return false;
            if (rememberKey != null && isKnown(p, rememberKey)) return false;
            if (!limiter().tryAcquire(p.getUUID(), p.serverLevel().getServer().getTickCount())) return false;
            send(p, pl);
            if (rememberKey != null) markKnown(p, rememberKey);
            return true;
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] map waypoint line failed (ignored)", ex);
            return false;
        }
    }

    // ---------------------------------------------------------------- the moments
    /** An Overworld (or other) Gateway the player has just used: told once per gate area. */
    public static void gate(ServerPlayer p, String dim, BlockPos pos) {
        try {
            offer(p, new Place(WaypointShare.Kind.GATE, "Labyrinth Gate", dim, pos.getX(), pos.getY(), pos.getZ()), "gate:" + dim + ":" + (pos.getX() >> 5) + ":" + (pos.getZ() >> 5));
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] map gate line failed (ignored)", ex);
        }
    }

    /** The three gates of Floor 0 (home, holy, abyssal), told once each. */
    public static void hubGates(ServerPlayer p) {
        try {
            for (Place pl : hubGatePlaces()) offer(p, pl, "hub:" + pl.name());
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] map hub line failed (ignored)", ex);
        }
    }

    public static List<Place> hubGatePlaces() {
        String dim = Labyrinth.SANCTUARY_DIMENSION.location().toString();
        return List.of(
                new Place(WaypointShare.Kind.HUB, "Hub: Home Gate", dim, HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z),
                new Place(WaypointShare.Kind.HUB, "Hub: Holy Gate", dim, HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z),
                new Place(WaypointShare.Kind.HUB, "Hub: Abyssal Gate", dim, HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z));
    }

    /** The Return Crystal saved a waypoint. Not remembered (it is a new place each time), only rate limited. */
    public static void returnWaypoint(ServerPlayer p, Waypoint w) {
        try {
            if (w == null) return;
            offer(p, placeOf(w), null);
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] map return line failed (ignored)", ex);
        }
    }

    public static Place placeOf(Waypoint w) {
        String name = w.floor() == 0 ? "Return Waypoint" : "Return Waypoint (floor " + w.floor() + ")";
        return new Place(WaypointShare.Kind.RETURN, name, w.dimension(), (int) Math.floor(w.x()), (int) Math.floor(w.y()), (int) Math.floor(w.z()));
    }

    /** A shortcut rift or a pocket rift opened near the player. */
    public static void rift(ServerPlayer p, BlockPos at, boolean pocket) {
        try {
            String dim = p.level().dimension().location().toString();
            offer(p, new Place(pocket ? WaypointShare.Kind.POCKET : WaypointShare.Kind.SHORTCUT, pocket ? "Pocket Rift" : "Shortcut Rift", dim, at.getX(), at.getY(), at.getZ()), null);
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] map rift line failed (ignored)", ex);
        }
    }

    /** The player has started to rest at a safe room's anchor: told once per anchor. */
    public static void safeRoom(ServerPlayer p, BlockPos at) {
        try {
            String dim = p.level().dimension().location().toString();
            offer(p, new Place(WaypointShare.Kind.SAFE, "Safe Room", dim, at.getX(), at.getY(), at.getZ()), "safe:" + dim + ":" + at.getX() + ":" + at.getY() + ":" + at.getZ());
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] map safe room line failed (ignored)", ex);
        }
    }

    // ---------------------------------------------------------------- what the commands print
    public static Place here(ServerPlayer p) {
        BlockPos b = p.blockPosition();
        return new Place(WaypointShare.Kind.HERE, "My position", p.level().dimension().location().toString(), b.getX(), b.getY(), b.getZ());
    }

    /** The gates this player knows: where they entered from (the Gateway), and the Hub's gates once they have been to Floor 0 or stand in it. */
    public static List<Place> knownGates(ServerPlayer p) {
        List<Place> out = new ArrayList<>();
        CompoundTag o = p.getPersistentData().getCompound(HubKeys.ORIGIN);
        if (!o.isEmpty() && o.contains(HubKeys.DIM)) {
            out.add(new Place(WaypointShare.Kind.GATE, "Labyrinth Gate (where you entered)", o.getString(HubKeys.DIM), (int) Math.floor(o.getDouble(HubKeys.X)), (int) Math.floor(o.getDouble(HubKeys.Y)), (int) Math.floor(o.getDouble(HubKeys.Z))));
        }
        if (Labyrinth.inSanctuary(p.level()) || knownList(p).contains("hub:Hub: Home Gate")) out.addAll(hubGatePlaces());
        return out;
    }

    /** The player's saved Return Waypoint as a place, or null. */
    public static Place returnPlace(ServerPlayer p) {
        Waypoint w = com.newuniverse.nusmp.labyrinth.mega.rift.WaypointService.get(p);
        return w == null ? null : placeOf(w);
    }
}
