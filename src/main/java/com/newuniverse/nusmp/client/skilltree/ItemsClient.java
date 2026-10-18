package com.newuniverse.nusmp.client.skilltree;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.item.SkillItems;
import com.newuniverse.nusmp.skilltree.item.SpKind;
import com.newuniverse.nusmp.skilltree.item.UnlockTiming;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.slf4j.Logger;

/**
 * Why: the client side of the items, four small jobs that only a client can do. (1) The TINT of a Skill Book: one 16x16 sprite pair serves every route, a white-and-grey
 * cover layer is multiplied by the route's own colour here (an item colour handler on the cover's tint index), so a new magic is a JSON file and needs no new texture;
 * an unknown route gets a neutral grey. (2) The SPRITE of an SP Book: an item property {@code nusmp:sp_kind} that the model's override list reads, so a stack made by hand,
 * with nothing but the kind component, still shows its own primer. (3) One extra tooltip line, "you already know this route", which needs the local player's synced tree
 * and so cannot be written by the item class (that is common code and has no player). (4) The "ROUTE UNLOCKED" BANNER with its screen flash: a GUI layer registered with
 * {@code RegisterGuiLayersEvent} (NeoForge 1.21's replacement of the old overlay interface, as the System window does) that draws, over the world and above everything else,
 * a flash in the route's colour, the words in large letters slamming in and settling, a line that draws itself, and the route's name below, for three seconds. The timing is
 * the pure {@link BannerTimeline}, counted from {@link UnlockTiming#BURST_TICK} ticks after the packet, because the server's circle bursts then; the packet itself is
 * only the route id ({@code RouteUnlockedPayload}), so the name and colour come from the client's own synced registry. The banner is skipped while a screen is open or the
 * GUI is hidden (a layer is not hidden by the game in those cases), and a draw that throws switches the banner off with one log line instead of one per frame. This class is
 * client only: it is started from {@code SkillTreeClient.init}, and the packet handler reaches it by name inside a lambda, so a dedicated server never loads it.
 */
public final class ItemsClient {
    private ItemsClient() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The tint colour of a Skill Book whose route this client does not know. */
    private static final int UNKNOWN_COVER = 0x8A8A9A;

    /** The banner being shown: the route's name and colour, and the game time of the packet; {@code startedAt} is -1 when there is none. */
    private static String bannerName = "";
    private static int bannerColor = 0xFFFFFF;
    private static long startedAt = -1L;
    private static boolean broken;

    public static void init(IEventBus modBus) {
        BookClient.init(modBus);                                                  // 1.82.0: the 3D book's table, franchise property and cover colour
        modBus.addListener(ItemsClient::registerColors);
        modBus.addListener(ItemsClient::registerLayers);
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(ItemsClient::registerProperties));
        NeoForge.EVENT_BUS.addListener(ItemsClient::onTooltip);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> startedAt = -1L);
    }

    // ------------------------------------------------------------------ tint, sprite, tooltip
    /** Tint index 1 (the cover layer) takes the route's colour; the book layer is left white. */
    private static void registerColors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> tint == 1 ? 0xFF000000 | coverColor(stack) : 0xFFFFFFFF, SkillItems.SKILL_BOOK.get());
    }

    private static int coverColor(ItemStack stack) {
        return BookClient.coverColor(stack);                                       // 1.82.0: route colour, else the shipped table's, else grey
    }

    /** The property the SP Book's model overrides read: the kind's model value (0.1 to 0.5), 0 for a stack of no known kind. */
    private static void registerProperties() {
        ItemProperties.register(SkillItems.SP_BOOK.get(), ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "sp_kind"), (stack, level, entity, seed) -> {
            SpKind kind = SpKind.byId(SkillItems.spKindOf(stack));
            return kind == null ? 0f : kind.modelValue();
        });
    }

    /** Adds "You already know this route." under a Skill Book whose route the local player has. */
    private static void onTooltip(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();
        if (!(stack.getItem() instanceof SkillBookItem) || e.getEntity() == null) return;
        SkillRoute route = RouteRegistry.byBook(SkillItems.bookIdOf(stack));
        if (route != null && SkillTreeApi.hasRoute(e.getEntity(), route.id())) {
            e.getToolTip().add(Component.translatableWithFallback("tooltip.nusmp.skill_book.known", "You already know this route.").withStyle(ChatFormatting.GREEN));
        }
    }

    // ------------------------------------------------------------------ the banner
    /** Called on the client thread when the server says a route was just unlocked. */
    public static void onRouteUnlocked(String routeId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SkillRoute route = RouteRegistry.byId(routeId);
        bannerName = route != null ? route.name() : BannerTimeline.fallbackName(routeId);
        bannerColor = route != null ? route.color() & 0xFFFFFF : 0xFFFFFF;
        startedAt = mc.level.getGameTime();
        broken = false;
    }

    private static void registerLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "route_unlocked"), ItemsClient::renderBanner);
    }

    /** The layer: called every frame by the GUI. */
    private static void renderBanner(GuiGraphics g, DeltaTracker delta) {
        if (startedAt < 0L || broken) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            startedAt = -1L;
            return;
        }
        float age = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false) - startedAt - UnlockTiming.BURST_TICK;
        if (BannerTimeline.done(age)) {
            startedAt = -1L;
            return;
        }
        if (age < 0f || mc.options.hideGui || mc.screen != null) return;
        try {
            draw(g, mc.font, age);
        } catch (RuntimeException | LinkageError ex) {
            broken = true;
            LOG.warn("[nusmp] the route-unlocked banner failed to draw and is switched off until the next unlock", ex);
        }
    }

    private static void draw(GuiGraphics g, Font font, float age) {
        int w = g.guiWidth(), h = g.guiHeight();
        float alpha = BannerTimeline.alpha(age), flash = BannerTimeline.flash(age);
        int rgb = bannerColor;
        if (flash > 0f) g.fill(0, 0, w, h, BannerTimeline.argb(BannerTimeline.lighten(rgb, 0.5f), 0.6f * flash));
        if (alpha <= 0.02f) return;
        int cx = w / 2, cy = (int) (h * 0.27f), half = 42;
        int dark = BannerTimeline.argb(0x000000, 0.55f * alpha), clear = BannerTimeline.argb(0x000000, 0f);
        int mid = cy + 4;                                            // the words (title above the line, name below it) sit a little below cy, so the band is centred on them
        g.fillGradient(0, mid - half, w, mid, clear, dark);
        g.fillGradient(0, mid, w, mid + half, dark, clear);
        Component title = Component.translatableWithFallback("title.nusmp.tree.items.route_unlocked", "ROUTE UNLOCKED");
        Component name = Component.literal(bannerName);
        float base = Math.min(3.0f, w * 0.6f / Math.max(1, font.width(title)));
        drawScaled(g, font, title, cx, cy - 12, base * BannerTimeline.scale(age), BannerTimeline.argb(BannerTimeline.lighten(rgb, 0.35f), alpha));
        int lineWidth = (int) (font.width(title) * base * 1.1f * BannerTimeline.rule(age));
        g.fill(cx - lineWidth / 2, cy + 6, cx + lineWidth / 2, cy + 8, BannerTimeline.argb(BannerTimeline.lighten(rgb, 0.15f), alpha));
        float nameScale = Math.min(1.9f, w * 0.7f / Math.max(1, font.width(name)));
        drawScaled(g, font, name, cx, cy + 24, nameScale, BannerTimeline.argb(0xFFFFFF, alpha));
    }

    /** Draws a text centred on (x, y) at a size factor. Text with almost no opacity is skipped: the game would draw an alpha of 0 fully opaque. */
    private static void drawScaled(GuiGraphics g, Font font, Component text, int x, int y, float scale, int argb) {
        if ((argb >>> 24) < 4) return;
        g.pose().pushPose();
        g.pose().translate((float) x, (float) y, 0f);
        g.pose().scale(scale, scale, 1f);
        g.drawCenteredString(font, text, 0, -font.lineHeight / 2, argb);
        g.pose().popPose();
    }
}
