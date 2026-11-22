package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.world.item.Rarity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DjinnRarityTest {
    @Test
    void mainCastIsEpicAndTheRestSplitAtTheMedian() {
        assertEquals(Rarity.EPIC, DjinnRarity.rarity(true, 1.0, 20.0));
        assertEquals(Rarity.RARE, DjinnRarity.rarity(false, 20.0, 20.0));
        assertEquals(Rarity.UNCOMMON, DjinnRarity.rarity(false, 19.9, 20.0));
    }

    @Test
    void everyDjinnHasARarityAndTheMainCastKeepsEpic() {
        Map<Djinn, Rarity> t = DjinnRarity.build();
        assertEquals(Djinn.values().length, t.size());
        for (Djinn d : DjinnRarity.MAIN_CAST) assertEquals(Rarity.EPIC, t.get(d));
        assertTrue(t.values().stream().anyMatch(r -> r == Rarity.UNCOMMON), "some Djinn are below RARE");
    }
}
