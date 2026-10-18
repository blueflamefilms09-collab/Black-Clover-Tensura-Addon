package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.client.skilltree.layout.EquipRule;
import com.newuniverse.nusmp.client.skilltree.layout.MenuCheckbox;
import com.newuniverse.nusmp.client.skilltree.layout.MenuNesting;
import com.newuniverse.nusmp.client.skilltree.layout.MenuPlacement;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.skilltree.PlayerTreeState;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.menu.LineStandInSkill;
import com.newuniverse.nusmp.skilltree.menu.MenuConfig;
import com.newuniverse.nusmp.skilltree.menu.MenuText;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: this is the NESTED view of a Unique Skill Line inside Tensura's Ability Selection menu. Tensura's own screen lists the line (its stand-in skill) in the Unique list; when the player selects that entry this
 * panel appears next to it and shows, under the line's name, the line's arts and passives, each passive with the Tensura-style checkbox (ticked = ON, empty = OFF), an Equip button (the single-active-line rule),
 * and a button to the Skill Tree. It is drawn in {@code ScreenEvent.Render.Post} and handles clicks and the wheel in the matching Pre events (cancelling them so a click on the panel never reaches a widget of
 * Tensura underneath), instead of being a screen of its own: the owner wants no separate GUI for this flow. Where it goes is {@link MenuPlacement}'s decision, from values read off the live screen, never from
 * guessed Tensura pixels. Everything that decides WHAT is shown is a pure, tested function ({@link MenuNesting}, {@link EquipRule}, {@link MenuCheckbox}); this class only draws it and forwards the clicks to the
 * requests the Skill Tree already sends ({@code TogglePassive}, {@code SetClass}, {@code Inspect}), so the server stays the only authority: a switch is flipped on the client at once for feel and the server's
 * answer (or refusal, which re-sends the tree) puts the truth back, exactly like the Passives panel. A line the player does not own (an operator's ADMIN INSPECT) is shown dimmed with no boxes, the tree being
 * requested from the server once. One panel at a time, so its few fields are static and are cleared when the screen closes.
 */
final class TensuraMenuPanel {
    private TensuraMenuPanel() {}

    private static final int PAD = 6, BTN_H = 14;
    private static final TensuraCheckbox BOX = new TensuraCheckbox();
    private static final Set<String> ASKED = new HashSet<>();

    // what the last frame drew (for hit tests); null when nothing was drawn
    private static Rect panel, toggle, equipBtn, treeBtn, list;
    private static String routeId = "", equipClass = "";
    private static List<MenuNesting.Row> rows = List.of();
    private static int rowsSignature, scroll, hoverRow = -1;
    private static boolean hoverBox, rowsFresh;
    private static Component hoverBoxTip = Component.empty();

    /** Forgets everything (the screen closed). */
    static void reset() {
        panel = toggle = equipBtn = treeBtn = list = null;
        routeId = "";
        equipClass = "";
        rows = List.of();
        rowsSignature = 0;
        scroll = 0;
        hoverRow = -1;
        ASKED.clear();
    }

    /** The stand-in skill that is selected in Tensura's list, or null. */
    static LineStandInSkill selected(AbilitySelectionScreen s) {
        ManasSkillInstance sel = s.selectedAbility;
        return sel != null && sel.getSkill() instanceof LineStandInSkill st ? st : null;
    }

    // ------------------------------------------------------------------ drawing
    static void render(AbilitySelectionScreen s, GuiGraphics g, int mx, int my) {
        panel = toggle = equipBtn = treeBtn = list = null;
        hoverRow = -1;
        hoverBox = false;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !TensuraScreenAccess.showsUniqueSkills(s)) return;
        Font font = mc.font;
        boolean may = InspectMode.available(player);
        MenuPlacement.Frame frame = TensuraScreenAccess.frame(s);
        LineStandInSkill sel = selected(s);
        MenuPlacement.Result placed = sel == null ? null : MenuPlacement.place(s.width, s.height, frame, TensuraScreenAccess.inner(s), MenuConfig.placement(), MenuConfig.panelWidth());
        List<FormattedCharSequence> tip = null;
        Component simpleTip = null;

        g.pose().pushPose();
        g.pose().translate(0f, 0f, 400f);
        if (may && MenuConfig.adminButton()) {
            boolean on = InspectMode.active(player);
            Component label = MenuText.tr(on ? MenuText.INSPECT_ON : MenuText.INSPECT);
            int w = font.width(label) + 12;
            toggle = MenuPlacement.toggleRect(s.width, s.height, frame, placed == null ? null : placed.panel(), w, BTN_H);
            boolean hover = toggle.contains(mx, my);
            TreeSkin.chip(g, toggle, on, hover, on ? 0xE05050 : 0x8A93A8);
            g.drawString(font, label, toggle.x() + 6, toggle.y() + (BTN_H - 8) / 2, on ? TreeSkin.gold() : TreeSkin.dim(), false);
            if (hover) simpleTip = MenuText.tr(MenuText.INSPECT_TIP);
        }
        if (sel != null) {
            panel = placed.panel();
            routeId = sel.routeId();
            tip = drawPanel(g, font, player, sel, panel, mx, my);
        }
        g.pose().popPose();

        if (tip != null && !tip.isEmpty()) g.renderTooltip(font, tip, mx, my);
        else if (simpleTip != null) g.renderTooltip(font, font.split(simpleTip, 200), mx, my);
    }

    private static List<FormattedCharSequence> drawPanel(GuiGraphics g, Font font, Player player, LineStandInSkill sel, Rect p, int mx, int my) {
        IPlayerSkillTree tree = SkillTreeApi.of(player);
        PlayerTreeState state = new PlayerTreeState(tree);
        SkillRoute route = RouteRegistry.byId(sel.routeId());
        boolean owned = tree.hasRoute(sel.routeId());
        int color = (route != null ? route.color() : sel.line().color()) & 0xFFFFFF;
        EquipRule.State eq = EquipRule.of(state, route);
        equipClass = EquipRule.classToSend(eq, route);

        TreeSkin.panel(g, p, color, 0.9f);
        int x = p.x() + PAD, y = p.y() + PAD, w = p.w() - 2 * PAD;

        // header: icon, name, status
        g.renderItem(ItemIcons.of(route == null ? "" : route.icon(), ""), x, y);
        String name = font.plainSubstrByWidth(route == null ? sel.line().name() : route.name(), w - 22);
        g.drawString(font, Component.literal(name).withStyle(ChatFormatting.BOLD), x + 22, y, TreeSkin.opaque(color), false);
        Component status = switch (eq) {
            case EQUIPPED -> MenuText.tr(MenuText.EQUIPPED);
            case NOT_OWNED -> MenuText.tr(MenuText.NOT_OWNED);
            default -> MenuText.tr(MenuText.NOT_EQUIPPED);
        };
        String statusText = font.plainSubstrByWidth(status.getString(), w - 22);
        g.drawString(font, statusText, x + 22, y + 10, eq == EquipRule.State.EQUIPPED ? TreeSkin.gold() : eq == EquipRule.State.NOT_OWNED ? TreeSkin.bad() : TreeSkin.dim(), false);
        y += 24;

        // buttons
        List<FormattedCharSequence> tip = null;
        int bx = x;
        if (eq == EquipRule.State.CAN_EQUIP || eq == EquipRule.State.NO_CLASS) {
            Component label = MenuText.tr(MenuText.EQUIP);
            int bw = font.width(label) + 14;
            equipBtn = new Rect(bx, y, bw, BTN_H);
            boolean hover = equipBtn.contains(mx, my);
            TreeSkin.chip(g, equipBtn, eq == EquipRule.State.CAN_EQUIP, hover, color);
            g.drawString(font, label, equipBtn.x() + 7, equipBtn.y() + (BTN_H - 8) / 2, eq == EquipRule.State.CAN_EQUIP ? TreeSkin.text() : TreeSkin.faint(), false);
            if (hover) tip = font.split(MenuText.tr(eq == EquipRule.State.CAN_EQUIP ? MenuText.EQUIP_TIP : MenuText.EQUIP_NONE), 200);
            bx += bw + 4;
        }
        if (owned) {
            Component label = MenuText.tr(MenuText.OPEN_TREE);
            int bw = font.width(label) + 14;
            treeBtn = new Rect(bx, y, bw, BTN_H);
            boolean hover = treeBtn.contains(mx, my);
            TreeSkin.chip(g, treeBtn, false, hover, color);
            g.drawString(font, label, treeBtn.x() + 7, treeBtn.y() + (BTN_H - 8) / 2, TreeSkin.text(), false);
            if (hover) tip = font.split(MenuText.tr(MenuText.OPEN_TREE_TIP), 200);
        }
        y += BTN_H + 4;

        // summary and description
        MenuNesting.Summary sum = MenuNesting.summary(route, state);
        if (sum.total() > 0 && owned) {
            g.drawString(font, font.plainSubstrByWidth(MenuText.tr(MenuText.UNLOCKED, sum.bought(), sum.total()).getString(), w), x, y, TreeSkin.dim(), false);
            y += 11;
        }
        if (route != null && !route.desc().isEmpty()) {
            List<FormattedCharSequence> d = font.split(Component.literal(route.desc()), w);
            for (int i = 0; i < Math.min(3, d.size()); i++) {
                g.drawString(font, d.get(i), x, y, TreeSkin.faint(), false);
                y += 10;
            }
            y += 2;
        }

        // the nested rows
        int bottom = p.bottom() - PAD;
        list = new Rect(x, y, w, Math.max(20, bottom - y));
        TreeSkin.well(g, list, color);
        refreshRows(route, state, tree, owned);
        if (route == null || route.nodes().isEmpty()) {
            askForTree(route, sel.routeId(), owned, InspectMode.active(player));
            drawCentered(g, font, MenuText.tr(MenuText.LOADING), list);
        } else if (rows.isEmpty()) {
            drawCentered(g, font, MenuText.tr(MenuText.NOTHING_BOUGHT), list);
        } else {
            tip = drawRows(g, font, route, tree, owned, mx, my, tip, color);
        }
        return tip;
    }

    private static void drawCentered(GuiGraphics g, Font font, Component text, Rect area) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(40, area.w() - 12));
        int y = area.y() + area.h() / 2 - lines.size() * 5;
        for (FormattedCharSequence l : lines) {
            g.drawCenteredString(font, l, area.x() + area.w() / 2, y, TreeSkin.dim());
            y += 10;
        }
    }

    /** Asks the server once for the full tree of a line the player does not own (an operator in inspect mode). */
    private static void askForTree(SkillRoute route, String id, boolean owned, boolean inspecting) {
        if (owned || !inspecting || (route != null && !route.nodes().isEmpty())) return;
        if (ASKED.add(id)) TreeActions.inspect(id);
    }

    private static void refreshRows(SkillRoute route, PlayerTreeState state, IPlayerSkillTree tree, boolean owned) {
        int sig = tree.passiveToggles().hashCode();
        sig = sig * 31 + tree.nodeLevels().hashCode();
        sig = sig * 31 + tree.activeClass().hashCode();
        sig = sig * 31 + tree.unlockedRoutes().hashCode();
        sig = sig * 31 + System.identityHashCode(route);
        sig = sig * 31 + (owned ? 1 : 0);
        sig = sig * 31 + routeId.hashCode();
        if (sig == rowsSignature && rowsFresh) return;
        rowsSignature = sig;
        rowsFresh = true;
        rows = MenuNesting.build(route, state, tree::isPassiveEnabled, owned);
        scroll = MenuNesting.clampScroll(rows, scroll, list == null ? 0 : list.h());
    }

    private static List<FormattedCharSequence> drawRows(GuiGraphics g, Font font, SkillRoute route, IPlayerSkillTree tree, boolean owned, int mx, int my, List<FormattedCharSequence> tip, int color) {
        Rect a = list;
        scroll = MenuNesting.clampScroll(rows, scroll, a.h());
        g.enableScissor(a.x() + 1, a.y() + 1, a.right() - 1, a.bottom() - 1);
        int top = 0;
        List<FormattedCharSequence> out = tip;
        for (int i = 0; i < rows.size(); i++) {
            MenuNesting.Row r = rows.get(i);
            int h = r.height();
            int ry = a.y() + top - scroll;
            top += h;
            if (ry + h <= a.y()) continue;
            if (ry >= a.bottom()) break;
            if (r.header()) {
                drawHeader(g, font, a, ry, r, color);
                continue;
            }
            boolean hover = a.contains(mx, my) && my >= ry && my < ry + h;
            if (hover) hoverRow = i;
            drawEntry(g, font, a, ry, r, route, hover, mx, my, color);
        }
        g.disableScissor();
        int total = MenuNesting.contentHeight(rows);
        if (total > a.h()) {
            int bar = Math.max(12, a.h() * a.h() / Math.max(1, total));
            int span = Math.max(1, total - a.h());
            TreeSkin.scrollbar(g, a.right() - 5, a.y() + 1, a.h() - 2, (int) ((a.h() - 2 - bar) * (scroll / (float) span)), bar);
        }
        if (hoverRow >= 0 && out == null) {
            MenuNesting.Row r = rows.get(hoverRow);
            out = hoverBox ? font.split(hoverBoxTip, 120) : entryTip(font, r, owned);
        }
        return out;
    }

    private static void drawHeader(GuiGraphics g, Font font, Rect a, int y, MenuNesting.Row r, int color) {
        int x = a.x() + 3, w = a.w() - 10;
        g.fill(x, y + 2, x + 3, y + MenuNesting.HEADER_H - 1, TreeSkin.opaque(color));
        String key = switch (r.kind()) {
            case HEADER_ARTS -> MenuText.ARTS;
            case HEADER_PASSIVES -> MenuText.PASSIVES;
            default -> MenuText.STATS;
        };
        g.drawString(font, MenuText.tr(key).getString() + " (" + r.count() + ")", x + 7, y + 3, TreeSkin.opaque(color), false);
        g.hLine(x, x + w, y + MenuNesting.HEADER_H - 1, UiMath.withAlpha(color, 0.35f));
    }

    private static void drawEntry(GuiGraphics g, Font font, Rect a, int y, MenuNesting.Row r, SkillRoute route, boolean hover, int mx, int my, int color) {
        int x = a.x() + 3, w = a.w() - 10, h = MenuNesting.ROW_H;
        SkillNode n = r.node();
        TreeSkin.chip(g, x, y + 1, w, h - 2, false, hover, r.dim() ? TreeSkin.SEALED & 0xFFFFFF : color);
        int iy = y + (h - 16) / 2;
        g.renderItem(ItemIcons.of(n.icon(), route.icon()), x + 3, iy);
        if (r.dim()) {                                                   // items are drawn at a depth of about 150, so the veil goes over them
            g.pose().pushPose();
            g.pose().translate(0f, 0f, 200f);
            g.fill(x + 3, iy, x + 19, iy + 16, UiMath.withAlpha(0x05070E, 0.55f));
            g.pose().popPose();
        }
        int right = x + w - 4;
        if (r.checkbox()) {
            Rect box = boxRect(a, y);
            BOX.place(box.x(), box.y(), r.checked(), r.dim());
            BOX.render(g, mx, my, 0f);
            if (hover && BOX.isMouseOver(mx, my)) {
                hoverBox = true;
                hoverBoxTip = BOX.tooltip();
            }
            right = box.x() - 4;
        }
        String lv = r.level() > 0 ? MenuText.tr(MenuText.LEVEL, r.level(), r.max()).getString() : "";
        if (!lv.isEmpty()) {
            g.drawString(font, lv, right - font.width(lv), y + (h - 8) / 2, TreeSkin.faint(), false);
            right -= font.width(lv) + 6;
        }
        String shown = font.plainSubstrByWidth(n.name(), Math.max(20, right - (x + 23)));
        g.drawString(font, shown, x + 23, y + (h - 8) / 2, r.dim() ? TreeSkin.faint() : (r.checkbox() && !r.checked()) ? TreeSkin.dim() : TreeSkin.text(), false);
    }

    /** Where the checkbox of the row whose top is {@code rowY} sits: the right end of the row, centred in its height. The drawing and the click use this one function, so they cannot disagree. */
    private static Rect boxRect(Rect a, int rowY) {
        int right = a.x() + 3 + (a.w() - 10) - 4;
        return new Rect(right - TensuraCheckbox.SIZE, rowY + (MenuNesting.ROW_H - TensuraCheckbox.SIZE) / 2, TensuraCheckbox.SIZE, TensuraCheckbox.SIZE);
    }

    private static List<FormattedCharSequence> entryTip(Font font, MenuNesting.Row r, boolean owned) {
        SkillNode n = r.node();
        List<FormattedCharSequence> out = new ArrayList<>(font.split(Component.literal(n.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), 200));
        String typeKey = switch (r.kind()) {
            case ART -> MenuText.TYPE_ART;
            case STANCE -> MenuText.TYPE_STANCE;
            case PASSIVE -> MenuText.TYPE_PASSIVE;
            default -> MenuText.TYPE_STAT;
        };
        out.addAll(font.split(MenuText.tr(typeKey).withStyle(ChatFormatting.AQUA), 200));
        if (r.level() > 0) out.addAll(font.split(MenuText.tr(MenuText.LEVEL, r.level(), r.max()).withStyle(ChatFormatting.GRAY), 200));
        if (!n.desc().isEmpty()) out.addAll(font.split(Component.literal(n.desc()), 200));
        if (r.checkbox()) out.addAll(font.split(MenuText.tr(MenuText.SWITCH_HINT).withStyle(ChatFormatting.DARK_GRAY), 200));
        return out;
    }

    // ------------------------------------------------------------------ input
    /** True when the click was for this panel (the caller cancels the event). */
    static boolean mouseClicked(AbilitySelectionScreen s, double mx, double my, int button) {
        if (button != 0) return panel != null && panel.contains(mx, my);
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (toggle != null && toggle.contains(mx, my)) {
            InspectMode.toggle();
            ASKED.clear();
            rowsFresh = false;
            TreeSounds.click();
            return true;
        }
        if (panel == null || !panel.contains(mx, my)) return false;
        if (equipBtn != null && equipBtn.contains(mx, my)) {
            if (!equipClass.isEmpty() && SkillTreeApi.isEnabled()) {
                TreeActions.setClass(equipClass);
                TreeSounds.click();
            }
            return true;
        }
        if (treeBtn != null && treeBtn.contains(mx, my)) {
            TreeSounds.click();
            Minecraft.getInstance().setScreen(new SkillTreeScreen(s));
            return true;
        }
        if (list != null && list.contains(mx, my) && !rows.isEmpty()) {
            int i = MenuNesting.rowAt(rows, (int) my - list.y() + scroll);
            if (i >= 0) {
                MenuNesting.Row r = rows.get(i);
                if (r.checkbox()) {
                    Rect box = boxRect(list, list.y() + MenuNesting.top(rows, i) - scroll);
                    if (box.inset(-3).contains(mx, my)) toggle(player, r);
                }
            }
        }
        return true;                                                       // a click anywhere on the panel belongs to the panel
    }

    private static void toggle(Player player, MenuNesting.Row r) {
        IPlayerSkillTree t = SkillTreeApi.of(player);
        if (t == null || !SkillTreeApi.isEnabled() || !t.hasRoute(routeId)) return;
        boolean next = MenuCheckbox.next(t.isPassiveEnabled(routeId, r.node().id()));
        TreeActions.toggle(routeId, r.node().id(), next);
        t.setPassiveEnabled(routeId, r.node().id(), next);                 // our copy only; the server's tree overwrites it, and a refusal sends the tree again
        TreeSounds.click();
        rowsFresh = false;
    }

    /** True when the wheel was over the panel's list (the caller cancels the event). */
    static boolean mouseScrolled(double mx, double my, double dy) {
        if (panel == null || !panel.contains(mx, my)) return false;
        if (list != null && list.contains(mx, my)) scroll = MenuNesting.clampScroll(rows, scroll - (int) Math.round(dy * MenuNesting.ROW_H * 2), list.h());
        return true;
    }
}
