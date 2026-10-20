package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.newuniverse.nusmp.labyrinth.mega.floors.AlignmentAttachments;
import com.newuniverse.nusmp.labyrinth.mega.floors.AlignmentNet;
import com.newuniverse.nusmp.labyrinth.mega.floors.AlignmentRules;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorTracker;
import com.newuniverse.nusmp.labyrinth.mega.rift.WaypointService;
import com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: {@code /dungeon reset <alignment|skills|waypoint|all>} puts a tester back to a "fresh player" so the entry workflows (the Hub's gates, the Directional Lock, the first
 * landing, the Return Crystal) can be run again without a new account. Each kind clears exactly the records the dungeon keeps for the player and nothing else. ALIGNMENT is the
 * {@code PlayerDungeonAlignment} data attachment (the road, the lock, the deepest floor of each road, through the same {@link AlignmentRules#reset} the {@code /multiverse mega floors
 * reset} repair tool uses) plus the floor tracker's memory of the floor they stood on, and it re-sends the cleared state to the client so the Hub's gate hints update. The Hub's
 * way home (the origin the home gate leads back to) and the gate cool-down stamps are left alone on purpose: clearing them would strand a player in the Sanctuary with no gate
 * that leads out. WAYPOINT is the {@code Waypoint} record the Return Crystal and the Gate Key read, plus the remembered deepest Landing of each road (which decides where the next
 * entry through a gate puts the player) and the two item cool-downs, so the next entry starts at the first floor. SKILLS is {@link SkillAdmin#forgetAll}: only the Unique skills the
 * skill books taught, never Tensura's own skills or any vanilla data. All three act on the player who ran the command (a tester resets themselves; for someone else use
 * {@code /execute as <player> run ...}).
 */
public final class ResetAdmin {
    private ResetAdmin() {}

    /** Clears the player's alignment attachment and the tracker's memory of their floor. Returns true when there was something to clear. */
    public static boolean alignment(ServerPlayer p) {
        boolean had = !AlignmentAttachments.get(p).isDefault();
        AlignmentRules.reset(AlignmentAttachments.get(p));
        FloorTracker.forget(p.getUUID());
        AlignmentNet.send(p);
        return had;
    }

    /** Clears the waypoint, the deepest-Landing memory of both roads and the two waypoint item cool-downs. Returns true when there was something to clear. */
    public static boolean waypoint(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        boolean had = WaypointService.get(p) != null || data.contains(MegaTravel.LANDING_KEY) || data.contains(MegaTravel.LANDING_HOLY) || data.contains(MegaTravel.LANDING_ABYSSAL);
        WaypointService.clear(p);
        data.remove(MegaTravel.LANDING_KEY);
        data.remove(MegaTravel.LANDING_HOLY);
        data.remove(MegaTravel.LANDING_ABYSSAL);
        data.remove(WaypointService.CRYSTAL_UNTIL);
        data.remove(WaypointService.KEY_UNTIL);
        return had;
    }

    /** {@code reset alignment}: clears it and says so. */
    public static int doAlignment(ServerPlayer p) {
        alignment(p);
        AdminMsg.tell(p, AdminMsg.green(AdminText.RESET_ALIGNMENT));
        return 1;
    }

    /** {@code reset waypoint}: clears it and says so (or says there was nothing). */
    public static int doWaypoint(ServerPlayer p) {
        if (waypoint(p)) {
            AdminMsg.tell(p, AdminMsg.green(AdminText.RESET_WAYPOINT));
            return 1;
        }
        AdminMsg.tell(p, AdminMsg.gray(AdminText.RESET_WAYPOINT_NONE));
        return 1;
    }

    /** {@code reset all}: alignment and waypoint now, the skills over the next ticks (its job ends with the "everything cleared" line). */
    public static int doAll(ServerPlayer p) {
        alignment(p);
        waypoint(p);
        AdminMsg.tell(p, AdminMsg.green(AdminText.RESET_ALIGNMENT));
        AdminMsg.tell(p, AdminMsg.green(AdminText.RESET_WAYPOINT));
        SkillAdmin.forgetAll(p, true);
        return 1;
    }
}
