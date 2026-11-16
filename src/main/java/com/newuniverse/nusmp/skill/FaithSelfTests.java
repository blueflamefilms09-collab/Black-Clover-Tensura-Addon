package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.FaithRules;
import com.newuniverse.nusmp.skill.rules.FaithWorshipRules;
import com.newuniverse.nusmp.skill.rules.FaithWorshipRules.Worshipper;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the Faith fix (docs/faith_fix.md) adds modes, commands, a saved worshipper list and server settings that nobody can watch in a game here, so the CI server smoke test checks them: key
 * {@code faith.rules} (the live SERVER numbers are inside their clamps and the pure rules answer sanely with them) and {@code faith.worship} (both skills are registered with their modes, the old
 * mode numbers kept, the worship commands exist on the dispatcher, a worshipper list survives the tag round trip and the evolution merge, the old Faith key is untouched). Registry reads and a
 * stand-in instance that never belongs to a player: no world, no tick work.
 */
public final class FaithSelfTests {
    private FaithSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("faith.rules", s -> rules());
        SelfTests.register("faith.worship", FaithSelfTests::worship);
    }

    private static Result rules() {
        int cap = NUConfig.faithMaxWorshippers();
        if (cap < FaithWorshipRules.MIN_CAP || cap > FaithWorshipRules.MAX_CAP) return Result.fail("maxWorshippers out of its clamp: " + cap);
        if (NUConfig.faithGiftDiscount() < 0 || NUConfig.faithGiftDiscount() > 90) return Result.fail("worshipperGiftDiscount out of its clamp");
        if (NUConfig.faithOfferingCost() < 1.0) return Result.fail("offeringMagiculeCost below 1");
        if (NUConfig.faithRecruitRange() < 2.0 || NUConfig.faithRecruitRange() > 32.0) return Result.fail("recruitRange out of its clamp");
        if (NUConfig.faithInviteTicks() < 200) return Result.fail("inviteSeconds below 10");
        for (int units : FaithWorshipRules.OFFER_STEPS) {
            if (FaithWorshipRules.offerCost(units, NUConfig.faithOfferingCost()) <= 0) return Result.fail("an offering step has no price: " + units);
        }
        if (FaithWorshipRules.isOfferStep(0) || FaithWorshipRules.isOfferStep(3) || FaithWorshipRules.isOfferStep(-5)) return Result.fail("a bad offering size is accepted");
        if (FaithWorshipRules.trickleGain(1_000_000, 64) > FaithWorshipRules.MAX_TRICKLE_PERIODS * 64) return Result.fail("the trickle is not bounded");
        if (FaithWorshipRules.giftCost(10, true, NUConfig.faithGiftDiscount()) > 10) return Result.fail("a worshipper pays more for a gift");
        if (FaithWorshipRules.prayBonus(1_000) > FaithWorshipRules.MAX_PRAY_BONUS) return Result.fail("the prayer bonus is not capped");
        return Result.ok("worship=" + NUConfig.faithWorship() + ", cap=" + cap + ", trickleTicks=" + NUConfig.faithTrickleTicks());
    }

    private static Result worship(MinecraftServer server) {
        ManasSkill faith = SkillAPI.getSkillRegistry().get(NUSkills.FAITH.getId());
        ManasSkill goddess = SkillAPI.getSkillRegistry().get(NUSkills.GODDESS_OF_FAITH.getId());
        if (!(faith instanceof FaithSkill f)) return Result.fail("faith is not registered as a FaithSkill");
        if (!(goddess instanceof GoddessOfFaithSkill g)) return Result.fail("goddess_of_faith is not registered as a GoddessOfFaithSkill");
        ManasSkillInstance fi = f.createDefaultInstance(), gi = g.createDefaultInstance();
        if (f.getModes(fi) != 5) return Result.fail("Faith has " + f.getModes(fi) + " modes, expected 5");
        if (g.getModes(gi) != 7) return Result.fail("Goddess of Faith has " + g.getModes(gi) + " modes, expected 7");
        if (!"faith.bestow".equals(f.getModeId(fi, 0)) || !"faith.pray".equals(f.getModeId(fi, 1)) || !"faith.check".equals(f.getModeId(fi, 2))) return Result.fail("an old Faith mode moved");
        if (!"faith.blessing".equals(g.getModeId(gi, 3)) || !"faith.miracle".equals(g.getModeId(gi, 4))) return Result.fail("an old Goddess mode moved");
        Set<String> seen = new HashSet<>();
        for (int m = 0; m < 5; m++) if (!seen.add(f.getModeId(fi, m))) return Result.fail("Faith repeats a mode id: " + f.getModeId(fi, m));
        seen.clear();
        for (int m = 0; m < 7; m++) if (!seen.add(g.getModeId(gi, m))) return Result.fail("Goddess repeats a mode id: " + g.getModeId(gi, m));
        if (!"faith.recruit".equals(f.getModeId(fi, 3)) || !"faith.gift".equals(f.getModeId(fi, 4))) return Result.fail("Faith modes 3 and 4 are not Recruit and Gift");
        if (!"faith.recruit".equals(g.getModeId(gi, 5)) || !"faith.gift".equals(g.getModeId(gi, 6))) return Result.fail("Goddess modes 5 and 6 are not Recruit and Gift");

        var root = server.getCommands().getDispatcher().getRoot();
        var faithNode = root.getChild("faith");
        if (faithNode == null) return Result.fail("the /faith command is not registered");
        for (String sub : new String[]{"status", "leave", "offering", "accept", "reject", "offer", "gift"}) {
            if (faithNode.getChild(sub) == null) return Result.fail("/faith " + sub + " is missing");
        }
        var admin = root.getChild("anime_multiverse");
        if (admin == null || admin.getChild("faith") == null) return Result.fail("/anime_multiverse faith (the admin command) is gone");

        // the saved list: round trip, the old key untouched, the evolution merge keeps it
        UUID a = UUID.nameUUIDFromBytes("faith-selftest-a".getBytes(StandardCharsets.UTF_8));
        UUID b = UUID.nameUUIDFromBytes("faith-selftest-b".getBytes(StandardCharsets.UTF_8));
        FaithSkill.setFaith(fi, 7);
        FaithWorship.save(fi, List.of(new Worshipper(a, "Alice", 3), new Worshipper(b, "Bob", 0)));
        List<Worshipper> back = FaithWorship.worshippers(fi);
        if (back.size() != 2 || !back.get(0).id().equals(a) || back.get(0).worship() != 3 || !"Bob".equals(back.get(1).name())) return Result.fail("the worshipper list did not survive the tag: " + back);
        if (FaithSkill.getFaith(fi) != 7 || !fi.getOrCreateTag().contains(FaithRules.FAITH_KEY)) return Result.fail("saving worshippers disturbed the Faith points");
        CompoundTag merged = gi.getOrCreateTag();
        merged.merge(fi.getOrCreateTag().copy());
        if (FaithSkill.getFaith(gi) != 7 || FaithWorship.worshippers(gi).size() != 2) return Result.fail("the evolution merge lost the Faith points or the worshippers");
        return Result.ok("modes 5/7, commands ok, list round trip ok, open state " + FaithWorship.openState());
    }
}
