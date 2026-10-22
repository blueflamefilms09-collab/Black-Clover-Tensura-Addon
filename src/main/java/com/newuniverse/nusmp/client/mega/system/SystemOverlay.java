package com.newuniverse.nusmp.client.mega.system;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.system.PointsText;
import com.newuniverse.nusmp.labyrinth.mega.system.Stat;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemNet;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Why: this is the game-side half of the holographic "System" window, kept as thin as it can be. In NeoForge 1.21 the old {@code IGuiOverlay} no longer exists;
 * the form is a {@code LayeredDraw.Layer} registered with {@code RegisterGuiLayersEvent}, which {@link SystemClient} does, and this class is the layer's
 * {@code render}. All it does is decide WHETHER to draw (the player's switches, the dimension, an open screen, the hide-GUI key), translate the words, hand the
 * facts to the pure {@link OverlayLayout} and replay the list of marks it returns on the {@code GuiGraphics}: fills, one gradient kind, the default font. No
 * textures, no shaders, a few dozen calls. A layer is NOT hidden by the game when a screen (chat, inventory) is open, nor when the GUI is hidden (only the vanilla
 * layers are), so both are checked here. It must never take a frame down: the whole draw is inside a try, the pose stack is popped in a finally, and a failure
 * switches the window off until the server sends news ({@link ClientSystem#broken}) with one log line instead of one per frame.
 *
 * <p>The window shows for every player in the dungeon dimensions (the Mega-Dungeon on both roads, and the Hub, where it is the status window with no floor
 * and no quest), at the place the player chose ({@link SystemClientConfig#place}, the size {@link SystemClientConfig#scale}). The place and the size are set with
 * the mouse in {@link SystemEditScreen}, which draws the window through {@link #drawPreview} (the real window, or a labelled sample when the real one is not on).
 * While free stat points are waiting, the header carries an amber badge with their number and the key that opens the stat screen.
 */
public final class SystemOverlay {
    private SystemOverlay() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static OverlayLayout layout;
    private static Font layoutFont;

    /** The layer: called every frame by the GUI. Open screens are left alone (the edit screen draws the window itself, through {@link #drawPreview}). */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && !GuiMove.editing()) return;                          // GUI Move's edit mode shows the real window while it is moved
        run(g, mc, delta.getGameTimeDeltaPartialTick(false));
    }

    private static void run(GuiGraphics g, Minecraft mc, float partialTick) {
        if (ClientSystem.broken) return;
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        boolean dungeon = Labyrinth.inMega(mc.level) || Labyrinth.inSanctuary(mc.level);
        boolean normal = SystemClientConfig.enabled() && !ClientSystem.hidden && dungeon;
        boolean live = normal && ClientSystem.active && !GuiMove.hidden(SystemClientConfig.GUI_MOVE_ID);     // GUI Move: the player can switch the window off (the pop-up lines stay)
        boolean toasts = normal && SystemClientConfig.toasts() && !ClientSystem.TOASTS.isEmpty();
        if (!live && !toasts) return;
        boolean pushed = false;
        try {
            float gs = com.newuniverse.nusmp.client.ui.GuiScale.hud("system_hud");
            float scale = SystemClientConfig.scale() * gs;     // GUI Size x GUI Move: the window's own size (set in either edit mode) times the player's GUI size, one product; its place is the stored anchor + offset
            g.pose().pushPose();
            pushed = true;
            g.pose().scale(scale, scale, 1.0f);
            draw(g, mc, partialTick, scale, gs, live, toasts);
        } catch (RuntimeException | LinkageError e) {
            ClientSystem.broken = true;
            LOG.warn("[nusmp] The System window failed to draw and is switched off until the server sends news", e);
        } finally {
            if (pushed) g.pose().popPose();
        }
    }

    private static OverlayLayout.View view(GuiGraphics g, float scale, float time) {
        return new OverlayLayout.View(g.guiWidth(), g.guiHeight(), scale, SystemClientConfig.anchor(), SystemClientConfig.alpha(), time,
                SystemClientConfig.offsetX(), SystemClientConfig.offsetY(), SystemClientConfig.centerX(), SystemClientConfig.centerY());
    }

    private static OverlayLayout layoutFor(Font font) {
        if (layout == null || layoutFont != font) {
            layout = new OverlayLayout(font::width);
            layoutFont = font;
        }
        return layout;
    }

    private static void draw(GuiGraphics g, Minecraft mc, float partialTick, float scale, float guiScale, boolean showPanel, boolean showToasts) {
        Font font = mc.font;
        OverlayLayout lay = layoutFor(font);
        long gameTime = mc.level.getGameTime();
        float time = gameTime + partialTick;
        OverlayLayout.View view = view(g, scale, time);
        int toastY = OverlayStyle.MARGIN + 2;
        if (showPanel) {
            OverlayLayout.Frame frame = lay.panel(panel(time, gameTime), view);
            GuiMove.reportDrawn(SystemClientConfig.GUI_MOVE_ID, frame.x() * (double) scale, frame.y() * (double) scale, frame.w(), frame.h(), scale, guiScale);     // GUI Move: where the window really is
            replay(g, font, frame.ops());
            toastY = OverlayMath.toastTop(frame.x(), frame.y(), frame.w(), frame.h(), view.h(), OverlayStyle.TOAST_COLUMN_W, OverlayStyle.MARGIN, 8);
        }
        if (showToasts) replay(g, font, lay.toasts(toastViews(time), OverlayStyle.MARGIN, toastY));
    }

    /**
     * Draws the window for {@link SystemEditScreen}: the real one when the System has a picture for the player, else the labelled sample (outside the dungeon, switched
     * off, hidden), at the place and size in the client config as they are RIGHT NOW (so a drag shows at once). Returns the frame (the window's rectangle in its own
     * pixels), or null when the drawing failed. Never throws.
     */
    static OverlayLayout.Frame drawPreview(GuiGraphics g, Minecraft mc, float partialTick) {
        boolean pushed = false;
        try {
            float scale = SystemClientConfig.scale();
            g.pose().pushPose();
            pushed = true;
            g.pose().scale(scale, scale, 1.0f);
            OverlayLayout lay = layoutFor(mc.font);
            long gameTime = mc.level == null ? 0L : mc.level.getGameTime();
            float time = gameTime + partialTick;
            boolean dungeon = mc.level != null && (Labyrinth.inMega(mc.level) || Labyrinth.inSanctuary(mc.level));
            boolean live = dungeon && ClientSystem.active && !ClientSystem.broken;
            OverlayLayout.Frame frame = lay.panel(live ? panel(time, gameTime) : samplePanel(), view(g, scale, time));
            replay(g, mc.font, frame.ops());
            return frame;
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] The System window preview failed to draw", e);
            return null;
        } finally {
            if (pushed) g.pose().popPose();
        }
    }

    /** The marks, in order, on the graphics. */
    private static void replay(GuiGraphics g, Font font, List<Op> ops) {
        for (Op op : ops) {
            switch (op) {
                case Op.Fill f -> g.fill(f.x1(), f.y1(), f.x2(), f.y2(), f.argb());
                case Op.Gradient gr -> g.fillGradient(gr.x1(), gr.y1(), gr.x2(), gr.y2(), gr.top(), gr.bottom());
                case Op.Text t -> g.drawString(font, t.text(), t.x(), t.y(), t.argb(), t.shadow());
            }
        }
    }

    // ================================================================ what the window says
    private static String tr(String key, Object... args) { return Component.translatableWithFallback(key, SystemText.english(key), args).getString(); }

    private static OverlayLayout.Panel panel(float time, long gameTime) {
        boolean holy = "holy".equals(ClientSystem.polarity), abyssal = "abyssal".equals(ClientSystem.polarity);
        int road = holy ? 1 : abyssal ? 2 : 0;
        OverlayLayout.Quest quest = ClientSystem.questState == SystemNet.Q_NONE || ClientSystem.boss ? null : quest(time, gameTime);
        String noQuest = ClientSystem.floor == 0 ? tr(SystemText.HUB) : tr(SystemText.NO_QUEST);               // Floor 0 is the Hub: a status window, no floor quests there
        return new OverlayLayout.Panel(tr(SystemText.HEADER), tr(SystemText.FLOOR), ClientSystem.floor, road, abyssal ? tr(SystemText.ABYSSAL) : tr(SystemText.HOLY),
                tr(SystemText.THREAT), ClientSystem.threat, ClientSystem.boss, tr(SystemText.BOSS_FLOOR), noQuest, quest, stats(),
                ClientPoints.free(), ClientPoints.free() > 0 ? tr(PointsText.BADGE, ClientPoints.free(), keyName(SystemClient.POINTS)) : "");
    }

    /** The stat row of the player's points, or null when the player switched the row off. */
    private static OverlayLayout.Stats stats() {
        if (!SystemClientConfig.showStats()) return null;
        return new OverlayLayout.Stats(new String[]{tr(SystemText.STAT_STR), tr(SystemText.STAT_AGI), tr(SystemText.STAT_VIT), tr(SystemText.STAT_INT), tr(SystemText.STAT_SENSE)},
                new int[]{ClientSystem.stat(Stat.STR), ClientSystem.stat(Stat.AGI), ClientSystem.stat(Stat.VIT), ClientSystem.stat(Stat.INT), ClientSystem.stat(Stat.SENSE)});
    }

    /** A labelled sample window for edit mode when the real one is not on (outside the dungeon, switched off, hidden): the player can set its place anywhere. */
    private static OverlayLayout.Panel samplePanel() {
        OverlayLayout.Quest quest = new OverlayLayout.Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, tr(SystemText.EDIT_SAMPLE_QUEST, 5), -1, false, tr(SystemText.UNHARMED),
                tr(SystemText.COMPLETE), tr(SystemText.FAILED), "", 2, 5, tr(SystemText.TIME), -1, 100, false);
        return new OverlayLayout.Panel(tr(SystemText.HEADER), tr(SystemText.FLOOR), 3, 1, tr(SystemText.HOLY), tr(SystemText.THREAT), 3, false, tr(SystemText.BOSS_FLOOR),
                tr(SystemText.NO_QUEST), quest, stats());
    }

    private static String keyName(KeyMapping k) { return k.getTranslatedKeyMessage().getString(); }

    private static OverlayLayout.Quest quest(float time, long gameTime) {
        int state = switch (ClientSystem.questState) {
            case SystemNet.Q_DONE -> OverlayLayout.Q_DONE;
            case SystemNet.Q_FAILED -> OverlayLayout.Q_FAILED;
            default -> OverlayLayout.Q_ACTIVE;
        };
        int mode = "speed".equals(ClientSystem.kind) ? OverlayLayout.MODE_CLOCK : "no_damage_stretch".equals(ClientSystem.kind) ? OverlayLayout.MODE_STATE : OverlayLayout.MODE_COUNT;
        String title = Language.getInstance().has(ClientSystem.titleKey)
                ? Component.translatable(ClientSystem.titleKey, ClientSystem.target).getString() : ClientSystem.titleFallback;
        if (Float.isNaN(ClientSystem.questSince)) ClientSystem.questSince = time;                  // first time on screen: the title types itself out
        float age = time - ClientSystem.questSince;
        boolean typing = !ToastTimeline.done(age) && ToastTimeline.visibleChars(age, title.length()) < title.length();
        int chars = typing ? ToastTimeline.visibleChars(age, title.length()) : -1;
        boolean cursor = ToastTimeline.cursor(age, title.length());
        String foreign = ClientSystem.questFloor != 0 && ClientSystem.questFloor != ClientSystem.floor ? tr(SystemText.QUEST_FLOOR, OverlayLayout.label(ClientSystem.questFloor)) : "";
        return new OverlayLayout.Quest(state, mode, title, chars, cursor, tr(SystemText.UNHARMED), tr(SystemText.COMPLETE), tr(SystemText.FAILED), foreign,
                ClientSystem.progress, ClientSystem.target, tr(SystemText.TIME), ClientSystem.timeLeftNow(gameTime), ClientSystem.target * 20,
                ClientSystem.clockRunning && state == OverlayLayout.Q_ACTIVE && ClientSystem.floor == ClientSystem.questFloor);
    }

    /** The first few pop-up lines with their ages; a line starts its life the first time it is drawn, and a line that has lived out its time is dropped. */
    private static List<OverlayLayout.ToastView> toastViews(float time) {
        List<OverlayLayout.ToastView> out = new ArrayList<>(OverlayStyle.TOAST_MAX);
        String tag = tr(SystemText.TAG);
        Iterator<ClientSystem.Toast> it = ClientSystem.TOASTS.iterator();
        int shown = 0;
        while (it.hasNext() && shown < OverlayStyle.TOAST_MAX) {
            ClientSystem.Toast t = it.next();
            if (Float.isNaN(t.start)) t.start = time;
            float age = time - t.start;
            if (ToastTimeline.done(age)) {
                it.remove();
                continue;
            }
            if (t.message == null) t.message = message(t);
            out.add(new OverlayLayout.ToastView(tag, t.message, t.kind.code(), age));
            shown++;
        }
        return out;
    }

    /** A toast's message: its key with the arguments in; an argument that starts with "@" is a lang key (an entity name), and names are shown in capitals like the rest. */
    private static String message(ClientSystem.Toast t) {
        Object[] args = new Object[t.args.size()];
        for (int i = 0; i < args.length; i++) {
            String a = t.args.get(i);
            args[i] = a.startsWith("@") ? Component.translatable(a.substring(1)).getString().toUpperCase(Locale.ROOT) : a;
        }
        return tr(t.key, args);
    }
}
