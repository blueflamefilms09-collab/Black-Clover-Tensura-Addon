package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.item.ElsdociaComponents;
import com.newuniverse.nusmp.item.ElsdociaRules;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
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
            moteRing(mc, p, r.nextFloat());                                                       // 1.97.0: a small mote per learned ability
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
                mc.level.addParticle(spark ? Fx.options(FxThemes.BOSS_ELSDOCIA, FxOld.END_ROD) : (r.nextBoolean() ? ICE : WHITE), at.x, at.y, at.z, 0, spark ? 0.01 : -0.005, 0);
            }
        }
    }

    /**
     * 1.97.0: one mote per learned magic (at most {@link ElsdociaRules#MAX_MOTES}) circling the blade, tinted by that magic's soul; each mote spawns on a
     * roll, so a holder spends at most one particle per tick on motes. Reads the synced lineage component, no packets.
     */
    private static void moteRing(Minecraft mc, Player p, float roll) {
        var lineage = ElsdociaComponents.get(p.getMainHandItem());
        int learned = Math.min(lineage.learned().size(), ElsdociaRules.MAX_MOTES);
        if (learned == 0 || roll > 0.35f || p.distanceToSqr(mc.player) > 24 * 24) return;
        int k = (int) (p.getRandom().nextFloat() * learned) % learned;
        var m = MagicType.lookup(lineage.learned().get(k));
        int rgb = m == null ? 0xFFFFFF : ElsdociaGlitter.moteColour(m.soulType);
        double yaw = Math.toRadians(p.yBodyRot), ang = (mc.level.getGameTime() * 0.12) + k * (Math.PI * 2 / learned);
        double r = 0.32 + 0.05 * Math.sin(ang * 2);
        Vec3 look = p.getLookAngle();
        Vec3 hand = new Vec3(p.getX() - Math.cos(yaw) * 0.36 + look.x * 0.2, p.getY() + 1.25 + 0.35 * Math.sin(ang * 0.7), p.getZ() - Math.sin(yaw) * 0.36 + look.z * 0.2);
        Vec3 at = hand.add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
        Fx.addDust(mc.level, FxThemes.BOSS_ELSDOCIA, new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 0.8f, at.x, at.y, at.z, 0, 0.002, 0);
    }
}
