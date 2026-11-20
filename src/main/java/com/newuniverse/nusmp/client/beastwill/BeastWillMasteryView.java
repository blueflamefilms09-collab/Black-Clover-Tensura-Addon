package com.newuniverse.nusmp.client.beastwill;

import com.newuniverse.nusmp.beastwill.BeastWillNet;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Weapon page of the Codex shows every mastery track the mod already has next to the weapon's own, and the Beast Will's mastery (one share 0..100 per bound will) lives in this package's cache, which is
 * package-private. This read-only view hands the page the names and shares of the wills the player holds, from what the server last sent; it asks for nothing and sends nothing. Built when the page is opened or
 * receives its data, never per frame.
 */
public final class BeastWillMasteryView {
    private BeastWillMasteryView() {}

    /** One will's mastery: its display name and the share 0..100. */
    public record Row(String name, int pct) {}

    public static List<Row> rows() {
        BeastWillNet.Sync s = BeastWillClient.sync();
        if (s == null) return List.of();
        List<Row> out = new ArrayList<>();
        for (BeastWillNet.Entry e : s.wills()) out.add(new Row(BeastWillClient.name(e.id()), Math.max(0, Math.min(100, e.mastery()))));
        return out;
    }
}
