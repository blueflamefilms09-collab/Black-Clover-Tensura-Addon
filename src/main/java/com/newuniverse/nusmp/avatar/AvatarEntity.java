package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Contract skeleton (AV1 fills in everything: registration, goals, outcome engine, saving; keep every public signature below, other builders compile against
 * them). The Avatar: a player-made NPC (Genesis Core item, creation GUI), see docs/avatar/*.md.
 */
public class AvatarEntity extends PathfinderMob {
    public static final EntityDataAccessor<Boolean> IS_FRENZIED = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<CompoundTag> APPEARANCE = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    public static final EntityDataAccessor<String> PRIMARY = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> SECONDARY = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> RACE = SynchedEntityData.defineId(AvatarEntity.class, EntityDataSerializers.STRING);

    protected AvatarEntity(EntityType<? extends AvatarEntity> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(IS_FRENZIED, false).define(APPEARANCE, new CompoundTag()).define(PRIMARY, AvatarPersonality.LOYAL.name()).define(SECONDARY, "").define(RACE, "");
    }

    public UUID creatorUuid() { return null; }
    public ServerPlayer creatorPlayer() { return null; }
    public String avatarName() { return getName().getString(); }
    public boolean isNamed() { return false; }
    public AvatarPersonality primary() { return AvatarPersonality.byName(getEntityData().get(PRIMARY)); }
    /** Null when the Avatar has no secondary personality. */
    public AvatarPersonality secondary() { return AvatarPersonality.byName(getEntityData().get(SECONDARY)); }
    public boolean frenzied() { return getEntityData().get(IS_FRENZIED); }
    public void setFrenzied(boolean on) { getEntityData().set(IS_FRENZIED, on); }
    public AppearanceData appearance() { return AppearanceData.fromTag(getEntityData().get(APPEARANCE)); }
    public double storedLuck() { return 0.0; }
    public String raceId() { return getEntityData().get(RACE); }
    /** Multiplier on skill cooldowns (1.0 normal, 0.1 in a Yandere frenzy); AV2's cast goal multiplies its cooldowns with it. */
    public float cooldownMultiplier() { return 1.0f; }
    /** True when this personality (primary or secondary) is active for the Avatar. */
    public boolean has(AvatarPersonality p) { return primary() == p || secondary() == p; }

}
