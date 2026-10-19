package com.newuniverse.nusmp.client.mega.system;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorCard;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorCardLines;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfoNet;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfoRules;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfoText;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: the client half of the System's floor report. The server sends one {@link FloorCard} on every arrival (and on a key press and on a mid-floor change); this keeps the
 * last one, draws it as a second System window (the pure {@link FloorCardLayout}, replayed here: fills, a gradient, the default font, nothing else) in the same holographic
 * style as the status window, fades it in and out, and owns the key that asks the server for the report again ("Show the floor report", default period, in the
 * "Multiverse: System" controls category; pressed while the report is up it dismisses it). The words are composed by {@link FloorCardLines} and translated here with the
 * player's own language, each key falling back to the English the card carries, so a theme or mechanic with no lang text still reads. The player's own switch is the client
 * config's {@code floorCard} (off = no report by itself; the key and the command still show it). Like the status window, a failed draw switches the report off for the session
 * with ONE log line instead of one per frame, and nothing is drawn with a screen open or the GUI hidden. Client only; started from {@code SystemClient.init}.
 */
public final class FloorCardClient {
    private FloorCardClient() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** Show the floor report (default period). */
    public static final KeyMapping KEY = new KeyMapping(FloorInfoText.KEY_FLOOR, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_PERIOD, SystemText.KEY_CATEGORY);

    private static FloorCard card;
    /** Game time (plus partial tick) the report started to show; NaN until it is first drawn, and set to -1 when it is dismissed. */
    private static float since = Float.NaN;
    private static int total;
    private static boolean broken;
    private static FloorCardLayout layout;
    private static Font layoutFont;
    private static List<FloorCardLines.Line> cached;
    private static FloorCard cachedFor;

    /** Called from {@code SystemClient.init}: the layer, the key, the logout clean-up. */
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "system_floor_card"), FloorCardClient::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
    }

    static void clear() {
        card = null;
        cached = null;
        cachedFor = null;
        since = Float.NaN;
        broken = false;
    }

    /** A report arrived (client thread). A repeat of what is already on screen is ignored; an unwanted automatic report is kept for the key but not shown. */
    public static void receive(FloorCard c) {
        broken = false;
        FloorInfoRules.Reason r = c.reasonKind();
        boolean asked = r == FloorInfoRules.Reason.REQUEST;
        card = c;
        cached = null;
        if (!asked && !SystemClientConfig.floorCard()) {
            since = -1f;                                                              // kept for the key, not shown by itself
            return;
        }
        since = Float.NaN;                                                            // starts when it is first drawn
        int seconds = SystemClientConfig.floorCardSeconds();
        total = (asked ? Math.round(seconds * 1.5f) : r == FloorInfoRules.Reason.UPDATE ? Math.max(3, seconds / 2) : seconds) * 20;
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            if (card != null && since >= 0f && !Float.isNaN(since) && visible(mc)) {
                since = -1f;                                                          // pressed while it is up: dismiss
                continue;
            }
            PacketDistributor.sendToServer(new FloorInfoNet.Request());               // the server rebuilds it, so a mechanic that changed since is in it
        }
    }

    private static boolean visible(Minecraft mc) {
        return mc.level != null && since >= 0f && !Float.isNaN(since) && (mc.level.getGameTime() - since) < total;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (card == null || broken || since < 0f || mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        if (!Labyrinth.inMega(mc.level) && !Labyrinth.inSanctuary(mc.level)) return;
        try {
            float time = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
            if (Float.isNaN(since)) since = time;
            float age = time - since;
            float fade = FloorCardLayout.fade(age, total);
            if (fade <= 0f) {
                since = -1f;
                return;
            }
            Font font = mc.font;
            if (layout == null || layoutFont != font) {
                layout = new FloorCardLayout(font::width);
                layoutFont = font;
            }
            if (cached == null || cachedFor != card) {
                cached = FloorCardLines.compose(card, FloorCardClient::tr, KEY.getTranslatedKeyMessage().getString(), true);
                cachedFor = card;
            }
            int road = card.floor() > 0 ? 1 : 2;
            FloorCardLayout.Frame f = layout.layout(cached, tr(SystemText.HEADER, "[ SYSTEM ]"), road, g.guiWidth(), g.guiHeight(), SystemClientConfig.alpha(), fade, time);
            for (Op op : f.ops()) {
                switch (op) {
                    case Op.Fill fl -> g.fill(fl.x1(), fl.y1(), fl.x2(), fl.y2(), fl.argb());
                    case Op.Gradient gr -> g.fillGradient(gr.x1(), gr.y1(), gr.x2(), gr.y2(), gr.top(), gr.bottom());
                    case Op.Text t -> g.drawString(font, t.text(), t.x(), t.y(), t.argb(), t.shadow());
                }
            }
        } catch (RuntimeException | LinkageError e) {
            broken = true;
            LOG.warn("[nusmp] The System floor report failed to draw and is switched off until the server sends the next one", e);
        }
    }

    /** The player's language, else the English the caller passes; the arguments are put in either way. */
    private static String tr(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(key, fallback, args).getString();
    }
}
