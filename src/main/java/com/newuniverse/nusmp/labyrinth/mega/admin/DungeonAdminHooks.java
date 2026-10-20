package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.ManaCoreItem;
import com.newuniverse.nusmp.labyrinth.mega.rift.DimensionalRiftBlockEntity;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftService;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

/**
 * Why: the {@code /dungeon boss ...} and {@code /dungeon rift ...} commands drive two systems that are written by OTHER hands and land at other times (the 200 Decennial bosses with
 * their thematic arenas, and the rare rifts with their exclusive pocket dungeons). The commands must not import either of them, or a rename in one would stop this tree from
 * compiling and the three could not be developed side by side; so the commands call only the two small interfaces below, and the interfaces are filled with whatever exists:
 * the DEFAULT implementations use the mechanisms the dungeon already has today (the D&amp;D Labyrinth Guardian that {@code /multiverse mega core guardian} and the pocket realms
 * spawn, scaled and profiled by the floor it is made for, and the rift block that {@code /multiverse mega rift open} places), so both commands work from the first build. When the
 * Decennial boss facade ({@code DecennialBosses.spawnBoss / killBosses}) and the rift facade ({@code RiftAdmin.spawnRift}) exist, ONE setter call each makes the commands use
 * them instead (see {@code AdminRegistry}); passing null to a setter puts the default back. The hooks are volatile statics because they are set once at start-up and read on the
 * server thread; a hook that throws is caught by the command, which then tells the admin and keeps the server running.
 */
public final class DungeonAdminHooks {
    private DungeonAdminHooks() {}

    // ---------------------------------------------------------------- the bosses
    /** Spawns and slays Decennial bosses. */
    public interface BossHook {
        /**
         * Spawns the boss of dungeon level {@code dungeonLevel} (-100 to -1, 1 to 100; level 4 is floor 40) at {@code pos}, with its arena where the facade builds one. True when a
         * boss was made.
         */
        boolean spawnBoss(ServerLevel level, int dungeonLevel, BlockPos pos);

        /** Slays the boss the player is up against (their floor's) and drops its Mana Core. Returns how many bosses were slain (0 when there was none). */
        int killBosses(ServerLevel level, ServerPlayer player);

        /** Builds a hook out of two method references, e.g. {@code BossHook.of(DecennialBosses::spawnBoss, DecennialBosses::killBosses)}. */
        static BossHook of(BossSpawn spawn, BossKill kill) {
            return new BossHook() {
                @Override public boolean spawnBoss(ServerLevel level, int dungeonLevel, BlockPos pos) { return spawn.spawn(level, dungeonLevel, pos); }

                @Override public int killBosses(ServerLevel level, ServerPlayer player) { return kill.kill(level, player); }
            };
        }
    }

    @FunctionalInterface
    public interface BossSpawn { boolean spawn(ServerLevel level, int dungeonLevel, BlockPos pos); }

    @FunctionalInterface
    public interface BossKill { int kill(ServerLevel level, ServerPlayer player); }

    // ---------------------------------------------------------------- the rifts
    /** Opens a rift. */
    @FunctionalInterface
    public interface RiftHook {
        /** Opens a rift at {@code pos}: a pocket-dungeon rift when {@code pocket}, else a floor shortcut. True when one opened. */
        boolean spawnRift(ServerLevel level, BlockPos pos, boolean pocket);
    }

    private static volatile BossHook boss = new DefaultBoss();
    private static volatile RiftHook rift = new DefaultRift();
    private static volatile boolean customBoss, customRift;

    /** The hook the boss commands call. */
    public static BossHook bossHook() { return boss; }

    /** The hook the rift command calls. */
    public static RiftHook riftHook() { return rift; }

    /** Replaces the boss hook (null puts the default, which uses the existing Labyrinth Guardian, back). */
    public static void setBossHook(BossHook hook) {
        customBoss = hook != null;
        boss = hook != null ? hook : new DefaultBoss();
    }

    /** Replaces the rift hook (null puts the default, which uses the existing rift block, back). */
    public static void setRiftHook(RiftHook hook) {
        customRift = hook != null;
        rift = hook != null ? hook : new DefaultRift();
    }

    /** Is a facade wired in (true) or are the default mechanisms in use (false)? For the status line and the self-test. */
    public static boolean customBoss() { return customBoss; }

    public static boolean customRift() { return customRift; }

    // ================================================================ the defaults
    /**
     * The boss the dungeon has today: the Labyrinth Guardian, made for the boss floor of the level exactly as a pocket realm makes its secret boss ({@code configureForFloor}: scaled by
     * the floor's threat, given the D&amp;D profile of that floor). It is a free boss (no altar), so it unseals nothing by itself; killing it drops the Mana Core of the sector it stands
     * in, which is what the Gatekeeper tests need.
     */
    private static final class DefaultBoss implements BossHook {
        /** A scoreboard tag on the bosses the admin tree made, so a slay finds them even when they wandered. */
        static final String TAG = "nusmp_admin_boss";

        @Override
        public boolean spawnBoss(ServerLevel level, int dungeonLevel, BlockPos pos) {
            if (!DungeonAdminMath.validLevel(dungeonLevel)) return false;
            int floor = DungeonAdminMath.bossFloorOfLevel(dungeonLevel);
            LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(level, pos, null, null);
            if (g == null) return false;
            g.configureForFloor(floor, themeFor(level, pos, floor));
            g.addTag(TAG);
            level.addFreshEntity(g);
            return true;
        }

        /** The theme of the sector the boss stands in, else the theme of the floor's own sector when it exists, else the baseline. */
        private static RegionTheme themeFor(ServerLevel level, BlockPos pos, int floor) {
            Sector here = MegaDungeon.sectorAt(level, pos);
            if (here != null) return here.theme;
            Polarity road = Polarity.ofFloor(floor);
            Sector at = road == null ? null : MegaData.get(level.getServer()).chainSector(0, road, DungeonAdminMath.chainIndexOf(floor, 1));
            return at != null ? at.theme : RegionTheme.GRAVITY;
        }

        @Override
        public int killBosses(ServerLevel level, ServerPlayer player) {
            List<LabyrinthGuardianEntity> near = level.getEntitiesOfClass(LabyrinthGuardianEntity.class, player.getBoundingBox().inflate(128.0), LabyrinthGuardianEntity::isAlive);
            int slain = 0;
            for (LabyrinthGuardianEntity g : near) {
                boolean bound = g.anchor() != null;                                    // a boss called by an altar: the altar forges its own core when it hears the death
                BlockPos at = g.blockPosition();
                Sector sector = g.sectorId() >= 0 ? MegaData.get(level.getServer()).sector(g.sectorId()) : MegaDungeon.sectorAt(level, at);
                g.dpsBudget().bypass(() -> g.setHealth(0f));                           // the boss's damage budget would cut /kill itself down to a few percent a second
                g.die(level.damageSources().genericKill());                            // the damage that bypasses invulnerability: the Mythic Trait does not answer it
                if (g.isAlive()) continue;
                slain++;
                if (!bound && sector != null) {
                    ItemEntity core = new ItemEntity(level, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, ManaCoreItem.forSector(sector.id, sector.theme));
                    ManaCoreItem.protect(core);
                    level.addFreshEntity(core);
                }
            }
            return slain;
        }
    }

    /**
     * The rift the dungeon has today: the same three calls {@code /multiverse mega rift open} makes (place the block entity, aim a shortcut from the floor under it, announce it to the
     * delvers nearby). A shortcut is only valid on a floor of the Mega-Dungeon, so one opened anywhere else is closed again and reported as not opened.
     */
    private static final class DefaultRift implements RiftHook {
        @Override
        public boolean spawnRift(ServerLevel level, BlockPos pos, boolean pocket) {
            RiftType type = pocket ? RiftType.POCKET : RiftType.SHORTCUT;
            DimensionalRiftBlockEntity be = RiftService.open(level, pos, type, level.getRandom().nextLong());
            if (be == null) return false;
            if (!pocket) {
                Sector here = Labyrinth.inMega(level) ? MegaData.get(level.getServer()).sectorAt(pos) : null;
                if (here == null) {
                    be.close();
                    return false;
                }
                RiftService.aimShortcut(MegaData.get(level.getServer()), here, be);
            }
            RiftService.announce(level, pos, type, null);
            return true;
        }
    }
}
