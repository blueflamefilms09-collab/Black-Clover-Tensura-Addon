package com.newuniverse.nusmp.client.skilltree;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.skilltree.layout.MenuEntries;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.menu.LineIndex;
import com.newuniverse.nusmp.skilltree.menu.LineStandInSkill;
import com.newuniverse.nusmp.skilltree.menu.LineStandIns;
import com.newuniverse.nusmp.skilltree.menu.MenuConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Why: this is the hook into Tensura's Ability Selection screen, built on screen EVENTS and not on a mixin: the owner's rule is to add and never replace, a mixin into a class whose source cannot be seen
 * is the most fragile way to touch it, and NeoForge's {@code ScreenEvent}s give exactly the three hand-holds that are needed from outside. (1) Before the screen draws (Render.Pre, which also runs after its
 * init) the list is RECONCILED: the stand-in of every line the player owns is in the screen's list, no other stand-in is (the fog of war), and for an operator with ADMIN INSPECT on, a synthetic stand-in of
 * every other line is added client-side, so all 283 appear in the left-hand list next to Tensura's own skills. It works on the screen's own public {@code abilities} list and asks the screen to rebuild its
 * {@code filtered} list with its own method, so Tensura's search and ordering rules apply to our entries too; it touches nothing but stand-ins and only when the pure {@link MenuEntries#diff} says something
 * changed (not every frame). (2) After the screen has drawn (Render.Post) the nested panel and the admin switch are drawn on top. (3) Mouse presses and the wheel over those two are handled and cancelled
 * so they never reach a widget underneath. Every handler catches and logs once: it runs inside Tensura's screen and an exception here would crash a menu the player did not ask to change. If Tensura's classes
 * ever change shape, the failure is logged and the screen simply behaves as Tensura wrote it.
 */
final class TensuraMenuClient {
    private TensuraMenuClient() {}

    private static boolean warned, listBroken;
    /** The stand-in route ids in the order the list wants: by the line's name, then id (a snapshot of the registry, which never changes). */
    private static final List<String> ORDER = order();

    static void init() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(TensuraMenuClient::onInit);
        bus.addListener(TensuraMenuClient::onRenderPre);
        bus.addListener(TensuraMenuClient::onRenderPost);
        bus.addListener(TensuraMenuClient::onClick);
        bus.addListener(TensuraMenuClient::onScroll);
        bus.addListener(TensuraMenuClient::onClosing);
    }

    private static List<String> order() {
        List<LineIndex.Line> lines = new ArrayList<>(LineStandIns.lines());
        lines.sort(Comparator.comparing((LineIndex.Line l) -> l.name().toLowerCase(Locale.ROOT)).thenComparing(LineIndex.Line::routeId));
        List<String> out = new ArrayList<>();
        for (LineIndex.Line l : lines) out.add(l.routeId());
        return List.copyOf(out);
    }

    private static void warn(Throwable t) {
        if (warned) return;
        warned = true;
        LogUtils.getLogger().warn("[nusmp] tensura menu: the integration with the Ability Selection screen failed once; the screen is left as Tensura draws it", t);
    }

    // ------------------------------------------------------------------ events
    private static void onInit(ScreenEvent.Init.Post e) {
        if (e.getScreen() instanceof AbilitySelectionScreen s) guarded(() -> reconcile(s));
    }

    private static void onRenderPre(ScreenEvent.Render.Pre e) {
        if (e.getScreen() instanceof AbilitySelectionScreen s) guarded(() -> reconcile(s));
    }

    private static void onRenderPost(ScreenEvent.Render.Post e) {
        if (e.getScreen() instanceof AbilitySelectionScreen s && MenuConfig.enabled()) guarded(() -> TensuraMenuPanel.render(s, e.getGuiGraphics(), e.getMouseX(), e.getMouseY()));
    }

    private static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!(e.getScreen() instanceof AbilitySelectionScreen s) || !MenuConfig.enabled()) return;
        try {
            if (TensuraMenuPanel.mouseClicked(s, e.getMouseX(), e.getMouseY(), e.getButton())) e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            warn(ex);
        }
    }

    private static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (!(e.getScreen() instanceof AbilitySelectionScreen) || !MenuConfig.enabled()) return;
        try {
            if (TensuraMenuPanel.mouseScrolled(e.getMouseX(), e.getMouseY(), e.getScrollDeltaY())) e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            warn(ex);
        }
    }

    private static void onClosing(ScreenEvent.Closing e) {
        if (e.getScreen() instanceof AbilitySelectionScreen) TensuraMenuPanel.reset();
    }

    private static void guarded(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException | LinkageError ex) {
            warn(ex);
        }
    }

    // ------------------------------------------------------------------ the list
    /** The route id a list entry stands for, or null when it is not one of our stand-ins. */
    private static String routeOf(ManasSkillInstance inst) { return inst != null && inst.getSkill() instanceof LineStandInSkill st ? st.routeId() : null; }

    /** Makes the stand-ins in the screen's list match the fog-of-war rule (owned lines; every line while an operator inspects). Cheap when nothing changed. */
    static void reconcile(AbilitySelectionScreen s) {
        if (listBroken || !MenuConfig.enabled()) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        List<String> present = new ArrayList<>();
        for (ManasSkillInstance inst : s.abilities) {
            String id = routeOf(inst);
            if (id != null) present.add(id);
        }
        boolean unique = TensuraScreenAccess.showsUniqueSkills(s);
        if (!unique && present.isEmpty()) return;
        List<String> wanted = MenuEntries.wanted(ORDER, SkillTreeApi.of(player).unlockedRoutes(), InspectMode.active(player));
        MenuEntries.Diff diff = MenuEntries.diff(present, wanted);
        if (!unique) diff = new MenuEntries.Diff(List.of(), diff.remove());                // other lists: only ever remove what the player may not see
        if (diff.isEmpty()) return;
        try {
            for (String id : diff.remove()) {
                s.abilities.removeIf(i -> id.equals(routeOf(i)));
                s.filtered.removeIf(i -> id.equals(routeOf(i)));
                if (id.equals(routeOf(s.selectedAbility))) s.selectedAbility = null;
            }
            List<ManasSkillInstance> added = new ArrayList<>();
            for (String id : diff.add()) {
                var holder = LineStandIns.holder(id);
                if (holder == null) continue;
                ManasSkillInstance inst = holder.get().createDefaultInstance();
                s.abilities.add(inst);
                added.add(inst);
            }
            if (!added.isEmpty() && !TensuraScreenAccess.refilter(s)) {
                String f = TensuraScreenAccess.nameFilter(s).toLowerCase(Locale.ROOT);
                for (ManasSkillInstance inst : added) if (f.isEmpty() || inst.getSkill().getName().getString().toLowerCase(Locale.ROOT).contains(f)) s.filtered.add(inst);
            }
        } catch (UnsupportedOperationException ex) {
            listBroken = true;                                                              // the screen's lists cannot be changed from outside: leave them alone for good
            LogUtils.getLogger().warn("[nusmp] tensura menu: the lists of Tensura's screen are read-only, so Unique Skill Lines cannot be added to them", ex);
        }
    }
}
