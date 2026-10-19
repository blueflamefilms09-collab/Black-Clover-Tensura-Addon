package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftService;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftType;
import com.newuniverse.nusmp.labyrinth.mega.rift.WaypointService;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the relic side of the rifts and the waypoint (1.72.0): the abilities of the Return Crystal, the Gate Key, the Return Portal, the Rift Compass, the Pocket Key and the Waypoint Anchor. Each is a
 * thin call into {@code labyrinth.mega.rift} (which owns the rules, the cooldowns and the messages), so a relic never duplicates a rule: the Crystal takes the holder to Floor 0 and keeps the place
 * they stood, the Key takes them back to that place from outside the dungeon (or, sneaking, opens a Return Portal for the whole party), the Compass names the nearest open rift and how far it
 * is, the Pocket Key opens a hidden realm's rift in front of the holder, and the Anchor marks the place now. Used from the generated wave 36 through the table's {@code rift:<name>} calls.
 */
public final class RiftActs {
    private RiftActs() {}

    public static void crystal(ActCtx c) { WaypointService.crystal(c.player()); }

    public static void key(ActCtx c) { WaypointService.gateKey(c.player()); }

    public static void portal(ActCtx c) {
        ServerPlayer p = c.player();
        if (Labyrinth.inMega(p.level())) {
            Fx.say(p, "A Return Portal opens on Floor 0 and the other gates, not inside the floors.");
            return;
        }
        RiftService.openReturnPortal(p);
    }

    public static void mark(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.say(p, WaypointService.save(p) ? "The anchor holds this place." : "The anchor will not hold here: stand on solid ground in a floor, away from the stairs.");
    }

    /** Names the nearest open rift within 96 blocks, by direction and distance. */
    public static void sense(ActCtx c) {
        ServerPlayer p = c.player();
        BlockPos best = null;
        double bd = 96.0 * 96.0;
        for (BlockPos r : RiftService.openRifts(p.level())) {              // 1.94.0: the rifts of the player's own dimension (the Hub's portals share coordinates with the floors)
            double d = r.distSqr(p.blockPosition());
            if (d < bd) {
                bd = d;
                best = r;
            }
        }
        if (best == null) {
            Fx.say(p, "The needle is still: no tear is open near you.");
            return;
        }
        double dx = best.getX() + 0.5 - p.getX(), dz = best.getZ() + 0.5 - p.getZ();
        String[] names = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        int i = (int) Math.round(((Math.toDegrees(Math.atan2(-dx, dz)) % 360 + 360) % 360) / 45.0) % 8;
        Fx.say(p, "A tear is open " + (int) Math.round(Math.sqrt(bd)) + " blocks " + names[i] + ", " + (best.getY() - (int) p.getY()) + " levels from you.");
    }

    /** Opens a hidden realm's rift three blocks ahead (inside a floor of the dungeon only). */
    public static void pocket(ActCtx c) {
        ServerPlayer p = c.player();
        if (!Labyrinth.inMega(p.level()) || RiftRules.slotAt(p.getX(), p.getZ()) >= 0) {
            Fx.say(p, "The key only turns inside the dungeon's floors.");
            return;
        }
        BlockPos at = p.blockPosition().relative(p.getDirection(), 3);
        for (int dy = 0; dy >= -1; dy--) {
            if (RiftService.open(p.serverLevel(), at.above(dy), RiftType.POCKET, p.getRandom().nextLong()) != null) {
                Fx.say(p, "The air parts: a hidden realm waits behind it.");
                return;
            }
        }
        Fx.say(p, "There is no room for a tear here.");
    }
}
