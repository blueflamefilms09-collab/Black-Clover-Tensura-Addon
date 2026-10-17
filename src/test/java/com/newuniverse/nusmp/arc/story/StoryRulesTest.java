package com.newuniverse.nusmp.arc.story;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/** The story chain: stage, saga, which bosses are open, what a victory opens, the closure the unlock command credits, and loops. */
class StoryRulesTest {
    private static ArcCharacter boss(String id, ArcSaga saga, String... after) {
        return ArcCharacter.boss(id, id).saga(saga).after(after).build();
    }

    private static final ArcCharacter REVCHI = boss("revchi", ArcSaga.ELF_REINCARNATION);
    private static final ArcCharacter HEATH = boss("heath_grice", ArcSaga.ELF_REINCARNATION, "revchi");
    private static final ArcCharacter ZAGRED = boss("zagred", ArcSaga.ELF_REINCARNATION, "heath_grice", "revchi");
    private static final ArcCharacter DANTE = boss("dante", ArcSaga.SPADE_RAID, "zagred");
    private static final ArcCharacter GHOSTLY = boss("ghostly", ArcSaga.SPADE_RAID, "not_merged_yet");
    private static final ArcCharacter YAMI = ArcCharacter.npc("yami", "Yami").after("revchi").build();
    private static final List<ArcCharacter> ROSTER = List.of(REVCHI, HEATH, ZAGRED, DANTE, GHOSTLY, YAMI);

    private static final Map<String, ArcCharacter> BY_ID = new HashMap<>();
    static {
        for (ArcCharacter c : ROSTER) BY_ID.put(c.id(), c);
    }
    private static final Predicate<String> EXISTS = BY_ID::containsKey;

    private static StoryData fallen(String... ids) {
        StoryData d = new StoryData();
        for (String id : ids) d.defeat(id);
        return d;
    }

    private static List<String> ids(List<ArcCharacter> l) {
        List<String> out = new ArrayList<>();
        for (ArcCharacter c : l) out.add(c.id());
        return out;
    }

    @Test
    void theStageCountsBossesOfTheRosterOnly() {
        assertEquals(0, StoryRules.stage(new StoryData(), ROSTER));
        assertEquals(2, StoryRules.stage(fallen("revchi", "heath_grice"), ROSTER));
        assertEquals(1, StoryRules.stage(fallen("revchi", "somebody_removed", "yami"), ROSTER), "an unknown id and an NPC do not count");
    }

    @Test
    void theSagaIsTheEarliestOneWithABossStillStanding() {
        assertEquals(ArcSaga.ELF_REINCARNATION, StoryRules.currentSaga(new StoryData(), ROSTER));
        assertEquals(ArcSaga.ELF_REINCARNATION, StoryRules.currentSaga(fallen("revchi", "heath_grice"), ROSTER));
        assertEquals(ArcSaga.SPADE_RAID, StoryRules.currentSaga(fallen("revchi", "heath_grice", "zagred"), ROSTER));
        assertEquals(ArcSaga.SPADE_RAID, StoryRules.currentSaga(fallen("revchi", "heath_grice", "zagred", "dante", "ghostly"), ROSTER), "all fallen: the last saga");
        assertEquals(ArcSaga.ELF_REINCARNATION, StoryRules.currentSaga(new StoryData(), List.of()), "an empty roster: the first saga");
        assertEquals(ArcSaga.ELF_REINCARNATION, StoryRules.currentSaga(new StoryData(), List.of(YAMI)));
    }

    @Test
    void aBossIsUnlockedWhenEveryPrerequisiteHasFallen() {
        assertTrue(StoryRules.unlocked(new StoryData(), REVCHI, EXISTS), "no prerequisite: open from the start");
        assertFalse(StoryRules.unlocked(new StoryData(), HEATH, EXISTS));
        assertTrue(StoryRules.unlocked(fallen("revchi"), HEATH, EXISTS));
        assertFalse(StoryRules.unlocked(fallen("heath_grice"), ZAGRED, EXISTS), "one of two is not enough");
        assertTrue(StoryRules.unlocked(fallen("heath_grice", "revchi"), ZAGRED, EXISTS));
        assertEquals(List.of("heath_grice", "revchi"), StoryRules.missing(new StoryData(), ZAGRED, EXISTS), "in the order the character lists them");
        assertEquals(List.of("revchi"), StoryRules.missing(fallen("heath_grice"), ZAGRED, EXISTS));
    }

    @Test
    void aPrerequisiteThatIsNotInTheRosterIsSkippedNotImpossible() {
        assertTrue(StoryRules.unlocked(new StoryData(), GHOSTLY, EXISTS));
        assertEquals(List.of("not_merged_yet"), StoryRules.missing(new StoryData(), GHOSTLY, id -> true), "it counts once it is real");
    }

    @Test
    void anNpcFollowsTheSameRule() {
        assertFalse(StoryRules.unlocked(new StoryData(), YAMI, EXISTS));
        assertTrue(StoryRules.unlocked(fallen("revchi"), YAMI, EXISTS));
    }

    @Test
    void availableAreTheUnlockedBossesNotYetBeaten() {
        assertEquals(List.of("revchi", "ghostly"), ids(StoryRules.available(new StoryData(), ROSTER, EXISTS)));
        assertEquals(List.of("heath_grice", "ghostly"), ids(StoryRules.available(fallen("revchi"), ROSTER, EXISTS)));
    }

    @Test
    void aVictoryOpensTheNextBossesAndOnlyThose() {
        StoryData before = fallen("revchi");
        StoryData after = fallen("revchi", "heath_grice");
        assertEquals(List.of("zagred"), ids(StoryRules.newlyAvailable(before, after, ROSTER, EXISTS)));
        assertEquals(List.of("heath_grice"), ids(StoryRules.newlyAvailable(new StoryData(), fallen("revchi"), ROSTER, EXISTS)), "the NPC is not a boss and is not listed");
        assertEquals(List.of(), ids(StoryRules.newlyAvailable(after, after, ROSTER, EXISTS)));
    }

    @Test
    void theClosureIsEverythingThatMustFallEarliestFirst() {
        assertEquals(List.of("revchi", "heath_grice", "zagred"), StoryRules.closure(DANTE, BY_ID::get));
        assertEquals(List.of("revchi"), StoryRules.closure(HEATH, BY_ID::get));
        assertEquals(List.of(), StoryRules.closure(REVCHI, BY_ID::get));
        assertEquals(List.of("revchi", "heath_grice"), StoryRules.closure(ZAGRED, BY_ID::get), "a shared ancestor is listed once");
        assertEquals(List.of(), StoryRules.closure(GHOSTLY, BY_ID::get), "an unknown prerequisite is not followed");
    }

    @Test
    void aLoopIsDetectedAndDoesNotHangTheClosure() {
        ArcCharacter a = boss("loop_a", ArcSaga.ALLIES, "loop_b");
        ArcCharacter b = boss("loop_b", ArcSaga.ALLIES, "loop_a");
        ArcCharacter self = boss("loop_self", ArcSaga.ALLIES, "loop_self");
        Map<String, ArcCharacter> m = Map.of("loop_a", a, "loop_b", b, "loop_self", self);
        assertTrue(StoryRules.cyclic(a, m::get));
        assertTrue(StoryRules.cyclic(b, m::get));
        assertTrue(StoryRules.cyclic(self, m::get));
        assertEquals(List.of("loop_b"), StoryRules.closure(a, m::get));
        assertFalse(StoryRules.cyclic(DANTE, BY_ID::get));
        assertFalse(StoryRules.cyclic(REVCHI, BY_ID::get));
    }

    @Test
    void theGateIsOpenWhenSwitchedOffOrWhenAnyoneInTheGroupHasItUnlocked() {
        assertTrue(StoryRules.gateOpen(false, false));
        assertTrue(StoryRules.gateOpen(true, true));
        assertFalse(StoryRules.gateOpen(true, false));
    }
}
