package com.newuniverse.nusmp.relics.djinn;

import java.util.Map;

/**
 * 1.101.0: the gestures that bind a Djinn Seal to a vessel, and the reasons one is refused (pure, no Minecraft classes).
 *
 * <p>Why this exists: with Better Combat installed a Seal could not be bound to a weapon. Better Combat gives a weapon profile {@code two_handed}
 * and then answers EMPTY for the off hand whenever the main-hand or off-hand stack is two-handed (its {@code PlayerEntityMixin.getEquippedStack_Pre},
 * Better Combat 2.4.0), so {@code Player.getItemInHand(OFF_HAND)} and {@code getOffhandItem()} lie. The ritual read "the other hand" that way and
 * refused with a vague "Hold a Metal Vessel in your other hand". The ritual now reads the raw inventory ({@link BcHands}), accepts the Seal from every
 * gesture listed here, and tells the player exactly why a gesture was refused.
 *
 * <p>A vessel is judged by its <b>item</b> (the relic catalog) plus the two components the ritual owns ({@code djinn_bound}, {@code relic_attuned});
 * every other component on the stack (Better Combat's {@code bettercombat:preset_id}, attribute modifiers, enchantments, other mods' data) is ignored,
 * which {@link #look} shows: it takes the components as a map and reads only those two keys.
 */
public final class BindGesture {
    private BindGesture() {}

    /** Every way to start the ritual. The first two are right-clicking the Seal; the rest never depend on an item's use hook. */
    public enum Gesture {
        /** The Seal in the main hand, the vessel in the off hand. */
        USE_SEAL_MAIN,
        /** The Seal in the off hand, the vessel in the main hand (Better Combat hides the off hand behind a two-handed weapon, so this one cannot always work). */
        USE_SEAL_OFF,
        /** In an inventory screen: the Seal on the cursor, right-click the vessel (or the vessel on the cursor, right-click the Seal). */
        GUI_STACK,
        /** {@code /multiverse djinn bind} (also {@code /djinn bind}). */
        COMMAND,
        /** The Djinn key (J) pressed while no Djinn is bound and a Seal is carried. */
        KEY
    }

    /** What the player is told. {@link #OK} is the only one that lets the ritual run. Appended only. */
    public enum Why {
        OK, GESTURE_DISABLED, NO_SEAL, EMPTY, NOT_A_RELIC, NOT_A_VESSEL, NOT_LEGEND, ALREADY_BOUND, NOT_ATTUNED, NOT_IN_INVENTORY;

        /** The lang key of the reason ({@code message.nusmp.djinn_bind.<name>}). */
        public String key() { return "message.nusmp.djinn_bind." + name().toLowerCase(java.util.Locale.ROOT); }

        /** The English text used when the key is not translated; {@code %s} is the vessel's name where it makes sense. */
        public String fallback() {
            return switch (this) {
                case OK -> "The ritual can begin.";
                case GESTURE_DISABLED -> "This way of binding is switched off on this server (config nusmp-djinn-bind-server.toml).";
                case NO_SEAL -> "You carry no Djinn Seal. A Seal is the dormant Djinn; bind it to a Legend-grade vessel.";
                case EMPTY -> "No vessel found. Hold the vessel, or right-click it with the Seal in the inventory screen, or use /multiverse djinn bind main|off|slot <n>.";
                case NOT_A_RELIC -> "%s is not a relic. A Djinn sleeps only in a Legend-grade Metal Vessel.";
                case NOT_A_VESSEL -> "%s is a relic but not a Metal Vessel: a Djinn cannot sleep in it.";
                case NOT_LEGEND -> "%s is below Legend grade. A Djinn sleeps only in a relic of Legend grade or above.";
                case ALREADY_BOUND -> "%s already holds a Djinn.";
                case NOT_ATTUNED -> "%s must be attuned to you first: sneak and hold use on it for five seconds.";
                case NOT_IN_INVENTORY -> "%s must be in your own inventory (not a chest) to be bound.";
            };
        }
    }

    /** What the ritual needs to know about the stack that should become the vessel. */
    public record Look(boolean empty, boolean relic, boolean flagged, boolean legend, boolean bound, boolean attuned) {
        public static final Look NONE = new Look(true, false, false, false, false, false);

        /** Flagged as a vessel by the catalog and of Legend grade or higher: the only thing {@link VesselAccess#isVessel} asks. */
        public boolean legal() { return !empty && relic && flagged && legend; }
    }

    /** The verdict on a vessel candidate (does not look at the Seal or the price). */
    public static Why judge(Look l) {
        if (l == null || l.empty()) return Why.EMPTY;
        if (!l.relic()) return Why.NOT_A_RELIC;
        if (!l.flagged()) return Why.NOT_A_VESSEL;
        if (!l.legend()) return Why.NOT_LEGEND;
        if (l.bound()) return Why.ALREADY_BOUND;
        if (!l.attuned()) return Why.NOT_ATTUNED;
        return Why.OK;
    }

    /** Is this gesture switched on? Right-clicking the Seal always works; the inventory stacking and the command / key each have a server switch. */
    public static boolean enabled(Gesture g, boolean commandSwitch, boolean guiSwitch) {
        return switch (g) {
            case USE_SEAL_MAIN, USE_SEAL_OFF -> true;
            case GUI_STACK -> guiSwitch;
            case COMMAND, KEY -> commandSwitch;
        };
    }

    /** The whole decision before any price is looked at: gesture on, a Seal there, and a vessel that is judged OK. */
    public static Why decide(Gesture g, boolean sealPresent, Look vessel, boolean commandSwitch, boolean guiSwitch) {
        if (!enabled(g, commandSwitch, guiSwitch)) return Why.GESTURE_DISABLED;
        if (!sealPresent) return Why.NO_SEAL;
        return judge(vessel);
    }

    // ---------------------------------------------------------------- a stack, as data (so tests can add any components)

    /** A stack reduced to what a test (or a log line) needs: the item id and its components by id. */
    public record Stack(String item, Map<String, String> components) {
        public static Stack of(String item, Map<String, String> components) { return new Stack(item, components == null ? Map.of() : components); }
        public boolean isEmpty() { return item == null || item.isEmpty() || item.equals("minecraft:air"); }
    }

    /** What the relic catalog says about an item id (the catalog is keyed by item, never by components). */
    public record Entry(boolean flagged, boolean legend, boolean needsAttunement) {}

    /** The catalog lookup: item id to entry, or null when the item is not a relic. */
    public interface Catalog { Entry of(String itemId); }

    public static final String COMPONENT_BOUND = "nusmp:djinn_bound", COMPONENT_ATTUNED = "nusmp:relic_attuned";

    /**
     * Reads a stack the way the ritual does: the catalog by item id, and only the two components the ritual owns. Anything else on the stack is ignored.
     * {@code playerId} is what the attunement component must hold for a relic that needs attunement.
     */
    public static Look look(Stack s, Catalog catalog, String playerId) {
        if (s == null || s.isEmpty()) return Look.NONE;
        Entry e = catalog.of(s.item());
        boolean bound = s.components().get(COMPONENT_BOUND) != null;
        if (e == null) return new Look(false, false, false, false, bound, false);
        boolean attuned = !e.needsAttunement() || (playerId != null && playerId.equals(s.components().get(COMPONENT_ATTUNED)));
        return new Look(false, true, e.flagged(), e.legend(), bound, attuned);
    }
}
