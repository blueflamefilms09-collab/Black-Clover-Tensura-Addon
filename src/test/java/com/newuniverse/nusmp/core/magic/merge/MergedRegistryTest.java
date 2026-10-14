package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.core.magic.profile.MagicAudit;
import com.newuniverse.nusmp.core.magic.profile.MagicProfile;
import com.newuniverse.nusmp.core.magic.profile.MagicProfiles;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The registry that casting, profiles and the client read, and the profile built from the parents. Uses the real MagicType enum. */
class MergedRegistryTest {
    private static MergeStore store(String name, String... parents) {
        MergeStore s = new MergeStore();
        MergeStore.Outcome o = s.create(List.of(parents), name, n -> MagicType.lookup(n) != null && !MagicType.lookup(n).restricted(), Set.of(), null);
        assertEquals(MergeStore.Status.CREATED, o.status(), String.valueOf(o.check()));
        return s;
    }

    @Test
    void publishingRegistersTheProfileAndClearingRemovesIt() {
        MergedRegistry.clear();
        try {
            MergedRegistry.replace(store(null, "WATER", "FLAME"));
            assertEquals(1, MergedRegistry.size());
            MergedDef def = MergedRegistry.byId("MERGED:Flame+Water");
            assertNotNull(def);
            assertSame(def, MergedRegistry.bySlot(def.slot()));
            assertSame(def, MergedRegistry.byKey(def.key()));
            assertTrue(MagicProfiles.hasOverride("merged:flame+water"));
            MagicProfile p = MagicProfiles.of("merged:flame+water");
            assertTrue(p.isMerged());
            assertEquals(List.of("FLAME", "WATER"), p.parentIds());
            assertEquals("Flame-Water Magic", p.displayName());
            assertFalse(p.restricted());
            MergedRegistry.clear();
            assertEquals(0, MergedRegistry.size());
            assertFalse(MagicProfiles.hasOverride("merged:flame+water"), "the profile goes with the definition");
            assertNotNull(MagicProfiles.of("merged:flame+water"), "and an id nobody knows still resolves a profile");
        } finally {
            MergedRegistry.clear();
        }
    }

    @Test
    void theMergedProfileIsBuiltFromTheParentsThroughTheFramework() {
        MergedRegistry.clear();
        try {
            MergedRegistry.replace(store(null, "WATER", "FLAME"));
            MagicProfile p = MagicProfiles.of("merged:flame+water");
            MagicProfile flame = MagicProfiles.of(MagicType.FLAME), water = MagicProfiles.of(MagicType.WATER);
            assertEquals(flame.soulType(), p.soulType(), "the dominant (first in canonical order) parent's soul");
            assertEquals(MagicType.HitEffect.BURN, p.hit(), "the dominant parent's hit effect");
            assertEquals(MagicType.HitEffect.PUSH, p.secondaryHit(), "the other parent's rides along");
            assertEquals(0xFF, p.color() >>> 24);
            assertNotEquals(flame.color(), p.color());
            assertNotEquals(water.color(), p.color());
            assertSame(com.newuniverse.nusmp.vfx.VfxShape.MERGED_CAST, p.castShape());
            assertSame(com.newuniverse.nusmp.vfx.VfxShape.MERGED_FIELD, p.fieldShape());
            assertSame(com.newuniverse.nusmp.vfx.VfxShape.MERGED_BURST, p.burstShape());
            assertSame(com.newuniverse.nusmp.aura.Aura.MERGED, p.auraStyle());
            assertTrue(p.safeCostPercent() >= Math.max(flame.safeCostPercent(), water.safeCostPercent()) * 1.15 - 1e-9);
            assertTrue(p.safeCooldownTicks() >= Math.max(flame.safeCooldownTicks(), water.safeCooldownTicks()));
        } finally {
            MergedRegistry.clear();
        }
    }

    @Test
    void theVersionOfASlotChangesWithItsDefinition() {
        MergedRegistry.clear();
        try {
            assertEquals(-1, MergedRegistry.version(0));
            MergedRegistry.replace(store(null, "FLAME", "WATER"));
            int v1 = MergedRegistry.version(0);
            assertNotEquals(-1, v1);
            assertEquals(v1, MergedRegistry.version(0), "stable while nothing changes");
            MergedRegistry.replace(store(null, "FLAME", "WIND"));
            assertNotEquals(v1, MergedRegistry.version(0), "a different magic in the slot is a different version");
        } finally {
            MergedRegistry.clear();
        }
    }

    @Test
    void theBlendIsCachedAndFollowsTheDefinition() {
        MergedRegistry.clear();
        try {
            MergedRegistry.replace(store(null, "FLAME", "WATER"));
            MergedDef def = MergedRegistry.byId("merged:flame+water");
            assertSame(MergedRegistry.blend(def), MergedRegistry.blend(def));
            assertEquals("FLAME", MergedRegistry.blend(def).soulType);
        } finally {
            MergedRegistry.clear();
        }
    }

    @Test
    void auditedMergedProfilesAreComplete() {
        MergedRegistry.clear();
        try {
            MergedRegistry.replace(store(null, "FLAME", "WATER"));
            MagicAudit.Report r = MagicAudit.run(MagicProfiles.registered(), null, 0);
            assertTrue(r.clean(), r.lines().toString());
            assertEquals(1, r.checked());
        } finally {
            MergedRegistry.clear();
        }
    }

    @Test
    void meshNormalisationCentresScalesAndFlips() {
        // one quad (a 2 x 4 wall on x/y, normal +z): x 0..2, y 0..4
        float[] v = {
                0, 0, 1, 0, 1, 0, 0, 1,
                2, 0, 1, 1, 1, 0, 0, 1,
                2, 4, 1, 1, 0, 0, 0, 1,
                0, 4, 1, 0, 0, 0, 0, 1};
        float[] n = MeshMath.normalise(v);
        assertEquals(v.length, n.length);
        // the largest side (4) becomes 1: x spans 0.5, y spans 1; centred on the origin
        float minX = 9, maxX = -9, minY = 9, maxY = -9;
        for (int i = 0; i < 4; i++) { minX = Math.min(minX, n[i * 8]); maxX = Math.max(maxX, n[i * 8]); minY = Math.min(minY, n[i * 8 + 1]); maxY = Math.max(maxY, n[i * 8 + 1]); }
        assertEquals(-0.25f, minX, 1e-6f);
        assertEquals(0.25f, maxX, 1e-6f);
        assertEquals(-0.5f, minY, 1e-6f);
        assertEquals(0.5f, maxY, 1e-6f);
        // y is flipped: the vertex that was at the top (y = 4, index 2) is now at the bottom
        assertEquals(-0.5f, n[2 * 8 + 1], 1e-6f);
        // winding reversed: the old vertex 1 (x = 2, y = 0) is now the last of the quad
        assertEquals(0.25f, n[3 * 8], 1e-6f);
        assertEquals(0.5f, n[3 * 8 + 1], 1e-6f);
        // uv follows its vertex, the normal's y flips (z stays)
        assertEquals(1f, n[3 * 8 + 3], 1e-6f);
        assertEquals(1f, n[0 * 8 + 7], 1e-6f);
        assertEquals(0f, n[0 * 8 + 6], 1e-6f);
        assertEquals(0, MeshMath.normalise(null).length);
        assertEquals(0, MeshMath.normalise(new float[8 * 3]).length, "not a whole number of quads");
        assertEquals(0, MeshMath.normalise(new float[8 * 4]).length, "all vertices at one point");
    }
}
