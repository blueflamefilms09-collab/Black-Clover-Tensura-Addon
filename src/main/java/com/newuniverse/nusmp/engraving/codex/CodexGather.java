package com.newuniverse.nusmp.engraving.codex;

import com.newuniverse.nusmp.franchise.FranchiseData;
import com.newuniverse.nusmp.franchise.FranchiseEngravings;
import com.newuniverse.nusmp.franchise.FranchiseMap;
import com.newuniverse.nusmp.item.ArmorEngravings;
import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponRules;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Why: the game-side half of the Engravement Codex. It reads the registries and the engraving tables of the mod (public read-only views: {@link WeaponEngravings#codexTable},
 * {@link ArmorEngravings#codexTable}, the relic catalog, the Djinn weapon rules, the franchise map) and hands the plain facts to the pure {@link CodexEntries}. Common code with no client
 * class (it only asks the registries and the language), so the CI server smoke test can run it; the client screen calls it once on first open and keeps the book. It changes nothing.
 */
public final class CodexGather {
    private CodexGather() {}

    /** The tag of Tensura's engravings. */
    public static final TagKey<Enchantment> ENGRAVING_TAG = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", "engraving"));

    /** A language function over the game's current language: the text of a key, or null when the language file has none. */
    public static Function<String, String> currentLanguage() {
        return key -> {
            Language l = Language.getInstance();
            return l.has(key) ? l.getOrDefault(key) : null;
        };
    }

    /** The whole book for these registries and this language. */
    public static CodexEntries.Book book(HolderLookup.Provider registries, Function<String, String> lang) {
        return CodexEntries.build(gather(registries, lang));
    }

    /** The inputs of the book. Runs once per open of the screen (or per self-test); walks the item registry once. */
    public static CodexEntries.Input gather(HolderLookup.Provider registries, Function<String, String> lang) {
        FranchiseData.Table table = FranchiseData.table();
        Map<String, String> names = new HashMap<>();                                       // item path -> display name (null marker for "not an item")
        Map<String, TreeSet<String>> carriers = new LinkedHashMap<>();                     // "ns:id" of a non-franchise engraving -> item names

        // ---- items that carry the franchise engravings: every engravable item of the mod that has a franchise in the map
        Map<String, TreeSet<String>> byFranchise = new LinkedHashMap<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> en : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = en.getKey().location();
            if (!"nusmp".equals(id.getNamespace())) continue;
            String franchise = FranchiseMap.franchiseOf(id.getPath());
            if (franchise == null) continue;
            Item item = en.getValue();
            ItemStack stack = item.getDefaultInstance();
            if (!FranchiseEngravings.engravable(stack)) continue;
            byFranchise.computeIfAbsent(franchise, k -> new TreeSet<>()).add(nameOf(names, id.getPath(), item));
        }

        // ---- items that carry Tensura engravings (and this mod's own marks): the weapon, armour, relic and Djinn weapon tables
        for (Map.Entry<String, List<String>> en : WeaponEngravings.codexTable().entrySet()) addCarrier(carriers, names, en.getKey(), en.getValue());
        for (Map.Entry<String, List<String>> en : ArmorEngravings.codexTable().entrySet()) addCarrier(carriers, names, en.getKey(), en.getValue());
        for (RelicDef def : RelicCatalog.defs()) {
            if (def.engravings().isEmpty() || def.isPairLeft()) continue;
            List<String> specs = new ArrayList<>();
            for (String s : def.engravings()) specs.add(s.indexOf('@') > 0 ? s : s + "@1");
            addCarrier(carriers, names, def.id(), specs);
        }
        for (Djinn d : Djinn.values()) {
            for (boolean dark : new boolean[]{false, true}) {
                List<String> specs = new ArrayList<>();
                for (DjinnWeaponRules.Engraving g : DjinnWeaponRules.engravings(d, dark)) specs.add(g.id() + "@" + g.level());
                addCarrier(carriers, names, DjinnWeaponRules.itemPath(d, dark), specs);
            }
        }

        // ---- the enchantment registry: this mod's own non-franchise enchantments and the members of Tensura's engraving tag
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, CodexEntries.Own> own = new TreeMap<>();
        lookup.listElements().forEach(ref -> {
            ResourceLocation id = ref.key().location();
            if (!"nusmp".equals(id.getNamespace()) || table.engravings().containsKey(id.getPath())) return;
            boolean mainOnly = !ref.value().definition().slots().isEmpty() && ref.value().definition().slots().stream().allMatch(s -> s == EquipmentSlotGroup.MAINHAND);
            own.put(id.getPath(), new CodexEntries.Own(id.getPath(), mainOnly ? "main" : "any", ref.value().getMaxLevel(), carriers.getOrDefault("nusmp:" + id.getPath(), new TreeSet<>())));
        });
        // an own enchantment named by an item table but not (yet) registered still gets its page
        for (String key : carriers.keySet()) {
            if (!key.startsWith("nusmp:")) continue;
            String path = key.substring("nusmp:".length());
            if (!table.engravings().containsKey(path) && !own.containsKey(path)) own.put(path, new CodexEntries.Own(path, "main", 0, carriers.get(key)));
        }

        Map<String, CodexEntries.TensuraRef> tensura = new TreeMap<>();
        Optional<net.minecraft.core.HolderSet.Named<Enchantment>> tagged = lookup.get(ENGRAVING_TAG);
        if (tagged.isPresent()) {
            for (Holder<Enchantment> h : tagged.get()) {
                Optional<net.minecraft.resources.ResourceKey<Enchantment>> key = h.unwrapKey();
                if (key.isEmpty()) continue;
                ResourceLocation id = key.get().location();
                if (!"tensura".equals(id.getNamespace())) continue;                          // this mod's engravings have their own groups
                String path = id.getPath();
                tensura.put(path, new CodexEntries.TensuraRef(path, shownName(h.value().description()), h.value().getMaxLevel(), carriers.getOrDefault("tensura:" + path, new TreeSet<>())));
            }
        }
        for (Map.Entry<String, TreeSet<String>> en : carriers.entrySet()) {                  // an engraving an item names that the tag does not list
            if (!en.getKey().startsWith("tensura:")) continue;
            String path = en.getKey().substring("tensura:".length());
            tensura.putIfAbsent(path, new CodexEntries.TensuraRef(path, null, 0, en.getValue()));
        }
        return new CodexEntries.Input(table, byFranchise, new ArrayList<>(own.values()), new ArrayList<>(tensura.values()), TensuraNotes.table(), lang);
    }

    /** The engraving's name if the language has a translation for it, else null (the book then makes one from the id). */
    private static String shownName(Component description) {
        if (description != null && description.getContents() instanceof TranslatableContents tc && Language.getInstance().has(tc.getKey())) return description.getString();
        return null;
    }

    private static void addCarrier(Map<String, TreeSet<String>> carriers, Map<String, String> names, String itemPath, List<String> specs) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("nusmp", itemPath));
        if (item.isEmpty()) return;
        String name = nameOf(names, itemPath, item.get());
        for (String spec : specs) {
            String id = spec;
            int at = id.indexOf('@');
            if (at > 0) id = id.substring(0, at);
            if (id.startsWith("minecraft:")) continue;                                       // a vanilla curse is not an engraving
            String key = id.contains(":") ? id : "tensura:" + id;
            carriers.computeIfAbsent(key, k -> new TreeSet<>()).add(name);
        }
    }

    private static String nameOf(Map<String, String> names, String path, Item item) {
        String n = names.get(path);
        if (n == null) {
            n = item.getDescription().getString();
            names.put(path, n);
        }
        return n;
    }
}
