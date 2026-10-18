package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.SimpleTreeState;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "own many lines, equip only one" is the owner's single-active-line rule. The engine already enforces it through the Skill Tree's active class, and the menu must only ask the same question; this proves it
 * on the real data. Every one of the 283 routes has its own class id, so with every route owned and any one class active, EXACTLY one line is equipped (and none when the class is empty or unknown); the Equip
 * button says the right thing for each state and sends the class id only when it should.
 */
class EquipRuleTest {
    @Test
    void theButtonStatesFollowOwnershipAndTheActiveClass() {
        SkillRoute r = MenuFixtures.line();
        assertEquals(EquipRule.State.NOT_OWNED, EquipRule.of(new SimpleTreeState(), r));
        assertEquals(EquipRule.State.NOT_OWNED, EquipRule.of(new SimpleTreeState(), null));
        assertEquals(EquipRule.State.CAN_EQUIP, EquipRule.of(new SimpleTreeState().unlock(r.id()).withClass("other", 0), r));
        assertEquals(EquipRule.State.CAN_EQUIP, EquipRule.of(new SimpleTreeState().unlock(r.id()), r));
        assertEquals(EquipRule.State.EQUIPPED, EquipRule.of(new SimpleTreeState().unlock(r.id()).withClass("t", 0), r));
        SkillRoute noClass = new SkillRoute("nusmp:n", "x", "N", "", "b", "B", "", 0, "", "", 0, 0, List.of());
        assertEquals(EquipRule.State.NO_CLASS, EquipRule.of(new SimpleTreeState().unlock("nusmp:n"), noClass));
    }

    @Test
    void theButtonSendsTheClassOnlyWhenItMay() {
        SkillRoute r = MenuFixtures.line();
        assertEquals("t", EquipRule.classToSend(EquipRule.State.CAN_EQUIP, r));
        assertEquals("", EquipRule.classToSend(EquipRule.State.EQUIPPED, r));
        assertEquals("", EquipRule.classToSend(EquipRule.State.NOT_OWNED, r));
        assertEquals("", EquipRule.classToSend(EquipRule.State.NO_CLASS, r));
        assertEquals("", EquipRule.classToSend(EquipRule.State.CAN_EQUIP, null));
    }

    @Test
    void ownerOfEveryLineHasExactlyOneEquippedAtATime() {
        List<SkillRoute> all = MenuFixtures.realRoutes();
        Set<String> classes = new HashSet<>();
        for (SkillRoute r : all) assertTrue(classes.add(r.classId()), "the class id " + r.classId() + " is shared by two routes, so two lines could be equipped at once");
        SimpleTreeState s = new SimpleTreeState();
        for (SkillRoute r : all) s.unlock(r.id());
        assertEquals(0, EquipRule.equippedCount(s.withClass("", 0), all), "no class, no line equipped");
        assertEquals(0, EquipRule.equippedCount(s.withClass("not_a_class", 0), all));
        for (SkillRoute r : all) assertEquals(1, EquipRule.equippedCount(s.withClass(r.classId(), 0), all), "owning all 283 and equipping " + r.id() + " equips exactly one");
    }

    @Test
    void equippingOneLineMakesTheOthersCanEquipAndALineYouLoseIsNotEquipped() {
        List<SkillRoute> all = MenuFixtures.realRoutes();
        SkillRoute a = all.get(0), b = all.get(1);
        SimpleTreeState s = new SimpleTreeState().unlock(a.id()).unlock(b.id()).withClass(a.classId(), 0);
        assertEquals(EquipRule.State.EQUIPPED, EquipRule.of(s, a));
        assertEquals(EquipRule.State.CAN_EQUIP, EquipRule.of(s, b));
        s.withClass(b.classId(), 0);
        assertEquals(EquipRule.State.CAN_EQUIP, EquipRule.of(s, a));
        assertEquals(EquipRule.State.EQUIPPED, EquipRule.of(s, b));
        SimpleTreeState lost = new SimpleTreeState().unlock(a.id()).withClass(b.classId(), 0);
        assertEquals(EquipRule.State.NOT_OWNED, EquipRule.of(lost, b), "an active class of a line you no longer own equips nothing");
    }
}
