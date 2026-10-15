package com.newuniverse.nusmp.labyrinth.vault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicLoot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Why: the loot pool entry type {@code nusmp:relic}: {@code {"type":"nusmp:relic","min_grade":"rare","max_grade":"legend","weight":10}}.
 * A relic is not a fixed item in a JSON list: it is "one relic of these grades that nobody in this world has yet", decided by the God of
 * Order's ledger. Naming relic items in a loot table would break that in three ways: every wave of new relics would need the JSON edited,
 * a table could hand out a relic that already lives (the ledger would then hunt the duplicate), and a pair would need two entries that
 * cannot be kept together. This entry goes through the same {@link RelicLoot#rollUnissuedAll} as the loot modifier and the Mimics, so every
 * relic ever registered (future waves included) is eligible the day it is added, the ledger stamps the copy it hands out, and a pair yields
 * both halves from one entry. Grades and weights stay in the datapack, where a server owner can retune them.
 * <p>
 * {@link #expand} is overridden to report "nothing here" when no unissued relic of the range exists, so the vanilla
 * {@code minecraft:alternatives} / {@code minecraft:group} composites can fall through to a mundane fallback entry and an exhausted range
 * simply drops out of a pool's weighting instead of eating a roll. {@code createItemStack} never throws: loot must not fail because of a relic.
 */
public final class RelicLootEntry extends LootPoolSingletonContainer {
    /** A grade by its lower-case name ("rare"); "Rare" and "Rare Grade" are accepted too, anything else is a parse error. */
    public static final Codec<Grade> GRADE_CODEC = Codec.STRING.comapFlatMap(s -> {
        Grade g = Grade.byName(s);
        return g == null ? DataResult.error(() -> "Unknown relic grade: " + s) : DataResult.success(g);
    }, g -> g.name().toLowerCase(Locale.ROOT));

    public static final MapCodec<RelicLootEntry> CODEC = RecordCodecBuilder.mapCodec(i -> singletonFields(i).and(i.group(
            GRADE_CODEC.optionalFieldOf("min_grade", Grade.NORMAL).forGetter(e -> e.min),
            GRADE_CODEC.optionalFieldOf("max_grade", Grade.GENESIS).forGetter(e -> e.max)
    )).apply(i, RelicLootEntry::new));

    private final Grade min, max;

    RelicLootEntry(int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions, Grade min, Grade max) {
        super(weight, quality, conditions, functions);
        Grade lo = min == null ? Grade.NORMAL : min, hi = max == null ? Grade.GENESIS : max;
        if (lo.ordinal() > hi.ordinal()) { Grade t = lo; lo = hi; hi = t; }          // a reversed range is an authoring slip, not a reason to yield nothing
        this.min = lo;
        this.max = hi;
    }

    public Grade minGrade() { return min; }

    public Grade maxGrade() { return max; }

    @Override
    public boolean expand(LootContext ctx, Consumer<LootPoolEntry> consumer) {
        try {
            if (!RelicLoot.anyUnissued(ctx.getLevel(), min, max)) return false;      // exhausted range: let a composite fall through
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
        return super.expand(ctx, consumer);
    }

    @Override
    protected void createItemStack(Consumer<ItemStack> consumer, LootContext ctx) {
        try {
            for (ItemStack s : RelicLoot.rollUnissuedAll(ctx.getLevel(), ctx.getRandom(), min, max)) consumer.accept(s);      // a pair: both halves
        } catch (RuntimeException | LinkageError ignored) {
            // loot must never fail because of a relic
        }
    }

    @Override
    public LootPoolEntryType getType() { return VaultRegistry.RELIC_ENTRY.get(); }
}
