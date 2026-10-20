package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicComponents2;
import com.newuniverse.nusmp.relics.UseRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 18: the abilities of the charms that are not bags: the Old Master's study, the Lover's Divide, the Coin of Fortune, the Defender's Dial, the
 * Elephant of the Room, the Bag of Kittens, the Mirror of Spell Reflection and the Dragon Satchel. Costs, cooldowns and per-day counters are settled by
 * the core before a body runs; everything here is server side and bounded (at most 24 foes per sweep, a handful of queued jobs per use).
 */
final class Wave18Acts {
    private Wave18Acts() {}

    // ---------------------------------------------------------------- Old Master's Spellbook
    static void study(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.addEffect(p, "tensura:inspiration", Wave18Tune.STUDY_TICKS, 0);
        Fx.addEffect(p, "tensura:magic_aura", Wave18Tune.STUDY_TICKS, 0);
        Fx.addEffect(p, "minecraft:haste", Wave18Tune.STUDY_TICKS, 0);
        Fx.lendSkill(p, "tensura:magic_sense", Wave18Tune.STUDY_TICKS);
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_18, FxTone.LIGHT, p.getX(), p.getY() + 1.6, p.getZ(), 30, 0.6, 0.4, 0.6, 0.8);
        Fx.say(p, "You find the shortcut in the old margins: learning and casting come easier for five minutes.");
    }

    // ---------------------------------------------------------------- The Lover's Divide
    static void link(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        AABB box = p.getBoundingBox().inflate(Wave18Tune.LINK_REACH);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive())) {
            boolean friend = (e instanceof ServerPlayer o && !o.isSpectator() && o.isShiftKeyDown()) || (e instanceof TamableAnimal t && t.isOwnedBy(p));
            double d = e.distanceToSqr(p);
            if (friend && d < bestD && d <= Wave18Tune.LINK_REACH * Wave18Tune.LINK_REACH) {
                best = e;
                bestD = d;
            }
        }
        if (best == null) {
            Fx.say(p, "No one answers: the bangle needs a friend within eight blocks who is sneaking to say yes, or a pet of yours.");
            return;
        }
        Wave18Events.link(p, best, level.getGameTime());
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, Wave18Kit.align(0xE8A23A), best.getX(), best.getY() + best.getBbHeight() * 0.6, best.getZ(), 18, 0.4, 0.4, 0.4, 0.0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, Wave18Kit.align(0xE8A23A), p.getX(), p.getY() + 1.0, p.getZ(), 18, 0.4, 0.4, 0.4, 0.0);
        Fx.say(p, "The bangles close: you and " + best.getName().getString() + " share every blow for five minutes.");
        if (best instanceof ServerPlayer o) Fx.say(o, p.getName().getString() + " has joined your hurts to theirs for five minutes.");
    }

    static void sever(ActCtx c) {
        ServerPlayer p = c.player();
        if (Wave18Events.linked(p.getUUID())) {
            Wave18Events.unlink(p.getUUID());
            Fx.say(p, "The chains between the bangles fall slack.");
        } else {
            Fx.say(p, "You are not linked to anyone.");
        }
    }

    // ---------------------------------------------------------------- Coin of Fortune
    static void flip(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        int d20 = p.getRandom().nextInt(20) + 1;
        boolean gold = Wave18Pure.goldFace(d20);
        Wave18Data.putInt(p, Wave18Data.FORTUNE, gold ? 1 : -1);
        Wave18Data.putLong(p, Wave18Data.FORTUNE_UNTIL, now + UseRules.DAY);
        Fx.addEffect(p, gold ? "minecraft:luck" : "minecraft:unluck", Wave18Tune.COIN_LUCK_TICKS, 1);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, gold ? 1.6f : 0.6f);
        Fx.say(p, gold ? "The gold face lands up (" + d20 + "): fortune smiles. Your next blow dealt or taken is blessed, and luck is with you." : "The dark face lands up (" + d20 + "): misfortune. Your next blow dealt or taken is cursed, and luck leaves you.");
    }

    // ---------------------------------------------------------------- Defender's Dial
    static void dial(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = c.stack();
        int idx = Wave18Pure.dialType(stack.getOrDefault(RelicComponents2.STATE.get(), 0));
        Wave18Data.putInt(p, Wave18Data.DIAL_TYPE, idx);
        Wave18Data.putLong(p, Wave18Data.DIAL_UNTIL, p.level().getGameTime() + Wave18Tune.DIAL_TICKS);
        stack.set(RelicComponents2.STATE.get(), Wave18Pure.dialType(idx + 1));
        if (idx == 0) p.clearFire();
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.0f, 1.4f);
        int[] colours = {0xFF7A2E, 0x8FD8FF, 0xFFE95A};
        FxRelic.burst(p.serverLevel(), p.position(), colours[idx]);
        Fx.say(p, "The dial clicks to " + Wave18Pure.dialName(idx) + ": that damage is cut by 60% for five minutes. Next: " + Wave18Pure.dialName(idx + 1) + ".");
    }

    // ---------------------------------------------------------------- Mirror of Spell Reflection
    static void raiseMirror(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        Wave18Data.putLong(p, Wave18Data.MIRROR_UNTIL, now + Wave18Tune.MIRROR_WINDOW);
        Fx.addEffect(p, "tensura:magic_barrier", Wave18Tune.MIRROR_WINDOW, 0);
        FxRelic.ring(p.serverLevel(), p.position(), 1.4, 0xC9D6F2);
        Fx.say(p, "You raise the mirror: the next spell to strike you in five seconds is thrown back at its caster.");
    }

    // ---------------------------------------------------------------- Elephant of the Room
    /** The size of "the room": the average free distance to a wall in eight directions, between 5 and 12 blocks. */
    static double roomRadius(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.getEyePosition();
        double sum = 0;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            Vec3 to = from.add(Math.cos(a) * Wave18Tune.ELEPHANT_MAX_R, 0, Math.sin(a) * Wave18Tune.ELEPHANT_MAX_R);
            var hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            sum += hit.getType() == HitResult.Type.MISS ? Wave18Tune.ELEPHANT_MAX_R : Math.sqrt(hit.getLocation().distanceToSqr(from));
        }
        return Math.max(Wave18Tune.ELEPHANT_MIN_R, Math.min(Wave18Tune.ELEPHANT_MAX_R, sum / 8));
    }

    static void elephant(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = c.stack();
        if (stack.getOrDefault(RelicComponents2.STATE.get(), 0) >= 1) {
            Fx.say(p, "The little elephant has been used once already. An elephant never forgets.");
            return;
        }
        stack.set(RelicComponents2.STATE.get(), 1);
        ServerLevel level = p.serverLevel();
        double r = roomRadius(p);
        Vec3 centre = p.position();
        Fx.say(p, "You say your deepest secret out loud, and the elephant swells to fill the room!");
        Fx.shield(p, 8f, Wave18Tune.ELEPHANT_TICKS);
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.6f, 0.5f);
        FxRelic.ring(level, centre, Math.min(r, 8.0), 0x555560);
        elephantPulse(level, p.getUUID(), centre, r, level.getGameTime(), level.getGameTime() + Wave18Tune.ELEPHANT_TICKS, true);
    }

    private static void elephantPulse(ServerLevel level, UUID pid, Vec3 centre, double r, long now, long end, boolean first) {
        ServerPlayer owner = Wave18Kit.player(level, pid);
        if (owner == null) return;
        if (now >= end) {
            Fx.say(owner, "The elephant deflates with a long sigh.");
            return;
        }
        AABB box = new AABB(centre, centre).inflate(r, 3.0, r);
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> Wave6Kit.isFoe(owner, x))) {
            if (n++ >= 24) break;
            Vec3 away = e.position().subtract(centre).multiply(1, 0, 1);
            if (away.lengthSqr() > r * r) continue;
            Vec3 dir = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            if (first) {
                e.setDeltaMovement(dir.x * 1.1, 0.35, dir.z * 1.1);                          // pushed aside as the elephant swells
                e.hurtMarked = true;
            }
            boolean pinned = e.horizontalCollision;
            boolean player = e instanceof net.minecraft.world.entity.player.Player;
            Fx.addEffect(e, "minecraft:slowness", 40, pinned ? (player ? 3 : 6) : 2);          // squeezing past it is difficult terrain
            if (pinned && !player) {
                Fx.addEffect(e, "minecraft:weakness", 40, 1);
                Fx.addEffect(e, "tensura:movement_interference", 40, 0);
            }
        }
        for (int i = 0; i < 24; i++) {
            double a = i * (Math.PI * 2 / 24);
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, FxTone.LIGHT, centre.x + Math.cos(a) * r, centre.y + 0.5, centre.z + Math.sin(a) * r, 1, 0.1, 0.4, 0.1, 0.01);
        }
        long next = now + Wave18Tune.ELEPHANT_PULSE;
        Wave18Events.QUEUE.at(next, () -> elephantPulse(level, pid, centre, r, next, end, false));
    }

    // ---------------------------------------------------------------- Bag of Kittens
    static int kittens(ItemStack stack) { return Wave18Pure.kittensIn(stack.get(RelicComponents2.STATE.get())); }

    static void pickKitten(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = c.stack();
        ServerLevel level = p.serverLevel();
        int n = kittens(stack);
        if (n <= 0) {
            Fx.say(p, "The bag is out of kittens. Sneak and use it near a kitten of yours to put one back.");
            return;
        }
        int owned = level.getEntitiesOfClass(Cat.class, p.getBoundingBox().inflate(128.0), cat -> cat.isOwnedBy(p)).size();   // every loaded cat of theirs, so leaving them behind does not raise the cap
        if (owned >= Wave18Tune.KITTEN_CAP_OWNED) {
            Fx.say(p, "You already have " + owned + " cats in your care: put one back first.");
            return;
        }
        Cat cat = EntityType.CAT.create(level);
        if (cat == null) return;
        Vec3 look = p.getLookAngle();
        cat.moveTo(p.getX() + look.x * 1.2, p.getY(), p.getZ() + look.z * 1.2, p.getYRot() + 180f, 0f);
        cat.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        cat.setBaby(true);
        cat.tame(p);
        cat.setPersistenceRequired();
        if (!level.addFreshEntity(cat)) return;
        stack.set(RelicComponents2.STATE.get(), n - 1);
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, FxTone.LIGHT, cat.getX(), cat.getY() + 0.5, cat.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, cat.blockPosition(), SoundEvents.CAT_AMBIENT, SoundSource.NEUTRAL, 1.0f, 1.6f);
        Fx.say(p, "You pick a kitten out of the bag (" + (n - 1) + " left inside).");
    }

    static void placeKitten(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = c.stack();
        ServerLevel level = p.serverLevel();
        int n = kittens(stack);
        if (n >= Wave18Tune.KITTENS_START) {
            Fx.say(p, "The kitten paradise is full.");
            return;
        }
        Cat best = null;
        double bd = Double.MAX_VALUE;
        for (Cat cat : level.getEntitiesOfClass(Cat.class, p.getBoundingBox().inflate(4.0), x -> x.isOwnedBy(p) && x.isBaby())) {
            double d = cat.distanceToSqr(p);
            if (d < bd) { best = cat; bd = d; }
        }
        if (best == null) {
            Fx.say(p, "No kitten of yours is close enough to pop into the bag.");
            return;
        }
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, FxTone.LIGHT, best.getX(), best.getY() + 0.5, best.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        best.discard();
        stack.set(RelicComponents2.STATE.get(), n + 1);
        Fx.say(p, "A kitten tumbles into the bag (" + (n + 1) + " inside).");
    }

    static void flood(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        int n = Math.max(1, kittens(c.stack()));
        int pulses = Wave18Pure.floodPulses(level.getRandom()::nextInt);
        Vec3 centre = p.position();
        for (int i = 0; i < 3; i++) {
            LivingEntity k = Fx.summon(p, "minecraft:cat", 20 * (pulses + 1));
            if (k instanceof Cat cat) cat.setBaby(true);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 2.0f, 1.8f);
        Fx.say(p, "You turn the bag inside out: a flood of kittens pours over the ground!");
        floodPulse(level, p.getUUID(), centre, n, 0, pulses);
    }

    private static void floodPulse(ServerLevel level, UUID pid, Vec3 centre, int kittens, int pulse, int pulses) {
        ServerPlayer owner = Wave18Kit.player(level, pid);
        if (owner == null || pulse >= pulses) return;
        double r = Wave18Pure.floodRadius(kittens, pulse);
        AABB box = new AABB(centre, centre).inflate(r, 3.0, r);
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (n++ >= 32) break;
            if (e.position().subtract(centre).multiply(1, 0, 1).lengthSqr() > r * r) continue;
            if (Wave6Kit.isFoe(owner, e)) {
                Fx.addEffect(e, "minecraft:slowness", 40, 1);                                // the ground is difficult terrain
                if (!(e instanceof net.minecraft.world.entity.player.Player)) Fx.addEffect(e, "minecraft:blindness", 30, 0);   // heavily obscured
            } else if (e instanceof net.minecraft.world.entity.player.Player || (e instanceof TamableAnimal t && t.isOwnedBy(owner))) {
                Fx.addEffect(e, "minecraft:regeneration", 40, 0);                            // purring
            }
        }
        for (int i = 0; i < 20; i++) {
            double a = i * (Math.PI * 2 / 20);
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, FxTone.LIGHT, centre.x + Math.cos(a) * r, centre.y + 0.4, centre.z + Math.sin(a) * r, 1, 0.2, 0.3, 0.2, 0.0);
        }
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, Wave18Kit.align(0xE8C48A), centre.x, centre.y + 0.3, centre.z, 24, r * 0.5, 0.2, r * 0.5, 0.0);
        long next = level.getGameTime() + 20;
        Wave18Events.QUEUE.at(next, () -> floodPulse(level, pid, centre, kittens, pulse + 1, pulses));
    }

    // ---------------------------------------------------------------- Dragon Satchel
    private static final String K_KIND = "w18_sat_kind", K_LEFT = "w18_sat_n", K_READY = "w18_sat_ready";

    private static CompoundTag tagOf(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    /** Fills the satchel with two handfuls from the pack: ash, sand, grass or snow. They turn to powder over a day. */
    static void fill(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack sat = c.stack();
        CompoundTag t = tagOf(sat);
        if (t.getInt(K_KIND) != 0 && t.getInt(K_LEFT) > 0) {
            Fx.say(p, "The satchel still holds " + Wave18Pure.powderName(t.getInt(K_KIND)) + " powder: throw it first.");
            return;
        }
        Inventory inv = p.getInventory();
        int kind = 0;
        for (int i = 0; i < inv.items.size() && kind == 0; i++) kind = Wave18Pure.powderOf(Wave18Pockets.id(inv.items.get(i).getItem()));
        if (kind == 0) {
            Fx.say(p, "Carry some ash (charcoal or coal), sand, grass or snow to fill it with.");
            return;
        }
        int have = 0;
        for (ItemStack s : inv.items) if (Wave18Pure.powderOf(Wave18Pockets.id(s.getItem())) == kind) have += s.getCount();
        if (have < 2) {
            Fx.say(p, "Two handfuls are needed.");
            return;
        }
        int need = 2;
        for (ItemStack s : inv.items) {
            if (need > 0 && Wave18Pure.powderOf(Wave18Pockets.id(s.getItem())) == kind) {
                int take = Math.min(need, s.getCount());
                s.shrink(take);
                need -= take;
            }
        }
        long ready = p.level().getGameTime() + Wave18Tune.SATCHEL_FILL_TICKS;
        final int k = kind;
        CustomData.update(DataComponents.CUSTOM_DATA, sat, tag -> {
            tag.putInt(K_KIND, k);
            tag.putInt(K_LEFT, Wave18Tune.SATCHEL_THROWS);
            tag.putLong(K_READY, ready);
        });
        Fx.say(p, "You fill the satchel with " + Wave18Pure.powderName(kind) + ". It becomes fine crystal powder in a day.");
    }

    /** Throws a handful of the powder: a cone of damage of the powder's element, 3d4, half for a foe that dodges. Twice empties the satchel. */
    static void breathe(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack sat = c.stack();
        ServerLevel level = p.serverLevel();
        CompoundTag t = tagOf(sat);
        int kind = t.getInt(K_KIND), left = t.getInt(K_LEFT);
        if (kind == 0 || left <= 0) {
            Fx.say(p, "The satchel is empty: sneak and use it to fill it.");
            return;
        }
        long now = level.getGameTime();
        if (now < t.getLong(K_READY)) {
            Fx.say(p, "The " + Wave18Pure.powderName(kind) + " is still ripening into powder (" + UseRules.span((int) Math.min(Integer.MAX_VALUE, t.getLong(K_READY) - now)) + ").");
            return;
        }
        Vec3 f = Wave18Kit.flatLook(p);
        for (LivingEntity e : Fx.foesAround(p, Wave18Tune.SATCHEL_RANGE)) {
            Vec3 d = e.position().subtract(p.position());
            if (Math.abs(d.y) > 2.5 || !Wave18Pure.inCone(d.x, d.z, f.x, f.z, Wave18Tune.SATCHEL_RANGE, Wave18Tune.SATCHEL_COS)) continue;
            float dmg = Wave18Pure.dice(3, 4, level.getRandom()::nextInt);
            if (level.getRandom().nextDouble() < Wave18Tune.SATCHEL_SAVE) dmg /= 2f;
            Fx.damage(p, e, dmg, Wave18Pure.powderDamageType(kind));
            switch (kind) {
                case 1 -> e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 80));
                case 2 -> Fx.addEffect(e, "minecraft:slowness", 30, 1);
                case 3 -> Fx.addEffect(e, "minecraft:poison", 60, 0);
                case 4 -> {
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 40));
                    Fx.addEffect(e, "minecraft:slowness", 40, 1);
                }
                default -> { }
            }
        }
        var dust = Wave18Kit.align(Wave18Pure.powderColour(kind));
        for (int i = 1; i <= 8; i++) {
            double reach = i * Wave18Tune.SATCHEL_RANGE / 8.0, spread = reach * 0.4;
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_18, dust, p.getX() + f.x * reach, p.getEyeY() - 0.3, p.getZ() + f.z * reach, 6, spread, 0.3, spread, 0.0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.6f);
        int after = left - 1;
        CustomData.update(DataComponents.CUSTOM_DATA, sat, tag -> {
            tag.putInt(K_LEFT, after);
            if (after <= 0) tag.putInt(K_KIND, 0);
        });
        if (after <= 0) Fx.say(p, "That was the last handful: the satchel is empty.");
    }
}
