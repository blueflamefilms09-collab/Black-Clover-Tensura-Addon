package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skilltree.unique.LegacyIds;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The CI self-tests of the overpowered wishes (prefix {@code dbwish.}, docs/dragon_balls_overpowered.md). They need no other mod:
 * <ul>
 *   <li>{@code dbwish.rules}: the settings read back inside their ranges and the pure rules agree with them (tables, caps, the item blocklist, the scatter guard, the weapon week, the skill tiers).</li>
 *   <li>{@code dbwish.registry}: the race, skill, item and relic lists are read from the LIVE registries and are not empty; every race name and every skill name resolves back to itself; no removed or managed skill is
 *       offered; the message carries the live counts per tier (so the CI log shows how many skills every loaded mod added); the spoken vocabulary holds the words and stays under 2000.</li>
 *   <li>{@code dbwish.apply}: a stand-in player becomes another race by wish, learns a skill by wish (a second try is refused as already known) and is promoted by the rank wish; nothing is used up by a refusal.</li>
 *   <li>{@code dbwish.scatter}: the anime scatter: the first granted wish scatters the set once (generation up, rows pending, stones, lock), the remaining wishes do not scatter again and the end of the ritual neither.</li>
 * </ul>
 */
public final class DbWishSelfTests {
    private DbWishSelfTests() {}

    public static void register() {
        SelfTests.register("dbwish.rules", DbWishSelfTests::rules);
        SelfTests.register("dbwish.registry", DbWishSelfTests::registry);
        SelfTests.register("dbwish.apply", DbWishSelfTests::apply);
        SelfTests.register("dbwish.scatter", DbWishSelfTests::scatter);
    }

    private static Result rules(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (BallSet s : BallSet.values()) {
            int w = DbConfig.wishes(s);
            int max = DbConfig.overpowered() ? WishRules.MAX_WISHES_OVERPOWERED : WishRules.MAX_WISHES;
            if (w < 1 || w > max) bad.add("wishes " + s + "=" + w);
            if (DbConfig.skillsPerWish(s) < 1 || DbConfig.skillsPerWish(s) > 5) bad.add("skills per wish");
            if (SkillRules.perWish(BallSet.EARTH, 5) != 1) bad.add("Earth teaches more than one skill");
        }
        if (DbConfig.powerCapNow() < 1 || DbConfig.powerCapNow() > WishRules.POWER_CAP_MAX) bad.add("power cap " + DbConfig.powerCapNow());
        if (DbConfig.lockMinutesNow() < 0) bad.add("lock minutes");
        if (WishRules.forSet(BallSet.EARTH, false).size() != WishRules.forSet(BallSet.EARTH).size()) bad.add("the old table changed");
        if (WishRules.match(BallSet.SUPER, "I wish to become a high orc", true) == null || WishRules.match(BallSet.SUPER, "I wish to become a high orc", true).kind() != WishRules.Kind.RACE) bad.add("the race wish is not matched");
        if (WishRules.match(BallSet.SUPER, "I wish to become a saiyan", true).kind() != WishRules.Kind.SAIYAN) bad.add("the Saiyan wish lost to the race wish");
        if (WishRules.match(BallSet.EARTH, "change my rate", true) == null || WishRules.match(BallSet.EARTH, "change my rate", true).kind() != WishRules.Kind.RANK) bad.add("the rank wish is not matched");
        if (WishRules.match(BallSet.SUPER, "I wish to become a high orc", false) != null && WishRules.match(BallSet.SUPER, "I wish to become a high orc", false).kind() == WishRules.Kind.RACE) bad.add("the race wish exists in the old mode");
        if (!WishRules.forbidden("I wish to be immortal") || !WishRules.forbidden("mind control")) bad.add("a forbidden wish is not refused");
        if (!WishRules.itemBlocked("minecraft:command_block") || !WishRules.itemBlocked("minecraft:barrier") || !WishRules.itemBlocked("anime_multiverse:dragon_ball_earth_1") || WishRules.itemBlocked("minecraft:diamond")) bad.add("the item blocklist is wrong");
        if (WishRules.scatterNow(true, 1, true) || WishRules.scatterNow(false, 1, false) || !WishRules.scatterNow(true, 1, false) || WishRules.scatterNow(true, 0, false)) bad.add("the scatter guard is wrong");
        if (WishRules.relicGradeAllowed(6, false) || !WishRules.relicGradeAllowed(5, false) || !WishRules.relicGradeAllowed(7, true)) bad.add("the relic grade rule is wrong");
        if (SkillRules.tierOf("unique") != SkillRules.Tier.UNIQUE || SkillRules.tierOf("intrinsic") != null) bad.add("the skill tiers are wrong");
        if (SkillRules.ceiling(BallSet.EARTH, SkillRules.Tier.EXTRA, true).ordinal() >= SkillRules.ceiling(BallSet.SUPER, SkillRules.Tier.EXTRA, true).ordinal()) bad.add("Earth is not below Super");
        return bad.isEmpty() ? Result.ok("overpowered=" + DbConfig.overpowered() + ": settings in range, wish table, caps, item blocklist, scatter guard, relic grades and skill tiers hold") : Result.fail(String.join("; ", bad));
    }

    private static boolean resolvesToItself(List<WishNames.Entry> list, WishNames.Entry e) {
        WishNames.Result r = WishNames.resolve(list, e.name());
        return (r.status() == WishNames.Status.OK && r.first().id().equals(e.id())) || (r.status() == WishNames.Status.AMBIGUOUS && r.entries().stream().anyMatch(x -> x.id().equals(e.id())));
    }

    private static Result registry(MinecraftServer server) {
        WishCatalog.invalidate();
        List<String> bad = new ArrayList<>();
        List<WishNames.Entry> races = WishCatalog.races(), skills = WishCatalog.allSkills(), items = WishCatalog.items(), relics = WishCatalog.relics(true);
        if (races.size() < 2) bad.add("the race list is empty (" + races.size() + ")");
        if (skills.isEmpty()) bad.add("the skill list is empty");
        if (items.isEmpty()) bad.add("the item list is empty");
        int unresolved = 0;
        for (WishNames.Entry e : races) if (!resolvesToItself(races, e) && unresolved++ < 5) bad.add("race " + e.name() + " does not resolve back to itself");
        for (WishNames.Entry e : skills) if (!resolvesToItself(skills, e) && unresolved++ < 10) bad.add("skill " + e.name() + " does not resolve back to itself");
        if (races.stream().noneMatch(e -> e.id().equals(WishCatalog.DEVIL_ID))) bad.add("the Devil race is missing");
        ResourceLocation human = ResourceLocation.fromNamespaceAndPath("tensura", "human");
        if (RaceAPI.getRaceRegistry().get(human) != null && races.stream().noneMatch(e -> e.id().equals(human.toString()))) bad.add("tensura:human is in the registry but not in the list");
        for (String path : LegacyIds.REMOVED_SKILLS) {
            String id = "anime_multiverse:" + path;
            if (skills.stream().anyMatch(e -> e.id().equals(id))) bad.add("the removed skill " + id + " is offered");
        }
        for (WishNames.Entry e : skills) {
            if (SkillAPI.getSkillRegistry().get(ResourceLocation.parse(e.id())) == null) bad.add("skill " + e.id() + " is not in the registry");
            if (SkillRules.Tier.byName(e.tag()) == null) bad.add("skill " + e.id() + " has no tier");
        }
        List<String> vocab = WishRules.spokenVocabulary();
        if (vocab.isEmpty() || vocab.size() > 2000 || !vocab.contains("race") || !vocab.contains("skill") || vocab.size() != vocab.stream().distinct().count()) bad.add("the spoken vocabulary is wrong (" + vocab.size() + ")");
        var counts = WishCatalog.skillCounts();
        String msg = races.size() + " races, " + skills.size() + " skills (resistance " + counts.getOrDefault(SkillRules.Tier.RESISTANCE, 0) + ", common " + counts.getOrDefault(SkillRules.Tier.COMMON, 0)
                + ", extra " + counts.getOrDefault(SkillRules.Tier.EXTRA, 0) + ", unique " + counts.getOrDefault(SkillRules.Tier.UNIQUE, 0) + ", ultimate " + counts.getOrDefault(SkillRules.Tier.ULTIMATE, 0)
                + "), " + items.size() + " items, " + relics.size() + " relics read from the live registries; " + vocab.size() + " spoken words; Earth reaches " + DbConfig.skillCeiling(BallSet.EARTH) + ", Super "
                + DbConfig.skillCeiling(BallSet.SUPER) + " (" + WishCatalog.skillsFor(BallSet.EARTH).size() + " / " + WishCatalog.skillsFor(BallSet.SUPER).size() + " skills)";
        return bad.isEmpty() ? Result.ok(msg) : Result.fail(String.join("; ", bad) + " | " + msg);
    }

    private static Result apply(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        FakePlayer p = SelfTests.standIn(ow, "dbwish_apply", 8.5, 100, 8.5);
        List<String> bad = new ArrayList<>();
        var skills = com.newuniverse.nusmp.dragonballs.WishCatalog.skillsFor(BallSet.SUPER);
        WishNames.Entry pick = null;
        var have = SkillAPI.getSkillsFrom(p);
        for (WishNames.Entry e : skills) {
            ResourceLocation id = ResourceLocation.parse(e.id());
            if ("COMMON".equals(e.tag()) && have.getSkill(id).isEmpty()) { pick = e; break; }
        }
        try {
            if (pick != null) {
                DbWishes.Outcome o = DbWishesPlus.skill(p, BallSet.SUPER, "i wish for the skill " + pick.name(), null);
                if (!o.ok()) bad.add("the skill wish for " + pick.name() + " was refused: " + o.msg().getString());
                else if (have.getSkill(ResourceLocation.parse(pick.id())).isEmpty()) bad.add("the skill was not learned");
                if (DbWishesPlus.skill(p, BallSet.SUPER, "skill " + pick.name(), null).ok()) bad.add("a skill already known was granted again");
            }
            if (DbWishesPlus.skill(p, BallSet.SUPER, "skill zzzqqq nothing", null).ok()) bad.add("an unknown skill was granted");
            if (DbWishesPlus.race(p, BallSet.SUPER, "race zzzqqq nothing").ok()) bad.add("an unknown race was granted");
            DbWishesPlus.item(p, BallSet.SUPER, "item command block");
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (!p.getInventory().getItem(i).isEmpty() && WishRules.itemBlocked(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.getInventory().getItem(i).getItem()).toString())) bad.add("an operator item was granted");
            ResourceLocation elf = ResourceLocation.fromNamespaceAndPath("tensura", "elf"), human = ResourceLocation.fromNamespaceAndPath("tensura", "human");
            if (RaceAPI.getRaceRegistry().get(elf) != null && RaceAPI.getRaceFrom(p).getRace().isPresent()) {
                RaceAPI.getRaceFrom(p).setRace(human, false);
                DbWishes.Outcome o = DbWishesPlus.race(p, BallSet.SUPER, "i wish to become an elf");
                var now = RaceAPI.getRaceFrom(p).getRace();
                if (!o.ok() || now.isEmpty() || !now.get().getRaceId().equals(elf)) bad.add("the race wish did not make an elf: " + o.msg().getString());
                if (DbWishesPlus.race(p, BallSet.SUPER, "race elf").ok()) bad.add("a race the player already has was granted again");
            }
            int step0 = com.newuniverse.nusmp.multiverse.MultiverseProfile.step(p);
            if (step0 < WishRules.rankCeiling(BallSet.EARTH)) {
                if (!DbWishesPlus.rank(p, BallSet.EARTH, "rank").ok()) bad.add("the rank wish was refused");
                else if (com.newuniverse.nusmp.multiverse.MultiverseProfile.step(p) <= step0) bad.add("the rank did not rise");
            }
        } finally {
            if (pick != null) have.forgetSkill(ResourceLocation.parse(pick.id()));
            RaceAPI.getRaceFrom(p).setRace(ResourceLocation.fromNamespaceAndPath("tensura", "human"), false);
            com.newuniverse.nusmp.multiverse.MultiverseProfile.setRank(p, com.newuniverse.nusmp.multiverse.KnightRank.JUNIOR, 5);
            p.getInventory().clearContent();
        }
        return bad.isEmpty() ? Result.ok("skill wish learned and refused when known, unknown names and operator items refused, race wish changed the race, rank wish promoted") : Result.fail(String.join("; ", bad));
    }

    private static Result scatter(MinecraftServer server) {
        if (!DbConfig.overpowered()) return Result.ok("dragonBalls.overpowered is off on this server: the old scatter timing, nothing to check");
        List<String> bad = new ArrayList<>();
        ServerLevel ow = server.overworld();
        ow.getChunk(0, 0);                                                                        // a test may load a chunk; the game code never does
        BallSet set = BallSet.SUPER;
        int o = set.ordinal(), n = DbConfig.count();
        DbData d = DbManager.data(server);
        if (DbSession.of(set) != null) DbSession.of(set).end(server, "self-test clean-up", false);
        DbManager.setLock(server, set, 0);
        DbManager.scatter(server, set, false);
        FakePlayer p = SelfTests.standIn(ow, "dbwish_scatter", 8.5, 100, 8.5);
        p.getInventory().clearContent();
        p.getPersistentData().remove(DbWishes.KEY);
        DbWishes.applyPower(p);
        for (int star = 1; star <= n; star++) p.getInventory().add(DragonBallItem.make(set, star, d.gen[o], false));
        net.minecraft.network.chat.Component err = DbManager.trySummon(p, set);
        if (err != null) return Result.fail("all balls did not summon: " + err.getString());
        DbSession s = DbSession.of(set);
        if (s == null) return Result.fail("no session after the summoning");
        int gen0 = d.gen[o];
        if (s.scattered || !d.active[o]) bad.add("the ritual starts scattered or not active");
        s.wish(p, "I wish to be immortal");                                                       // refused: no scatter
        if (s.scattered || d.gen[o] != gen0) bad.add("a refused wish scattered the balls");
        s.wish(p, "power up");
        if (!s.scattered) bad.add("the first granted wish did not scatter the set");
        if (d.gen[o] != gen0 + 1) bad.add("the generation did not rise exactly once (" + gen0 + " -> " + d.gen[o] + ")");
        if (d.active[o]) bad.add("the ritual is still marked active after the scatter (a restart would scatter twice)");
        int rows = 0;
        for (int star = 1; star <= n; star++) {
            DbData.Ball b = d.balls[o][star - 1];
            if (b != null && b.pending && b.stone == (DbConfig.lockMinutesNow() > 0)) rows++;
        }
        if (rows != n) bad.add("the set was not scattered as " + n + " pending rows (" + rows + ")");
        if (DbConfig.lockMinutesNow() > 0 && !DbManager.locked(server, set)) bad.add("the set is not locked after the first wish");
        if (DbManager.current(server, DragonBallItem.make(set, 1, gen0, false), set)) bad.add("a ball of the old generation is still current");
        int gen1 = d.gen[o];
        if (DbSession.of(set) == s) {                                                             // wishes are left: a second wish must not scatter again
            s.wish(p, "power up");
            if (d.gen[o] != gen1) bad.add("a second wish scattered the set again");
            s.end(server, "self-test clean-up", true);
        }
        if (d.gen[o] != gen1) bad.add("the end of the ritual scattered the set a second time");
        if (d.active[o]) bad.add("the set is marked active after the ritual");
        p.getInventory().clearContent();
        p.getPersistentData().remove(DbWishes.KEY);
        DbWishes.applyPower(p);
        DbManager.setLock(server, set, 0);
        return bad.isEmpty() ? Result.ok("the first granted wish scattered the " + n + " balls once (generation up, stones, lock, not active), later wishes and the end of the ritual did not scatter again") : Result.fail(String.join("; ", bad));
    }
}
