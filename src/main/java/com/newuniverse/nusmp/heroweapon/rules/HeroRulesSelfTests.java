package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the owner's restrictions are the part of the hero weapons a player notices at once when they are wrong, so the CI server smoke test runs this self-test
 * ({@code hero.rules}) in the real game: the weapon-class table on real items (vanilla swords, axes, bows, crossbows, tridents, shields, maces, tools, food, the
 * 21 hero weapons and three of this mod's own), the rejection decision for stand-in wielders of a Holy and of a Seven Star Weapon, the shock (it does not kill and
 * a second one within the second does no further damage), the interference of two Holy stand-ins (both blocked when near, free when far or when one is not a Holy
 * wielder) with the weapon's experience refused while blocked and granted after, and the pure rule behind each. The stand-ins are released afterwards and the
 * blocked set is cleared.
 */
public final class HeroRulesSelfTests {
    private HeroRulesSelfTests() {}

    public static void register() { SelfTests.register("hero.rules", HeroRulesSelfTests::rules); }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static void clean(ServerPlayer p) {
        if (HeroBonds.has(p)) HeroBonds.release(p);
        p.getInventory().clearContent();
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
    }

    private static void table(List<String> bad, Item item, WeaponClass want) {
        WeaponClass got = HeroRules.classOf(new ItemStack(item));
        if (got != want) bad.add(item + " is " + got + ", expected " + want);
    }

    private static SelfTests.Result rules(MinecraftServer server) {
        if (!HeroConfig.enabled()) return ok("skipped: heroweapon.enabled is false in the config");
        ServerLevel level = server.overworld();
        FakePlayer a = SelfTests.player(level, "hero_core_r1", 0.5, 100, 0.5);
        FakePlayer b = SelfTests.player(level, "hero_core_r2", 0.5, 100, 0.5);
        clean(a);
        clean(b);
        List<String> bad = new ArrayList<>();
        String note = "";
        try {
            // ---- the class table on real items
            table(bad, Items.IRON_SWORD, WeaponClass.SWORD);
            table(bad, Items.NETHERITE_SWORD, WeaponClass.SWORD);
            table(bad, Items.DIAMOND_AXE, WeaponClass.AXE);
            table(bad, Items.BOW, WeaponClass.BOW);
            table(bad, Items.CROSSBOW, WeaponClass.BOW);
            table(bad, Items.TRIDENT, WeaponClass.SPEAR);
            table(bad, Items.SHIELD, WeaponClass.SHIELD);
            table(bad, Items.MACE, WeaponClass.BLUNT);
            table(bad, Items.NETHERITE_PICKAXE, WeaponClass.NONE);
            table(bad, Items.NETHERITE_SHOVEL, WeaponClass.NONE);
            table(bad, Items.NETHERITE_HOE, WeaponClass.NONE);
            table(bad, Items.STICK, WeaponClass.NONE);
            table(bad, Items.APPLE, WeaponClass.NONE);
            table(bad, Items.DIRT, WeaponClass.NONE);
            table(bad, Items.FISHING_ROD, WeaponClass.NONE);
            table(bad, Items.SHEARS, WeaponClass.NONE);
            table(bad, NUItems.MAGEBANE_SHIELD.get(), WeaponClass.SHIELD);
            table(bad, NUItems.BOLT_THROWER.get(), WeaponClass.BOW);
            table(bad, NUItems.DOOM_HAMMER.get(), WeaponClass.BLUNT);
            for (HeroKind k : HeroKind.values()) {
                WeaponClass got = HeroRules.classOf(HeroItems.stack(k));
                if (got != WeaponClassifier.ownClass(k)) bad.add(k + " is classed " + got + ", expected " + WeaponClassifier.ownClass(k));
            }

            // ---- the rejection decision
            ItemStack shield = new ItemStack(Items.SHIELD), sword = new ItemStack(Items.IRON_SWORD), bow = new ItemStack(Items.BOW), pick = new ItemStack(Items.NETHERITE_PICKAXE),
                    stick = new ItemStack(Items.STICK), hammer = HeroItems.stack(HeroKind.HAMMER);
            if (HeroRules.rejectedClass(a, shield) != WeaponClass.NONE) bad.add("a player with no weapon was rejected");
            boolean swordBound = HeroBonds.bind(a, HeroKind.SWORD) == null;
            if (swordBound) {
                if (HeroConfig.holyRejectsOthers()) {
                    if (HeroRules.rejectedClass(a, shield) != WeaponClass.SHIELD) bad.add("the Sword hero was not rejected by a shield");
                    if (HeroRules.rejectedClass(a, bow) != WeaponClass.BOW) bad.add("the Sword hero was not rejected by a bow");
                } else {
                    note += "; holyRejectsOthers is off, so the rejection was not exercised";
                }
                if (HeroRules.rejectedClass(a, sword) != WeaponClass.NONE) bad.add("the Sword hero was rejected by a sword");
                if (HeroRules.rejectedClass(a, pick) != WeaponClass.NONE) bad.add("the Sword hero was rejected by a pickaxe");
                if (HeroRules.rejectedClass(a, stick) != WeaponClass.NONE) bad.add("the Sword hero was rejected by a stick");
                if (HeroRules.rejectedClass(a, hammer) != WeaponClass.NONE) bad.add("the Sword hero was rejected by a Seven Star weapon");
            } else {
                note += "; the Sword could not be bound to a stand-in, so the Holy rejection was not exercised";
            }
            HeroBonds.release(a);
            if (HeroBonds.bind(a, HeroKind.HAMMER) == null) {
                if (HeroRules.rejectedClass(a, bow) != WeaponClass.NONE || HeroRules.rejectedClass(a, shield) != WeaponClass.NONE) bad.add("a Seven Star wielder was rejected");
                HeroBonds.release(a);
            }

            // ---- the shock does not kill
            if (HeroBonds.bind(a, HeroKind.SHIELD) == null) {
                a.setHealth(1.5f);
                HeroRules.shock(a);
                if (!a.isAlive() || a.getHealth() < 0.99f) bad.add("the shock took a wielder at 1.5 health to " + a.getHealth());
                float afterFirst = a.getHealth();
                if (HeroConfig.shockSlowTicks() > 0 && !a.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) bad.add("the shock did not slow the wielder");
                HeroRules.shock(a);
                if (a.getHealth() > afterFirst + 1e-4f || afterFirst - a.getHealth() > 1e-4f) bad.add("a second shock within the second changed the health from " + afterFirst + " to " + a.getHealth());
                a.setHealth(a.getMaxHealth());
                a.removeAllEffects();
                HeroBonds.release(a);
            } else {
                note += "; the Shield could not be bound to a stand-in, so the shock was not exercised";
            }

            // ---- interference
            if (!HeroConfig.repelXp()) {
                note += "; repelXp is off, so the interference was not exercised";
            } else if (HeroBonds.bind(a, HeroKind.SWORD) == null && HeroBonds.bind(b, HeroKind.SHIELD) == null) {
                a.setPos(0.5, 100, 0.5);
                b.setPos(10.5, 100, 0.5);
                Set<UUID> blocked = HeroInterference.refresh(List.of(a, b));
                if (!blocked.contains(a.getUUID()) || !blocked.contains(b.getUUID())) bad.add("two Holy stand-ins 10 blocks apart were not both blocked");
                if (!HeroInterference.blocked(a)) bad.add("blocked(player) disagrees with the refreshed set");
                long expBefore = HeroBonds.of(a).exp();
                if (HeroXp.gain(a, 5) != 0 || HeroBonds.of(a).exp() != expBefore) bad.add("a blocked wielder gained weapon experience");
                b.setPos(10000.5, 100, 0.5);
                if (!HeroInterference.refresh(List.of(a, b)).isEmpty()) bad.add("two Holy stand-ins 10000 blocks apart are still blocked");
                long gained = HeroXp.gain(a, 5);
                if (HeroConfig.expMultiplier() >= 0.2 && gained <= 0) bad.add("an unblocked wielder gained no weapon experience");
                HeroBonds.release(b);
                b.setPos(10.5, 100, 0.5);
                if (HeroBonds.bind(b, HeroKind.HAMMER) == null) {
                    if (!HeroInterference.refresh(List.of(a, b)).isEmpty()) bad.add("a Holy wielder beside a Seven Star wielder was blocked");
                }
            } else {
                note += "; two Holy Weapons could not be bound to stand-ins, so the interference was not exercised";
            }
        } finally {
            HeroInterference.refresh(List.of());
            clean(a);
            clean(b);
        }
        if (!bad.isEmpty()) return fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
        return ok("the class table, the rejection decision, the shock that does not kill and the interference of stand-in wielders all hold" + note);
    }
}
