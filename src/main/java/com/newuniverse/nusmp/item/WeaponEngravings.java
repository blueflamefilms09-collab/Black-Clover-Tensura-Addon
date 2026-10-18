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
            Map.entry("elsdocia", List.of(e("nusmp:legacy_of_the_wizard_kings", 1), e("magic_interference", 2), e("magicule_absorption", 2), e("barrier_piercing", 1))),
            // the two crossbows of the "Magic Item Compendium I" sheet
            Map.entry("bolt_thrower", List.of(e("barrier_piercing", 2), e("severance", 1), e("elemental_boost", 1), e("magic_weapon", 1))), // bolts of force that go through wards and cut cleanly
            Map.entry("crossbow_of_swift_death", List.of(e("severance", 2), e("barrier_piercing", 1), e("enervation", 1), e("soul_eater", 1))), // a quick death: cuts, saps, takes the soul
            // the Club of Rot and the Doom Hammer of the same sheet
            Map.entry("club_of_rot", List.of(e("enervation", 2), e("soul_eater", 1), e("crushing", 1), e("energy_steal", 1))),   // rot saps strength and takes the life: a bone club that does not heal clean
            Map.entry("doom_hammer", List.of(e("crushing", 3), e("severance", 2), e("sturdy", 1), e("barrier_piercing", 1))),   // a coffin-wood haft, a black-iron head: it breaks what it strikes
            // the Magebane Shield and the Weapon of Resurgence
            Map.entry("magebane_shield", List.of(e("magic_interference", 1), e("magicule_absorption", 2), e("magic_protection", 1), e("sturdy", 2))),   // made in the Spellplague days to absorb and repel magic
            Map.entry("weapon_of_resurgence", List.of(e("holy_weapon", 2), e("restoration", 1), e("vitality", 1), e("sturdy", 1))),                       // a paladin's blade: holy, mending, bonded
            // the Brooch Pin and the Hand of Providence
            Map.entry("brooch_pin", List.of(e("severance", 2), e("barrier_piercing", 2))),                               // a pin that cuts clean and goes through wards
            Map.entry("hand_of_providence", List.of(e("holy_weapon", 2), e("restoration", 2), e("magic_protection", 1), e("sturdy", 1), e("crushing", 1))), // a god's fist: holy, mending, warding, hard-hitting
            // the Butcher Cleaver of the Living Dungeon
            Map.entry("butcher_cleaver", List.of(e("severance", 2), e("sturdy", 1), e("enervation", 1))),                 // cuts clean along the weak point and saps what it wounds
            // 0.100: the reference-sheet weapons
            Map.entry("quicklings_ivy", List.of(e("swift", 2), e("magic_weapon", 1), e("enervation", 1))),               // living ivy, poisons and slows
            Map.entry("magebane_longsword", List.of(e("magic_interference", 2), e("barrier_piercing", 2), e("magicule_absorption", 1))), // bane of mages
            Map.entry("prism_blade", List.of(e("holy_weapon", 2), e("elemental_boost", 2), e("barrier_piercing", 1))),   // a blade of refracted light
            Map.entry("thundercleave", List.of(e("elemental_boost", 2), e("crushing", 2), e("severance", 1))),           // a thunder-struck greatsword
            Map.entry("bloodbound_katana", List.of(e("energy_steal", 2), e("severance", 1), e("soul_eater", 1))),        // drinks the blood it draws
            Map.entry("permafrost_sabre", List.of(e("elemental_boost", 2), e("swift", 1), e("sturdy", 1))),              // frost-grained, one of the Boreal Blades
            Map.entry("cyclone_rapier", List.of(e("swift", 2), e("elemental_boost", 1), e("magic_weapon", 1))),        // gale-etched, one of the Boreal Blades
            // 1.62.0: the weapons of The Rising of the Shield Hero (docs/heroweapon/weapon_<name>.md section 8 gives the reason for each engraving)
            Map.entry("vassal_mirror", List.of(e("magic_interference", 2), e("magic_protection", 1), e("elemental_boost", 1))),   // glass that turns spells aside and stores their light
            Map.entry("vassal_instrument", List.of(e("swift", 1), e("magic_weapon", 1), e("enervation", 1))),   // a violin and bow: quick, magical, its dirges sap strength
            Map.entry("vassal_harpoon", List.of(e("barrier_piercing", 2), e("elemental_boost", 2), e("severance", 1))),   // a lightning harpoon that skewers through wards and fells whales
            Map.entry("vassal_sewing_set", List.of(e("severance", 1), e("restoration", 1), e("magic_interference", 1))),   // a needle that cuts clean, mends, and seals
            Map.entry("vassal_chain", List.of(e("sturdy", 2), e("enervation", 2), e("crushing", 1))),   // a heavy war chain: hard to break, tiring to be caught in, hits like a flail
            Map.entry("legendary_shield", List.of(e("sturdy", 3), e("magic_protection", 2), e("magic_interference", 1), e("holy_weapon", 1))),   // the Legendary Shield: unbreakable, wards magic, a holy weapon of the Waves
            Map.entry("legendary_sword", List.of(e("swift", 2), e("severance", 2), e("barrier_piercing", 1), e("holy_weapon", 1))),   // the Legendary Sword: fast clean strikes through wards, a holy weapon of the Waves
            Map.entry("vassal_fan", List.of(e("swift", 2), e("elemental_boost", 1), e("magicule_absorption", 1))),   // Glass' fan: wind, glide; residue is absorbed magic
            Map.entry("vassal_scythe", List.of(e("soul_eater", 2), e("severance", 1), e("energy_steal", 1))),   // every kill gathers a soul; clean reaping cut; capped heals
            Map.entry("vassal_katana", List.of(e("severance", 2), e("swift", 2), e("barrier_piercing", 1))),   // duelist: lines, iaido speed, thrusts through a guard
            Map.entry("vassal_book", List.of(e("magic_weapon", 1), e("elemental_boost", 2), e("restoration", 1), e("magic_protection", 1))),   // spellbook: fire/lightning pages, Healing Verse, Paper Wall
            Map.entry("vassal_hammer", List.of(e("crushing", 3), e("sturdy", 2), e("enervation", 1))),   // heavy blunt head that breaks armour and poise, forge-hardened, saps what it breaks
            Map.entry("vassal_claw", List.of(e("swift", 2), e("severance", 2), e("energy_steal", 1))),   // lightest, fastest weapon; cuts open wounds; the gem feeds on what bleeds
            Map.entry("vassal_axe", List.of(e("severance", 2), e("crushing", 2), e("sturdy", 1))),   // cleaving arcs, heavy chops that break strength and guard, a body hardened by Brawn
            Map.entry("vassal_whip", List.of(e("swift", 1), e("enervation", 1), e("magic_interference", 1))),   // quick lash that binds and saps; takes hold of the power of monsters
            Map.entry("legendary_spear", List.of(e("holy_weapon", 2), e("barrier_piercing", 3), e("swift", 2), e("restoration", 1))),   // Motoyasu's Cardinal weapon: holy, a lance and beams that go through wards, the fast one of the four, and Time Reversal mends
            Map.entry("legendary_bow", List.of(e("holy_weapon", 2), e("barrier_piercing", 2), e("swift", 2), e("elemental_boost", 1))),   // Itsuki's Cardinal weapon: holy, arrows of light that go through wards, a quick draw, frost and fire in its rains
            Map.entry("vassal_staff", List.of(e("magic_weapon", 1), e("swift", 1), e("magic_interference", 1))),   // a casting focus; accelerated casting is its signature; Magic Prism and Float Mirror bend others' magic
            Map.entry("vassal_gauntlet", List.of(e("crushing", 2), e("barrier_piercing", 1), e("sturdy", 1))),   // fists that break defences (Turtle Crush, Conflagration Fist); the quaking shock and palm go through guards; the gauntlet is armour too (Iron Guard)
            Map.entry("vassal_projectile", List.of(e("swift", 2), e("severance", 1), e("magic_weapon", 1))),   // speed of throw; knives, kunai and chakrams cut clean; the base form is a blade conjured through the gem
            Map.entry("vassal_carriage", List.of(e("sturdy", 2), e("crushing", 1), e("swift", 1), e("vitality", 1))));   // built to protect those aboard; rams and runs foes down; Haikuikku and the Queen's pull; the Soothing Ride tends the riders

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
