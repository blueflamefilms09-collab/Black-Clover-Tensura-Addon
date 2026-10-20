package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the active abilities of the twelve weapons of wave 20 (the server side of right-click, sneak + right-click and the worn-relic key), one method per
 * ability, so {@link Wave20} stays a readable list of definitions.
 */
final class Wave20Acts {
    private Wave20Acts() {}

    // keys in the player's persistent data (all timers are game-time ticks)
    static final String K_GRAP_T = "nusmp_w20_grap_t", K_GRAP_UNTIL = "nusmp_w20_grap_until", K_GRAP_NEXT = "nusmp_w20_grap_next";
    static final String K_STAR_AT = "nusmp_w20_star_at", K_STAR_X = "nusmp_w20_star_x", K_STAR_Y = "nusmp_w20_star_y", K_STAR_Z = "nusmp_w20_star_z", K_STAR_E = "nusmp_w20_star_e";
    static final String K_CARN_N = "nusmp_w20_carn_n", K_CARN_AT = "nusmp_w20_carn_at";
    static final String K_AURA = "nusmp_w20_aura", K_HOVER = "nusmp_w20_hover", K_LIT = "nusmp_w20_lit";

    // ================================================================ Demon Shovel
    /** Evil gardening: crops, saplings and grass within five blocks are coaxed to grow, as if with bone meal (never in the Labyrinth or the Dungeon). */
    static void evilGardening(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        if (Wave20Util.protectedWorld(level)) {
            Fx.say(p, "The stone of this place grows nothing. The shovel only hums.");
            return;
        }
        int[] grown = {0};
        BlockPos o = p.blockPosition();
        DungeonGuard.internal(() -> {
            for (int dx = -5; dx <= 5 && grown[0] < 12; dx++) {
                for (int dz = -5; dz <= 5 && grown[0] < 12; dz++) {
                    for (int dy = -1; dy <= 2 && grown[0] < 12; dy++) {
                        BlockPos pos = o.offset(dx, dy, dz);
                        BlockState st = level.getBlockState(pos);
                        if (level.mayInteract(p, pos) && st.getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(level, pos, st)) {
                            b.performBonemeal(level, p.getRandom(), pos, st);
                            grown[0]++;
                        }
                    }
                }
            }
        });
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_20, FxTone.DARK, p.getX(), p.getY() + 0.3, p.getZ(), 12, 2.5, 0.2, 2.5, 0.02);
        Fx.say(p, grown[0] > 0 ? "Evil gardening: " + grown[0] + " plants lean in and grow, a little too eagerly." : "Nothing here wants to grow, evilly or otherwise.");
    }

    /** Evil hole digging: a pit of shadow opens where the shovel points; foes in it are hurt, slowed and weakened for a moment. */
    static void evilHole(ActCtx c) {
        ServerPlayer p = c.player();
        Vec3 look = p.getLookAngle();
        double reach = Math.min(5.0, Wave20Util.lineLength(p, 5.0));
        Vec3 centre = p.getEyePosition().add(look.scale(reach));
        int n = 0;
        for (LivingEntity t : Wave20Util.foesNear(p, centre, 3.0)) {
            Fx.damage(p, t, 5.0f, "minecraft:wither");
            Fx.addEffect(t, "minecraft:slowness", 60, Wave20Util.isBig(t) ? 1 : 3);
            Fx.addEffect(t, "minecraft:weakness", 60, 0);
            t.setDeltaMovement(t.getDeltaMovement().x * 0.2, -0.8, t.getDeltaMovement().z * 0.2);
            t.hurtMarked = true;
            n++;
        }
        FxRelic.ring(p.serverLevel(), centre, 3.0, 0x2A1038);
        Fx.say(p, n > 0 ? "A pit of shadow opens: " + n + (n == 1 ? " foe tumbles" : " foes tumble") + " in." : "A pit of shadow opens, and nothing falls in.");
    }

    // ================================================================ Dragoon's Halberd
    static void chooseElement(ActCtx c) {
        var bag = Wave20Store.bag(Wave20Store.stack(c));
        Wave20Rules.Element e = Wave20Rules.Element.of(bag.get("elem")).next();
        bag.put("elem", e.ordinal());
        Fx.say(c.player(), "The halberd's drop attack now carries " + e.label() + ".");
    }

    /** The Dragoon's leap: up and forward, ready to come down on a foe. */
    static void dragoonLeap(ActCtx c) {
        ServerPlayer p = c.player();
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(look.x * 0.6, 1.05, look.z * 0.6);
        p.hurtMarked = true;
        p.fallDistance = 0;
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_20, p.position(), FxTone.LIGHT, 8);
        Fx.say(p, "You leap. Come down on something large.");
    }

    // ================================================================ Efreeti's Kusarigama
    static void grip(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave20Util.lookTarget(p, 9.0);
        if (t == null) {
            Fx.say(p, "No foe within the chain's reach.");
            return;
        }
        long now = p.level().getGameTime();
        CompoundTag pd = p.getPersistentData();
        pd.putUUID(K_GRAP_T, t.getUUID());
        pd.putLong(K_GRAP_UNTIL, now + Wave20Rules.GRAPPLE_TICKS);
        pd.putLong(K_GRAP_NEXT, now + Wave20Rules.GRAPPLE_STEP);
        Fx.damage(p, t, Wave20Rules.GRAPPLE_DAMAGE, "minecraft:in_fire");
        t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
        Wave20Events.pull(p, t);
        com.newuniverse.nusmp.particle.fx.Fx.stream(p.serverLevel(), FxThemes.RELIC_WAVE_20, p, t, FxTone.LIGHT, 6);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);
        Fx.say(p, "The burning chain wraps round the foe and holds it for six seconds, scorching it as it struggles.");
    }

    // ================================================================ Ilmare Gin
    static void starlightThrow(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        LivingEntity t = Wave20Util.lookTarget(p, 20.0);
        Vec3 eye = p.getEyePosition();
        Vec3 end;
        CompoundTag pd = p.getPersistentData();
        pd.remove(K_STAR_E);
        if (t != null) {
            end = t.getBoundingBox().getCenter();
            Fx.damage(p, t, 4.5f, "minecraft:player_attack");
            pd.putUUID(K_STAR_E, t.getUUID());
        } else {
            end = eye.add(p.getLookAngle().scale(Math.min(20.0, Wave20Util.lineLength(p, 20.0)) - 0.4));
        }
        pd.putLong(K_STAR_AT, level.getGameTime());
        pd.putDouble(K_STAR_X, end.x);
        pd.putDouble(K_STAR_Y, end.y);
        pd.putDouble(K_STAR_Z, end.z);
        com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_20, eye.add(0, -0.3, 0), end, FxTone.LIGHT, 6);
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_20, end, FxTone.LIGHT, 8);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
        Fx.say(p, (t != null ? "The katana flies into the foe." : "The katana flies and bites into the " + "mark.") + " Sneak + use within five seconds to step to it.");
    }

    static void starStep(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        CompoundTag pd = p.getPersistentData();
        long now = level.getGameTime();
        if (!Wave20Rules.stepOpen(now, pd.getLong(K_STAR_AT))) {
            Fx.say(p, "The katana has not struck anything lately. Use it first, then sneak + use to step to the mark.");
            return;
        }
        Vec3 dest = new Vec3(pd.getDouble(K_STAR_X), pd.getDouble(K_STAR_Y), pd.getDouble(K_STAR_Z));
        if (pd.hasUUID(K_STAR_E) && level.getEntity(pd.getUUID(K_STAR_E)) instanceof LivingEntity t && t.isAlive()) {
            Vec3 away = p.position().subtract(t.position());
            Vec3 dir = away.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : away.normalize();
            dest = t.position().add(dir.x * 1.4, 0.0, dir.z * 1.4);
        } else {
            dest = dest.add(0, -p.getEyeHeight() * 0.5, 0);
        }
        Vec3 spot = Wave20Util.safeSpot(level, p, dest);
        if (spot == null) {
            Fx.say(p, "There is no room to stand where the katana struck.");
            return;
        }
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_20, p.position(), FxTone.LIGHT, 8);
        p.teleportTo(spot.x, spot.y, spot.z);
        p.fallDistance = 0;
        pd.remove(K_STAR_AT);
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_20, spot.add(0, 1, 0), FxTone.LIGHT, 10);
        level.playSound(null, p.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.4f);
        Fx.say(p, "You step along the starlight and take the katana in hand.");
    }

    // ================================================================ Katana of Carnage
    static void bloodlust(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag pd = p.getPersistentData();
        long now = p.level().getGameTime();
        int stacks = Wave20Rules.carnageNow(pd.getInt(K_CARN_N), pd.getLong(K_CARN_AT), now);
        if (stacks < 1) {
            Fx.say(p, "No carnage to spend: the katana needs a kill in the last eight seconds.");
            return;
        }
        int ticks = Wave20Rules.bloodlustTicks(stacks);
        Fx.addEffect(p, "minecraft:speed", ticks, 1);
        Fx.addEffect(p, "minecraft:haste", ticks, Math.min(3, stacks - 1));
        Fx.heal(p, stacks * 1.0f);
        pd.putInt(K_CARN_N, 0);
        pd.putLong(K_CARN_AT, 0L);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_20, p, FxTone.DARK, 10);
        Fx.say(p, "Bloodlust: " + stacks + " stacks of carnage become " + (ticks / 20) + " seconds of speed and haste, and a little blood back.");
    }

    // ================================================================ Katana of the Crimson Shogun
    static void steelWindStrike(ActCtx c) {
        ServerPlayer p = c.player();
        List<LivingEntity> foes = Wave20Util.foesNear(p, p.position(), 8.0);
        if (foes.size() > 5) foes = foes.subList(0, 5);
        LivingEntity last = null;
        for (LivingEntity t : foes) {
            Fx.damage(p, t, 7.0f, "minecraft:magic");
            com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_20, t, FxTone.LIGHT, 6);
            last = t;
        }
        if (last != null) {
            Vec3 spot = Wave20Util.safeSpot(p.serverLevel(), p, last.position().add(1.2, 0, 0));
            if (spot != null) {
                p.teleportTo(spot.x, spot.y, spot.z);
                p.fallDistance = 0;
            }
        }
        FxRelic.ring(p.serverLevel(), p.position(), 4.0, 0xE8F0FF);
        Fx.say(p, foes.isEmpty() ? "Steel Wind Strike: the wind has nothing to cut." : "Steel Wind Strike: you flicker between " + foes.size() + (foes.size() == 1 ? " foe." : " foes."));
    }

    // ================================================================ Lightworker's Scepter
    static void rechannel(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.heal(p, 10.0f);
        Fx.addEffect(p, "minecraft:regeneration", 100, 1);
        int n = 0;
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8.0), x -> x != p && x.isAlive() && Wave20Util.friend(p, x))) {
            if (t instanceof ServerPlayer sp) Fx.heal(sp, 6.0f);
            else t.heal(6.0f);
            n++;
            if (n >= 8) break;
        }
        FxRelic.ring(p.serverLevel(), p.position(), 8.0, 0xBFEFFF);
        Fx.say(p, "A gentle voice: \"Breathe. Mercy is also strength.\" You are mended" + (n > 0 ? ", and so are " + n + " near you." : "."));
    }

    static void soothingPresence(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(K_AURA, p.level().getGameTime() + Wave20Rules.AURA_TICKS);
        Wave20Events.pulseAura(p);
        FxRelic.ring(p.serverLevel(), p.position(), 6.0, 0xDFF6FF);
        Fx.say(p, "Your aura turns radiant: for a minute everyone within six blocks is soothed and feels safe.");
    }

    static void etherealLight(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave20Util.lookTarget(p, 24.0);
        Vec3 centre = t != null ? t.position() : p.getEyePosition().add(p.getLookAngle().scale(Math.min(12.0, Wave20Util.lineLength(p, 12.0))));
        int n = 0;
        for (LivingEntity v : Wave20Util.foesNear(p, centre, 10.0)) {
            boolean undead = v.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD);
            Fx.damage(p, v, undead ? 21.0f : 14.0f, "minecraft:magic");
            com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_20, v, FxTone.LIGHT, 8);
            n++;
        }
        FxRelic.ring(p.serverLevel(), centre, 10.0, 0xEAF8FF);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.4f);
        Fx.say(p, "Mighty Ethereal Light falls in a ten-block ring" + (n > 0 ? ": " + n + (n == 1 ? " foe is smitten." : " foes are smitten.") : ", and finds no one."));
    }

    // ================================================================ Mjolnir
    static void hammerThrow(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double len = Wave20Util.lineLength(p, Wave20Rules.THROW_RANGE);
        int n = 0;
        for (LivingEntity t : Wave20Util.foesOnLine(p, Wave20Rules.THROW_RANGE, 1.0, 3)) {
            Fx.damage(p, t, 8.0f, "minecraft:player_attack");
            t.knockback(0.8, -look.x, -look.z);
            n++;
        }
        Vec3 end = eye.add(look.scale(len));
        com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_20, eye.add(0, -0.3, 0), end, FxTone.LIGHT, 6);
        com.newuniverse.nusmp.particle.fx.Fx.burst(level, FxThemes.RELIC_WAVE_20, end, FxTone.LIGHT, 8);
        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 1.4f);
        Fx.say(p, n > 0 ? "Mjolnir flies, strikes " + n + (n == 1 ? " foe" : " foes") + " and is back in your hand." : "Mjolnir flies, finds nothing and is back in your hand.");
    }

    static void lightningBolt(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double len = Wave20Util.lineLength(p, Wave20Rules.THROW_RANGE);
        float dmg = Wave20Rules.boltDamage(12.0f, level.isThundering());
        int n = 0;
        for (LivingEntity t : Wave20Util.foesOnLine(p, Wave20Rules.THROW_RANGE, 1.6, 8)) {
            Fx.damage(p, t, dmg, "minecraft:lightning_bolt");
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
            n++;
        }
        Vec3 end = eye.add(look.scale(len));
        com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_20, eye.add(0, -0.3, 0), end, FxTone.LIGHT, 6);
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_20, FxTone.LIGHT, end.x, end.y + 1.0, end.z, 12, 0.3, 1.2, 0.3, 0.3);
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.2f);
        Fx.say(p, n > 0 ? "Lightning Bolt: the sky answers and " + n + (n == 1 ? " foe is" : " foes are") + " struck." : "Lightning Bolt: the sky answers, and the bolt hits only air.");
    }

    // ================================================================ Overprotective Halberd
    static void hover(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag pd = p.getPersistentData();
        long now = p.level().getGameTime();
        if (pd.getLong(K_HOVER) > now) {
            pd.remove(K_HOVER);
            Fx.say(p, "The halberd settles back into your hand.");
            return;
        }
        pd.putLong(K_HOVER, now + Wave20Rules.HOVER_TICKS);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_20, p, FxTone.LIGHT, 8);
        Fx.say(p, "The halberd rises and hovers round you for three minutes: it strikes back at whoever hits you in melee, once every two seconds, even when it is not in your hand.");
    }

    // ================================================================ Phoenix Flame
    static void commandWord(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag pd = p.getPersistentData();
        if (pd.getBoolean(K_LIT)) {
            pd.putBoolean(K_LIT, false);
            Fx.say(p, "The phoenix's flames die down.");
            return;
        }
        pd.putBoolean(K_LIT, true);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_20, p.position().add(0, 1.0, 0), FxTone.LIGHT, 12);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.8f);
        Fx.say(p, "\"Rise.\" Flames erupt along the blade and light the way. They burn until you speak again, drop the halberd or stow it.");
    }

    // ================================================================ Primal Chaos
    static void voidRend(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        int n = 0, voids = 0;
        for (LivingEntity t : Wave20Util.foesOnLine(p, 6.0, 1.0, 5)) {
            boolean v = Wave20Util.isVoid(t);
            Fx.damage(p, t, v ? 11.0f : 5.0f, "minecraft:magic");
            if (v) {
                Fx.addEffect(t, "tensura:spatial_blockade", 60, 0);
                voids++;
            }
            n++;
        }
        Fx.dash(p, 5.0);
        com.newuniverse.nusmp.particle.fx.Fx.stream(level, FxThemes.RELIC_WAVE_20, p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(5.0)), FxTone.LIGHT, 6);
        Fx.say(p, n > 0 ? "Void Rend: the rapier lunges through " + n + (n == 1 ? " foe" : " foes") + (voids > 0 ? ", and the void creatures among them cannot slip away." : ".") : "Void Rend: you lunge through empty air.");
    }

    // ================================================================ Raven's Wing
    static void tollTheDead(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave20Util.lookTarget(p, 20.0);
        if (t == null) {
            Fx.say(p, "The bell has no one to toll for.");
            return;
        }
        boolean saved = Wave20Rules.saveHolds(p.getRandom().nextDouble()) || Wave20Util.isBig(t);
        Fx.damage(p, t, Wave20Rules.tollDamage(t.getHealth(), t.getMaxHealth(), saved), "minecraft:magic");
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_20, FxTone.DARK, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 12, 0.4, 0.6, 0.4, 0.05);
        p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.2f, 0.6f);
        Fx.say(p, saved ? "Toll the Dead: the creature holds its ground and takes half." : "Toll the Dead: the bell rings in its bones.");
    }

    static void spareTheDying(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity worst = null;
        float worstFrac = 1.1f;
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10.0), x -> x.isAlive() && Wave20Util.friend(p, x))) {
            float frac = t.getHealth() / Math.max(1.0f, t.getMaxHealth());
            if (Wave20Rules.dying(t.getHealth(), t.getMaxHealth()) && frac < worstFrac) {
                worstFrac = frac;
                worst = t;
            }
        }
        if (worst == null) {
            Fx.say(p, "No one near is dying.");
            return;
        }
        if (worst instanceof ServerPlayer sp) Fx.heal(sp, 6.0f);
        else worst.heal(6.0f);
        Fx.addEffect(worst, "minecraft:regeneration", 100, 1);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_20, worst, FxTone.LIGHT, 8);
        Fx.say(p, "Spare the Dying: the raven's wing brushes " + (worst == p ? "you" : "an ally") + " and the bleeding stops.");
    }

    static void ravenSense(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.lendSkills(p, 3600, "tensura:magic_sense", "tensura:danger_sense");
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 30.0)) {
            Fx.addEffect(t, "minecraft:glowing", 200, 0);
            n++;
        }
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_20, p.position().add(0, 2.0, 0), FxTone.DARK, 10);
        Fx.say(p, "Ravens wheel overhead and tell you what they see: " + n + " creature" + (n == 1 ? "" : "s") + " outlined within thirty blocks for ten seconds, and your senses stay sharp for three minutes.");
    }
}
