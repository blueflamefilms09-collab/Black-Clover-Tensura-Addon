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
 * Tensura engravings on this mod's armour (CLAUDE.md rule 6, in the spirit of {@link WeaponEngravings}): the robe sets of the Magic Knights come engraved with the Tensura
 * enchantments that fit their lore (protection against magic, elemental resistance, vitality, magic capacity ...). The chest piece carries the whole list of its set, the leggings
 * its first two engravings, the hood and the boots the first one. An armour stack is engraved once, the first time it sits in a player's inventory, and marked with the same
 * "NUEngraved" flag as the weapons, so a grindstone or a re-engrave is not undone; levels above an engraving's maximum are capped and an id Tensura does not have is skipped.
 * The armour relics that name no engravings of their own get them from {@link #RELICS}; a relic def that names some ({@code RelicDef#engrave}) is engraved by {@code relics.RelicEngravings}.
 */
public final class ArmorEngravings {
    private ArmorEngravings() {}

    record Engraving(String id, int level) {}

    static Engraving e(String id, int level) { return new Engraving(id, level); }

    /** Set id (the chest piece's registry path) -> its engravings, most characteristic first. */
    static final Map<String, List<Engraving>> SETS = Map.ofEntries(
            Map.entry("magic_knight_robe_junior", List.of(e("magic_capacity", 1), e("sturdy", 1))),                                    // a student's robe: a little more mana, a little tougher
            Map.entry("magic_knight_robe_senior", List.of(e("magic_capacity", 2), e("magic_protection", 1), e("sturdy", 1))),         // a captain's robe
            Map.entry("golden_dawn_mantle", List.of(e("holy_coat", 1), e("magic_protection", 2), e("vitality", 1))),                  // light and stars: a holy ward
            Map.entry("black_bull_robe", List.of(e("sturdy", 2), e("energy_protection", 1), e("vitality", 1))),                       // rough, hard-wearing, hard to put down
            Map.entry("silver_eagle_cloak", List.of(e("magic_protection", 2), e("energy_protection", 1), e("swift", 1))),             // mercury and steel: quick and warded
            Map.entry("crimson_lion_mantle", List.of(e("elemental_resistance", 2), e("sturdy", 1), e("vigor", 1))),                   // flame and magma: a burning mane
            Map.entry("coral_peacock_robe", List.of(e("elemental_resistance", 1), e("breathing_support", 1), e("vitality", 1))),      // water and mist
            Map.entry("heart_kingdom_uniform", List.of(e("magic_capacity", 2), e("restoration", 1), e("magic_protection", 1))),       // a healer's and a spender's uniform
            Map.entry("diamond_mage_coat", List.of(e("magic_capacity", 1), e("vigor", 1), e("sturdy", 2))),                           // hard as a crystal, quick to recover
            Map.entry("spade_war_coat", List.of(e("sturdy", 2), e("magic_protection", 1), e("severance_protection", 1))),             // a war coat of the Spade Kingdom
            Map.entry("devil_bound_coat", List.of(e("spiritual_protection", 2), e("magic_protection", 2), e("magicule_absorption", 1))));   // bound to a devil: it drinks what it is struck with

    /**
     * The armour relics (head, chest and feet defs of the relic catalog) by item id: the relic defs of waves 3, 6, 7, 14 and 15 name no engravings of their own, so this table gives
     * each the armour enchantments of its lore. A def that does name some ({@code RelicDef#engrave}) is engraved first by {@code relics.RelicEngravings} and the shared flag keeps this
     * table off it.
     */
    static final Map<String, List<Engraving>> RELICS = Map.ofEntries(
            Map.entry("gold_circlet", List.of(e("magic_protection", 2), e("magic_capacity", 1))),
            Map.entry("silver_circlet", List.of(e("holy_coat", 1), e("magic_protection", 1))),
            Map.entry("steel_circlet", List.of(e("sturdy", 2), e("magic_protection", 1))),
            Map.entry("tiny_duck_crown", List.of(e("vitality", 1), e("breathing_support", 1))),
            Map.entry("unicorn_crown", List.of(e("holy_coat", 2), e("restoration", 1), e("vitality", 1))),
            Map.entry("dark_unicorn_crown", List.of(e("spiritual_protection", 2), e("magicule_absorption", 1), e("magic_protection", 1))),
            Map.entry("circlet_of_the_phoenix", List.of(e("restoration", 2), e("elemental_resistance", 2), e("vitality", 1))),
            Map.entry("crown_of_the_conjurer", List.of(e("magic_capacity", 2), e("magic_protection", 1))),
            Map.entry("diadem_of_fabulous_truthiness", List.of(e("magic_protection", 1), e("vigor", 1))),
            Map.entry("ruby_boots_of_planestriding", List.of(e("swift", 1), e("spiritual_protection", 1))),
            Map.entry("mudwalkers", List.of(e("sturdy", 1), e("vigor", 1))),
            Map.entry("boots_of_the_high_ground", List.of(e("sturdy", 2), e("vigor", 1))),
            Map.entry("waterwalkers", List.of(e("breathing_support", 1), e("swift", 1))),
            Map.entry("sandals_of_sneaking", List.of(e("swift", 1), e("vigor", 1))),
            Map.entry("dragon_fire_boots", List.of(e("elemental_resistance", 2), e("swift", 1))),
            Map.entry("dante_gravity_crown", List.of(e("sturdy", 2), e("magic_protection", 2), e("energy_protection", 1))),
            Map.entry("skycrystal_armor", List.of(e("magic_protection", 2), e("elemental_resistance", 1), e("energy_protection", 1))),
            Map.entry("slime_armour", List.of(e("sturdy", 1), e("restoration", 1), e("energy_protection", 1))),
            Map.entry("armor_of_the_defiant_dawn", List.of(e("holy_coat", 2), e("vitality", 2), e("sturdy", 1))),
            Map.entry("suit_of_the_astral_overseer", List.of(e("magic_capacity", 2), e("magic_protection", 2), e("spiritual_protection", 1))),
            Map.entry("shadow_robes", List.of(e("spiritual_protection", 1), e("swift", 1), e("magic_protection", 1))),
            Map.entry("mistwalker_robe", List.of(e("breathing_support", 1), e("elemental_resistance", 1))),
            Map.entry("nekets_practical_mage_robe", List.of(e("magic_capacity", 1), e("magic_protection", 1))),
            Map.entry("armor_of_plot", List.of(e("sturdy", 3), e("magic_protection", 2), e("severance_protection", 2))),
            Map.entry("zar_sancuis_plate_of_the_red_prophet", List.of(e("sturdy", 3), e("spiritual_protection", 2), e("magic_protection", 2))),
            Map.entry("skitchs_smuggling_coat", List.of(e("swift", 1), e("sturdy", 1))),
            Map.entry("armour_of_the_stone_sentinel", List.of(e("sturdy", 2), e("severance_protection", 1))),
            Map.entry("starstudded_leathers", List.of(e("swift", 1), e("vigor", 1), e("magic_protection", 1))),
            Map.entry("coat_of_the_duelist", List.of(e("swift", 2), e("severance_protection", 1))),
            Map.entry("suit_of_the_golden_shadow", List.of(e("swift", 2), e("spiritual_protection", 1), e("magic_protection", 1))),
            Map.entry("armor_of_the_lionheart", List.of(e("sturdy", 2), e("vitality", 2), e("holy_coat", 1))),
            Map.entry("armor_of_the_beheaded", List.of(e("sturdy", 2), e("severance_protection", 2), e("spiritual_protection", 1))),
            Map.entry("false_dragon_armor", List.of(e("elemental_resistance", 2), e("sturdy", 2), e("energy_protection", 1))),
            Map.entry("soul_armor", List.of(e("spiritual_protection", 2), e("magicule_absorption", 1))),
            Map.entry("arcane_conduction_armor", List.of(e("magic_capacity", 2), e("magicule_absorption", 1))),
            Map.entry("delphynes_cursed_armor", List.of(e("sturdy", 2), e("spiritual_protection", 1), e("energy_protection", 1))),
            Map.entry("arcanists_circlet", List.of(e("magic_capacity", 1), e("magic_protection", 1))),
            Map.entry("prides_crown", List.of(e("magic_protection", 1), e("vitality", 1))),
            Map.entry("dreamwalker_cowl", List.of(e("spiritual_protection", 1), e("magic_protection", 1))),
            Map.entry("captains_tricorn", List.of(e("sturdy", 1), e("swift", 1))),
            Map.entry("godking_crown", List.of(e("magic_protection", 3), e("spiritual_protection", 3), e("vitality", 2), e("sturdy", 2))),
            Map.entry("boots_of_burglery", List.of(e("swift", 2), e("vigor", 1))));

    /** The engravings of one piece: the whole list for the chest piece, two for the leggings, one for the hood and the boots; empty if the item is not of a set. */
    static List<Engraving> forItem(String path) {
        // 1.112.0 (lag audit): the answer for an item path never changes, and the unlisted case walked every set with string concatenations for every item of every inventory every second
        return FOR_ITEM.computeIfAbsent(path, ArmorEngravings::forItem0);
    }

    private static final Map<String, List<Engraving>> FOR_ITEM = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Read-only view for the Engravement Codex (engraving/codex/CodexGather): item path -> its engravings as "id@level", for every set piece (chest, leggings, hood, boots, as {@link #forItem}
     * gives them) and every armour relic. A path that is not a registered item is simply never found by the caller. Built on first use.
     */
    public static Map<String, List<String>> codexTable() {
        Map<String, List<String>> t = CODEX_TABLE;
        if (t == null) {
            java.util.LinkedHashMap<String, List<String>> m = new java.util.LinkedHashMap<>();
            java.util.ArrayList<String> paths = new java.util.ArrayList<>(SETS.keySet());
            for (String set : SETS.keySet()) { paths.add(set + "_leggings"); paths.add(set + "_hood"); paths.add(set + "_boots"); }
            paths.addAll(RELICS.keySet());
            for (String p : paths) {
                java.util.ArrayList<String> specs = new java.util.ArrayList<>();
                for (Engraving g : forItem(p)) specs.add(g.id() + "@" + g.level());
                if (!specs.isEmpty()) m.put(p, List.copyOf(specs));
            }
            CODEX_TABLE = t = java.util.Collections.unmodifiableMap(m);
        }
        return t;
    }

    private static volatile Map<String, List<String>> CODEX_TABLE;

    private static List<Engraving> forItem0(String path) {
        List<Engraving> all = SETS.get(path);
        if (all != null) return all;
        all = RELICS.get(path);
        if (all != null) return all;
        for (Map.Entry<String, List<Engraving>> s : SETS.entrySet()) {
            String set = s.getKey();
            if (path.equals(set + "_leggings")) return s.getValue().subList(0, Math.min(2, s.getValue().size()));
            if (path.equals(set + "_hood") || path.equals(set + "_boots")) return s.getValue().subList(0, 1);
        }
        return List.of();
    }

    /** Engraves a stack of one of this mod's robe pieces or armour relics if it hasn't been yet; true if it changed. */
    public static boolean engrave(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"nusmp".equals(key.getNamespace())) return false;
        List<Engraving> list = forItem(key.getPath());
        if (list.isEmpty()) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains(WeaponEngravings.FLAG)) return false;                 // 1.112.0 (lag audit): no copy of the tag every second
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (Engraving g : list) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("tensura", g.id());
            Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id));
            if (h.isEmpty()) continue;
            int level = Math.max(1, Math.min(g.level(), h.get().value().getMaxLevel()));
            if (stack.getEnchantments().getLevel(h.get()) < level) stack.enchant(h.get(), level);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> t.putBoolean(WeaponEngravings.FLAG, true));
        return true;
    }

    /** Server player tick: once a second, engrave any of this mod's robe pieces and armour relics in the inventory that aren't yet. */
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_ENGRAVINGS_ARMOR = com.newuniverse.nusmp.perf.PerfProbe.section("s.engravings.armor");   // 1.99.0: performance probe (off by default: one flag read)

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(event);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_ENGRAVINGS_ARMOR, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p instanceof ServerPlayer sp) || sp.tickCount % 20 != 13) return;
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) engrave(inv.getItem(i), sp.registryAccess());
    }
}
