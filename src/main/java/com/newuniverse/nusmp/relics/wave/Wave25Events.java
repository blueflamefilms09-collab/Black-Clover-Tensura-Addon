package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Why: the game-bus handlers of the wave 25 relics. One damage listener (the Rose's ensnaring blow and thorns, the Forgotten Ring's ambush, the spider
 * form's bite), one target listener (vanished players and disguised faces are not picked as targets), one death listener (the Corpse Mask's faces and
 * the Spirit Lantern's remains), a use listener (a golden apple mends the Mimic's bitten finger) and a once-a-second step per player that runs every
 * worn relic's upkeep. Every table is bounded and cleared when the server stops.
 */
final class Wave25Events {
    private Wave25Events() {}

    /** The remains of something that died near a lantern-bearer: where, what, when, to whom, carrying what. */
    record Remains(ResourceKey<Level> dimension, Vec3 at, String type, String name, long gameTime, String killer, String carried) {}

    /** A ghost being replayed above remains for a few seconds. */
    record Replay(ResourceKey<Level> dimension, Vec3 at, long until) {}

    private static final Deque<Remains> REMAINS = new ArrayDeque<>();
    private static final List<Replay> REPLAYS = new ArrayList<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wave25Events::onIncoming);
        NeoForge.EVENT_BUS.addListener(Wave25Events::onTarget);
        NeoForge.EVENT_BUS.addListener(Wave25Events::onDeath);
        NeoForge.EVENT_BUS.addListener(Wave25Events::onUseFinish);
        NeoForge.EVENT_BUS.addListener(Wave25Events::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Wave25Events::onServerTick);
        NeoForge.EVENT_BUS.addListener(Wave25Events::onStopping);
    }

    private static void onStopping(ServerStoppingEvent e) {
        REMAINS.clear();
        REPLAYS.clear();
    }

    // ================================================================ damage
    private static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypes.THORNS)) return;
        long now = victim.level().getGameTime();

        // a blow from a player's own hand or weapon
        if (src.getEntity() instanceof ServerPlayer atk && atk != victim && src.getDirectEntity() == atk) {
            // the Forgotten Ring: the first blow from hiding hits harder, and ends the hiding
            if (Wave25Data.getBool(atk, Wave25Data.VANISHED)) {
                e.setAmount((float) (e.getAmount() * Wave25Tune.VANISH_AMBUSH));
                unvanish(atk, "You strike from hiding and are seen.");
            }
            // the Rose Petal's Ensnaring Strike
            long until = Wave25Data.getLong(atk, Wave25Data.ENSNARE_UNTIL);
            if (until > now && RelicFlags.has(atk, Wave25Tune.F_ROSE)) {
                Wave25Data.remove(atk, Wave25Data.ENSNARE_UNTIL);
                e.setAmount(e.getAmount() + Wave25Tune.ENSNARE_BONUS);
                Fx.addEffect(victim, "minecraft:slowness", Wave25Tune.ENSNARE_ROOT, 5);
                RukhFx.burst(atk.serverLevel(), victim, Wave25Kit.light(), 5);
                Wave25Kit.hint(atk, "Thorned vines bind " + victim.getName().getString() + ".");
            } else if (until != 0L && until <= now) {
                Wave25Data.remove(atk, Wave25Data.ENSNARE_UNTIL);
            }
            // the spider form's bite
            if (Wave25Data.getBool(atk, Wave25Data.FORM_ON) && RelicFlags.has(atk, Wave25Tune.F_WITCH)
                    && Wave25Pure.Form.of(Wave25Data.getInt(atk, Wave25Data.FORM)).poisonBite()) {
                Fx.addEffect(victim, "minecraft:poison", 100, 0);
            }
        }
        // the Rose Petal's thorns on whoever strikes a halberd-bearer
        if (victim instanceof ServerPlayer wearer && src.getDirectEntity() instanceof LivingEntity hitter && hitter != wearer
                && RelicFlags.has(wearer, Wave25Tune.F_ROSE) && RelicEngine.isOn(wearer, Wave25Tune.ID_ROSE, "bloom")) {
            hitter.hurt(wearer.damageSources().thorns(wearer), Wave25Tune.THORN_DAMAGE);
        }
    }

    // ================================================================ targeting
    private static void onTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p) || !(e.getEntity() instanceof Mob mob)) return;
        if (Wave25Data.getBool(p, Wave25Data.VANISHED) && mob.distanceTo(p) > Wave25Tune.VANISH_SEEN_RANGE) {
            e.setCanceled(true);
            return;
        }
        if (disguised(p) && kin(mob, p.getPersistentData().getString("nusmp_w25_face"))) e.setCanceled(true);
    }

    static boolean disguised(Player p) {
        return Wave25Data.getLong(p, Wave25Data.MASK_UNTIL) > p.level().getGameTime() && !p.getPersistentData().getString("nusmp_w25_face").isEmpty();
    }

    /** Is this mob of the wearer's stolen face's kind: the same type, or the same faction (the walking dead, the raiders)? */
    static boolean kin(Mob mob, String face) {
        EntityType<?> want = Wave25Acts.typeOf(face);
        if (want == null) return false;
        EntityType<?> t = mob.getType();
        if (t == want) return true;
        if (want.is(EntityTypeTags.UNDEAD) && t.is(EntityTypeTags.UNDEAD)) return true;
        return want.is(EntityTypeTags.RAIDERS) && t.is(EntityTypeTags.RAIDERS);
    }

    /** Makes the mobs near a freshly disguised player who are of the face's kind drop it as a target. */
    static void calmKin(ServerPlayer p, String face) {
        int n = 0;
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0), m -> m.getTarget() == p)) {
            if (n++ >= 24) break;
            if (kin(m, face)) m.setTarget(null);
        }
    }

    // ================================================================ deaths: the mask's faces and the lantern's remains
    private static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) return;
        boolean humanoid = dead instanceof Player || Wave25Pure.isHumanoidId(Wave25Kit.typeId(dead));
        boolean lantern = false;
        long now = level.getGameTime();
        for (Player pl : level.players()) {
            if (!(pl instanceof ServerPlayer w) || !Wave25Kit.live(w) || w == dead) continue;
            double d2 = w.distanceToSqr(dead);
            if (humanoid && d2 <= sq(Wave25Tune.MASK_WITNESS_RANGE) && RelicFlags.has(w, Wave25Tune.F_MASK)) {
                ItemStack mask = Wave6Kit.find(w, Wave25Tune.ID_MASK);
                if (!mask.isEmpty() && Wave25Mask.store(mask, Wave25Kit.typeId(dead) + "|" + dead.getName().getString(), now)) {
                    Wave25Kit.hint(w, "The mask drinks in the face of " + dead.getName().getString() + ".");
                }
            }
            if (d2 <= sq(Wave25Tune.REMAINS_NEAR_PLAYER) && RelicFlags.has(w, Wave25Tune.F_LANTERN)) lantern = true;
        }
        if (lantern) {
            while (REMAINS.size() >= Wave25Tune.REMAINS_MAX) REMAINS.pollFirst();
            LivingEntity by = e.getSource().getEntity() instanceof LivingEntity k ? k : null;
            ItemStack held = dead.getMainHandItem();
            REMAINS.addLast(new Remains(level.dimension(), dead.position(), Wave25Kit.typeId(dead), dead.getName().getString(), now,
                    by == null ? "something unseen" : by.getName().getString(), held.isEmpty() ? "nothing in hand" : held.getHoverName().getString()));
        }
    }

    private static double sq(double v) { return v * v; }

    /** The Spirit Lantern's shining: finds the newest remains within six blocks and starts their ghost. */
    static void shine(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        Remains best = null;
        double bestD = sq(Wave25Tune.SHINE_RANGE);
        for (Remains r : REMAINS) {
            if (!r.dimension().equals(level.dimension()) || now - r.gameTime() > Wave25Tune.REMAINS_AGE) continue;
            double d = r.at().distanceToSqr(p.position());
            if (d <= bestD) { best = r; bestD = d; }
        }
        if (best == null) {
            Fx.say(p, "The cold light finds no fresh remains within six blocks (the lantern remembers the dead of the last day it was near).");
            return;
        }
        if (REPLAYS.size() >= 8) REPLAYS.remove(0);
        REPLAYS.add(new Replay(level.dimension(), best.at(), now + Wave25Tune.REPLAY_TICKS));
        Fx.say(p, "A ghost of " + best.name() + " rises. It died here " + Wave25Pure.ageText(now - best.gameTime()) + " ago, to " + best.killer() + ", with "
                + best.carried() + " in hand. It shows no one else who was there.");
    }

    // ================================================================ use: a golden apple mends the finger
    private static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        String id = Wave25Kit.itemId(e.getItem());
        if ((id.equals("minecraft:golden_apple") || id.equals("minecraft:enchanted_golden_apple")) && Wave25Data.getLong(p, Wave25Data.FINGER_UNTIL) > 0L) {
            Wave25Data.remove(p, Wave25Data.FINGER_UNTIL);
            Wave25Kit.hint(p, "Magic regrows the finger the Mimic ring devoured.");
        }
    }

    // ================================================================ ticks
    private static void onServerTick(ServerTickEvent.Post e) {
        if (REPLAYS.isEmpty() || e.getServer().getTickCount() % 4 != 0) return;
        MinecraftServer server = e.getServer();
        for (int i = REPLAYS.size() - 1; i >= 0; i--) {
            Replay r = REPLAYS.get(i);
            ServerLevel level = server.getLevel(r.dimension());
            if (level == null || level.getGameTime() >= r.until()) { REPLAYS.remove(i); continue; }
            double a = level.getGameTime() * 0.35, h = 0.4 + ((r.until() - level.getGameTime()) % 40) * 0.04;
            RukhFx.spray(level, Wave25Kit.light(), r.at().x + Math.cos(a) * 0.5, r.at().y + h, r.at().z + Math.sin(a) * 0.5, 1, 0, 0.04, 0, 0.0);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        if (!p.isAlive()) return;
        step(p);
    }

    /** The once-a-second upkeep of everything the player wears from this wave (modifiers are switched on or off by what is worn right now). */
    private static void step(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        boolean live = !p.isSpectator();

        boolean rock = live && RelicFlags.has(p, Wave25Tune.F_ROCK);
        if (rock && p.tickCount % Wave25Tune.ROCK_AUTO_PERIOD == 7 && p.getFoodData().getFoodLevel() <= Wave25Tune.ROCK_AUTO_HUNGRY) {
            if (Wave25Acts.eatRocks(p, Wave25Tune.ROCK_AUTO_ITEMS) > 0) Wave25Kit.hint(p, "Your stomach grinds down a rock or two.");
        }

        if (live && RelicFlags.has(p, Wave25Tune.F_GAME)) game(p, now);

        if (live && RelicFlags.has(p, Wave25Tune.F_MIMIC) && p.tickCount % Wave25Tune.MIMIC_SCAN == 7) mimics(p, now);
        long finger = Wave25Data.getLong(p, Wave25Data.FINGER_UNTIL);
        boolean bitten = finger > now;
        if (finger != 0L && !bitten) Wave25Data.remove(p, Wave25Data.FINGER_UNTIL);
        Wave25Kit.setMod(p, "minecraft:generic.attack_speed", "finger_speed", Wave25Tune.FINGER_SPEED, Operation.ADD_MULTIPLIED_TOTAL, bitten);
        Wave25Kit.setMod(p, "minecraft:player.block_break_speed", "finger_break", Wave25Tune.FINGER_BREAK, Operation.ADD_MULTIPLIED_TOTAL, bitten);

        boolean halberd = live && RelicFlags.has(p, Wave25Tune.F_ROSE) && RelicEngine.isOn(p, Wave25Tune.ID_ROSE, "bloom");
        Wave25Kit.setMod(p, "minecraft:generic.attack_damage", "halberd_attack", Wave25Tune.HALBERD_ATTACK, Operation.ADD_VALUE, halberd);
        Wave25Kit.setMod(p, "minecraft:player.entity_interaction_range", "halberd_reach", Wave25Tune.HALBERD_REACH, Operation.ADD_VALUE, halberd);

        spoon(p, now, live);
        if (live && RelicFlags.has(p, Wave25Tune.F_LANTERN) && p.tickCount % Wave25Tune.LANTERN_SCAN == 7) lantern(p, level);
        forgotten(p, live);
        witch(p, live);
        if (live && RelicFlags.has(p, Wave25Tune.F_BUCKET)) bucket(p, level, now);
        mask(p, now);
        dkh(p, level, live);
    }

    // ---------------------------------------------------------------- Ring of the Game
    private static void game(ServerPlayer p, long now) {
        long next = Wave25Data.getLong(p, Wave25Data.GAME_NEXT);
        if (next == 0L) {                                                       // first time worn: the Game starts counting
            Wave25Data.putLong(p, Wave25Data.GAME_NEXT, now + Wave25Tune.GAME_INTERVAL);
            return;
        }
        if (now < next || p.getRandom().nextDouble() >= Wave25Tune.GAME_THINK_CHANCE) return;
        Wave25Data.putLong(p, Wave25Data.GAME_NEXT, now + Wave25Tune.GAME_INTERVAL);
        float hurt = Wave25Pure.selfHurt(Wave25Pure.dice(1, 4, p.getRandom()::nextInt), p.getHealth(), p.getMaxHealth(), Wave25Tune.GAME_CAP);
        if (hurt > 0f) p.hurt(p.damageSources().magic(), hurt);
        Wave25Kit.hint(p, "You thought about The Game. You lost.");
        boolean saved = Wave25Pure.saves(p.getRandom().nextInt(20), Wave25Tune.GAME_DC, Wave25Tune.GAME_WIS_BONUS);
        if (saved) return;
        ServerLevel level = p.serverLevel();
        for (Player pl : level.players()) {
            if (pl instanceof ServerPlayer o && o != p && o.distanceToSqr(p) <= sq(Wave25Tune.GAME_ECHO_RANGE)) Fx.say(o, p.getName().getString() + " curses loudly about The Game!");
        }
        Fx.say(p, "You curse loudly about The Game!");
        int n = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(Wave25Tune.GAME_CURSE_RANGE), m -> m.isAlive() && m instanceof net.minecraft.world.entity.monster.Enemy)) {
            if (n++ >= Wave25Tune.GAME_CURSE_MOBS) break;
            if (!p.isCreative()) m.setTarget(p);
        }
        RukhFx.burst(level, p, Wave25Kit.dark(), 6);
    }

    // ---------------------------------------------------------------- Ring of the Mimic
    private static void mimics(ServerPlayer p, long now) {
        List<net.minecraft.world.entity.Entity> found = p.serverLevel().getEntities(p, p.getBoundingBox().inflate(Wave25Tune.MIMIC_RANGE),
                en -> Wave25Pure.isMimicId(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(en.getType()).toString()));
        if (found.isEmpty()) return;
        int n = 0;
        for (net.minecraft.world.entity.Entity en : found) {
            if (n++ >= 8) break;
            if (en instanceof LivingEntity le) Fx.addEffect(le, "minecraft:glowing", Wave25Tune.MIMIC_GLOW, 0);
        }
        if (now - Wave25Data.getLong(p, Wave25Data.MIMIC_MSG) >= Wave25Tune.MIMIC_MSG_GAP) {
            Wave25Data.putLong(p, Wave25Data.MIMIC_MSG, now);
            Wave25Kit.hint(p, "The ring trembles: a mimic is near.");
        }
    }

    // ---------------------------------------------------------------- Soup Spoon of Improbability
    private static void spoon(ServerPlayer p, long now, boolean live) {
        boolean worn = live && RelicFlags.has(p, Wave25Tune.F_SPOON);
        long embarrassedUntilDay = Wave25Data.getLong(p, Wave25Data.SPOON_DAY);
        boolean bold = worn && Wave25Kit.today(p.serverLevel()) >= embarrassedUntilDay;
        Wave25Kit.setMod(p, "minecraft:generic.attack_damage", "spoon_str", Wave25Tune.SPOON_DAMAGE, Operation.ADD_VALUE, bold);
        Wave25Kit.setMod(p, "minecraft:generic.attack_speed", "spoon_dex", Wave25Tune.SPOON_SPEED, Operation.ADD_VALUE, bold);
        if (live && Wave25Data.getLong(p, Wave25Data.STEADY_UNTIL) > now) {
            for (var inst : new ArrayList<>(p.getActiveEffects())) {
                var key = inst.getEffect().unwrapKey();
                if (key.isPresent() && inst.getAmplifier() <= 1 && Wave25Pure.isDisadvantage(key.get().location().toString())) p.removeEffect(inst.getEffect());
            }
        }
    }

    // ---------------------------------------------------------------- Spirit Lantern
    private static void lantern(ServerPlayer p, ServerLevel level) {
        RukhFx.aura(level, p, Wave25Kit.light());
        if (level.getMaxLocalRawBrightness(p.blockPosition()) < Wave25Tune.LANTERN_DARK) Wave6Kit.give(p, "minecraft:night_vision", 260, 0, true);
        int n = 0;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(Wave25Tune.LANTERN_GLOW_RANGE), t -> t != p && t.isAlive() && t.getType().is(EntityTypeTags.UNDEAD))) {
            if (n++ >= 8) break;
            Fx.addEffect(t, "minecraft:glowing", 60, 0);
        }
    }

    // ---------------------------------------------------------------- The Forgotten Ring
    private static void forgotten(ServerPlayer p, boolean live) {
        if (!Wave25Data.getBool(p, Wave25Data.VANISHED)) return;
        if (!live || !RelicFlags.has(p, Wave25Tune.F_FORGOTTEN)) {
            unvanish(p, "The ring is gone from your finger and you are seen again.");
            return;
        }
        Wave6Kit.give(p, "minecraft:invisibility", 60, 0, true);
    }

    static void unvanish(ServerPlayer p, String line) {
        Wave25Data.putBool(p, Wave25Data.VANISHED, false);
        Wave6Kit.lift(p, "minecraft:invisibility");
        if (line != null) Wave25Kit.hint(p, line);
        RukhFx.burst(p.serverLevel(), p, Wave25Kit.dark(), 4);
    }

    // ---------------------------------------------------------------- Witch's Ring
    private static void witch(ServerPlayer p, boolean live) {
        boolean on = Wave25Data.getBool(p, Wave25Data.FORM_ON);
        if (on && (!live || !RelicFlags.has(p, Wave25Tune.F_WITCH))) { endForm(p, "The ring leaves you and the beast with it."); on = false; }
        if (on) {
            int left = Wave25Pure.allowanceAfter(Wave25Data.getInt(p, Wave25Data.FORM_LEFT), 20);
            Wave25Data.putInt(p, Wave25Data.FORM_LEFT, left);
            if (left <= 0) { endForm(p, "Your beast form runs out and you stand as yourself."); on = false; }
        }
        Wave25Pure.Form f = Wave25Pure.Form.of(Wave25Data.getInt(p, Wave25Data.FORM));
        Wave25Kit.setMod(p, "minecraft:generic.scale", "form_scale", f.scale(), Operation.ADD_MULTIPLIED_BASE, on);
        Wave25Kit.setMod(p, "minecraft:generic.attack_damage", "form_attack", f.attackBonus(), Operation.ADD_VALUE, on && f.attackBonus() != 0);
        Wave25Kit.setMod(p, "minecraft:generic.knockback_resistance", "form_knock", f.knockbackResistance(), Operation.ADD_VALUE, on && f.knockbackResistance() != 0);
        if (!on) return;
        for (String fx : f.effects()) {
            int c = fx.lastIndexOf(':');
            Wave6Kit.give(p, fx.substring(0, c), 60, Integer.parseInt(fx.substring(c + 1)), true);
        }
    }

    static void endForm(ServerPlayer p, String line) {
        Wave25Data.putBool(p, Wave25Data.FORM_ON, false);
        for (String m : new String[]{"form_scale", "form_attack", "form_knock"}) {
            String attr = m.equals("form_scale") ? "minecraft:generic.scale" : m.equals("form_attack") ? "minecraft:generic.attack_damage" : "minecraft:generic.knockback_resistance";
            Wave25Kit.setMod(p, attr, m, 0, Operation.ADD_VALUE, false);
        }
        if (line != null) Wave25Kit.hint(p, line);
        RukhFx.burst(p.serverLevel(), p, Wave25Kit.dark(), 6);
    }

    // ---------------------------------------------------------------- Bucket of Disguise
    private static void bucket(ServerPlayer p, ServerLevel level, long now) {
        long today = Wave25Kit.today(level);
        if (Wave25Data.getLong(p, Wave25Data.BUCKET_DAY) != today + 1) {
            Wave25Data.putLong(p, Wave25Data.BUCKET_DAY, today + 1);
            Fx.say(p, "Nobody can see you under the bucket. You are quite certain of it. (And a murmur of thoughts that are not your own begins.)");
        }
        if (p.tickCount % Wave25Tune.BUCKET_PERIOD != 7) return;
        // a mob that has you in its sights, even from further off, gives itself away first
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(Wave25Tune.BUCKET_WARN_RANGE), m -> m.isAlive() && m.getTarget() == p)) {
            Wave25Kit.hint(p, "You hear " + m.getName().getString() + " thinking: \"Is that a bucket? I will hit the bucket.\"");
            return;
        }
        List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(Wave25Tune.BUCKET_RANGE),
                t -> t != p && t.isAlive() && !(t instanceof ArmorStand) && !t.isSpectator()));
        if (near.isEmpty()) return;
        LivingEntity t = near.get(p.getRandom().nextInt(Math.min(near.size(), 6)));
        Wave25Kit.hint(p, "You hear " + t.getName().getString() + " thinking: " + Wave25Pure.thought(Wave25Kit.typeId(t), p.getRandom().nextInt(100)));
    }

    // ---------------------------------------------------------------- Corpse Mask
    private static void mask(ServerPlayer p, long now) {
        long until = Wave25Data.getLong(p, Wave25Data.MASK_UNTIL);
        if (until == 0L) return;
        if (until <= now || !RelicFlags.has(p, Wave25Tune.F_MASK)) {
            Wave25Data.remove(p, Wave25Data.MASK_UNTIL);
            p.getPersistentData().remove("nusmp_w25_face");
            Wave25Kit.hint(p, "The borrowed face slides off.");
            return;
        }
        calmKin(p, p.getPersistentData().getString("nusmp_w25_face"));
    }

    // ---------------------------------------------------------------- Death Knight's Helmet
    private static void dkh(ServerPlayer p, ServerLevel level, boolean live) {
        boolean worn = live && RelicFlags.has(p, Wave25Tune.F_DKH);
        if (!worn) {
            Wave25Kit.setMod(p, "minecraft:generic.max_health", "dkh_con", 0, Operation.ADD_VALUE, false);
            return;
        }
        long today = Wave25Kit.today(level);
        boolean known = Wave25Data.has(p, Wave25Data.DK_LAST);
        long last = Wave25Data.getLong(p, Wave25Data.DK_LAST);
        if (!known || Wave25Pure.helmetLapsed(last, today)) {
            Wave25Data.putInt(p, Wave25Data.DK_DAYS, 0);
            Wave25Data.putInt(p, Wave25Data.DK_CON, 0);
            Wave25Data.putInt(p, Wave25Data.DK_STAGE, 0);
            last = today - 1;
        }
        if (last != today) {
            int days = Wave25Data.getInt(p, Wave25Data.DK_DAYS) + 1;
            int con = Wave25Pure.conAfterDay(Wave25Data.getInt(p, Wave25Data.DK_CON));
            int dc = Wave25Pure.helmetDc(days);
            Wave25Data.putInt(p, Wave25Data.DK_DAYS, days);
            Wave25Data.putLong(p, Wave25Data.DK_LAST, today);
            boolean saved = Wave25Pure.saves(p.getRandom().nextInt(20), dc, Wave25Tune.DKH_WIS_BONUS);
            if (saved) {
                Wave25Data.putInt(p, Wave25Data.DK_CON, con);
                Fx.say(p, "Death's Call (DC " + dc + "): you resist. The helmet's stamina grows: Constitution bonus +" + con + ".");
            } else {
                int stage = Wave25Data.getInt(p, Wave25Data.DK_STAGE) + 1;
                Wave25Data.putInt(p, Wave25Data.DK_STAGE, stage);
                Wave25Data.putInt(p, Wave25Data.DK_CON, 0);
                float dmg = Wave25Pure.selfHurt(Wave25Pure.curseDamage(stage, p.getRandom()::nextInt), p.getHealth(), p.getMaxHealth(), Wave25Tune.DKH_CAP);
                if (dmg > 0f) p.hurt(p.damageSources().magic(), dmg);
                RukhFx.burst(level, p, Wave25Kit.dark(), 8);
                Fx.say(p, "Death's Call (DC " + dc + ") finds you: your mind burns for " + (int) dmg + " and the helmet takes its stamina back. The next failure hurts " + (stage + 1) + " times 2d8.");
            }
        }
        int con = Wave25Data.getInt(p, Wave25Data.DK_CON);
        Wave25Kit.setMod(p, "minecraft:generic.max_health", "dkh_con", con * Wave25Tune.DKH_CON_HP, Operation.ADD_VALUE, con > 0);
        Wave6Kit.lift(p, "tensura:fear");                                       // immune to being frightened
    }
}
