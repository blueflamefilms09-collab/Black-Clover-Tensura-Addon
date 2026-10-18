package com.newuniverse.nusmp.client.elite.records;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.multiverse.FourKingdomsScreen;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusScreen;
import com.newuniverse.nusmp.client.skilltree.layout.ButtonPlacement;
import com.newuniverse.nusmp.client.skilltree.layout.ButtonPlacement.Menu;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.ui.CodexScreen;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.elite.records.RecordsText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Why: the owner wants new screens reachable from the menu the player already has, with the existing menus left exactly as they are (add the new, keep the old). This is the same pattern as the Skill
 * Tree's button ({@code client.skilltree.MenuInjection}) in a class of its own that edits none of it: a NeoForge {@code ScreenEvent.Init.Post} listener adds a "Records" button to the Codex (and to the two
 * older Four Kingdoms / Status screens, which are kept in the code) matched by the screen's EXACT class, so another mod's screen or a subclass is never touched. The listener runs at LOW priority, after the
 * Skill Tree's, so the widgets that listener added are already in the screen's list and the shared {@link ButtonPlacement} keeps this button clear of them and of every other widget. On a rebuild
 * of the screen the old button is removed first (it can never double), and if a frame is drawn without the button in the screen's list (an event that did not fire) it is put back. The handlers never throw:
 * a surprise is logged once and the menu is left as it was, because they run inside a screen's initialisation.
 */
final class RecordsMenuInjection {
    private RecordsMenuInjection() {}

    private record Active(Screen screen, ThemedButton button, Consumer<GuiEventListener> add) {}

    private static Active active;
    private static boolean warned;

    static void init() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, RecordsMenuInjection::onInit);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, RecordsMenuInjection::onRender);
    }

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
            if (a == null || a.screen() != event.getScreen() || a.screen().children().contains(a.button())) return;
            Menu menu = menuOf(a.screen());
            if (menu == null) return;
            Rect r = place(menu, a.screen(), new ArrayList<>(a.screen().children()), a.button().getMessage());
            a.button().setPosition(r.x(), r.y());
            a.add().accept(a.button());
        } catch (RuntimeException e) {
            warn(e);
        }
    }

    private static void warn(RuntimeException e) {
        if (warned) return;
        warned = true;
        LogUtils.getLogger().warn("[nusmp] The Records button could not be added to a menu; the menu is unchanged", e);
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
        if (active != null && active.screen() == screen) event.removeListener(active.button());      // the event fires again after a rebuild: never two buttons
        Component label = RecordsText.tr(RecordsText.BUTTON);
        Rect r = place(menu, screen, new ArrayList<>(event.getListenersList()), label);
        ThemedButton button = new ThemedButton(r.x(), r.y(), r.w(), r.h(), label, b -> RecordsClient.open(screen));
        button.setTooltip(Tooltip.create(RecordsText.tr(RecordsText.BUTTON_TIP)));
        event.addListener(button);
        active = new Active(screen, button, event::addListener);
    }

    /** Where the button goes in {@code screen}, given the widgets the screen has now (every visible widget is kept clear of). */
    private static Rect place(Menu menu, Screen screen, List<GuiEventListener> children, Component label) {
        List<Rect> occupied = new ArrayList<>();
        for (GuiEventListener l : children) {
            if (l instanceof AbstractWidget w && w.visible && !(active != null && w == active.button())) occupied.add(new Rect(w.getX(), w.getY(), w.getWidth(), w.getHeight()));
        }
        return ButtonPlacement.place(menu, screen.width, screen.height, Minecraft.getInstance().font.width(label), occupied);
    }
}
