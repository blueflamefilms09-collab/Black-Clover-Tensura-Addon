package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registration entry point of the Mob Foundry, the pattern of every package of the Mega-Dungeon: its own deferred register and one {@link #init} (mod bus: the one entity
 * type {@code nusmp:foundry_mob}, its attributes, the in-game self-tests) and one {@link #register} (game bus: the data reload listener and the drop listeners), called from
 * {@code MegaSystem}. The lead registers {@link FoundryConfig#SPEC} as {@code nusmp-labyrinth-mega-foundry-server.toml}, attaches {@link #command()} under
 * {@code /multiverse mega}, and adds the client call {@code FoundryClient.init(modBus)} to {@code LabyrinthClient}. There is no spawn egg and no creative-tab entry on purpose: a
 * foundry mob is made by the Dungeon Master or by the admin command, which gives it the identity the entity needs.
 */
public final class FoundryRegistry {
    private FoundryRegistry() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    /** The one entity type of the 400 mobs; which mob it is, is its synced identity string. The hitbox (0.6 x 1.95) is scaled per mob by the SCALE attribute. */
    public static final DeferredHolder<EntityType<?>, EntityType<FoundryMobEntity>> MOB = TYPES.register("foundry_mob",
            () -> EntityType.Builder.of(FoundryMobEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10).build("foundry_mob"));

    public static void init(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(FoundryRegistry::attributes);
        FoundrySelfTests.register();
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(MOB.get(), FoundryMobEntity.attributes().build());
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new FoundryData.Reload()));
        gameBus.addListener(EventPriority.HIGHEST, FoundryDrops::onMinionDrops);
        gameBus.addListener(FoundryDrops::onDrops);
    }

    /** The {@code foundry} admin node ({@code list | spawn | count | verify}); attached under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() { return FoundryCommand.command(); }
}
