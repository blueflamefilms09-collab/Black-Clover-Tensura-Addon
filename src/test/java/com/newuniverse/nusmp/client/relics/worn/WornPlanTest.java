package com.newuniverse.nusmp.client.relics.worn;

import com.newuniverse.nusmp.client.relics.worn.WornPlan.Ctx;
import com.newuniverse.nusmp.client.relics.worn.WornPlan.Entry;
import com.newuniverse.nusmp.client.relics.worn.WornPlan.Fallback;
import com.newuniverse.nusmp.client.relics.worn.WornPlan.Item;
import com.newuniverse.nusmp.client.relics.worn.WornPlan.Kind;
import com.newuniverse.nusmp.client.relics.worn.WornPlan.Plan;
import com.newuniverse.nusmp.client.relics.worn.WornPlan.Source;
import com.newuniverse.nusmp.client.relics.worn.WornPlan.View;
import com.newuniverse.nusmp.client.relics.worn.WornPlan.Why;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The decision "what is drawn for this wearer" ({@link WornPlan}): pure, so every rule that used to hide a relic silently is pinned here. */
class WornPlanTest {
    static Entry e(String id, String slot, Source src, Boolean eye, boolean custom, boolean model, int tier) {
        return new Entry(id, slot, "ANY", src, eye, custom, model, tier);
    }

    static Ctx ctx(boolean slim, View view, boolean models, boolean hideArmour, Fallback fb) {
        return new Ctx(slim, view, false, false, false, models, hideArmour, fb);
    }

    static Ctx normal() { return ctx(false, View.OTHER, true, false, Fallback.BOXES); }

    static Item only(Plan p) {
        assertEquals(1, p.all().size());
        return p.all().get(0);
    }

    @Test
    void aCuriosRelicWithAModelIsDrawnFromTheLibraryOnEveryBodyAndEveryView() {
        for (boolean slim : new boolean[]{false, true})
            for (View v : View.values()) {
                Item it = only(WornPlan.decide(ctx(slim, v, true, false, Fallback.BOXES), List.of(e("a", "NECK", Source.CURIOS, true, false, true, 3))));
                assertEquals(Kind.LIBRARY_MODEL, it.kind(), "slim=" + slim + " view=" + v);
                assertEquals(Why.DRAWN, it.why());
            }
    }

    @Test
    void aHandRelicIsHeldNotWorn() {
        Item it = only(WornPlan.decide(normal(), List.of(e("sword", "MAIN_HAND", Source.HAND, null, false, false, 5))));
        assertEquals(Kind.SKIP, it.kind());
        assertEquals(Why.HELD_IN_HAND, it.why());
    }

    @Test
    void aConsumableIsNotWorn() {
        assertEquals(Why.NOT_A_WORN_SLOT, only(WornPlan.decide(normal(), List.of(e("pill", "CONSUMABLE", Source.PACK, null, false, false, 1)))).why());
    }

    @Test
    void anInvisibleSpectatorOrFarWearerDrawsNothingWhateverItWears() {
        List<Entry> worn = List.of(e("a", "NECK", Source.CURIOS, true, false, true, 3), e("b", "HEAD", Source.ARMOUR, null, false, true, 2), e("c", "CHARM", Source.CURIOS, true, true, false, 7));
        Plan spectator = WornPlan.decide(new Ctx(false, View.OTHER, true, false, false, true, false, Fallback.BOXES), worn);
        Plan invisible = WornPlan.decide(new Ctx(false, View.OTHER, false, true, false, true, false, Fallback.BOXES), worn);
        Plan far = WornPlan.decide(new Ctx(false, View.OTHER, false, false, true, true, false, Fallback.BOXES), worn);
        assertTrue(spectator.isEmpty() && invisible.isEmpty() && far.isEmpty());
        assertEquals(Why.WEARER_SPECTATOR, spectator.all().get(0).why());
        assertEquals(Why.WEARER_INVISIBLE, invisible.all().get(1).why());
        assertEquals(Why.TOO_FAR, far.all().get(2).why());
        assertEquals(3, far.all().size(), "every relic still has its answer, for the report");
    }

    @Test
    void theCuriosEyeIconHidesOnlyACuriosRelicAndOnlyWhenItIsOff() {
        assertEquals(Why.EYE_OFF, only(WornPlan.decide(normal(), List.of(e("a", "BACK", Source.CURIOS, false, false, true, 3)))).why());
        assertEquals(Why.DRAWN, only(WornPlan.decide(normal(), List.of(e("a", "BACK", Source.CURIOS, null, false, true, 3)))).why(), "an eye the list does not know is not off");
        assertEquals(Why.DRAWN, only(WornPlan.decide(normal(), List.of(e("a", "BACK", Source.CURIOS, true, false, true, 3)))).why());
        assertEquals(Why.DRAWN, only(WornPlan.decide(normal(), List.of(e("a", "BACK", Source.SYNCED, false, false, true, 3)))).why(), "the server's list carries no eye state");
        assertEquals(Why.DRAWN, only(WornPlan.decide(normal(), List.of(e("a", "HEAD", Source.ARMOUR, false, false, true, 3)))).why(), "armour has no eye icon");
    }

    @Test
    void theArmourOptionHidesArmourRelicsAndNothingElse() {
        Ctx hide = ctx(false, View.OTHER, true, true, Fallback.BOXES);
        assertEquals(Why.ARMOUR_OPTION, only(WornPlan.decide(hide, List.of(e("h", "HEAD", Source.ARMOUR, null, false, true, 3)))).why());
        assertEquals(Why.ARMOUR_OPTION, only(WornPlan.decide(hide, List.of(e("c", "CHEST", Source.ARMOUR, null, false, true, 3)))).why());
        assertEquals(Why.ARMOUR_OPTION, only(WornPlan.decide(hide, List.of(e("f", "FEET", Source.ARMOUR, null, false, true, 3)))).why());
        assertEquals(Why.DRAWN, only(WornPlan.decide(hide, List.of(e("n", "NECK", Source.CURIOS, true, false, true, 3)))).why());
    }

    @Test
    void aModelOptionOffGivesCuriosRelicsTheirPlainBoxesButKeepsArmourModels() {
        Ctx off = ctx(false, View.OTHER, false, false, Fallback.BOXES);
        assertEquals(Kind.PLAIN_BOX, only(WornPlan.decide(off, List.of(e("n", "NECK", Source.CURIOS, true, false, true, 3)))).kind());
        assertEquals(Kind.LIBRARY_MODEL, only(WornPlan.decide(off, List.of(e("h", "HEAD", Source.ARMOUR, null, false, true, 3)))).kind(), "a head or boot model replaces the painted box: not optional");
    }

    @Test
    void aCuriosRelicWithoutAModelIsNeverNothing() {
        assertEquals(Kind.PLAIN_BOX, only(WornPlan.decide(normal(), List.of(e("n", "CHARM", Source.CURIOS, true, false, false, 3)))).kind());
        assertEquals(Kind.FLOATING_CHARM, only(WornPlan.decide(ctx(false, View.OTHER, true, false, Fallback.CHARM), List.of(e("n", "CHARM", Source.CURIOS, true, false, false, 3)))).kind());
        assertEquals(Kind.FLOATING_CHARM, only(WornPlan.decide(normal(), List.of(e("n", "BACK", Source.PACK, null, false, false, 3)))).kind(), "a mob's pack relic is a charm, never a box on a player body");
        assertEquals(Kind.FLOATING_CHARM, only(WornPlan.decide(normal(), List.of(e("h", "HEAD", Source.PACK, null, false, false, 3)))).kind(), "an armour relic in a pack has no armour slot to draw it");
    }

    @Test
    void anArmourRelicWithoutAModelIsLeftToTheArmourLayer() {
        Item it = only(WornPlan.decide(normal(), List.of(e("h", "CHEST", Source.ARMOUR, null, false, false, 3))));
        assertEquals(Kind.SKIP, it.kind());
        assertEquals(Why.VANILLA_ARMOUR, it.why());
    }

    @Test
    void aRelicWithItsOwnRendererIsDrawnByIt() {
        Plan p = WornPlan.decide(normal(), List.of(e("dante_gravity_crown", "HEAD", Source.ARMOUR, null, true, true, 7)));
        assertEquals(Kind.CUSTOM, only(p).kind());
        assertEquals(1, p.custom().size());
        assertTrue(p.library().isEmpty());
    }

    @Test
    void theCameraInTheHeadKeepsHeadPiecesAwayFromTheViewButNotTheRest() {
        Ctx first = ctx(false, View.SELF_FIRST, true, false, Fallback.BOXES);
        assertEquals(Why.FIRST_PERSON_HEAD, only(WornPlan.decide(first, List.of(e("h", "HEAD", Source.ARMOUR, null, false, true, 3)))).why());
        assertEquals(Why.DRAWN, only(WornPlan.decide(first, List.of(e("n", "NECK", Source.CURIOS, true, false, true, 3)))).why());
        assertEquals(Why.DRAWN, only(WornPlan.decide(ctx(false, View.SELF_THIRD, true, false, Fallback.BOXES), List.of(e("h", "HEAD", Source.ARMOUR, null, false, true, 3)))).why());
    }

    @Test
    void theViewIsFirstPersonOnlyForTheOwnBodyWithNoScreenOpen() {
        assertEquals(View.OTHER, WornPlan.viewOf(false, true, false));
        assertEquals(View.SELF_FIRST, WornPlan.viewOf(true, true, false));
        assertEquals(View.SELF_THIRD, WornPlan.viewOf(true, true, true), "an inventory preview is not a first-person body");
        assertEquals(View.SELF_THIRD, WornPlan.viewOf(true, false, false));
    }

    @Test
    void libraryModelsComeOutBestGradeFirstThenById() {
        Plan p = WornPlan.decide(normal(), List.of(e("zz", "NECK", Source.CURIOS, true, false, true, 3), e("bb", "BACK", Source.CURIOS, true, false, true, 5),
                e("aa", "NECK", Source.CURIOS, true, false, true, 3), e("cc", "BELT", Source.CURIOS, true, false, true, 7)));
        List<String> order = new ArrayList<>();
        for (Item i : p.library()) order.add(i.entry().id());
        assertEquals(List.of("cc", "bb", "aa", "zz"), order);
        assertEquals(List.of(3, 1, 2, 0), p.library().stream().map(Item::index).toList(), "the index points back into the list that was given");
    }

    @Test
    void plainBoxesAndCharmsStandAfterTheLibraryModelsOfTheirSlot() {
        Plan p = WornPlan.decide(normal(), List.of(
                e("box1", "CHARM", Source.CURIOS, true, false, false, 2),
                e("model1", "CHARM", Source.CURIOS, true, false, true, 2),
                e("box2", "CHARM", Source.CURIOS, true, false, false, 2),
                e("model2", "CHARM", Source.CURIOS, true, false, true, 2),
                e("neckbox", "NECK", Source.CURIOS, true, false, false, 2)));
        assertEquals(2, p.plain().stream().filter(i -> i.entry().slot().equals("CHARM")).count());
        assertEquals(2, p.plain().get(0).slotIndex(), "after the two library charms");
        assertEquals(3, p.plain().get(1).slotIndex());
        assertEquals(0, p.plain().get(2).slotIndex(), "a slot with no library model starts at the untouched place");
    }

    @Test
    void slimArmsMoveArmPiecesHalfAPixelTowardTheBody() {
        assertEquals(0f, WornPlan.decide(ctx(false, View.OTHER, true, false, Fallback.BOXES), List.of()).armInset());
        assertEquals(WornPlan.SLIM_INSET, WornPlan.decide(ctx(true, View.OTHER, true, false, Fallback.BOXES), List.of()).armInset());
    }

    @Test
    void theSameInputGivesTheSameAnswerEveryFrame() {
        List<Entry> worn = List.of(e("b", "NECK", Source.CURIOS, true, false, true, 3), e("a", "NECK", Source.CURIOS, true, false, true, 3), e("x", "CHARM", Source.PACK, null, false, false, 1));
        Plan one = WornPlan.decide(normal(), worn), two = WornPlan.decide(normal(), worn);
        assertEquals(one.all(), two.all());
        assertEquals(one.library(), two.library());
    }

    @Test
    void everyRelicSlotNameIsEitherWornOrExplained() {
        for (String slot : List.of("MAIN_HAND", "HEAD", "CHEST", "NECK", "BACK", "BELT", "HANDS", "CHARM", "CONSUMABLE", "FEET")) {
            Item it = only(WornPlan.decide(normal(), List.of(e("r", slot, Source.CURIOS, true, false, true, 3))));
            assertEquals(WornPlan.isWornSlot(slot), it.drawn(), slot + " is " + it.describe());
            assertNotNull(it.why().text);
        }
    }

    @Test
    void theReportLineSaysWhichRelicAndWhy() {
        Item it = only(WornPlan.decide(normal(), List.of(e("ring_x", "CHARM", Source.CURIOS, false, false, true, 3))));
        assertTrue(it.describe().startsWith("ring_x: SKIP"), it.describe());
        assertTrue(it.describe().contains("eye"), it.describe());
    }
}
