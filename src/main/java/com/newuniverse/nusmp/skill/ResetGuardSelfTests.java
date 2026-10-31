package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.item.misc.ResetScrollItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.function.Consumer;

/**
 * The CI self-test of the Reset Scroll guard (3.42.0, id {@code skill.reset_guard}). A stand-in player is given a skill of this mod, then Tensura's own reset calls are run on it (skill reset,
 * skill reset with the "everything" flag, full reset): the skill must survive each of them, while a removal by this mod's own code (the way a grimoire swap or an admin take works) must still
 * work. A Tensura call that throws on a stand-in (no connection, no race data) is not our failure: it is told in the result, and the check goes on with the next one. A Tensura skill is
 * learned alongside as a control and its fate is reported, so a reader can see that the scroll really removes skills on the test player (the result never fails on the control).
 */
public final class ResetGuardSelfTests {
    private ResetGuardSelfTests() {}

    public static void register() {
        SelfTests.register("skill.reset_guard", ResetGuardSelfTests::guard);
    }

    private static Result guard(MinecraftServer server) {
        if (!ResetGuard.enabled()) return Result.ok("the guard is switched off in the server config (guard.resetScrollGuard = false); nothing to check");
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "reset_guard", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        var skills = SkillAPI.getSkillsFrom(p);
        ResourceLocation ours = NUSkills.HERCULES.getId();
        ResourceLocation control = controlSkill();
        StringBuilder sb = new StringBuilder();
        try {
            skills.forgetSkill(ours);
            if (control != null) skills.forgetSkill(control);

            // 1. this mod's own removal still works (it is not a scroll call)
            if (!skills.learnSkill(NUSkills.HERCULES.get().createDefaultInstance())) return Result.fail("the stand-in could not learn the test skill");
            skills.forgetSkill(ours);
            if (skills.getSkill(ours).isPresent()) return Result.fail("a removal by this mod's own code was refused (the guard must only refuse the scrolls)");
            sb.append("own removal passes; ");

            // 2. the three Tensura resets
            int ran = 0;
            String[] names = {"skill reset", "skill reset (all)", "full reset"};
            List<Consumer<ServerPlayer>> calls = List.of(pl -> ResetScrollItem.resetSkill(pl), pl -> ResetScrollItem.resetSkill(pl, true), pl -> ResetScrollItem.resetEverything(pl));
            for (int i = 0; i < calls.size(); i++) {
                skills.learnSkill(NUSkills.HERCULES.get().createDefaultInstance());
                if (skills.getSkill(ours).isEmpty()) return Result.fail("the stand-in could not learn the test skill again");
                boolean controlLearned = control != null && skills.learnSkill(control);
                String note;
                try {
                    calls.get(i).accept(p);
                    ran++;
                    note = "ran";
                } catch (RuntimeException | LinkageError e) {
                    note = "threw " + e.getClass().getSimpleName();
                }
                if (skills.getSkill(ours).isEmpty()) return Result.fail("Tensura's " + names[i] + " removed a skill of this mod (" + note + ")");
                sb.append(names[i]).append(" ").append(note).append(", ours kept");
                if (controlLearned) sb.append(", control ").append(control.getPath()).append(skills.getSkill(control).isPresent() ? " kept" : " removed");
                sb.append("; ");
                skills.forgetSkill(ours);
                if (control != null) skills.forgetSkill(control);
            }
            if (ran == 0) sb.append("NOTE: no Tensura reset could run on a stand-in, so the survival of the skill was not exercised; ");
            return Result.ok(sb.toString().trim());
        } finally {
            try {
                skills.forgetSkill(ours);
                if (control != null) skills.forgetSkill(control);
            } catch (RuntimeException ignored) {
                // the clean-up of a stand-in never hides the result
            }
        }
    }

    /** The first Tensura skill in the registry (the control), or null when there is none. */
    private static ResourceLocation controlSkill() {
        try {
            for (ResourceLocation k : SkillAPI.getSkillRegistry().getIds()) if ("tensura".equals(k.getNamespace())) return k;
        } catch (RuntimeException ignored) {
            // no control: the check of our own skill still runs
        }
        return null;
    }
}
