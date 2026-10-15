package com.newuniverse.nusmp.client.ui.codex;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.core.discovery.DiscoveryKeys;
import com.newuniverse.nusmp.core.discovery.DiscoveryRules;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Why: the registry-driven catalogues of the Codex (Blocks, Entities, Magic, Effects) and the bridge from a data section to rows. Nothing here is a
 * list of names: each source walks a registry (every {@code nusmp:} block, entity type, mob effect, every {@link MagicType}) at the moment the
 * Codex opens, so content added to the mod appears in the book without anyone editing a list. Each row's text is a lang line when one exists
 * ({@code nusmp.codex.<kind>.<path>}, {@code nusmp.magic.<id>.desc}), otherwise a line generated from what the registry knows. Entities and
 * magics carry their discovery key, so a creature or magic the player has not met stays "???".
 */
public final class CodexSources {
    private CodexSources() {}

    private static final String NS = "nusmp";

    private static String lang(String key, String fallback) { return I18n.exists(key) ? I18n.get(key) : fallback; }

    private static List<CodexBlock> singles(List<CodexRow> rows) {
        List<CodexBlock> out = new ArrayList<>(rows.size());
        for (CodexRow r : rows) out.add(CodexBlock.single(r));
        return out;
    }

    // ---------------------------------------------------------------- blocks
    public static List<CodexBlock> blocks() {
        List<CodexRow> rows = new ArrayList<>();
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) if (NS.equals(id.getNamespace())) ids.add(id);
        ids.sort(Comparator.comparing(ResourceLocation::getPath));
        for (ResourceLocation id : ids) {
            Block b = BuiltInRegistries.BLOCK.get(id);
            Item item = b.asItem();
            ItemStack icon = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
            float hardness = b.defaultDestroyTime();
            String generated = hardness < 0f ? "A block of this mod. It cannot be broken." : "A block of this mod. Hardness " + CodexRuleValue.trim(hardness) + ".";
            rows.add(new CodexRow(b.getName().getString(), lang("nusmp.codex.block." + id.getPath(), generated), "", icon, id.toString()));
        }
        return singles(rows);
    }

    // ---------------------------------------------------------------- entities
    public static List<CodexBlock> entities() {
        List<CodexRow> rows = new ArrayList<>();
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) if (NS.equals(id.getNamespace())) ids.add(id);
        ids.sort(Comparator.comparing(ResourceLocation::getPath));
        for (ResourceLocation id : ids) {
            EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.get(id);
            SpawnEggItem egg = SpawnEggItem.byId(t);
            ItemStack icon = egg == null ? ItemStack.EMPTY : new ItemStack(egg);
            rows.add(new CodexRow(t.getDescription().getString(), lang("nusmp.codex.entity." + id.getPath(), id.toString()), nullToEmpty(DiscoveryKeys.mob(t)), icon, ""));
        }
        return singles(rows);
    }

    // ---------------------------------------------------------------- magic attributes
    public static List<CodexBlock> magic() {
        List<CodexRow> rows = new ArrayList<>();
        for (MagicType m : MagicType.values()) {
            String id = m.name().toLowerCase(Locale.ROOT);
            String soul = CodexData.prettify(m.soulType().toLowerCase(Locale.ROOT)), hit = CodexData.prettify(m.hitEffect().name().toLowerCase(Locale.ROOT));
            String generated = m.displayName() + " belongs to the " + soul + " soul. Its spells hit with " + hit.toLowerCase(Locale.ROOT) + ".";
            String note = "Soul: " + soul + "   Hit: " + hit + (m.restricted() ? "   Never rolled for a soul" : "");
            rows.add(new CodexRow(m.displayName(), lang("nusmp.magic." + id + ".desc", generated), DiscoveryRules.magicKey(m.name()), ItemStack.EMPTY, note));
        }
        return singles(rows);
    }

    // ---------------------------------------------------------------- mob effects
    public static List<CodexBlock> effects() {
        List<CodexRow> rows = new ArrayList<>();
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.MOB_EFFECT.keySet()) if (NS.equals(id.getNamespace())) ids.add(id);
        ids.sort(Comparator.comparing(ResourceLocation::getPath));
        for (ResourceLocation id : ids) {
            MobEffect e = BuiltInRegistries.MOB_EFFECT.get(id);
            String kind = switch (e.getCategory()) { case BENEFICIAL -> "beneficial"; case HARMFUL -> "harmful"; default -> "neutral"; };
            rows.add(new CodexRow(e.getDisplayName().getString(), lang("nusmp.codex.effect." + id.getPath(), "A " + kind + " status effect of this mod."), "", ItemStack.EMPTY, id.toString()));
        }
        return singles(rows);
    }

    // ---------------------------------------------------------------- a data section (assets/<ns>/codex/<section>/*.json)
    public static List<CodexBlock> section(String section) {
        List<CodexEntryData> entries = CodexData.get().sections().getOrDefault(section, List.of());
        List<CodexRow> rows = new ArrayList<>();
        for (CodexEntryData e : entries) {
            String live = CodexRuleState.valueFor(e.id());
            ItemStack icon = ItemStack.EMPTY;
            String itemId = CodexData.iconItemId(e.icon());
            if (itemId != null) {
                ResourceLocation rl = ResourceLocation.tryParse(itemId);
                if (rl != null) icon = BuiltInRegistries.ITEM.getOptional(rl).map(ItemStack::new).orElse(ItemStack.EMPTY);
            }
            rows.add(new CodexRow(e.title(), e.text(), e.keys(), icon, live == null ? "" : "Now: " + live));
        }
        return singles(rows);
    }

    /** The tab title of a section: the lang key {@code nusmp.codex.section.<dir>} when translated, else the prettified directory name. */
    public static String sectionTitle(String section) { return lang(CodexData.sectionKey(section), CodexData.prettify(section)); }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
