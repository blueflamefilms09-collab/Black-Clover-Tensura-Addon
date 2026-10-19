package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.RelicArmorItem;
import com.newuniverse.nusmp.relics.RelicBearer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The armour items of this mod that are NOT relics (the Magic Knight robe sets and any future {@code ArmorItem} of the {@code nusmp} namespace) as worn models: which
 * ensemble a stack on a player stands for, whether the 3D model replaces the item's older renderer, and the empty model that makes the older renderer draw nothing.
 * The relic armour ({@code RelicArmorItem}) keeps its own path (the relic list of the layer). Client thread only.
 */
public final class ArmorItems {
    private ArmorItems() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static HumanoidModel<LivingEntity> empty;

    private static ArmorLooks.Slot slotOf(EquipmentSlot s) {
        return switch (s) {
            case HEAD -> ArmorLooks.Slot.HEAD;
            case CHEST -> ArmorLooks.Slot.CHEST;
            case LEGS -> ArmorLooks.Slot.LEGS;
            case FEET -> ArmorLooks.Slot.FEET;
            default -> null;
        };
    }

    /** Is this item one the library draws (an armour item of the mod's namespace that is not a relic)? */
    public static boolean isLibraryArmor(Item item) {
        if (!(item instanceof ArmorItem) || item instanceof RelicArmorItem) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && "nusmp".equals(id.getNamespace());
    }

    /** The ensemble of the stack worn in {@code slot}, or null (not a library armour item, the wrong slot, or no model at all). */
    public static WornLibrary.Ensemble of(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (!isLibraryArmor(item)) return null;
        ArmorLooks.Slot s = slotOf(slot);
        if (s == null || ((ArmorItem) item).getEquipmentSlot() != slot) return null;
        return WornLibrary.ofItem(BuiltInRegistries.ITEM.getKey(item).getPath(), s);
    }

    /**
     * The ensembles of what a player wears that is not a relic, appended to {@code out}: the armour, head to feet, and the Curios items of the mod that have a worn model
     * (the dungeon relics, the charter grimoires). None when the option {@code relicfx.models} is off (the older renderers then draw).
     */
    public static void collect(AbstractClientPlayer p, List<WornLibrary.Ensemble> out) {
        if (!RelicFxOptions.models()) return;
        for (EquipmentSlot slot : SLOTS) {
            WornLibrary.Ensemble e = of(p.getItemBySlot(slot), slot);
            if (e != null) out.add(e);
        }
        collectCurios(p, out);
    }

    private record CurioCache(long tick, List<WornLibrary.Ensemble> sets) {}

    private static final Map<Integer, CurioCache> CURIOS = new HashMap<>();

    /** The worn Curios items of the mod that are not relics and have a model, read through Curios at most every 10 ticks per wearer (the reads are reflective). */
    private static void collectCurios(AbstractClientPlayer p, List<WornLibrary.Ensemble> out) {
        if (!CuriosLink.available()) return;
        long now = p.level().getGameTime();
        CurioCache c = CURIOS.get(p.getId());
        if (c == null || now - c.tick() >= 10L || now < c.tick()) {
            List<WornLibrary.Ensemble> sets = new ArrayList<>(2);
            for (CuriosLink.RenderFlag f : CuriosLink.flags(p)) {
                if (!f.render()) continue;                                                 // the eye icon of the Curios screen is off
                Item item = f.stack().getItem();
                if (item instanceof RelicBearer) continue;                                 // a relic: the relic list of the layer draws it
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (!"nusmp".equals(id.getNamespace())) continue;
                WornLibrary.Ensemble e = WornLibrary.ofCurio(id.getPath());
                if (e == null && !isWeapon(item)) e = WornLibrary.fallbackCharm(id.getPath());       // a worn item with no model of its own: a small floating charm, never nothing
                if (e != null) sets.add(e);
            }
            c = new CurioCache(now, List.copyOf(sets));
            if (CURIOS.size() > 64) CURIOS.clear();
            CURIOS.put(p.getId(), c);
        }
        for (int i = 0; i < c.sets().size(); i++) out.add(c.sets().get(i));
    }

    /** Weapons worn in a Curios slot keep their sheath / held look and get no floating charm. */
    private static boolean isWeapon(Item item) {
        return item instanceof net.minecraft.world.item.TieredItem || item instanceof net.minecraft.world.item.ProjectileWeaponItem
                || item instanceof net.minecraft.world.item.ShieldItem || item instanceof net.minecraft.world.item.TridentItem;
    }

    /**
     * Should the older renderer of this item (the 3D GeckoLib robe) draw nothing because the model library draws it? Only for a player wearing it, with the option on and a
     * model available: a mob or an armour stand wearing the item still gets the older renderer, so nothing is ever invisible.
     */
    public static boolean replaces(LivingEntity wearer, ItemStack stack, EquipmentSlot slot) {
        return wearer instanceof AbstractClientPlayer && slot != null && RelicFxOptions.models() && of(stack, slot) != null;
    }

    /** A humanoid model with no cubes: handed to the armour layer in place of an older renderer, it draws nothing. */
    public static HumanoidModel<LivingEntity> emptyModel() {
        if (empty == null) {
            Map<String, ModelPart> parts = Map.of("head", blank(), "hat", blank(), "body", blank(), "right_arm", blank(), "left_arm", blank(), "right_leg", blank(),
                    "left_leg", blank());
            empty = new HumanoidModel<>(new ModelPart(List.of(), parts));
        }
        return empty;
    }

    private static ModelPart blank() { return new ModelPart(List.of(), Map.of()); }

    /** Forgets the cached model (a resource reload). */
    public static void clear() {
        empty = null;
        CURIOS.clear();
    }
}
