package com.newuniverse.nusmp.client.mega.system;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.system.Stat;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemNet;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import net.minecraft.client.DeltaTracker;
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
 */
public final class SystemOverlay {
    private SystemOverlay() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static OverlayLayout layout;
    private static Font layoutFont;

    /** The layer: called every frame by the GUI. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        if (ClientSystem.broken) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        if (!SystemClientConfig.enabled() || ClientSystem.hidden || !Labyrinth.inMega(mc.level)) return;
        boolean panel = ClientSystem.active;
        boolean toasts = SystemClientConfig.toasts() && !ClientSystem.TOASTS.isEmpty();
        if (!panel && !toasts) return;
        boolean pushed = false;
        try {
            float scale = SystemClientConfig.scale();
            g.pose().pushPose();
            pushed = true;
            g.pose().scale(scale, scale, 1.0f);
            draw(g, mc, delta, scale, panel, toasts);
        } catch (RuntimeException | LinkageError e) {
            ClientSystem.broken = true;
            LOG.warn("[nusmp] The System window failed to draw and is switched off until the server sends news", e);
        } finally {
            if (pushed) g.pose().popPose();
        }
    }

    private static void draw(GuiGraphics g, Minecraft mc, DeltaTracker delta, float scale, boolean showPanel, boolean showToasts) {
        Font font = mc.font;
        if (layout == null || layoutFont != font) {
            layout = new OverlayLayout(font::width);
            layoutFont = font;
        }
        long gameTime = mc.level.getGameTime();
        float time = gameTime + delta.getGameTimeDeltaPartialTick(false);
        OverlayLayout.View view = new OverlayLayout.View(g.guiWidth(), g.guiHeight(), scale, SystemClientConfig.anchor(), SystemClientConfig.alpha(), time);
        int toastY = OverlayStyle.MARGIN + 2;
        if (showPanel) {
            OverlayLayout.Frame frame = layout.panel(panel(time, gameTime), view);
            replay(g, font, frame.ops());
            if (SystemClientConfig.anchor() == 0) toastY = frame.y() + frame.h() + 8;
        }
        if (showToasts) replay(g, font, layout.toasts(toastViews(time), OverlayStyle.MARGIN, toastY));
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
        OverlayLayout.Stats stats = null;
        if (SystemClientConfig.showStats()) {
            stats = new OverlayLayout.Stats(new String[]{tr(SystemText.STAT_STR), tr(SystemText.STAT_AGI), tr(SystemText.STAT_VIT), tr(SystemText.STAT_INT), tr(SystemText.STAT_SENSE)},
                    new int[]{ClientSystem.stat(Stat.STR), ClientSystem.stat(Stat.AGI), ClientSystem.stat(Stat.VIT), ClientSystem.stat(Stat.INT), ClientSystem.stat(Stat.SENSE)});
        }
        return new OverlayLayout.Panel(tr(SystemText.HEADER), tr(SystemText.FLOOR), ClientSystem.floor, road, abyssal ? tr(SystemText.ABYSSAL) : tr(SystemText.HOLY),
                tr(SystemText.THREAT), ClientSystem.threat, ClientSystem.boss, tr(SystemText.BOSS_FLOOR), tr(SystemText.NO_QUEST), quest, stats);
    }

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
