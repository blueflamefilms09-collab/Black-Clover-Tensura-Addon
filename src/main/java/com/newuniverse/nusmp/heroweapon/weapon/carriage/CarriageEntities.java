package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Vassal Carriage is the one hero weapon that adds an entity to the game (the carriage you ride), and an entity type has to be registered on the mod
 * event bus before the registries open. The weapon's logic is built inside the mod constructor, after the shared start-up has stored the bus
 * ({@code HeroWeaponSystem.modBus()}), so its constructor calls {@link #init} once: it attaches this register and the attributes event to that bus. The type
 * is {@code nusmp:vassal_carriage_wagon}, 2.2 blocks wide and 1.7 high (the model is drawn larger, 3.2 blocks long), tracked by clients within 10 chunks, fire
 * immune, never saved with the world (a carriage is called, not kept: a restart has none), not available to {@code /summon}, and it seats its riders at the three
 * places of {@link CarriageNumbers#SEATS}.
 */
public final class CarriageEntities {
    private CarriageEntities() {}

    public static final String ID = "vassal_carriage_wagon";

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<CarriageEntity>> CARRIAGE = TYPES.register(ID,
            () -> EntityType.Builder.<CarriageEntity>of(CarriageEntity::new, MobCategory.MISC)
                    .sized(CarriageNumbers.WIDTH, CarriageNumbers.HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .passengerAttachments(seats())
                    .build(ID));

    private static boolean done;

    private static Vec3[] seats() {
        Vec3[] out = new Vec3[CarriageNumbers.SEATS.length];
        for (int i = 0; i < out.length; i++) out[i] = new Vec3(CarriageNumbers.SEATS[i][0], CarriageNumbers.SEATS[i][1], CarriageNumbers.SEATS[i][2]);
        return out;
    }

    /** Attaches the register and the attributes to the mod bus; safe to call twice. */
    public static synchronized void init(IEventBus modBus) {
        if (done || modBus == null) return;
        done = true;
        TYPES.register(modBus);
        modBus.addListener(CarriageEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent e) { e.put(CARRIAGE.get(), CarriageEntity.attributes().build()); }
}
