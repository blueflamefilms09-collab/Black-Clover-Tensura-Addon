package com.newuniverse.nusmp.labyrinth.loot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Why: a monster part that is only an ingredient is half a part. Three of them have a use no recipe grid can give, so they work at the anvil, with the vanilla anvil screen
 * and its experience cost (the NeoForge {@code AnvilUpdateEvent} lets a mod offer a result and a price for a pair of stacks without a new block):
 * <ul>
 *   <li><b>Shadow Core Shard</b> (or the System's Shadow Core) on a piece of loot gear works ONE extra stat line into it, once ({@code GearRoll.tempered}). The line is drawn from
 *       the piece's own seed, so tempering the same piece always adds the same line, and it counts like any other (its modifier, its tooltip).</li>
 *   <li><b>Golem Core</b> on anything damaged mends it completely: a stone heart that remembers the shape of the metal.</li>
 *   <li><b>Venom Sac</b> on a melee weapon coats it: its next twenty melee blows poison the target (shorter on a boss). The count is kept in the stack's custom data and
 *       counted down by {@link #onHit}.</li>
 * </ul>
 * Each costs one of the part and a few levels. Nothing is replaced: any other pair of stacks goes to the vanilla anvil untouched.
 */
public final class AnvilUses {
    private AnvilUses() {}

    private static final TagKey<Item> SHADOW_CORES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("nusmp", "loot/shadow_cores"));
    static final int VENOM_STRIKES = 20;

    public static void onAnvil(AnvilUpdateEvent e) {
        try {
            ItemStack left = e.getLeft(), right = e.getRight();
            if (left.isEmpty() || right.isEmpty()) return;
            String part = loot(right);
            if (part == null && !right.is(SHADOW_CORES)) return;
            if (right.is(SHADOW_CORES)) temper(e, left);
            else if (part.equals("golem_core")) mend(e, left);
            else if (part.equals("venom_sac")) coat(e, left);
        } catch (RuntimeException ignored) {
            // the vanilla anvil goes on
        }
    }

    private static String loot(ItemStack s) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id != null && "nusmp".equals(id.getNamespace()) && LootItems.isLoot(id.getPath()) ? id.getPath() : null;
    }

    private static void temper(AnvilUpdateEvent e, ItemStack left) {
        LootGear g = GearStacks.gearOf(left);
        GearRoll roll = GearStacks.rollOf(left);
        if (g == null || roll == null || roll.tempered()) return;
        double headroom = g.gearKind() == GearKind.WEAPON ? GearRollMath.headroom(roll.rarity(), g.weaponType()) : 0.0;
        ItemStack out = left.copy();
        GearStacks.apply(out, roll.withTemper(GearRollMath.temperLine(roll, g.gearKind(), headroom)));
        e.setOutput(out);
        e.setCost(3L);
        e.setMaterialCost(1);
    }

    private static void mend(AnvilUpdateEvent e, ItemStack left) {
        if (!left.isDamaged()) return;
        ItemStack out = left.copy();
        out.setDamageValue(0);
        e.setOutput(out);
        e.setCost(2L);
        e.setMaterialCost(1);
    }

    private static void coat(AnvilUpdateEvent e, ItemStack left) {
        if (!(left.getItem() instanceof LootWeaponItem) && !isMeleeWeapon(left)) return;
        ItemStack out = left.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> tag.putInt(GearTooltip.VENOM, VENOM_STRIKES));
        e.setOutput(out);
        e.setCost(2L);
        e.setMaterialCost(1);
    }

    /** A damageable item that deals attack damage in the main hand: a sword, an axe, a mod's blade. */
    static boolean isMeleeWeapon(ItemStack s) {
        if (!s.isDamageableItem()) return false;
        boolean[] hit = {false};
        s.getAttributeModifiers().forEach(net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND, (attr, mod) -> {
            if (attr.equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) hit[0] = true;
        });
        return hit[0];
    }

    /** A melee blow landed: a coated weapon spends one strike and poisons the target. */
    public static void onHit(LivingDamageEvent.Post e) {
        try {
            if (!(e.getSource().getEntity() instanceof LivingEntity attacker) || e.getSource().getDirectEntity() != attacker || attacker.level().isClientSide) return;
            ItemStack weapon = attacker.getMainHandItem();
            int left = GearTooltip.venom(weapon);
            if (left <= 0) return;
            final int next = left - 1;
            CustomData.update(DataComponents.CUSTOM_DATA, weapon, tag -> {
                if (next > 0) tag.putInt(GearTooltip.VENOM, next);
                else tag.remove(GearTooltip.VENOM);
            });
            LivingEntity target = e.getEntity();
            boolean boss = com.newuniverse.nusmp.balance.BalanceLaw.isBoss(target);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, boss ? 40 : 80, 0));
        } catch (RuntimeException ignored) {
            // a coated blade must never break a hit
        }
    }
}
