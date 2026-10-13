package com.newuniverse.nusmp.entity.avatar;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.entity.zogratis.DevilStyle;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Everything the Devil Avatar registers: its entity type (one type, two variants), its attributes and a spawn egg (registered through
 * {@link NUItems#ITEMS}). Wire it in NUSMP: call {@link #init()}, {@code ENTITIES.register(modEventBus)} and
 * {@code modEventBus.addListener(AvatarEntities::attributes)}; the client registers its renderer with {@code AvatarClient::register}.
 */
public final class AvatarEntities {
    private AvatarEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DevilAvatarBoss>> DEVIL_AVATAR = ENTITIES.register("devil_avatar",
            () -> EntityType.Builder.<DevilAvatarBoss>of(DevilAvatarBoss::new, MobCategory.MONSTER).sized(0.9f, 3.2f).fireImmune().clientTrackingRange(24).updateInterval(1).build("devil_avatar"));

    public static final DeferredItem<DeferredSpawnEggItem> DEVIL_AVATAR_EGG = NUItems.ITEMS.register("devil_avatar_spawn_egg",
            () -> new DeferredSpawnEggItem(DEVIL_AVATAR, 0x120D1C, 0x8A4DE0, new Item.Properties()));

    /** The egg, for the creative tab. */
    public static List<DeferredItem<DeferredSpawnEggItem>> spawnEggs() { return List.of(DEVIL_AVATAR_EGG); }

    /** Loads this class (and so registers the egg) before the item register's event; call it from the NUSMP constructor. */
    public static void init() {}

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(DEVIL_AVATAR.get(), DevilAvatarBoss.createAttributes().build());
    }

    public static boolean isAvatar(Entity e) { return e != null && e.getType() == DEVIL_AVATAR.get(); }

    /**
     * Spawns an Avatar of 'variant' at 'at' with its arena at 'arena' (the block it stands on when null) and the collapse centred on 'centre'
     * (the arena when null). With 'flourish' there is a summoning circle and a sound. Null if it could not be created.
     */
    public static DevilAvatarBoss spawn(ServerLevel sl, Vec3 at, float yaw, int variant, BlockPos arena, BlockPos centre, boolean flourish) {
        DevilAvatarBoss boss = DEVIL_AVATAR.get().create(sl);
        if (boss == null) return null;
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setVariant(variant);
        boss.setArena(arena != null ? arena : BlockPos.containing(at));
        boss.setCentre(centre);
        sl.addFreshEntity(boss);
        if (flourish) {
            VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, at.add(0, 0.05, 0), at.add(0, 1, 0), 0xFF000000 | DevilStyle.color(boss.devil()), 60, 3.4f);
            sl.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.5f);
        }
        return boss;
    }
}
