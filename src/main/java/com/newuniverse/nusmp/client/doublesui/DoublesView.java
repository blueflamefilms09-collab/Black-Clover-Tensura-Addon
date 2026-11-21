package com.newuniverse.nusmp.client.doublesui;

import com.newuniverse.nusmp.doublesui.DoubleRow;
import com.newuniverse.nusmp.doublesui.DoublesUiMath;
import com.newuniverse.nusmp.doublesui.S2C_DoublesPayload;

import java.util.UUID;

/**
 * Why: the client's copy of the last roster the server sent (docs/doubles_ui.md): flat arrays of at most 16 rows that the overlay reads every frame without allocating, plus the
 * game tick the picture arrived on so the time bars can count down on their own. Written by the packet handler and read by the overlay, both on the client thread.
 */
final class DoublesView {
    private DoublesView() {}

    static final int MAX = DoublesUiMath.MAX_ROWS;
    static int n;
    /** Index of the selected double, or {@link DoublesUiMath#AUTO}. */
    static int active = DoublesUiMath.AUTO;
    static long syncTick;
    static final UUID[] id = new UUID[MAX];
    static final int[] entityId = new int[MAX], life = new int[MAX], lifeMax = new int[MAX];
    static final float[] hp = new float[MAX], maxHp = new float[MAX];
    // text caches, refreshed only when the number they show changes (no string is built per frame)
    static final int[] distShown = new int[MAX], timeShown = new int[MAX];
    static final String[] distTxt = new String[MAX], timeTxt = new String[MAX];

    static void apply(S2C_DoublesPayload p, long gameTime) {
        n = Math.min(p.rows().size(), MAX);
        active = DoublesUiMath.AUTO;
        for (int i = 0; i < n; i++) {
            DoubleRow r = p.rows().get(i);
            id[i] = r.id(); entityId[i] = r.entityId(); hp[i] = r.health(); maxHp[i] = r.maxHealth(); life[i] = r.lifeLeft(); lifeMax[i] = Math.max(1, r.lifeMax());
            distShown[i] = -1; timeShown[i] = -1;
            if (r.id().equals(p.active())) active = i;
        }
        syncTick = gameTime;
    }

    static void clear() { n = 0; active = DoublesUiMath.AUTO; }
}
