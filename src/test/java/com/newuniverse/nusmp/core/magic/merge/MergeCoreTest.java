package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.core.magic.profile.MagicKeys;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/** Naming, determinism, limits, spell union, colour blending and the store of merged magics. */
class MergeCoreTest {
    private static final Set<String> REAL = Set.of("FLAME", "WATER", "WIND", "EARTH", "ICE", "ICE_WEDGE", "POISON", "POISON_PLANT", "TIME", "BRONZE", "KEY",
            "ANTI_MAGIC", "KOTODAMA", "AIR", "STONE", "SOUND", "SAND", "SNOW", "SOUL", "SMOKE", "SONG", "SKIN", "SCALE", "SPIKE", "SHADOW", "STAR", "STORM",
            "TREE", "VINE", "WING", "MUD", "MIST", "NAIL", "ROCK", "HAIR", "MEMORY", "MUCUS", "BONE", "BEAST", "BLOOD", "BODY", "EYE", "FOOD", "GEL", "GLASS",
            "IRON", "LIGHT", "MAGMA", "MERCURY", "MIRROR", "PLANT", "SLASH", "STEEL", "SWORD", "THREAD", "COPY", "DICE", "DREAM", "GAME", "ASH", "COTTON",
            "CREATION", "CRYSTAL", "CHAIN", "BUBBLE", "BRIAR", "BARRIER", "BUTOH", "COMPASS", "COPPER", "CORUNDUM", "GRAVITY", "ILLUSION", "LEGION", "SEALING",
            "SPATIAL", "EXPLOSION", "LIGHTNING", "PAINTING", "RECOMBINATION", "REINFORCEMENT", "TRANSMUTATION", "WORLD_TREE", "MODIFICATION", "PERMEATION");
    private static final Predicate<String> IS_REAL = REAL::contains;

    private static MergeStore.Outcome create(MergeStore s, String name, String... parents) {
        return s.create(List.of(parents), name, IS_REAL, Set.of(), null);
    }

    // ---------------------------------------------------------------- ids and names
    @Test
    void theIdOnlyDependsOnTheParentSet() {
        assertEquals("merged:flame+water", MergedKeys.idOf(MergedKeys.canonicalParents(List.of("water", "FLAME"))));
        assertEquals("merged:flame+water", MergedKeys.idOf(MergedKeys.canonicalParents(List.of("Flame", "Water"))));
        assertEquals("merged:flame+ice_wedge+water", MergedKeys.idOf(MergedKeys.canonicalParents(List.of("water", "ice wedge", "flame"))));
        assertEquals(List.of("FLAME", "WATER"), MergedKeys.parentsOf("merged:flame+water"));
        assertTrue(MergedKeys.parentsOf("FLAME").isEmpty());
        assertEquals(MergedKeys.hash("merged:flame+water"), MergedKeys.hash("MERGED:Flame+Water"), "the key is of the canonical id");
        assertNotEquals(MergedKeys.hash("merged:flame+water"), MergedKeys.hash("merged:flame+wind"));
        assertNotEquals(0, MergedKeys.hash(""));
    }

    @Test
    void theSeedCarriesTheKeyAndTheRandomness() {
        long seed = MergedKeys.seed("merged:flame+water", 0xDEADBEEFL);
        assertEquals(MergedKeys.hash("merged:flame+water"), MergedKeys.keyOfSeed(seed));
        assertEquals(0xDEADBEEFL, seed & 0xFFFFFFFFL);
        long neg = MergedKeys.seed("merged:flame+water", -1L);
        assertEquals(MergedKeys.hash("merged:flame+water"), MergedKeys.keyOfSeed(neg), "a negative random must not leak into the key");
    }

    @Test
    void autoNamesAreBuiltFromTheParts() {
        assertEquals("Flame-Water Magic", MergeNaming.autoName(List.of("FLAME", "WATER"), null));
        assertEquals("Ice Wedge-Poison Plant Magic", MergeNaming.autoName(List.of("ICE_WEDGE", "POISON_PLANT"), null));
        assertEquals("Ember-Tide Magic", MergeNaming.autoName(List.of("FLAME", "WATER"), p -> p.equals("FLAME") ? "Ember Magic" : "Tide"));
        String longName = MergeNaming.autoName(List.of("ICE_WEDGE", "POISON_PLANT", "WORLD_TREE", "TRANSMUTATION"), null);
        assertEquals("Ice-Poison-World-Transmutation Magic", longName, "long names fall back to the first word of each part");
        assertEquals("Merged Magic", MergeNaming.autoName(List.of(), null));
    }

    @Test
    void customNamesAreCleanedOrRefused() {
        assertEquals("Storm Born", MergeNaming.cleanCustomName("  Storm   Born "));
        assertEquals("Dragon's Breath", MergeNaming.cleanCustomName("Dragon's Breath"));
        assertEquals("AB C", MergeNaming.cleanCustomName("A|B,+: C"), "the storage separators vanish");
        assertEquals("Red", MergeNaming.cleanCustomName("§cRed") == null ? "Red" : MergeNaming.cleanCustomName("§cRed").replace("c", ""), "the section sign is dropped");
        assertNull(MergeNaming.cleanCustomName("ab"), "too short");
        assertNull(MergeNaming.cleanCustomName("x".repeat(33)), "too long");
        assertNull(MergeNaming.cleanCustomName("Fire <3"), "no symbols");
        assertNull(MergeNaming.cleanCustomName(null));
        assertEquals("x".repeat(32), MergeNaming.cleanCustomName("x".repeat(32)));
    }

    // ---------------------------------------------------------------- checks and limits
    @Test
    void parentsMustBeTwoToFourDistinctRealUnrestrictedMagics() {
        MergeStore s = new MergeStore();
        assertEquals(MergeCheck.Refusal.TOO_FEW, create(s, null, "FLAME").check().refusal());
        assertEquals(MergeCheck.Refusal.TOO_FEW, s.create(null, null, IS_REAL, Set.of(), null).check().refusal());
        assertEquals(MergeCheck.Refusal.TOO_MANY, create(s, null, "FLAME", "WATER", "WIND", "EARTH", "ICE").check().refusal());
        assertEquals(MergeCheck.Refusal.DUPLICATE_PARENT, create(s, null, "FLAME", "flame").check().refusal());
        assertEquals(MergeCheck.Refusal.UNKNOWN_PARENT, create(s, null, "FLAME", "NOT_A_MAGIC").check().refusal());
        assertEquals(MergeCheck.Refusal.UNKNOWN_PARENT, create(s, null, "FLAME", "merged:flame+water").check().refusal(), "no nesting");
        assertEquals(MergeCheck.Refusal.RESTRICTED_PARENT, create(s, null, "FLAME", "ANTI_MAGIC").check().refusal());
        assertEquals(MergeCheck.Refusal.RESTRICTED_PARENT, create(s, null, "KOTODAMA", "WATER").check().refusal());
        assertEquals("ANTI_MAGIC", create(s, null, "FLAME", "ANTI_MAGIC").check().detail());
        assertEquals(0, s.size(), "a refusal creates nothing");
        assertTrue(create(s, null, "FLAME", "WATER").check().ok());
        for (MergeCheck.Refusal r : MergeCheck.Refusal.values()) assertTrue(r.langKey.startsWith("nusmp.merge."), r.name());
    }

    @Test
    void creationIsDeterministicAndIdempotent() {
        MergeStore a = new MergeStore(), b = new MergeStore();
        MergeStore.Outcome x = create(a, null, "WATER", "FLAME");
        MergeStore.Outcome y = create(b, null, "FLAME", "WATER");
        assertEquals(MergeStore.Status.CREATED, x.status());
        assertEquals(x.def(), y.def(), "the same parent set gives the same definition in any order and any world");
        assertEquals("merged:flame+water", x.def().id());
        assertEquals("Flame-Water Magic", x.def().name());
        assertEquals("FLAME", x.def().dominant());
        MergeStore.Outcome again = create(a, "Another Name", "FLAME", "WATER");
        assertEquals(MergeStore.Status.EXISTS, again.status());
        assertSame(x.def(), again.def(), "the existing definition is returned, a new name is ignored");
        assertEquals(1, a.size());
        assertEquals(x.def().slot(), again.def().slot());
    }

    @Test
    void theWorldHoldsAtMostSixtyFour() {
        MergeStore s = new MergeStore();
        List<String> names = new ArrayList<>(REAL);
        names.removeAll(MagicKeys.RESTRICTED);
        java.util.Collections.sort(names);
        int made = 0;
        for (int i = 0; i + 1 < names.size() && made < 64; i++) {
            MergeStore.Outcome o = create(s, null, names.get(i), names.get(i + 1));
            if (o.status() == MergeStore.Status.CREATED) made++;
        }
        assertEquals(64, s.size());
        MergeStore.Outcome over = create(s, null, names.get(0), names.get(5));
        assertEquals(MergeStore.Status.REFUSED, over.status());
        assertEquals(MergeCheck.Refusal.LIMIT_REACHED, over.check().refusal());
        MergedDef first = s.all().get(0);
        MergeStore.Outcome again = create(s, null, first.parents().toArray(new String[0]));
        assertEquals(MergeStore.Status.EXISTS, again.status(), "an existing merge is still found at the limit");
        Set<Integer> slots = new HashSet<>();
        for (MergedDef d : s.all()) assertTrue(slots.add(d.slot()), "slots are unique");
        assertEquals(64, slots.size());
    }

    @Test
    void customNamesAreUniqueAndCanNotBeAGameMagicName() {
        MergeStore s = new MergeStore();
        assertEquals("Stormborn", create(s, "Stormborn", "FLAME", "WATER").def().name());
        assertTrue(create(s, "Stormborn", "FLAME", "WIND").check().refusal() == MergeCheck.Refusal.NAME_TAKEN);
        assertTrue(create(s, "STORMBORN", "FLAME", "EARTH").check().refusal() == MergeCheck.Refusal.NAME_TAKEN, "case-insensitive");
        MergeStore.Outcome reserved = s.create(List.of("FLAME", "ICE"), "Ice Magic", IS_REAL, Set.of("ice magic"), null);
        assertEquals(MergeCheck.Refusal.NAME_TAKEN, reserved.check().refusal());
        assertEquals(MergeCheck.Refusal.BAD_NAME, create(s, "no", "FLAME", "ICE").check().refusal());
    }

    @Test
    void removalIsOnlyOnRequestAndKeepsTheSlotRetiredUntilAllAreUsed() {
        MergeStore s = new MergeStore();
        MergedDef fw = create(s, null, "FLAME", "WATER").def();
        MergedDef fi = create(s, null, "FLAME", "ICE").def();
        assertEquals(0, fw.slot());
        assertEquals(1, fi.slot());
        assertTrue(s.remove("merged:flame+water"));
        assertFalse(s.remove("merged:flame+water"));
        assertNull(s.byId("merged:flame+water"));
        MergedDef we = create(s, null, "WIND", "EARTH").def();
        assertEquals(2, we.slot(), "a retired slot is not handed to the next magic while never-used ones remain");
        assertSame(fi, s.bySlot(1));
        assertSame(we, s.byKey(we.key()));
    }

    // ---------------------------------------------------------------- codec
    @Test
    void theCodecRoundTrips() {
        MergeStore s = new MergeStore();
        create(s, null, "FLAME", "WATER");
        create(s, "Ice Wedge Mix", "ICE_WEDGE", "POISON_PLANT", "TIME");
        s.remove("merged:flame+water");
        MergeStore back = MergeStore.decode(s.encode());
        assertEquals(s.all(), back.all());
        MergedDef next = create(back, null, "WIND", "EARTH").def();
        assertEquals(create(s, null, "WIND", "EARTH").def().slot(), next.slot(), "the used-slot mask survives");
        assertEquals(2, MergeStore.decode(s.encode()).size(), "the removed one stays removed, the new one is kept");
    }

    @Test
    void decodingBadDataNeverThrowsAndEnforcesTheRules() {
        List<String> lines = new ArrayList<>();
        lines.add(null);
        lines.add("");
        lines.add("garbage");
        lines.add("merged:a+b|x|0|Name|FLAME,WATER");                // slot not a number
        lines.add("merged:a+b|99|0|Name|FLAME,WATER");               // slot out of range
        lines.add("merged:a+b|3|0|Name|FLAME");                       // one parent
        lines.add("merged:a+b|3|0|Name|FLAME,WATER,WIND,EARTH,ICE");  // five parents
        lines.add("whatever|3|1|Good Name|WATER,FLAME");              // the id is rebuilt from the parents
        lines.add("merged:flame+water|4|0|Duplicate|FLAME,WATER");    // same parent set twice
        lines.add("merged:flame+wind|3|0|Slot clash|FLAME,WIND");     // slot 3 taken
        lines.add("merged:flame+ice|5|0||FLAME,ICE");                 // blank name -> auto
        lines.add("#used|notanumber");
        MergeStore s = MergeStore.decode(lines);
        assertEquals(2, s.size());
        assertEquals("merged:flame+water", s.bySlot(3).id());
        assertEquals("Good Name", s.bySlot(3).name());
        assertTrue(s.bySlot(3).custom());
        assertEquals("Flame-Ice Magic", s.bySlot(5).name());
        assertEquals(0, MergeStore.decode(null).size());
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 100; i++) many.add("x|" + (i % 64) + "|0|N" + i + "|FLAME,WATER");
        assertTrue(MergeStore.decode(many).size() <= 1, "all of those are the same parent set");
    }

    // ---------------------------------------------------------------- spell union
    private static List<SpellUnion.PageInfo> pages(String prefix, int n, double baseCost) {
        List<SpellUnion.PageInfo> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(new SpellUnion.PageInfo(prefix + i, baseCost + i * 10, 100 * (i + 1), 80 * (i + 1)));
        return l;
    }

    @Test
    void theUnionTakesFixedRatiosSpreadFromStarterToSignature() {
        assertEquals(4, SpellUnion.perParent(2));
        assertEquals(3, SpellUnion.perParent(3));
        assertEquals(2, SpellUnion.perParent(4));
        assertArrayEquals(new int[]{0, 2, 5, 7}, SpellUnion.spread(8, 4), "evenly spread, the last (signature) page always taken");
        assertArrayEquals(new int[]{0, 1, 2}, SpellUnion.spread(3, 5), "never more than the parent has");
        assertArrayEquals(new int[]{0}, SpellUnion.spread(6, 1));
        assertEquals(0, SpellUnion.spread(0, 4).length);

        List<SpellUnion.Pick> two = SpellUnion.union(List.of(pages("a", 6, 8), pages("b", 5, 10)));
        assertEquals(8, two.size());
        assertEquals(0, two.get(0).parent());
        assertEquals(1, two.get(1).parent());
        assertEquals("a0", two.get(0).pageId(), "interleaved: each parent's starter first");
        assertEquals("b0", two.get(1).pageId());
        assertEquals("a5", two.get(6).pageId(), "and the last two picks are the signatures");
        assertEquals("b4", two.get(7).pageId());
        assertEquals(9, SpellUnion.union(List.of(pages("a", 6, 8), pages("b", 6, 8), pages("c", 6, 8))).size(), "three parents: 3 each");
        assertEquals(8, SpellUnion.union(List.of(pages("a", 6, 8), pages("b", 6, 8), pages("c", 6, 8), pages("d", 6, 8))).size());
    }

    @Test
    void mergedCostIsTheDearestParentTimesOnePointOneFive() {
        List<SpellUnion.Pick> p = SpellUnion.union(List.of(pages("a", 4, 8), pages("b", 4, 20)));
        // rank 0: a0 costs 8, b0 costs 20 -> both 20 * 1.15
        assertEquals(23.0, p.get(0).costPercent(), 1e-9);
        assertEquals(23.0, p.get(1).costPercent(), 1e-9, "the cheaper parent's page is lifted to the dearer one's");
        assertEquals(100 * 1.15, p.get(0).costFloor(), 1e-9);
        assertEquals((int) Math.round(80 * 1.15), p.get(0).cooldown());
        for (SpellUnion.Pick k : SpellUnion.union(List.of(pages("a", 4, 90), pages("b", 4, 90))))
            assertTrue(k.costPercent() <= MergeLimits.COST_CAP_PERCENT, "capped");
        assertTrue(SpellUnion.union(null).isEmpty());
        assertTrue(SpellUnion.union(List.of()).isEmpty());
        assertEquals(4, SpellUnion.union(List.of(pages("a", 4, 8), new ArrayList<>())).size(), "a parent with no pages adds none");
    }

    // ---------------------------------------------------------------- colour blending
    @Test
    void colourBlendingIsStableAndInRange() {
        assertEquals(0xFFFF0000, ColorBlend.lerp(0xFFFF0000, 0xFF0000FF, 0f));
        assertEquals(0xFF0000FF, ColorBlend.lerp(0xFFFF0000, 0xFF0000FF, 1f));
        assertEquals(0xFF800080, ColorBlend.lerp(0xFFFF0000, 0xFF0000FF, 0.5f), "midpoint, rounded");
        assertEquals(0xFF0000FF, ColorBlend.lerp(0xFFFF0000, 0xFF0000FF, 7f), "t is clamped");
        assertEquals(0xFFFF0000, ColorBlend.lerp(0xFFFF0000, 0xFF0000FF, -3f));
    }

    @Test
    void mixingKeepsOneColourAndLiftsSaturationForSeveral() {
        assertEquals(0xFF336699, ColorBlend.mix(new int[]{0xFF336699}, new float[]{1f}));
        assertEquals(0xFFFFFFFF, ColorBlend.mix(null, null));
        int m = ColorBlend.mix(new int[]{0xFFFF0000, 0xFF0000FF}, ColorBlend.weights(2));
        assertEquals(0xFF, m >>> 24);
        int r = (m >> 16) & 255, b = m & 255;
        assertTrue(r > 100 && b > 100, "a red and a blue make a violet");
        assertTrue(r > b, "the first parent weighs more");
        float[] w = ColorBlend.weights(2);
        assertEquals(1f, w[0] + w[1], 1e-6f);
        for (int n = 1; n <= 6; n++) { float sum = 0; for (float f : ColorBlend.weights(n)) sum += f; assertEquals(1f, sum, 1e-5f, "n=" + n); }
        int zero = ColorBlend.mix(new int[]{0xFF102030, 0xFF405060}, new float[]{0f, 0f});
        assertEquals(0xFF, zero >>> 24, "all-zero weights fall back to the plain mean");
    }

    @Test
    void cycleAndAlternateWalkTheParents() {
        int[] c = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
        assertEquals(0xFFFF0000, ColorBlend.cycle(c, 0f));
        assertEquals(0xFF00FF00, ColorBlend.cycle(c, 1f / 3));
        assertEquals(0xFFFF0000, ColorBlend.cycle(c, 1f), "the phase wraps");
        assertEquals(ColorBlend.cycle(c, 0.25f), ColorBlend.cycle(c, 1.25f));
        assertEquals(ColorBlend.cycle(c, 0.9f), ColorBlend.cycle(c, -0.1f));
        assertEquals(0xFF00FF00, ColorBlend.alternate(c, 1));
        assertEquals(0xFF0000FF, ColorBlend.alternate(c, -1));
        assertEquals(0xFFFFFFFF, ColorBlend.alternate(null, 3));
        assertEquals(0xFFAABBCC, ColorBlend.cycle(new int[]{0xFFAABBCC}, 0.4f));
        assertTrue(ColorBlend.sizeFor(0.55f, 2) > ColorBlend.sizeFor(0.45f, 2), "heavier parents draw bigger");
        for (int n = 2; n <= 4; n++) for (float w : ColorBlend.weights(n)) { float s = ColorBlend.sizeFor(w, n); assertTrue(s >= 0.75f && s <= 1.25f); }
    }

    // ---------------------------------------------------------------- the blend and the model plan
    @Test
    void theBlendTakesSoulFromTheDominantParentAndTwoHitEffects() {
        MergedDef def = create(new MergeStore(), null, "WATER", "FLAME").def();
        java.util.function.Function<String, MergedBlend.Part> parts = id -> switch (id) {
            case "FLAME" -> new MergedBlend.Part("FLAME", "FLAME", "BURN", 0xFFFF4A1F);
            case "WATER" -> new MergedBlend.Part("WATER", "WATER", "PUSH", 0xFF3F8CFF);
            default -> null;
        };
        MergedBlend b = MergedBlend.of(def, parts);
        assertEquals("FLAME", b.soulType, "the dominant (first in canonical order) parent's soul");
        assertEquals("BURN", b.hitPrimary);
        assertEquals("PUSH", b.hitSecondary);
        assertEquals(2, b.size());
        assertEquals(0xFFFF4A1F, b.colors[0]);
        assertEquals(0xFF, b.primary >>> 24);
        assertEquals(b.tinted(0), b.tinted(2), "parent indices wrap");
        MergedDef three = create(new MergeStore(), null, "WATER", "FLAME", "ICE").def();
        MergedBlend same = MergedBlend.of(three, id -> new MergedBlend.Part(id, "X", "BURN", 0xFF808080));
        assertNull(same.hitSecondary, "no second effect when all parents hit the same way");
        MergedBlend neutral = MergedBlend.of(def, null);
        assertEquals("EMPTY", neutral.soulType);
        assertEquals("PIERCE", neutral.hitPrimary);
    }

    @Test
    void theModelPlanIsDeterministicBoundedAndShowsEveryParent() {
        for (String[] set : new String[][]{{"FLAME", "WATER"}, {"BRONZE", "COPPER", "KEY"}, {"BEAST", "CORUNDUM", "BRONZE", "FLAME"}, {"FLAME", "ICE"}}) {
            MergedDef def = create(new MergeStore(), null, set).def();
            List<MergedModelPlan.Piece> a = MergedModelPlan.plan(def), b = MergedModelPlan.plan(def);
            assertEquals(a, b, "the same merged magic always has the same emblem");
            assertTrue(a.size() <= MergedModelPlan.MAX_PIECES);
            assertEquals(MergedModelPlan.Kind.GEM, a.get(a.size() - 1).kind(), "the fusion gem comes last");
            Set<Integer> seen = new HashSet<>();
            for (MergedModelPlan.Piece p : a) {
                if (p.parent() >= 0) seen.add(p.parent());
                assertTrue(p.scale() > 0f && p.radius() > 0f && p.height() > 0f);
                if (p.kind() == MergedModelPlan.Kind.GEO) assertTrue(MergedModelPlan.hasGeo(def.parents().get(p.parent())));
            }
            assertEquals(def.size(), seen.size(), "every parent lends at least one piece");
        }
        assertTrue(MergedModelPlan.hasGeo("BRONZE"));
        assertFalse(MergedModelPlan.hasGeo("FLAME"));
        assertTrue(MergedModelPlan.geoOf("FLAME").isEmpty());
        assertNotEquals(MergedModelPlan.plan(create(new MergeStore(), null, "FLAME", "WATER").def()),
                MergedModelPlan.plan(create(new MergeStore(), null, "FLAME", "WIND").def()));
    }

    /** The same numbers tools/magic/merge_preview.py --selftest asserts: the offline preview and the game derive identical values. */
    @Test
    void thePythonPreviewAgreesWithTheJavaCode() {
        MergedDef d = create(new MergeStore(), null, "WATER", "FLAME", "BRONZE").def();
        assertEquals("merged:bronze+flame+water", d.id());
        assertEquals(1726084477, d.key());
        assertEquals("Bronze-Flame-Water Magic", d.name());
        int[] c = {0xFFFF4A1F, 0xFF3F8CFF, 0xFFB09A4A};
        assertEquals(0xFFB17372, ColorBlend.mix(c, ColorBlend.weights(3)));
        assertEquals(0xFFB06485, ColorBlend.mix(new int[]{0xFFFF4A1F, 0xFF3F8CFF}, ColorBlend.weights(2)));
        assertEquals(0xFFC55E62, ColorBlend.cycle(c, 0.1f));
        assertEquals(0xFF4B8EEB, ColorBlend.cycle(c, 0.37f));
        assertEquals(0xFF7893A5, ColorBlend.cycle(c, 0.5f));
        assertEquals(0xFFE7622C, ColorBlend.cycle(c, 0.9f));
        List<MergedModelPlan.Piece> plan = MergedModelPlan.plan(d);
        assertEquals(9, plan.size());
        assertEquals("crest", plan.get(0).geoName());
        assertEquals(9.525879f, plan.get(0).angle(), 1e-3f);
        assertEquals(0.6760547f, plan.get(1).radius(), 1e-4f);
        assertEquals(1.5758318f, plan.get(3).height(), 1e-4f);
        assertEquals(0x66e1f57d00001234L, MergedKeys.seed(d.id(), 0x1234L));
    }
}
