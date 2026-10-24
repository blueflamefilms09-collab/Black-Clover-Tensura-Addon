package com.newuniverse.nusmp.client.companion.gs;

/**
 * What a choice of the server's view IS, read from its id alone, so the Great Spirit screen can give the shared bond choices their own widgets (the gift slot, the stance icons, the follow switch, the pouch
 * slots) and leave every other choice as a plain row. The ids are the bond system's reserved {@code k:} ids ({@code Bond}), the growth panel's {@code g:panel} and the companion's own: an id that ends in
 * {@code :join} or {@code :wait} is the follow switch (the sample companion's are {@code t:join} and {@code t:wait}). The screen still only ever sends back an id that is on the view. Pure, unit tested.
 */
public final class OptionKinds {
    private OptionKinds() {}

    public enum Kind { TOPIC, FIVE, GIFT, LIKES, STANCE, PUT, TAKE, TAKE_ALL, USE_ON, USE_OFF, PANEL, JOIN, WAIT }

    /** The kind of an id (TOPIC for a companion's own choice and for anything unknown). */
    public static Kind of(String id) {
        if (id == null) return Kind.TOPIC;
        if (id.startsWith("k:")) {
            return switch (id) {
                case "k:five" -> Kind.FIVE;
                case "k:gift" -> Kind.GIFT;
                case "k:likes" -> Kind.LIKES;
                case "k:put" -> Kind.PUT;
                case "k:takeall" -> Kind.TAKE_ALL;
                case "k:use:on" -> Kind.USE_ON;
                case "k:use:off" -> Kind.USE_OFF;
                default -> id.startsWith("k:stance:") ? Kind.STANCE : id.startsWith("k:take:") ? Kind.TAKE : Kind.TOPIC;
            };
        }
        if (id.equals("g:panel")) return Kind.PANEL;
        if (id.endsWith(":join")) return Kind.JOIN;
        if (id.endsWith(":wait")) return Kind.WAIT;
        return Kind.TOPIC;
    }

    /** The stance name of a {@code k:stance:<name>} id, or "". */
    public static String stanceOf(String id) { return of(id) == Kind.STANCE ? id.substring("k:stance:".length()) : ""; }

    /** The pouch slot of a {@code k:take:<n>} id, or -1. */
    public static int takeSlot(String id) {
        if (of(id) != Kind.TAKE) return -1;
        try {
            int n = Integer.parseInt(id.substring("k:take:".length()));
            return n >= 0 && n < 9 ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Does the screen draw this kind with a widget of its own instead of a row (so it is left out of the plain list)? */
    public static boolean hasWidget(Kind k) { return k != Kind.TOPIC; }
}
