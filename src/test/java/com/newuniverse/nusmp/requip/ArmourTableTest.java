package com.newuniverse.nusmp.requip;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Requip: The Knight is one table ({@link Armour}, {@link ArmourSets}) plus the arithmetic of {@link RequipRules}; the skill, the painter and the documentation all read it. The saved skill keeps the
 * worn armour as an ordinal, so the order is pinned (append only); every number a player can feel (what an armour adds, what it takes off a hit, what it costs, how its pieces are staggered) is pinned
 * to a sane range; the pieces obey the Djinn Full Equip's rules (a stagger of 0 to 12 ticks, a fly-in of 2 to 14 pixels); and the modifier map, the resistances, the reflection and the costs are checked
 * as numbers instead of by hand in game.
 */
class ArmourTableTest {
    private static final List<String> ORDER = List.of("heart_kreuz", "heavens_wheel", "flame_empress", "lightning_empress", "sea_empress", "black_wing", "clear_heart",
            "nakagami", "grand_chariot", "adamantine", "purgatory", "flight_armor", "wind_god", "fairy_armor");

    @Test
    void theArmoursKeepTheirSavedOrderAndTheSkillHasADismissModeAfterThem() {
        Armour[] all = Armour.values();
        assertEquals(ORDER.size(), all.length);
        for (int i = 0; i < ORDER.size(); i++) assertEquals(ORDER.get(i), all[i].id(), "armour " + i);
        assertEquals(all.length, RequipRules.DISMISS_MODE);
        assertEquals(all.length + 1, RequipRules.MODES);
        assertNull(Armour.byOrdinal(RequipRules.DISMISS_MODE));
        assertNull(Armour.byOrdinal(-1));
        assertEquals(Armour.NAKAGAMI, Armour.byId("NAKAGAMI"));
    }

    @Test
    void everyArmourHasItsOwnIdThemeAndTwoDistinctAttacks() {
        Set<String> ids = new HashSet<>(), attacks = new HashSet<>();
        for (Armour a : Armour.values()) {
            assertTrue(ids.add(a.id()), "duplicate id " + a.id());
            assertEquals("erza_" + a.id(), a.geoKey());
            assertFalse(a.displayName().isBlank());
            assertTrue(a.theme() != 0 && a.glow() != 0);
            for (Armour.Attack atk : a.attacks()) {
                assertTrue(attacks.add(atk.id()), "duplicate attack id " + atk.id());
                assertFalse(atk.name().isBlank());
                assertTrue(atk.cooldown() >= 40 && atk.cooldown() <= 1200, atk.id() + " cooldown " + atk.cooldown());
                assertTrue(atk.cost() > 0 && atk.cost() <= 0.30, atk.id() + " cost " + atk.cost());
                assertTrue(atk.selfDamage() >= 0 && atk.selfDamage() < 0.5, atk.id() + " self damage");
                if (atk.kind() == Armour.Kind.VOLLEY || atk.kind() == Armour.Kind.FLURRY) assertTrue(atk.count() > 0, atk.id() + " needs a count");
                if (atk.kind() != Armour.Kind.GUARD) assertTrue(atk.range() > 0, atk.id() + " needs a range");
                if (atk.kind() != Armour.Kind.GUARD) assertTrue(atk.raw() > 0, atk.id() + " needs damage");
            }
        }
    }

    @Test
    void theNumbersStayInAFairRange() {
        for (Armour a : Armour.values()) {
            Armour.Stats s = a.stats();
            assertTrue(s.speed() >= -0.2 && s.speed() <= 0.6, a.id() + " speed " + s.speed());
            assertTrue(s.attack() >= 0 && s.attack() <= 12, a.id() + " attack");
            assertTrue(s.armor() >= 0 && s.armor() <= 20, a.id() + " armor");
            assertTrue(s.toughness() >= 0 && s.toughness() <= 10, a.id() + " toughness");
            assertTrue(s.attackSpeed() >= -0.3 && s.attackSpeed() <= 0.6, a.id() + " attack speed");
            assertTrue(s.knockbackResistance() >= 0 && s.knockbackResistance() <= 1.0, a.id() + " knockback");
            for (Armour.DamageClass c : Armour.DamageClass.values()) {
                double r = a.resist().of(c);
                assertTrue(r >= 0 && r <= RequipRules.MAX_REDUCTION, a.id() + " resists " + c + " " + r);
            }
            assertTrue(a.cost().equip() >= 0.01 && a.cost().equip() <= 0.2, a.id() + " equip cost");
            assertTrue(a.cost().upkeep() >= 0.001 && a.cost().upkeep() <= 0.02, a.id() + " upkeep");
            assertTrue(a.unlock() >= 0 && a.unlock() <= 0.5, a.id() + " unlock");
            assertTrue(a.flight() == Armour.Flight.NONE ? a.thrust() == 0 : a.thrust() > 0, a.id() + " thrust matches its flight");
        }
    }

    @Test
    void theNamedTradeoffsAreThere() {
        assertTrue(Armour.FLAME_EMPRESS.resist().of(Armour.DamageClass.FIRE) >= 0.75, "Flame Empress resists fire");
        assertTrue(Armour.LIGHTNING_EMPRESS.resist().of(Armour.DamageClass.LIGHTNING) >= 0.75, "Lightning Empress resists lightning");
        assertTrue(Armour.SEA_EMPRESS.resist().of(Armour.DamageClass.WATER) >= 0.75, "Sea Empress resists water");
        assertTrue(Armour.CLEAR_HEART.stats().speed() >= 0.4 && Armour.CLEAR_HEART.stats().attackSpeed() >= 0.4, "Clear Heart is the fastest");
        assertTrue(Armour.CLEAR_HEART.stats().armor() <= 2, "and the thinnest");
        for (Armour a : Armour.values()) if (a != Armour.CLEAR_HEART) assertTrue(a.stats().attackSpeed() <= Armour.CLEAR_HEART.stats().attackSpeed(), a.id());
        assertTrue(Armour.ADAMANTINE.stats().armor() >= 15 && Armour.ADAMANTINE.stats().knockbackResistance() >= 1.0, "Adamantine is the wall");
        for (Armour a : Armour.values()) if (a != Armour.ADAMANTINE) assertTrue(a.stats().armor() < Armour.ADAMANTINE.stats().armor(), a.id());
        assertTrue(Armour.NAKAGAMI.cost().equip() >= 0.1 && Armour.NAKAGAMI.cost().upkeep() >= 0.01, "Nakagami is expensive");
        for (Armour a : Armour.values()) if (a != Armour.NAKAGAMI) assertTrue(a.cost().equip() < Armour.NAKAGAMI.cost().equip(), a.id());
        assertEquals(Armour.Kind.NULLIFY, Armour.NAKAGAMI.primary().kind(), "Nakagami cuts through magic");
        assertEquals(Armour.Element.ANTI_MAGIC, Armour.NAKAGAMI.secondary().element());
        assertTrue(Armour.PURGATORY.upkeepSelfDamage() > 0 && Armour.PURGATORY.primary().selfDamage() > 0 && Armour.PURGATORY.secondary().selfDamage() > Armour.PURGATORY.primary().selfDamage(), "Purgatory hurts its wearer");
        for (Armour a : Armour.values()) if (a != Armour.PURGATORY) assertTrue(a.primary().raw() < Armour.PURGATORY.primary().raw() && a.secondary().raw() < Armour.PURGATORY.secondary().raw(), a.id() + " hits less than Purgatory");
        assertTrue(Armour.GRAND_CHARIOT.primary().count() > Armour.HEAVENS_WHEEL.primary().count(), "the Grand Chariot rains more swords");
        assertTrue(Armour.GRAND_CHARIOT.primary().raw() >= Armour.HEAVENS_WHEEL.primary().raw(), "and each hits at least as hard");
        assertEquals(Armour.Kind.VOLLEY, Armour.HEAVENS_WHEEL.primary().kind());
        assertEquals(Armour.Kind.LIFT, Armour.HEAVENS_WHEEL.secondary().kind(), "telekinesis");
        for (Armour a : List.of(Armour.HEAVENS_WHEEL, Armour.BLACK_WING, Armour.GRAND_CHARIOT, Armour.FLIGHT_ARMOR, Armour.WIND_GOD, Armour.FAIRY_ARMOR))
            assertTrue(a.flight() != Armour.Flight.NONE, a.id() + " flies");
    }

    @Test
    void theArmoursWithAWeaponNameARealWeaponAndNoOtherSharesIt() {
        Set<String> used = new HashSet<>();
        for (Armour a : Armour.values()) if (a.weapon() != null) assertTrue(used.add(a.weapon()), "weapon shared: " + a.weapon());
        assertEquals(6, used.size());
        assertEquals(Set.of("flame_empress_blade", "lightning_empress_halberd", "sea_empress_trident", "black_wing_blade", "nakagami_edge", "purgatory_mace"), used);
    }

    // ------------------------------------------------------------------ the pieces and the equip animation
    @Test
    void everyPieceObeysTheDjinnFullEquipRules() {
        for (Armour a : Armour.values()) {
            List<ArmourSets.Piece> set = ArmourSets.of(a);
            assertNotNull(set, a.id());
            assertTrue(set.size() >= 7, a.id() + " has only " + set.size() + " pieces");
            Set<String> names = new HashSet<>();
            boolean helm = false, chest = false;
            for (ArmourSets.Piece p : set) {
                assertTrue(names.add(p.name()), a.id() + " repeats piece " + p.name());
                assertTrue(p.delay() >= 0f && p.delay() <= 12f, a.id() + "/" + p.name() + " delay " + p.delay());
                float far = Math.max(Math.abs(p.dx()), Math.max(Math.abs(p.dy()), Math.abs(p.dz())));
                assertTrue(far >= 2f && far <= 16f, a.id() + "/" + p.name() + " flies in from " + far + " pixels");
                if (p.clip() != null) assertEquals("idle", p.clip(), a.id() + "/" + p.name());
                if (p.anchor() == ArmourSets.Anchor.HEAD && p.name().equals("helm")) helm = true;
                if (p.anchor() == ArmourSets.Anchor.BODY && p.name().equals("chest")) chest = true;
            }
            assertTrue(helm && chest, a.id() + " needs a helm and a chest");
        }
    }

    @Test
    void thePiecesFlyInFromTheirOwnSide() {
        for (Armour a : Armour.values()) for (ArmourSets.Piece p : ArmourSets.of(a)) {
            if (p.name().endsWith("_r") && (p.anchor() == ArmourSets.Anchor.ARM_R)) assertTrue(p.dx() < 0f, a.id() + "/" + p.name() + " comes from the right");
            if (p.name().endsWith("_l") && (p.anchor() == ArmourSets.Anchor.ARM_L)) assertTrue(p.dx() > 0f, a.id() + "/" + p.name() + " comes from the left");
            if (p.anchor() == ArmourSets.Anchor.HEAD) assertTrue(p.dy() < 0f, a.id() + "/" + p.name() + " comes from above");
            if (p.anchor() == ArmourSets.Anchor.LEG_R || p.anchor() == ArmourSets.Anchor.LEG_L) assertTrue(p.dy() > 0f, a.id() + "/" + p.name() + " comes from below");
            if (p.name().equals("backdrop") || p.name().startsWith("wing") || p.name().equals("cape") || p.name().equals("tail")) assertTrue(p.dz() > 0f, a.id() + "/" + p.name() + " comes from behind");
        }
    }

    @Test
    void theShatterWindowIsTheDjinnsAndTheSwapWaitsForIt() {
        assertEquals(18, RequipRules.SHATTER_WINDOW);
        assertEquals(RequipRules.SHATTER_WINDOW, RequipRules.SWAP_GAP);
        for (Armour a : Armour.values()) assertTrue(ArmourSets.maxDelay(a) <= 12f, a.id());
        assertTrue(RequipRules.FORGE_TICKS >= 20, "the last piece (delay 12) forges for 8 ticks");
        assertTrue(RequipRules.AURA_TICKS > 3 * RequipRules.AURA_REFRESH / 2, "the aura outlives its refresh");
        assertTrue(RequipRules.AURA_REFRESH > RequipRules.SHATTER_WINDOW + RequipRules.FORGE_TICKS, "a refresh never lands inside the forge or the shatter");
        assertTrue(RequipRules.SWAP_COOLDOWN > 0 && RequipRules.SWAP_COOLDOWN <= 100, "a swap has a short cooldown");
        assertEquals(ArmourSets.allModelIds().size(), new HashSet<>(ArmourSets.allModelIds()).size(), "model ids are unique");
    }

    // ------------------------------------------------------------------ the arithmetic
    @Test
    void theModifierMapMatchesTheTableAndMasteryStrengthensIt() {
        for (Armour a : Armour.values()) {
            List<RequipRules.Mod> base = RequipRules.modifiers(a, 0.0), full = RequipRules.modifiers(a, 1.0);
            assertEquals(base.size(), full.size(), a.id());
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < base.size(); i++) {
                RequipRules.Mod m = base.get(i);
                assertTrue(seen.add(m.attribute()), a.id() + " sets " + m.attribute() + " twice");
                assertTrue(RequipRules.ATTRIBUTES.contains(m.attribute()), m.attribute());
                assertEquals("erza_requip_" + m.attribute(), m.modifierPath());
                assertTrue(Math.abs(m.amount()) > 0, a.id() + " has a zero modifier for " + m.attribute());
                double want = switch (m.attribute()) {
                    case "movement_speed" -> a.stats().speed();
                    case "attack_damage" -> a.stats().attack();
                    case "armor" -> a.stats().armor();
                    case "armor_toughness" -> a.stats().toughness();
                    case "attack_speed" -> a.stats().attackSpeed();
                    default -> a.stats().knockbackResistance();
                };
                assertEquals(want, m.amount(), 1e-9, a.id() + " " + m.attribute());
                boolean strengthened = m.attribute().equals("attack_damage") || m.attribute().equals("armor") || m.attribute().equals("armor_toughness");
                assertEquals(strengthened ? want * 1.25 : want, full.get(i).amount(), 1e-9, a.id() + " " + m.attribute() + " at full mastery");
                boolean mult = m.attribute().equals("movement_speed") || m.attribute().equals("attack_speed");
                assertEquals(mult ? RequipRules.Op.ADD_MULTIPLIED_BASE : RequipRules.Op.ADD_VALUE, m.op(), a.id() + " " + m.attribute());
            }
        }
        assertEquals(RequipRules.modifiers(Armour.HEART_KREUZ, 5.0).get(1).amount(), RequipRules.modifiers(Armour.HEART_KREUZ, 1.0).get(1).amount(), 1e-9, "mastery is clamped");
    }

    @Test
    void resistanceTakesAShareOffAHitAndTheGuardAddsToIt() {
        assertEquals(2f, RequipRules.afterResist(Armour.FLAME_EMPRESS, Armour.DamageClass.FIRE, 10f, false), 1e-5);
        assertEquals(10f, RequipRules.afterResist(Armour.CLEAR_HEART, Armour.DamageClass.FIRE, 10f, false), 1e-5);
        assertEquals(8.5f, RequipRules.afterResist(Armour.HEART_KREUZ, Armour.DamageClass.PHYSICAL, 10f, false), 1e-5);
        assertEquals(5.5f, RequipRules.afterResist(Armour.HEART_KREUZ, Armour.DamageClass.PHYSICAL, 10f, true), 1e-5);
        assertEquals(1f, RequipRules.afterResist(Armour.FLAME_EMPRESS, Armour.DamageClass.FIRE, 10f, true), 1e-5, "never more than 90 percent off");
        for (Armour a : Armour.values()) for (Armour.DamageClass c : Armour.DamageClass.values()) {
            for (boolean g : new boolean[]{false, true}) {
                float left = RequipRules.afterResist(a, c, 10f, g);
                assertTrue(left >= 1f - 1e-5 && left <= 10f + 1e-5, a.id() + " " + c + " guard " + g + " left " + left);
            }
            assertTrue(RequipRules.afterResist(a, c, 10f, true) <= RequipRules.afterResist(a, c, 10f, false) + 1e-6f, "the guard never makes it worse");
        }
    }

    @Test
    void onlyTheAdamantineReflectsAndItsStanceReflectsMore() {
        for (Armour a : Armour.values()) {
            if (a == Armour.ADAMANTINE) continue;
            assertEquals(0.0, RequipRules.reflect(a, false), 0.0, a.id());
            assertEquals(0.0, RequipRules.reflect(a, true), 0.0, a.id());
        }
        assertEquals(0.25, RequipRules.reflect(Armour.ADAMANTINE, false), 1e-9);
        assertEquals(0.70, RequipRules.reflect(Armour.ADAMANTINE, true), 1e-9);
        assertTrue(RequipRules.guardTicks(Armour.ADAMANTINE) > 0 && RequipRules.guardTicks(Armour.HEART_KREUZ) > 0);
    }

    @Test
    void costsScaleWithAShareAndASwapIsCheaperThanAFreshEquip() {
        for (Armour a : Armour.values()) {
            assertEquals(a.cost().equip(), RequipRules.equipShare(a, false), 1e-12);
            assertEquals(a.cost().equip() * 0.6, RequipRules.equipShare(a, true), 1e-12);
            assertTrue(RequipRules.equipShare(a, true) < RequipRules.equipShare(a, false));
        }
        assertTrue(RequipRules.EQUIP_FLOOR > 0 && RequipRules.UPKEEP_FLOOR > 0 && RequipRules.ATTACK_FLOOR > 0);
    }

    @Test
    void theBookGateWantsHighEpOrAMasteredUnique() {
        assertTrue(RequipRules.eligible(RequipRules.REQUIRED_EP, 0));
        assertTrue(RequipRules.eligible(RequipRules.REQUIRED_EP * 5, 0));
        assertFalse(RequipRules.eligible(RequipRules.REQUIRED_EP - 1, 0));
        assertTrue(RequipRules.eligible(RequipRules.REQUIRED_EP_WITH_UNIQUE, 1));
        assertFalse(RequipRules.eligible(RequipRules.REQUIRED_EP_WITH_UNIQUE - 1, 3));
        assertFalse(RequipRules.eligible(RequipRules.REQUIRED_EP_WITH_UNIQUE, 0));
        assertFalse(RequipRules.eligible(0, 5));
    }

    @Test
    void armoursUnlockWithMasteryAndOperatorsAreNeverLocked() {
        assertTrue(RequipRules.unlocked(Armour.HEART_KREUZ, 0.0, false));
        assertTrue(RequipRules.unlocked(Armour.CLEAR_HEART, 0.0, false));
        assertFalse(RequipRules.unlocked(Armour.PURGATORY, 0.1, false));
        assertTrue(RequipRules.unlocked(Armour.PURGATORY, Armour.PURGATORY.unlock(), false));
        assertTrue(RequipRules.unlocked(Armour.PURGATORY, 0.0, true));
        for (Armour a : Armour.values()) assertTrue(RequipRules.unlocked(a, 1.0, false), a.id() + " is open at full mastery");
    }

    @Test
    void theSwordsOfAVolleyAreSharedOutAndSelfDamageNeverKills() {
        assertEquals(10, sum(RequipRules.shareSwords(10, 3)));
        assertEquals(24, sum(RequipRules.shareSwords(24, 7)));
        assertEquals(4, RequipRules.shareSwords(10, 3)[0]);
        assertEquals(3, RequipRules.shareSwords(10, 3)[2]);
        assertEquals(0, RequipRules.shareSwords(10, 0).length);
        assertEquals(3, RequipRules.volleyTargets(3, 10, 10));
        assertEquals(10, RequipRules.volleyTargets(40, 10, 10));
        assertEquals(0, RequipRules.volleyTargets(0, 10, 10));
        assertEquals(3f, RequipRules.selfDamage(20f, 0.15), 1e-5f);
        assertEquals(5f, RequipRules.selfDamage(6f, 0.99), 1e-5f, "never below one health point");
        assertEquals(0f, RequipRules.selfDamage(1f, 0.5), 0f);
        assertEquals(0f, RequipRules.selfDamage(20f, 0.0), 0f);
        assertEquals(1.4, RequipRules.waterBonus(Armour.SEA_EMPRESS, true), 1e-9);
        assertEquals(1.0, RequipRules.waterBonus(Armour.SEA_EMPRESS, false), 1e-9);
        assertEquals(1.0, RequipRules.waterBonus(Armour.FLAME_EMPRESS, true), 1e-9);
        assertEquals(0.0, RequipRules.antiCasterBonus(0), 1e-9);
        assertEquals(0.15, RequipRules.antiCasterBonus(100_000), 1e-9);
        assertEquals(0.60, RequipRules.antiCasterBonus(1e9), 1e-9, "capped");
    }

    private static int sum(int[] a) { int n = 0; for (int v : a) n += v; return n; }
}
