package com.newuniverse.nusmp.arc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Why: the friendly characters of the arcs (thirty of them) are ONE entity type, {@code nusmp:arc_npc}, whose identity is a synced string, exactly like a
 * villager's profession. Their look (a model of the art kit), their lines and their quests are data keyed by that id, so adding an NPC is a record in the
 * roster, a Python model spec and a dialogue file, never a new Java class, a new registration or a new renderer. This is the skeleton the other builders
 * compile against; the talking, the story state and the interaction are filled in by the arc core builder.
 */
public class ArcNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> CHARACTER = SynchedEntityData.defineId(ArcNpcEntity.class, EntityDataSerializers.STRING);

    public ArcNpcEntity(EntityType<? extends ArcNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARACTER, "");
    }

    /** The roster id of the character this NPC is ("asta"), or empty for a blank one. */
    public String characterId() { return entityData.get(CHARACTER); }

    /** The character record, or null when the id is empty or not registered (an old world whose character was removed). */
    public ArcCharacter character() { return ArcRoster.byId(characterId()); }

    public void setCharacter(String id) { entityData.set(CHARACTER, id == null ? "" : id); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Character", characterId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCharacter(tag.getString("Character"));
    }
}
