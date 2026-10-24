package com.newuniverse.nusmp.client.manazone;

import com.newuniverse.nusmp.mana.zone.ZoneCastNet;
import io.github.manasmods.tensura.client.TensuraKeybinds;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * The client half of the Mana Zone's omnidirectional cast (docs/magic/mana_zone_upgrade.md). No key of its own: while the local player's OWN zone is open and one of the
 * skill slot keys is held, it casts a ray from the camera through the crosshair, as far as the zone reaches (stopped by the first block, or a living thing on the way), and tells the
 * server where it points: the aimed point and the foe's id, in one 28-byte packet. The server decides everything (it checks the sender, the zone, the numbers and the page, and
 * then casts the page from the zone at that point); this class only looks and asks, so a client that sends nothing, or lies, gets the ordinary cast from the hand.
 *
 * <p>Cost: the tick handler returns at once unless the local player has a zone open (one list walk of a few entries); while a skill key is held it casts one ray and one small entity
 * query every {@value #CHECK} ticks and sends a packet only when the aim moved {@code 0.75} blocks or {@value #RESEND} ticks passed, and one more when the key is let go: at most
 * 6 packets per second, the server accepts 10. No allocation per tick on the idle path.</p>
 */
public final class ZoneCastClient {
    private ZoneCastClient() {}

    private static final int CHECK = 3, RESEND = 6;
    private static final double MOVED2 = 0.75 * 0.75;

    private static KeyMapping[] keys;
    private static boolean keysTried, held;
    private static long lastCheck, lastSent = Long.MIN_VALUE / 4;
    private static double lx, ly, lz;
    private static final double[] AIM = new double[4];

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ZoneCastClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> held = false);
    }

    private static boolean keyDown() {
        if (!keysTried) {
            keysTried = true;
            try {
                keys = new KeyMapping[]{TensuraKeybinds.ACTIVATE_SLOT_1, TensuraKeybinds.ACTIVATE_SLOT_2, TensuraKeybinds.ACTIVATE_SLOT_3};
            } catch (Throwable absent) {
                keys = null;                                                           // no Tensura keys: the cast simply stays at the hand
            }
        }
        if (keys == null) return false;
        for (KeyMapping k : keys) if (k != null && k.isDown()) return true;
        return false;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) { held = false; return; }
        long now = mc.level.getGameTime();
        double radius = ManaZoneClient.ownRadius(p.getId(), now);
        if (radius < 3.0) { held = false; return; }                                 // the idle path: no own zone
        boolean down = keyDown();
        if (down) {
            if (held && now - lastCheck < CHECK) return;
            lastCheck = now;
            if (aim(mc, p, radius)) {
                double dx = AIM[0] - lx, dy = AIM[1] - ly, dz = AIM[2] - lz;
                if (!held || now - lastSent >= RESEND || dx * dx + dy * dy + dz * dz >= MOVED2) send(now);
            }
        } else if (held && now - lastSent >= 2 && aim(mc, p, radius)) {
            send(now);                                                              // the key was let go: the spell fires about now, from where the crosshair is
        }
        held = down;
    }

    private static void send(long now) {
        lastSent = now;
        lx = AIM[0];
        ly = AIM[1];
        lz = AIM[2];
        PacketDistributor.sendToServer(new ZoneCastNet.AimPayload(AIM[0], AIM[1], AIM[2], (int) AIM[3]));
    }

    /** Camera ray to the crosshair, as far as the zone reaches from the camera. Writes x, y, z and the foe's id (-1 none) into AIM. */
    private static boolean aim(Minecraft mc, LocalPlayer p, double radius) {
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 start = cam.getPosition();
        Vector3f lv = cam.getLookVector();
        double fromCentre = start.distanceTo(new Vec3(p.getX(), p.getY() + 0.9, p.getZ()));
        double reach = Math.max(1.0, radius - fromCentre - 0.5);                    // the camera of a third-person view sits outside the centre
        Vec3 end = start.add(lv.x() * reach, lv.y() * reach, lv.z() * reach);
        HitResult block = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult foe = ProjectileUtil.getEntityHitResult(mc.level, p, start, stop, new AABB(start, stop).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != p);
        if (foe != null) {
            Vec3 c = foe.getEntity().getBoundingBox().getCenter();
            AIM[0] = c.x;
            AIM[1] = c.y;
            AIM[2] = c.z;
            AIM[3] = foe.getEntity().getId();
        } else {
            AIM[0] = stop.x;
            AIM[1] = stop.y;
            AIM[2] = stop.z;
            AIM[3] = -1;
        }
        return true;
    }
}
