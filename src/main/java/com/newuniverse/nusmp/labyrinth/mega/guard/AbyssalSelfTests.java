package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the look of a boss is decided on the server (the profile, the sector, the phase) and drawn on the client from the entity's synced data, so the part that can break without any compiler noticing is
 * the hand-over. {@code guard.abyssal_looks} checks it in the real game with real Guardians that are never added to the level: every look of the table has all its files on the class path (model, atlas,
 * glow, animation, and for a humanoid the skin and the extras); every loaded profile that names a look makes the Guardian wear exactly that look, and one that does not keeps the old look (an evil boss
 * of a demon sector wears the demon of its tier); the phase the boss reaches (2, then the mythic 3) is in the synced data and announces itself with a cast cue; and the look and the phase survive a save
 * and a load.
 */
final class AbyssalSelfTests {
    private AbyssalSelfTests() {}

    private static final class Expect extends RuntimeException {
        Expect(String message) { super(message, null, false, false); }
    }

    private static void expect(boolean condition, String message) { if (!condition) throw new Expect(message); }

    static Result looks(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ClassLoader cl = AbyssalLooks.class.getClassLoader();
        List<String> missing = AbyssalLooks.problems(path -> cl.getResource(path) != null);
        expect(missing.isEmpty(), "the table of looks has problems: " + missing.subList(0, Math.min(3, missing.size())));
        expect(AbyssalLooks.all().size() >= 22, "only " + AbyssalLooks.all().size() + " looks");
        BlockPos at = new BlockPos(0, 100, 0);
        int named = 0, kept = 0;
        List<LabyrinthGuardianEntity> made = new ArrayList<>();
        try {
            for (DndProfile p : DndProfiles.all()) {
                LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(level, at, null, null);
                expect(g != null, "no Guardian made");
                made.add(g);
                g.configure(-1, 450, RegionTheme.GRAVITY, null);
                g.dnd().configure(p);
                g.syncLook();
                if (!p.look().isEmpty()) {
                    expect(p.look().equals(g.lookId()), p.id() + " names the look " + p.look() + " but the Guardian wears '" + g.lookId() + "'");
                    expect(AbyssalLooks.get(g.lookId()) != null, p.id() + ": the look " + g.lookId() + " is not in the table");
                    named++;
                } else {
                    expect(g.lookId().isEmpty(), p.id() + " names no look but the Guardian wears '" + g.lookId() + "' on a gravity sector");
                    kept++;
                }
                if (p.side().equals("evil") && p.look().isEmpty()) {                       // an evil boss on a demon sector wears the demon of its tier
                    g.configure(-1, 450, RegionTheme.ABYSS, null);
                    g.dnd().configure(p);
                    g.syncLook();
                    expect(!g.lookId().isEmpty() && AbyssalLooks.get(g.lookId()) != null, p.id() + " on an Abyss sector wears no demon (" + g.lookId() + ")");
                }
                if (p.famous() && !p.look().isEmpty()) {                                    // the phases reach the synced data, each with a cast cue
                    g.configure(-1, 450, RegionTheme.GRAVITY, null);
                    g.dnd().configure(p);
                    g.syncLook();
                    expect(g.lookPhase() == 1, p.id() + " does not start in phase 1");
                    g.refreshDndName(p, 2);
                    expect(g.lookPhase() == 2, p.id() + " did not reach phase 2 (" + g.lookPhase() + ")");
                    expect(g.castKind() == 2 && g.castAt() >= 0, p.id() + " did not cue its change of phase");
                    g.refreshDndName(p, 3);
                    expect(g.lookPhase() == 3, p.id() + " did not reach phase 3 (" + g.lookPhase() + ")");
                }
            }
            // a Guardian that is saved and loaded again wears the same look in the same phase
            int sector = -1;
            DndProfile famous = null;
            for (int s = 0; s < 400 && famous == null; s++) {
                DndProfile p = DndProfiles.forBoss(450, s);
                if (p.famous() && !p.look().isEmpty()) {
                    famous = p;
                    sector = s;
                }
            }
            expect(famous != null, "no sector of floor 450 gets a famous boss with a look");
            LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(level, at, null, null);
            made.add(g);
            g.configure(sector, 450, RegionTheme.GRAVITY, null);
            expect(famous.look().equals(g.lookId()), "sector " + sector + " should wear " + famous.look() + ", not '" + g.lookId() + "'");
            g.dnd().phase = 2;
            g.refreshDndName(famous, 2);
            CompoundTag tag = new CompoundTag();
            g.addAdditionalSaveData(tag);
            LabyrinthGuardianEntity h = LabyrinthGuardianEntity.create(level, at, null, null);
            made.add(h);
            h.readAdditionalSaveData(tag);
            expect(famous.look().equals(h.lookId()), "the look did not survive a save: '" + h.lookId() + "'");
            expect(h.lookPhase() == 2, "the phase did not survive a save: " + h.lookPhase());
            return Result.ok(AbyssalLooks.all().size() + " looks with all their files; " + named + " profiles wear their named look, " + kept + " keep the old one; the phases reach the client; a saved " + famous.id() + " wears " + h.lookId() + " again");
        } finally {
            for (LabyrinthGuardianEntity e : made) e.discard();
        }
    }
}
