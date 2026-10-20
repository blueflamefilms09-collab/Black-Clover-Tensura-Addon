package com.newuniverse.nusmp.relics.vu;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Why: the signature effect of every act of the upgraded relics (see {@link VuActTable}). Called once from {@code RelicEngine.activate} after an act has run (so a refused or failed
 * act shows nothing); it sends at most {@link VuActTable#MAX_STEPS} effects through {@link VfxSpawn}, which counts them against the per-tick effect packet budget of the performance
 * pass, so a crowd of players casting cannot flood anyone. It changes nothing in the world and never throws. Server side; the table is read from the jar once.
 */
public final class VuActVfx {
    private VuActVfx() {}

    private static VuActTable table;

    private static VuActTable table() {
        if (table == null) {
            VuActTable t = VuActTable.parse(java.util.List.of());
            try (InputStream in = VuActVfx.class.getResourceAsStream("/data/nusmp/visual_upgrade/act_vfx.txt")) {
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    t = VuActTable.parse(r.lines().toList());
                    for (String p : t.problems()) LogUtils.getLogger().error("[nusmp] visual_upgrade/act_vfx.txt: {}", p);
                }
            } catch (Exception ex) {
                LogUtils.getLogger().error("[nusmp] visual_upgrade/act_vfx.txt could not be read", ex);
            }
            table = t;
        }
        return table;
    }

    /** Sends the effect of this act of this relic, if the table has one. */
    public static void fire(ServerPlayer p, RelicDef def, Act act) {
        try {
            if (p == null || def == null || act == null) return;
            var steps = table().steps(def.id(), act.key());
            if (steps.isEmpty()) return;
            ServerLevel level = p.serverLevel();
            Vec3 feet = p.position(), eye = p.getEyePosition(), look = p.getLookAngle();
            for (VuActTable.Step s : steps) {
                VfxShape shape = VfxShape.valueOf(s.shape());
                Vec3 from, to;
                switch (s.where()) {
                    case "front" -> {
                        from = eye.add(look.scale(1.2));
                        to = eye.add(look.scale(10.0));
                    }
                    case "ground" -> {
                        from = feet.add(0, 0.08, 0);
                        to = feet.add(0, 1.0, 0);
                    }
                    case "sky" -> {
                        from = feet.add(0, 5.0, 0);
                        to = feet;
                    }
                    default -> {
                        from = feet.add(0, 1.0, 0);
                        to = feet.add(0, 2.0, 0);
                    }
                }
                VfxSpawn.send(level, shape, from, to, 0xFF000000 | s.rgb(), s.duration(), s.power());
            }
        } catch (RuntimeException | LinkageError ignored) {
            // an effect must never break an act
        }
    }
}
