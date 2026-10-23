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
            Map.entry("shishou", List.of(e("severance", 2), e("barrier_piercing", 2), e("magicule_absorption", 2), e("energy_steal", 1), e("magic_interference", 1))),   // Shishou, the talking sword (Akashic Relic): cuts clean, goes through wards, drinks the magic of what it eats
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
            Map.entry("vassal_carriage", List.of(e("sturdy", 2), e("crushing", 1), e("swift", 1), e("vitality", 1))),   // built to protect those aboard; rams and runs foes down; Haikuikku and the Queen's pull; the Soothing Ride tends the riders
            // Requip: The Knight (requip.RequipItems): the weapons the armours put in the hand
            Map.entry("flame_empress_blade", List.of(e("elemental_boost", 2), e("severance", 1), e("barrier_piercing", 1))),   // Flame Empress: a blade of living flame that cuts clean and burns through wards
            Map.entry("lightning_empress_halberd", List.of(e("elemental_boost", 2), e("swift", 2), e("crushing", 1))),   // Lightning Empress: thunder in the head, a quick long weapon that hits hard
            Map.entry("sea_empress_trident", List.of(e("elemental_boost", 2), e("barrier_piercing", 1), e("magic_weapon", 1))),   // Sea Empress: the deep's trident, a magic weapon that goes through wards
            Map.entry("black_wing_blade", List.of(e("swift", 2), e("severance", 2), e("enervation", 1))),   // Black Wing: a fast shadow-edged blade that cuts clean and saps
            Map.entry("nakagami_edge", List.of(e("nusmp:anti_magic", 1), e("barrier_piercing", 3), e("magic_interference", 2), e("magicule_absorption", 1))),   // Nakagami: parts spells and wards, jams casting, drinks the magic it cuts
            Map.entry("purgatory_mace", List.of(e("crushing", 3), e("sturdy", 2), e("elemental_boost", 1), e("severance", 1))),   // Purgatory: a hell-forged mace that breaks what it strikes
            // Celestial Spirit Magic: the twelve Gold Zodiac keys, the silver keys, the gate key and the sceptre of the Spirit King (each engraving follows the spirit it calls)
            Map.entry("aquarius_key", List.of(e("elemental_boost", 1), e("magic_weapon", 1))),                       // a torrent: boosts elements
            Map.entry("taurus_key", List.of(e("crushing", 1), e("sturdy", 1))),                                      // the golden bull's axe
            Map.entry("leo_key", List.of(e("holy_weapon", 1), e("elemental_boost", 1))),                             // Regulus: holy light
            Map.entry("virgo_key", List.of(e("enervation", 1), e("sturdy", 1))),                                     // chains that tire what they bind
            Map.entry("sagittarius_key", List.of(e("swift", 1), e("barrier_piercing", 1))),                          // arrows through wards
            Map.entry("gemini_key", List.of(e("magic_interference", 1), e("swift", 1))),                             // the copy that confuses magic
            Map.entry("cancer_key", List.of(e("severance", 1), e("enervation", 1))),                                 // scissors: clean cut, sapping wound
            Map.entry("scorpio_key", List.of(e("enervation", 1), e("elemental_boost", 1))),                          // sand and sting
            Map.entry("capricorn_key", List.of(e("crushing", 1), e("sturdy", 1))),                                   // the goat's charge
            Map.entry("pisces_key", List.of(e("restoration", 1), e("elemental_boost", 1))),                          // tide that heals
            Map.entry("aries_key", List.of(e("magic_protection", 1), e("restoration", 1))),                          // wool that wards and mends
            Map.entry("libra_key", List.of(e("magic_interference", 1), e("magic_protection", 1))),                  // the scales weigh magic
            Map.entry("horologium_key", List.of(e("magic_protection", 1), e("sturdy", 1))),                          // the shelter
            Map.entry("crux_key", List.of(e("holy_weapon", 1))),                                                     // a guiding star
            Map.entry("lyra_key", List.of(e("restoration", 1))),                                                     // a soothing song
            Map.entry("nikora_key", List.of(e("swift", 1))),                                                         // the little dog is quick
            Map.entry("spirit_king_gate_key", List.of(e("holy_weapon", 2), e("barrier_piercing", 1), e("magic_interference", 1), e("magic_weapon", 1))),   // the key of the Celestial Gate
            Map.entry("spirit_king_sceptre", List.of(e("holy_weapon", 2), e("restoration", 2), e("magic_protection", 1), e("elemental_boost", 1), e("magic_weapon", 1))),   // the sceptre of the Celestial Spirit King: holy, mending, warding
            // 1.99.0: the monster-drop weapons of the dungeon loot (labyrinth/loot/roster.json lists the same engravings)
            Map.entry("goblin_fang_dagger", List.of(e("swift", 1), e("severance", 1))),                                  // a quick stab that cuts clean
            Map.entry("delvers_longsword", List.of(e("sturdy", 1), e("severance", 1))),                                  // the plain sword a delver trusts: hard to break, cuts clean
            Map.entry("bone_lance", List.of(e("soul_eater", 1), e("enervation", 1), e("barrier_piercing", 1))),          // bone of the dead: takes a soul, saps, goes through wards
            Map.entry("orc_cleaver", List.of(e("crushing", 2), e("sturdy", 1))),                                         // a slab of orc iron that breaks what it hits
            Map.entry("shadowsteel_katana", List.of(e("severance", 2), e("swift", 1), e("barrier_piercing", 1))),        // dark steel: a fast clean cut that parts wards
            Map.entry("drakefang_greatsword", List.of(e("elemental_boost", 1), e("crushing", 1), e("severance", 1))),    // a drake's fang: fire in a heavy edge
            Map.entry("wisp_staff", List.of(e("magic_weapon", 1), e("magicule_absorption", 1), e("swift", 1))),          // a casting focus that drinks stray mana
            Map.entry("stormcaller_staff", List.of(e("elemental_boost", 2), e("magic_weapon", 1), e("barrier_piercing", 1), e("swift", 1))),   // Storm Magic (Fairy Tail): storm and lightning boosted, a casting staff, bolts that go through wards, a gale-quick rod
            Map.entry("magnas_soul_bat", List.of(e("crushing", 2), e("elemental_boost", 2), e("magic_weapon", 1), e("sturdy", 1))),   // Magna Swing: a club that breaks what it strikes, flame boosted, a casting focus that sends his spells, a hard soul-bound wood
            Map.entry("golem_maul", List.of(e("crushing", 2), e("sturdy", 2), e("magic_interference", 1))),              // a golem's fist on a haft: heavy, hard, jams spells
            // the Wind Spirit blades (spiritblades.SpiritBladesBoot; creative-display items): the sword of Zephyr cuts clean through wards and is holy; the bow of Boreas jams magic
            Map.entry("spirit_of_zephyr", List.of(e("severance", 1), e("barrier_piercing", 1), e("holy_weapon", 1))),    // Spirit of Zephyr: a clean cut that parts wards, light of the wind spirit
            Map.entry("spirit_of_boreas", List.of(e("magic_interference", 1))));                                        // Spirit of Boreas: its arrow of light jams the magic it strikes

    static final String FLAG = "NUEngraved";

    /** Read-only view for the Engravement Codex (engraving/codex/CodexGather): item path -> its engravings as "id@level" ("nusmp:anti_magic@1"). Built on first use. */
    public static Map<String, List<String>> codexTable() {
        Map<String, List<String>> t = CODEX_TABLE;
        if (t == null) {
            java.util.LinkedHashMap<String, List<String>> m = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, List<Engraving>> en : ENGRAVINGS.entrySet()) {
                java.util.ArrayList<String> specs = new java.util.ArrayList<>();
                for (Engraving g : en.getValue()) specs.add(g.id() + "@" + g.level());
                m.put(en.getKey(), List.copyOf(specs));
            }
            CODEX_TABLE = t = java.util.Collections.unmodifiableMap(m);
        }
        return t;
    }

    private static volatile Map<String, List<String>> CODEX_TABLE;

    /** Engraves a stack of one of this mod's weapons if it hasn't been yet; true if it changed. */
    public static boolean engrave(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"nusmp".equals(key.getNamespace())) return false;
        List<Engraving> list = ENGRAVINGS.get(key.getPath());
        if (list == null) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains(FLAG)) return false;                      // 1.112.0 (lag audit): the marker is only ever set to true; no copy of the whole tag every second
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
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_ENGRAVINGS_WEAPON = com.newuniverse.nusmp.perf.PerfProbe.section("s.engravings.weapon");   // 1.99.0: performance probe (off by default: one flag read)

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onPlayerTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_ENGRAVINGS_WEAPON, t0);
        }
    }

    private static void onPlayerTick0(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (!(p instanceof ServerPlayer sp) || sp.tickCount % 20 != 7) return;
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            engrave(inv.getItem(i), sp.registryAccess());
            com.newuniverse.nusmp.franchise.FranchiseEngravings.engrave(inv.getItem(i), sp.registryAccess());      // 1.111.0: the franchise engravings (own marker "NUFranchise")
        }
    }
}
