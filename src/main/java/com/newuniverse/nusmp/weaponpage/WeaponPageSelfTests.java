package com.newuniverse.nusmp.weaponpage;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Bar;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Entry;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Opt;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.View;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: nothing of the Weapon page can be clicked by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these. {@code weaponpage.rules}:
 * the request checks refuse a wrong weapon, a wrong action for a weapon, a malformed or oversized argument, and accept the well formed ones; the checksum changes when anything the page shows changes; a view out
 * of bounds is refused. {@code weaponpage.world}: with a stand-in player who owns no weapon, the page is empty, an open sends once and an open that carries the matching checksum sends nothing, a second request
 * inside the gap is refused, a click on a weapon the player does not own is refused and changes nothing, bad requests are refused before anything is built, and the bookkeeping is dropped by {@code forget}.
 * Server safe: no client class. The world is left as it was found.
 */
public final class WeaponPageSelfTests {
    private WeaponPageSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("weaponpage.rules", s -> rules());
        SelfTests.register("weaponpage.world", WeaponPageSelfTests::world);
    }

    private static Result rules() {
        WeaponPageRules.Verdict ok = WeaponPageRules.Verdict.OK;
        if (WeaponPageRules.check("", WeaponPageRules.ACT_OPEN, "") != ok || WeaponPageRules.check("", WeaponPageRules.ACT_OPEN, "-12345") != ok) return Result.fail("a well formed open was refused");
        if (WeaponPageRules.check("hero", WeaponPageRules.ACT_OPEN, "") == ok || WeaponPageRules.check("", WeaponPageRules.ACT_OPEN, "abc") == ok) return Result.fail("a bad open was accepted");
        if (WeaponPageRules.check("hero", WeaponPageRules.ACT_FORM, "small_shield") != ok || WeaponPageRules.check("greed", WeaponPageRules.ACT_FORM, "7") != ok || WeaponPageRules.check("sloth", WeaponPageRules.ACT_FORM, "2") != ok)
            return Result.fail("a well formed form change was refused");
        if (WeaponPageRules.check("greed", WeaponPageRules.ACT_FORM, "8") == ok || WeaponPageRules.check("sloth", WeaponPageRules.ACT_FORM, "3") == ok || WeaponPageRules.check("hero", WeaponPageRules.ACT_FORM, "Bad Id") == ok)
            return Result.fail("a form past the end or a bad form id was accepted");
        if (WeaponPageRules.check("sloth", WeaponPageRules.ACT_LEARN, "x") == ok || WeaponPageRules.check("envy", WeaponPageRules.ACT_FEED, "minecraft:leather") == ok || WeaponPageRules.check("hero", WeaponPageRules.ACT_HALF, "") == ok)
            return Result.fail("an action of another weapon was accepted");
        if (WeaponPageRules.check("hero", WeaponPageRules.ACT_FEED, "minecraft:leather") != ok || WeaponPageRules.check("hero", WeaponPageRules.ACT_FEED, "../x") == ok || WeaponPageRules.check("greed", WeaponPageRules.ACT_HALF, "") != ok)
            return Result.fail("feed or half-starvation was checked wrongly");
        if (WeaponPageRules.check("nobody", WeaponPageRules.ACT_FORM, "0") == ok || WeaponPageRules.check("hero", 99, "") == ok || WeaponPageRules.check("hero", -1, "") == ok || WeaponPageRules.check("hero", WeaponPageRules.ACT_FORM, "a".repeat(200)) == ok)
            return Result.fail("an unknown weapon, an unknown action or an oversized argument was accepted");
        View a = sample(3), b = sample(4);
        if (a.sum() == b.sum() || a.sum() != sample(3).sum()) return Result.fail("the checksum does not follow the view");
        if (!a.bounded() || new View(5, a.entries()).bounded()) return Result.fail("the bounds of a view are wrong");
        if (a.option("hero", WeaponPageRules.ACT_FORM, "base") == null || a.option("hero", WeaponPageRules.ACT_FORM, "nothing") != null || a.option("greed", WeaponPageRules.ACT_HALF, "") != null) return Result.fail("option lookup is wrong");
        List<Bar> eight = new java.util.ArrayList<>(), nine = new java.util.ArrayList<>();                               // 4.63.0: the hero weapon may send up to MAX_BARS bars (the other forms' mastery), no more
        for (int i = 0; i < WeaponPageRules.MAX_BARS; i++) eight.add(new Bar("anime_multiverse.weaponpage.bar.formmore", "F" + i, i, 100, i));
        nine.addAll(eight);
        nine.add(new Bar("anime_multiverse.weaponpage.bar.formmore", "x", 1, 100, 1));
        Entry e0 = a.entries().get(0);
        if (!new View(0, List.of(new Entry(e0.id(), e0.name(), e0.grade(), e0.level(), e0.max(), e0.pct(), e0.form(), eight, e0.opts()))).bounded()
                || new View(0, List.of(new Entry(e0.id(), e0.name(), e0.grade(), e0.level(), e0.max(), e0.pct(), e0.form(), nine, e0.opts()))).bounded()) return Result.fail("the bar bound of an entry is wrong");
        return Result.ok("request checks, argument shapes, the checksum, the bounds and the option lookup hold");
    }

    private static View sample(int level) {
        Entry e = new Entry("hero", "Vassal Sword", "God Grade", level, 100, 40, "Base", List.of(new Bar("anime_multiverse.weaponpage.bar.level", "", level, 100, 40)),
                List.of(new Opt(WeaponPageRules.ACT_FORM, "base", "Base", WeaponPageRules.ST_CURRENT, "")));
        return new View(0, List.of(e));
    }

    private static Result world(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer p = SelfTests.standIn(level, "weaponpage_a", at.x, at.y, at.z);
        try {
            WeaponPageService.forget(p.getUUID());
            View v = WeaponPageService.build(p);
            if (!v.bounded()) return Result.fail("the built page is out of bounds");
            if (WeaponPageService.handle(p, "", 99, "") != WeaponPageService.Result.BAD_REQUEST) return Result.fail("a bad action was not refused");
            if (WeaponPageService.handle(p, "greed", WeaponPageRules.ACT_FORM, "99") != WeaponPageService.Result.BAD_REQUEST) return Result.fail("a form past the end was not refused");
            if (WeaponPageService.trackedPlayers() != 0) return Result.fail("a refused request left bookkeeping behind");
            if (WeaponPageService.handle(p, "", WeaponPageRules.ACT_OPEN, "") != WeaponPageService.Result.SENT) return Result.fail("an open with no copy did not send");
            if (WeaponPageService.handle(p, "", WeaponPageRules.ACT_OPEN, "") != WeaponPageService.Result.TOO_SOON) return Result.fail("a second open inside the gap was not refused");
            WeaponPageService.forget(p.getUUID());
            if (WeaponPageService.handle(p, "", WeaponPageRules.ACT_OPEN, Integer.toString(v.sum())) != WeaponPageService.Result.UNCHANGED) return Result.fail("an open with the current copy sent again");
            WeaponPageService.forget(p.getUUID());
            if (v.entry("greed") == null && WeaponPageService.handle(p, "greed", WeaponPageRules.ACT_FORM, "1") != WeaponPageService.Result.NOT_OFFERED) return Result.fail("a click on a weapon the player does not own was not refused");
            WeaponPageService.forget(p.getUUID());
            if (v.entry("hero") == null && WeaponPageService.handle(p, "hero", WeaponPageRules.ACT_LEARN, "base") != WeaponPageService.Result.NOT_OFFERED) return Result.fail("a learn with no hero weapon was not refused");
            WeaponPageService.forget(p.getUUID());
            if (WeaponPageService.trackedPlayers() != 0) return Result.fail("forget left bookkeeping behind");
            return Result.ok("bad requests refused first, one send per change, the gap holds, a weapon not owned cannot be clicked, bookkeeping dropped");
        } finally {
            WeaponPageService.forget(p.getUUID());
        }
    }
}
