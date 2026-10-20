package com.newuniverse.nusmp.relics.upgrade;

import com.newuniverse.nusmp.perf.PerfParticles;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * The visuals of the acts of the upgraded relics (1.102.0): each act of an upgraded God relic has an effect of its own (an effect shape of the VFX system plus a burst of particles, in the
 * relic's colours), listed in its table {@code assets/nusmp/visual_upgrade/<id>.json}. {@code RelicEngine} calls {@link #after} once an act has run. Server side, purely visual: it
 * changes no damage, effect or cost, goes out through the effect packet budget of {@link VfxSpawn} and the particle budget of {@link PerfParticles}, and never throws into the act.
 */
public final class UpgradeActFx {
    private UpgradeActFx() {}

    /** Plays the visuals of {@code act} of {@code def} for the player; nothing for a relic or an act without an entry. */
    public static void after(ServerPlayer p, RelicDef def, Act act) {
        try {
            UpgradeFxData d = UpgradeFxData.forId(def.id());
            if (d == null) return;
            List<UpgradeFxData.ActFx> list = d.actFx(act.key());
            if (list.isEmpty()) return;
            ServerLevel level = p.serverLevel();
            Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
            for (UpgradeFxData.ActFx fx : list) {
                Vec3 from = switch (fx.at()) {
                    case "eye" -> eye;
                    case "aim" -> eye.add(look.scale(Math.max(2.0, fx.radius())));
                    default -> p.position().add(0.0, 0.05, 0.0);
                };
                Vec3 to = eye.add(look.scale(Math.max(3.0, fx.radius() * 2.0)));
                if (!fx.shape().isEmpty()) {
                    try {
                        VfxSpawn.send(level, VfxShape.valueOf(fx.shape()), from, to, 0xFF000000 | fx.color(), Math.max(4, fx.duration()), fx.power());
                    } catch (IllegalArgumentException unknownShape) {
                        // a shape name the VFX system does not have: the burst below still plays
                    }
                }
                if (fx.count() > 0) {
                    ParticleOptions o = options(fx.particle(), fx.color());
                    if (o != null) {
                        double s = Math.max(0.05, fx.spread());
                        PerfParticles.send(level, o, from.x, from.y + fx.height() * 0.5, from.z, Math.min(120, fx.count()), s, fx.height() * 0.5, s, 0.02);
                    }
                }
            }
        } catch (RuntimeException | LinkageError failed) {
            org.slf4j.LoggerFactory.getLogger("nusmp").error("[nusmp] the act visuals of {} failed", def.id(), failed);
        }
    }

    /** The particle options a table entry names: "dust" or "" (coloured by {@code color}) or a simple particle type id; null when unknown. */
    public static ParticleOptions options(String particle, int color) {
        if (particle == null || particle.isEmpty() || particle.equals("dust")) {
            return new DustParticleOptions(new Vector3f((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f), 1.0f);
        }
        ResourceLocation id = ResourceLocation.tryParse(particle);
        if (id == null) return null;
        ParticleType<?> t = BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null);
        return t instanceof ParticleOptions po ? po : null;
    }
}
