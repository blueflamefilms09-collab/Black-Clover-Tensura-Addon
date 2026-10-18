package com.newuniverse.nusmp.client.skilltree;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.multiverse.FourKingdomsScreen;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusScreen;
import com.newuniverse.nusmp.client.skilltree.layout.ButtonPlacement;
import com.newuniverse.nusmp.client.skilltree.layout.ButtonPlacement.Menu;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.ui.CodexScreen;
import com.newuniverse.nusmp.skilltree.line.LineText;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Why: the owner asked for the Skill Tree to live in the menu the player already has, with no separate keybind, and for the existing menus to stay exactly as they are (add the new,
 * keep the old). A NeoForge screen event does both: {@code ScreenEvent.Init.Post} fires every time a screen has built its widgets (on open and again on every resize, after the
 * widget list was cleared), hands over the screen and its widget list, and lets a listener add a widget, so the button is added from outside, in one removable class, and no line of
 * a host menu changes. It is matched by the screen's exact class, not by name or by a prefix, so another mod's screen (or a subclass) is never touched, and a redesign of a menu
 * cannot make the button land on the wrong screen. The live menu is the Codex ({@code client.ui.CodexScreen}): since 1.11.0 the Four Kingdoms slot in Tensura's magic screen and the
 * Multiverse key open it, and the older {@code FourKingdomsScreen} and {@code MultiverseStatusScreen} are kept in the code but nothing opens them; they are injected too, as the
 * design lists them, so the button is there the day either is linked again. Where the button goes is {@link ButtonPlacement}'s job; it is told which widgets the screen really has,
 * so the button is never put over a control. The handler never throws (a surprise is logged once and the menu is left as it was), because it runs inside a screen's initialisation
 * and an exception there would crash the menu, not just the button.
 * <p>
 * Two guards against the one thing that makes a widget injected from outside fragile, a screen that rebuilds its widgets without the event: a resize (or returning from the Skill Tree
 * screen to its parent) clears the widget list and runs {@code init()} again, and the event is expected to fire again on that fresh list, so the previous button is removed first and
 * the button can never double; and if the button turns out to be missing from the screen while it is drawn (an event that did not fire) the next frame puts it back where it belongs.
 * Only the screen that is currently open is tracked, in one field, so nothing accumulates, and the field is cleared on logout.
 */
final class MenuInjection {
    private MenuInjection() {}

    /** The button of the open host screen and the function that adds a widget to that screen (the event's own, so the screen's lists stay consistent). */
    private record Active(Screen screen, ThemedButton button, ThemedButton passives, Consumer<GuiEventListener> add) {}

    private static Active active;
    private static boolean warned;

    static void init() {
        NeoForge.EVENT_BUS.addListener(MenuInjection::onInit);
        NeoForge.EVENT_BUS.addListener(MenuInjection::onRender);
    }

    /** Forgets the tracked screen (the player left the world). */
    static void forget() { active = null; }

    private static void onInit(ScreenEvent.Init.Post event) {
        try {
            inject(event);
        } catch (RuntimeException e) {
            warn(e);
        }
    }

    private static void onRender(ScreenEvent.Render.Pre event) {
        try {
            Active a = active;
            if (a == null || a.screen() != event.getScreen() || (a.screen().children().contains(a.button()) && a.screen().children().contains(a.passives()))) return;
            Menu menu = menuOf(a.screen());                                  // the event did not run after the last rebuild: put the buttons back where the menu wants them
            if (menu == null) return;
            Rect r = place(menu, a.screen(), new ArrayList<>(a.screen().children()), SkillTreeLang.tr(SkillTreeLang.BUTTON), null);
            a.button().setPosition(r.x(), r.y());
            if (!a.screen().children().contains(a.button())) a.add().accept(a.button());
            Rect r2 = place(menu, a.screen(), new ArrayList<>(a.screen().children()), LineText.tr(LineText.PASSIVES_BUTTON), r);
            a.passives().setPosition(r2.x(), r2.y());
            if (!a.screen().children().contains(a.passives())) a.add().accept(a.passives());
        } catch (RuntimeException e) {
            warn(e);
        }
    }

    private static void warn(RuntimeException e) {
        if (warned) return;
        warned = true;
        LogUtils.getLogger().warn("[nusmp] The Skill Tree button could not be added to a menu; the menu is unchanged", e);
    }

    /** The menu a screen is, by its exact class, or null for every other screen. */
    static Menu menuOf(Screen screen) {
        Class<?> c = screen.getClass();
        if (c == CodexScreen.class) return Menu.CODEX;
        if (c == FourKingdomsScreen.class) return Menu.FOUR_KINGDOMS;
        if (c == MultiverseStatusScreen.class) return Menu.STATUS;
        return null;
    }

    private static void inject(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        Menu menu = menuOf(screen);
        if (menu == null) return;
        if (active != null && active.screen() == screen) {                                            // the event fires again after a rebuild: never two buttons
            event.removeListener(active.button());
            event.removeListener(active.passives());
        }
        Component label = SkillTreeLang.tr(SkillTreeLang.BUTTON);
        Rect r = place(menu, screen, new ArrayList<>(event.getListenersList()), label, null);
        ThemedButton button = new ThemedButton(r.x(), r.y(), r.w(), r.h(), label, b -> Minecraft.getInstance().setScreen(new SkillTreeScreen(screen)));
        button.setTooltip(Tooltip.create(SkillTreeLang.tr(SkillTreeLang.BUTTON_TIP)));
        event.addListener(button);
        // 1.82.0: the Passives panel of the Unique Skill Lines, a second button placed by the same rules, clear of the first
        Component label2 = LineText.tr(LineText.PASSIVES_BUTTON);
        Rect r2 = place(menu, screen, new ArrayList<>(event.getListenersList()), label2, r);
        ThemedButton passives = new ThemedButton(r2.x(), r2.y(), r2.w(), r2.h(), label2, b -> Minecraft.getInstance().setScreen(new PassivesScreen(screen)));
        passives.setTooltip(Tooltip.create(LineText.tr(LineText.PASSIVES_BUTTON_TIP)));
        event.addListener(passives);
        active = new Active(screen, button, passives, event::addListener);
    }

    /** Where the button goes in {@code screen}, given the widgets the screen has now (every visible widget is kept clear of). */
    private static Rect place(Menu menu, Screen screen, List<GuiEventListener> children, Component label, Rect alsoClearOf) {
        List<Rect> occupied = new ArrayList<>();
        for (GuiEventListener l : children) {
            if (l instanceof AbstractWidget w && w.visible && !(active != null && (w == active.button() || w == active.passives()))) occupied.add(new Rect(w.getX(), w.getY(), w.getWidth(), w.getHeight()));
        }
        if (alsoClearOf != null) occupied.add(alsoClearOf);
        return ButtonPlacement.place(menu, screen.width, screen.height, Minecraft.getInstance().font.width(label), occupied);
    }
}
