package com.newuniverse.nusmp.client.relics;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicActPayload;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.UseRules;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Use Worn Relic" key (default V, under Controls > Multiverse of Anime). The abilities of the relics a player wears (and of a held relic's
 * third act and beyond, which a right-click cannot reach) form one ring: the key uses the selected one, SHIFT + the key moves to the next. An ability
 * that must be held is charged while the key is down and fired when it is let go. A line at the bottom right of the screen shows the selection, the
 * way to use it and its cooldown. The server checks everything again ({@code RelicEngine.onActPacket}).
 */
public final class RelicActKeys {
    private RelicActKeys() {}

    public static final KeyMapping USE = new KeyMapping("key.nusmp.use_worn_relic", InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_V, "key.categories.nusmp");

    /** One ability on the ring. */
    record Entry(RelicDef def, Act act) {}

    private static int selected;
    private static boolean wasDown;
    private static Entry holding;
    private static long holdStart;

    /** Mod bus. */
    public static void register(RegisterKeyMappingsEvent e) { e.register(USE); }

    /** The abilities on the ring, in the order the player carries them. */
    static List<Entry> entries(Player p) {
        List<Entry> out = new ArrayList<>();
        for (Relics.Worn w : Relics.loadout(p).active()) {
            RelicDef d = w.def();
            int i = 0;
            for (Act a : d.acts()) {
                if (!(d.slot().isHand() && !d.isAdopted() && i < 2)) out.add(new Entry(d, a));   // a new hand relic's first two acts are the right-click and sneak + right-click (an old item has its own click)
                i++;
            }
        }
        return out;
    }

    /** Game bus, client tick. */
    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { holding = null; wasDown = false; return; }
        boolean down = USE.isDown() && mc.screen == null;
        while (USE.consumeClick()) { /* the edge is read from isDown below; clicks are drained so they do not pile up */ }
        long now = mc.level.getGameTime();
        List<Entry> ring = entries(mc.player);
        if (selected >= ring.size()) selected = 0;
        if (down && !wasDown) {
            if (ring.isEmpty()) {
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("You wear no relic with an ability."), true);
            } else if (Screen.hasShiftDown()) {
                selected = (selected + 1) % ring.size();
            } else {
                Entry en = ring.get(selected);
                if (en.act().holdTicks() > 0) { holding = en; holdStart = now; }
                else send(en, 0);
            }
        } else if (!down && wasDown && holding != null) {
            send(holding, (int) Math.min(1200, now - holdStart));
            holding = null;
        }
        if (!down) holding = null;
        wasDown = down;
    }

    private static void send(Entry en, int charge) {
        PacketDistributor.sendToServer(new RelicActPayload(en.def().id(), en.act().key(), charge));
    }

    /** The HUD line (a GUI layer, registered in {@link RelicClient}). */
    public static void renderHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        List<Entry> ring = entries(mc.player);
        if (ring.isEmpty()) return;
        Entry en = ring.get(Math.min(selected, ring.size() - 1));
        long now = mc.level.getGameTime();
        int left = RelicClientState.remaining(en.def().id(), en.act().key(), now);
        String line = "[" + USE.getTranslatedKeyMessage().getString() + "] " + en.def().cardName() + " ▸ " + en.act().displayName()
                + (left > 0 ? "  (" + UseRules.span(left) + ")" : "")
                + (ring.size() > 1 ? "   (" + (Math.min(selected, ring.size() - 1) + 1) + "/" + ring.size() + ", shift: next)" : "");
        if (holding != null && holding.act().holdTicks() > 0) {
            int pct = (int) Math.min(100, (now - holdStart) * 100 / holding.act().holdTicks());
            line += "  charging " + pct + "%";
        }
        int w = mc.font.width(line);
        int x = g.guiWidth() - w - 8, y = g.guiHeight() - 14;
        g.fill(x - 3, y - 2, x + w + 3, y + 10, 0x66000000);
        g.drawString(mc.font, line, x, y, left > 0 ? 0xFF9A9AA8 : 0xFF000000 | en.def().grade().rgb(), true);
    }
}
