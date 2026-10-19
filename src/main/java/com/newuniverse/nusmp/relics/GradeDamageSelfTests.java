package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * The self-test {@code relic.grade_damage} (run by the CI server smoke test, see {@link SelfTests}): the real attack damage on the registered item of every
 * main-hand relic weapon lies inside the damage range of its grade ({@link GradeDamage}, docs/grade_damage.md), and a tier ascension of each lands inside the
 * range of the effective grade. Elsdocia (its own 400) is the one exception.
 */
public final class GradeDamageSelfTests {
    private GradeDamageSelfTests() {}

    public static void register() { SelfTests.register("relic.grade_damage", server -> run()); }

    /** Total attack damage on an item's default attribute modifiers (the player's own 1 included), or -1 if it has none. */
    static double attackOf(ItemStack stack) {
        ItemAttributeModifiers mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods == null) return -1;
        double sum = 0;
        boolean any = false;
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute().value() == Attributes.ATTACK_DAMAGE.value() && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                sum += e.modifier().amount();
                any = true;
            }
        }
        return any ? sum + 1.0 : -1;
    }

    static SelfTests.Result run() {
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (RelicDef d : Relics.all()) {
            if (d.slot() != RelicSlot.MAIN_HAND || GradeDamage.exemptId(d.id())) continue;
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(Relics.itemId(d)));
            double dmg = attackOf(new ItemStack(item));
            if (dmg < 0) continue;                                                   // a shield or tool with no blow of its own
            checked++;
            int[] r = GradeDamage.range(d.grade().ordinal());
            if (dmg < r[0] || dmg > r[1]) bad.add(d.id() + " " + dmg + " not in " + r[0] + "-" + r[1]);
            String type = GradeDamageItems.weaponTypeOf(d);
            for (int e = d.grade().ordinal() + 1; e < 7; e++) {
                double up = GradeDamage.raised(d.grade().ordinal(), e, dmg, type);
                int[] er = GradeDamage.range(e);
                if (up < er[0] || up > er[1]) bad.add(d.id() + " raised to " + e + " = " + up);
            }
        }
        if (checked == 0) return SelfTests.Result.fail("no main-hand relic weapon was found");
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " out of range, first: " + bad.get(0));
        return SelfTests.Result.ok(checked + " relic weapons inside their grade range, tiers included");
    }
}
