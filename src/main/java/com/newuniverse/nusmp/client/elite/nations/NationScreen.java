package com.newuniverse.nusmp.client.elite.nations;

import com.newuniverse.nusmp.elite.nations.core.Permission;
import com.newuniverse.nusmp.elite.nations.net.ActionPayload;
import com.newuniverse.nusmp.elite.nations.world.Msg;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the mouse-driven NATIONS screen (owner: "a GUI screen (mouse-driven: overview, members/roles, treasury, contracts, diplomacy) opened by a key and from a banner right-click"). Five tabs, all drawn by hand
 * from the last snapshot the server sent ({@link NationsClient#data()}): there are no vanilla button widgets to rebuild when a snapshot arrives, so a refresh never loses what the player is typing; the only
 * real widgets are two text boxes that are moved and shown per tab. Every click on a button sends ONE {@link ActionPayload} (an action word and up to two strings and a number); the server decides, answers in chat
 * and pushes a fresh snapshot. A row is selected by clicking it; lists scroll with the mouse wheel. Nothing in here changes game state.
 */
public final class NationScreen extends Screen {
    private static final int W = 392, H = 244;
    private static final int BG = 0xF0101018, BORDER = 0xFF3A3A55, TEXT = 0xFFE4E4EE, MUTED = 0xFF8A8AA2, GOLD = 0xFFE8C468, GREEN = 0xFF6FD07A, RED = 0xFFE05A5A, ROW = 0xFF1B1B28, ROW_SEL = 0xFF2C2C46, BTN = 0xFF2A2A40, BTN_HOT = 0xFF3C3C5E, BTN_OFF = 0xFF1A1A24;
    private static final String[] TAB_KEYS = {"nusmp.nation.gui.tab_overview", "nusmp.nation.gui.tab_members", "nusmp.nation.gui.tab_treasury", "nusmp.nation.gui.tab_contracts", "nusmp.nation.gui.tab_diplomacy"};
    private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.ROOT);

    private record Hit(int x, int y, int w, int h, Runnable run) {}

    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Integer> scroll = new HashMap<>();
    private int tab;
    private int mouseX, mouseY;
    private String selMember = "", selRole = "", selNation = "";
    private long selContract = -1;
    private boolean confirmDisband, useUbs;
    private EditBox in1, in2;
    private int left, top;

    public NationScreen(int tab) {
        super(Component.translatable("nusmp.nation.gui.title"));
        this.tab = Math.max(0, Math.min(4, tab));
    }

    /** A new snapshot arrived: keep what the player is doing, just redraw. */
    public void refresh(int openTab) {
        if (openTab >= 0) tab = Math.min(4, openTab);
        confirmDisband = false;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        in1 = new EditBox(font, left, top, 100, 14, Component.empty());
        in2 = new EditBox(font, left, top, 60, 14, Component.empty());
        in1.setMaxLength(48);
        in2.setMaxLength(12);
        addRenderableWidget(in1);
        addRenderableWidget(in2);
        in1.visible = in2.visible = false;
    }

    @Override
    public void removed() { NationsClient.closed(); }

    @Override
    public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------------------------------------ data helpers
    private CompoundTag root() { return NationsClient.data(); }

    private boolean hasNation() { return root().contains("N"); }

    private CompoundTag me() { return root().getCompound("N"); }

    private static List<CompoundTag> list(CompoundTag t, String key) {
        List<CompoundTag> out = new ArrayList<>();
        ListTag l = t.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) out.add(l.getCompound(i));
        return out;
    }

    private boolean can(Permission p) { return hasNation() && (me().getInt("Perms") & p.bit()) != 0; }

    private static String num(long v) { return NUM.format(v); }

    private static Component tr(String key, Object... args) { return Msg.of(key, args); }

    private void act(String action, String a, String b, long n) { PacketDistributor.sendToServer(new ActionPayload(action, a, b, n)); }

    // ------------------------------------------------------------------------------------------------ drawing helpers
    private void text(GuiGraphics g, Component c, int x, int y, int color) { g.drawString(font, c, x, y, color, false); }

    private void text(GuiGraphics g, String s, int x, int y, int color) { g.drawString(font, s, x, y, color, false); }

    private void clip(GuiGraphics g, String s, int x, int y, int maxW, int color) { g.drawString(font, font.plainSubstrByWidth(s, maxW), x, y, color, false); }

    private void button(GuiGraphics g, Component label, int x, int y, int w, int h, boolean enabled, Runnable run) {
        boolean hot = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, !enabled ? BTN_OFF : hot ? BTN_HOT : BTN);
        g.renderOutline(x, y, w, h, enabled ? BORDER : 0xFF22222E);
        g.drawString(font, font.plainSubstrByWidth(label.getString(), w - 4), x + Math.max(2, (w - font.width(label.getString())) / 2), y + (h - 8) / 2, enabled ? TEXT : MUTED, false);
        if (enabled) hits.add(new Hit(x, y, w, h, run));
    }

    private void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int color) {
        g.fill(x, y, x + w, y + h, 0xFF0B0B12);
        g.fill(x, y, x + (int) Math.round(w * Math.max(0.0, Math.min(1.0, frac))), y + h, color);
        g.renderOutline(x, y, w, h, BORDER);
    }

    private void row(GuiGraphics g, int x, int y, int w, int h, boolean selected, Runnable onClick) {
        g.fill(x, y, x + w, y + h, selected ? ROW_SEL : ROW);
        if (onClick != null) hits.add(new Hit(x, y, w, h, onClick));
    }

    private void place(EditBox box, boolean visible, int x, int y, int w) {
        box.visible = visible;
        box.active = visible;
        box.setX(x);
        box.setY(y);
        box.setWidth(w);
    }

    private int scrollOf(String key, int size, int visible) {
        int max = Math.max(0, size - visible);
        int v = Math.max(0, Math.min(max, scroll.getOrDefault(key, 0)));
        scroll.put(key, v);
        return v;
    }

    // ------------------------------------------------------------------------------------------------ frame
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        mouseX = mx;
        mouseY = my;
        hits.clear();
        left = (width - W) / 2;
        top = (height - H) / 2;
        g.fill(left, top, left + W, top + H, BG);
        g.renderOutline(left, top, W, H, BORDER);
        CompoundTag r = root();
        if (!r.getBoolean("On")) {
            text(g, tr("nusmp.nation.gui.disabled"), left + 12, top + 14, RED);
            place(in1, false, 0, 0, 10);
            place(in2, false, 0, 0, 10);
            return;
        }
        int accent = hasNation() ? 0xFF000000 | me().getInt("Color") : GOLD;
        g.fill(left + 1, top + 1, left + W - 1, top + 3, accent);
        // tabs
        int tw = (W - 12) / 5;
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            boolean sel = i == tab;
            int x = left + 6 + i * tw;
            g.fill(x, top + 6, x + tw - 2, top + 22, sel ? ROW_SEL : ROW);
            g.renderOutline(x, top + 6, tw - 2, 16, sel ? accent : BORDER);
            Component label = tr(TAB_KEYS[i]);
            g.drawString(font, label, x + (tw - 2 - font.width(label)) / 2, top + 11, sel ? TEXT : MUTED, false);
            hits.add(new Hit(x, top + 6, tw - 2, 16, () -> { tab = idx; confirmDisband = false; }));
        }
        place(in1, false, 0, 0, 10);
        place(in2, false, 0, 0, 10);
        int cx = left + 8, cy = top + 28;
        switch (tab) {
            case 0 -> drawOverview(g, cx, cy);
            case 1 -> drawMembers(g, cx, cy);
            case 2 -> drawTreasury(g, cx, cy);
            case 3 -> drawContracts(g, cx, cy);
            default -> drawDiplomacy(g, cx, cy);
        }
    }

    // ------------------------------------------------------------------------------------------------ tab 0: overview
    private void drawOverview(GuiGraphics g, int x, int y) {
        CompoundTag r = root();
        if (!hasNation()) {
            text(g, tr("nusmp.nation.gui.no_nation"), x, y, GOLD);
            text(g, tr("nusmp.nation.gui.wallet", num(r.getLong("Wallet"))), x, y + 12, MUTED);
            text(g, tr("nusmp.nation.gui.found_hint"), x, y + 26, MUTED);
            place(in1, true, x, y + 40, 170);
            button(g, tr("nusmp.nation.gui.found"), x + 176, y + 38, 90, 18, true, () -> { act("found", in1.getValue(), "", 0); in1.setValue(""); });
            int yy = y + 66;
            text(g, tr("nusmp.nation.gui.invitations"), x, yy, GOLD);
            yy += 12;
            for (CompoundTag n : list(r, "All")) {
                if (!n.getBoolean("Invited")) continue;
                final String id = n.getString("Id");
                row(g, x, yy, W - 16, 16, false, null);
                text(g, n.getString("Name"), x + 4, yy + 4, 0xFF000000 | n.getInt("Color"));
                button(g, tr("nusmp.nation.gui.accept"), x + W - 16 - 124, yy, 60, 16, true, () -> act("accept", id, "", 0));
                button(g, tr("nusmp.nation.gui.decline"), x + W - 16 - 62, yy, 60, 16, true, () -> act("decline", id, "", 0));
                yy += 18;
                if (yy > y + 120) break;
            }
            drawDirectory(g, x, y + 126, W - 16, 4);
            return;
        }
        CompoundTag n = me();
        int color = 0xFF000000 | n.getInt("Color");
        g.fill(x, y, x + 6, y + 40, color);
        text(g, n.getString("Name"), x + 12, y, color);
        text(g, tr("nusmp.nation.gui.level", n.getInt("Lv"), n.getInt("MaxLv")), x + 12, y + 12, TEXT);
        bar(g, x + 12, y + 24, 180, 8, n.getInt("Prog") / 1000.0, n.getInt("Missing") < 0 ? GOLD : GREEN);
        text(g, n.getInt("Missing") < 0 ? tr("nusmp.nation.gui.max_level") : tr("nusmp.nation.gui.next_level", n.getInt("Missing")), x + 198, y + 24, MUTED);
        text(g, tr("nusmp.nation.gui.leader", n.getString("Leader")), x, y + 46, TEXT);
        text(g, tr("nusmp.nation.gui.members_count", list(n, "Members").size(), n.getInt("MemCap")), x, y + 58, TEXT);
        text(g, tr("nusmp.nation.gui.treasury_line", num(n.getLong("Bal")), num(n.getLong("Cap"))), x, y + 70, TEXT);
        text(g, tr("nusmp.nation.gui.stats", n.getInt("Done"), n.getInt("Wins"), n.getInt("Losses"), n.getInt("Area")), x, y + 82, MUTED);
        if (n.getBoolean("HasCap")) text(g, tr("nusmp.nation.gui.capital", n.getInt("Cx"), n.getInt("Cy"), n.getInt("Cz")), x, y + 94, TEXT);
        else {
            text(g, tr("nusmp.nation.gui.no_capital"), x, y + 94, RED);
            if (n.getBoolean("IsLeader")) button(g, tr("nusmp.nation.gui.get_banner"), x + 150, y + 91, 110, 14, true, () -> act("banner_item", "", "", 0));
        }
        // perks
        int px = x + 200, py = y + 46;
        text(g, tr("nusmp.nation.gui.perks"), px, py, GOLD);
        py += 12;
        int shown = 0;
        for (CompoundTag p : list(n, "Perks")) {
            if (shown >= 9) break;
            boolean next = p.getBoolean("Next");
            String label = perkLabel(p);
            clip(g, (next ? "+ " : "") + label, px, py + shown * 10, W - 16 - 200 + 8, next ? GOLD : GREEN);
            shown++;
        }
        // buttons
        int by = y + 120;
        if (n.getBoolean("IsLeader")) {
            button(g, confirmDisband ? tr("nusmp.nation.gui.disband_confirm") : tr("nusmp.nation.gui.disband"), x, by, 130, 16, true, () -> {
                if (confirmDisband) { act("disband", "", "", 0); confirmDisband = false; } else confirmDisband = true;
            });
        } else {
            button(g, tr("nusmp.nation.gui.leave"), x, by, 130, 16, true, () -> act("leave", "", "", 0));
        }
        // reputation
        int ry = y + 142;
        text(g, tr("nusmp.nation.gui.reputation"), x, ry, GOLD);
        int shownRep = 0;
        for (CompoundTag rep : list(root(), "Rep")) {
            if (shownRep >= 5) break;
            clip(g, rep.getString("Name") + "  " + rep.getInt("V") + "  " + tr("nusmp.nation.tier." + rep.getString("Tier")).getString(), x, ry + 11 + shownRep * 10, 190, MUTED);
            shownRep++;
        }
    }

    private static String perkLabel(CompoundTag p) {
        String type = p.getString("Type");
        double v = p.getDouble("V");
        String val = v == Math.rint(v) ? Long.toString((long) v) : String.format(Locale.ROOT, "%.1f", v);
        String name = Msg.of("nusmp.nation.perk." + type).getString();
        return "attribute".equals(type) ? name + " " + p.getString("Arg").replace("minecraft:generic.", "") + " +" + val : name + " +" + val;
    }

    private void drawDirectory(GuiGraphics g, int x, int y, int w, int rows) {
        text(g, tr("nusmp.nation.gui.nations"), x, y, GOLD);
        List<CompoundTag> all = list(root(), "All");
        int start = scrollOf("dir", all.size(), rows);
        for (int i = 0; i < rows && start + i < all.size(); i++) {
            CompoundTag n = all.get(start + i);
            int yy = y + 11 + i * 14;
            row(g, x, yy, w, 13, false, null);
            text(g, n.getString("Name"), x + 4, yy + 3, 0xFF000000 | n.getInt("Color"));
            text(g, tr("nusmp.nation.gui.dir_stats", n.getInt("Lv"), n.getInt("Mem")), x + 150, yy + 3, MUTED);
        }
    }

    // ------------------------------------------------------------------------------------------------ tab 1: members and roles
    private void drawMembers(GuiGraphics g, int x, int y) {
        if (!hasNation()) { text(g, tr("nusmp.nation.gui.no_nation"), x, y, GOLD); return; }
        CompoundTag n = me();
        Map<String, String> roleNames = new HashMap<>();
        for (CompoundTag r : list(n, "Roles")) roleNames.put(r.getString("Id"), r.getString("Name"));
        text(g, tr("nusmp.nation.gui.members"), x, y, GOLD);
        List<CompoundTag> members = list(n, "Members");
        int vis = 8, start = scrollOf("members", members.size(), vis);
        for (int i = 0; i < vis && start + i < members.size(); i++) {
            CompoundTag m = members.get(start + i);
            final String name = m.getString("N");
            int yy = y + 11 + i * 14;
            row(g, x, yy, 176, 13, name.equals(selMember), () -> selMember = name);
            g.fill(x + 3, yy + 4, x + 8, yy + 9, m.getBoolean("On") ? GREEN : 0xFF444455);
            clip(g, name, x + 12, yy + 3, 90, TEXT);
            clip(g, roleNames.getOrDefault(m.getString("R"), m.getString("R")), x + 104, yy + 3, 70, MUTED);
        }
        // selected member actions
        int ax = x + 184;
        text(g, selMember.isEmpty() ? tr("nusmp.nation.gui.select_member").getString() : selMember, ax, y, GOLD);
        boolean memberSelected = !selMember.isEmpty();
        List<CompoundTag> roles = list(n, "Roles");
        int rx = ax, ry = y + 12;
        for (CompoundTag r : roles) {
            final String rid = r.getString("Id");
            if ("leader".equals(rid)) continue;
            button(g, Component.literal(r.getString("Name")), rx, ry, 64, 13, memberSelected && can(Permission.ROLES), () -> act("setrole", selMember, rid, 0));
            rx += 66;
            if (rx > ax + 130) { rx = ax; ry += 15; }
        }
        ry = Math.max(ry + (rx == ax ? 0 : 15), y + 27);
        button(g, tr("nusmp.nation.gui.kick"), ax, ry, 64, 14, memberSelected && can(Permission.KICK), () -> act("kick", selMember, "", 0));
        button(g, tr("nusmp.nation.gui.transfer"), ax + 66, ry, 64, 14, memberSelected && n.getBoolean("IsLeader"), () -> act("transfer", selMember, "", 0));
        // roles and permissions
        int ry2 = y + 11 + 8 * 14 + 4;
        text(g, tr("nusmp.nation.gui.roles"), x, ry2, GOLD);
        int rowX = x;
        for (CompoundTag r : roles) {
            final String rid = r.getString("Id");
            int w = font.width(r.getString("Name")) + 8;
            button(g, Component.literal(r.getString("Name")), rowX, ry2 + 11, w, 13, true, () -> selRole = rid);
            if (rid.equals(selRole)) g.renderOutline(rowX, ry2 + 11, w, 13, GOLD);
            rowX += w + 2;
        }
        CompoundTag sel = null;
        for (CompoundTag r : roles) if (r.getString("Id").equals(selRole)) sel = r;
        if (sel != null) {
            final CompoundTag role = sel;
            int px = ax;
            int py = y + ((8 * 14) / 2) + 18;
            // permissions as toggle buttons in two columns
            int idx = 0;
            for (Permission p : Permission.values()) {
                boolean on = (role.getInt("Mask") & p.bit()) != 0;
                boolean editable = can(Permission.ROLES) && !"leader".equals(role.getString("Id"));
                int bx = ax + (idx % 2) * 66, by = py + (idx / 2) * 14;
                button(g, Component.literal((on ? "+ " : "- ") + Msg.of("nusmp.nation.perm." + p.id()).getString()), bx, by, 64, 13, editable, () -> act("role_perm", role.getString("Id"), p.id(), on ? 0 : 1));
                idx++;
            }
        }
        // input row: invite / new role
        int iy = top + H - 20;
        place(in1, true, left + 8, iy, 120);
        button(g, tr("nusmp.nation.gui.invite"), left + 132, iy - 1, 52, 16, can(Permission.INVITE), () -> { act("invite", in1.getValue(), "", 0); in1.setValue(""); });
        button(g, tr("nusmp.nation.gui.new_role"), left + 186, iy - 1, 64, 16, can(Permission.ROLES), () -> {
            String name = in1.getValue().trim();
            act("role_create", name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_"), name, 0);
            in1.setValue("");
        });
        button(g, tr("nusmp.nation.gui.delete_role"), left + 252, iy - 1, 70, 16, can(Permission.ROLES) && !selRole.isEmpty(), () -> act("role_delete", selRole, "", 0));
    }

    // ------------------------------------------------------------------------------------------------ tab 2: treasury
    private void drawTreasury(GuiGraphics g, int x, int y) {
        if (!hasNation()) { text(g, tr("nusmp.nation.gui.no_nation"), x, y, GOLD); return; }
        CompoundTag n = me();
        long bal = n.getLong("Bal"), cap = n.getLong("Cap");
        text(g, tr("nusmp.nation.gui.treasury_line", num(bal), num(cap)), x, y, TEXT);
        bar(g, x, y + 12, 200, 8, cap <= 0 ? 0 : (double) bal / cap, GOLD);
        text(g, tr("nusmp.nation.gui.wallet", num(root().getLong("Wallet"))), x, y + 24, MUTED);
        text(g, tr("nusmp.nation.gui.tax_line", n.getInt("Tax"), n.getInt("MaxTax")), x + 210, y + 12, TEXT);
        final int[] steps = {100, 1000, 10000};
        int bx = x;
        for (int s : steps) {
            button(g, Component.literal("+" + num(s)), bx, y + 38, 56, 15, true, () -> act("deposit", useUbs ? "ubs" : "", "", s));
            button(g, Component.literal("-" + num(s)), bx, y + 55, 56, 15, can(Permission.TREASURY), () -> act("withdraw", useUbs ? "ubs" : "", "", s));
            bx += 58;
        }
        place(in2, true, x + 180, y + 40, 60);
        button(g, tr("nusmp.nation.gui.deposit"), x + 244, y + 38, 54, 15, true, () -> act("deposit", useUbs ? "ubs" : "", "", parse(in2.getValue())));
        button(g, tr("nusmp.nation.gui.withdraw"), x + 244, y + 55, 54, 15, can(Permission.TREASURY), () -> act("withdraw", useUbs ? "ubs" : "", "", parse(in2.getValue())));
        if (root().getBoolean("Ubs")) button(g, useUbs ? tr("nusmp.nation.gui.source_ubs") : tr("nusmp.nation.gui.source_wallet"), x + 304, y + 38, 70, 15, true, () -> useUbs = !useUbs);
        // tax buttons
        button(g, Component.literal("-"), x + 210, y + 24, 14, 12, can(Permission.TREASURY), () -> act("tax", "", "", Math.max(0, n.getInt("Tax") - 1)));
        button(g, Component.literal("+"), x + 226, y + 24, 14, 12, can(Permission.TREASURY), () -> act("tax", "", "", Math.min(n.getInt("MaxTax"), n.getInt("Tax") + 1)));
        text(g, tr("nusmp.nation.gui.ledger"), x, y + 76, GOLD);
        List<CompoundTag> led = list(n, "Ledger");
        int vis = 8, start = scrollOf("ledger", led.size(), vis);
        for (int i = 0; i < vis && start + i < led.size(); i++) {
            CompoundTag e = led.get(start + i);
            int yy = y + 87 + i * 13;
            row(g, x, yy, W - 16, 12, false, null);
            long v = e.getLong("V");
            text(g, new java.text.SimpleDateFormat("MM-dd HH:mm", Locale.ROOT).format(new java.util.Date(e.getLong("T"))), x + 3, yy + 2, MUTED);
            clip(g, e.getString("K").toLowerCase(Locale.ROOT), x + 68, yy + 2, 50, MUTED);
            clip(g, e.getString("A").isEmpty() ? "-" : e.getString("A"), x + 120, yy + 2, 60, TEXT);
            text(g, (v >= 0 ? "+" : "") + num(v), x + 184, yy + 2, v >= 0 ? GREEN : RED);
            clip(g, e.getString("Note"), x + 244, yy + 2, W - 16 - 244 - 4, MUTED);
        }
    }

    private static long parse(String s) {
        try { return Math.max(0L, Long.parseLong(s.trim())); } catch (NumberFormatException e) { return 0L; }
    }

    // ------------------------------------------------------------------------------------------------ tab 3: contracts and bounties
    private void drawContracts(GuiGraphics g, int x, int y) {
        if (!hasNation()) { text(g, tr("nusmp.nation.gui.no_nation"), x, y, GOLD); }
        else {
            CompoundTag n = me();
            text(g, tr("nusmp.nation.gui.board"), x, y, GOLD);
            List<CompoundTag> board = list(n, "Board");
            int vis = 6, start = scrollOf("board", board.size(), vis);
            for (int i = 0; i < vis && start + i < board.size(); i++) {
                CompoundTag c = board.get(start + i);
                final long uid = c.getLong("Uid");
                int yy = y + 11 + i * 20;
                row(g, x, yy, 220, 19, uid == selContract, () -> selContract = uid);
                String state = c.getString("State");
                int col = "TAKEN".equals(state) ? (c.getBoolean("Mine") ? GREEN : GOLD) : TEXT;
                clip(g, "[" + c.getInt("Tier") + "] " + c.getString("Title"), x + 3, yy + 2, 214, col);
                String right = c.getInt("Prog") + "/" + c.getInt("Goal") + "  " + num(c.getLong("Coins")) + "c  +" + c.getLong("Xp") + "xp";
                clip(g, right + ("TAKEN".equals(state) ? "  (" + (c.getString("Taker").isEmpty() ? "?" : c.getString("Taker")) + ")" : ""), x + 3, yy + 10, 214, MUTED);
            }
            CompoundTag sel = null;
            for (CompoundTag c : board) if (c.getLong("Uid") == selContract) sel = c;
            int by = y + 11 + vis * 20 + 2;
            boolean can = can(Permission.CONTRACTS) && sel != null;
            boolean open = sel != null && "OPEN".equals(sel.getString("State"));
            boolean mine = sel != null && sel.getBoolean("Mine");
            button(g, tr("nusmp.nation.gui.take"), x, by, 70, 15, can && open, () -> act("take", "", "", selContract));
            button(g, tr("nusmp.nation.gui.abandon"), x + 72, by, 70, 15, mine, () -> act("abandon", "", "", selContract));
            button(g, tr("nusmp.nation.gui.deliver"), x + 144, by, 76, 15, mine && sel != null && "DELIVER_ITEM".equals(sel.getString("Type")), () -> act("deliver", "", "", selContract));
        }
        // bounties on the right
        int bx = x + 228;
        text(g, tr("nusmp.nation.gui.bounties"), bx, y, GOLD);
        List<CompoundTag> bl = list(root(), "Bounties");
        int vis = 6, start = scrollOf("bounties", bl.size(), vis);
        for (int i = 0; i < vis && start + i < bl.size(); i++) {
            CompoundTag b = bl.get(start + i);
            final long uid = b.getLong("Uid");
            int yy = y + 11 + i * 14;
            row(g, bx, yy, 148, 13, false, null);
            clip(g, b.getString("Name"), bx + 3, yy + 3, 70, "PLAYER".equals(b.getString("Kind")) ? RED : TEXT);
            text(g, num(b.getLong("Coins")), bx + 76, yy + 3, GOLD);
            if (b.getBoolean("Mine")) button(g, Component.literal("x"), bx + 134, yy, 14, 13, true, () -> act("bounty_cancel", "", "", uid));
        }
        int iy = y + 11 + vis * 14 + 4;
        text(g, tr("nusmp.nation.gui.post_bounty"), bx, iy, MUTED);
        place(in1, true, bx, iy + 10, 86);
        place(in2, true, bx + 90, iy + 10, 58);
        button(g, tr("nusmp.nation.gui.on_player"), bx, iy + 27, 72, 14, true, () -> { act("bounty_player", in1.getValue(), "", parse(in2.getValue())); });
        button(g, tr("nusmp.nation.gui.on_mob"), bx + 76, iy + 27, 72, 14, true, () -> { act("bounty_mob", in1.getValue(), "", parse(in2.getValue())); });
        button(g, tr("nusmp.nation.gui.as_nation_player"), bx, iy + 43, 72, 14, can(Permission.TREASURY), () -> act("bounty_player_nation", in1.getValue(), "", parse(in2.getValue())));
        button(g, tr("nusmp.nation.gui.as_nation_mob"), bx + 76, iy + 43, 72, 14, can(Permission.TREASURY), () -> act("bounty_mob_nation", in1.getValue(), "", parse(in2.getValue())));
    }

    // ------------------------------------------------------------------------------------------------ tab 4: diplomacy
    private void drawDiplomacy(GuiGraphics g, int x, int y) {
        if (!hasNation()) { text(g, tr("nusmp.nation.gui.no_nation"), x, y, GOLD); return; }
        CompoundTag n = me();
        String myId = n.getString("Id");
        text(g, tr("nusmp.nation.gui.nations"), x, y, GOLD);
        List<CompoundTag> all = new ArrayList<>();
        for (CompoundTag c : list(root(), "All")) if (!c.getString("Id").equals(myId)) all.add(c);
        int vis = 7, start = scrollOf("dip", all.size(), vis);
        CompoundTag sel = null;
        for (int i = 0; i < vis && start + i < all.size(); i++) {
            CompoundTag c = all.get(start + i);
            final String id = c.getString("Id");
            int yy = y + 11 + i * 15;
            row(g, x, yy, 190, 14, id.equals(selNation), () -> selNation = id);
            clip(g, c.getString("Name"), x + 3, yy + 3, 90, 0xFF000000 | c.getInt("Color"));
            String rel = c.getBoolean("Ally") ? tr("nusmp.nation.gui.rel_ally").getString() : c.contains("Phase") ? tr("nusmp.nation.war.phase." + c.getString("Phase")).getString()
                    : c.getBoolean("PropIn") ? tr("nusmp.nation.gui.rel_prop_in").getString() : c.getBoolean("PropOut") ? tr("nusmp.nation.gui.rel_prop_out").getString() : "";
            clip(g, rel, x + 96, yy + 3, 92, c.getBoolean("Ally") ? GREEN : c.contains("Phase") ? RED : GOLD);
        }
        for (CompoundTag c : all) if (c.getString("Id").equals(selNation)) sel = c;
        // actions on the selection
        int ax = x + 198, ay = y;
        text(g, sel == null ? tr("nusmp.nation.gui.select_nation").getString() : sel.getString("Name"), ax, ay, GOLD);
        boolean dip = can(Permission.DIPLOMACY) && sel != null, war = can(Permission.WAR) && sel != null && root().getBoolean("War");
        boolean ally = sel != null && sel.getBoolean("Ally"), propIn = sel != null && sel.getBoolean("PropIn");
        String phase = sel != null && sel.contains("Phase") ? sel.getString("Phase") : "";
        final String id = selNation;
        button(g, tr("nusmp.nation.gui.ally_propose"), ax, ay + 12, 90, 14, dip && !ally && phase.isEmpty(), () -> act("ally_propose", id, "", 0));
        button(g, tr("nusmp.nation.gui.ally_accept"), ax + 94, ay + 12, 90, 14, dip && propIn, () -> act("ally_accept", id, "", 0));
        button(g, tr("nusmp.nation.gui.ally_decline"), ax, ay + 28, 90, 14, dip && propIn, () -> act("ally_decline", id, "", 0));
        button(g, tr("nusmp.nation.gui.ally_break"), ax + 94, ay + 28, 90, 14, dip && ally, () -> act("ally_break", id, "", 0));
        place(in1, true, ax, ay + 46, 184);
        button(g, tr("nusmp.nation.gui.war_declare"), ax, ay + 62, 90, 14, war && phase.isEmpty() && !ally, () -> { act("war_declare", id, in1.getValue(), 0); in1.setValue(""); });
        button(g, tr("nusmp.nation.gui.war_cancel"), ax + 94, ay + 62, 90, 14, war && ("declared".equals(phase) || "preparation".equals(phase)), () -> act("war_cancel", id, "", 0));
        button(g, tr("nusmp.nation.gui.war_ceasefire"), ax, ay + 78, 90, 14, war && "active".equals(phase), () -> act("war_ceasefire", id, "", 0));
        button(g, tr("nusmp.nation.gui.war_surrender"), ax + 94, ay + 78, 90, 14, war && "active".equals(phase) && n.getBoolean("IsLeader"), () -> act("war_surrender", id, "", 0));
        // my wars
        int wy = y + 11 + 7 * 15 + 6;
        text(g, tr("nusmp.nation.gui.my_wars"), x, wy, GOLD);
        int shown = 0;
        for (CompoundTag w : list(n, "Wars")) {
            if (shown >= 3) break;
            String line = w.getString("OtherName") + "  " + tr("nusmp.nation.war.phase." + w.getString("Phase")).getString() + "  " + w.getInt("Me") + " : " + w.getInt("Them")
                    + "  " + (w.getLong("Ends") / 20) + "s" + (w.getString("CeaseFrom").isEmpty() ? "" : "  " + tr("nusmp.nation.gui.ceasefire_offered").getString());
            clip(g, line, x, wy + 11 + shown * 10, 190, TEXT);
            shown++;
        }
        // history
        int hy = y + 11 + 7 * 15 + 6;
        text(g, tr("nusmp.nation.gui.history"), ax, hy, GOLD);
        int hs = 0;
        for (CompoundTag h : list(n, "History")) {
            if (hs >= 4) break;
            clip(g, h.getString("A") + " v " + h.getString("D") + "  " + h.getInt("SA") + ":" + h.getInt("SD") + "  " + h.getString("Outcome").toLowerCase(Locale.ROOT), ax, hy + 11 + hs * 10, 190, MUTED);
            hs++;
        }
    }

    // ------------------------------------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x() && mx < h.x() + h.w() && my >= h.y() && my < h.y() + h.h()) {
                h.run().run();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        String key = switch (tab) {
            case 0 -> "dir";
            case 1 -> "members";
            case 2 -> "ledger";
            case 3 -> mx > left + 8 + 228 ? "bounties" : "board";
            default -> "dip";
        };
        scroll.merge(key, sy > 0 ? -1 : 1, Integer::sum);
        scroll.put(key, Math.max(0, scroll.get(key)));
        return true;
    }
}
