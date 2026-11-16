package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The CI self-tests of the Dragon Balls (run by the server smoke test with a stand-in player; the smoke list entry is the prefix {@code dragonballs}):
 * <ul>
 *   <li>{@code dragonballs.rules}: every ball item (2 sets x 8 stars), the Dragon Radar and the Shenron entity are registered; the phrase matcher, the wish caps, the forbidden wishes, the lock maths and the
 *       scatter separation hold; the settings read back inside their ranges.</li>
 *   <li>{@code dragonballs.scatter}: a scatter makes exactly {@code dragonBalls.count} rows, all waiting for a chunk and apart from each other, bumps the generation (an old ball turns invalid); the radar answers; a lost ball gets a new place.</li>
 *   <li>{@code dragonballs.ritual}: all balls but one do not summon, all of them do; the balls are taken, one ritual per set, a second summoner is refused; a wish is validated and applied (power level, cap), the
 *       ritual ends with its wishes, the balls scatter as stones and the set is locked; a locked set refuses; the admin unlock opens it.</li>
 * </ul>
 */
public final class DbSelfTests {
    private DbSelfTests() {}

    public static void register() {
        SelfTests.register("dragonballs.rules", DbSelfTests::rules);
        SelfTests.register("dragonballs.scatter", DbSelfTests::scatter);
        SelfTests.register("dragonballs.ritual", DbSelfTests::ritual);
    }

    private static Result rules(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (BallSet s : BallSet.values())
            for (int star = 1; star <= DbRules.MAX_COUNT; star++)
                if (BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("anime_multiverse", s.itemId(star))).isEmpty()) bad.add("no item " + s.itemId(star));
        if (BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "dragon_radar")).isEmpty()) bad.add("no dragon_radar");
        if (BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "shenron")).isEmpty()) bad.add("no shenron entity");
        int count = DbConfig.count();
        if (count < DbRules.MIN_COUNT || count > DbRules.MAX_COUNT) bad.add("count " + count);
        for (BallSet s : BallSet.values()) {
            int w = DbConfig.wishes(s);
            if (w < 1 || w > WishRules.MAX_WISHES) bad.add("wishes " + s + "=" + w);
            if (!DbRules.matchesAny("Everyone! " + s.defaultPhrases().get(0).toUpperCase() + "!!", s.defaultPhrases())) bad.add("the first phrase of " + s + " does not match itself");
        }
        if (!DbRules.contains("Takkaradoppo, Pakkoradoppo -- YAIKANAAN!", "takkaradoppo pakkoradoppo yaikanaan")) bad.add("the Namekian phrase does not match through punctuation");
        if (WishRules.addable(WishRules.POWER_CAP_MAX, 2, WishRules.POWER_CAP_MAX) != 0) bad.add("power is not capped");
        if (!WishRules.forbidden("I wish for immortality") || !WishRules.forbidden("destroy the world")) bad.add("a forbidden wish is not refused");
        WishRules.Wish pole = WishRules.match(BallSet.SUPER, "the power pole");
        if (WishRules.match(BallSet.EARTH, "please revive me") == null || pole == null || pole.kind() != WishRules.Kind.WEAPON || WishRules.match(BallSet.EARTH, "the ruyi") != null) bad.add("the wish matcher is wrong");
        if (DbRules.locked(DbRules.lockUntil(1000L, 60), 1000L + 59 * 60_000L) == false || DbRules.locked(DbRules.lockUntil(1000L, 60), 1000L + 61 * 60_000L)) bad.add("the lock maths is wrong");
        List<int[]> pts = DbRules.scatter(new Random(7), count, 0, 0, 300, 3000, 500);
        for (int i = 0; i < pts.size(); i++)
            for (int j = i + 1; j < pts.size(); j++) {
                double dx = pts.get(i)[0] - pts.get(j)[0], dz = pts.get(i)[1] - pts.get(j)[1];
                if (dx * dx + dz * dz < 500.0 * 500.0 * 0.99) bad.add("two scattered balls are closer than 500");
            }
        return bad.isEmpty() ? Result.ok("2 sets x 8 stars + radar + Shenron registered; count=" + count + "; phrases, caps, forbidden wishes, lock and scatter hold") : Result.fail(String.join("; ", bad));
    }

    private static Result scatter(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        DbData d = DbManager.data(server);
        BallSet set = BallSet.SUPER;
        int o = set.ordinal(), n = DbConfig.count();
        if (DbSession.of(set) != null) return Result.fail("a ritual of the super set is running");
        int g0 = d.gen[o];
        net.minecraft.world.item.ItemStack old = DragonBallItem.make(set, 1, g0, false);
        DbManager.scatter(server, set, false);
        if (d.gen[o] != g0 + 1) bad.add("the generation did not rise");
        if (DbManager.current(server, old, set)) bad.add("a ball of the old scatter is still current");
        if (!DbManager.current(server, DragonBallItem.make(set, 1, d.gen[o], false), set)) bad.add("a fresh ball is not current");
        if (!DbManager.current(server, DragonBallItem.make(set, 1, -1, false), set)) bad.add("a creative ball (no generation) is refused");
        int rows = 0;
        for (int star = 1; star <= DbRules.MAX_COUNT; star++) {
            DbData.Ball b = d.balls[o][star - 1];
            if (star <= n) {
                if (b == null || !b.pending) bad.add("ball " + star + " has no pending place");
                else rows++;
            } else if (b != null) bad.add("ball " + star + " exists beyond the count");
        }
        if (rows != n) bad.add(rows + " rows, expected " + n);
        for (int i = 1; i <= n; i++)
            for (int j = i + 1; j <= n; j++) {
                DbData.Ball a = d.balls[o][i - 1], b = d.balls[o][j - 1];
                if (a == null || b == null) continue;
                if (Math.hypot(a.x - b.x, a.z - b.z) < Math.min(DbConfig.scatterSeparation(), 100) * 0.99) bad.add("balls " + i + " and " + j + " are on top of each other");
            }
        ServerLevel ow = server.overworld();
        FakePlayer p = SelfTests.standIn(ow, "dragonballs_radar", 0.5, 100, 0.5);
        Component r = DbManager.radar(p);
        if (r == null || r.getString().isEmpty()) bad.add("the radar answered nothing");
        DbManager.relocate(server, set, 1);
        if (!d.balls[o][0].pending) bad.add("a lost ball did not get a new place");
        if (!"N".equals(DbManager.direction(0, -10)) || !"E".equals(DbManager.direction(10, 0)) || !"S".equals(DbManager.direction(0, 10))) bad.add("the radar compass is wrong");
        return bad.isEmpty() ? Result.ok("scatter made " + n + " pending rows, bumped the generation, the radar and the void return work") : Result.fail(String.join("; ", bad));
    }

    private static Result ritual(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        ServerLevel ow = server.overworld();
        ow.getChunk(0, 0);                                                                        // a test may load a chunk; the game code never does
        BallSet set = BallSet.EARTH;
        int o = set.ordinal(), n = DbConfig.count();
        DbData d = DbManager.data(server);
        if (DbSession.of(set) != null) DbSession.of(set).end(server, "self-test clean-up", false);
        DbManager.setLock(server, set, 0);
        DbManager.scatter(server, set, false);
        FakePlayer p = SelfTests.standIn(ow, "dragonballs_summoner", 8.5, 100, 8.5), q = SelfTests.standIn(ow, "dragonballs_other", 12.5, 100, 8.5);
        p.getInventory().clearContent();
        p.getPersistentData().remove(DbWishes.KEY);
        DbWishes.applyPower(p);
        float hp0 = p.getMaxHealth();

        for (int star = 1; star < n; star++) p.getInventory().add(DragonBallItem.make(set, star, d.gen[o], false));
        if (DbManager.trySummon(p, set) == null) bad.add("a ritual started with one ball missing");
        if (DbSession.of(set) != null) bad.add("a session exists after a refused summoning");
        p.getInventory().add(DragonBallItem.make(set, n, d.gen[o], false));
        Component err = DbManager.trySummon(p, set);
        if (err != null) return Result.fail("all " + n + " balls did not summon: " + err.getString());
        DbSession s = DbSession.of(set);
        if (s == null) return Result.fail("no session after the summoning");
        if (!d.active[o]) bad.add("the ritual is not marked active (a crash would lose the balls)");
        if (s.dragon == null || ow.getEntity(s.dragon) == null) bad.add("Shenron is not in the world");
        int left = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).getItem() instanceof DragonBallItem) left++;
        if (left != 0) bad.add(left + " balls were not taken");
        q.getInventory().clearContent();
        for (int star = 1; star <= n; star++) q.getInventory().add(DragonBallItem.make(set, star, d.gen[o], false));
        if (DbManager.trySummon(q, set) == null) bad.add("a second ritual of the same set was allowed");
        q.getInventory().clearContent();

        s.wish(p, "I wish to be immortal");                                                       // refused, not used up
        if (s.wishesGranted != 0 || DbSession.of(set) != s) bad.add("a forbidden wish was granted");
        s.wish(p, "power up");
        if (DbWishes.powerLevels(p) != WishRules.powerLevels(set)) bad.add("the power wish did not add its level (" + DbWishes.powerLevels(p) + ")");
        if (p.getMaxHealth() < hp0 + WishRules.healthPerLevel() * WishRules.powerLevels(set) - 0.01f) bad.add("the health bonus is missing: " + p.getMaxHealth() + " from " + hp0);
        if (DbConfig.wishes(set) <= 1) {
            if (DbSession.of(set) != null) bad.add("the ritual did not end with its last wish");
            if (DbConfig.lockMinutes() > 0 && !DbManager.locked(server, set)) bad.add("the set is not locked after the wish");
            if (d.active[o]) bad.add("the ritual is still marked active");
            int stones = 0;
            for (int star = 1; star <= n; star++) {
                DbData.Ball b = d.balls[o][star - 1];
                if (b != null && b.pending && (b.stone == (DbConfig.lockMinutes() > 0))) stones++;
            }
            if (stones != n) bad.add("the balls did not scatter as stones (" + stones + "/" + n + ")");
            if (DbConfig.lockMinutes() > 0) {
                for (int star = 1; star <= n; star++) p.getInventory().add(DragonBallItem.make(set, star, d.gen[o], false));
                Component locked = DbManager.trySummon(p, set);
                if (locked == null) bad.add("a locked set summoned");
                DbManager.setLock(server, set, 0);
                if (DbManager.locked(server, set)) bad.add("the admin unlock did not open the set");
            }
        } else if (DbSession.of(set) != null) {
            DbSession.of(set).end(server, "self-test clean-up", false);
        }
        // clean up: the stand-in goes back to a plain player, the set is open and freshly scattered
        p.getInventory().clearContent();
        p.getPersistentData().remove(DbWishes.KEY);
        DbWishes.applyPower(p);
        DbManager.setLock(server, set, 0);
        return bad.isEmpty() ? Result.ok("N-1 balls refused, " + n + " balls summon Shenron, one ritual per set, forbidden wish refused, power wish applied and capped, the ritual ended with its wish, stones + lock, unlock") : Result.fail(String.join("; ", bad));
    }
}
