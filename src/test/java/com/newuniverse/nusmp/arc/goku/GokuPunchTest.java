package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.boss.rules.BossSkillPolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 4.98.0: Goku's fists hit harder, and no boss drains or steals from a player. */
class GokuPunchTest {
    @Test
    void punchesMultiplyOnlyPhysicalBlowsAndRaiseTheirCap() {
        assertEquals(40f, GokuBossMath.punchDamage(10f, true, false, 4.0), 1e-4);
        assertEquals(10f, GokuBossMath.punchDamage(10f, true, true, 4.0), 1e-4);
        assertEquals(10f, GokuBossMath.punchDamage(10f, false, false, 4.0), 1e-4);
        assertEquals(0.75, GokuBossMath.hitShare(0.3, true, false, 0.75), 1e-9);
        assertEquals(0.3, GokuBossMath.hitShare(0.3, true, true, 0.75), 1e-9);
        assertEquals(0.5, GokuBossMath.hitShare(0.5, true, false, 0.2), 1e-9);
    }

    @Test
    void skillsThatTakeFromTheTargetAreRecognised() {
        for (String id : new String[] {"tensura:energy_drain", "tensura:magic_steal", "x:mana_siphon", "x:absorb_mana", "x:life_leech", "x:plunder"}) assertTrue(BossSkillPolicy.stealsPools(id), id);
        for (String id : new String[] {"tensura:flame_orb", "tensura:sense", "", null}) assertFalse(BossSkillPolicy.stealsPools(id), String.valueOf(id));
    }

    @Test
    void wiringIsInPlace() throws Exception {
        String omega = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/goku/GokuOmega.java"));
        assertTrue(omega.contains("BossRulesConfig.noPoolTheft()"), "Omega's drain and seal must honour noPoolTheft");
        String bridge = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/book/EnergyBridge.java"));
        assertTrue(bridge.contains("noPoolTheft()"), "the energy-drain helper must give a boss nothing");
        String realm = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/realm/BossRealm.java"));
        assertTrue(realm.contains("timeoutEvicts()") && realm.contains("returnOnlyAfterWin()"), "the realm must not send people home mid-fight");
        String goku = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/goku/GokuBoss.java"));
        assertTrue(goku.contains("punchTick(sl);") && goku.contains("GokuBossMath.punchDamage"));
    }
}
