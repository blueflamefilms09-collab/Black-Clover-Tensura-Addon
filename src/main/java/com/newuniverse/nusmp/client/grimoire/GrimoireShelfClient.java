package com.newuniverse.nusmp.client.grimoire;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.BlackCloverRegistry;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireShelfLayout;
import com.newuniverse.nusmp.grimoire.GrimoireShowcase;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Floating grimoire shelf (client only). Opening it lays the grimoires from your inventory (or, if you carry none, the creative-tab
 * showcase looks) on an arc in front of you. Each frame {@link #onRender} asks {@link GrimoireShelfLayout#open} for every book's pose
 * at {@code age = (game time - open time) + partial tick} and draws the book item there: no entity, no per-tick movement, nothing
 * sent over the network. Books turn to face the camera; the one you look at tilts back 8 degrees and grows 22%.
 *
 * <p>Open / close: {@code /grimoireshelf} (toggle), {@code /grimoireshelf close}, or the "Grimoire Shelf" key (unbound by default).
 * It closes on its own when you walk {@link #MAX_DISTANCE} blocks away, change dimension or log out.
 */
public final class GrimoireShelfClient {
    /** Base size of a shelf book (the model is drawn untransformed, one block across). */
    private static final float BOOK_SCALE = 0.6f;
    private static final float HOVER_SCALE = 1.22f;
    private static final float HOVER_TILT_DEG = -8f;
    /** How close to the look ray a book's centre must be to count as looked at, in blocks. */
    private static final double PICK_RADIUS = 0.3;
    private static final double MAX_DISTANCE = 6.0;

    public static final KeyMapping KEY = new KeyMapping("key.nusmp.grimoire_shelf", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.nusmp");

    private static List<ItemStack> books = List.of();
    private static long openTime;
    private static double anchorX, anchorY, anchorZ;
    private static float anchorYaw;
    private static ResourceKey<Level> dimension;
    /** Book centres for the current frame (x, y, z per book), reused to avoid allocating every frame. */
    private static double[] centers = new double[0];

    private GrimoireShelfClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        NeoForge.EVENT_BUS.addListener(GrimoireShelfClient::onRender);
        NeoForge.EVENT_BUS.addListener(GrimoireShelfClient::onTick);
        NeoForge.EVENT_BUS.addListener(GrimoireShelfClient::registerCommands);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> close());
    }

    public static boolean isOpen() { return !books.isEmpty(); }

    /** Opens the shelf in front of the local player. Returns how many books it shows. */
    public static int open() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;
        List<ItemStack> found = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(BlackCloverRegistry.GRIMOIRE.get())) found.add(s.copyWithCount(1));
        }
        if (found.isEmpty()) {
            for (GrimoireShowcase.Entry e : GrimoireShowcase.entries()) found.add(GrimoireItem.createWithLook(e.look(), e.magic(), null));
        }
        books = List.copyOf(found);
        centers = new double[books.size() * 3];
        openTime = player.level().getGameTime();
        Vec3 eye = player.getEyePosition();
        anchorX = eye.x; anchorY = eye.y; anchorZ = eye.z;
        anchorYaw = player.getYRot();
        dimension = player.level().dimension();
        return books.size();
    }

    public static void close() {
        books = List.of();
        centers = new double[0];
    }

    private static void onTick(ClientTickEvent.Post event) {
        while (KEY.consumeClick()) {
            if (isOpen()) close(); else open();
        }
        if (!isOpen()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.level().dimension() != dimension
                || player.getEyePosition().distanceToSqr(anchorX, anchorY, anchorZ) > MAX_DISTANCE * MAX_DISTANCE) close();
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("grimoireshelf")
                .executes(ctx -> {
                    if (isOpen()) {
                        close();
                        ctx.getSource().sendSuccess(() -> Component.translatable("nusmp.grimoire_shelf.closed"), false);
                    } else {
                        int n = open();
                        ctx.getSource().sendSuccess(() -> Component.translatable("nusmp.grimoire_shelf.opened", n), false);
                    }
                    return 1;
                })
                .then(Commands.literal("close").executes(ctx -> {
                    close();
                    ctx.getSource().sendSuccess(() -> Component.translatable("nusmp.grimoire_shelf.closed"), false);
                    return 1;
                })));
    }

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !isOpen()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        List<ItemStack> shelf = books;
        int n = shelf.size();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float age = (mc.level.getGameTime() - openTime) + partial;
        Vec3 cam = event.getCamera().getPosition();

        // Where every book is this frame, then which one the camera looks at.
        for (int i = 0; i < n; i++) {
            GrimoireShelfLayout.Pose p = GrimoireShelfLayout.open(i, n, age);
            centers[i * 3] = anchorX + GrimoireShelfLayout.worldDX(p.right(), p.forward(), anchorYaw);
            centers[i * 3 + 1] = anchorY + p.up();
            centers[i * 3 + 2] = anchorZ + GrimoireShelfLayout.worldDZ(p.right(), p.forward(), anchorYaw);
        }
        Vector3f look = event.getCamera().getLookVector();
        int hovered = GrimoireShelfLayout.pick(cam.x, cam.y, cam.z, look.x(), look.y(), look.z(), centers, PICK_RADIUS);

        // The pose stack here is camera-relative without the camera rotation; pin the model-view to this frame's view matrix.
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        PoseStack pose = new PoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (int i = 0; i < n; i++) {
            double x = centers[i * 3], y = centers[i * 3 + 1], z = centers[i * 3 + 2];
            pose.pushPose();
            pose.translate(x - cam.x, y - cam.y, z - cam.z);
            pose.mulPose(Axis.YP.rotation(GrimoireShelfLayout.faceYaw(cam.x - x, cam.z - z)));
            float scale = BOOK_SCALE;
            if (i == hovered) {
                pose.mulPose(Axis.XP.rotationDegrees(HOVER_TILT_DEG));
                scale *= HOVER_SCALE;
            }
            pose.scale(scale, scale, scale);
            mc.getItemRenderer().renderStatic(shelf.get(i), ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    pose, buffers, mc.level, i);
            pose.popPose();
        }
        buffers.endBatch();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }
}
