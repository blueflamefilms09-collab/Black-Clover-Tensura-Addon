package com.newuniverse.nusmp.prop;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Preview stub of the mod's MagicPropEntity: the read API the painters use (kind, scale, life, maxLife, lifeFrac, param, seed, health,
 * owner, target, plus Entity's getX / getY / getZ, tickCount, getBbWidth / getBbHeight, getYRot, level()). The scene sets the values.
 * Like the game's, owner() is ALWAYS null on the client (the real one needs the ServerLevel); the preview counts the calls and warns.
 */
public class MagicPropEntity extends Entity {
    private PropKind kind = PropKind.values()[0];
    private float scale = 1f, health;
    private int life, maxLife = 100, param, seed;
    private UUID ownerId;
    private Entity target;
    /** Preview only: how often a painter asked for owner(). */
    public int previewOwnerCalls;

    public MagicPropEntity(EntityType<? extends MagicPropEntity> type, Level level) { super(type, level); }

    public PropKind kind() { return kind; }
    public float scale() { return scale; }
    public int life() { return life; }
    public int maxLife() { return maxLife; }
    /** 0..1 of its life used. */
    public float lifeFrac() { return Math.min(1f, life / (float) Math.max(1, maxLife())); }
    public int param() { return param; }
    public void setParam(int v) { param = v; }
    public int seed() { return seed; }
    public float health() { return health; }
    public UUID ownerId() { return ownerId; }

    public LivingEntity owner() {
        previewOwnerCalls++;
        return null;
    }

    public void setTarget(Entity t) { target = t; }
    public Entity target() { return target; }

    /** Preview helper (not in the game): sets everything the scene describes. */
    public void previewSet(PropKind kind, float scale, int maxLife, int param, int seed, float health) {
        this.kind = kind;
        this.scale = scale;
        this.maxLife = maxLife;
        this.param = param;
        this.seed = seed;
        this.health = health;
    }

    public void previewSetLife(int life) { this.life = life; }
}
