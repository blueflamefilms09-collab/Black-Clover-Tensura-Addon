package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Why: thirty bosses must not mean thirty hand-written registrations in four places (the entity type, the attributes event, the renderer event, the
 * creative tab). A boss class makes ONE call here from a static field and everything else is derived: the entity type is registered under the
 * character's id, its attributes are handed to the attributes event, and the client's one generic renderer reads the {@link ArcLook} that was stated
 * next to the type. The classes holding those static fields are loaded by {@code ArcCharacters.bootstrap()}, which runs before the registry events, so the
 * deferred register already knows every type when the game asks for them.
 */
public final class ArcEntities {
    private ArcEntities() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    /** One registered boss: its type, how it is drawn, and its attributes. */
    public record Entry(String id, DeferredHolder<EntityType<?>, ? extends EntityType<? extends ArcBoss>> type, ArcLook look, Supplier<AttributeSupplier.Builder> attributes) {}

    private static final List<Entry> BOSSES = new ArrayList<>();

    /** The one entity type of every friendly character ({@code nusmp:arc_npc}); which character it is is its synced id. */
    public static final DeferredHolder<EntityType<?>, EntityType<ArcNpcEntity>> NPC = TYPES.register("arc_npc",
            () -> EntityType.Builder.of(ArcNpcEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("arc_npc"));

    /**
     * Registers a boss type: {@code id} is the entity path ({@code nusmp:<id>}), {@code width} and {@code height} its hitbox, {@code look} how it is drawn,
     * {@code attributes} its stats ({@code ZogratisBoss.bossAttributes(...)} plus whatever it adds).
     */
    public static <T extends ArcBoss> DeferredHolder<EntityType<?>, EntityType<T>> boss(String id, EntityType.EntityFactory<T> factory, float width, float height,
                                                                                         ArcLook look, Supplier<AttributeSupplier.Builder> attributes) {
        DeferredHolder<EntityType<?>, EntityType<T>> holder = TYPES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height).clientTrackingRange(10).fireImmune().build(id));
        synchronized (BOSSES) { BOSSES.add(new Entry(id, holder, look, attributes)); }
        return holder;
    }

    /** Every boss registered so far (the client registers one renderer per entry). */
    public static List<Entry> bosses() {
        synchronized (BOSSES) { return Collections.unmodifiableList(new ArrayList<>(BOSSES)); }
    }

    /** Called once from the mod constructor, after {@code ArcCharacters.bootstrap()}. */
    public static void init(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(ArcEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(NPC.get(), ArcNpcEntity.attributes().build());
        for (Entry b : bosses()) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"}) EntityType<? extends net.minecraft.world.entity.LivingEntity> type = (EntityType) b.type().get();
                e.put(type, b.attributes().get().build());
            } catch (RuntimeException ex) {
                com.mojang.logging.LogUtils.getLogger().error("[nusmp] arc boss {} has no attributes: {}", b.id(), ex.toString());
            }
        }
    }
}
