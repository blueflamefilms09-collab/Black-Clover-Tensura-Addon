package com.newuniverse.nusmp.client.playermodel;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.labyrinth.foundry.FoundryRenderer;
import com.newuniverse.nusmp.client.corpse.NamedCorpseRenderer;
import com.newuniverse.nusmp.client.floor.goliath.GoliathRenderer;
import com.newuniverse.nusmp.client.remodel.AbyssalRenderer;
import com.newuniverse.nusmp.client.remodel.RemodelRenderers;
import com.newuniverse.nusmp.corpse.CorpseEntities;
import com.newuniverse.nusmp.dungeon.DungeonEntities;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathBoss;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathEntities;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryRegistry;
import com.newuniverse.nusmp.labyrinth.mega.guard.GuardRegistry;
import com.newuniverse.nusmp.labyrinth.seal.SealRegistry;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Why: the registration of phase 2 (docs/playermodel_phase2.md): the humanoids of the dungeon move onto the kit by ADDING the kit's renderer, never by removing the old one. Each type gets a
 * {@link PlayerModelRegistry.Split}: the kit's {@link PlayerModelRenderer} for an entity that the flag {@code client.playermodel.dungeon} ({@link PlayerModelOptions#dungeon}) allows and whose look
 * the table has, and the renderer the type had before (built from the provider the art kit's {@code RemodelRenderers} registered, or the old class itself) for everything else: the flag turned off,
 * a Guardian whose boss is a creature, a Foundry mob that is a creature, a look a resource pack removed. The old renderer classes and all their assets stay in the tree, and the entity types,
 * their ids, attributes, AI, drops and save data are not touched: this is a client registration. Every registration is guarded, so a type that is renamed or missing logs an error and every other
 * one still loads. Called from {@link PlayerModelRegistry#register}, after the phase-1 conversions, in the same renderer event (the kit's listener is the last, so its renderer is the one that stays).
 */
public final class DungeonRenderers {
    private DungeonRenderers() {}

    private static final Logger LOG = LogUtils.getLogger();

    private static void guard(String what, Runnable r) {
        try {
            r.run();
        } catch (RuntimeException | LinkageError ex) {
            LOG.error("[nusmp] the player-model renderer of {} could not be registered: {}", what, ex.toString());
        }
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        DungeonBodies.Armour armour = new DungeonBodies.Armour();
        guard("living_armor", () -> kitOrOld(e, DungeonEntities.LIVING_ARMOR.get(), armour, DungeonBodies.Armour::kitDraws, ctx -> old(DungeonEntities.LIVING_ARMOR.get(), ctx)));
        guard("floor_warden", () -> kitOrOld(e, SealRegistry.FLOOR_WARDEN.get(), armour, DungeonBodies.Armour::kitDraws, ctx -> old(SealRegistry.FLOOR_WARDEN.get(), ctx)));
        guard("labyrinth_guardian", () -> kitOrOld(e, GuardRegistry.GUARDIAN.get(), armour, DungeonBodies.Armour::kitDraws, AbyssalRenderer::new));
        guard("goliath", () -> kitOrOld(e, GoliathEntities.GOLIATH.get(), new DungeonBodies.Goliath(), DungeonBodies.Goliath::kitDraws,
                ctx -> { EntityRenderer<GoliathBoss> o = old(GoliathEntities.GOLIATH.get(), ctx); return o != null ? o : new GoliathRenderer(ctx); }));
        guard("foundry_mob", () -> kitOrOld(e, FoundryRegistry.MOB.get(), new DungeonBodies.Foundry(), DungeonBodies.Foundry::kitDraws, FoundryRenderer::new));
        DungeonBodies.Corpse corpse = new DungeonBodies.Corpse();
        guard("corpse_jimmy", () -> kitOrOld(e, CorpseEntities.JIMMY.get(), corpse, DungeonBodies.Corpse::kitDraws, NamedCorpseRenderer::new));
        guard("corpse_alfredo", () -> kitOrOld(e, CorpseEntities.ALFREDO.get(), corpse, DungeonBodies.Corpse::kitDraws, NamedCorpseRenderer::new));
        guard("corpse_carl", () -> kitOrOld(e, CorpseEntities.CARL.get(), corpse, DungeonBodies.Corpse::kitDraws, NamedCorpseRenderer::new));
    }

    /** The renderer the art kit registered for a type (the old renderer of the type), or null when it registered none. */
    private static <T extends LivingEntity> EntityRenderer<T> old(EntityType<? extends T> type, EntityRendererProvider.Context ctx) {
        EntityRendererProvider<T> p = RemodelRenderers.providerOf(type);
        return p == null ? null : p.create(ctx);
    }

    /**
     * Registers {@code type} with a renderer that draws the kit's body for an entity when {@code PlayerModelOptions.dungeon()} is on and {@code kitDraws} says the entity has a look, and the
     * old renderer for any other. With no old renderer to fall back to, the kit's renderer alone is registered.
     */
    @SuppressWarnings("unchecked")
    private static <T extends LivingEntity> void kitOrOld(EntityRenderersEvent.RegisterRenderers e, EntityType<? extends T> type, PlayerBody<T> body, Predicate<T> kitDraws,
                                                          Function<EntityRendererProvider.Context, EntityRenderer<T>> old) {
        EntityType<T> t = (EntityType<T>) type;
        e.registerEntityRenderer(t, ctx -> {
            PlayerModelRenderer<T> kit = new PlayerModelRenderer<>(ctx, body);
            EntityRenderer<T> legacy = old.apply(ctx);
            if (legacy == null) return kit;
            return new PlayerModelRegistry.Split<T>(ctx, en -> DungeonLooks.usesKit(PlayerModelOptions.dungeon(), kitDraws.test(en)), kit, legacy);
        });
    }
}
