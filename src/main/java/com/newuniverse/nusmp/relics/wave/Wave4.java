package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.Passive;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Relics of Many Worlds, wave 4: the eight neck relics (amulets and pendants), all worn as Curios necklaces and all needing attunement.
 * <ul>
 *   <li>Amulet of the Coven (LEGEND): the hag goddess's eye: Distant Watcher, Bloody Presage (Scrying), Weakening Gaze.</li>
 *   <li>Pendant of Echoed Souls (UNIQUE): a lethal blow leaves 1 hit point and spectral warriors guard you, once a day.</li>
 *   <li>Amulet of the Forest (LEGEND): six spirit figurines, one blessing at a time for a day; the peacock charms humanoids.</li>
 *   <li>Arcane Glass of Seeing (RARE): six charges; a d6 sharpens or clouds your next blow.</li>
 *   <li>Voice of the Mountain (SPECIAL): a horn that rouses you and your allies.</li>
 *   <li>Amulet of the Sea of Stars (UNIQUE): Darkvision, Truesight, resistance to force, radiant and necrotic; raises Astralis's charges.</li>
 *   <li>Pendant of the Dreamer (RARE): six charges for water breathing, gills and a mind-blast.</li>
 *   <li>Beast Masters Amulet (RARE): stores creatures and lets them out again.</li>
 * </ul>
 * The long card texts are in the lang fragment; numbers and the tabletop-to-game translation are in docs (doc_wave4.md).
 */
public final class Wave4 {
    private Wave4() {}

    private static boolean hooked;

    /** The flag the Amulet of the Sea of Stars raises: while worn and attuned, Astralis's Dimension Walker holds 2 more charges. */
    public static final String FLAG_OVERSEERS_MIGHT = "overseers_might";

    /** The extra Dimension Walker charges Astralis holds for this wearer: 2 while the Amulet of the Sea of Stars works for them, else 0. */
    public static int walkerBonus(LivingEntity wearer) { return RelicFlags.has(wearer, FLAG_OVERSEERS_MIGHT) ? 2 : 0; }

    public static void defs(Consumer<RelicDef> out) {
        out.accept(coven());
        out.accept(echoedSouls());
        out.accept(forest());
        out.accept(glass());
        out.accept(voice());
        out.accept(seaOfStars());
        out.accept(dreamer());
        out.accept(beastMasters());
    }

    // ---------------------------------------------------------------- the relics
    private static RelicDef coven() {
        return RelicDef.of("amulet_of_the_coven", "Amulet of the Coven", Grade.LEGEND, RelicSlot.NECK)
                .attune().vessel().look(Look.AMULET).palette(0x5C7A34, 0xD8CFA4, 0xC1272D)
                .desc("The closed eye of a hag goddess: it shows you what hides ahead, scries a creature for a price in blood, and strips your foes' defences.")
                .passive(Passive.effect("minecraft:night_vision", 0))
                .act(Act.of("distant_watcher", "Distant Watcher", Wave4CovenActs::watcher).cooldown(600).mana(2))
                .act(Act.of("bloody_presage", "Bloody Presage", Wave4CovenActs::presage).cooldown(200).mana(3))
                .act(Act.of("weakening_gaze", "Weakening Gaze", Wave4CovenActs::gaze).cooldown(600).mana(6).usesPerDay(2));
    }

    private static RelicDef echoedSouls() {
        return RelicDef.of("pendant_of_echoed_souls", "Pendant of Echoed Souls", Grade.UNIQUE, RelicSlot.NECK)
                .attune().look(Look.PENDANT).palette(0x3A3D45, 0xC9BFA6, 0xD9D2BC)
                .desc("Once a day a killing blow leaves you at 1 health and calls spectral warriors; while they stand, nothing can touch you.")
                .passive(Passive.flag(Wave4Handlers.FLAG_ECHO));
    }

    private static RelicDef forest() {
        return RelicDef.of("amulet_of_the_forest", "Amulet of the Forest", Grade.LEGEND, RelicSlot.NECK)
                .attune().vessel().look(Look.AMULET).palette(0x7A5A38, 0x4F8A4B, 0x7CE0A0)
                .desc("Six forest spirits hang from it. Touch a figurine and that spirit's blessing is yours for a day: one at a time.")
                .passive(Passive.flag(Wave4Upkeep.FLAG_FOREST))
                .act(forestAct("forest_bear", "Touch the Bear", Wave4Forest.BEAR))
                .act(forestAct("forest_cat", "Touch the Cat", Wave4Forest.CAT))
                .act(forestAct("forest_beetle", "Touch the Beetle", Wave4Forest.BEETLE))
                .act(forestAct("forest_raccoon", "Touch the Raccoon", Wave4Forest.RACCOON))
                .act(forestAct("forest_owl", "Touch the Owl", Wave4Forest.OWL))
                .act(forestAct("forest_peacock", "Touch the Peacock", Wave4Forest.PEACOCK))
                .act(Act.of("forest_dismiss", "Send the Spirit Away", Wave4ForestActs::dismiss).cooldown(100));
    }

    private static Act forestAct(String key, String name, Wave4Forest spirit) {
        return Act.of(key, name, c -> Wave4ForestActs.touch(c, spirit)).cooldown(100).mana(2);
    }

    private static RelicDef glass() {
        return RelicDef.of("arcane_glass_of_seeing", "Arcane Glass of Seeing", Grade.RARE, RelicSlot.NECK)
                .attune().look(Look.LOCKET).palette(0xD0A53A, 0x8F6B1E, 0xC8D2E6)
                .desc("Six charges. Look into the glass and a d6 decides your next blow: a 3 to 6 strikes true, a 1 or 2 falls short.")
                .act(Act.of("gaze_into_the_glass", "Look into the Glass", Wave4ChargeActs::glass).cooldown(100));
    }

    private static RelicDef voice() {
        return RelicDef.of("voice_of_the_mountain", "Voice of the Mountain", Grade.SPECIAL, RelicSlot.NECK)
                .attune().look(Look.PENDANT).palette(0xEDE8DA, 0xA7ABB5, 0x2EC27E)
                .desc("A horn that carries far. Sound it to rouse yourself and your allies within 24 blocks with Speed and Haste.")
                .act(Act.of("rousing_call", "Rousing Call", Wave4MiscActs::rouse).cooldown(1200).mana(2));
    }

    private static RelicDef seaOfStars() {
        return RelicDef.of("amulet_of_the_sea_of_stars", "Amulet of the Sea of Stars", Grade.UNIQUE, RelicSlot.NECK)
                .attune().look(Look.PENDANT).palette(0xC9CED8, 0x8E9AAE, 0xA855F7)
                .desc("Darkvision always; once a day Truesight for five minutes. Resists force, radiant and necrotic harm. Raises Astralis's charges by 2.")
                .passive(Passive.effect("minecraft:night_vision", 0))
                .passive(Passive.resist("minecraft:magic", 0.4))
                .passive(Passive.resist("minecraft:indirect_magic", 0.4))
                .passive(Passive.resist("tensura:magic_generic", 0.4))
                .passive(Passive.resist("tensura:holy_damage", 0.4))
                .passive(Passive.resist("tensura:light_elemental", 0.4))
                .passive(Passive.resist("minecraft:wither", 0.4))
                .passive(Passive.resist("tensura:darkness_elemental", 0.4))
                .passive(Passive.resist("tensura:curse", 0.4))
                .passive(Passive.flag(Wave4Upkeep.FLAG_SEA))
                .passive(Passive.flag(FLAG_OVERSEERS_MIGHT))
                .act(Act.of("star_eyed", "Star-Eyed", Wave4MiscActs::starEyed).cooldown(200).mana(3).usesPerDay(1));
    }

    private static RelicDef dreamer() {
        return RelicDef.of("pendant_of_the_dreamer", "Pendant of the Dreamer", Grade.RARE, RelicSlot.NECK)
                .attune().look(Look.PENDANT).palette(0x6B4B2A, 0x2F2216, 0x3FA9FF)
                .desc("Six charges that return at midnight: breathe water, grow gills and webbing, or reach into a creature's mind.")
                .passive(Passive.flag(Wave4Upkeep.FLAG_DREAMER))
                .act(Act.of("blessing_of_the_deep", "Blessing of the Deep", Wave4ChargeActs::deep).cooldown(60))
                .act(Act.of("features_of_the_ancient", "Features of the Ancient", Wave4ChargeActs::gills).cooldown(60))
                .act(Act.of("force_of_mhighraine", "Force of Mhigh'Raine", Wave4ChargeActs::mind).cooldown(100));
    }

    private static RelicDef beastMasters() {
        return RelicDef.of("beast_masters_amulet", "Beast Masters Amulet", Grade.RARE, RelicSlot.NECK)
                .attune().look(Look.AMULET).palette(0x73777F, 0x2B2E34, 0xB8C0CC)
                .desc("Holds up to ten points of creatures safely: your tamed beasts freely, wild ones once wounded to half health.")
                .act(Act.of("store_creature", "Store Creature", Wave4BeastActs::store).cooldown(40))
                .act(Act.of("release_creature", "Release Creature", Wave4BeastActs::release).cooldown(40));
    }

    // ---------------------------------------------------------------- the game-bus listeners (called once from the mod constructor)
    public static void hooks() {
        if (hooked) return;
        hooked = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Wave4Handlers::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Wave4Handlers::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave4Handlers::onHeal);
        NeoForge.EVENT_BUS.addListener(Wave4Handlers::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Wave4Handlers::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave4Handlers::onLogout);
        NeoForge.EVENT_BUS.addListener(Wave4Handlers::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(Wave4Handlers::onRespawn);
        NeoForge.EVENT_BUS.addListener(Wave4Handlers::onServerStopping);
    }
}
