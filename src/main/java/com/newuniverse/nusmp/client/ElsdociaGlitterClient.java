package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.item.MagicWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * 1.29.0: faint crystalline glitter round Elsdocia while a player holds it (any player in view: the effect is the weapon's, not the owner's).
 * Client side only, no packets. The blade is taken as a line from the holder's main hand along their look direction (tip up and forward,
 * the way the in-hand model sits); particles are a pale blue dust and the occasional end-rod spark, capped per client per tick by
 * {@link ElsdociaGlitter}. The pulse of the blade itself is the mesh's glow line (WeaponRenderer), so nothing here touches rendering.
 * Registered by {@link #init()} from the client section of the mod constructor.
 */
public final class ElsdociaGlitterClient {
    private ElsdociaGlitterClient() {}

    private static final DustParticleOptions ICE = new DustParticleOptions(new Vector3f(0.45f, 0.82f, 1.0f), 0.7f);
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(0.9f, 0.97f, 1.0f), 0.55f);

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ElsdociaGlitterClient::onTick);
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        int spent = 0;
        for (Player p : mc.level.players()) {
            if (!ElsdociaGlitter.budgetLeft(spent)) return;
            if (!(p.getMainHandItem().getItem() instanceof MagicWeaponItem w) || w.kind != MagicWeaponItem.Kind.ELSDOCIA) continue;
            var r = p.getRandom();
            int n = ElsdociaGlitter.count(p.distanceToSqr(mc.player), r.nextFloat());
            if (n == 0) continue;
            double yaw = Math.toRadians(p.yBodyRot);
            Vec3 look = p.getLookAngle();
            // the hand: a little in front of the body at about hip height, on the right
            Vec3 hand = new Vec3(p.getX() - Math.cos(yaw) * 0.36 - Math.sin(yaw) * -0.15, p.getY() + 0.95, p.getZ() - Math.sin(yaw) * 0.36 + Math.cos(yaw) * 0.15);
            Vec3 dir = new Vec3(look.x * 0.45, 0.9, look.z * 0.45).normalize();                // tip up and forward
            for (int k = 0; k < n && ElsdociaGlitter.budgetLeft(spent); k++, spent++) {
                float along = ElsdociaGlitter.along(r.nextFloat()) - 0.3f;                      // metres from the hand
                double ox = (r.nextDouble() - 0.5) * 0.16, oz = (r.nextDouble() - 0.5) * 0.16, oy = (r.nextDouble() - 0.5) * 0.1;
                Vec3 at = hand.add(dir.scale(along)).add(ox, oy, oz);
                boolean spark = r.nextInt(5) == 0;
                mc.level.addParticle(spark ? ParticleTypes.END_ROD : (r.nextBoolean() ? ICE : WHITE), at.x, at.y, at.z, 0, spark ? 0.01 : -0.005, 0);
            }
        }
    }
}
