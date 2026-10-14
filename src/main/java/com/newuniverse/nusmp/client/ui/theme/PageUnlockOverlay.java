package com.newuniverse.nusmp.client.ui.theme;

import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The mastery page-unlock notification of the anime look. The server already tells the player in chat when mastery opens a grimoire page
 * ({@code MasteryPages.tryUnlock}) and syncs the new status; this overlay reads that status (client side, nothing on the server changes),
 * notices a page that was not open in the previous status of the same grimoire ({@link PageUnlockTracker}) and shows a Great-Sage analysis
 * panel at the top of the screen: an animated magic circle that builds up, the page's name, a typed "Notice / description / Result" block and a
 * mastery bar. Several unlocks queue up and show one after another. In the classic look nothing is shown (the chat message is the whole
 * notification, as before).
 */
public final class PageUnlockOverlay {
    private PageUnlockOverlay() {}

    private static final int W = 300;
    private static final PageUnlockTracker TRACKER = new PageUnlockTracker();
    private static final ArrayDeque<Toast> QUEUE = new ArrayDeque<>();
    private static final SageBlock SAGE = new SageBlock();
    private static final UiBar BAR = new UiBar();
    private static CompoundTag seen;
    private static Toast current;
    private static float shownAt;
    private static String descShown = "";

    /** One page that opened: what the toast prints. */
    record Toast(String name, String desc, int cost, int cooldown, int mastery, int unlocked, int pages) {}

    /** Mod bus. */
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "page_unlock"), PageUnlockOverlay::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
    }

    private static void clear() {
        TRACKER.reset();
        QUEUE.clear();
        current = null;
        seen = null;
    }

    /** Client tick: a new status object means the server synced; look at the grimoire's open pages (cheap: only on a change). */
    private static void tick() {
        CompoundTag s = MultiverseStatusClient.status();
        if (s == seen) return;
        seen = s;
        CompoundTag gr = s.getCompound("Grimoire");
        ListTag list = gr.getList("PageList", Tag.TAG_COMPOUND);
        List<String> ids = new ArrayList<>(list.size());
        Map<String, CompoundTag> byId = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            String id = e.getString("Id");
            ids.add(id);
            byId.put(id, e);
        }
        List<String> fresh = TRACKER.update(gr.getString("Cover") + "|" + gr.getString("MagicId"), ids);
        if (fresh.isEmpty() || !UiTheme.anime() || !UiThemeOptions.pageToast()) return;
        for (String id : fresh) {
            CompoundTag e = byId.get(id);
            if (e == null) continue;
            QUEUE.add(new Toast(e.getString("Name"), describe(e.getString("Key"), e.getString("Name")), e.getInt("Cost"), e.getInt("Cooldown"),
                    gr.getInt("Mastery"), gr.getInt("Unlocked"), gr.getInt("Pages")));
        }
    }

    /** The page's description with its leading "Name - " cut off (descriptions are written "Name - what it does"); the toast clamps it to two lines. */
    static String describe(String key, String name) {
        if (key.isEmpty() || !UiLang.has(key)) return "";
        String text = UiLang.get(key);
        int dash = text.indexOf(" - ");
        if (dash > 0 && dash < 60) text = text.substring(dash + 3);
        return text;
    }

    /** GUI layer, above everything. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        float now = UiTheme.realNow();
        if (current == null) {
            current = QUEUE.poll();
            if (current == null) return;
            shownAt = now;
            descShown = SageLayout.clamp(current.desc(), mc.font::width, W - 80, 2);       // at most two lines of description
            SAGE.restart(UiTheme.now() + ToastTimeline.TEXT_START);
        }
        float age = now - shownAt;
        if (ToastTimeline.done(age)) { current = null; return; }
        Toast t = current;
        Font font = mc.font;
        float alpha = ToastTimeline.alpha(age);
        float anim = UiTheme.now();
        int tx0 = 72, tw = W - 80;
        String result = UiLang.get(UiLang.RESULT, t.cooldown() > 0 ? UiLang.get("nusmp.ui.toast.cost", t.cost(), t.cooldown()) : UiLang.get("nusmp.ui.toast.cost_nocd", t.cost()));
        SAGE.set(font, UiLang.get("nusmp.ui.toast.page_open"), descShown, result, tw, anim + ToastTimeline.TEXT_START);
        int h = Math.max(62, SAGE.height(font) + 35);                       // name, the text, the bar: the panel is as tall as its text
        int x = (g.guiWidth() - W) / 2;
        int y = 8 - Math.round((1f - ToastTimeline.slide(age)) * (h + 16));

        UiPanel.panelScan(g, x, y, W, h, UiTheme.CYAN, 1f, alpha, anim);
        MagicCircle.draw(g, x + 34, y + h / 2f, 29f, anim, UiTheme.CYAN, alpha, MagicCircle.STANDARD, ToastTimeline.build(age));

        int tx = x + tx0;
        g.drawString(font, Component.literal(t.name()).withStyle(st -> st.withBold(true)), tx, y + 6, UiMath.scaleAlpha(UiTheme.GOLD, alpha), true);
        SAGE.draw(g, font, tx, y + 19, anim, alpha);

        BAR.update(t.mastery() / 100f, anim, true);
        String label = UiLang.get("nusmp.ui.toast.mastery", t.mastery(), t.unlocked(), t.pages());
        int lw = font.width(label);
        BAR.draw(g, tx, y + h - 11, tw - lw - 6, 4, UiTheme.VIOLET, anim, alpha);
        if (((UiMath.scaleAlpha(UiTheme.TEXT_DIM, alpha)) >>> 24) >= 8)
            g.drawString(font, label, tx + tw - lw, y + h - 13, UiMath.scaleAlpha(UiTheme.TEXT_DIM, alpha), false);
    }
}
