package com.newuniverse.nusmp.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Why: vanilla triggers cannot see most of what this mod does (a grimoire ceremony, a spell cast, a squad founded, a boss phase). Instead of
 * one trigger per feature, ONE trigger {@code nusmp:event} carries a string key, and the game code reports events with
 * {@link NusmpEvents#fire}. Advancements and the FTB questline then ask for keys in plain JSON.
 *
 * <p>JSON form: {@code {"trigger":"nusmp:event","conditions":{"key":"spell.cast","arg":"thread","count":10}}}. {@code arg} and
 * {@code count} are optional (any argument, count 1); the matching rule is {@link TriggerRule#matches}.
 */
public class NusmpEventTrigger extends SimpleCriterionTrigger<NusmpEventTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** Runs every listening criterion of this player through {@code test}; the ones that pass are awarded by the base class. */
    void fire(ServerPlayer player, Predicate<TriggerInstance> test) {
        trigger(player, test);
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String key, int count, Optional<String> arg)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("key").forGetter(TriggerInstance::key),
                Codec.INT.optionalFieldOf("count", 1).forGetter(TriggerInstance::count),
                Codec.STRING.optionalFieldOf("arg").forGetter(TriggerInstance::arg)
        ).apply(i, TriggerInstance::new));
    }
}
