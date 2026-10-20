package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.function.Consumer;

/**
 * Why: the loot entry {@code nusmp:pocket_relic}: {@code {"type":"nusmp:pocket_relic","relic":"ignareths_forge_hammer","pocket":"ember_sanctum"}} is one named relic of one tomb. A relic is not an
 * ordinary item in a table: a unique relic must be issued through the God of Order's ledger (one copy in the world, stamped with a serial), exactly as {@code nusmp:relic} does for the random
 * ones, so this entry goes through {@link UniqueLedger#issue}. It is also the SECOND lock of the hard gate: the entry itself refuses (answers "nothing here", so a sibling in an
 * {@code alternatives} composite can run) unless {@link PocketGate} says the roll is inside the live tomb of its {@code pocket}, whatever the pool's conditions say. And it never throws:
 * loot must not fail because of a relic.
 */
public final class PocketRelicEntry extends LootPoolSingletonContainer {
    public static final MapCodec<PocketRelicEntry> CODEC = RecordCodecBuilder.mapCodec(i -> singletonFields(i).and(i.group(
            Codec.STRING.fieldOf("relic").forGetter(e -> e.relic),
            Codec.STRING.fieldOf("pocket").forGetter(e -> e.pocket)
    )).apply(i, PocketRelicEntry::new));

    private final String relic, pocket;

    PocketRelicEntry(int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions, String relic, String pocket) {
        super(weight, quality, conditions, functions);
        this.relic = relic;
        this.pocket = pocket;
    }

    public String relic() { return relic; }

    public String pocket() { return pocket; }

    private boolean gate(LootContext ctx) {
        try {
            return PocketGate.allows(ctx.getLevel(), ctx.getParamOrNull(LootContextParams.ORIGIN), ctx.getParamOrNull(LootContextParams.THIS_ENTITY), pocket, PocketRules.Stage.ANY);
        } catch (RuntimeException | LinkageError ex) {
            return false;
        }
    }

    @Override
    public boolean expand(LootContext ctx, Consumer<LootPoolEntry> consumer) {
        try {
            if (!gate(ctx)) return false;
            RelicDef d = Relics.byId(relic);
            if (d == null) return false;
            if (d.isUnique() && UniqueLedger.liveIds(ctx.getLevel().getServer()).contains(d.id())) return false;       // a copy lives already: let the fallback of the composite run
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
        return super.expand(ctx, consumer);
    }

    @Override
    protected void createItemStack(Consumer<ItemStack> consumer, LootContext ctx) {
        try {
            if (!gate(ctx)) return;
            RelicDef d = Relics.byId(relic);
            if (d == null) return;
            ItemStack s = RelicCatalog.newStack(d);
            if (s.isEmpty()) return;
            if (d.isUnique() && !UniqueLedger.issue(ctx.getLevel(), d.id(), s)) return;
            consumer.accept(s);
        } catch (RuntimeException | LinkageError ignored) {
            // loot must never fail because of a relic
        }
    }

    @Override
    public LootPoolEntryType getType() { return PocketLoot.RELIC_ENTRY.get(); }
}
