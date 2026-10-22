package com.newuniverse.nusmp.client.ex;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.relics.ExSingularity;
import com.newuniverse.nusmp.shishou.ShishouEntity;
import com.newuniverse.nusmp.shishou.ShishouItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Why: the EX-Singularity grade (above Genesis, {@link ExSingularity}) shows in two more places besides the tooltip and the sword's own glow: (1) the inventory and hotbar slot of the item gets a
 * prismatic frame that cycles round it (an item decorator, so every place a stack is drawn in a GUI gets it: hotbar, inventory, chests, the cursor); (2) whoever holds the sword, the sword's body in
 * the world and the sword lying on the ground shed a few prismatic motes (the mod's particle theme {@code relic.grade.AKASHIC}, tinted with the glow's current hue). Client only (registered from
 * {@code RelicClient.init}); every number below is bounded:
 * <ul>
 *   <li>the frame: {@link ExSingularity#FRAME_FILLS} fills in ONE managed draw per decorated slot, at most {@link ExSingularity#FRAME_SLOTS_PER_FRAME} slots a frame (a counter reset by the frame
 *       clock); a stack that is not the sword costs nothing (the decorator is registered for that one item only); no allocation (one preallocated {@link Runnable}, static scratch fields);</li>
 *   <li>the motes: one particle per carrier every four ticks ({@link ExSingularity#MOTES_PER_SECOND} a second), carriers looked at only within 48 blocks and at most twelve players a tick; the
 *       particle call itself goes through the mod's client particle budget and level-of-detail rules ({@code Fx}), so a crowded screen thins them first.</li>
 * </ul>
 */
public final class ExSingularityClient {
    private ExSingularityClient() {}

    /** The particle theme of the grade (tools/particles/gen_themes.py: relic.grade.AKASHIC). */
    public static final ThemeKey THEME = ThemeKey.of("relic.grade.AKASHIC");

    private static final double LOOK_RANGE_SQ = 48.0 * 48.0;

    /** Called from {@code RelicClient.init}. */
    public static void init(IEventBus modBus) {
        modBus.addListener(ExSingularityClient::registerDecorations);
        NeoForge.EVENT_BUS.addListener(ExSingularityClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ExSingularityClient::onEntityTick);
    }

    // ---------------------------------------------------------------- the slot frame
    static void registerDecorations(RegisterItemDecorationsEvent e) {
        e.register(ShishouItems.SHISHOU.get(), ExSingularityClient::frame);
    }

    private static long lastFrame = Long.MIN_VALUE;
    private static int usedThisFrame;
    private static GuiGraphics curG;
    private static int curX, curY;
    private static long curMs;
    private static final Runnable PAINT = ExSingularityClient::paint;

    /** The decorator: false = nothing to reset (it only draws gui fills). Never throws into the GUI. */
    static boolean frame(GuiGraphics g, Font font, ItemStack stack, int x, int y) {
        try {
            long frame = Minecraft.getInstance().getFrameTimeNs();
            if (frame != lastFrame) {
                lastFrame = frame;
                usedThisFrame = 0;
            }
            if (++usedThisFrame > ExSingularity.FRAME_SLOTS_PER_FRAME) return false;
            curG = g;
            curX = x;
            curY = y;
            curMs = System.currentTimeMillis();
            g.drawManaged(PAINT);
        } catch (RuntimeException | LinkageError failed) {
            // the slot keeps its plain look
        } finally {
            curG = null;
        }
        return false;
    }

    /** Perimeter units of the outer ring: 18 px top, 16 right, 18 bottom, 16 left. The hue runs once round it, so all of the spectrum is on the frame at once. */
    private static final int PERIMETER = 68;

    /**
     * The frame itself: an outer ring (six 3 px pieces on the top and the bottom, two gradient fills on each side) whose hue runs round the slot and turns with time, an inner ring turning the other
     * way, a faint sheen over the item and one twinkling corner. {@link ExSingularity#FRAME_FILLS} (23) fills at most, one managed draw.
     */
    private static void paint() {
        GuiGraphics g = curG;
        int x = curX, y = curY;
        long ms = curMs;
        RenderType rt = RenderType.guiOverlay();
        int oa = ExSingularity.frameAlpha(false, ms) << 24, ia = ExSingularity.frameAlpha(true, ms) << 24;
        int n = PERIMETER;
        for (int i = 0; i < 6; i++) {
            g.fill(rt, x - 1 + 3 * i, y - 1, x + 2 + 3 * i, y, oa | ExSingularity.frameRgb(3 * i + 1, n, ms, false));                  // top, left to right
            g.fill(rt, x + 14 - 3 * i, y + 16, x + 17 - 3 * i, y + 17, oa | ExSingularity.frameRgb(35 + 3 * i, n, ms, false));         // bottom, right to left
        }
        g.fillGradient(rt, x + 16, y, x + 17, y + 8, oa | ExSingularity.frameRgb(18, n, ms, false), oa | ExSingularity.frameRgb(26, n, ms, false), 0);          // right, top to bottom
        g.fillGradient(rt, x + 16, y + 8, x + 17, y + 16, oa | ExSingularity.frameRgb(26, n, ms, false), oa | ExSingularity.frameRgb(34, n, ms, false), 0);
        g.fillGradient(rt, x - 1, y + 8, x, y + 16, oa | ExSingularity.frameRgb(60, n, ms, false), oa | ExSingularity.frameRgb(52, n, ms, false), 0);         // left, bottom to top
        g.fillGradient(rt, x - 1, y, x, y + 8, oa | ExSingularity.frameRgb(68, n, ms, false), oa | ExSingularity.frameRgb(60, n, ms, false), 0);
        // inner ring: one softer strip per side, the hues turning the other way
        g.fill(rt, x, y, x + 16, y + 1, ia | ExSingularity.frameRgb(0, 4, ms, true));
        g.fill(rt, x + 15, y + 1, x + 16, y + 16, ia | ExSingularity.frameRgb(1, 4, ms, true));
        g.fill(rt, x, y + 15, x + 15, y + 16, ia | ExSingularity.frameRgb(2, 4, ms, true));
        g.fill(rt, x, y + 1, x + 1, y + 15, ia | ExSingularity.frameRgb(3, 4, ms, true));
        // the sheen: a very faint wash of the current hue over the sprite
        g.fill(rt, x, y, x + 16, y + 16, 0x14000000 | ExSingularity.frameRgb(0, 1, ms, false));
        // the twinkle: a plus-shaped glint at the corner whose turn it is (at most one at a time)
        for (int c = 0; c < 4; c++) {
            float tw = ExSingularity.twinkle(c, ms);
            if (tw <= 0.05f) continue;
            int cx = (c == 0 || c == 3) ? x : x + 15, cy = (c < 2) ? y : y + 15;
            int a = Math.round(255f * tw) << 24 | ExSingularity.towardWhite(ExSingularity.frameRgb(c * 17, n, ms, false), 0.7f);
            g.fill(rt, cx - 2, cy, cx + 3, cy + 1, a);
            g.fill(rt, cx, cy - 2, cx + 1, cy + 3, a);
            break;
        }
    }

    // ---------------------------------------------------------------- the motes
    /** Client tick: the people who hold the sword shed a mote every four ticks. */
    static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || level.getGameTime() % 4 != 0) return;
        Item sword = ShishouItems.SHISHOU.get();
        int looked = 0;
        for (AbstractClientPlayer p : level.players()) {
            if (p.isSpectator() || p.isInvisible() || p.distanceToSqr(mc.player) > LOOK_RANGE_SQ || ++looked > 12) continue;
            if (p.getMainHandItem().getItem() != sword && p.getOffhandItem().getItem() != sword) continue;
            mote(level, p.getX(), p.getY() + 0.4, p.getZ(), p.getBbHeight() * 0.8, 0.55, p.tickCount);
        }
    }

    /** Entity tick: Shishou's body in the world and the sword lying on the ground. */
    static void onEntityTick(EntityTickEvent.Post e) {
        Entity en = e.getEntity();
        if (!en.level().isClientSide() || en.tickCount % 4 != 0) return;
        boolean body = en instanceof ShishouEntity;
        if (!body && !(en instanceof ItemEntity ie && ie.getItem().getItem() == ShishouItems.SHISHOU.get())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused() || en.distanceToSqr(mc.player) > LOOK_RANGE_SQ) return;
        mote((ClientLevel) en.level(), en.getX(), en.getY() + (body ? en.getBbHeight() * 0.3 : 0.1), en.getZ(), body ? 0.9 : 0.5, body ? 0.6 : 0.35, en.tickCount);
    }

    /** One tinted mote on a circle of {@code radius} blocks round the point, up to {@code height} above it; hue = the glow's current hue plus a spread by the angle. */
    private static void mote(ClientLevel level, double x, double y, double z, double height, double radius, int seed) {
        float t = level.getGameTime();
        double a = seed * 0.9 + level.random.nextDouble() * 6.2831853;
        double r = radius * (0.7 + 0.3 * level.random.nextDouble());
        int rgb = ExSingularity.moteRgb(t, level.random.nextFloat() * 0.25f);
        FxSlot slot = (seed & 3) == 0 ? FxSlot.SPARK : FxSlot.MOTE;
        Fx.sprayTinted(level, THEME, slot, rgb, 1.0f, x + Math.cos(a) * r, y + level.random.nextDouble() * height, z + Math.sin(a) * r, 1, 0, 0.02, 0, 1.0);
    }
}
