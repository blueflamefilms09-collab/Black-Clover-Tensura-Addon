package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.beastwill.BeastWillState;
import com.newuniverse.nusmp.beastwill.BeastWillStore;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.skill.rules.WeaponSkillRules;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The CI self-test of the weapon skills (key {@code weapon.skills}, docs/weapon_skills.md): (1) the registry holds all 12 skills as {@link WeaponSkill} of type Extra, each wheel is the table's, every mode's id is unique;
 * (2) on a stand-in player the grant follows the owner-ship both ways, a Djinn (own, then let go) and a Beast Will (bound, then cleared), and a press of a skill the stand-in does not own does nothing and throws nothing; (3) the
 * setting {@code weaponSkills} is read. The Vassal weapon bond is not bonded here (it needs the whole hero system's gates); its listener is the same {@code set} the other two use.
 */
public final class WeaponSkillSelfTests {
    private WeaponSkillSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("weapon.skills", WeaponSkillSelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        int n = 0;
        java.util.Set<String> modeIds = new java.util.HashSet<>();
        for (String id : WeaponSkillRules.allIds()) {
            var h = WeaponSkills.holder(id);
            if (h == null) return Result.fail(id + " was not registered");
            ManasSkill s = SkillAPI.getSkillRegistry().get(h.getId());
            if (!(s instanceof WeaponSkill w)) return Result.fail(id + " is not registered as a weapon skill");
            if (w.getType() != Skill.SkillType.EXTRA) return Result.fail(id + ": wrong skill type");
            if (w.wheel().size() != WeaponSkillRules.modes(w.family()).size() || w.wheel().size() < 2) return Result.fail(id + ": the wheel does not match the table");
            if (w.family() == WeaponSkillRules.Kind.HERO && !WeaponSkillRules.coversHero(w.heroKind())) return Result.fail(id + ": a hero skill for a weapon the table does not cover");
            for (int k = 0; k < w.wheel().size(); k++) if (!modeIds.add(w.getModeId(null, k))) return Result.fail(id + ": the mode id " + w.getModeId(null, k) + " is used twice");
            n++;
        }
        if (n != 12 || WeaponSkills.count() != 12) return Result.fail(n + " weapon skills registered, expected 12 (10 Vassal weapons, the Djinn, the Beast Will)");
        if (WeaponSkillRules.heroKinds().size() != 10) return Result.fail("the table covers " + WeaponSkillRules.heroKinds().size() + " weapons, expected the 10 Vassal Weapons");

        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "weapon_skills", at.x, at.y, at.z);
        boolean on = NUConfig.WEAPON_SKILLS.get();
        String played = "grant/take-back not exercised (setting weaponSkills is off)";
        if (on) {
            try {
                if (SkillAPI.getSkillsFrom(p) == null) return Result.ok(n + " weapon skills registered; the grant was NOT exercised (the stand-in has no skill storage)");
                WeaponSkills.set(p, WeaponSkillRules.DJINN_ID, false);
                WeaponSkills.set(p, WeaponSkillRules.BEAST_ID, false);

                // a press without owning anything: no exception, nothing granted
                WeaponSkill djinn = WeaponSkills.holder(WeaponSkillRules.DJINN_ID).get();
                djinn.onPressed(djinn.createDefaultInstance(), p, 0, 0);

                // the Djinn: bound -> skill, let go -> gone
                DjinnState st = DjinnState.of(p);
                st.bind(Djinn.BAAL, UUID.nameUUIDFromBytes("weapon.skills".getBytes(java.nio.charset.StandardCharsets.UTF_8)), 20);
                if (!WeaponSkills.has(p, WeaponSkillRules.DJINN_ID)) return Result.fail("binding a Djinn did not grant the Djinn Equip skill");
                st.clearBond();
                if (WeaponSkills.has(p, WeaponSkillRules.DJINN_ID)) return Result.fail("letting the Djinn go did not take the Djinn Equip skill back");

                // the Beast Will: bound -> skill, cleared -> gone
                BeastWillState bs = BeastWillStore.of(p);
                bs.wills.clear();
                bs.active = "";
                bs.bind("lion_will");
                bs.activate("lion_will", 0L);
                WeaponSkills.syncBeast(p);
                if (!WeaponSkills.has(p, WeaponSkillRules.BEAST_ID)) return Result.fail("binding a Beast Will did not grant the Beast Will skill");
                WeaponSkill beast = WeaponSkills.holder(WeaponSkillRules.BEAST_ID).get();
                WeaponSkills.forgetTurn(p);                                               // the self-test presses twice in one tick
                beast.onPressed(beast.createDefaultInstance(), p, 0, 0);                  // the first mode (Acquire Form) runs without an exception (a stand-in with no progress: the manager says why on the action bar)
                bs.wills.clear();
                bs.active = "";
                WeaponSkills.syncBeast(p);
                if (WeaponSkills.has(p, WeaponSkillRules.BEAST_ID)) return Result.fail("clearing the wills did not take the Beast Will skill back");
                played = "the Djinn and the Beast Will skills follow the bond both ways and a press of each runs without an exception";
            } finally {
                try { DjinnState.of(p).clearBond(); } catch (RuntimeException ignored) { /* the stand-in is thrown away */ }
                BeastWillState bs = BeastWillStore.peek(p);
                if (bs != null) { bs.wills.clear(); bs.active = ""; }
                WeaponSkills.set(p, WeaponSkillRules.DJINN_ID, false);
                WeaponSkills.set(p, WeaponSkillRules.BEAST_ID, false);
            }
        }
        return Result.ok(n + " weapon skills registered (" + WeaponSkillRules.heroKinds().size() + " Vassal weapons, Djinn Equip, Beast Will), all wheels match the table, " + played + "; server setting weaponSkills=" + on);
    }
}
