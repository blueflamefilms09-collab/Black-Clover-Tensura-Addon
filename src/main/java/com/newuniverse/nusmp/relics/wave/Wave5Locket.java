package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Lovelorn Locket. The tabletop card is a social charm (advantage on Persuasion, any former soldier will house you for the night). Here the
 * charm makes villagers deal fairly (the Hero of the Village effect, a passive) and the "former soldier" is a weaponsmith, armourer or
 * toolsmith villager, or an iron golem: once a day, beside one, the wearer is billeted for the night: their bed spot is set here, they are fed
 * and healed and a long rest's worth of regeneration runs through them.
 */
final class Wave5Locket {
    private Wave5Locket() {}

    static final String ID = "lovelorn_locket";
    static final double REACH = 24.0;

    /** Billeted for the night (once a day: counted here, so a try with no soldier near does not use the day up). */
    static void billet(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        ItemStack s = Wave5Data.live(p, ID, c.stack());
        if (s.isEmpty()) return;
        CompoundTag st = Wave5Data.stack(s);
        long today = Wave5Pure.day(l.getServer().overworld().getDayTime());
        if (st.contains("billet_day") && st.getLong("billet_day") == today) {
            Wave5Aim.msg(p, "You have been billeted once today already.", ChatFormatting.GRAY);
            return;
        }
        List<Villager> folk = l.getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(REACH), v -> v.isAlive() && !v.isBaby() && soldier(v));
        boolean golem = folk.isEmpty() && !l.getEntitiesOfClass(IronGolem.class, p.getBoundingBox().inflate(REACH), g -> g.isAlive()).isEmpty();
        if (folk.isEmpty() && !golem) {
            Wave5Aim.msg(p, "No old soldier within " + (int) REACH + " blocks: a weaponsmith, an armourer, a toolsmith or an iron golem.", ChatFormatting.GRAY);
            return;
        }
        st.putLong("billet_day", today);
        Wave5Data.save(s, st);
        p.setRespawnPosition(l.dimension(), p.blockPosition(), p.getYRot(), true, true);   // forced: a bed is not needed for the soldier's billet to hold
        Fx.heal(p, p.getMaxHealth() * 0.5f);
        p.getFoodData().eat(8, 0.8f);
        Fx.addEffect(p, "minecraft:regeneration", 400, 1);
        Wave5Aim.puff(l, p.position().add(0, 1.4, 0), com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 8, 0.5);
        Wave5Aim.sound(l, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        Wave5Aim.msg(p, "An old soldier opens a door to you. You sleep warm, and you will wake here if you fall.", ChatFormatting.LIGHT_PURPLE);
    }

    private static boolean soldier(Villager v) {
        VillagerProfession pr = v.getVillagerData().getProfession();
        return pr == VillagerProfession.WEAPONSMITH || pr == VillagerProfession.ARMORER || pr == VillagerProfession.TOOLSMITH;
    }
}
