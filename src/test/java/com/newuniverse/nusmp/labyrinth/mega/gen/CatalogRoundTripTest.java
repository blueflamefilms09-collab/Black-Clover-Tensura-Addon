package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the catalog reader and the little JSON reader under it are what lets the assembler be tested against the REAL templates, so they must not lose or
 * invent anything on the way. The synthetic catalog is written out in the {@code catalog.json} format, read back, and compared socket by socket, marker by
 * marker and pool by pool; then the solver runs on the re-read catalog and must give exactly the plans it gave on the original (so the file path, the
 * object path and the determinism are all proved at once). The JSON reader is tested on what it must accept and what it must refuse.
 */
class CatalogRoundTripTest {

    @Test
    void theSyntheticCatalogSurvivesAWriteAndRead() {
        SyntheticCatalog orig = SyntheticCatalog.standard();
        CatalogSource back = CatalogSource.parse(orig.toJson());
        assertEquals(orig.shapes().size(), back.shapeCount());
        assertEquals(orig.pools().size(), back.poolCount());
        for (Shape a : orig.shapes().values()) {
            Shape b = back.shape(a.id);
            assertNotNull(b, a.id);
            assertEquals(a.sx, b.sx);
            assertEquals(a.sy, b.sy);
            assertEquals(a.sz, b.sz);
            assertEquals(a.kind, b.kind, a.id);
            assertEquals(a.sockets, b.sockets, a.id + " sockets");
            assertEquals(a.markers.size(), b.markers.size(), a.id + " markers");
            for (int i = 0; i < a.markers.size(); i++) {
                assertEquals(a.markers.get(i).cell(), b.markers.get(i).cell());
                assertEquals(a.markers.get(i).spec().kind(), b.markers.get(i).spec().kind());
                assertEquals(a.markers.get(i).spec().args(), b.markers.get(i).spec().args());
            }
            assertEquals(a.expansionSocket, b.expansionSocket);
            assertEquals(a.sealW, b.sealW);
        }
        for (PoolDef a : orig.pools().values()) {
            PoolDef b = back.pool(a.id());
            assertNotNull(b, a.id());
            assertEquals(a.emptyWeight(), b.emptyWeight());
            assertEquals(a.fallback(), b.fallback());
            assertEquals(a.entries().size(), b.entries().size());
            for (int i = 0; i < a.entries().size(); i++) {
                assertEquals(a.entries().get(i).shape(), b.entries().get(i).shape());
                assertEquals(a.entries().get(i).weight(), b.entries().get(i).weight());
            }
        }
    }

    @Test
    void plansFromTheRereadCatalogEqualPlansFromTheOriginal() {
        SyntheticCatalog orig = SyntheticCatalog.standard();
        CatalogSource back = CatalogSource.parse(orig.toJson());
        for (long seed = 0; seed < 5; seed++) {
            SectorSolver.Solved a = SectorSolver.solve(TestPlans.first(orig, seed), 24), b = SectorSolver.solve(TestPlans.first(back, seed), 24);
            assertTrue(a.ok() && b.ok());
            assertEquals(TestPlans.fingerprint(a.result()), TestPlans.fingerprint(b.result()), "seed " + seed);
        }
    }

    @Test
    void aMissingCatalogFileIsAnEmptySourceNotAnError() throws IOException {
        CatalogSource c = CatalogSource.load(Path.of("no/such/catalog.json"));
        assertTrue(c.isEmpty());
        assertNull(c.pool("nusmp:mega/side"));
        assertNull(c.shape("nusmp:mega/hall/x"));
    }

    @Test
    void theEmptyCatalogOfAnUnfinishedRepositoryParses() {
        CatalogSource c = CatalogSource.parse("{\n \"floorRow\": 8,\n \"height\": 48,\n \"module\": 9,\n \"pools\": {},\n \"templates\": {}\n}\n");
        assertTrue(c.isEmpty());
        assertEquals(0, c.poolCount());
    }

    @Test
    void theJsonReaderReadsWhatItShouldAndRefusesTheRest() {
        Map<String, Object> m = MiniJson.object(MiniJson.parse("{\"a\": [1, 2.5, -3, 1e2, \"x\\ny\\u0041\", true, false, null], \"b\": {}, \"c\": []}"));
        List<Object> a = MiniJson.array(m.get("a"));
        assertEquals(1L, a.get(0));
        assertEquals(2.5, a.get(1));
        assertEquals(-3L, a.get(2));
        assertEquals(100.0, a.get(3));
        assertEquals("x\nyA", a.get(4));
        assertEquals(Boolean.TRUE, a.get(5));
        assertEquals(Boolean.FALSE, a.get(6));
        assertNull(a.get(7));
        assertTrue(MiniJson.object(m.get("b")).isEmpty());
        assertTrue(MiniJson.array(m.get("c")).isEmpty());
        for (String bad : new String[] {"{\"a\": 1} x", "{\"a\" 1}", "[1, 2", "{\"a\": }", "", "tru"}) {
            boolean threw = false;
            try { MiniJson.parse(bad); } catch (RuntimeException e) { threw = true; }
            assertTrue(threw, "should refuse: " + bad);
        }
    }
}
