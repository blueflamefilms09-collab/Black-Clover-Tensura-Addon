package com.newuniverse.nusmp.client.weaponpage;

import com.newuniverse.nusmp.client.beastwill.BeastWillMasteryView;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import com.newuniverse.nusmp.lords.LordRules;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Bar;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Entry;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Opt;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.View;
import com.newuniverse.nusmp.weaponpage.WeaponPageNet;
import com.newuniverse.nusmp.weaponpage.WeaponPageRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the client half of the Weapon page of the Codex decides nothing. It keeps the one page the server sent (a few hundred bytes, only when it differs from what this client holds, see
 * {@link WeaponPageNet}), turns it into ready-to-draw text ONCE when it arrives (lang keys translated, item names looked up, the other mastery tracks the mod already has read from their caches), and sends the
 * player's clicks as the one {@link WeaponPageNet.Act} packet. Nothing is sent while the page is not looked at: the open request goes out the first time the page is drawn after the Codex opened, and a click is
 * answered by the server with a fresh page. Cost per frame: none here (the page reads the cooked arrays); per sync: a few dozen strings; memory: the cooked page, dropped at logout.
 */
public final class WeaponPageClient {
    private WeaponPageClient() {}

    /** One option row, ready to draw. */
    public static final class Row {
        public final String text;
        public final int state, action;
        public final String arg;

        Row(String text, int state, int action, String arg) { this.text = text; this.state = state; this.action = action; this.arg = arg; }

        public boolean clickable() { return state == WeaponPageRules.ST_READY || action == WeaponPageRules.ACT_HALF && state == WeaponPageRules.ST_CURRENT; }
    }

    /** One weapon, ready to draw. */
    public static final class Page {
        public String id, chip, title, grade, level, form;
        public int rgb;
        public Row[] opts = new Row[0];
        public String[] barLabel = new String[0], barValue = new String[0];
        public float[] barFrac = new float[0];
    }

    private static View view = View.EMPTY;
    private static Page[] pages = new Page[0];
    private static String[] exLabel = new String[0], exValue = new String[0];
    private static float[] exFrac = new float[0];
    private static boolean have, asked, picked;
    private static int sum, sel;
    private static long lastAct = Long.MIN_VALUE / 2;

    /** Called once from the client block of the mod constructor. */
    public static void init() {
        WeaponPageNet.clientSync = WeaponPageClient::onSync;
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
    }

    private static void clear() {
        view = View.EMPTY;
        pages = new Page[0];
        exLabel = new String[0];
        exValue = new String[0];
        exFrac = new float[0];
        have = asked = picked = false;
        sum = sel = 0;
    }

    // ---------------------------------------------------------------- state
    public static boolean received() { return have; }

    public static Page[] pages() { return pages; }

    public static int selected() { return pages.length == 0 ? -1 : Math.max(0, Math.min(pages.length - 1, sel)); }

    public static void select(int i) {
        if (i >= 0 && i < pages.length) {
            sel = i;
            picked = true;
        }
    }

    public static String[] extraLabels() { return exLabel; }

    public static String[] extraValues() { return exValue; }

    public static float[] extraFracs() { return exFrac; }

    // ---------------------------------------------------------------- the network
    /** The Codex opened: the next draw of the page asks again, and the page is cooked afresh (the language or the other tracks may have changed). */
    public static void opened() {
        asked = false;
        picked = false;
        cook();
    }

    /** First draw after the Codex opened: one open request carrying the checksum of the copy this client holds (the server sends nothing if it is current). */
    public static void ensureAsked() {
        if (asked) return;
        asked = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        PacketDistributor.sendToServer(new WeaponPageNet.Act("", WeaponPageRules.ACT_OPEN, have ? Integer.toString(sum) : ""));
    }

    /** A click on a ready option: the one packet. The server checks everything again; at most one every few ticks leaves this client. */
    public static void act(Row r, int page) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null || !r.clickable() || page < 0 || page >= pages.length) return;
        long now = mc.level.getGameTime();
        if (now - lastAct < 5) return;
        lastAct = now;
        PacketDistributor.sendToServer(new WeaponPageNet.Act(pages[page].id, r.action, r.arg));
    }

    private static void onSync(View v) {
        view = v;
        sum = v.sum();
        have = true;
        if (!picked) sel = Math.max(0, v.held());
        cook();
    }

    // ---------------------------------------------------------------- cooking (once per sync or open, never per frame)
    private static String tr(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.indexOf(' ') < 0 && s.indexOf('.') > 0 && I18n.exists(s) ? I18n.get(s) : s;
    }

    private static int rgbOf(String id) {
        int at = WeaponPageRules.WEAPONS.indexOf(id) - 1;
        return at < 0 ? 0xE8C468 : LordRules.Sin.byOrdinal(at).rgb;
    }

    private static String itemName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? id : BuiltInRegistries.ITEM.getOptional(rl).map(i -> i.getDescription().getString()).orElse(id);
    }

    private static void cook() {
        List<Entry> entries = view.entries();
        Page[] out = new Page[entries.size()];
        for (int i = 0; i < out.length; i++) out[i] = cook(entries.get(i));
        pages = out;
        sel = Math.max(0, Math.min(Math.max(0, out.length - 1), sel));
        List<String> label = new ArrayList<>(), value = new ArrayList<>();
        List<Float> frac = new ArrayList<>();
        CompoundTag g = MultiverseStatusClient.status().getCompound("Grimoire");
        if (g.contains("Mastery")) {
            int m = Math.max(0, Math.min(100, g.getInt("Mastery")));
            label.add(I18n.get("anime_multiverse.weaponpage.other.grimoire"));
            value.add(m + "%");
            frac.add(m / 100f);
        }
        for (BeastWillMasteryView.Row r : BeastWillMasteryView.rows()) {
            label.add(I18n.get("anime_multiverse.weaponpage.other.beast", r.name()));
            value.add(r.pct() + "%");
            frac.add(r.pct() / 100f);
        }
        exLabel = label.toArray(new String[0]);
        exValue = value.toArray(new String[0]);
        exFrac = new float[frac.size()];
        for (int i = 0; i < exFrac.length; i++) exFrac[i] = frac.get(i);
    }

    private static Page cook(Entry e) {
        Page p = new Page();
        boolean tier = e.id().equals("greed");
        p.id = e.id();
        p.rgb = rgbOf(e.id());
        p.title = tr(e.name());
        p.chip = p.title;
        p.grade = e.grade();
        p.level = I18n.get(tier ? "anime_multiverse.weaponpage.level.tier" : "anime_multiverse.weaponpage.level", e.level(), e.max());
        p.form = I18n.get("anime_multiverse.weaponpage.form", tr(e.form()));
        List<Bar> bars = e.bars();
        p.barLabel = new String[bars.size()];
        p.barValue = new String[bars.size()];
        p.barFrac = new float[bars.size()];
        for (int i = 0; i < bars.size(); i++) {
            Bar b = bars.get(i);
            String key = b.label();
            p.barLabel[i] = b.arg().isEmpty() ? tr(key) : I18n.exists(key) ? I18n.get(key, b.arg()) : key;
            p.barValue[i] = b.level() + "/" + b.max();
            p.barFrac[i] = Math.max(0f, Math.min(1f, b.pct() / 100f));
        }
        List<Opt> opts = e.opts();
        p.opts = new Row[opts.size()];
        for (int i = 0; i < opts.size(); i++) {
            Opt o = opts.get(i);
            String text = o.action() == WeaponPageRules.ACT_FEED ? I18n.get("anime_multiverse.weaponpage.opt.feed", itemName(o.arg())) : tr(o.label());
            String note = tr(o.note());
            p.opts[i] = new Row(note.isEmpty() ? text : text + " - " + note, o.state(), o.action(), o.arg());
        }
        return p;
    }
}
