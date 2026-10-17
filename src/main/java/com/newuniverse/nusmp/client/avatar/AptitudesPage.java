package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.aptitude.Aptitude;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeLook;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeRoll;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.avatar.menu.ForgeState;
import com.newuniverse.nusmp.avatar.menu.SlotReel;
import com.newuniverse.nusmp.avatar.net.ForgeAptitudeRow;
import com.newuniverse.nusmp.avatar.net.S2C_AptitudeRollPayload;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Why: the fifth category of the Soul Forge, "Magic Aptitude": the Avatar's gift for a family of magic. There are as many round slots as the server allows
 * (two by default). An aptitude can be taken BY HAND from the grid, which costs a permanent tax on the creator's maximum magicule that grows with its rarity
 * (the tooltip says exactly how much, in percent and in magicule), or won from the free "Randomize Aptitude" gamble, which costs nothing: that is the whole
 * balance of the page. The gamble is the server's: the button only asks, the answer (aptitude and a token) comes back, and the slot-machine reel then spins
 * through the catalog's icons and slows exactly onto it ({@link SlotReel}), and only when it stops does the aptitude drop into its slot and the screen allow
 * Finalize again. The odds the tooltip shows are computed by the same pure function the server rolls with ({@link AptitudeRoll}), from the creator's luck.
 */
final class AptitudesPage extends ForgePage {
    private static final int GAP = 4;
    private static final int REEL_CELLS = 30, REEL_TICKS = 58;

    private Rect[] slots = new Rect[0];
    private Rect action = Rect.EMPTY, button = Rect.EMPTY, gridArea = Rect.EMPTY;
    private ForgeLayout.Grid grid = ForgeLayout.Grid.fit(Rect.EMPTY, 28, GAP, 0);
    private int scroll;
    private List<ForgeAptitudeRow> sorted = List.of();
    private int sortedVersion = -1;

    private SlotReel reel;
    private float reelAge;
    private String pendingId = "";
    private long pendingToken;
    private int seenRoll = -1, waitTicks;
    private float popAge = 99f;

    AptitudesPage(ForgeHost host) { super(host); seenRoll = SoulForgeClientData.rollVersion; }

    @Override int accent() { return 0xE8C468; }

    @Override String title() { return ForgeText.s("nusmp.avatar.forge.page.aptitudes", "Magic Aptitude"); }

    private void resort() {
        sorted = new ArrayList<>(SoulForgeClientData.aptitudes);
        sorted.sort(Comparator.<ForgeAptitudeRow>comparingInt(ForgeAptitudeRow::rarity).thenComparing(ForgeAptitudeRow::name));
        sortedVersion = SoulForgeClientData.stateVersion;
    }

    @Override
    void layout(Rect area) {
        super.layout(area);
        if (sortedVersion != SoulForgeClientData.stateVersion) resort();
        int n = Math.max(1, Math.min(8, h.rules().maxAptitudes()));
        Rect top = new Rect(area.x(), area.y(), area.w(), area.h());
        slots = ForgeLayout.slotRow(top, n, Math.min(h.slotSize(), Math.max(20, area.h() / 3)));
        int rowH = ForgeLayout.slotRowHeight(slots);
        int actH = Math.max(h.font().lineHeight + 22, Math.min(46, h.iconSize() + 10));
        action = new Rect(area.x(), area.y() + rowH + 2, area.w(), actH);
        button = new Rect(action.x(), action.y(), action.w(), 18);
        gridArea = ForgeLayout.below(area, rowH + actH + 4);
        grid = ForgeLayout.Grid.fit(gridArea, Math.min(h.iconSize(), Math.max(20, gridArea.h())), GAP, sorted.size());
        scroll = grid.clampScroll(scroll);
    }

    // ================================================================ the free roll and its reel
    @Override
    void tickAlways() {
        ForgeState st = h.state();
        if (seenRoll != SoulForgeClientData.rollVersion) {
            seenRoll = SoulForgeClientData.rollVersion;
            S2C_AptitudeRollPayload r = SoulForgeClientData.roll;
            if (r != null && !r.aptitudeId().isEmpty()) startReel(r);
        }
        if (st.rollPending()) {
            if (++waitTicks > 70) {                                    // the answer never came
                st.rollFailed();
                h.say(ForgeText.t("nusmp.avatar.forge.apt.noanswer", "The Soul Forge did not answer the roll."), false);
                waitTicks = 0;
            }
        } else waitTicks = 0;
        if (reel != null) {
            float before = reelAge;
            reelAge += 1f;
            int crossed = reel.cellsCrossed(before, reelAge);
            if (crossed > 0 && h.motion()) h.tickSound(0.9f + 0.9f * (reelAge / reel.totalTicks()));
            if (reel.done(reelAge)) finishReel();
        }
        if (popAge < 99f) popAge += 1f;
    }

    private void startReel(S2C_AptitudeRollPayload r) {
        List<String> ids = new ArrayList<>();
        for (ForgeAptitudeRow row : sorted) ids.add(row.id());
        reel = SlotReel.build(r.aptitudeId(), ids, r.reelSeed(), REEL_CELLS, h.motion() ? REEL_TICKS : 6);
        reelAge = 0f;
        pendingId = r.aptitudeId();
        pendingToken = r.token();
        h.state().rollAnswered();
        h.state().setReelBusy(true);
    }

    private void finishReel() {
        ForgeState st = h.state();
        st.setReelBusy(false);
        reel = null;
        st.onRolledAptitude(pendingId, pendingToken);
        popAge = 0f;
        ForgeAptitudeRow row = SoulForgeClientData.aptitudeRow(pendingId);
        h.pulse(row == null ? 0xE8C468 : row.tint());
        h.changed();
    }

    private void roll() {
        ForgeState st = h.state();
        if (!st.canRoll(h.rules(), h.rollsLeft())) return;
        h.click();
        st.beginRoll();
        waitTicks = 0;
        h.requestAptitudeRoll();
    }

    // ================================================================ render
    private static ChatFormatting rarityFormat(Aptitude.Rarity r) {
        return switch (r) {
            case COMMON -> ChatFormatting.GRAY;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.AQUA;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
        };
    }

    private int rarityRgb(ForgeAptitudeRow row) {
        Aptitude a = SoulForgeClientData.aptitude(row.id());
        return AptitudeLook.rarityColour(a == null ? Aptitude.Rarity.COMMON : a.rarity());
    }

    @Override
    void render(GuiGraphics g, int mx, int my, float partial) {
        var font = h.font();
        if (sortedVersion != SoulForgeClientData.stateVersion) { resort(); layout(body); }
        ForgeState st = h.state();
        ForgeRules rules = h.rules();
        List<String> picks = st.picks();
        // ---- the slots
        for (int i = 0; i < slots.length; i++) {
            Rect s = slots[i];
            boolean hand = i < picks.size(), rolled = !hand && st.useRoll() && i == picks.size();
            String id = hand ? picks.get(i) : rolled ? st.rolledId() : "";
            ForgeAptitudeRow row = id.isEmpty() ? null : SoulForgeClientData.aptitudeRow(id);
            boolean over = s.contains(mx, my);
            float hv = h.hover("aslot" + i, over);
            if (row == null) {
                ForgeUi.disc(g, s, 0x05080F, 0.7f);
                ForgeUi.slotRing(g, s, 0xE8C468, 0.45f + 0.4f * hv);
                if (i == picks.size() + (st.useRoll() ? 1 : 0) && (reel != null || st.rollPending())) ForgeUi.glow(g, s.cx(), s.cy(), s.w() * 0.7f, 0xE8C468, 0.16f + 0.1f * (float) Math.sin(h.now() * 0.4));
            } else {
                int frame = rolled ? 0xE8C468 : rarityRgb(row);
                float pop = rolled && popAge < 14f ? 1f - popAge / 14f : 0f;
                ForgeUi.medallion(g, font, s, ForgeIcons.aptitudeTexture(row), row.name(), row.tint(), frame, Math.max(hv, pop), true, false);
                // the price tag of a hand pick, the "free" tag of the roll
                String tag = rolled ? ForgeText.s("nusmp.avatar.forge.apt.free", "FREE") : taxText(row);
                int tagRgb = rolled ? 0x7CF0B0 : (row.taxPercent() > 0 ? 0xE8C468 : 0x7CF0B0);
                int tw = font.width(tag) + 4;
                int tx = s.cx() - tw / 2, ty = s.bottom() - 4;
                g.fill(tx, ty, tx + tw, ty + font.lineHeight + 1, ForgeUi.argb(0x03050A, 0.9f));
                g.renderOutline(tx, ty, tw, font.lineHeight + 1, ForgeUi.argb(tagRgb, 0.7f));
                ForgeUi.text(g, font, tag, tx + 2, ty + 1, tagRgb);
            }
            if (over) {
                h.tooltip(slotTooltip(row, rolled));
                if (row != null) h.leanTo(row.tint());
            }
        }
        // ---- the button or the reel
        if (reel != null || st.rollPending()) drawReel(g, partial);
        else drawButton(g, mx, my);
        // ---- the grid
        ForgeAptitudeRow hovered = null;
        int vis = grid.visibleRows();
        for (int i = 0; i < sorted.size(); i++) {
            int row = i / grid.cols() - scroll;
            if (row < 0 || row >= vis) continue;
            ForgeAptitudeRow r = sorted.get(i);
            Rect c = grid.cellRect(i, scroll);
            boolean picked = picks.contains(r.id()), isRoll = st.useRoll() && r.id().equals(st.rolledId());
            boolean over = c.contains(mx, my);
            if (over) hovered = r;
            float hv = h.hover("apt" + r.id(), over);
            ForgeUi.medallion(g, font, c, ForgeIcons.aptitudeTexture(r), r.name(), r.tint(), picked || isRoll ? 0xFFFFFF : rarityRgb(r), hv, picked || isRoll, false);
        }
        ForgeUi.scrollbar(g, new Rect(gridArea.right() - 2, gridArea.y(), 2, gridArea.h()), scroll, grid.maxScroll(), vis, grid.rows(), 0xE8C468);
        if (hovered != null) {
            h.leanTo(hovered.tint());
            h.tooltip(aptitudeTooltip(hovered));
        }
    }

    private String taxText(ForgeAptitudeRow row) {
        double pct = row.taxPercent() * h.rules().aptitudeTaxScale();
        return pct <= 0.0 ? ForgeText.s("nusmp.avatar.forge.apt.notax", "free") : "-" + (pct == Math.rint(pct) ? String.valueOf((long) pct) : String.format(Locale.ROOT, "%.1f", pct)) + "%";
    }

    private void drawButton(GuiGraphics g, int mx, int my) {
        var font = h.font();
        ForgeState st = h.state();
        boolean can = st.canRoll(h.rules(), h.rollsLeft());
        boolean over = button.contains(mx, my);
        ForgeUi.button(g, font, button, ForgeText.s("nusmp.avatar.forge.apt.randomize", "Randomize Aptitude"), can, h.hover("aroll", over && can), 0xE8C468);
        String caption = h.rollsLeft() <= 0 ? ForgeText.s("nusmp.avatar.forge.apt.none_left", "No free rolls left")
                : ForgeText.s("nusmp.avatar.forge.apt.caption", "Free: %s rolls left. No tax.", h.rollsLeft());
        ForgeUi.centered(g, font, ForgeUi.fit(font, caption, action.w()), action.cx(), button.bottom() + 3, h.rollsLeft() <= 0 ? 0xFF6A6A : 0x8FA3BF);
        if (over || (action.contains(mx, my) && my > button.bottom())) h.tooltip(rollTooltip(can));
    }

    private void drawReel(GuiGraphics g, float partial) {
        var font = h.font();
        int cell = Math.max(18, action.h() - 4);
        int step = cell + 3;
        float pos = reel != null ? reel.position(reelAge + partial) : h.now() * 0.9f;
        int cx = action.cx();
        g.fill(action.x(), action.y(), action.right(), action.bottom(), ForgeUi.argb(0x03050A, 0.8f));
        g.renderOutline(action.x(), action.y(), action.w(), action.h(), ForgeUi.argb(0xE8C468, 0.5f));
        h.scissor(g, action);
        int centre = (int) Math.floor(pos + 0.5f);
        for (int k = centre - 4; k <= centre + 4; k++) {
            float rel = k - pos;
            int x = Math.round(cx + rel * step - cell / 2f);
            Rect c = new Rect(x, action.y() + (action.h() - cell) / 2, cell, cell);
            if (c.right() < action.x() || c.x() > action.right()) continue;
            String id = reel != null ? reel.idAt(k) : sorted.isEmpty() ? "" : sorted.get(Math.floorMod(k * 7 + 3, sorted.size())).id();
            ForgeAptitudeRow row = SoulForgeClientData.aptitudeRow(id);
            float fade = 1f - Math.min(1f, Math.abs(rel) / 3.2f);
            if (row == null) continue;
            ForgeUi.medallion(g, font, c, ForgeIcons.aptitudeTexture(row), row.name(), row.tint(), rarityRgb(row), 0f, false, fade < 0.45f);
        }
        h.endScissor(g);
        Rect mid = new Rect(cx - cell / 2 - 2, action.y() + (action.h() - cell) / 2 - 2, cell + 4, cell + 4);
        g.renderOutline(mid.x(), mid.y(), mid.w(), mid.h(), ForgeUi.argb(0xFFFFFF, 0.85f));
    }

    // ================================================================ tooltips
    private List<Component> slotTooltip(ForgeAptitudeRow row, boolean rolled) {
        List<Component> t = new ArrayList<>();
        if (row == null) {
            t.add(Component.literal(title()).withStyle(ChatFormatting.GOLD));
            t.add(ForgeText.t("nusmp.avatar.forge.apt.slot.empty", "An empty slot: pick an aptitude below, or use the free roll.").withStyle(ChatFormatting.GRAY));
            return t;
        }
        t.add(Component.literal(row.name()).withStyle(rarityFormat(SoulForgeClientData.aptitude(row.id()) == null ? Aptitude.Rarity.COMMON : SoulForgeClientData.aptitude(row.id()).rarity())));
        t.add(rolled ? ForgeText.t("nusmp.avatar.forge.apt.slot.rolled", "Won from the free roll: no tax. Click to let it go.").withStyle(ChatFormatting.GREEN)
                : ForgeText.t("nusmp.avatar.forge.apt.slot.picked", "Taken by hand. Click to give it back.").withStyle(ChatFormatting.GRAY));
        return t;
    }

    private List<Component> rollTooltip(boolean can) {
        ForgeRules rules = h.rules();
        ForgeContext c = h.reserves();
        List<Component> t = new ArrayList<>();
        t.add(ForgeText.t("nusmp.avatar.forge.apt.randomize.title", "Randomize Aptitude").withStyle(ChatFormatting.GOLD));
        t.add(ForgeText.t("nusmp.avatar.forge.apt.randomize.tip", "A free gamble, decided by the server. What it gives you costs no tax at all.").withStyle(ChatFormatting.GRAY));
        List<Aptitude> pool = new ArrayList<>();
        for (ForgeAptitudeRow r : sorted) { Aptitude a = SoulForgeClientData.aptitude(r.id()); if (a != null && !h.state().picks().contains(a.id())) pool.add(a); }
        double[] odds = AptitudeRoll.odds(pool, c.luck(), rules.luckStrength());
        Aptitude.Rarity[] all = Aptitude.Rarity.values();
        for (int i = 0; i < all.length && i < odds.length; i++) {
            if (odds[i] <= 0.0) continue;
            t.add(Component.literal(" " + cap(all[i].name()) + "  " + String.format(Locale.ROOT, "%.1f", odds[i] * 100.0) + "%").withStyle(rarityFormat(all[i])));
        }
        t.add(ForgeText.t("nusmp.avatar.forge.apt.randomize.luck", "Your luck (%s) lifts the rarer ones.", String.format(Locale.ROOT, "%.1f", c.luck())).withStyle(ChatFormatting.DARK_GRAY));
        if (!can) {
            if (h.rollsLeft() <= 0) t.add(ForgeText.t("nusmp.avatar.forge.apt.randomize.out", "You have no free rolls left.").withStyle(ChatFormatting.RED));
            else if (h.state().picks().size() >= rules.maxAptitudes()) t.add(ForgeText.t("nusmp.avatar.forge.apt.randomize.full", "Your hand picks fill every slot. Give one back to roll.").withStyle(ChatFormatting.RED));
        }
        return t;
    }

    private static String cap(String s) { return s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT); }

    private List<Component> aptitudeTooltip(ForgeAptitudeRow r) {
        ForgeRules rules = h.rules();
        ForgeContext c = h.reserves();
        ForgeState st = h.state();
        Aptitude a = SoulForgeClientData.aptitude(r.id());
        Aptitude.Rarity rar = a == null ? Aptitude.Rarity.COMMON : a.rarity();
        List<Component> t = new ArrayList<>();
        t.add(Component.literal(r.name()).withStyle(rarityFormat(rar)));
        t.add(Component.literal(cap(rar.name())).withStyle(ChatFormatting.GRAY)
                .append(r.magic().isEmpty() ? Component.empty() : Component.literal("  -  ").append(ForgeText.t("nusmp.avatar.forge.apt.magic", "%s magic", ForgeUi.titleCase(r.magic()))).withStyle(ChatFormatting.DARK_GRAY)));
        if (!r.description().isEmpty()) for (String line : ForgeUi.wrapLines(h.font(), r.description(), 190, 4)) t.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        if (!r.skills().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String s : r.skills()) names.add(ForgeIcons.skillLabel(s));
            t.add(ForgeText.t("nusmp.avatar.forge.apt.lends", "Lends:").withStyle(ChatFormatting.AQUA));
            for (String line : ForgeUi.wrapLines(h.font(), String.join(", ", names), 190, 4)) t.add(Component.literal(" " + line).withStyle(ChatFormatting.AQUA));
        }
        double pct = r.taxPercent() * rules.aptitudeTaxScale();
        if (pct <= 0.0) {
            t.add(ForgeText.t("nusmp.avatar.forge.apt.tip.free", "Costs no tax, however you get it.").withStyle(ChatFormatting.GREEN));
        } else {
            t.add(ForgeText.t("nusmp.avatar.forge.apt.tip.tax", "Taken by hand: %s%% of your maximum magicule, for good.", String.format(Locale.ROOT, "%.1f", pct)).withStyle(ChatFormatting.GOLD));
            if (c.maxMagicule() > 0) t.add(ForgeText.t("nusmp.avatar.forge.apt.tip.pay", "That is about %s magicule gone for good, and about %s now.",
                    Math.round(pct / 100.0 * c.maxMagicule()), Math.round(pct / 100.0 * rules.upfrontFactor() * c.maxMagicule())).withStyle(ChatFormatting.YELLOW));
            t.add(ForgeText.t("nusmp.avatar.forge.apt.tip.roll", "Won from the free roll, it costs nothing.").withStyle(ChatFormatting.GREEN));
        }
        if (st.picks().contains(r.id())) t.add(ForgeText.t("nusmp.avatar.forge.apt.tip.remove", "Click to give it back.").withStyle(ChatFormatting.DARK_GRAY));
        else if (st.useRoll() && r.id().equals(st.rolledId())) t.add(ForgeText.t("nusmp.avatar.forge.apt.tip.have", "You hold it from the roll.").withStyle(ChatFormatting.DARK_GRAY));
        else if (st.aptitudeCount() >= rules.maxAptitudes()) t.add(ForgeText.t("nusmp.avatar.forge.apt.tip.full", "Every slot is taken.").withStyle(ChatFormatting.RED));
        else t.add(ForgeText.t("nusmp.avatar.forge.apt.tip.pick", "Click to take it (%s of %s slots used).", st.aptitudeCount(), rules.maxAptitudes()).withStyle(ChatFormatting.DARK_GRAY));
        return t;
    }

    // ================================================================ input
    @Override
    boolean mouseClicked(double mx, double my, int button) {
        ForgeState st = h.state();
        boolean busy = st.reelBusy() || st.rollPending();
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].contains(mx, my)) continue;
            List<String> picks = st.picks();
            if (busy) return true;
            if (i < picks.size()) { h.click(); st.togglePick(picks.get(i), h.rules()); h.changed(); }
            else if (st.useRoll() && i == picks.size()) { h.click(); st.discardRoll(); h.changed(); }
            return true;
        }
        if (button == 0 && (reel == null && !st.rollPending()) && this.button.contains(mx, my)) { roll(); return true; }
        if (button != 0 || !gridArea.contains(mx, my)) return false;
        int idx = grid.indexAt(mx, my, scroll);
        if (idx < 0 || idx >= sorted.size()) return true;
        if (busy) { h.say(ForgeText.t("nusmp.avatar.forge.apt.busy", "Wait for the roll to finish."), false); return true; }
        ForgeAptitudeRow r = sorted.get(idx);
        ForgeState.Pick res = st.togglePick(r.id(), h.rules());
        switch (res) {
            case ADDED -> { h.click(); h.pulse(r.tint()); h.changed(); }
            case REMOVED -> { h.click(); h.changed(); }
            case FULL -> h.say(ForgeText.t("nusmp.avatar.forge.apt.full", "Every aptitude slot is taken. Give one back first."), false);
            case ROLLED -> h.say(ForgeText.t("nusmp.avatar.forge.apt.have", "You already hold it from the roll."), false);
        }
        return true;
    }

    @Override
    boolean mouseScrolled(double mx, double my, double dy) {
        if (!gridArea.contains(mx, my) || grid.maxScroll() <= 0) return false;
        scroll = grid.clampScroll(scroll - (int) Math.signum(dy));
        return true;
    }
}
