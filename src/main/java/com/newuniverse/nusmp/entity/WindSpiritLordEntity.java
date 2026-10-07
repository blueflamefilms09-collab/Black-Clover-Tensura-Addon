package com.newuniverse.nusmp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 0.53 Sylph, the Wind Spirit Lord, as a boss (command: /multiverse boss sylph). STUB: the attributes and the summon are real, the fight
 * (flying AI, boss bar, phases, wind attacks, drops) and the look (client/WindSpiritLordRenderer: geo.json model, translucent iridescent
 * wings, glow, wind trails) are being built; see docs/sylph_wind_spirit_lord.md.
 */
public class WindSpiritLordEntity extends Monster {
    public WindSpiritLordEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 300;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 400).add(Attributes.ATTACK_DAMAGE, 8).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    /** Summons the boss at 'at' (the command). */
    public static WindSpiritLordEntity summon(ServerLevel sl, Vec3 at) {
        WindSpiritLordEntity e = NUEntities.WIND_SPIRIT_LORD.get().create(sl);
        if (e == null) return null;
        e.moveTo(at.x, at.y, at.z, 0, 0);
        sl.addFreshEntity(e);
        return e;
    }
}
