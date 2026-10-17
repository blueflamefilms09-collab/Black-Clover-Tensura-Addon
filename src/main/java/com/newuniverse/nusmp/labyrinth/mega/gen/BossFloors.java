package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.Sector;

/**
 * Why: until 1.58.0 "sector 0" was a whole ten-floor sector, so it always held the boss chain (the arena with the Guardian, the Gatekeeper door, the vault with the
 * Crucible, the Way Onward), and the in-game self-tests of the Guardian, the Crucible and the themes all took sector 0 as the sector to test. In the floor model sector 0
 * is floor +1, an ordinary floor with no boss chain at all (the chain belongs to every tenth floor), so those tests need another sector: the first BOSS floor. This is the
 * one place that says which sector that is in either model, so a self-test asks a single question, "give me a sector that has a boss chain", and the CI smoke test only
 * has to grow a road to floor 10 for it to have an answer.
 */
public final class BossFloors {
    private BossFloors() {}

    /**
     * The first finished sector that holds a boss chain: sector 0 in a world of ten-floor sectors (it always does), else the boss floor with the lowest id (floor +10 of
     * the Holy road unless only the Abyssal road has grown that far), else null (no boss floor has been built yet).
     */
    public static Sector first(MegaData data) {
        if (data == null) return null;
        Sector s0 = data.sector(0);
        if (s0 != null && !s0.isFloor()) return s0;
        Sector best = null;
        for (Sector s : data.sectors()) {
            if (s.isFloor() && s.isBossFloor() && SectorGenerator.playable(s) && (best == null || s.id < best.id)) best = s;
        }
        return best;
    }
}
