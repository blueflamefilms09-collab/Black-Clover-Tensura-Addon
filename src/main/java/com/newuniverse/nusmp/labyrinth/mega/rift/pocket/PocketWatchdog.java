package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a tomb is some 40,000 blocks placed over many ticks into chunks that can be unloaded, saved and loaded again, and the owner's report was a tomb that was ONE ROOM, so nobody may take it on trust
 * that what the plan says stands, stands (1.101.0, docs/labyrinth/pocket_fix.md). This reads the finished tomb back: a few dozen probes (the middle of every doorway of every door, the floor of every
 * room, a sample of the seal, the lever, the Sigil's chest, the pedestals, the exit rifts), and reports what is missing in words. The tomb's tick runs it every few seconds while somebody is inside; when
 * it finds a gap it starts a REPAIR pass ({@link PocketBuild} with {@code repair}): the same plan placed again over the tomb, leaving an opened seal and a taken relic as they are. The probes know what a
 * tomb looks like at each stage: before the seal opens its blocks are crying obsidian, after it they are air; the lever and the Sigil's chest only matter until the seal is open. Server thread only.
 */
public final class PocketWatchdog {
    private PocketWatchdog() {}

    /** Ticks between two probes of a tomb that has people in it. */
    public static final int EVERY_TICKS = com.newuniverse.nusmp.labyrinth.mega.rift.PocketSwitchRules.WATCHDOG_TICKS;

    /** What is wrong with a built tomb: an empty list when it is whole. */
    public static List<String> missing(ServerLevel level, PocketRuntime rt, PocketRealm.Pocket pk) {
        List<String> out = new ArrayList<>();
        PocketPlan plan = rt.plan;
        PocketLayout layout = rt.layout;
        for (PocketLayout.Door d : layout.doors()) {
            if (d.sealed()) continue;
            for (int roomId : new int[]{d.a(), d.b()}) {
                int[] w = plan.doorway(d, roomId);
                BlockPos mid = PocketCell.world(rt.slot, (w[0] + w[3]) / 2, w[1] + 1, (w[2] + w[5]) / 2);
                if (!level.getBlockState(mid).getCollisionShape(level, mid).isEmpty()) out.add("the doorway of room " + roomId + " toward " + (roomId == d.a() ? d.b() : d.a()) + " is closed at " + mid.toShortString());
            }
        }
        for (PocketLayout.Room r : layout.rooms()) {
            BlockPos floor = PocketCell.world(rt.slot, r.cx(layout.grid()), PocketPlan.Y0 - 1, r.cz(layout.grid()));
            if (r.kind() != PocketLayout.Kind.RIVER && !level.getBlockState(floor).blocksMotion() && !(pk.phase == 2 && (rt.fallen & (1L << r.id())) != 0)) out.add("no floor in room " + r.id() + " (" + r.kind() + ") at " + floor.toShortString());
        }
        int sampled = 0, wrong = 0;
        for (long l : rt.sealBlocks) {
            if ((sampled++ & 7) != 0) continue;
            BlockPos b = BlockPos.of(l);
            boolean obsidian = level.getBlockState(b).is(Blocks.CRYING_OBSIDIAN);
            if (pk.sealOpen ? obsidian : !obsidian) wrong++;
        }
        if (wrong > 0) out.add("the seal is " + (pk.sealOpen ? "standing though it was opened" : "broken though it was never opened") + " (" + wrong + " sampled blocks)");
        if (!pk.sealOpen && rt.switchPos != null && !level.getBlockState(rt.switchPos).is(Blocks.LEVER)) out.add("the seal lever is missing at " + rt.switchPos.toShortString());
        if (!pk.sealOpen && rt.sigilChest != null && !level.getBlockState(rt.sigilChest).is(Blocks.CHEST)) out.add("the Sigil's chest is missing at " + rt.sigilChest.toShortString());
        for (BlockPos s : rt.signs) {
            if (!pk.sealOpen && !(level.getBlockState(s).getBlock() instanceof WallSignBlock)) {
                out.add("a sign is missing at " + s.toShortString());
                break;
            }
        }
        if (rt.exit != null && !level.getBlockState(rt.exit).is(RiftRegistry.RIFT.get()) && pk.phase < 3) out.add("the exit rift is missing at " + rt.exit.toShortString());
        if (rt.shortcutExit != null && !level.getBlockState(rt.shortcutExit).is(RiftRegistry.RIFT.get()) && pk.phase < 3) out.add("the treasury's exit rift is missing at " + rt.shortcutExit.toShortString());
        return out;
    }

    /** Is a probe due (people inside, a built tomb in its open phase, and the interval has passed)? Pure. */
    public static boolean due(long now, long lastProbe, int phase, boolean occupied) { return com.newuniverse.nusmp.labyrinth.mega.rift.PocketSwitchRules.watchdogDue(now, lastProbe, phase, occupied); }
}
