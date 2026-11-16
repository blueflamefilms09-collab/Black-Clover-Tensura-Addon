package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * The CI self-tests of the guide P1 fixes (ids {@code guide.p1.damage_source}, {@code guide.p1.grimoire_skills}, {@code guide.p1.spell_runtime}): a rebuilt skill's hit with its instance is a
 * Tensura skill source (magic = Tensura magic, attack = a physical player attack) while the setting is on and the old vanilla source without an instance; every grimoire_<magic> skill is registered;
 * a stand-in player in its own level is not "gone" for SpellRuntime. Reads only: no damage dealt, no block placed, no list cleared (the smoke's --restart runs the real stop path).
 */
public final class GuideP1SelfTests {
    private GuideP1SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("guide.p1.damage_source", GuideP1SelfTests::damageSource);
        SelfTests.register("guide.p1.grimoire_skills", s -> grimoireSkills());
        SelfTests.register("guide.p1.spell_runtime", GuideP1SelfTests::spellRuntime);
    }

    private static Result damageSource(MinecraftServer server) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(NUSkills.GATE_OF_BABYLON.getId());
        if (!(skill instanceof GateOfBabylonSkill gate)) return Result.fail("Gate of Babylon is not registered: " + skill);
        ManasSkillInstance inst = gate.createDefaultInstance();
        FakePlayer p = SelfTests.player(server.overworld(), "GuideP1Caster", 0, 300, 0);
        DamageSource magic = SkillDamage.source(p, SkillDamage.Kind.MAGIC, inst), attack = SkillDamage.source(p, SkillDamage.Kind.ATTACK, inst);
        if (magic.getEntity() != p || attack.getEntity() != p) return Result.fail("the skill source does not credit the caster");
        boolean tensura = NUConfig.TENSURA_SKILL_DAMAGE_SOURCE.get();
        if (tensura && !magic.is(TensuraDamageTypes.MAGIC_GENERIC)) return Result.fail("a magic hit with the instance is not Tensura magic: " + magic.getMsgId());
        if (!tensura && !magic.is(DamageTypes.INDIRECT_MAGIC)) return Result.fail("tensuraSkillDamageSource=false must keep the vanilla magic source: " + magic.getMsgId());
        if (!attack.is(DamageTypes.PLAYER_ATTACK)) return Result.fail("an attack hit is not a physical player attack: " + attack.getMsgId());
        if (!SkillDamage.source(p, SkillDamage.Kind.MAGIC, null).is(DamageTypes.INDIRECT_MAGIC)) return Result.fail("a hit without an instance lost the old vanilla source");
        return Result.ok("magic=" + magic.getMsgId() + " attack=" + attack.getMsgId() + " (tensuraSkillDamageSource=" + tensura + ")");
    }

    private static Result grimoireSkills() {
        for (MagicType m : MagicType.values()) {
            var holder = NUSkills.GRIMOIRES.get(m);
            if (holder == null || !(SkillAPI.getSkillRegistry().get(holder.getId()) instanceof GrimoireMagicSkill)) return Result.fail("grimoire_" + m.name().toLowerCase() + " is not registered");
        }
        return Result.ok(MagicType.values().length + " grimoire skills registered (names and descriptions: GrimoireSkillLangTest)");
    }

    private static Result spellRuntime(MinecraftServer server) {
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "GuideP1Runtime", 0, 300, 0);
        if (SpellRuntime.gone(p, level)) return Result.fail("a present caster counts as gone");
        for (ServerLevel other : server.getAllLevels())
            if (other != level && !SpellRuntime.gone(p, other)) return Result.fail("a caster in another level does not count as gone");
        return Result.ok("bolts of removed, disconnected or moved casters fizzle; temp blocks are restored on server stop");
    }
}
