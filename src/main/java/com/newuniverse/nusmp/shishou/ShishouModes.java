package com.newuniverse.nusmp.shishou;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Shishou is an ULTIMATE skill of the Tensura skill system (2.1.0, replacing the three keys H, J and K), and a Tensura skill has MODES that the player switches with the skill system's own mode key and
 * activates with its skill key. His 31 abilities are grouped into a bounded number of modes ({@link #COUNT}), each mode holding the abilities that can be CAST (the passive ones are simply on once he has them).
 * Inside a mode that holds more than one ability, sneak + the skill key picks the next ability and the plain skill key casts the picked one. This is the whole routing table, pure and tested, so the skill
 * class ({@code skill/ShishouUltimateSkill}) is only glue. Entries are only ever appended (a saved mode index is a position).
 */
public final class ShishouModes {
    private ShishouModes() {}

    /** What a press of the skill key does in a mode. */
    public enum Action { CAST, CYCLE, OPEN_STORAGE, THROW_RECALL, STATUS }

    /** The modes, in the order of the skill system's mode key. */
    public enum Mode {
        TELEKINESIS("telekinesis", "Telekinesis"),
        SENSORY("sensory", "Sensory"),
        DEFENCE("defence", "Defence"),
        OFFENCE("offence", "Offence"),
        STORAGE("storage", "Storage"),
        MAGIC("magic", "Magic"),
        COUNSEL("counsel", "Counsel");

        private final String id, english;

        Mode(String id, String english) {
            this.id = id;
            this.english = english;
        }

        public String id() { return id; }
        public String english() { return english; }
        public String langKey() { return "shishou.mode." + id; }

        /** The castable abilities of this mode, in the order the sneak key cycles through them. */
        public List<ShishouSkill> skills() {
            return switch (this) {
                case TELEKINESIS -> List.of(ShishouSkill.TELEKINESIS);
                case SENSORY -> List.of(ShishouSkill.MAGIC_SENSE, ShishouSkill.STEALTH);
                case DEFENCE -> List.of(ShishouSkill.MULTILAYER_BARRIER, ShishouSkill.SPATIAL_MOTION, ShishouSkill.SPATIAL_MOTION_SHORT);
                case OFFENCE -> List.of(ShishouSkill.BLACK_LIGHTNING, ShishouSkill.DIVINE_SWORD_RELEASE, ShishouSkill.DOPPELGANGER);
                case STORAGE -> List.of(ShishouSkill.STOMACH);
                case MAGIC -> List.of(ShishouSkill.FIRE_MAGIC, ShishouSkill.WIND_MAGIC, ShishouSkill.EARTH_MAGIC, ShishouSkill.WATER_ICE_MAGIC, ShishouSkill.DARKNESS_MAGIC, ShishouSkill.HEALING_MAGIC, ShishouSkill.PURIFICATION);
                case COUNSEL -> List.of();
            };
        }
    }

    public static final int COUNT = Mode.values().length;

    /** The mode of a skill-system mode index, or null for an index that is not one. */
    public static Mode byIndex(int index) { return index >= 0 && index < COUNT ? Mode.values()[index] : null; }

    /** The next mode (the skill system asks for it when the mode key is pressed; {@code reverse} for the scroll the other way). */
    public static int next(int mode, boolean reverse) { return reverse ? Math.floorMod(mode - 1, COUNT) : Math.floorMod(mode + 1, COUNT); }

    /** The mode that holds this ability, or null for a passive one (always on, in no mode). */
    public static Mode modeOf(ShishouSkill s) {
        for (Mode m : Mode.values()) if (m.skills().contains(s)) return m;
        return null;
    }

    /** The abilities of the mode that this sword may use now (the level or the absorbed bits), in order. */
    public static List<ShishouSkill> available(Mode m, int level, long learned) {
        List<ShishouSkill> out = new ArrayList<>();
        for (ShishouSkill s : m.skills()) if (s.available(level, learned)) out.add(s);
        return out;
    }

    /** The ability a press casts: the remembered one ({@code storedBit}, a skill bit or -1) when it is still in the mode and available, else the first available one; null when none is. */
    public static ShishouSkill current(Mode m, int storedBit, int level, long learned) {
        List<ShishouSkill> a = available(m, level, learned);
        if (a.isEmpty()) return null;
        ShishouSkill s = storedBit < 0 ? null : ShishouSkill.byBit(storedBit);
        return s != null && a.contains(s) ? s : a.get(0);
    }

    /** The ability after {@code cur} (wraps); the first when {@code cur} is null or gone. */
    public static ShishouSkill cycle(Mode m, ShishouSkill cur, int level, long learned) {
        List<ShishouSkill> a = available(m, level, learned);
        if (a.isEmpty()) return null;
        int i = cur == null ? -1 : a.indexOf(cur);
        return a.get((i + 1) % a.size());
    }

    /** What the skill key does: sneaking cycles a mode that holds several abilities, opens Stomach in the Storage mode, and otherwise is the same as the plain press. */
    public static Action action(Mode m, boolean sneaking, int availableCount) {
        return switch (m) {
            case TELEKINESIS -> Action.THROW_RECALL;
            case COUNSEL -> Action.STATUS;
            case STORAGE -> sneaking ? Action.OPEN_STORAGE : Action.CAST;
            default -> sneaking && availableCount > 1 ? Action.CYCLE : Action.CAST;
        };
    }

    /** Every castable ability is reachable through exactly one mode (a test pins this): used by the tests and the docs. */
    public static List<ShishouSkill> routed() {
        List<ShishouSkill> out = new ArrayList<>();
        for (Mode m : Mode.values()) out.addAll(m.skills());
        return out;
    }
}
