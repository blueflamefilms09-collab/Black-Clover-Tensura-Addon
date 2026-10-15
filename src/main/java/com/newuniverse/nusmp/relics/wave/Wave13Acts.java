package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;

/** Wave 13: the active abilities of the nine blades (the server side of right-click and sneak + right-click). */
final class Wave13Acts {
    private Wave13Acts() {}

    static final String K_DRAW = "nusmp_w13_draw", K_AURA = "nusmp_w13_aura", K_FALLEN = "nusmp_w13_fallen", K_BANISH_CD = "nusmp_w13_banish_cd";
    static final int ETHEREAL_TICKS = 2400, AURA_TICKS = 1200;

    // ================================================================ Dream Glass
    static void etherealForm(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        Wave13Store.bag(Wave13Store.stack(c)).put("eth_until", now + ETHEREAL_TICKS);
        Fx.ring(p.serverLevel(), p.position(), 1.8, 0x9FEFFF);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.6f);
        Fx.say(p, "The glass blade turns to dream. For two minutes it passes through worn armour (not through barriers or natural hide).");
    }

    static void dreamDraw(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(K_DRAW, p.level().getGameTime() + 120);
        Fx.addEffect(p, "minecraft:invisibility", 30, 0);
        Fx.burst(p.serverLevel(), p.position(), 0x9FEFFF);
        Fx.say(p, "The blade fades out of the world: your next blow within six seconds comes from nowhere.");
    }

    // ================================================================ Axe of the Crimson Saint
    private static final String[] OFFERS = {"Nodol. You give nothing, and the axe gives nothing back until dawn.",
            "One heart offered. The axe bites harder (+1.5).", "Two hearts offered (+3).", "Three hearts offered (+4.5).", "Four hearts offered (+6).",
            "Todol. Everything: six hearts and more, and the axe crits one blow in four (+9)."};

    static void offerBlood(ActCtx c) {
        ServerPlayer p = c.player();
        var bag = Wave13Store.bag(Wave13Store.stack(c));
        long now = p.level().getGameTime();
        int cur = Wave13Rules.stage(bag, now);
        if (cur >= Wave13Rules.STAGE_EVERYTHING) {
            Fx.say(p, "You gave everything. The axe will ask again at dawn.");
            return;
        }
        int next = Wave13Rules.nextOffer(cur);
        Wave13Rules.setStage(bag, now, next);
        Fx.say(p, OFFERS[next]);
        RukhFx.spray(p.serverLevel(), RukhFx.Alignment.DARK, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
    }

    static void everything(ActCtx c) {
        ServerPlayer p = c.player();
        var bag = Wave13Store.bag(Wave13Store.stack(c));
        long now = p.level().getGameTime();
        if (Wave13Rules.stage(bag, now) >= Wave13Rules.STAGE_EVERYTHING) {
            Fx.say(p, "You already gave everything today.");
            return;
        }
        Wave13Rules.setStage(bag, now, Wave13Rules.STAGE_EVERYTHING);
        Fx.say(p, OFFERS[Wave13Rules.STAGE_EVERYTHING]);
        Fx.burst(p.serverLevel(), p.position(), 0xC01818);
    }

    // ================================================================ Palladion
    static void begone(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 7)) {
            if (Wave13Util.otherworldly(t)) {
                Fx.damage(p, t, 4.0f, "minecraft:magic");
                Wave13Util.banish(p, t, 100);
                n++;
            } else {
                double dx = p.getX() - t.getX(), dz = p.getZ() - t.getZ();
                t.knockback(0.8, dx, dz);
            }
        }
        Fx.ring(p.serverLevel(), p.position(), 7.0, 0xFFF0B0);
        Fx.say(p, n > 0 ? "\"Begone!\" " + n + (n == 1 ? " creature is" : " creatures are") + " sent back beyond the plane for five seconds." : "\"Begone!\" Nothing from beyond answers; the rest are driven back.");
    }

    static void sentinelSense(ActCtx c) {
        ServerPlayer p = c.player();
        List<LivingEntity> near = Wave13Util.otherworldlyNear(p, 32.0);
        for (LivingEntity t : near) Fx.addEffect(t, "minecraft:glowing", 100, 0);
        Fx.say(p, near.isEmpty() ? "Palladion is dark: nothing from beyond is near." : "Palladion shines: " + near.size() + " from beyond within 32 blocks, outlined for five seconds.");
    }

    // ================================================================ Humbling Longsword
    static void demandSurrender(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 6)) {
            if (Wave13Rules.surrenders(t.getHealth(), t.getMaxHealth()) || (t.getMaxHealth() <= 150.0f && t.getHealth() / t.getMaxHealth() <= 0.5f)) {
                Wave13Util.surrender(t);
                n++;
            }
        }
        Fx.say(p, n > 0 ? "\"Yield.\" " + n + (n == 1 ? " foe gives" : " foes give") + " up the fight." : "\"Yield.\" None of them is beaten enough to listen.");
    }

    // ================================================================ Mountainguard's Greataxe
    static void lastLine(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(K_AURA, p.level().getGameTime() + AURA_TICKS);
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.6f);
        Fx.ring(level, p.position(), 9.0, 0xFFD25A);
        Wave13Events.pulseAura(p);
        Fx.say(p, "The handle rings on the floor: for one minute you and your allies within 9 blocks cannot be frightened and are shielded.");
    }

    // ================================================================ Eschaton
    static void readCharge(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        long ch = Wave13Rules.charge(Wave13Store.bag(Wave13Store.stack(c)), now);
        int tier = Wave13Rules.tier(ch);
        String next = tier >= Wave13Rules.THRESHOLDS.length ? "all gifts open" : "next gift at " + Wave13Rules.THRESHOLDS[tier] + " charges";
        Fx.say(p, "Eschaton: " + (ch / 100) + " charges (tier " + tier + "), " + next + ". Spend magicules on spells to charge it; the charges fade at dawn.");
    }

    static void discharge(ActCtx c) {
        ServerPlayer p = c.player();
        var bag = Wave13Store.bag(Wave13Store.stack(c));
        long now = p.level().getGameTime();
        long ch = Wave13Rules.charge(bag, now);
        if (ch < 1500) {
            Fx.say(p, "Eschaton holds too little charge to discharge (15 needed, " + (ch / 100) + " held).");
            return;
        }
        float dmg = (float) Math.min(14.0, 3.0 + (ch / 100.0) * 0.18);
        Wave13Rules.clearCharge(bag, now);
        Fx.aoe(p, 5.0, dmg, "minecraft:magic", "minecraft:slowness", 40, 1);
        Fx.ring(p.serverLevel(), p.position(), 5.0, 0x6FFFE0);
        Fx.say(p, "The segmented blade lashes out and the charge is spent.");
    }

    // ================================================================ Contingency Plan
    static void setCircumstance(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave13Store.stack(c);
        var bag = Wave13Store.bag(s);
        Wave13Rules.Trigger t = Wave13Rules.nextTrigger((int) bag.get("trig"));
        bag.put("trig", t.ordinal());
        String stored = Wave13Store.getStr(s, "fx");
        Fx.say(p, "Contingency: it takes effect when " + t.when() + ". " + (Wave13Rules.decode(stored).isEmpty() ? "Nothing is stored yet (sneak + use with a potion in the off-hand)." : "A spell is stored."));
    }

    static void castIntoBlade(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave13Store.stack(c);
        var bag = Wave13Store.bag(s);
        long now = p.level().getGameTime();
        if (Wave13Rules.spentToday(bag, now)) {
            Fx.say(p, "The dagger is spent until the next dawn.");
            return;
        }
        ItemStack off = p.getOffhandItem();
        PotionContents contents = off.isEmpty() || !(off.getItem() instanceof PotionItem) ? null : off.get(DataComponents.POTION_CONTENTS);
        List<Wave13Rules.Stored> list = new ArrayList<>();
        if (contents != null) {
            for (MobEffectInstance inst : contents.getAllEffects()) {
                var key = inst.getEffect().unwrapKey();
                if (key.isPresent()) list.add(new Wave13Rules.Stored(key.get().location().toString(), inst.getDuration(), inst.getAmplifier()));
            }
        }
        String text = Wave13Rules.encode(list);
        if (text.isEmpty()) {
            Fx.say(p, "Hold a potion in the off-hand and sneak + use to touch it to the blade. One spell at a time.");
            return;
        }
        Wave13Store.putStr(s, "fx", text);
        if (!p.isCreative()) {
            off.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!p.getInventory().add(bottle)) p.drop(bottle, false);
        }
        Wave13Rules.Trigger t = Wave13Rules.triggerAt(bag.get("trig"));
        Fx.burst(p.serverLevel(), p.position(), 0x8FE8FF);
        Fx.say(p, "The spell sinks into the blade. It takes effect on you when " + t.when() + ", once, and the dagger rests until dawn.");
    }

    // ================================================================ Time Bender Dadao
    static void slowTime(ActCtx c) {
        ServerPlayer p = c.player();
        var bag = Wave13Store.bag(Wave13Store.stack(c));
        long now = p.level().getGameTime();
        int tier = Wave13Rules.timeTier(bag.get("wield"));
        if (tier < 2) {
            Fx.say(p, "The dadao has not yet opened its second tier (" + (Wave13Rules.untilNextTier(bag.get("wield")) / 1200) + " minutes of wielding to go).");
            return;
        }
        if (!Wave13Rules.useUp(bag, "slow", now, 1)) {
            Fx.say(p, "Time has bent once today already.");
            return;
        }
        for (LivingEntity t : Fx.foesAround(p, 14)) {
            int hold = t instanceof net.minecraft.world.entity.player.Player ? 60 : 120;         // the balance law: 3 s on a player
            Fx.addEffect(t, "minecraft:slowness", hold, 3);
            Fx.addEffect(t, "minecraft:mining_fatigue", hold, 1);
            Fx.addEffect(t, "tensura:movement_interference", hold, 0);
        }
        Fx.lendSkill(p, "tensura:thought_acceleration", 160);
        Fx.addEffect(p, "minecraft:speed", 160, 2);
        Fx.addEffect(p, "minecraft:haste", 160, 1);
        Fx.ring(p.serverLevel(), p.position(), 6.0, 0x3FD0FF);
        Fx.say(p, "Time slows for everyone but you: eight seconds of another turn.");
    }

    static void dimensionToss(ActCtx c) {
        ServerPlayer p = c.player();
        var bag = Wave13Store.bag(Wave13Store.stack(c));
        long now = p.level().getGameTime();
        if (Wave13Rules.timeTier(bag.get("wield")) < 3) {
            Fx.say(p, "The third tier is still closed (" + (Wave13Rules.untilNextTier(bag.get("wield")) / 1200) + " minutes of wielding to go).");
            return;
        }
        LivingEntity t = Wave13Util.lookTarget(p, 7.0);
        if (t == null) {
            Fx.say(p, "No foe in front of the blade.");
            return;
        }
        if (!Wave13Rules.useUp(bag, "toss", now, 1)) {
            Fx.say(p, "You tossed a foe through the planes today already.");
            return;
        }
        Fx.damage(p, t, 9.0f, "minecraft:magic");
        boolean big = t.getMaxHealth() > 200.0f;
        int hold = big ? 40 : t instanceof net.minecraft.world.entity.player.Player ? 60 : 120;   // the balance law: 3 s on a player
        Fx.addEffect(t, "minecraft:slowness", hold, big ? 3 : 9);
        if (!big) {
            Fx.addEffect(t, "tensura:spatial_blockade", hold, 0);
            Fx.addEffect(t, "minecraft:blindness", hold, 0);
        }
        RukhFx.spray(p.serverLevel(), RukhFx.Alignment.DARK, t.getX(), t.getY() + 1.0, t.getZ(), 50, 0.4, 0.7, 0.4, 0.5);
        Fx.say(p, "The target is flung into another dimension for a round and comes back dazed.");
    }

}
