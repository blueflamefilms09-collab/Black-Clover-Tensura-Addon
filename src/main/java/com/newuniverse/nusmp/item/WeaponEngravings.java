package com.newuniverse.nusmp.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 0.50: Tensura engravings on this mod's weapons. Tensura Reincarnated's engravings are its enchantments (the
 * {@code #tensura:engraving} tag: severance, barrier piercing, holy weapon, magic interference ...); every weapon here comes
 * engraved with the ones that fit its lore. Owner rule (CLAUDE.md): every new or reworked weapon gets its engravings listed here.
 * <p>
 * A weapon is engraved once, the first time it sits in a player's inventory (crafted, given, looted or taken from the creative
 * tab), and the stack is marked "NUEngraved" so a grindstone or a re-engrave is not undone. A level above an engraving's max is
 * capped; an id Tensura doesn't have (an older or newer Tensura) is skipped.
 */
public final class WeaponEngravings {
    private WeaponEngravings() {}

    record Engraving(String id, int level) {}

    static Engraving e(String id, int level) { return new Engraving(id, level); }

    /** Item id (nusmp:) -> its engravings. */
    static final Map<String, List<Engraving>> ENGRAVINGS = Map.ofEntries(
            Map.entry("demon_slasher_katana", List.of(e("nusmp:anti_magic", 1), e("barrier_piercing", 2), e("swift", 1))), // Asta's: cuts through magic
            Map.entry("miasma_infused_katana", List.of(e("severance", 2), e("enervation", 1))),             // Yami: dimension-cutting darkness
            Map.entry("spell_forged_rapier", List.of(e("swift", 2), e("magic_weapon", 1))),                 // spatial edge, spell-forged
            Map.entry("severing_greatsword", List.of(e("severance", 2), e("crushing", 1))),                 // Sword Rain, a heavy cleaver
            Map.entry("demon_slayer_sword", List.of(e("nusmp:anti_magic", 1), e("barrier_piercing", 3), e("magic_interference", 1))), // the Genesis anti-magic blade
            Map.entry("demon_dweller_sword", List.of(e("nusmp:anti_magic", 1), e("barrier_piercing", 2), e("energy_steal", 1))), // drinks the magic it cuts
            Map.entry("demon_destroyer_sword", List.of(e("nusmp:anti_magic", 1), e("magic_interference", 1), e("sturdy", 2))), // undoes spell effects
            Map.entry("licht_dweller_sword", List.of(e("holy_weapon", 2), e("elemental_boost", 1))),        // Licht's white blades
            Map.entry("licht_destroyer_sword", List.of(e("holy_weapon", 2), e("barrier_piercing", 1))),
            Map.entry("rimeheart_runeblade", List.of(e("elemental_boost", 2), e("magicule_absorption", 1))), // a cryo mana lattice
            Map.entry("otherworld_trident", List.of(e("soul_eater", 1), e("elemental_boost", 1))),          // Zagred's otherworld weapon
            Map.entry("magic_tool_sword", List.of(e("magic_weapon", 1))),                                   // mana-forged tools
            Map.entry("magic_tool_spear", List.of(e("magic_weapon", 1))),
            Map.entry("magic_tool_bow", List.of(e("magic_weapon", 1))),
            Map.entry("last_word", List.of(e("barrier_piercing", 2), e("severance", 1))),                   // 0.52: Zagred's quill-blade
            Map.entry("elsdocia", List.of(e("nusmp:legacy_of_the_wizard_kings", 1), e("magic_interference", 2), e("magicule_absorption", 2), e("barrier_piercing", 1))));

    static final String FLAG = "NUEngraved";

    /** Engraves a stack of one of this mod's weapons if it hasn't been yet; true if it changed. */
    public static boolean engrave(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"nusmp".equals(key.getNamespace())) return false;
        List<Engraving> list = ENGRAVINGS.get(key.getPath());
        if (list == null) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.copyTag().getBoolean(FLAG)) return false;
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (Engraving g : list) {
            ResourceLocation id = g.id().contains(":") ? ResourceLocation.parse(g.id())
                    : ResourceLocation.fromNamespaceAndPath("tensura", g.id());
            Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id));
            if (h.isEmpty()) continue;
            int level = Math.max(1, Math.min(g.level(), h.get().value().getMaxLevel()));
            if (stack.getEnchantments().getLevel(h.get()) < level) stack.enchant(h.get(), level);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> t.putBoolean(FLAG, true));
        return true;
    }

    /** Server player tick: once a second, engrave any of this mod's weapons in the inventory that aren't yet. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (!(p instanceof ServerPlayer sp) || sp.tickCount % 20 != 7) return;
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) engrave(inv.getItem(i), sp.registryAccess());
    }
}
