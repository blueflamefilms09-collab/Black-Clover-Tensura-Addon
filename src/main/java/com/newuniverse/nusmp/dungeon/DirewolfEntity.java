package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.dungeon.rework.BeastBrain;
import com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.BeastKind;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** A Direwolf (tier 3): a fast, heavy hunter of Living Armors and slimes that guards the place it sleeps in. Sleeps when full. */
public class DirewolfEntity extends DungeonCreature {
    public DirewolfEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 28).add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override public int tier() { return 3; }

    @Override public String soundFamily() { return "beast"; }

    @Override public boolean isPrey(LivingEntity e) { return e instanceof LivingArmorEntity || e instanceof DungeonSlimeEntity; }

    @Override public boolean huntsPlayers() { return true; }

    /** Dungeon-boss rework (docs/dungeon_bosses_rework.md): the signature moves, plain class only (the Floor Warden and the Guardian keep their own fight), behind {@code dungeonBosses.creatures}. */
    private BeastBrain moves;

    @Override
    protected void reworkTick(ServerLevel sl) {
        if (getClass() != DirewolfEntity.class || !BossRulesConfig.dungeonCreatures()) {
            if (moves != null) moves.reset();
            return;
        }
        if (moves == null) moves = new BeastBrain(BeastKind.DIREWOLF);
        moves.tick(this, sl);
    }

    @Override protected boolean busy() { return moves != null && moves.busy(); }
}
