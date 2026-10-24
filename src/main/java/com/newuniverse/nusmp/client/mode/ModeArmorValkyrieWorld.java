package com.newuniverse.nusmp.client.mode;

import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.ModeArmorValkyrieBudget;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.client.aura.DiveCast;
import com.newuniverse.nusmp.valkyrielance.ValkyrieLanceBoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * What the game says about the Valkyrie wearers this frame, in one small class so that the layer ({@link ModeArmorValkyrieV2}) stays free of world queries (and the headless preview has a stub of just this):
 * who wears the mode and how far from the camera (one pass over the level's players, once a frame, into fixed arrays), whether a wearer holds the Valkyrie Drill Lance item (then its armour lance stays off), and
 * whether the wearer is charging or attacking (the lance spins up). Render thread only; nothing is allocated.
 */
final class ModeArmorValkyrieWorld {
    private ModeArmorValkyrieWorld() {}

    /**
     * Fills the arrays with up to {@code ids.length} wearers of the mode VALKYRIE that are drawn this frame (not the local player in first person, not invisible), the nearest ones when there are more; returns
     * how many. {@code holds[i]} = the wearer holds the lance item.
     */
    static int scan(long gameTime, int[] ids, double[] distSq, boolean[] holds) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return 0;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        int n = 0, cap = ids.length;
        var players = level.players();
        for (int i = 0, size = players.size(); i < size; i++) {
            AbstractClientPlayer p = players.get(i);
            if (ModeArmorClient.mode(p.getId(), gameTime) != ModeArmor.Mode.VALKYRIE || p.isInvisible() || (p == mc.player && firstPerson)) continue;
            double d = p.distanceToSqr(cam);
            int at;
            if (n < cap) at = n++;
            else {                                                               // a full table: the new wearer replaces the farthest one if it is nearer
                at = 0;
                for (int k = 1; k < cap; k++) if (distSq[k] > distSq[at]) at = k;
                if (distSq[at] <= d) continue;
            }
            ids[at] = p.getId();
            distSq[at] = d;
            holds[at] = holdsLance(p);
        }
        return n;
    }

    /** The armour's pool of mesh quads for this client's effect quality level (1200 on HIGH, 720 on MEDIUM, 360 on LOW). */
    static int pool() { return ModeArmorValkyrieBudget.poolFor(PerfSettings.vfxQuality().frameVertices); }

    /** True when the player holds the Valkyrie Drill Lance item in either hand. */
    static boolean holdsLance(AbstractClientPlayer p) {
        net.minecraft.world.item.Item lance = ValkyrieLanceBoot.LANCE.get();
        return p.getMainHandItem().getItem() == lance || p.getOffhandItem().getItem() == lance;
    }

    /** True while the wearer is swinging, using an item or casting: the lance spins up. */
    static boolean busy(AbstractClientPlayer p) { return p.swinging || p.isUsingItem() || DiveCast.casting(p.getId()); }
}
