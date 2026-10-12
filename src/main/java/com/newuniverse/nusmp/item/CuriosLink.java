package com.newuniverse.nusmp.item;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 1.15.0: the weapons work as Curios items.
 * <ul>
 *   <li><b>Slots</b> are data: data/curios/tags/item/{curio,belt,back,hands,charm}.json list the weapons, so Curios (when it is installed)
 *       lets them be socketed: blades on the belt, long arms and the shield on the back, the Hand of Providence in the hands slot, the Brooch
 *       Pin as a charm. Nothing here needs Curios to be present.</li>
 *   <li><b>Carried passives</b>: with Curios loaded, a weapon worn in a Curios slot (and not also in a hand) grants its small sheathed bonus,
 *       listed in {@link #PASSIVES}. The Curios inventory is read by reflection (no compile dependency on the API: the build cannot reach its
 *       maven), and every failure just means "nothing is worn": the weapons stay fully usable in hand.</li>
 * </ul>
 */
public final class CuriosLink {
    private CuriosLink() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static Boolean loaded;
    private static boolean broken;
    private static Method getInventory, getEquipped;

    private static boolean loaded() {
        if (loaded == null) {
            ModList list = ModList.get();
            if (list == null) return false;
            loaded = list.isLoaded("curios");
        }
        return loaded;
    }

    // ---------------------------------------------------------------- reading the Curios inventory
    /** Every stack worn in a Curios slot of this entity (empty without Curios, or if its API is not what was expected). */
    public static List<ItemStack> equipped(LivingEntity e) {
        if (broken || !loaded()) return List.of();
        try {
            if (getInventory == null) {
                getInventory = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
                getEquipped = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler").getMethod("getEquippedCurios");
            }
            Object opt = getInventory.invoke(null, e);
            if (!(opt instanceof Optional<?> o) || o.isEmpty()) return List.of();
            if (!(getEquipped.invoke(o.get()) instanceof IItemHandler h)) return List.of();
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        } catch (Throwable t) {
            broken = true;
            LOG.warn("[nusmp] Curios is present but its inventory could not be read ({}); weapons worn in Curios slots give no passive", t.toString());
            return List.of();
        }
    }

    /** Is a weapon of this kind worn in a Curios slot? */
    public static boolean carries(LivingEntity e, Kind k) {
        for (ItemStack s : equipped(e)) if (s.getItem() instanceof MagicWeaponItem w && w.kind == k) return true;
        return false;
    }

    // ---------------------------------------------------------------- the sheathed passives
    /** One carried bonus: an attribute change (the amount is a share for the multiplied operations) and its tooltip text. */
    private record Pass(Holder<Attribute> attr, double amount, Operation op, String text) {}

    private static final Map<Kind, Pass> PASSIVES = new EnumMap<>(Kind.class);

    private static void put(Kind k, Holder<Attribute> a, double amount, Operation op, String text) { PASSIVES.put(k, new Pass(a, amount, op, text)); }

    static {
        Operation add = Operation.ADD_VALUE, pct = Operation.ADD_MULTIPLIED_BASE;
        put(Kind.DEMON_SLASHER_KATANA, Attributes.MOVEMENT_SPEED, 0.04, pct, "Ready stance: +4% speed");
        put(Kind.MIASMA_KATANA, Attributes.MOVEMENT_SPEED, 0.04, pct, "Shrouded steps: +4% speed");
        put(Kind.SPELL_FORGED_RAPIER, Attributes.ENTITY_INTERACTION_RANGE, 0.3, add, "Long reach: +0.3 reach");
        put(Kind.SEVERING_GREATSWORD, Attributes.ARMOR_TOUGHNESS, 1, add, "Heavy guard: +1 armour toughness");
        put(Kind.DEMON_SLAYER, Attributes.ARMOR, 2, add, "Anti-magic aegis: +2 armour");
        put(Kind.DEMON_DWELLER, Attributes.ARMOR_TOUGHNESS, 1, add, "Anti-magic guard: +1 armour toughness");
        put(Kind.DEMON_DESTROYER, Attributes.ARMOR_TOUGHNESS, 1, add, "Anti-magic guard: +1 armour toughness");
        put(Kind.LICHT_DWELLER, Attributes.MAX_HEALTH, 2, add, "Licht's light: +2 health");
        put(Kind.LICHT_DESTROYER, Attributes.MAX_HEALTH, 2, add, "Licht's light: +2 health");
        put(Kind.RIMEHEART, Attributes.KNOCKBACK_RESISTANCE, 0.1, add, "Rooted in frost: +10% knockback resistance");
        put(Kind.LAST_WORD, Attributes.LUCK, 1, add, "The last word: +1 luck");
        put(Kind.ELSDOCIA, Attributes.MAX_HEALTH, 4, add, "Legacy of the Wizard Kings: +4 health");
        put(Kind.QUICKLINGS_IVY, Attributes.MOVEMENT_SPEED, 0.04, pct, "Quickling's grace: +4% speed");
        put(Kind.MAGEBANE_LONGSWORD, Attributes.ARMOR_TOUGHNESS, 1, add, "Magebane guard: +1 armour toughness");
        put(Kind.PRISM_BLADE, Attributes.LUCK, 1, add, "Spectrum light: +1 luck");
        put(Kind.THUNDERCLEAVE, Attributes.ATTACK_DAMAGE, 1, add, "Thunder in the bones: +1 attack damage");
        put(Kind.BLOODBOUND_KATANA, Attributes.ATTACK_DAMAGE, 1, add, "Blood-bound: +1 attack damage");
        put(Kind.PERMAFROST_SABRE, Attributes.KNOCKBACK_RESISTANCE, 0.1, add, "Permafrost: +10% knockback resistance");
        put(Kind.CYCLONE_RAPIER, Attributes.MOVEMENT_SPEED, 0.05, pct, "Cyclone step: +5% speed");
        put(Kind.BOLT_THROWER, Attributes.LUCK, 0.5, add, "Sure shot: +0.5 luck");
        put(Kind.CROSSBOW_OF_SWIFT_DEATH, Attributes.LUCK, 0.5, add, "Swift death: +0.5 luck");
        put(Kind.CLUB_OF_ROT, Attributes.MAX_HEALTH, 2, add, "Hardy rot: +2 health");
        put(Kind.DOOM_HAMMER, Attributes.KNOCKBACK_RESISTANCE, 0.15, add, "Immovable: +15% knockback resistance");
        put(Kind.MAGEBANE_SHIELD, Attributes.ARMOR, 1, add, "Magic damage taken is cut by a third (a half with the Magebane Longsword in a hand); +1 armour");
        put(Kind.WEAPON_OF_RESURGENCE, Attributes.MAX_HEALTH, 2, add, "Resurgent: +2 health");
        put(Kind.BROOCH_PIN, Attributes.LUCK, 1, add, "Lucky pin: +1 luck");
        put(Kind.BUTCHER_CLEAVER, Attributes.LUCK, 0.5, add, "A cook's luck: +0.5 luck");
        put(Kind.HAND_OF_PROVIDENCE, Attributes.MAX_HEALTH, 4, add, "Providence: +4 health");
    }

    private static ResourceLocation id(Kind k) { return ResourceLocation.fromNamespaceAndPath("nusmp", "curio_" + k.name().toLowerCase(java.util.Locale.ROOT)); }

    /** The tooltip line of a weapon that can be worn. */
    static void tooltip(Kind k, List<Component> tip) {
        Pass p = PASSIVES.get(k);
        if (p == null) return;
        tip.add(Component.literal("Worn in a Curios slot: " + p.text()).withStyle(ChatFormatting.DARK_GREEN));
    }

    /** Every second: bring each weapon's carried bonus in line with what is worn (and not already in a hand, where its own modifiers apply). */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 13) return;
        Set<Kind> worn = EnumSet.noneOf(Kind.class);
        for (ItemStack s : equipped(p)) if (s.getItem() instanceof MagicWeaponItem w) worn.add(w.kind);
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) if (s.getItem() instanceof MagicWeaponItem w) worn.remove(w.kind);
        for (Map.Entry<Kind, Pass> e : PASSIVES.entrySet()) {
            AttributeInstance inst = p.getAttribute(e.getValue().attr());
            if (inst == null) continue;
            ResourceLocation id = id(e.getKey());
            boolean has = inst.hasModifier(id);
            if (worn.contains(e.getKey())) {
                if (!has) inst.addTransientModifier(new AttributeModifier(id, e.getValue().amount(), e.getValue().op()));
            } else if (has) inst.removeModifier(id);
        }
    }
}
