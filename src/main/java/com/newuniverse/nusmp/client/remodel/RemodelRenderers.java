package com.newuniverse.nusmp.client.remodel;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.DungeonCreature;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

/**
 * Why: ONE call registers the art-kit renderer of every remodelled mob (the rows of {@link RemodelTable}): the framework bosses get a {@link KitBossRenderer}, every other living mob a
 * {@link KitMobRenderer} with the {@link KitProbes} of its kind, the cotton cloud its own small renderer and the legacy Zagred the one that keeps its aura and target ring. The entity types are looked
 * up by id in the registry (not through the classes that register them), so a mob that another part of the mod renames or removes is logged and skipped and never stops the others from loading. The
 * listener is added with the LOWEST priority (see {@link #init}) so it runs after the registrations of the old renderers (which stay in the tree, as legacy) and the new look is the one that is
 * drawn: NeoForge keeps the last renderer registered for a type. Call {@link #init} from the mod constructor on the client only.
 */
public final class RemodelRenderers {
    private RemodelRenderers() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** Adds the listener to the mod bus, last in line. */
    public static void init(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, RemodelRenderers::register);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        for (RemodelTable.Entry en : RemodelTable.ENTRIES) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, en.entityId())).orElse(null);
            if (type == null) {
                LOG.warn("[nusmp] the remodelled mob {} is not registered: no art-kit renderer for it", en.entityId());
                continue;
            }
            try {
                registerOne(e, en, type);
            } catch (RuntimeException ex) {
                LOG.error("[nusmp] the art-kit renderer of {} could not be registered: {}", en.entityId(), ex.toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> void put(EntityRenderersEvent.RegisterRenderers e, EntityType<?> type, EntityRendererProvider<T> provider) {
        e.registerEntityRenderer((EntityType<T>) type, provider);
    }

    private static <T extends LivingEntity> void mob(EntityRenderersEvent.RegisterRenderers e, EntityType<?> type, String model, KitMobRenderer.Size size, KitMobRenderer.Probe<T> probe) {
        RemodelRenderers.<T>put(e, type, ctx -> new KitMobRenderer<T>(ctx, model, size, probe));
    }

    /** One case per family (RemodelRenderersTest fails when a family of the table has none, or an entity of the table has no look or probe below). */
    private static void registerOne(EntityRenderersEvent.RegisterRenderers e, RemodelTable.Entry en, EntityType<?> type) {
        switch (en.family()) {
            case BOSS -> boss(e, en.entityId(), type);
            case MOB -> mobOf(e, en.entityId(), type);
            case CLOUD -> RemodelRenderers.<com.newuniverse.nusmp.entity.CottonCloudEntity>put(e, type, KitCloudRenderer::new);
            case ZAGRED -> RemodelRenderers.<com.newuniverse.nusmp.entity.ZagredBossEntity>put(e, type, KitZagredRenderer::new);
        }
    }

    // ------------------------------------------------------------------------------------------------ the framework bosses
    /** The bigger of two models' boxes, so a boss with a second form is culled by the one that reaches farther. */
    private static double growXz(String a, String b) { return Math.max(RemodelLooks.of(a).growXz(), b == null ? 0f : RemodelLooks.of(b).growXz()); }

    private static double growUp(String a, String b) { return Math.max(RemodelLooks.of(a).growUp(), b == null ? 0f : RemodelLooks.of(b).growUp()); }

    private static <T extends ZogratisBoss> KitBossRenderer.Look<T> look(String model, boolean split, boolean secondFromTransform, boolean follows, int flash, boolean pitches,
                                                                          double down, KitBossRenderer.Hooks<T> hooks) {
        String second = RemodelTable.SECOND_FORMS.contains(model) ? RemodelTable.secondForm(model) : null;
        return new KitBossRenderer.Look<>(model, second, split, secondFromTransform, follows, 1f, flash, pitches, growXz(model, second), growUp(model, second), down, hooks);
    }

    private static <T extends ZogratisBoss> void bossOf(EntityRenderersEvent.RegisterRenderers e, EntityType<?> type, KitBossRenderer.Look<T> look) {
        RemodelRenderers.<T>put(e, type, ctx -> new KitBossRenderer<T>(ctx, look));
    }

    private static void boss(EntityRenderersEvent.RegisterRenderers e, String id, EntityType<?> type) {
        switch (id) {
            // the Triad: the hybrid body is worn from the first frame of the transformation and the hitbox grows with it
            case "dante_zogratis", "vanica_zogratis", "zenon_zogratis" -> bossOf(e, type, look(id, false, true, true, -1, false, 0.5, null));
            case "lucius_zogratis" -> bossOf(e, type, look(id, true, false, false, -1, false, 0.5, KitBossHooks.LUCIUS));
            case "devil_avatar" -> bossOf(e, type, look(id, true, false, true, -1, false, 0.5, KitBossHooks.AVATAR));
            case "god_of_order" -> bossOf(e, type, look(id, true, false, false, 0xE8B923, false, 0.5, new KitBossHooks.Order()));
            case "goliath" -> bossOf(e, type, look(id, false, false, true, 0xFFAA33, false, 0.5, KitBossHooks.GOLIATH));
            case "charybdis_spawn" -> bossOf(e, type, look(id, false, false, false, 0x2FD6C8, true, 3.0, null));
            default -> throw new IllegalArgumentException("no boss look for " + id);
        }
    }

    // ------------------------------------------------------------------------------------------------ every other living mob
    private static void mobOf(EntityRenderersEvent.RegisterRenderers e, String id, EntityType<?> type) {
        switch (id) {
            case "dark_disciple" -> mob(e, type, "dark_disciple", KitMobRenderer.Size.fit(), KitProbes.DISCIPLE);
            case "blood_beast" -> mob(e, type, "blood_beast", KitMobRenderer.Size.fit(1.2f), KitProbes.BLOOD_BEAST);
            case "zogratis_clone" -> mob(e, type, "lucius_zogratis", KitMobRenderer.Size.fit(), KitProbes.CLONE);
            case "dungeon_direwolf" -> mob(e, type, "dungeon_direwolf", KitMobRenderer.Size.fit(1.3f), new KitProbes.Sleeper<DungeonCreature>("dungeon_direwolf"));
            case "basilisk" -> mob(e, type, "basilisk", KitMobRenderer.Size.fit(1.15f), new KitProbes.Sleeper<DungeonCreature>("basilisk"));
            case "dungeon_slime" -> mob(e, type, "dungeon_slime", KitMobRenderer.Size.fit(1.3f), KitProbes.SLIME);
            case "living_armor", "floor_warden", "labyrinth_guardian" -> mob(e, type, id.equals("living_armor") ? "living_armor" : "floor_warden", KitMobRenderer.Size.fit(), KitProbes.ARMOUR);
            case "spirit_lord" -> mob(e, type, "spirit_salamander", KitMobRenderer.Size.fit(), KitProbes.SPIRIT_LORD);
            case "elemental_spirit_lord" -> mob(e, type, "spirit_salamander", KitMobRenderer.Size.fit(), KitProbes.ELEMENTAL);
            case "wind_spirit_lord" -> mob(e, type, "spirit_sylph", KitMobRenderer.Size.fit(), KitProbes.WIND);
            case "grimoire_daemon" -> mob(e, type, "grimoire_daemon", KitMobRenderer.Size.fit(), KitProbes.DAEMON);
            case "painted_construct" -> mob(e, type, "painted_knight", KitMobRenderer.Size.fit(1.15f), KitProbes.PAINTED);
            case "rouge" -> mob(e, type, "rouge", KitMobRenderer.Size.fit(), KitProbes.ROUGE);
            case "tower_wizard" -> mob(e, type, "tower_wizard", KitMobRenderer.Size.fit(), KitProbes.WIZARD);
            case "cotton_sheep" -> mob(e, type, "cotton_sheep", KitMobRenderer.Size.fit(), KitProbes.SHEEP);
            default -> throw new IllegalArgumentException("no mob probe for " + id);
        }
    }
}
