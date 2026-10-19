package com.newuniverse.nusmp.client.playermodel;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.avatar.AvatarEntities;
import com.newuniverse.nusmp.client.avatar.AvatarPlayerBody;
import com.newuniverse.nusmp.elite.hub.HubNpcs;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.PaintedConstructEntity;
import com.newuniverse.nusmp.entity.zogratis.DanteBoss;
import com.newuniverse.nusmp.entity.zogratis.VanicaBoss;
import com.newuniverse.nusmp.entity.zogratis.ZenonBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisEntities;
import com.newuniverse.nusmp.relics.order.OrderEntities;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Why: ONE place registers the kit's renderer for every converted character, and one hook lets another builder add more. {@link #init} adds a listener with the LOWEST priority, after the one of
 * {@code RemodelRenderers}, so the player-model renderer is the LAST one registered for each type and replaces the art-kit renderer (NeoForge keeps the last registration; the old renderers and
 * their models stay in the tree as legacy). Each conversion is registered inside its own try / catch, so a type that is renamed or missing logs an error and every other character still loads.
 *
 * <p>Extending (the dungeon's humanoids): call {@link #extend} once from client init code that runs in the mod constructor (before the renderer event), with a {@code Consumer} that calls
 * {@link #put} for each of your types; the consumer runs in the same event, after the built-in conversions, and a failure of yours is logged and skipped. The conversions of
 * {@link PlayerModelTable#CONVERTED_ENTITIES} are listed in docs/playermodel.md together with the humanoids that were NOT converted.
 */
public final class PlayerModelRegistry {
    private PlayerModelRegistry() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final List<Consumer<EntityRenderersEvent.RegisterRenderers>> EXTENSIONS = new ArrayList<>();

    /** Adds the listener to the mod bus, last in line (the same priority as RemodelRenderers, registered after it). Call from the client block of the mod constructor. */
    public static void init(IEventBus modBus) {
        PlayerAssets.init(modBus);
        modBus.addListener(EventPriority.LOWEST, PlayerModelRegistry::register);
    }

    /** Adds a registration that runs in the renderer event after the built-in ones. */
    public static synchronized void extend(Consumer<EntityRenderersEvent.RegisterRenderers> more) { EXTENSIONS.add(more); }

    /** Registers the kit's renderer for one entity type. */
    public static <T extends LivingEntity> void put(EntityRenderersEvent.RegisterRenderers e, EntityType<? extends T> type, PlayerBody<T> body) {
        @SuppressWarnings("unchecked") EntityType<T> t = (EntityType<T>) type;
        e.registerEntityRenderer(t, ctx -> new PlayerModelRenderer<>(ctx, body));
    }

    /** Registers any renderer for a type with the same guard (used by renderers that choose between the kit and an older one). */
    public static <T extends Entity> void putRaw(EntityRenderersEvent.RegisterRenderers e, EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        @SuppressWarnings("unchecked") EntityType<T> t = (EntityType<T>) type;
        e.registerEntityRenderer(t, provider);
    }

    private static void guard(String what, Runnable r) {
        try {
            r.run();
        } catch (RuntimeException | LinkageError ex) {
            LOG.error("[nusmp] the player-model renderer of {} could not be registered: {}", what, ex.toString());
        }
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        guard("arc_npc", () -> put(e, ArcEntities.NPC.get(), new PlayerBodies.ArcNpc()));
        PlayerBodies.Arc arc = new PlayerBodies.Arc();
        for (ArcEntities.Entry b : ArcEntities.bosses()) {
            if (!PlayerModelTable.ARC_BOSSES.contains(b.id())) continue;
            guard("arc boss " + b.id(), () -> {
                @SuppressWarnings("unchecked") EntityType<ArcBoss> type = (EntityType<ArcBoss>) b.type().get();
                put(e, type, arc);
            });
        }
        guard("dante_zogratis", () -> put(e, ZogratisEntities.DANTE.get(), new PlayerBodies.Triad<DanteBoss>("dante_zogratis")));
        guard("vanica_zogratis", () -> put(e, ZogratisEntities.VANICA.get(), new PlayerBodies.Triad<VanicaBoss>("vanica_zogratis")));
        guard("zenon_zogratis", () -> put(e, ZogratisEntities.ZENON.get(), new PlayerBodies.Triad<ZenonBoss>("zenon_zogratis")));
        guard("lucius_zogratis", () -> put(e, ZogratisEntities.LUCIUS.get(), new PlayerBodies.Lucius()));
        guard("dark_disciple", () -> put(e, ZogratisEntities.DARK_DISCIPLE.get(), new PlayerBodies.Disciple()));
        guard("zogratis_clone", () -> put(e, ZogratisEntities.ZOGRATIS_CLONE.get(), new PlayerBodies.Clone()));
        guard("devil_avatar", () -> put(e, com.newuniverse.nusmp.entity.avatar.AvatarEntities.DEVIL_AVATAR.get(), new PlayerBodies.DevilAvatar()));
        guard("zagred_legacy", () -> put(e, NUEntities.ZAGRED.get(), new PlayerBodies.LegacyZagred()));
        guard("god_of_order", () -> put(e, OrderEntities.GOD_OF_ORDER.get(), new PlayerBodies.Order()));
        guard("quest_giver_npc", () -> put(e, HubNpcs.QUEST_GIVER.get(), new PlayerBodies.HubNpc()));
        guard("merchant_npc", () -> put(e, HubNpcs.MERCHANT.get(), new PlayerBodies.HubNpc()));
        guard("tower_wizard", () -> put(e, NUEntities.TOWER_WIZARD.get(), new PlayerBodies.Wizard()));
        guard("avatar", () -> put(e, AvatarEntities.AVATAR.get(), new AvatarPlayerBody()));
        guard("painted_construct", () -> PaintedSplit.register(e));
        List<Consumer<EntityRenderersEvent.RegisterRenderers>> more;
        synchronized (PlayerModelRegistry.class) { more = new ArrayList<>(EXTENSIONS); }
        for (Consumer<EntityRenderersEvent.RegisterRenderers> c : more) guard("an extension", () -> c.accept(e));
    }

    /**
     * An entity type whose kinds are not all people (the painted construct: a knight is a person, a hound and a giant are not) keeps its old renderer for the kinds the kit does not draw. The
     * dispatcher picks per entity with {@code isPerson}; the old renderer is built from the provider that was registered before (the art kit's), asked for by the caller.
     */
    public static final class Split<T extends LivingEntity> extends EntityRenderer<T> {
        private final EntityRenderer<T> person, other;
        private final Predicate<T> isPerson;

        public Split(EntityRendererProvider.Context ctx, Predicate<T> isPerson, EntityRenderer<T> person, EntityRenderer<T> other) {
            super(ctx);
            this.person = person;
            this.other = other;
            this.isPerson = isPerson;
        }

        private EntityRenderer<T> pick(T e) { return isPerson.test(e) ? person : other; }

        @Override
        public net.minecraft.resources.ResourceLocation getTextureLocation(T e) { return pick(e).getTextureLocation(e); }

        @Override
        public boolean shouldRender(T e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return pick(e).shouldRender(e, f, x, y, z); }

        @Override
        public net.minecraft.world.phys.Vec3 getRenderOffset(T e, float partial) { return pick(e).getRenderOffset(e, partial); }

        @Override
        public void render(T e, float yaw, float partial, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
            pick(e).render(e, yaw, partial, pose, buffers, light);
        }
    }

    /** The painted construct: the knight on the kit, the hound and the giant on the art kit's renderer. */
    static final class PaintedSplit {
        private PaintedSplit() {}

        static void register(EntityRenderersEvent.RegisterRenderers e) {
            EntityType<PaintedConstructEntity> type = NUEntities.PAINTED_CONSTRUCT.get();
            e.registerEntityRenderer(type, ctx -> new Split<PaintedConstructEntity>(ctx, c -> c.kind() == PaintedConstructEntity.Kind.KNIGHT,
                    new PlayerModelRenderer<>(ctx, new PlayerBodies.PaintedKnight()), com.newuniverse.nusmp.client.remodel.RemodelRenderers.paintedOther(ctx)));
        }
    }
}
