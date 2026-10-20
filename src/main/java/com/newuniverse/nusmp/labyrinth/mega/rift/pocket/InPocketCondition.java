package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Locale;
import java.util.Set;

/**
 * Why: the loot-table half of the hard gate. {@code {"condition":"nusmp:in_pocket","pocket":"ember_sanctum","stage":"boss"}} is true only when the roll happens inside the live exclusive pocket
 * of that id (and, for the stage, after its boss died or with its seal open): see {@link PocketGate}. Every pool of every table under {@code loot_table/pocket/} carries it, so the relics and the
 * tomb's treasure cannot be rolled anywhere else even if someone copies the table into another chest; the data test walks all of them. It needs the roll's origin, which every chest and entity
 * loot context has.
 */
public record InPocketCondition(String pocket, PocketRules.Stage stage) implements LootItemCondition {
    public static final MapCodec<InPocketCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("pocket").forGetter(InPocketCondition::pocket),
            Codec.STRING.optionalFieldOf("stage", "any").xmap(PocketRules::stageOf, s -> s.name().toLowerCase(Locale.ROOT)).forGetter(InPocketCondition::stage)
    ).apply(i, InPocketCondition::new));

    @Override
    public LootItemConditionType getType() { return PocketLoot.IN_POCKET.get(); }

    @Override
    public boolean test(LootContext ctx) {
        try {
            return PocketGate.allows(ctx.getLevel(), ctx.getParamOrNull(LootContextParams.ORIGIN), ctx.getParamOrNull(LootContextParams.THIS_ENTITY), pocket, stage);
        } catch (RuntimeException | LinkageError ex) {
            return false;                       // loot must never fail the game; a roll that cannot be judged is a roll that gives nothing of the tomb
        }
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.ORIGIN); }
}
