package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.item.ElsdociaComponents;
import com.newuniverse.nusmp.item.ElsdociaRules;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import com.newuniverse.nusmp.client.upgrade.UpgradeGate;
import com.newuniverse.nusmp.particle.fx.UpgradeParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * 1.29.0: faint crystalline glitter round Elsdocia while a player holds it (any player in view: the effect is the weapon's, not the owner's).
 * Client side only, no packets. The blade is taken as a line from the holder's main hand along their look direction (tip up and forward,
 * the way the in-hand model sits); particles are a pale blue dust and the occasional end-rod spark, capped per client per tick by
 * {@link ElsdociaGlitter}. The pulse of the blade itself is the mesh's glow line (WeaponRenderer), so nothing here touches rendering.
 * Registered by {@link #init()} from the client section of the mod constructor.
 */
public final class ElsdociaGlitterClient {
    private ElsdociaGlitterClient() {}

    private static ParticleOptions ice, white, spark;                                    // 1.110.0: the mod's own particles, built on first use (the registry is bound by then)

    private static ParticleOptions ice() {
        if (ice == null) ice = UpgradeParticleOptions.dust(0x73D1FF, 0.7f);
        return ice;
    }

    private static ParticleOptions white() {
        if (white == null) white = UpgradeParticleOptions.dust(0xE6F7FF, 0.55f);
        return white;
    }

    private static ParticleOptions spark() {
        if (spark == null) spark = UpgradeParticleOptions.of("nusmp:mote", 0xCFEFFF, 0.6f);
        return spark;
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ElsdociaGlitterClient::onTick);
    }

    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if (!UpgradeGate.begin(mc, mc.level.getGameTime())) return;                          // 1.110.0: paused, low frame rate, particles.upgradeDensity 0: nothing
        int spent = 0;
        for (Player p : mc.level.players()) {
            if (!ElsdociaGlitter.budgetLeft(spent)) return;
            if (!(p.getMainHandItem().getItem() instanceof MagicWeaponItem w) || w.kind != MagicWeaponItem.Kind.ELSDOCIA) continue;
            if (!(p instanceof AbstractClientPlayer cp)) continue;
            float gate = UpgradeGate.scale(cp);                                               // density x quality x fps x distance (others: 16 blocks) x view direction
            if (gate <= 0f) continue;
            var r = p.getRandom();
            moteRing(mc, cp, r.nextFloat(), gate);                                            // 1.97.0: a small mote per learned ability
            int n = ElsdociaGlitter.count(p.distanceToSqr(mc.player), r.nextFloat());
            n = UpgradeGate.take(cp, 0x45, UpgradeGate.count(n * gate, r));                    // the item and player buckets, the live cap, the client allowance
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
                UpgradeGate.add(mc.level, spark ? spark() : (r.nextBoolean() ? ice() : white()), at.x, at.y, at.z, 0, spark ? 0.01 : -0.005, 0);
            }
        }
    }

    /**
     * 1.97.0: one mote per learned magic (at most {@link ElsdociaRules#MAX_MOTES}) circling the blade, tinted by that magic's soul; each mote spawns on a
     * roll, so a holder spends at most one particle per tick on motes. Reads the synced lineage component, no packets.
     */
    private static void moteRing(Minecraft mc, AbstractClientPlayer p, float roll, float gate) {
        var lineage = ElsdociaComponents.get(p.getMainHandItem());
        int learned = Math.min(lineage.learned().size(), ElsdociaRules.MAX_MOTES);
        if (learned == 0 || roll > 0.35f * gate || p.distanceToSqr(mc.player) > 24 * 24 || UpgradeGate.take(p, 0x46, 1) <= 0) return;
        int k = (int) (p.getRandom().nextFloat() * learned) % learned;
        var m = MagicType.lookup(lineage.learned().get(k));
        int rgb = m == null ? 0xFFFFFF : ElsdociaGlitter.moteColour(m.soulType);
        double yaw = Math.toRadians(p.yBodyRot), ang = (mc.level.getGameTime() * 0.12) + k * (Math.PI * 2 / learned);
        double r = 0.32 + 0.05 * Math.sin(ang * 2);
        Vec3 look = p.getLookAngle();
        Vec3 hand = new Vec3(p.getX() - Math.cos(yaw) * 0.36 + look.x * 0.2, p.getY() + 1.25 + 0.35 * Math.sin(ang * 0.7), p.getZ() - Math.sin(yaw) * 0.36 + look.z * 0.2);
        Vec3 at = hand.add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
        UpgradeGate.add(mc.level, UpgradeParticleOptions.dust(rgb, 0.8f), at.x, at.y, at.z, 0, 0.002, 0);
    }
}
