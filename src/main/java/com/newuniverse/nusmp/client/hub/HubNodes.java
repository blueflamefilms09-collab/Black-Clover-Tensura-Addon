package com.newuniverse.nusmp.client.hub;

/**
 * Why: the nodes of the Multiverse Hub (docs/ui_hub.md) as one append-only enum, with no Minecraft types so the order, the names and the lock rules are unit tested ({@code HubNodesTest}). The ring puts
 * node 0 at the top and goes clockwise, so the order of this enum IS the order on the ring: entries are only ever appended at the end (rule 2). A node is either a native tab (the hub draws its page: Status,
 * Weapons, Quests, Multiverse, Settings) or a launcher (a click opens the mod's own existing screen with the hub as its parent: Skills, Attunement, Codex). The lock rule uses only facts the client already
 * holds; a locked node shows a lock and one hint line instead of its page.
 */
public final class HubNodes {
    private HubNodes() {}

    /** The tabs. Append new ones at the END; the ring spreads {@link #COUNT} nodes evenly, so adding one moves the others a little but never reorders them. */
    public enum Tab {
        STATUS("status", "St", 0x5CD8FF, false),
        WEAPONS("weapons", "Wp", 0xE8C468, false),
        SKILLS("skills", "Sk", 0x9A7CFF, true),
        QUESTS("quests", "Qd", 0x7CF0B0, false),
        ATTUNEMENT("attunement", "At", 0xB39CF0, true),
        MULTIVERSE("multiverse", "Mv", 0xFF9F6A, false),
        CODEX("codex", "Cx", 0x8FD0FF, true),
        SETTINGS("settings", "Se", 0xC0C8D8, false);

        /** The lang key tail: {@code anime_multiverse.hub.tab.<key>} (name), {@code anime_multiverse.hub.desc.<key>} (one line), {@code anime_multiverse.hub.locked.<key>} (the lock hint). */
        public final String key;
        /** The two-letter mark drawn in the node. */
        public final String glyph;
        public final int rgb;
        /** True when a click opens an existing screen instead of showing a page inside the hub. */
        public final boolean launcher;

        Tab(String key, String glyph, int rgb, boolean launcher) { this.key = key; this.glyph = glyph; this.rgb = rgb; this.launcher = launcher; }
    }

    public static final int COUNT = Tab.values().length;

    public static Tab at(int i) { return Tab.values()[Math.floorMod(i, COUNT)]; }

    /** What the client knows when it asks whether a node is locked. Plain numbers, no game types. */
    public record Facts(boolean weaponsKnown, int weapons, int quests, boolean map) {
        public static final Facts NONE = new Facts(false, 0, 0, false);
    }

    /**
     * True when the node's feature has nothing to show yet and the client can tell: the Weapons page once the server answered with no weapon at all, the Quests page when there is no quest in the tracker and no
     * dungeon map has been used. Launchers and the other pages are never locked (the screens they open explain themselves).
     */
    public static boolean locked(Tab t, Facts f) {
        return switch (t) {
            case WEAPONS -> f.weaponsKnown() && f.weapons() <= 0;
            case QUESTS -> f.quests() <= 0 && !f.map();
            default -> false;
        };
    }
}
