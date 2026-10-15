package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the active abilities of the wave 25 relics: eating rocks, passing the Game, the Mimic's Guidance and bite, the Rose Petal's bloom and three spells,
 * the Lyre's laughter and dissonance, the spoon's sip, the lantern's shining, the Forgotten Ring's vanishing, the Witch's beast forms and the Corpse
 * Mask's faces. Costs, cooldowns and per-day counters are settled by the core before a body runs; everything here is server side and bounded (at most
 * 24 foes per sweep, eight rocks per meal).
 */
final class Wave25Acts {
    private Wave25Acts() {}

    // ---------------------------------------------------------------- Ring of Rock Eating
    static void eatRocks(ActCtx c) {
        ServerPlayer p = c.player();
        int eaten = eatRocks(p, Wave25Tune.ROCK_EAT_MAX);
        if (eaten > 0) Fx.say(p, "You crunch down " + eaten + (eaten == 1 ? " rock" : " rocks") + " and feel fed.");
        else if (p.getFoodData().getFoodLevel() >= 20) Fx.say(p, "You are not hungry: the rocks can wait.");
        else Fx.say(p, "You have no stone, gravel or flint to eat.");
    }

    /** Eats up to {@code maxItems} rocks from the inventory for the hunger the player has; returns how many items were eaten. */
    static int eatRocks(ServerPlayer p, int maxItems) {
        int missing = 20 - p.getFoodData().getFoodLevel();
        if (missing <= 0) return 0;
        Inventory inv = p.getInventory();
        double fed = 0;
        int eaten = 0;
        for (int i = 0; i < inv.items.size() && eaten < maxItems && fed < missing; i++) {
            ItemStack s = inv.items.get(i);
            double per = Wave25Pure.rockNutrition(Wave25Kit.itemId(s));
            int n = Wave25Pure.itemsToEat(per, s.getCount(), maxItems - eaten, missing - fed);
            if (n <= 0) continue;
            s.shrink(n);
            fed += per * n;
            eaten += n;
        }
        if (eaten > 0) {
            int gain = (int) Math.max(1, Math.round(fed));
            p.getFoodData().setFoodLevel(Math.min(20, p.getFoodData().getFoodLevel() + gain));
            p.getFoodData().setSaturation(Math.min(p.getFoodData().getFoodLevel(), p.getFoodData().getSaturationLevel() + gain * Wave25Tune.ROCK_SATURATION_PER_FOOD));
            RukhFx.burst(p.serverLevel(), p, Wave25Kit.light(), 4);
        }
        return eaten;
    }

    // ---------------------------------------------------------------- Ring of the Game
    static void passTheGame(ActCtx c) {
        ServerPlayer p = c.player();
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave25Tune.PASS_RANGE)) {
            Fx.damage(p, t, Wave25Tune.PASS_DAMAGE + p.getRandom().nextInt(4), "minecraft:magic");
            Fx.addEffect(t, "tensura:confusion", Wave25Tune.PASS_CONFUSE, 0);
            Fx.addEffect(t, "minecraft:nausea", Wave25Tune.PASS_CONFUSE, 0);
            RukhFx.burst(p.serverLevel(), t, Wave25Kit.dark(), 3);
            n++;
        }
        Fx.say(p, n == 0 ? "You say it aloud, but nobody near you has anything to lose." : n + (n == 1 ? " foe has" : " foes have") + " just lost The Game.");
    }

    // ---------------------------------------------------------------- Ring of the Mimic
    static void guidance(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.addEffect(p, "minecraft:luck", Wave25Tune.GUIDANCE_TICKS, 0);
        Fx.addEffect(p, "tensura:inspiration", Wave25Tune.GUIDANCE_TICKS / 2, 0);
        RukhFx.burst(p.serverLevel(), p, Wave25Kit.light(), 5);
        int face = p.getRandom().nextInt(Wave25Tune.GUIDANCE_FUMBLE_SIDES);
        if (!Wave25Pure.fumble(face)) {
            Fx.say(p, "Guidance: the ring licks your finger and the next minute goes your way.");
            return;
        }
        // the curse: a critical failure, the ring spins and devours the finger
        float bite = Wave25Pure.selfHurt(Wave25Pure.dice(1, 12, p.getRandom()::nextInt), p.getHealth(), p.getMaxHealth(), Wave25Tune.GAME_CAP);
        if (bite > 0f) p.hurt(p.damageSources().generic(), bite);
        Attune.release(p, c.stack());
        Wave25Data.putLong(p, Wave25Data.FINGER_UNTIL, p.level().getGameTime() + Wave25Tune.FINGER_TICKS);
        RukhFx.burst(p.serverLevel(), p, Wave25Kit.dark(), 8);
        Fx.say(p, "The ring begins to spin and devours your finger! Its bond is broken. You are slower and clumsier until magic mends it (a golden apple does) or a while passes. Attune it again to a new finger.");
    }

    // ---------------------------------------------------------------- Rose Petal
    private static boolean bloomed(ServerPlayer p) { return RelicEngine.isOn(p, Wave25Tune.ID_ROSE, "bloom"); }

    static void bloom(ActCtx c) {
        ServerPlayer p = c.player();
        boolean on = c.isOn("bloom");
        RukhFx.burst(p.serverLevel(), p, Wave25Kit.light(), 6);
        Fx.say(p, on ? "The rose unfolds into a thorned halberd: +2 damage, longer reach, and thorns for those who strike you." : "The halberd folds back into a rose.");
    }

    /** Spends {@code cost} charges of the Rose (dawn refill applied); false, with a line, if it is not in halberd form or cannot pay. */
    private static boolean spendRose(ActCtx c, int cost) {
        ServerPlayer p = c.player();
        if (!bloomed(p)) {
            Fx.say(p, "The rose must be a halberd to use its spells: bloom it first.");
            return false;
        }
        ItemStack s = c.stack();
        Wave6Pool.State st = Wave6Kit.pool(s, "rose", Wave25Tune.ROSE_MAX, Wave6Kit.today(p.serverLevel()), () -> Wave25Pure.dawnGain(p.getRandom()::nextInt));
        if (!Wave6Pool.canSpend(st, cost)) {
            Fx.say(p, "The rose has " + st.charges() + " charges; that spell needs " + cost + ". It gains 2d6+2 at dawn.");
            return false;
        }
        Wave6Pool.State after = Wave6Pool.spend(st, cost);
        Wave6Kit.put(s, "rose", after);
        Wave25Kit.hint(p, "Rose Petal: " + after.charges() + " of " + Wave25Tune.ROSE_MAX + " charges left");
        return true;
    }

    static void ensnare(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendRose(c, Wave25Tune.ENSNARE_COST)) return;
        Wave25Data.putLong(p, Wave25Data.ENSNARE_UNTIL, p.level().getGameTime() + Wave25Tune.ENSNARE_WINDOW);
        RukhFx.gather(p.serverLevel(), p, Wave25Kit.light(), 8);
        Fx.say(p, "Ensnaring Strike: your next blow within half a minute lashes the target with thorned vines.");
    }

    static void grasp(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendRose(c, Wave25Tune.GRASP_COST)) return;
        ServerLevel level = p.serverLevel();
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave25Tune.GRASP_RANGE)) {
            Fx.damage(p, t, Wave25Tune.GRASP_DAMAGE, "minecraft:thorns");
            Fx.addEffect(t, "minecraft:slowness", Wave25Tune.GRASP_ROOT, 4);
            Vec3 pull = p.position().subtract(t.position());
            if (pull.lengthSqr() > 4.0) {
                Vec3 d = pull.normalize().scale(0.5);
                t.setDeltaMovement(d.x, 0.1, d.z);
                t.hurtMarked = true;
            }
            RukhFx.burst(level, t, Wave25Kit.light(), 4);
            n++;
        }
        Fx.say(p, "Grasping Vine: thorned vines tear out of the ground" + (n == 0 ? " but find no one." : " and drag " + n + " foe(s) in, rooted."));
    }

    static void guardian(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spendRose(c, Wave25Tune.GUARDIAN_COST)) return;
        if (c.sneaking()) {
            Fx.shield(p, Wave25Tune.GUARDIAN_SHIELD, Wave25Tune.GUARDIAN_TICKS);
            Wave6Kit.give(p, "minecraft:resistance", Wave25Tune.GUARDIAN_TICKS, 0, true);
            Wave6Kit.give(p, "minecraft:regeneration", Wave25Tune.GUARDIAN_TICKS, 0, true);
            Fx.say(p, "Guardian of Nature, Great Tree: bark covers you for a minute: temporary health, resistance and mending.");
        } else {
            Wave6Kit.give(p, "minecraft:strength", Wave25Tune.GUARDIAN_TICKS, 0, true);
            Wave6Kit.give(p, "minecraft:speed", Wave25Tune.GUARDIAN_TICKS, 0, true);
            Wave6Kit.give(p, "minecraft:night_vision", Wave25Tune.GUARDIAN_TICKS, 0, true);
            Wave6Kit.give(p, "minecraft:jump_boost", Wave25Tune.GUARDIAN_TICKS, 1, true);
            Fx.say(p, "Guardian of Nature, Primal Beast: for a minute you are strong, quick and keen-eyed. (Sneak while casting for the Great Tree.)");
        }
        RukhFx.gather(p.serverLevel(), p, Wave25Kit.light(), 10);
    }

    // ---------------------------------------------------------------- Silver Tongued Lyre
    static void laughter(ActCtx c) {
        ServerPlayer p = c.player();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestCos = Wave25Tune.LAUGH_COS;
        for (LivingEntity t : Fx.foesAround(p, Wave25Tune.LAUGH_RANGE)) {
            Vec3 to = t.getEyePosition().subtract(p.getEyePosition());
            double len = to.length();
            if (len < 0.5) continue;
            double cos = to.dot(look) / len;
            if (cos > bestCos) { best = t; bestCos = cos; }
        }
        if (best == null) {
            Fx.say(p, "Nothing in front of you is within earshot of the joke (eighteen blocks).");
            return;
        }
        boolean saved = p.getRandom().nextDouble() < Wave25Tune.LYRE_SAVE;
        int ticks = saved ? Wave25Tune.LAUGH_TICKS / 2 : Wave25Tune.LAUGH_TICKS;
        Fx.addEffect(best, "minecraft:slowness", ticks, 5);
        Fx.addEffect(best, "minecraft:weakness", ticks, 2);
        Fx.addEffect(best, "minecraft:nausea", ticks, 0);
        Fx.addEffect(best, "tensura:confusion", ticks, 0);
        RukhFx.burst(p.serverLevel(), best, Wave25Kit.dark(), 6);
        Fx.say(p, "Hideous Laughter: " + best.getName().getString() + (saved ? " chuckles and shakes it off in part." : " collapses in helpless laughter."));
    }

    static void dissonance(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        int hit = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave25Tune.LYRE_RANGE)) {
            String id = Wave25Kit.typeId(t);
            if (!Wave25Pure.hearsLyre(id)) continue;
            boolean celestial = Wave25Pure.isCelestialId(id);
            double chance = celestial ? Wave25Pure.withDisadvantage(Wave25Tune.LYRE_SAVE) : Wave25Tune.LYRE_SAVE;
            boolean saved = p.getRandom().nextDouble() < chance;
            float dmg = Wave25Pure.lyreDamage(Wave25Pure.dice(4, 6, p.getRandom()::nextInt), saved);
            Fx.damageHeavy(p, t, dmg, "minecraft:magic");
            Fx.addEffect(t, "minecraft:weakness", Wave25Tune.DISSONANCE_WEAK, saved ? 0 : 1);
            RukhFx.burst(level, t, Wave25Kit.dark(), 3);
            hit++;
        }
        RukhFx.gather(level, p, Wave25Kit.dark(), 10);
        Fx.say(p, hit == 0 ? "You play the dissonant tune, but no one who can hear it (humanoid or celestial) is within eighteen blocks." : "Hellish Dissonance rips through " + hit + " listener(s).");
    }

    // ---------------------------------------------------------------- Soup Spoon of Improbability
    static void sip(ActCtx c) {
        ServerPlayer p = c.player();
        long today = Wave25Kit.today(p.serverLevel());
        ItemStack potion = ItemStack.EMPTY;
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof PotionItem) { potion = s; break; }
        boolean ok = p.getRandom().nextDouble() < Wave25Tune.SPOON_SIP_SUCCESS;
        if (ok) {
            if (!potion.isEmpty()) {
                PotionContents pc = potion.get(DataComponents.POTION_CONTENTS);
                if (pc != null) for (MobEffectInstance inst : pc.getAllEffects()) p.addEffect(new MobEffectInstance(inst));
                if (!p.isCreative()) { potion.shrink(1); ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE); if (!p.getInventory().add(bottle)) p.drop(bottle, false); }
            }
            Wave25Data.putLong(p, Wave25Data.STEADY_UNTIL, p.level().getGameTime() + Wave25Tune.SPOON_STEADY);
            Fx.say(p, "A perfect, steady sip" + (potion.isEmpty() ? " of cool water" : "") + ": for a while nothing throws you off (weakness, slowness, fatigue, blindness and the like fade fast).");
            RukhFx.burst(p.serverLevel(), p, Wave25Kit.light(), 4);
        } else {
            if (!potion.isEmpty() && !p.isCreative()) { potion.shrink(1); ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE); if (!p.getInventory().add(bottle)) p.drop(bottle, false); }
            Wave25Data.putLong(p, Wave25Data.SPOON_DAY, today + 1);
            Fx.say(p, "You spill it down your front. Embarrassed, you lose the spoon's boldness until the next dawn.");
        }
    }

    // ---------------------------------------------------------------- Spirit Lantern
    static void shine(ActCtx c) {
        ServerPlayer p = c.player();
        Wave25Events.shine(p);
    }

    // ---------------------------------------------------------------- The Forgotten Ring
    static void vanish(ActCtx c) {
        ServerPlayer p = c.player();
        if (Wave25Data.getBool(p, Wave25Data.VANISHED)) {
            Wave25Events.unvanish(p, "You let the ring go quiet and step back into view.");
            return;
        }
        Wave25Data.putBool(p, Wave25Data.VANISHED, true);
        Wave6Kit.give(p, "minecraft:invisibility", 60, 0, true);
        RukhFx.burst(p.serverLevel(), p, Wave25Kit.dark(), 8);
        Fx.say(p, "You vanish. You stay hidden until you strike, cast, take the ring off or use it again. Your first blow from hiding hits half again as hard.");
    }

    // ---------------------------------------------------------------- Witch's Ring
    static void shapechange(ActCtx c) {
        ServerPlayer p = c.player();
        if (Wave25Data.getBool(p, Wave25Data.FORM_ON)) {
            Wave25Events.endForm(p, "You shrug off the beast and stand as yourself.");
            return;
        }
        long today = Wave25Kit.today(p.serverLevel());
        if (Wave25Data.getLong(p, Wave25Data.FORM_DAY) != today) {
            Wave25Data.putLong(p, Wave25Data.FORM_DAY, today);
            Wave25Data.putInt(p, Wave25Data.FORM_LEFT, Wave25Tune.FORM_ALLOWANCE);
        }
        if (Wave25Data.getInt(p, Wave25Data.FORM_LEFT) <= 0) {
            Fx.say(p, "Your two hours of beast form for today are spent; they return at dawn.");
            return;
        }
        if (!Wave25Data.has(p, Wave25Data.FORM)) {
            Wave25Data.putInt(p, Wave25Data.FORM, Wave25Pure.rollForm(p.getRandom()::nextInt));
            Fx.say(p, "The ring chooses your beast with a roll of the d8: the " + Wave25Pure.Form.of(Wave25Data.getInt(p, Wave25Data.FORM)).label() + ".");
        }
        Wave25Data.putBool(p, Wave25Data.FORM_ON, true);
        RukhFx.burst(p.serverLevel(), p, Wave25Kit.dark(), 8);
        Fx.say(p, "You become the " + Wave25Pure.Form.of(Wave25Data.getInt(p, Wave25Data.FORM)).label() + " (" + Wave25Data.getInt(p, Wave25Data.FORM_LEFT) / 20 + " seconds of form left today).");
    }

    static void changeForm(ActCtx c) {
        ServerPlayer p = c.player();
        if (Wave25Data.getBool(p, Wave25Data.FORM_ON)) Wave25Events.endForm(p, null);
        int next = Wave25Pure.formAfter(Wave25Data.has(p, Wave25Data.FORM) ? Wave25Data.getInt(p, Wave25Data.FORM) : Wave25Pure.rollForm(p.getRandom()::nextInt));
        Wave25Data.putInt(p, Wave25Data.FORM, next);
        Fx.say(p, "In the night the ring changes its beast: now the " + Wave25Pure.Form.of(next).label() + ".");
    }

    // ---------------------------------------------------------------- Corpse Mask
    static void wearFace(ActCtx c) {
        ServerPlayer p = c.player();
        List<String> faces = Wave25Mask.faces(c.stack());
        if (faces.isEmpty()) {
            Fx.say(p, "The mask holds no face yet: it stores the faces of the humanoids that die within twenty-four blocks while you wear it.");
            return;
        }
        String face = faces.get(Wave25Mask.selected(c.stack()));
        Wave25Data.putLong(p, Wave25Data.MASK_UNTIL, p.level().getGameTime() + Wave25Tune.MASK_TICKS);
        p.getPersistentData().putString("nusmp_w25_face", face);
        Wave25Events.calmKin(p, face);
        RukhFx.burst(p.serverLevel(), p, Wave25Kit.dark(), 8);
        Fx.say(p, "You take the face of " + Wave25Pure.faceName(face) + " for five minutes. Their own kind take you for one of them.");
    }

    static void cycleFace(ActCtx c) {
        ServerPlayer p = c.player();
        List<String> faces = Wave25Mask.faces(c.stack());
        if (faces.isEmpty()) {
            Fx.say(p, "The mask holds no face yet.");
            return;
        }
        int next = Wave25Pure.nextFace(Wave25Mask.selected(c.stack()), faces.size());
        Wave25Mask.select(c.stack(), next);
        Fx.say(p, "Face " + (next + 1) + " of " + faces.size() + ": " + Wave25Pure.faceName(faces.get(next)) + ".");
    }

    /** The entity type behind a stored face (for the mob-faction check); null if the id is gone. */
    static EntityType<?> typeOf(String face) {
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(Wave25Pure.faceType(face));
        return rl == null ? null : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }

    static boolean isMob(LivingEntity e) { return e instanceof Mob; }
}
