package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.relics.wave.Wave0;
import com.newuniverse.nusmp.relics.wave.Wave1;
import com.newuniverse.nusmp.relics.wave.Wave2;
import com.newuniverse.nusmp.relics.wave.Wave3;
import com.newuniverse.nusmp.relics.wave.Wave4;
import com.newuniverse.nusmp.relics.wave.Wave5;
import com.newuniverse.nusmp.relics.wave.Wave6;
import com.newuniverse.nusmp.relics.wave.Wave7;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Collects the defs of every wave, checks them, registers each new relic as an item in the shared {@code NUItems.ITEMS} register, and binds the
 * result into {@link Relics}. A wave that throws, a duplicate id, an id that an item of the mod already has, or a vessel flag below LEGEND is
 * logged and dealt with here (skipped or corrected) instead of stopping the game from starting.
 * <p>
 * Call {@link #init()} from the mod constructor BEFORE {@code NUItems.ITEMS.register(modEventBus)}, and {@link #hooks()} once with the game-bus events.
 */
public final class RelicCatalog {
    private RelicCatalog() {}

    private static final Logger LOG = LoggerFactory.getLogger("nusmp");
    private static final Map<String, DeferredItem<Item>> ITEMS = new LinkedHashMap<>();
    private static List<RelicDef> defs = List.of();
    private static boolean inited;

    /** Reads every wave, checks the defs and registers the new relics as items. Safe to call twice (the second call does nothing). */
    public static synchronized void init() {
        if (inited) return;
        inited = true;
        List<RelicDef> raw = new ArrayList<>();
        // 1.29.0: a def that declares a pair (RelicDef.pair) brings its left half along, right after it (see docs/relic_pairs.md)
        Consumer<RelicDef> out = d -> {
            raw.add(d);
            if (d != null && d.hasPair()) raw.add(d.pairLeft());
        };
        guard("Wave0", () -> Wave0.defs(out));
        guard("Wave1", () -> Wave1.defs(out));
        guard("Wave2", () -> Wave2.defs(out));
        guard("Wave3", () -> Wave3.defs(out));
        guard("Wave4", () -> Wave4.defs(out));
        guard("Wave5", () -> Wave5.defs(out));
        guard("Wave6", () -> Wave6.defs(out));
        guard("Wave7", () -> Wave7.defs(out));
        guard("Wave102", () -> com.newuniverse.nusmp.relics.wave.Wave102.defs(out));                  // 1.111.0: 9 God + 20 Special relics (docs/relics/god_special_new.md)
        guard("later waves", () -> WaveRegistry.defs(out));                 // 1.25.0: WaveBoss and Wave8 and up, found by name
        Set<String> taken = new HashSet<>();
        for (DeferredHolder<Item, ? extends Item> h : NUItems.ITEMS.getEntries()) taken.add(h.getId().getPath());
        RelicChecks.Checked c = RelicChecks.check(raw, taken);
        for (String problem : c.problems()) LOG.error("[nusmp] relics: {}", problem);
        defs = c.defs();
        for (RelicDef d : defs) {
            if (d.isAdopted()) continue;
            Holder<ArmorMaterial> material = d.slot().isArmor() ? RelicArmorMaterials.of(d) : null;
            ITEMS.put(d.id(), NUItems.ITEMS.register(d.id(), () -> make(d, material)));
        }
        Relics.bind(defs);
        LOG.info("[nusmp] relics: {} defs, {} new items, {} adopted", defs.size(), ITEMS.size(), defs.size() - ITEMS.size());
    }

    private static Item make(RelicDef d, Holder<ArmorMaterial> material) {
        return switch (d.slot()) {
            case MAIN_HAND -> switch (d.rangedKind()) {                                  // 1.25.0: drawn weapons
                case BOW -> new RelicBowItem(d);
                case CROSSBOW -> new RelicCrossbowItem(d);
                default -> new RelicWeaponItem(d);
            };
            case HEAD, CHEST, FEET -> new RelicArmorItem(d, material);
            case CONSUMABLE -> new RelicConsumableItem(d);
            default -> new RelicGearItem(d);
        };
    }

    private static void guard(String wave, Runnable r) {
        try {
            r.run();
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] relics: {} could not give its defs; its relics are missing", wave, failed);
        }
    }

    /** Registers each wave's game-bus listeners (call once, after {@link #init()}). */
    public static void hooks() {
        guard("Wave0 hooks", Wave0::hooks);
        guard("Wave1 hooks", Wave1::hooks);
        guard("Wave2 hooks", Wave2::hooks);
        guard("Wave3 hooks", Wave3::hooks);
        guard("Wave4 hooks", Wave4::hooks);
        guard("Wave5 hooks", Wave5::hooks);
        guard("Wave6 hooks", Wave6::hooks);
        guard("Wave7 hooks", Wave7::hooks);
        guard("Wave102 hooks", com.newuniverse.nusmp.relics.wave.Wave102::hooks);                      // 1.111.0: the weapons' passive rules and the self-test relic.god_special_new
        guard("later waves hooks", WaveRegistry::hooks);
        guard("pair hooks", com.newuniverse.nusmp.relics.pair.PairHooks::register);          // 1.29.0: the off-hand strike and the first-login left half
    }

    public static List<RelicDef> defs() { return defs; }

    /** The registered item of a new relic (not an adopted one), or null. */
    public static Supplier<? extends Item> itemOf(String relicId) { return ITEMS.get(relicId); }

    /** A fresh stack of this relic's item (an adopted relic's existing item, or the registered one), or EMPTY if it is not registered. */
    public static net.minecraft.world.item.ItemStack newStack(RelicDef d) {
        try {
            Item item = d.isAdopted() ? d.adopted().get() : ITEMS.containsKey(d.id()) ? ITEMS.get(d.id()).get() : null;
            return item == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(item);
        } catch (RuntimeException notBound) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    /** The new relics with a 3D hand mesh: add them to {@code WeaponRenderer.WEAPONS}. */
    public static List<Supplier<? extends Item>> model3dItems() {
        List<Supplier<? extends Item>> out = new ArrayList<>();
        for (RelicDef d : defs) if (d.hasModel3d() && !d.isAdopted() && ITEMS.containsKey(d.id())) out.add(ITEMS.get(d.id()));
        return out;
    }

    /** Every new relic item for the creative tab, lowest grade first, then by name. */
    public static List<Supplier<? extends Item>> creativeItems() {
        List<RelicDef> sorted = new ArrayList<>();
        for (RelicDef d : defs) if (!d.isAdopted() && ITEMS.containsKey(d.id())) sorted.add(d);
        sorted.sort(Comparator.comparing(RelicDef::grade).thenComparing(RelicDef::cardName));
        List<Supplier<? extends Item>> out = new ArrayList<>(sorted.size());
        for (RelicDef d : sorted) out.add(ITEMS.get(d.id()));
        return out;
    }

    /** The default English of every new relic (name and short description), for the lead to merge into en_us.json. */
    public static Map<String, String> langDefaults() {
        Map<String, String> m = new LinkedHashMap<>();
        for (RelicDef d : defs) {
            if (d.isAdopted()) continue;
            m.put("item.nusmp." + d.id(), d.cardName());
            m.put("item.nusmp." + d.id() + ".desc", d.desc());
        }
        return m;
    }
}
