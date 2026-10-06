package com.newuniverse.nusmp.grimoire;

import com.mojang.serialization.JsonOps;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.MagicType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Headless rules for the procedural grimoire generator: 103,000 unique ids, stable codecs, rarity-correct owner looks. */
class GrimoireAppearanceTest {

    /** Everything that makes two grimoires look different (colours included). */
    private record Identity(InsigniaType insignia, CoverMaterial cover, TrimMetal metal, Thickness thickness, ClaspType clasp,
                            int primary, int trim, int aura) {
        static Identity of(GrimoireAppearance a) {
            return new Identity(a.insignia(), a.cover(), a.trimMetal(), a.thickness(), a.clasp(), a.primaryColor(), a.trimColor(), a.auraColor());
        }
    }

    @Test
    void everyIdMapsToADistinctLook() {
        Set<Identity> seen = new HashSet<>();
        for (int id = 0; id < GrimoireAppearance.ID_COUNT; id++) {
            assertTrue(seen.add(Identity.of(GrimoireAppearance.fromGrimoireId(id))), "id " + id + " repeats an earlier look");
        }
        assertEquals(103_000, seen.size());
        assertTrue(GrimoireAppearance.COMBINATIONS >= GrimoireAppearance.ID_COUNT);
    }

    @Test
    void generatorCoversEveryOptionAndStaysDeterministic() {
        Set<InsigniaType> insignia = new HashSet<>();
        Set<CoverMaterial> covers = new HashSet<>();
        Set<TrimMetal> metals = new HashSet<>();
        Set<Thickness> thick = new HashSet<>();
        Set<ClaspType> clasps = new HashSet<>();
        Set<Integer> auras = new HashSet<>();
        for (int id = 0; id < GrimoireAppearance.ID_COUNT; id++) {
            GrimoireAppearance a = GrimoireAppearance.fromGrimoireId(id);
            insignia.add(a.insignia()); covers.add(a.cover()); metals.add(a.trimMetal()); thick.add(a.thickness()); clasps.add(a.clasp()); auras.add(a.auraColor());
            assertEquals(a.insignia().count, a.leafCount());
            if (a.insignia().kingdom != null) assertEquals(a.insignia().kingdom, a.kingdom());
        }
        assertEquals(InsigniaType.values().length, insignia.size());
        assertEquals(CoverMaterial.values().length, covers.size());
        assertEquals(TrimMetal.values().length, metals.size());
        assertEquals(Thickness.values().length, thick.size());
        assertEquals(ClaspType.values().length, clasps.size());
        assertEquals(AuraAffinity.values().length, auras.size());
        assertEquals(GrimoireAppearance.fromGrimoireId(54_321), GrimoireAppearance.fromGrimoireId(54_321));
        assertEquals(GrimoireAppearance.fromSeed(-1L), GrimoireAppearance.fromSeed(-1L));
    }

    @Test
    void idsOutsideTheRangeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GrimoireAppearance.fromGrimoireId(-1));
        assertThrows(IllegalArgumentException.class, () -> GrimoireAppearance.fromGrimoireId(GrimoireAppearance.ID_COUNT));
        assertDoesNotThrow(() -> GrimoireAppearance.fromGrimoireId(GrimoireAppearance.ID_COUNT - 1));
    }

    @Test
    void anySeedLandsOnAValidId() {
        Random r = new Random(7);
        for (int i = 0; i < 200_000; i++) {
            int id = GrimoireAppearance.idOfSeed(r.nextLong());
            assertTrue(id >= 0 && id < GrimoireAppearance.ID_COUNT);
        }
        for (long seed : new long[]{0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE}) assertNotNull(GrimoireAppearance.fromSeed(seed));
        assertEquals(GrimoireAppearance.fromGrimoireId(GrimoireAppearance.idOfSeed(99)), GrimoireAppearance.fromSeed(99));
    }

    @Test
    void codecAndNetworkRoundTrip() {
        for (int id = 0; id < GrimoireAppearance.ID_COUNT; id += 37) {
            GrimoireAppearance a = GrimoireAppearance.fromGrimoireId(id);
            var json = GrimoireAppearance.CODEC.encodeStart(JsonOps.INSTANCE, a).getOrThrow();
            assertEquals(a, GrimoireAppearance.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            ByteBuf buf = Unpooled.buffer();
            GrimoireAppearance.STREAM_CODEC.encode(buf, a);
            assertEquals(16, buf.readableBytes(), "wire size");
            assertEquals(a, GrimoireAppearance.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
            buf.release();
        }
    }

    @Test
    void colourChannelsMapToTheTintIndices() {
        GrimoireAppearance a = GrimoireAppearance.fromGrimoireId(12_345);
        assertEquals(a.primaryColor(), a.tint(0));
        assertEquals(a.trimColor(), a.tint(1));
        assertEquals(a.insignia().color, a.tint(2));
        assertEquals(a.auraColor(), a.tint(3));
        assertEquals(-1, a.tint(4));
        assertEquals(-1, a.tint(-1));
        for (int id = 0; id < GrimoireAppearance.ID_COUNT; id += 11) {
            GrimoireAppearance g = GrimoireAppearance.fromGrimoireId(id);
            for (int t = 0; t < 4; t++) assertEquals(0, g.tint(t) & 0xFF000000, "tint must be plain RGB");
        }
    }

    @Test
    void everyExistingCoverAndMagicHasALook() {
        for (GrimoireCover cover : GrimoireCover.values()) {
            for (MagicType magic : MagicType.values()) {
                GrimoireAppearance a = GrimoireAppearance.fromCover(cover, magic);
                assertSame(a, GrimoireAppearance.fromCover(cover, magic), "cached");
                assertEquals(InsigniaType.of(cover), a.insignia());
                assertEquals(cover.kingdom, a.kingdom());
                int palette = MagicPalette.primary(magic);
                if (cover == GrimoireCover.FIVE_LEAF && magic == MagicType.ANTI_MAGIC) assertEquals(0x4A423E, a.primaryColor(), "grimy leather so the black clover shows");
                else if (cover.tier >= 5) {
                    for (int shift : new int[]{16, 8, 0}) assertTrue(((a.primaryColor() >> shift) & 255) <= ((palette >> shift) & 255), "a corrupted cover turns darker");
                    assertNotEquals(palette, a.primaryColor());
                } else assertEquals(palette, a.primaryColor());
                if (cover == GrimoireCover.FOUR_LEAF) {   // Yuno-style
                    assertEquals(CoverMaterial.METALLIC_TRIM, a.cover(), "rare = ornate gilded");
                    assertEquals(TrimMetal.GOLD, a.trimMetal());
                }
                if (cover == GrimoireCover.THREE_LEAF) assertEquals(CoverMaterial.PLAIN, a.cover(), "common = simple border");
                assertEquals(ClaspType.OPEN, a.clasp());
                assertEquals(MagicPalette.aura(magic), a.auraColor());
                assertEquals(InsigniaType.of(cover).toCover(), cover, "crest <-> cover must round-trip for the original 12");
            }
        }
        assertNotNull(GrimoireAppearance.DEFAULT);
    }

    @Test
    void ownerLooksAreStableVariedAndRarityCorrect() {
        Random r = new Random(11);
        for (GrimoireCover cover : GrimoireCover.values()) {
            Set<Identity> looks = new HashSet<>();
            for (int i = 0; i < 400; i++) {
                UUID owner = new UUID(r.nextLong(), r.nextLong());
                GrimoireAppearance a = GrimoireAppearance.forOwner(cover, MagicType.FLAME, owner);
                assertEquals(a, GrimoireAppearance.forOwner(cover, MagicType.FLAME, owner), "same owner, same look");
                assertEquals(InsigniaType.of(cover), a.insignia(), "crest never changes");
                assertEquals(MagicPalette.aura(MagicType.FLAME), a.auraColor(), "aura stays the magic's");
                looks.add(Identity.of(a));
                assertEquals(ClaspType.OPEN, a.clasp(), "canon grimoires have no straps, chains or locks");
                GrimoireAppearance base = GrimoireAppearance.fromCover(cover, MagicType.FLAME);
                assertEquals(base.trimMetal(), a.trimMetal(), "trim comes from the cover, not the owner");
                if (cover.isCracked()) assertEquals(CoverMaterial.TATTERED, a.cover());
                else if (!cover.isForbidden() && cover.tier == 3) assertTrue(a.cover() == CoverMaterial.PLAIN || a.cover() == CoverMaterial.LEATHER_STITCHED);
                else assertEquals(base.cover(), a.cover(), "rarity-defining cover is not up to the owner");
                if (cover.kingdom != com.newuniverse.nusmp.blackclover.Kingdom.CLOVER) assertEquals(TrimMetal.NONE, a.trimMetal(), "no invented hardware outside canon");
                if (cover.tier == 3 && !cover.isCracked()) assertEquals(TrimMetal.NONE, a.trimMetal(), "ordinary books have no hardware");
                assertNotEquals(CoverMaterial.DRAGON_HIDE, a.cover());
                assertNotEquals(TrimMetal.RUNED, a.trimMetal());
            }
            assertTrue(looks.size() > 40, cover + " should give owners distinct looks, got " + looks.size());
        }
    }

    @Test
    void renderKeyIgnoresColourOnly() {
        GrimoireAppearance a = GrimoireAppearance.fromGrimoireId(777);
        GrimoireAppearance recoloured = new GrimoireAppearance(a.kingdom(), a.insignia(), a.leafCount(), a.cover(), a.trimMetal(),
                0x123456, 0x654321, 0xABCDEF, a.thickness(), a.clasp());
        assertEquals(a.renderKey(), recoloured.renderKey(), "colours must not rebuild geometry");
        assertNotEquals(a, recoloured);
    }

    @Test
    void renderKeysAreFewEnoughForTheCache() {
        Set<GrimoireRenderKey> keys = new HashSet<>();
        for (int id = 0; id < GrimoireAppearance.ID_COUNT; id++) keys.add(GrimoireAppearance.fromGrimoireId(id).renderKey());
        assertTrue(keys.size() <= 16 * 5 * 4 * 4 * 4, "key space is bounded: " + keys.size());
        System.out.println("distinct render keys across all 103,000 grimoires: " + keys.size());
    }
}
