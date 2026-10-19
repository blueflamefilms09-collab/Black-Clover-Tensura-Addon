package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the same report is read in two places that draw it differently (the System window on the client, the chat lines of {@code /dungeon floor} on the server), and
 * the owner's requirement is a CLEAR message: floor and direction, the theme with its description and alignment, every mechanic with what it does and how to beat
 * it, and the depth. So the words are composed once, here, as a list of lines tagged with what they ARE (a title, a theme line, a mechanic's name, a counter ...),
 * and each consumer only decides how to colour and wrap a kind of line. The translation is injected ({@link Tr}), because the client and the server have different
 * language objects: a key that has no text falls back to the English the card carries, so a mechanic or a theme added tomorrow reads properly the moment it exists.
 * Pure, so the whole composition is a unit test.
 */
public final class FloorCardLines {
    private FloorCardLines() {}

    /** What a line is, for colouring. */
    public enum Kind { TITLE, SUB, NOTE, BOSS, THEME, ALIGN, DESC, HEAD, MECH, STATE, BODY, COUNTER, MORE, DEPTH, FOOT }

    /** One line of the report. {@code text} may be longer than a window: the consumer wraps it. */
    public record Line(Kind kind, String text) {}

    /** Translation: the text of a key with its arguments, or the fallback (formatted the same way) when the key has no text. */
    @FunctionalInterface
    public interface Tr {
        String get(String key, String fallback, Object... args);
    }

    /**
     * The lines of a card.
     *
     * @param keyName the name of the key that re-shows the report (for the footer hint); empty to leave the hint out
     * @param withFoot add the footer lines (the hint and the unstuck line)
     */
    public static List<Line> compose(FloorCard c, Tr tr, String keyName, boolean withFoot) {
        List<Line> out = new ArrayList<>();
        String dir = tr.get(c.up() ? FloorInfoText.DIR_UP : FloorInfoText.DIR_DOWN, c.up() ? "UP" : "DOWN");
        String label = c.floor() > 0 ? "+" + c.floor() : Integer.toString(c.floor());
        out.add(new Line(Kind.TITLE, tr.get(FloorInfoText.TITLE, "FLOOR %1$s  -  %2$s", label, dir)));
        out.add(new Line(Kind.SUB, tr.get(c.floor() > 0 ? FloorInfoText.ROAD_HOLY : FloorInfoText.ROAD_ABYSSAL, c.floor() > 0
                ? "HOLY ROAD: the way on climbs UP" : "ABYSSAL ROAD: the way on descends DOWN")));
        switch (c.reasonKind()) {
            case FORWARD -> out.add(new Line(Kind.NOTE, tr.get(FloorInfoText.NOTE_FORWARD, "You go %1$s, to floor %2$s.", dir, label)));
            case BACK -> out.add(new Line(Kind.NOTE, tr.get(FloorInfoText.NOTE_BACK, "You go back %1$s, to floor %2$s.", dir, label)));
            case REQUEST -> out.add(new Line(Kind.NOTE, tr.get(FloorInfoText.NOTE_REQUEST, "Floor report, as it stands now.")));
            case UPDATE -> out.add(new Line(Kind.NOTE, tr.get(FloorInfoText.NOTE_UPDATE, "The floor has changed.")));
            default -> out.add(new Line(Kind.NOTE, tr.get(FloorInfoText.NOTE_ARRIVE, "You arrive on floor %1$s.", label)));
        }
        if (c.boss()) out.add(new Line(Kind.BOSS, tr.get(FloorInfoText.BOSS, "BOSS FLOOR: the Guardian waits at the end of this floor.")));

        // ---- the theme
        if (c.themeId() == null || c.themeId().isEmpty()) {
            out.add(new Line(Kind.THEME, tr.get(FloorInfoText.THEME_NONE, "THEME: not shaped yet")));
        } else {
            String name = tr.get("message.nusmp.mega.mgd.theme." + c.themeId(), c.themeName());
            out.add(new Line(Kind.THEME, tr.get(FloorInfoText.THEME, "THEME: %1$s", name)));
            out.add(new Line(Kind.ALIGN, alignText(c, tr)));
            String desc = tr.get(FloorInfoText.themeDescKey(c.themeId()), c.themeDesc());
            if (!desc.isEmpty()) out.add(new Line(Kind.DESC, desc));
        }

        // ---- the mechanics
        if (!c.mechanicsOn()) {
            out.add(new Line(Kind.HEAD, tr.get(FloorInfoText.MECH_OFF, "Mechanics are switched off on this server.")));
        } else {
            out.add(new Line(Kind.HEAD, tr.get(FloorInfoText.MECH_HEAD, "FLOOR MECHANICS (%1$s)", c.floorMechs().size())));
            if (c.floorMechs().isEmpty()) out.add(new Line(Kind.BODY, tr.get(FloorInfoText.MECH_NONE, "No floor-wide mechanic is in effect on this floor.")));
            mechanics(out, c.floorMechs(), tr);
            if (!c.hereMechs().isEmpty()) {
                out.add(new Line(Kind.HEAD, tr.get(FloorInfoText.MECH_HERE_HEAD, "ACTIVE HERE (%1$s)", c.hereMechs().size())));
                mechanics(out, c.hereMechs(), tr);
            }
            if (c.hiddenRooms() > 0) out.add(new Line(Kind.BODY, tr.get(FloorInfoText.MECH_ROOMS,
                    "%1$s more mechanic(s) wait in the rooms of this floor. You are told when you step into one.", c.hiddenRooms())));
        }

        // ---- the depth
        if (c.healthPct() <= 0 && c.damagePct() <= 0 && c.mind() <= 0 && c.xpTimes100() <= 100) {
            out.add(new Line(Kind.DEPTH, tr.get(FloorInfoText.DEPTH_FLAT, "DEPTH %1$s: the first floor, monsters have their base strength.", c.depth())));
        } else {
            out.add(new Line(Kind.DEPTH, tr.get(FloorInfoText.DEPTH, "DEPTH %1$s: monsters +%2$s%% health and +%3$s%% damage, mind level %4$s, kills give x%5$s experience.",
                    c.depth(), c.healthPct(), c.damagePct(), c.mind(), FloorInfoRules.times(c.xpTimes100()))));
        }
        if (withFoot) {
            if (keyName != null && !keyName.isEmpty()) out.add(new Line(Kind.FOOT, tr.get(FloorInfoText.HINT, "Press %1$s, or type /dungeon floor, to read this again.", keyName)));
            out.add(new Line(Kind.FOOT, tr.get(FloorInfoText.FOOT_STUCK, "Stuck? Type /dungeon unstuck.")));
        }
        return out;
    }

    private static String alignText(FloorCard c, Tr tr) {
        String road = c.alignRoad();
        String roadText = switch (road) {
            case "holy" -> tr.get(FloorInfoText.ALIGN_HOLY, "HOLY ROAD");
            case "abyssal" -> tr.get(FloorInfoText.ALIGN_ABYSSAL, "ABYSSAL ROAD");
            default -> tr.get(FloorInfoText.ALIGN_ANY, "BOTH ROADS");
        };
        String align = road.equals("any") ? roadText
                : c.alignOnly() ? tr.get(FloorInfoText.ALIGN_ONLY, "%1$s ONLY", roadText) : tr.get(FloorInfoText.ALIGN_FAVOURED, "%1$s FAVOURED", roadText);
        return align + "  -  " + tr.get(FloorInfoText.DANGER, "DANGER x%1$s", FloorInfoRules.times(c.dangerHundredths()));
    }

    private static void mechanics(List<Line> out, List<FloorCard.Mech> list, Tr tr) {
        int shown = Math.min(list.size(), FloorCard.MAX_MECHS);
        for (int i = 0; i < shown; i++) {
            FloorCard.Mech m = list.get(i);
            String name = tr.get("mechanic.nusmp." + m.id() + ".name", m.name().isEmpty() ? FloorInfoRules.humanize(m.id()) : m.name());
            out.add(new Line(Kind.MECH, name));
            out.add(new Line(Kind.STATE, stateText(m, tr)));
            String desc = tr.get("mechanic.nusmp." + m.id() + ".desc", m.desc());
            if (!desc.isEmpty()) out.add(new Line(Kind.BODY, desc));
            out.add(new Line(Kind.COUNTER, counterText(m, tr)));
        }
        if (list.size() > shown) out.add(new Line(Kind.MORE, tr.get(FloorInfoText.MECH_MORE, "+%1$s more", list.size() - shown)));
    }

    private static String stateText(FloorCard.Mech m, Tr tr) {
        String scope = switch (m.scope()) {
            case "floor" -> tr.get(FloorInfoText.MECH_SCOPE_FLOOR, "whole floor");
            case "zone" -> tr.get(FloorInfoText.MECH_SCOPE_ZONE, "this spot");
            case "boss" -> tr.get(FloorInfoText.MECH_SCOPE_BOSS, "boss arena");
            default -> tr.get(FloorInfoText.MECH_SCOPE_ROOM, "this room");
        };
        String state = switch (m.state()) {
            case "solved" -> tr.get(FloorInfoText.MECH_STATE_SOLVED, "OVERCOME");
            case "waiting" -> tr.get(FloorInfoText.MECH_STATE_WAITING, "WAITING");
            default -> tr.get(FloorInfoText.MECH_STATE_ARMED, "ACTIVE");
        };
        return state + "  -  " + scope;
    }

    private static String counterText(FloorCard.Mech m, Tr tr) {
        String kind = m.counterKind() == null ? "" : m.counterKind().toLowerCase(Locale.ROOT);
        if (kind.isEmpty()) return tr.get(FloorInfoText.MECH_COUNTER_UNKNOWN, "Counter: see the mechanic's own hint in the room.");
        if (kind.equals("none")) return tr.get(FloorInfoText.MECH_COUNTER_NONE, "Counter: none. Survive it, or leave its area.");
        String hint = tr.get("message.nusmp.mega.mech.hint." + kind, m.hint() == null || m.hint().isEmpty() ? FloorInfoRules.humanize(kind) : m.hint());
        return tr.get(FloorInfoText.MECH_COUNTER, "Counter: %1$s", hint);
    }

    /** A {@link Tr} that never translates: the fallback formatted with the arguments (the server's English output and the tests). */
    public static Tr english() {
        return (key, fallback, args) -> {
            String f = FloorInfoText.englishOrNull(key);
            String text = f != null ? f : fallback;
            return format(text, args);
        };
    }

    /** Formats a numbered {@code %1$s} text with arguments, never throwing: a bad pattern comes back as it is. */
    public static String format(String text, Object... args) {
        if (text == null) return "";
        if (args == null || args.length == 0) return text.replace("%%", "%");
        try {
            return String.format(Locale.ROOT, text, args);
        } catch (RuntimeException bad) {
            return text;
        }
    }
}
