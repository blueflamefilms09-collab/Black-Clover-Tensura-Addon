package com.newuniverse.nusmp.labyrinth.mega.system;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.FloorListener;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.enc.DepthConfig;
import com.newuniverse.nusmp.labyrinth.mega.enc.DepthRules;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Instance;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicDef;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicLibrary;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsConfig;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsData;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsDirector;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Messages;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Planner;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Scope;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeDef;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeDefs;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: this is the System telling a player what the floor under their feet IS. The owner's request: "the solo leveling system should tell you what the theme is
 * for the dungeon's floor and also what the mechanic". So whenever a player enters a floor (a staircase, an arrival from the Hub, a respawn, a login, a party member
 * who joins: all of them are one event, {@link FloorListener#onFloorEntered}, because the floor tracker forgets a player's floor at login, respawn and dimension
 * change and announces the first floor it sees again) this builds ONE {@link FloorCard} and sends it to the System window: the floor number and the direction, the theme
 * (name, a line about it, the road it belongs to), the mechanics of the floor (what each does and how it is beaten) and the effect of the depth on the monsters.
 * Everything is read from the existing registries (the sector's {@code RegionTheme} and its {@code ThemeDef}, the mechanics plan the Director made for the sector, the
 * Dungeon Master's depth rules), so a theme or a mechanic added tomorrow is covered the moment it exists; a key that has no lang text falls back to the English the card
 * carries. No spam: {@link FloorInfoRules.Throttle} shows an arrival once per floor change, the same floor again only after a cooldown, a key press at most twice a
 * second, and a mid-floor update only after its own cooldown. While a player stands on a floor a cheap watcher (once a second) compares the mechanics around them with
 * what they were last told, and announces a mechanic that woke in a room or was overcome with one pop-up line and a refreshed report. Server thread only.
 */
public final class FloorInfo implements FloorListener {
    private static final Logger LOG = LogUtils.getLogger();

    /** The one listener {@link FloorInfoRegistry#init} registers with {@code MegaDungeon.addFloorListener}. */
    public static final FloorInfo LISTENER = new FloorInfo();

    private static final Map<UUID, FloorInfoRules.Throttle> THROTTLE = new HashMap<>();
    /** What the player was last told about the mechanics around them ("id|scope|state" entries). */
    private static final Map<UUID, Set<String>> SEEN = new HashMap<>();
    /** The last card built for each player (also what the self-test reads for a player without a connection). */
    private static final Map<UUID, FloorCard> LAST = new HashMap<>();

    private FloorInfo() {}

    // ================================================================ the floor event
    @Override
    public void onFloorEntered(ServerPlayer player, int floor, int previous) {
        try {
            show(player, floor, previous, FloorInfoRules.reasonFor(previous, floor));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The floor report failed: {}", e.toString());
        }
    }

    /**
     * Shows the floor report if the rules allow it. Returns true when a card was built and sent (a player without a connection still gets one built, so a test can
     * read it from {@link #last}).
     */
    public static boolean show(ServerPlayer p, int floor, int previous, FloorInfoRules.Reason reason) {
        if (p == null || floor == 0 || !FloorInfoConfig.enabled() || !SystemConfig.enabled()) return false;
        FloorInfoRules.Throttle t = THROTTLE.computeIfAbsent(p.getUUID(), k -> new FloorInfoRules.Throttle());
        long now = p.serverLevel().getGameTime();
        if (!t.offer(reason, floor, now, FloorInfoConfig.gap())) return false;
        FloorCard card = build(p, floor, previous, reason);
        LAST.put(p.getUUID(), card);
        SEEN.put(p.getUUID(), signature(p, floor));
        SystemSync.send(p, new FloorInfoNet.Card(card));
        if (reason != FloorInfoRules.Reason.REQUEST && FloorInfoConfig.get(FloorInfoConfig.CHAT_COPY)) sendLines(p, card);
        return true;
    }

    /** The key press: report again, now. Outside the dungeon the player is told so. */
    public static void request(ServerPlayer p) {
        try {
            int floor = floorOf(p);
            if (!FloorInfoConfig.enabled()) {
                tell(p, SystemMsg.text(FloorInfoText.CMD_OFF));
                return;
            }
            if (floor == 0) {
                tell(p, SystemMsg.text(FloorInfoText.CMD_NOT_HERE));
                return;
            }
            show(p, floor, 0, FloorInfoRules.Reason.REQUEST);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The floor report request failed: {}", e.toString());
        }
    }

    /** The floor a player stands on for the report: 0 when they are not in the Mega-Dungeon. */
    public static int floorOf(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (!MegaDungeon.in(level)) return 0;
        int floor = SystemQuests.currentFloor(level, p);
        return floor != 0 ? floor : SystemQuests.lastFloor(p);
    }

    /** The last card built for a player, or null. */
    public static FloorCard last(UUID id) { return LAST.get(id); }

    // ================================================================ building the card
    /** Builds the report for the place the player stands on. Reads the world and the registries; sends nothing. */
    public static FloorCard build(ServerPlayer p, int floor, int previous, FloorInfoRules.Reason reason) {
        ServerLevel level = p.serverLevel();
        Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
        RegionTheme theme = s == null ? null : s.theme;

        String themeId = "", themeName = "", themeDesc = "", align = "any";
        int danger = 100;
        if (theme != null) {
            ThemeDef def = ThemeDefs.get(theme);
            themeId = theme.id();
            themeName = def == null || def.name() == null || def.name().isEmpty() ? FloorInfoRules.humanize(themeId) : def.name();
            String built = FloorInfoText.themeDesc(themeId);
            themeDesc = built != null ? built : FloorInfoRules.genericThemeDesc(def == null ? "" : FloorInfoRules.favouredCatastrophe(def.catastrophes()));
            align = FloorInfoRules.alignCode(theme.route().name(), theme.exclusive());
            danger = def == null ? 100 : (int) Math.round(def.danger() * 100.0);
        }

        FloorInfoRules.Depth depth = depthOf(floor);
        List<FloorCard.Mech> floorMechs = new ArrayList<>(), here = new ArrayList<>();
        int hidden = 0;
        boolean mechOn = MechanicsConfig.enabled();
        if (mechOn && s != null && s.state != Sector.State.GENERATING && s.state != Sector.State.FAILED) {
            MechanicsData data = MechanicsData.get(level.getServer());
            for (Planner.Slot slot : MechanicsDirector.planFor(s)) {
                String key = MechanicsDirector.keyOf(s, slot);
                Instance in = MechanicsDirector.get(key);
                boolean solved = data.isSolved(key) || (in != null && in.state == Instance.State.SOLVED);
                MechanicDef d = slot.def();
                if (d.scope() == Scope.FLOOR) {
                    floorMechs.add(mech(d, solved ? "solved" : in != null ? "armed" : "waiting"));
                } else if (in != null && in.active() && in.area.contains(p)) {
                    here.add(mech(d, solved ? "solved" : "armed"));
                } else if (!solved && in == null) {
                    hidden++;
                }
            }
        }
        boolean boss = com.newuniverse.nusmp.labyrinth.mega.FloorMap.isBossFloor(floor);
        return new FloorCard(reason.code(), floor, previous, FloorInfoRules.up(previous, floor), depth.depth(), boss, themeId, themeName, themeDesc, align, danger,
                depth.healthPct(), depth.damagePct(), depth.mind(), depth.xpTimes100(), mechOn, hidden, cap(floorMechs), cap(here));
    }

    private static List<FloorCard.Mech> cap(List<FloorCard.Mech> list) {
        return list.size() <= FloorCard.MAX_MECHS + 1 ? list : new ArrayList<>(list.subList(0, FloorCard.MAX_MECHS + 1));
    }

    /** The depth numbers of a floor, honouring the switches of the depth layer (a switched-off layer adds nothing). */
    static FloorInfoRules.Depth depthOf(int floor) {
        boolean on = DepthConfig.get(DepthConfig.ENABLED);
        double strength = on && DepthConfig.get(DepthConfig.STRENGTH) ? DepthConfig.get(DepthConfig.STRENGTH_SCALE) : 0.0;
        FloorInfoRules.Depth d = FloorInfoRules.depth(floor, DepthRules.parseThresholds(DepthConfig.get(DepthConfig.MIND_FLOORS)),
                on && DepthConfig.get(DepthConfig.DROPS) ? DepthConfig.dropParams() : null, strength);
        int mind = on && DepthConfig.get(DepthConfig.SMARTS) ? d.mind() : 0;
        int xp = on && DepthConfig.get(DepthConfig.DROPS) ? d.xpTimes100() : 100;
        return new FloorInfoRules.Depth(d.depth(), d.healthPct(), d.damagePct(), mind, xp);
    }

    private static FloorCard.Mech mech(MechanicDef d, String state) {
        String kind = d.counter().kind();
        String hint = "";
        try {
            hint = d.counter().none() ? "" : Messages.hint(kind).getString();
        } catch (RuntimeException ignored) {
            // a hint is a nicety: the client falls back to the counter kind's own name
        }
        return new FloorCard.Mech(d.id(), d.name(), d.desc(), kind, hint, d.scope().id(), state);
    }

    // ================================================================ the mid-floor watcher
    /** The mechanics around a player as "id|scope|state" entries: the floor-wide ones of their sector and the room, zone and boss ones whose area they stand in. */
    static Set<String> signature(ServerPlayer p, int floor) {
        Set<String> out = new LinkedHashSet<>();
        if (!MechanicsConfig.enabled()) return out;
        Sector s = MegaDungeon.sectorAt(p.serverLevel(), p.blockPosition());
        if (s == null) return out;
        MechanicsData data = MechanicsData.get(p.serverLevel().getServer());
        for (Planner.Slot slot : MechanicsDirector.planFor(s)) {
            String key = MechanicsDirector.keyOf(s, slot);
            Instance in = MechanicsDirector.get(key);
            boolean solved = data.isSolved(key) || (in != null && in.state == Instance.State.SOLVED);
            Scope sc = slot.def().scope();
            if (sc == Scope.FLOOR) out.add(slot.def().id() + "|floor|" + (solved ? "solved" : in != null ? "armed" : "waiting"));
            else if (in != null && in.active() && in.area.contains(p)) out.add(slot.def().id() + "|" + sc.id() + "|" + (solved ? "solved" : "armed"));
        }
        return out;
    }

    /**
     * What a change in the signature means for the player: a mechanic that WOKE somewhere that is not the whole floor (a room, a spot, a boss arena) and any mechanic
     * that was OVERCOME. A floor-wide mechanic going from waiting to active is not news (the report already lists it), and a mechanic that merely left the player's
     * area is not news either.
     */
    public record Event(String id, boolean overcome) {}

    /** The events between two signatures, in the order of the ids. Pure. */
    public static List<Event> events(Set<String> before, Set<String> after) {
        List<Event> out = new ArrayList<>();
        FloorInfoRules.Diff d = FloorInfoRules.diff(before, after);
        for (String added : d.added()) {
            String[] part = added.split("\\|", -1);
            if (part.length < 3) continue;
            String id = part[0], scope = part[1], state = part[2];
            if (state.equals("solved")) out.add(new Event(id, true));
            else if (state.equals("armed") && !scope.equals("floor")) out.add(new Event(id, false));
        }
        return out;
    }

    /** Server tick: once a second, compare what each player was told with what is around them now. */
    public static void tick(MinecraftServer server) {
        if (SEEN.isEmpty() || server.getTickCount() % 20 != 7 || !FloorInfoConfig.enabled() || !FloorInfoConfig.get(FloorInfoConfig.MECHANIC_UPDATES)) return;
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) return;
        for (ServerPlayer p : new ArrayList<>(mega.players())) {
            try {
                watch(p);
            } catch (RuntimeException e) {
                LOG.debug("[nusmp] floor report watcher: {}", e.toString());
            }
        }
    }

    static void watch(ServerPlayer p) {
        Set<String> before = SEEN.get(p.getUUID());
        if (before == null || !p.isAlive() || p.isSpectator()) return;
        int floor = floorOf(p);
        if (floor == 0) return;
        Set<String> now = signature(p, floor);
        if (now.equals(before)) return;
        List<Event> events = events(before, now);
        if (events.isEmpty()) {                                                    // a change that is not news: remember it silently so it is not compared again
            SEEN.put(p.getUUID(), now);
            return;
        }
        FloorInfoRules.Throttle t = THROTTLE.computeIfAbsent(p.getUUID(), k -> new FloorInfoRules.Throttle());
        if (!t.offer(FloorInfoRules.Reason.UPDATE, floor, p.serverLevel().getGameTime(), FloorInfoConfig.gap())) return;      // too soon: the change stays in the diff for the next look
        SEEN.put(p.getUUID(), now);
        for (Event e : events) {
            MechanicDef d = MechanicLibrary.get(e.id());
            String name = d == null ? FloorInfoRules.humanize(e.id()) : serverName(d);
            SystemSync.toast(p, e.overcome() ? ToastKind.SUCCESS : ToastKind.INFO, e.overcome() ? FloorInfoText.TOAST_MECH_DONE : FloorInfoText.TOAST_MECH_NEW, name);
        }
        FloorCard card = build(p, floor, 0, FloorInfoRules.Reason.UPDATE);
        LAST.put(p.getUUID(), card);
        SystemSync.send(p, new FloorInfoNet.Card(card));
    }

    private static String serverName(MechanicDef d) {
        Language lang = Language.getInstance();
        return lang.has(d.nameKey()) ? lang.getOrDefault(d.nameKey()) : d.name();
    }

    // ================================================================ words for the chat
    private static void tell(ServerPlayer p, Component c) {
        if (SystemSync.hasConnection(p)) p.sendSystemMessage(c);                     // a fake player of a self-test has no connection
    }

    /** The server's translator: the loaded language, else the English the card or the map carries. */
    public static FloorCardLines.Tr serverTr() {
        return (key, fallback, args) -> {
            Language lang = Language.getInstance();
            String own = FloorInfoText.englishOrNull(key);
            String base = lang.has(key) ? lang.getOrDefault(key) : own != null ? own : fallback;
            return FloorCardLines.format(base, args);
        };
    }

    /** Writes the report into the player's chat, one component per line, coloured by what the line is. */
    public static void sendLines(ServerPlayer p, FloorCard card) {
        tell(p, SystemMsg.text(FloorInfoText.CMD_HEAD).withStyle(ChatFormatting.AQUA));
        for (FloorCardLines.Line l : FloorCardLines.compose(card, serverTr(), "", true)) {
            tell(p, Component.literal(indent(l)).withStyle(style(l.kind())));
        }
    }

    private static String indent(FloorCardLines.Line l) {
        return switch (l.kind()) {
            case MECH, MORE -> "  > " + l.text();
            case STATE, BODY, COUNTER -> "      " + l.text();
            default -> l.text();
        };
    }

    private static ChatFormatting style(FloorCardLines.Kind k) {
        return switch (k) {
            case TITLE -> ChatFormatting.AQUA;
            case SUB, NOTE, FOOT, MORE -> ChatFormatting.GRAY;
            case BOSS -> ChatFormatting.RED;
            case THEME, MECH -> ChatFormatting.GOLD;
            case ALIGN, STATE -> ChatFormatting.YELLOW;
            case HEAD -> ChatFormatting.AQUA;
            case COUNTER -> ChatFormatting.GREEN;
            case DEPTH -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.WHITE;
        };
    }

    // ================================================================ lifecycle
    public static void forget(UUID id) {
        THROTTLE.remove(id);
        SEEN.remove(id);
        LAST.remove(id);
    }

    public static void clear() {
        THROTTLE.clear();
        SEEN.clear();
        LAST.clear();
    }
}
