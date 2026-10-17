package com.newuniverse.nusmp.arc.midnight;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Third Eye is not one person but the trio at the head of the Eye of the Midnight Sun: Vetto (Despair), Rhya (the Disloyal) and Fana (Hatred), each
 * an elf soul in a human body marked by a third eye ({@link EyeBondRules}). The brief asked for the Third Eye as a multi-body encounter if the story supports
 * it, and it does, so the three stay three separate bosses of the roster (the roster, the story chain, the Charter and the floor picker all want one boss per
 * record) and this class is what makes them fight as one: when one of them falls, the others that are still standing within an arena's reach take one rage
 * step (+10% power, the same step the Dark Triad takes when a sibling falls), and an operator or an arena can raise all three at once with {@link #summonTrio}.
 * The bond listens to the arc layer's own "a boss was beaten" hook, so it needs no change to anyone's boss class (Vetto's included): a member the roster does
 * not know simply never takes part.
 */
public final class ThirdEyeBond {
    private ThirdEyeBond() {}

    private static boolean installed;

    /** Hooks the bond into the arc layer's defeat listeners, once. Called from the static initialiser of the boss classes that are part of the trio. */
    static synchronized void install() {
        if (installed) return;
        installed = true;
        ArcBoss.ON_DEFEATED.add(ThirdEyeBond::onFall);
    }

    /** A boss was beaten: if it is one of the trio, every other member standing nearby rages (once per fall). */
    static void onFall(ArcBoss fallen) {
        try {
            if (!MidnightConfig.bondRage() || !(fallen.level() instanceof ServerLevel sl)) return;
            String id = fallen.character().id();
            if (!EyeBondRules.isMember(id)) return;
            List<ArcBoss> others = sl.getEntitiesOfClass(ArcBoss.class, fallen.getBoundingBox().inflate(EyeBondRules.RAGE_RADIUS),
                    b -> b != fallen && b.isAlive() && EyeBondRules.ragesOn(id, b.character().id()));
            for (ArcBoss b : others) b.enrage(fallen.character().name());
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] the Third Eye bond failed: {}", e.toString());
        }
    }

    /**
     * Raises the whole Third Eye round {@code centre}: Vetto, Fana and Rhya that the roster knows, on a ring of five blocks, facing the middle. Each goes
     * through the normal summoning rules ({@link ArcSpawner#summon}); {@code force} skips the story gate, as an operator's command does. Returns the bosses made.
     */
    public static List<ArcBoss> summonTrio(ServerLevel sl, Vec3 centre, boolean force) {
        List<ArcBoss> out = new ArrayList<>();
        int n = EyeBondRules.THIRD_EYE.size();
        for (int i = 0; i < n; i++) {
            ArcCharacter c = ArcRoster.byId(EyeBondRules.THIRD_EYE.get(i));
            if (c == null || !c.isBoss()) continue;
            double a = Math.PI * 2 * i / n;
            Vec3 at = com.newuniverse.nusmp.entity.zogratis.ZogratisBoss.ground(sl, centre.add(Math.cos(a) * 5.0, 0, Math.sin(a) * 5.0));
            float yaw = (float) (Math.toDegrees(a) + 90.0);
            ArcSpawner.Result r = ArcSpawner.summon(sl, c, at, yaw, List.of(), force);
            if (r.boss() != null) out.add(r.boss());
        }
        return out;
    }
}
