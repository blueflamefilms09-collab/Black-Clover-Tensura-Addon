package com.newuniverse.nusmp.star;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the registry of stars and panes is the hard bound of what this feature can ever hold on a public server (rule 11). The per-player cap, the server-wide cap with the OLDEST star of anybody evicted, the
 * expiry, the cleanup on logout / death / dimension change and the pane book (one per owner, eight on the server) are pure, so they are pinned here with many players at once (rule 9).
 */
class StarNodesTest {
    private static StarNodes.Node node(StarNodes n, String dim, double x, long expiry) { return new StarNodes.Node(UUID.randomUUID(), dim, x, 64, 0, n.nextSeq(), expiry); }

    @Test
    void theOldestStarOfAPlayerGoesFirstAtThePerPlayerCap() {
        StarNodes n = new StarNodes();
        UUID a = UUID.randomUUID();
        List<StarNodes.Node> placed = new ArrayList<>();
        List<StarNodes.Node> evicted = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            StarNodes.Node s = node(n, "o", i, 1000);
            placed.add(s);
            evicted.addAll(n.add(a, s, 6, 128));
        }
        assertEquals(6, n.count(a));
        assertEquals(6, n.total());
        assertEquals(placed.subList(0, 3), evicted, "the three oldest were evicted, in order");
        List<StarNodes.Node> live = n.live(a, "o", 10, null);
        assertEquals(placed.subList(3, 9), live, "newest last");
    }

    @Test
    void theServerCapEvictsTheOldestStarOfAnybody() {
        StarNodes n = new StarNodes();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        List<StarNodes.Node> all = new ArrayList<>();
        UUID[] owners = {a, b, c};
        List<StarNodes.Node> evicted = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            StarNodes.Node s = node(n, "o", i, 1000);
            all.add(s);
            evicted.addAll(n.add(owners[i % 3], s, 8, 12));
        }
        assertEquals(12, n.total(), "never more than the server cap");
        assertEquals(30 - 12, evicted.size());
        for (int i = 0; i < evicted.size(); i++) assertEquals(all.get(i), evicted.get(i), "global eviction order is placement order");
        assertEquals(12, n.count(a) + n.count(b) + n.count(c));
    }

    @Test
    void manyPlayersAtOnceNeverExceedTheCapsAndDoNotTouchEachOther() {
        StarNodes n = new StarNodes();
        int players = 40;
        UUID[] ids = new UUID[players];
        for (int p = 0; p < players; p++) ids[p] = UUID.randomUUID();
        for (int round = 0; round < 12; round++) for (int p = 0; p < players; p++) n.add(ids[p], node(n, "o", round, 5000), 6, 128);
        assertTrue(n.total() <= 128, "server cap: " + n.total());
        for (int p = 0; p < players; p++) assertTrue(n.count(ids[p]) <= 6);
        int before = n.count(ids[0]);
        n.clear(ids[1]);
        assertEquals(before, n.count(ids[0]), "clearing one player leaves the others");
        assertEquals(0, n.count(ids[1]));
    }

    @Test
    void expiredStarsAreDroppedAndOnlyTheCurrentDimensionIsListed() {
        StarNodes n = new StarNodes();
        UUID a = UUID.randomUUID();
        StarNodes.Node old = node(n, "o", 0, 50), keep = node(n, "o", 1, 500), nether = node(n, "nether", 2, 500);
        n.add(a, old, 6, 128);
        n.add(a, keep, 6, 128);
        n.add(a, nether, 6, 128);
        List<StarNodes.Node> expired = new ArrayList<>();
        List<StarNodes.Node> live = n.live(a, "o", 100, expired);
        assertEquals(List.of(keep), live);
        assertEquals(List.of(old), expired);
        assertEquals(2, n.count(a), "the star of the other dimension is still held");
        assertEquals(List.of(nether), n.live(a, "nether", 100, null));
        assertEquals(List.of(), n.live(a, "o", 1000, null), "everything expires");
        assertEquals(0, n.total());
    }

    @Test
    void logoutDeathAndDimensionChangeLeaveNothingBehind() {
        StarNodes n = new StarNodes();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        for (int i = 0; i < 4; i++) { n.add(a, node(n, "o", i, 999), 8, 128); n.add(a, node(n, "nether", i, 999), 8, 128); n.add(b, node(n, "o", i, 999), 8, 128); }
        assertEquals(4, n.clearDimension(a, "o").size());
        assertEquals(4, n.count(a), "only the nether stars are left");
        assertEquals(4, n.clear(a).size());
        assertEquals(0, n.count(a));
        assertEquals(4, n.total(), "b is untouched");
        assertEquals(4, n.clearAll().size());
        assertEquals(0, n.total());
        assertEquals(0, n.owners());
        assertEquals(List.of(), n.clear(UUID.randomUUID()), "clearing a stranger is harmless");
    }

    @Test
    void usingAStarRemovesOnlyThatStar() {
        StarNodes n = new StarNodes();
        UUID a = UUID.randomUUID();
        StarNodes.Node s1 = node(n, "o", 0, 99), s2 = node(n, "o", 1, 99);
        n.add(a, s1, 6, 128);
        n.add(a, s2, 6, 128);
        assertTrue(n.remove(a, s1.entity()));
        assertFalse(n.remove(a, s1.entity()), "a second removal finds nothing");
        assertEquals(List.of(s2), n.live(a, "o", 0, null));
        assertTrue(n.nextSeq() > s2.seq(), "placement numbers only rise");
    }

    @Test
    void thePaneBookKeepsOnePanePerOwnerAndEightOnTheServer() {
        StarPaneBook book = new StarPaneBook();
        StarMath.Pane geom = new StarMath.Pane(new StarMath.P(0, 3, 0), 0, 4, 3, 1.5);
        UUID owner = UUID.randomUUID();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        assertEquals(0, book.add(first, owner, "o", geom, 30f, 100).size());
        List<StarPaneBook.Pane> replaced = book.add(second, owner, "o", geom, 30f, 100);
        assertEquals(1, replaced.size());
        assertEquals(first, replaced.get(0).entity);
        assertEquals(1, book.size());
        for (int i = 0; i < 20; i++) book.add(UUID.randomUUID(), UUID.randomUUID(), "o", geom, 30f, 100);
        assertEquals(8, book.size(), "eight panes on the whole server");
        UUID e = UUID.randomUUID();
        book.add(e, UUID.randomUUID(), "o", geom, 10f, 100);
        assertFalse(book.damage(e, 4f));
        assertEquals(60, book.get(e).percent());
        assertTrue(book.damage(e, 7f), "used up");
        assertTrue(book.remove(e));
        assertNull(book.get(e));
        assertEquals(7, book.size(), "the new pane evicted the oldest, then it was removed");
        assertEquals(7, book.clearAll().size(), "everything left is returned");
        assertEquals(0, book.size());
    }

    @Test
    void thePaneBookSkipsFriendlyOwnersAndOtherDimensions() {
        StarPaneBook book = new StarPaneBook();
        StarMath.Pane geom = new StarMath.Pane(new StarMath.P(0, 3, 0), 0, 4, 3, 1.5);
        UUID mine = UUID.randomUUID(), theirs = UUID.randomUUID();
        book.add(UUID.randomUUID(), mine, "o", geom, 10f, 99);
        StarMath.P a = new StarMath.P(5, 3, 0), b = new StarMath.P(-5, 3, 0);
        assertNull(book.firstCrossed("o", a, b, owner -> owner.equals(mine)), "my own pane does not stop my shot");
        assertTrue(book.firstCrossed("o", a, b, owner -> false) != null);
        assertNull(book.firstCrossed("nether", a, b, owner -> false), "another dimension");
        book.add(UUID.randomUUID(), theirs, "o", geom, 10f, 99);
        assertEquals(theirs, book.firstCrossed("o", a, b, owner -> owner.equals(mine)).owner);
    }
}
