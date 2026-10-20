package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialData.Site;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaFrame;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaThemes;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardianAltarBlockEntity;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the public face of the Decennial feature, the four calls the rest of the mod (admin commands, other builders' systems, tests) may make without knowing a single internal class: stand up the
 * boss of a level, kill the bosses near a person, describe the boss of a level in a sentence, and list the levels that have one. The signatures are fixed by the brief and are not to change.
 * {@link #spawnBoss} builds the boss's own hall around {@code at} (its middle: the walking surface under it) and calls the boss through the hall's Guardian Altar, the same way a floor does, so the
 * boss is a real Labyrinth Guardian wearing the level's profile, leashed to its hall, with its seal and its elements; a boss an admin calls pays no loot and unseals nothing. Floors need no call
 * at all: the generator hook builds their halls and the Guardian Altar calls the boss when a party arrives.
 */
public final class DecennialBosses {
    private DecennialBosses() {}

    /**
     * Stands up the boss of dungeon level {@code dungeonLevel} (-100..-1 Evil, 1..100 Holy) with its hall: the hall of the boss's theme is built whole with its middle at {@code at} (the walking
     * surface), and the boss is called on its Guardian Altar. Returns the boss, or null when the level has no boss, the hall does not fit between the bottom and the top of the level, or the
     * game refused the entity.
     */
    public static Entity spawnBoss(ServerLevel level, int dungeonLevel, BlockPos at) {
        if (level == null || at == null) return null;
        DecennialProfile p = DecennialRoster.forLevel(dungeonLevel);
        if (p == null) return null;
        BossArenaStructure plan = ArenaThemes.build(p.theme());
        if (plan == null) return null;
        ArenaFrame frame = new ArenaFrame(at.getX(), at.getY(), at.getZ(), plan.size, 0);
        if (!ArenaPlacer.fits(level, plan, frame.y0())) return null;
        Site site = DecennialArenas.registerStandalone(level, p, plan, frame);
        ArenaPlacer.buildNow(level, plan, frame, true);
        site.cursor = ArenaPlacer.total(plan);
        site.built = true;
        DecennialData.get(level.getServer()).setDirty();
        BlockPos altar = ArenaPlacer.worldPos(frame, plan.c, 3, plan.c);
        if (!(level.getBlockEntity(altar) instanceof GuardianAltarBlockEntity block)) return null;
        DecennialLayer.wish(dungeonLevel, site.key);
        try {
            LabyrinthGuardianEntity g = block.summonNow(level);
            if (g != null) g.decennial().markNoLoot();
            return g;
        } finally {
            DecennialLayer.unwish();
        }
    }

    /**
     * Kills every Decennial boss within 128 blocks of {@code near} (or in the whole level when {@code near} is null), paying no loot, and returns how many died. Their halls open and reset as they do
     * when a boss falls.
     */
    public static int killBosses(ServerLevel level, ServerPlayer near) {
        if (level == null) return 0;
        List<LabyrinthGuardianEntity> found = new ArrayList<>();
        if (near != null) {
            AABB box = near.getBoundingBox().inflate(128.0);
            found.addAll(level.getEntitiesOfClass(LabyrinthGuardianEntity.class, box, g -> g.isAlive() && g.decennial().active()));
        } else {
            for (Entity e : level.getAllEntities()) if (e instanceof LabyrinthGuardianEntity g && g.isAlive() && g.decennial().active()) found.add(g);
        }
        int n = 0;
        for (LabyrinthGuardianEntity g : found) {
            g.adminKill();
            n++;
        }
        return n;
    }

    /** One paragraph about the boss of a level: who, where it fights, how the fight goes, what it pays. */
    public static String describe(int dungeonLevel) {
        DecennialProfile p = DecennialRoster.forLevel(dungeonLevel);
        if (p == null) return "Level " + dungeonLevel + " has no Decennial boss (the levels run from -100 to -1 on the Evil road and from 1 to 100 on the Holy road).";
        StringBuilder b = new StringBuilder();
        b.append("Level ").append(p.level()).append(" (floor ").append(p.floor()).append("): ").append(p.name()).append(", ").append(p.title()).append(" [").append(p.creature()).append("]. ");
        ArenaThemes.Def hall = ArenaThemes.def(p.theme());
        b.append("Hall: ").append(hall == null ? p.theme() : hall.title()).append(" (").append(p.theme()).append("). Phases: ");
        for (int i = 1; i <= p.phaseCount(); i++) {
            DecennialProfile.Phase ph = p.phase(i);
            if (i > 1) b.append(", ");
            b.append(ph.name()).append(" (").append(Math.round(ph.at() * 100)).append("%)");
        }
        b.append(". Lair actions: ");
        for (int i = 0; i < p.lairs().size(); i++) {
            DecennialProfile.LairRef l = p.lairs().get(i);
            if (i > 0) b.append(", ");
            b.append(l.id()).append(" every ").append(l.every()).append(" s");
        }
        b.append(". Signature: ").append(p.signature()).append(". Resists: ").append(p.resist().isEmpty() ? "nothing" : String.join(", ", p.resist())).append(". Pays a ").append(p.loot().grade())
                .append(' ').append(p.loot().kind()).append(" and a tier ").append(p.manaCore().tier()).append(" Mana Core.");
        if (p.band() > 0) b.append(" Gate band ").append(p.band()).append(" of the Underworld Root.");
        if (!p.devil().isEmpty()) b.append(" It is the devil ").append(p.devil()).append('.');
        return b.toString();
    }

    /** The levels that have a boss, ascending (-100 .. -1, 1 .. 100). */
    public static List<Integer> levels() { return DecennialRoster.levels(); }
}
