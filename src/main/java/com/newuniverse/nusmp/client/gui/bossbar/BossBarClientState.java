package com.newuniverse.nusmp.client.gui.bossbar;

import com.newuniverse.nusmp.boss.bar.BossBarNet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * What the client knows about the mod bosses' bars: the style the server sent for each bar id, and the animation state of each. A bar with
 * no entry here is drawn like vanilla. Client thread only (the packets are handled on it). Entries disappear with {@link BossBarNet.Clear},
 * on logout, and when their bar has been missing from the vanilla bar list for a while.
 */
public final class BossBarClientState {
    private BossBarClientState() {}

    /** Ticks a style may outlive its bar before it is forgotten (a style can arrive a moment before the vanilla add packet). */
    private static final int KEEP_TICKS = 200;

    /** The style, the animation and a few cached strings of one bar. */
    public static final class Entry {
        public BossBarNet.Style style;
        public final BossBarAnim anim;
        long lastSeen;
        int percent = -1;
        String percentText = "";

        Entry(BossBarNet.Style style) {
            this.style = style;
            this.anim = new BossBarAnim(style.bar().getLeastSignificantBits() ^ style.bar().getMostSignificantBits());
        }

        /** "73%" for a progress, rebuilt only when the number changes. */
        String percentText(float progress) {
            int p = BossBarLayout.percent(progress);
            if (p != percent) {
                percent = p;
                percentText = p + "%";
            }
            return percentText;
        }
    }

    private static final Map<UUID, Entry> BARS = new HashMap<>();

    public static void receive(BossBarNet.Style s) {
        Entry e = BARS.get(s.bar());
        if (e == null) BARS.put(s.bar(), new Entry(s));
        else e.style = s;
    }

    public static void receive(BossBarNet.Clear c) {
        BARS.remove(c.bar());
    }

    /** The entry of a bar, or null for a bar the mod did not style. */
    public static Entry get(UUID bar) {
        return BARS.isEmpty() ? null : BARS.get(bar);
    }

    /** Forgets styles whose bars have not been in 'present' for {@link #KEEP_TICKS}. */
    static void purge(Map<UUID, ?> present, long now) {
        if (BARS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Entry>> it = BARS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Entry> m = it.next();
            if (present.containsKey(m.getKey())) m.getValue().lastSeen = now;
            else if (m.getValue().lastSeen == 0L) m.getValue().lastSeen = now;      // never drawn yet: the clock starts now
            else if (now - m.getValue().lastSeen > KEEP_TICKS) it.remove();
        }
    }

    public static void clear() {
        BARS.clear();
    }
}
