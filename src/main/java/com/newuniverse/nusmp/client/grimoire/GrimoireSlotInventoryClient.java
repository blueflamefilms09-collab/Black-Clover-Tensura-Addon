package com.newuniverse.nusmp.client.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Grimoire Slot as a slot of the player's own inventory screen (docs/grimoire_slot_rework.md, setting {@code grimoireSlotInventory}): a gold-framed
 * box above the offhand slot, showing the book (or a dimmed book icon when empty). No mixin: the box is drawn from {@code ContainerScreenEvent.Render.Background},
 * its tooltip from {@code ScreenEvent.Render.Post}, and a click is cancelled and sent to the server as a {@link GrimoireSlot.SlotClickPayload}; the server
 * decides (it swaps the cursor with the slot) and syncs the cursor and the hip book back. The stack shown is the client copy of the hip book.
 *
 * <p>Cost per frame: one 20x20 box, one item draw, one tooltip while hovered; nothing is built per frame. The stack is read once per client tick.
 */
public final class GrimoireSlotInventoryClient {
    /** Slot position (the item's top-left) relative to the inventory screen's top-left: above the offhand slot (77, 62), right of the player model. */
    static final int SLOT_X = 77, SLOT_Y = 40;
    private static final int GOLD = 0xFFE8C468, EDGE = 0xFF1B1B22, WELL = 0xFF2B2B33;

    /** True once the server said it runs the inventory mode (sent at login); false everywhere else, so nothing is drawn on a server without it. */
    private static boolean mode;
    private static ItemStack self = ItemStack.EMPTY;
    private static int readTick = Integer.MIN_VALUE;
    private static ItemStack bookIcon;

    private GrimoireSlotInventoryClient() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(GrimoireSlotInventoryClient::onBackground);
        NeoForge.EVENT_BUS.addListener(GrimoireSlotInventoryClient::onRenderPost);
        NeoForge.EVENT_BUS.addListener(GrimoireSlotInventoryClient::onPress);
        NeoForge.EVENT_BUS.addListener(GrimoireSlotInventoryClient::onRelease);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { mode = false; self = ItemStack.EMPTY; readTick = Integer.MIN_VALUE; });
    }

    public static boolean active() { return mode; }

    /** The server's mode (at login, or when the key / button / command asks for the slot: {@code open} then shows the inventory screen). */
    public static void receiveMode(boolean inventory, boolean open) {
        mode = inventory;
        Minecraft mc = Minecraft.getInstance();
        if (open && inventory && mc.player != null && !(mc.screen instanceof InventoryScreen)) mc.setScreen(new InventoryScreen(mc.player));
    }

    // ---------------------------------------------------------------- geometry
    private static boolean applies(Screen s) { return mode && s instanceof InventoryScreen; }

    private static boolean inside(AbstractContainerScreen<?> s, double mx, double my) {
        int x = s.getGuiLeft() + SLOT_X, y = s.getGuiTop() + SLOT_Y;
        return mx >= x - 1 && mx < x + 17 && my >= y - 1 && my < y + 17;
    }

    /** The local copy of the hip book, read from the map once per client tick (no boxing per frame). */
    private static ItemStack stack(Minecraft mc) {
        if (mc.player == null) return ItemStack.EMPTY;
        if (mc.player.tickCount != readTick) {
            readTick = mc.player.tickCount;
            self = GrimoireFloatClient.hip(mc.player.getId());
        }
        return self;
    }

    // ---------------------------------------------------------------- draw
    private static void onBackground(ContainerScreenEvent.Render.Background e) {
        if (!applies(e.getContainerScreen())) return;
        AbstractContainerScreen<?> s = e.getContainerScreen();
        GuiGraphics g = e.getGuiGraphics();
        int x = s.getGuiLeft() + SLOT_X, y = s.getGuiTop() + SLOT_Y;
        g.fill(x - 2, y - 2, x + 18, y + 18, GOLD);                       // the small gold frame
        g.fill(x - 1, y - 1, x + 17, y + 17, EDGE);
        g.fill(x, y, x + 16, y + 16, WELL);
        ItemStack st = stack(Minecraft.getInstance());
        if (st.isEmpty()) {
            if (bookIcon == null) bookIcon = new ItemStack(Items.BOOK);
            g.renderFakeItem(bookIcon, x, y);
            g.fill(x, y, x + 16, y + 16, 0xB02B2B33);                      // dimmed: nothing bound
        } else {
            g.renderItem(st, x, y);
        }
        if (inside(s, e.getMouseX(), e.getMouseY())) g.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
    }

    private static void onRenderPost(ScreenEvent.Render.Post e) {
        if (!applies(e.getScreen()) || !inside((AbstractContainerScreen<?>) e.getScreen(), e.getMouseX(), e.getMouseY())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !((InventoryScreen) e.getScreen()).getMenu().getCarried().isEmpty()) return;      // like a vanilla slot: no tooltip with something on the cursor
        ItemStack st = stack(mc);
        List<Component> lines;
        if (st.isEmpty()) {
            lines = new ArrayList<>(2);
            lines.add(Component.translatable("anime_multiverse.ui.slot.inv.title").withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("anime_multiverse.ui.slot.inv.empty").withStyle(ChatFormatting.GRAY));
        } else {
            lines = new ArrayList<>(Screen.getTooltipFromItem(mc, st));
            lines.add(Component.translatable("anime_multiverse.ui.slot.inv.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        e.getGuiGraphics().renderTooltip(mc.font, lines, java.util.Optional.empty(), e.getMouseX(), e.getMouseY());
    }

    // ---------------------------------------------------------------- click
    private static void onPress(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!applies(e.getScreen()) || !inside((AbstractContainerScreen<?>) e.getScreen(), e.getMouseX(), e.getMouseY())) return;
        e.setCanceled(true);                                                // never a vanilla click on this spot
        int b = e.getButton();
        if (b == 0 || b == 1) PacketDistributor.sendToServer(new GrimoireSlot.SlotClickPayload(b, Screen.hasShiftDown()));
    }

    private static void onRelease(ScreenEvent.MouseButtonReleased.Pre e) {
        if (applies(e.getScreen()) && inside((AbstractContainerScreen<?>) e.getScreen(), e.getMouseX(), e.getMouseY())) e.setCanceled(true);
    }
}
