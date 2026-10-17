package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeCosts;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkill;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.avatar.menu.ForgeState;
import com.newuniverse.nusmp.avatar.net.ForgeSkillRow;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Why: the fourth category of the Soul Forge, "Skills": one Unique and one Ultimate skill slot, filled from a scrollable grid of round icons. The grid is the
 * skills the SERVER offered (real skills of the game's own registry, each already sorted into the slot it fits), drawn with the skill's own icon and name; a
 * tooltip says what binding it costs (the permanent tax on the creator's maximum magicule, and the EP the creator must already have), and a skill the creator
 * cannot carry is greyed and refuses the click with the reason. Below the grid is the paid Instant Skill Awakening: a free chance at birth (the d100 of the
 * rules) or a guaranteed one for an extra Genesis Core and a small permanent tax. The page only chooses; every cost is the pure {@link ForgeCosts} and the
 * server decides.
 */
final class SkillsPage extends ForgePage {
    private static final int GAP = 4;

    private ForgeSkill.Kind active = ForgeSkill.Kind.UNIQUE;
    private Rect[] slots = new Rect[0];
    private Rect gridArea = Rect.EMPTY, awake = Rect.EMPTY;
    private ForgeLayout.Grid grid = ForgeLayout.Grid.fit(Rect.EMPTY, 28, GAP, 0);
    private int scroll;
    private List<ForgeSkillRow> shown = List.of();
    private int shownVersion = -1;
    private int refuseFlash;

    SkillsPage(ForgeHost host) { super(host); }

    @Override int accent() { return 0x9A7CFF; }

    @Override String title() { return ForgeText.s("nusmp.avatar.forge.page.skills", "Unique & Ultimate Skills"); }

    private static int kindColour(ForgeSkill.Kind k) { return k == ForgeSkill.Kind.UNIQUE ? 0x5CD8FF : 0xE8C468; }

    private static net.minecraft.network.chat.MutableComponent kindName(boolean ultimate) {
        return ultimate ? ForgeText.t("nusmp.avatar.forge.ultimate", "Ultimate") : ForgeText.t("nusmp.avatar.forge.unique", "Unique");
    }

    private List<ForgeSkillRow> rowsOf(ForgeSkill.Kind kind) {
        List<ForgeSkillRow> out = new ArrayList<>();
        for (ForgeSkillRow r : SoulForgeClientData.skills) if (r.ultimate() == (kind == ForgeSkill.Kind.ULTIMATE)) out.add(r);
        out.sort(Comparator.comparingDouble(ForgeSkillRow::epTaxPercent).thenComparing(ForgeSkillRow::name));
        return out;
    }

    @Override
    void layout(Rect area) {
        super.layout(area);
        int awakeH = area.h() >= 150 ? 36 : 14;
        awake = new Rect(area.x(), area.bottom() - awakeH, area.w(), awakeH);
        Rect top = new Rect(area.x(), area.y(), area.w(), Math.max(0, awake.y() - 3 - area.y()));
        slots = ForgeLayout.slotRow(top, 2, Math.min(h.slotSize(), Math.max(20, top.h() / 3)));
        gridArea = ForgeLayout.below(top, ForgeLayout.slotRowHeight(slots));
        rebuildGrid();
    }

    private void rebuildGrid() {
        shown = rowsOf(active);
        shownVersion = SoulForgeClientData.stateVersion;
        grid = ForgeLayout.Grid.fit(gridArea, Math.min(h.iconSize(), Math.max(20, gridArea.h())), GAP, shown.size());
        scroll = grid.clampScroll(scroll);
    }

    private ForgeSkill skillOf(ForgeSkillRow r) { return SoulForgeClientData.skill(r.id()); }

    /** True when the creator's maximum EP is below the skill's gate (never in creative). */
    private boolean cannotCarry(ForgeSkillRow r) {
        ForgeContext c = h.reserves();
        return !c.creative() && c.maxEp() + 1.0e-9 < r.minEp();
    }

    @Override
    void tick() { if (refuseFlash > 0) refuseFlash--; }

    @Override
    void render(GuiGraphics g, int mx, int my, float partial) {
        var font = h.font();
        if (shownVersion != SoulForgeClientData.stateVersion) rebuildGrid();
        ForgeState st = h.state();
        // ---- the two slots
        for (int i = 0; i < slots.length; i++) {
            ForgeSkill.Kind kind = i == 0 ? ForgeSkill.Kind.UNIQUE : ForgeSkill.Kind.ULTIMATE;
            Rect s = slots[i];
            String id = i == 0 ? st.unique() : st.ultimate();
            ForgeSkillRow row = id.isEmpty() ? null : SoulForgeClientData.skillRow(id);
            boolean on = kind == active, over = s.contains(mx, my);
            float hv = h.hover("slot" + i, over);
            int col = kindColour(kind);
            if (row == null) {
                ForgeUi.disc(g, s, 0x05080F, 0.7f);
                if (on) ForgeUi.glow(g, s.cx(), s.cy(), s.w() * 0.75f, col, 0.25f);
                ForgeUi.slotRing(g, s, col, on ? 1f : 0.5f + 0.4f * hv);
                ForgeUi.centered(g, font, "+", s.cx(), s.cy() - font.lineHeight / 2 + 1, col & 0xFFFFFF);
            } else {
                ForgeUi.medallion(g, font, s, ForgeIcons.skillTexture(row), row.name(), row.tint(), col, hv, true, false);
            }
            String label = row == null ? kindName(i == 1).getString() : ForgeIcons.skillName(row).getString();
            ForgeUi.centered(g, font, ForgeUi.fit(font, label, Math.max(20, s.w() + 14)), s.cx(), s.bottom() + 2, row == null ? (on ? col : UiTheme.TEXT_DIM) : UiTheme.TEXT);
            if (over) {
                h.tooltip(slotTooltip(kind, row));
                if (row != null) h.leanTo(row.tint());
            }
        }
        // ---- the grid of skills of the active slot
        if (shown.isEmpty()) {
            ForgeUi.wrapped(g, font, ForgeText.s("nusmp.avatar.forge.skills.none", "This world offers no skill for this slot."), gridArea.x(), gridArea.y() + 2, gridArea.w(), UiTheme.TEXT_DIM, 3);
        }
        ForgeSkillRow hovered = null;
        int vis = grid.visibleRows();
        for (int i = 0; i < shown.size(); i++) {
            int row = i / grid.cols() - scroll;
            if (row < 0 || row >= vis) continue;
            ForgeSkillRow r = shown.get(i);
            Rect c = grid.cellRect(i, scroll);
            boolean selected = r.id().equals(active == ForgeSkill.Kind.UNIQUE ? st.unique() : st.ultimate());
            boolean over = c.contains(mx, my);
            if (over) hovered = r;
            float hv = h.hover("skill" + r.id(), over);
            ForgeUi.medallion(g, font, c, ForgeIcons.skillTexture(r), r.name(), r.tint(), selected ? 0xFFFFFF : kindColour(active), hv, selected, cannotCarry(r));
        }
        ForgeUi.scrollbar(g, new Rect(gridArea.right() - 2, gridArea.y(), 2, gridArea.h()), scroll, grid.maxScroll(), vis, grid.rows(), kindColour(active));
        if (hovered != null) {
            h.leanTo(hovered.tint());
            h.tooltip(skillTooltip(hovered));
        }
        // ---- the awakening
        drawAwakening(g, mx, my);
    }

    // ================================================================ tooltips
    private List<Component> slotTooltip(ForgeSkill.Kind kind, ForgeSkillRow row) {
        List<Component> t = new ArrayList<>();
        t.add(kindName(kind == ForgeSkill.Kind.ULTIMATE).withStyle(kind == ForgeSkill.Kind.UNIQUE ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        if (row == null) t.add(ForgeText.t("nusmp.avatar.forge.slot.empty", "Empty. Choose a skill from the grid below.").withStyle(ChatFormatting.GRAY));
        else {
            t.add(ForgeIcons.skillName(row).copy().withStyle(ChatFormatting.WHITE));
            t.add(ForgeText.t("nusmp.avatar.forge.slot.remove", "Click to remove it.").withStyle(ChatFormatting.DARK_GRAY));
        }
        return t;
    }

    private List<Component> skillTooltip(ForgeSkillRow r) {
        ForgeRules rules = h.rules();
        ForgeContext c = h.reserves();
        List<Component> t = new ArrayList<>();
        t.add(ForgeIcons.skillName(r).copy().withStyle(r.ultimate() ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        t.add(kindName(r.ultimate()).withStyle(ChatFormatting.GRAY));
        if (!r.description().isEmpty()) for (String line : ForgeUi.wrapLines(h.font(), r.description(), 190, 4)) t.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        double tax = r.epTaxPercent() / 100.0 * rules.skillTaxScale();
        String pct = String.format(Locale.ROOT, "%.1f", tax * 100.0);
        t.add(ForgeText.t("nusmp.avatar.forge.skill.tax", "Binding it takes %s%% of your maximum magicule, for good.", pct).withStyle(ChatFormatting.GOLD));
        if (c.maxMagicule() > 0) t.add(ForgeText.t("nusmp.avatar.forge.skill.pay", "You also pay about %s magicule now.", Math.round(tax * rules.upfrontFactor() * c.maxMagicule())).withStyle(ChatFormatting.YELLOW));
        if (r.minEp() > 0) {
            boolean short_ = cannotCarry(r);
            t.add(ForgeText.t("nusmp.avatar.forge.skill.ep", "Needs %s EP of you (you have %s).", Math.round(r.minEp()), Math.round(c.maxEp())).withStyle(short_ ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
        return t;
    }

    // ================================================================ the awakening
    private boolean awakeningAllowed() {
        ForgeContext c = h.reserves();
        return c.creative() || c.cores() >= 1 + h.rules().awakeningExtraCores();
    }

    private void drawAwakening(GuiGraphics g, int mx, int my) {
        var font = h.font();
        ForgeRules rules = h.rules();
        boolean paid = h.state().paidAwakening(), allowed = awakeningAllowed();
        boolean over = awake.contains(mx, my);
        float hv = h.hover("awake", over && allowed);
        Rect box = new Rect(awake.x(), awake.y() + 1, 11, 11);
        int col = allowed ? 0xE8C468 : 0x5C6E88;
        g.fill(box.x(), box.y(), box.right(), box.bottom(), ForgeUi.argb(0x03050A, 0.9f));
        g.renderOutline(box.x(), box.y(), box.w(), box.h(), ForgeUi.argb(col, 0.5f + 0.5f * hv));
        if (paid) g.fill(box.x() + 3, box.y() + 3, box.right() - 3, box.bottom() - 3, ForgeUi.argb(col, 1f));
        String l1 = ForgeText.s("nusmp.avatar.forge.awaken.short", "Paid Awakening");
        ForgeUi.text(g, font, ForgeUi.fit(font, l1, awake.w() - 15), box.right() + 4, box.y() + 2, allowed ? (paid ? UiTheme.GOLD : UiTheme.TEXT) : UiTheme.TEXT_FAINT);
        if (awake.h() >= 30) {
            double chance = ForgeCosts.awakeningChance(rules, h.reserves().luck()) * 100.0;
            String l2 = ForgeText.s("nusmp.avatar.forge.awaken.sub", "Free chance %s%%. Paid: +%s core, %s%% tax for good.",
                    String.format(Locale.ROOT, "%.0f", chance), rules.awakeningExtraCores(), String.format(Locale.ROOT, "%.1f", rules.awakeningTax() * 100.0));
            ForgeUi.wrapped(g, font, l2, awake.x(), box.bottom() + 2, awake.w(), UiTheme.TEXT_DIM, 2);
        }
        if (over) h.tooltip(awakeningTooltip(allowed));
    }

    private List<Component> awakeningTooltip(boolean allowed) {
        ForgeRules rules = h.rules();
        double chance = ForgeCosts.awakeningChance(rules, h.reserves().luck()) * 100.0;
        List<Component> t = new ArrayList<>();
        t.add(ForgeText.t("nusmp.avatar.forge.awaken", "Guaranteed Instant Awakening").withStyle(ChatFormatting.GOLD));
        t.add(ForgeText.t("nusmp.avatar.forge.awaken.tip1", "At birth the Avatar rolls a d100: below %s plus your luck, it awakens a Unique or Ultimate skill at once.", Math.round(rules.awakeningBase())).withStyle(ChatFormatting.GRAY));
        t.add(ForgeText.t("nusmp.avatar.forge.awaken.tip2", "Your free chance now: %s%%.", String.format(Locale.ROOT, "%.0f", chance)).withStyle(ChatFormatting.GREEN));
        t.add(ForgeText.t("nusmp.avatar.forge.awaken.tip3", "Pay %s extra Genesis Core and %s%% of your maximum magicule for good, and it is certain.", rules.awakeningExtraCores(),
                String.format(Locale.ROOT, "%.1f", rules.awakeningTax() * 100.0)).withStyle(ChatFormatting.YELLOW));
        if (!allowed) t.add(ForgeText.t("nusmp.avatar.forge.awaken.need", "You hold %s Genesis Core(s); you need %s.", h.reserves().cores(), 1 + rules.awakeningExtraCores()).withStyle(ChatFormatting.RED));
        return t;
    }

    // ================================================================ input
    @Override
    boolean mouseClicked(double mx, double my, int button) {
        ForgeState st = h.state();
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].contains(mx, my)) continue;
            ForgeSkill.Kind kind = i == 0 ? ForgeSkill.Kind.UNIQUE : ForgeSkill.Kind.ULTIMATE;
            String id = i == 0 ? st.unique() : st.ultimate();
            h.click();
            if (button == 1 || (kind == active && !id.isEmpty())) { st.clearSkill(kind); h.changed(); }
            if (kind != active) { active = kind; scroll = 0; rebuildGrid(); }
            return true;
        }
        if (awake.contains(mx, my) && button == 0) {
            if (awakeningAllowed()) { h.click(); st.setPaidAwakening(!st.paidAwakening()); h.pulse(0xE8C468); h.changed(); }
            return true;
        }
        if (button != 0 || !gridArea.contains(mx, my)) return false;
        int idx = grid.indexAt(mx, my, scroll);
        if (idx < 0 || idx >= shown.size()) return true;
        ForgeSkillRow r = shown.get(idx);
        ForgeSkill s = skillOf(r);
        if (s == null) return true;
        boolean already = r.id().equals(active == ForgeSkill.Kind.UNIQUE ? st.unique() : st.ultimate());
        if (!already && cannotCarry(r)) {
            refuseFlash = 20;
            h.say(ForgeText.t("nusmp.avatar.forge.skill.ep_short", "You need %s EP to carry this skill.", Math.round(r.minEp())), false);
            return true;
        }
        h.click();
        boolean now = st.toggleSkill(s);
        if (now) h.pulse(r.tint());
        h.changed();
        return true;
    }

    @Override
    boolean mouseScrolled(double mx, double my, double dy) {
        if (!gridArea.contains(mx, my) || grid.maxScroll() <= 0) return false;
        scroll = grid.clampScroll(scroll - (int) Math.signum(dy));
        return true;
    }
}
