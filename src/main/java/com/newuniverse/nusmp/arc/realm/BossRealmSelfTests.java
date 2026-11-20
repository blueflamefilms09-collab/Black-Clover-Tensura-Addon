package com.newuniverse.nusmp.arc.realm;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.arena.ArcArena;
import com.newuniverse.nusmp.arc.arena.ArenaShape;
import com.newuniverse.nusmp.arc.arena.ArenaShapes;
import com.newuniverse.nusmp.arc.arena.RealmShapes;
import com.newuniverse.nusmp.arc.goku.GokuBoss;
import com.newuniverse.nusmp.arc.magi.SinbadBoss;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the boss realm cannot be played by its author, so the CI server smoke test checks it in the real game ({@code /multiverse mega selftest}, keys {@code boss.realm.*}): the boss list (Goku
 * and Sinbad, nobody with a dungeon floor), the arenas and their choice by the setting, the settings' bounds, the arena dimension, and the door's refusal of nobody. It never moves a player (a stand-in
 * cannot change dimension), spawns nothing and sends nothing; the cost is a walk over the roster and two arena shapes' arrival lists.
 */
public final class BossRealmSelfTests {
    private BossRealmSelfTests() {}

    public static void register() {
        SelfTests.register("boss.realm.rules", BossRealmSelfTests::rules);
        SelfTests.register("boss.realm.shapes", BossRealmSelfTests::shapes);
        SelfTests.register("boss.realm.door", BossRealmSelfTests::door);
    }

    private static SelfTests.Result rules(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<String> realm = new ArrayList<>();
        for (ArcCharacter c : ArcRoster.bosses()) if (BossRealm.isRealmBoss(c)) realm.add(c.id());
        if (!realm.contains(GokuBoss.ID)) bad.add("goku_boss is not a realm boss");
        if (!realm.contains(SinbadBoss.ID)) bad.add("sinbad_boss is not a realm boss");
        for (String id : realm) {
            ArcCharacter c = ArcRoster.byId(id);
            if (c.floorMax() > 0) bad.add(id + " has a dungeon floor but is a realm boss");
        }
        for (ArcCharacter c : ArcRoster.bosses()) if (c.floorMax() > 0 && BossRealm.isRealmBoss(c)) bad.add(c.id() + " (a floor boss) is a realm boss");
        if (BossRealm.isRealmBoss(null)) bad.add("null is a realm boss");
        if (BossRealmConfig.partyCap() < 1 || BossRealmConfig.partyCap() > 16) bad.add("partyCap out of range");
        if (BossRealmConfig.maxActiveSites() < 1 || BossRealmConfig.maxActiveSites() > 16) bad.add("maxActiveSites out of range");
        if (BossRealmConfig.partyRadius() < 1.0 || BossRealmConfig.partyRadius() > 64.0) bad.add("partyRadius out of range");
        if (BossRealmConfig.graceTicks() < 0 || BossRealmConfig.returnTicks() < 0 || BossRealmConfig.timeoutTicks() < 0) bad.add("a time setting is negative");
        return bad.isEmpty() ? SelfTests.Result.ok("realm bosses: " + realm + "; the settings are in range (" + (BossRealmConfig.enabled() ? "on" : "off") + ")")
                : SelfTests.Result.fail(String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    private static SelfTests.Result shapes(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (RealmShapes.all().size() != 2) bad.add("expected two realm arenas, found " + RealmShapes.all().size());
        for (ArenaShape s : RealmShapes.all()) {
            if (ArenaShapes.byId(s.id()) != null) bad.add(s.id() + " is also in the story registry");
            if (RealmShapes.byId(s.id()) != s) bad.add(s.id() + " is not found by its id");
            if (s.arrivalSpots(ArenaShape.MAX_ARRIVALS).length < 1 || s.bossSpot().length != 2) bad.add(s.id() + " has no arrival or boss spot");
            if (!s.standable(s.bossSpot()[0], s.bossSpot()[1])) bad.add(s.id() + ": the boss cannot stand on its spot");
        }
        ArcCharacter goku = ArcRoster.byId(GokuBoss.ID), sinbad = ArcRoster.byId(SinbadBoss.ID);
        if (goku == null || sinbad == null) return SelfTests.Result.fail("Goku or Sinbad is not in the roster");
        ArenaShape g = ArenaShapes.forCharacter(goku), s = ArenaShapes.forCharacter(sinbad);
        if (BossRealmConfig.enabled()) {
            if (g != RealmShapes.TOURNAMENT_GROUNDS) bad.add("Goku is not fought on the tournament grounds");
            if (s != RealmShapes.SEVEN_SEAS_ISLE) bad.add("Sinbad is not fought on the Isle of the Seven Seas");
        } else if (RealmShapes.forCharacter(goku) != null || RealmShapes.forCharacter(sinbad) != null) {
            bad.add("the setting is off but a realm arena is still chosen");
        }
        return bad.isEmpty() ? SelfTests.Result.ok("two realm arenas, chosen only while the setting is on: " + g.id() + ", " + s.id())
                : SelfTests.Result.fail(String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    private static SelfTests.Result door(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (BossRealm.open(null, ArcRoster.byId(GokuBoss.ID))) bad.add("the door took a null player");
        if (server.getLevel(ArcArena.DIMENSION) == null) bad.add("the arena dimension is not loaded");
        if (BossRealm.presentWithGrace(0, "no_such_boss", 3, 100L) != 3) bad.add("a story arena's player count was changed");
        if (BossRealm.presentWithGrace(0, null, 2, 100L) != 2) bad.add("a missing character changed the count");
        if (BossRealm.clocks() > 256) bad.add("more clocks than sites: " + BossRealm.clocks());
        if (BossRealm.doorEntries() > 512) bad.add("the door table is over its cap: " + BossRealm.doorEntries());
        return bad.isEmpty() ? SelfTests.Result.ok("the door refuses nobody it should not, a story arena keeps its count, " + BossRealm.clocks() + " clocks, " + BossRealm.doorEntries() + " door entries")
                : SelfTests.Result.fail(String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }
}
