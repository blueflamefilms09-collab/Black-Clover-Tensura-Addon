package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Why: the server side of every use of the twelve wave-24 relics (act bodies). The core has already checked attunement, paid mana or existence
 * points and set the cooldown and the daily-use counter before a body runs. The numbers are in {@link Wave24Rules}, the long-lived things (timers,
 * the rage, the trance, the orb's flight) are finished by {@link Wave24Events}. Nothing here edits blocks except Lumpy's hand pressing a lever, a
 * button, a door or a gate, and not inside the Labyrinth.
 */
final class Wave24Acts {
    private Wave24Acts() {}

    // keys inside the player's own saved compound (Wave24Util.data)
    static final String K_COOK_UNTIL = "cook_until", K_COOK_ITEM = "cook_item", K_COOK_N = "cook_n", K_EGG_UNTIL = "egg_until", K_LUMPY_AT = "lumpy_at",
            K_RAGE_UNTIL = "rage_until", K_REGEN_AT = "regen_at", K_REGROW_AT = "regrow_at", K_TRAIL = "trail", K_MEM = "mem", K_MEM_DIM = "mem_dim",
            K_MEM_SEALED = "mem_sealed", K_TRANCE_UNTIL = "trance_until", K_TRANCE_STEP = "trance_step", K_DRY_TARGET = "dry_target", K_DRY_NEXT = "dry_next",
            K_DRY_LEFT = "dry_left", K_TRAIL_AT = "trail_at";
    // keys on a creature
    static final String K_HALT_UNTIL = "nusmp_w24_halt_until", K_HALT_FOR = "nusmp_w24_halt_for", K_DUST = "nusmp_w24_dust";
    // keys on the Orb of Plagues stack
    static final String K_CHARGES = "w24_charges", K_CHARGE_DAY = "w24_charge_day";

    static final String ORB = "orb_of_plagues";

    private static long now(ServerPlayer p) { return p.level().getGameTime(); }

    // ================================================================ Instant Cooking Pot
    /** Cook: puts up to sixteen raw foods from the hands (or the pack) in the pot; a d4 of five-second steps later the cooked food drops into the pack. */
    static void cook(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag d = Wave24Util.data(p);
        if (d.getLong(K_COOK_UNTIL) > 0) {
            Wave24Util.bar(p, "The pot is still cooking.", ChatFormatting.GRAY);
            return;
        }
        ItemStack raw = rawFood(p);
        if (raw.isEmpty()) {
            Wave24Util.bar(p, "Nothing in your hands or pack the pot can cook.", ChatFormatting.GRAY);
            return;
        }
        String cooked = Wave24Rules.cooked(Wave24Util.idOf(raw));
        int n = Wave24Rules.cookCount(raw.getCount());
        raw.shrink(n);
        int die = Wave24Rules.d(Wave24Util.dice(p), 4);
        int ticks = Wave24Rules.cookTicks(die);
        d.putLong(K_COOK_UNTIL, now(p) + ticks);
        d.putString(K_COOK_ITEM, cooked);
        d.putInt(K_COOK_N, n);
        ServerLevel l = p.serverLevel();
        RukhFx.burst(l, p.position().add(0, 1.0, 0), RukhFx.Alignment.LIGHT, 8);
        l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
        Wave24Util.say(p, "Instant Cooking Pot: rolled " + die + ", " + n + " item" + (n == 1 ? "" : "s") + " in the pot, ready in " + ticks / 20 + " seconds.", ChatFormatting.GOLD);
    }

    private static ItemStack rawFood(ServerPlayer p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) if (!s.isEmpty() && Wave24Rules.cooked(Wave24Util.idOf(s)) != null) return s;
        for (ItemStack s : p.getInventory().items) if (!s.isEmpty() && Wave24Rules.cooked(Wave24Util.idOf(s)) != null) return s;
        return ItemStack.EMPTY;
    }

    /** The microwave pings: the cooked food goes to the pack (or the ground if it is full). Runs from the player's second, whether or not the pot is still worn. */
    static void finishCooking(ServerPlayer p, CompoundTag d) {
        if (d.getLong(K_COOK_UNTIL) <= 0 || d.getLong(K_COOK_UNTIL) > now(p)) return;
        String id = d.getString(K_COOK_ITEM);
        int n = d.getInt(K_COOK_N);
        d.remove(K_COOK_UNTIL);
        d.remove(K_COOK_ITEM);
        d.remove(K_COOK_N);
        ResourceLocation rl = Wave24Util.rl(id);
        Item item = rl == null ? null : BuiltInRegistries.ITEM.get(rl);
        if (item == null || n <= 0 || item == net.minecraft.world.item.Items.AIR) return;
        ItemStack out = new ItemStack(item, n);
        p.getInventory().placeItemBackInInventory(out);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
        Wave24Util.say(p, "Instant Cooking Pot: ding! " + n + " cooked.", ChatFormatting.GOLD);
    }

    // ================================================================ Lucky Egg
    /** Kiss the Egg: for thirty seconds the next real blow against you is rolled again (never worse), and Luck rises. */
    static void kissEgg(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave24Util.data(p).putLong(K_EGG_UNTIL, now(p) + Wave24Rules.EGG_ARMED_TICKS);
        Wave24Util.give(p, "minecraft:luck", Wave24Rules.EGG_ARMED_TICKS, 1);
        RukhFx.gather(p.serverLevel(), p, RukhFx.Alignment.LIGHT, 8);
        Wave24Util.say(p, "Lucky Egg: you kiss the egg. For 30 seconds the next heavy blow is rolled again, and fortune leans your way.", ChatFormatting.AQUA);
    }

    // ================================================================ Lumpy
    /** Mage Hand: a spectral hand presses a lever, button, door or gate within eight blocks and pulls loose items within ten blocks to you. */
    static void mageHand(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        boolean did = false;
        Vec3 eye = p.getEyePosition();
        Vec3 far = eye.add(p.getLookAngle().scale(Wave24Rules.LUMPY_BLOCK_REACH));
        BlockHitResult hit = l.clip(new ClipContext(eye, far, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.BLOCK && !DungeonGuard.on(l)) {
            BlockState st = l.getBlockState(hit.getBlockPos());
            var b = st.getBlock();
            if (b instanceof ButtonBlock || b instanceof LeverBlock || b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock) {
                st.useWithoutItem(l, p, hit);
                RukhFx.stream(l, eye.add(p.getLookAngle().scale(0.8)), Vec3.atCenterOf(hit.getBlockPos()), RukhFx.Alignment.LIGHT, 3);
                did = true;
            }
        }
        Vec3 c = p.position();
        List<ItemEntity> items = l.getEntitiesOfClass(ItemEntity.class, new AABB(c, c).inflate(Wave24Rules.LUMPY_PULL_RADIUS), ItemEntity::isAlive);
        int pulled = 0;
        for (ItemEntity it : items) {
            if (pulled >= 24) break;
            Vec3 to = p.position().add(0, 0.6, 0).subtract(it.position());
            it.setDeltaMovement(to.scale(0.22).add(0, 0.12, 0));
            it.setNoPickUpDelay();
            it.hurtMarked = true;
            RukhFx.stream(l, it.position(), p.position().add(0, 0.8, 0), RukhFx.Alignment.LIGHT, 1);
            pulled++;
        }
        did |= pulled > 0;
        Wave24Util.bar(p, did ? "Lumpy: \"Mage Hand! Wiggle wiggle!\"" : "Lumpy: \"There is nothing to grab, genius.\"", ChatFormatting.AQUA);
    }

    /** Message: a telepathic ping to every other player within 64 blocks, with where you are. */
    static void message(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        int sent = 0;
        String where = (int) p.getX() + " " + (int) p.getY() + " " + (int) p.getZ();
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || o.isSpectator() || o.distanceToSqr(p) > (double) Wave24Rules.LUMPY_MESSAGE_RADIUS * Wave24Rules.LUMPY_MESSAGE_RADIUS) continue;
            Wave24Util.say(o, "Lumpy whispers in your head: " + p.getName().getString() + " is calling from " + where + ".", ChatFormatting.AQUA);
            sent++;
        }
        Wave24Util.say(p, sent == 0 ? "Lumpy: \"Nobody is listening. Typical.\"" : "Lumpy: \"Done! " + sent + " poor soul" + (sent == 1 ? "" : "s") + " now know where you are.\"", ChatFormatting.AQUA);
    }

    /** Detect Thoughts: creatures within sixteen blocks glow for eight seconds and Lumpy reads out what the nearest few are thinking. */
    static void detectThoughts(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        List<LivingEntity> minds = Wave24Util.around(l, p.position(), Wave24Rules.LUMPY_SCAN_RADIUS, e -> e != p && (e instanceof Mob || e instanceof Player));
        if (minds.isEmpty()) {
            Wave24Util.say(p, "Lumpy: \"I hear crickets. Mostly yours.\"", ChatFormatting.AQUA);
            return;
        }
        int shown = 0;
        for (LivingEntity e : minds) {
            Wave24Util.give(e, "minecraft:glowing", 160, 0);
            if (shown >= Wave24Rules.LUMPY_THOUGHTS) continue;
            Wave24Util.say(p, "  " + e.getName().getString() + " " + thought(p, e), ChatFormatting.DARK_AQUA);
            shown++;
        }
        Wave24Util.say(p, "Lumpy: \"I peeked into " + minds.size() + " head" + (minds.size() == 1 ? "" : "s") + ". It was mostly empty in there.\"", ChatFormatting.AQUA);
        RukhFx.gather(l, p, RukhFx.Alignment.LIGHT, 8);
    }

    private static String thought(ServerPlayer reader, LivingEntity e) {
        if (e instanceof Player o) return Wave24Util.data(o).getBoolean(K_MEM_SEALED) ? "has a blank wall where their thoughts should be." : "is thinking about " + (o.getHealth() < o.getMaxHealth() * 0.4f ? "bandages." : "what to do next.");
        if (e instanceof Mob m) {
            LivingEntity t = m.getTarget();
            if (t == reader) return "is thinking about hurting YOU.";
            if (t != null) return "is thinking about hurting " + t.getName().getString() + ".";
            if (e instanceof Enemy) return "is restless and hungry for company.";
            return "is thinking about grass, mostly.";
        }
        return "is not thinking anything.";
    }

    // ================================================================ Monster Compass
    /** The nearest hostile monster within 64 blocks, or null. */
    static LivingEntity nearestMonster(ServerPlayer p) {
        Vec3 c = p.position();
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(Wave24Rules.COMPASS_RANGE), x -> x.isAlive() && !x.isSpectator() && x instanceof Enemy)) {
            double dd = e.distanceToSqr(c);
            if (dd < bd) {
                bd = dd;
                best = e;
            }
        }
        return best;
    }

    private static ChatFormatting eyeColour(int threat) {
        return switch (threat) {
            case 0 -> ChatFormatting.YELLOW;
            case 1 -> ChatFormatting.GOLD;
            case 2 -> ChatFormatting.RED;
            case 3 -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.DARK_GRAY;
        };
    }

    /** Reads the compass once: arrow, distance and the darkness of the eye, on the action bar. */
    static void readCompass(ServerPlayer p) {
        LivingEntity t = nearestMonster(p);
        if (t == null) {
            Wave24Util.bar(p, "Monster Compass: the needle spins, no monster near.", ChatFormatting.GRAY);
            return;
        }
        double dx = t.getX() - p.getX(), dz = t.getZ() - p.getZ();
        int bearing = Wave24Rules.bearing(dx, dz, p.getYRot());
        double atk = t.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                ? t.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) : 0.0;
        int threat = Wave24Rules.threat(t.getMaxHealth(), atk);
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        String vert = t.getY() - p.getY() > 6 ? " (above)" : t.getY() - p.getY() < -6 ? " (below)" : "";
        Component msg = Component.literal("Monster Compass  " + Wave24Rules.arrow(bearing) + "  " + dist + " m" + vert + "  eye: " + Wave24Rules.eye(threat)).withStyle(eyeColour(threat));
        Wave24Util.bar(p, msg);
    }

    /** Open or close the lid: while open the compass is read every second. */
    static void compassLid(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (ctx.isOn("lid")) readCompass(p);
        else Wave24Util.bar(p, "Monster Compass: the lid snaps shut.", ChatFormatting.GRAY);
    }

    // ================================================================ Official-Looking Badge
    /** Show the Badge: ordinary hostile creatures within eight blocks lose their nerve (fear) and stop hunting you for ten seconds. */
    static void showBadge(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        long until = now(p) + Wave24Rules.BADGE_HALT_TICKS;
        int n = 0;
        for (LivingEntity e : Wave24Util.around(l, p.position(), Wave24Rules.BADGE_RADIUS, x -> Wave24Util.foe(p, x) && !(x instanceof Player))) {
            if (!Wave24Rules.respects(e.getMaxHealth())) continue;
            Wave24Util.give(e, "tensura:fear", Wave24Rules.BADGE_FEAR_TICKS, 0);
            e.getPersistentData().putLong(K_HALT_UNTIL, until);
            e.getPersistentData().putUUID(K_HALT_FOR, p.getUUID());
            if (e instanceof Mob m && m.getTarget() == p) m.setTarget(null);
            n++;
        }
        RukhFx.burst(l, p.position().add(0, 1.6, 0), RukhFx.Alignment.LIGHT, 8);
        Wave24Util.bar(p, n == 0 ? "You flash the badge. Nobody is impressed." : "You flash the badge: " + n + " creature" + (n == 1 ? " backs" : "s back") + " away.", ChatFormatting.GOLD);
    }

    /** Is this creature still halted by the badge of this player? */
    static boolean halted(LivingEntity e, Player p) {
        CompoundTag t = e.getPersistentData();
        return t.getLong(K_HALT_UNTIL) > e.level().getGameTime() && t.hasUUID(K_HALT_FOR) && t.getUUID(K_HALT_FOR).equals(p.getUUID());
    }

    // ================================================================ Orb of Plagues
    private static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    /** Charges the orb holds after any dawn regain (a fresh orb is full); writes the regained number back. */
    static int charges(ServerPlayer p, ItemStack orb) {
        CompoundTag t = tag(orb);
        long today = Wave24Rules.day(now(p));
        if (!t.contains(K_CHARGES, Tag.TAG_INT)) {
            CustomData.update(DataComponents.CUSTOM_DATA, orb, (CompoundTag c) -> {
                c.putInt(K_CHARGES, Wave24Rules.ORB_MAX_CHARGES);
                c.putLong(K_CHARGE_DAY, today);
            });
            return Wave24Rules.ORB_MAX_CHARGES;
        }
        int have = t.getInt(K_CHARGES), after = Wave24Rules.regained(have, t.getLong(K_CHARGE_DAY), today, Wave24Rules.d(Wave24Util.dice(p), 6));
        if (after != have || t.getLong(K_CHARGE_DAY) != today) CustomData.update(DataComponents.CUSTOM_DATA, orb, (CompoundTag c) -> {
            c.putInt(K_CHARGES, after);
            c.putLong(K_CHARGE_DAY, today);
        });
        return after;
    }

    private static void setCharges(ItemStack orb, int n) {
        CustomData.update(DataComponents.CUSTOM_DATA, orb, (CompoundTag c) -> c.putInt(K_CHARGES, Math.max(0, n)));
    }

    /** Pays the charges of a spell. False (and nothing paid) if the orb has too few; an empty orb rolls a d20 and shatters on a 1. */
    private static boolean pay(ActCtx ctx, int cost) {
        ServerPlayer p = ctx.player();
        ItemStack orb = ctx.stack();
        int have = charges(p, orb);
        if (have <= 0) {
            if (Wave24Rules.shatters(Wave24Rules.d(Wave24Util.dice(p), 20))) {
                Wave24Util.say(p, "Orb of Plagues: the empty orb cracks in your hand!", ChatFormatting.DARK_GREEN);
                shatter(p, orb, 3.0, 4);
            } else {
                Wave24Util.bar(p, "Orb of Plagues: no charges left (it regains 1d6+3 at dawn).", ChatFormatting.GRAY);
            }
            return false;
        }
        if (have < cost) {
            Wave24Util.bar(p, "Orb of Plagues: that needs " + cost + " charges, it holds " + have + ".", ChatFormatting.GRAY);
            return false;
        }
        setCharges(orb, have - cost);
        Wave24Util.bar(p, "Orb of Plagues: " + (have - cost) + " charge" + (have - cost == 1 ? "" : "s") + " left.", ChatFormatting.DARK_GREEN);
        return true;
    }

    private static void sicken(ServerPlayer p, LivingEntity t, int dc, Wave24Rules.Disease dis, int ticks) {
        boolean saved = Wave24Rules.saves(Wave24Rules.d(Wave24Util.dice(p), 20), Wave24Rules.saveBonus(t.getMaxHealth()), dc);
        int dur = Wave24Rules.afterSave(ticks, saved, t.getMaxHealth());
        if (dur <= 0) return;
        int k = 0;
        for (String spec : dis.effects) {
            if (saved && k > 0) break;                                      // a saved throw shrugs off all but the first symptom
            Wave24Util.give(t, Wave24Rules.effectId(spec), dur, Wave24Rules.effectAmp(spec));
            k++;
        }
    }

    private static Vec3 aimPoint(ServerPlayer p, Wave24Util.Aim a, double fallback) {
        return a.target() != null ? a.target().position().add(0, a.target().getBbHeight() * 0.5, 0) : (a.end().distanceTo(p.getEyePosition()) < 1.0 ? p.getEyePosition().add(p.getLookAngle().scale(fallback)) : a.end());
    }

    static void raySickness(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave24Util.Aim a = Wave24Util.aim(p, Wave24Rules.ORB_RANGE, e -> Wave24Util.touchable(p, e));
        if (a.target() == null) {
            Wave24Util.bar(p, "Ray of Sickness: nothing in the line.", ChatFormatting.GRAY);
            return;
        }
        if (!pay(ctx, Wave24Rules.COST_SICKNESS)) return;
        LivingEntity t = a.target();
        Wave24Util.trail(p.serverLevel(), p.getEyePosition(), aimPoint(p, a, 8), true);
        Fx.damage(p, t, Wave24Rules.roll(Wave24Util.dice(p), 2, 8), "minecraft:magic");
        boolean saved = Wave24Rules.saves(Wave24Rules.d(Wave24Util.dice(p), 20), Wave24Rules.saveBonus(t.getMaxHealth()), 13);
        Wave24Util.give(t, "minecraft:poison", Wave24Rules.afterSave(200, saved, t.getMaxHealth()), 0);
    }

    static void rayEnfeeblement(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave24Util.Aim a = Wave24Util.aim(p, Wave24Rules.ORB_RANGE, e -> Wave24Util.touchable(p, e));
        if (a.target() == null) {
            Wave24Util.bar(p, "Ray of Enfeeblement: nothing in the line.", ChatFormatting.GRAY);
            return;
        }
        if (!pay(ctx, Wave24Rules.COST_ENFEEBLE)) return;
        LivingEntity t = a.target();
        Wave24Util.trail(p.serverLevel(), p.getEyePosition(), aimPoint(p, a, 8), true);
        boolean saved = Wave24Rules.saves(Wave24Rules.d(Wave24Util.dice(p), 20), Wave24Rules.saveBonus(t.getMaxHealth()), 13);
        int dur = Wave24Rules.afterSave(360, saved, t.getMaxHealth());
        Wave24Util.give(t, "minecraft:weakness", dur, 1);
        Wave24Util.give(t, "minecraft:slowness", dur, 0);
        Wave24Util.give(t, "tensura:fragility", dur / 2, 0);
    }

    static void stinkingCloud(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        if (!pay(ctx, Wave24Rules.COST_CLOUD)) return;
        Wave24Util.Aim a = Wave24Util.aim(p, Wave24Rules.ORB_RANGE, e -> false);
        Vec3 c = a.end().distanceTo(p.getEyePosition()) < 2.0 ? p.position().add(p.getLookAngle().scale(6)) : a.end();
        RukhFx.spray(l, RukhFx.Alignment.DARK, c.x, c.y + 0.5, c.z, 12, 1.6, 0.6, 1.6, 0.02);
        for (LivingEntity e : Wave24Util.around(l, c, Wave24Rules.CLOUD_RADIUS, x -> Wave24Util.touchable(p, x))) {
            boolean saved = Wave24Rules.saves(Wave24Rules.d(Wave24Util.dice(p), 20), Wave24Rules.saveBonus(e.getMaxHealth()), 14);
            int dur = Wave24Rules.afterSave(240, saved, e.getMaxHealth());
            Wave24Util.give(e, "minecraft:poison", dur, 0);
            Wave24Util.give(e, "minecraft:nausea", dur, 0);
        }
    }

    static void blight(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave24Util.Aim a = Wave24Util.aim(p, Wave24Rules.ORB_RANGE, e -> Wave24Util.touchable(p, e));
        if (a.target() == null) {
            Wave24Util.bar(p, "Blight: nothing in the line.", ChatFormatting.GRAY);
            return;
        }
        if (!pay(ctx, Wave24Rules.COST_BLIGHT)) return;
        LivingEntity t = a.target();
        Wave24Util.trail(p.serverLevel(), p.getEyePosition(), aimPoint(p, a, 8), true);
        boolean saved = Wave24Rules.saves(Wave24Rules.d(Wave24Util.dice(p), 20), Wave24Rules.saveBonus(t.getMaxHealth()), 15);
        float dmg = Wave24Rules.roll(Wave24Util.dice(p), 4, 8);
        Fx.damageHeavy(p, t, saved ? dmg / 2f : dmg, "minecraft:magic");
        Wave24Util.give(t, "minecraft:wither", Wave24Rules.afterSave(120, saved, t.getMaxHealth()), 0);
    }

    static void contagion(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        Wave24Util.Aim a = Wave24Util.aim(p, Wave24Rules.ORB_RANGE, e -> Wave24Util.touchable(p, e));
        if (a.target() == null) {
            Wave24Util.bar(p, "Contagion: nothing in the line.", ChatFormatting.GRAY);
            return;
        }
        if (!pay(ctx, Wave24Rules.COST_CONTAGION)) return;
        Wave24Rules.Disease dis = Wave24Rules.disease(Wave24Rules.d(Wave24Util.dice(p), 6));
        Wave24Util.trail(p.serverLevel(), p.getEyePosition(), aimPoint(p, a, 8), true);
        sicken(p, a.target(), 15, dis, Wave24Rules.DISEASE_TICKS);
        Wave24Util.say(p, "Contagion: " + a.target().getName().getString() + " is struck with " + dis.label + ".", ChatFormatting.DARK_GREEN);
    }

    /** Plague Cloud: smash the orb at your feet; every creature within seven blocks (not you) is struck with a disease, the saving throw harder the more charges were left. The orb is destroyed. */
    static void plagueCloud(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ItemStack orb = ctx.stack();
        int left = charges(p, orb);
        shatter(p, orb, Wave24Rules.PLAGUE_RADIUS, Wave24Rules.plagueDc(left));
        Wave24Util.say(p, "Plague Cloud: you smash the orb. It cannot be used again.", ChatFormatting.DARK_GREEN);
    }

    /** The orb breaks: a cloud of the given radius round the bearer strikes every creature in it (not the bearer) with a disease; the orb is used up. */
    private static void shatter(ServerPlayer p, ItemStack orb, double radius, int dc) {
        ServerLevel l = p.serverLevel();
        Vec3 c = p.position();
        RukhFx.spray(l, RukhFx.Alignment.DARK, c.x, c.y + 1.0, c.z, 12, radius * 0.35, 0.8, radius * 0.35, 0.03);
        RukhFx.spray(l, RukhFx.Alignment.DARK, c.x, c.y + 0.5, c.z, 12, radius * 0.5, 0.4, radius * 0.5, 0.05);
        l.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);
        for (LivingEntity e : Wave24Util.around(l, c, radius, x -> Wave24Util.touchable(p, x))) {
            sicken(p, e, dc, Wave24Rules.disease(Wave24Rules.d(Wave24Util.dice(p), 6)), Wave24Rules.DISEASE_TICKS);
        }
        orb.shrink(1);
        Relics.invalidate(p);
    }

    /** Ritual: Detect Poison and Disease. Creatures within 24 blocks that carry a poison or a sickness glow for eight seconds. Costs nothing. */
    static void detectPoison(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        int n = 0;
        for (LivingEntity e : Wave24Util.around(p.serverLevel(), p.position(), Wave24Rules.DETECT_RADIUS, x -> x != p)) {
            if (sick(e)) {
                Wave24Util.give(e, "minecraft:glowing", 160, 0);
                n++;
            }
        }
        Wave24Util.bar(p, n == 0 ? "Orb of Plagues: no poison or disease near." : "Orb of Plagues: " + n + " afflicted creature" + (n == 1 ? "" : "s") + " now glow.", ChatFormatting.DARK_GREEN);
    }

    private static boolean sick(LivingEntity e) {
        for (String id : new String[]{"minecraft:poison", "minecraft:wither", "minecraft:nausea", "tensura:infection", "tensura:fatal_poison", "tensura:magicule_poison", "tensura:fragility"}) {
            var h = Wave24Util.effect(id);
            if (h.isPresent() && e.hasEffect(h.get())) return true;
        }
        return false;
    }

    // ================================================================ Orb of the Dryad
    /** Nature's Light: the orb floats beside you and sheds light (night sight for you); toggled, refreshed from the player's second. */
    static void dryadLight(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        if (ctx.isOn("light")) {
            Wave24Util.give(p, "minecraft:night_vision", Wave24Rules.DRYAD_LIGHT_TICKS, 0);
            Wave24Util.bar(p, "Orb of the Dryad: the orb shines.", ChatFormatting.GREEN);
        } else {
            Wave24Util.bar(p, "Orb of the Dryad: the light fades.", ChatFormatting.GRAY);
        }
    }

    /** Healing Flight: the orb flies at the creature in your look (or back to you), heals everyone it passes, then hovers and heals the target three more times. */
    static void healingFlight(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Wave24Util.Aim a = Wave24Util.aim(p, Wave24Rules.DRYAD_RANGE, e -> !(e instanceof Enemy) && e.isAlive());
        LivingEntity target = a.target() != null ? a.target() : p;
        Vec3 from = p.getEyePosition().add(0, -0.3, 0), to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        if (target != p && Wave24Rules.dryadBlocked(Wave24Util.solidBetween(l, from, to))) {
            Wave24Util.bar(p, "Orb of the Dryad: a thick wall stops the orb. It floats back to you.", ChatFormatting.GRAY);
            target = p;
            to = from;
        }
        int healed = 0;
        if (target != p) {
            AABB box = new AABB(from, to).inflate(2.0);
            for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive() && !x.isSpectator() && !(x instanceof Enemy))) {
                if (e == target || healed >= 12) continue;
                if (e.getBoundingBox().inflate(Wave24Rules.DRYAD_LINE_WIDTH * 0.5).clip(from, to).isEmpty()) continue;
                healTouch(e, Wave24Util.dice(p));
                healed++;
            }
            RukhFx.stream(l, from, to, RukhFx.Alignment.LIGHT, 5);
        }
        CompoundTag d = Wave24Util.data(p);
        d.putString(K_DRY_TARGET, target.getUUID().toString());
        d.putInt(K_DRY_LEFT, Wave24Rules.DRYAD_PULSES);
        d.putLong(K_DRY_NEXT, now(p) + Wave24Rules.flightTicks(from.distanceTo(to)));
        Wave24Util.bar(p, "Orb of the Dryad: the orb flies to " + (target == p ? "you" : target.getName().getString()) + (healed > 0 ? ", mending " + healed + " on the way." : "."), ChatFormatting.GREEN);
    }

    static void healTouch(LivingEntity e, Wave24Rules.Dice dice) {
        float amt = Wave24Rules.dryadHeal(Wave24Rules.d(dice, 4));
        if (e instanceof ServerPlayer sp) Fx.heal(sp, amt);
        else e.heal(amt);
        if (e.level() instanceof ServerLevel l) RukhFx.aura(l, e, RukhFx.Alignment.LIGHT);
    }

    /** One second of the orb's hover: when the next pulse is due, it heals the target again; at the end it flies home if the bearer is within range. */
    static void dryadPass(ServerPlayer p, CompoundTag d) {
        int left = d.getInt(K_DRY_LEFT);
        if (left <= 0 || d.getLong(K_DRY_NEXT) > now(p)) return;
        LivingEntity t = null;
        try {
            if (p.serverLevel().getEntity(UUID.fromString(d.getString(K_DRY_TARGET))) instanceof LivingEntity le && le.isAlive()) t = le;
        } catch (IllegalArgumentException bad) {
            t = null;
        }
        if (t == null) {
            d.putInt(K_DRY_LEFT, 0);
            return;
        }
        healTouch(t, Wave24Util.dice(p));
        left--;
        d.putInt(K_DRY_LEFT, left);
        d.putLong(K_DRY_NEXT, now(p) + Wave24Rules.DRYAD_PULSE_TICKS);
        if (left == 0 && t != p && t.distanceToSqr(p) <= Wave24Rules.DRYAD_RANGE * Wave24Rules.DRYAD_RANGE) {
            RukhFx.stream(p.serverLevel(), t, p, RukhFx.Alignment.LIGHT, 3);           // the orb returns; it does not heal on the way back
        }
    }

    // ================================================================ Pocket Time
    /** Roll 1d12 and skip forward to the next time the clock shows that hour (the day moves on for the whole world; not in the Labyrinth or where time is fixed). */
    static void pocketTime(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        if (DungeonGuard.on(l) || l.dimensionType().hasFixedTime()) {
            Wave24Util.bar(p, "Pocket Time: the watch ticks, but time does not move here.", ChatFormatting.GRAY);
            return;
        }
        int roll = Wave24Rules.d(Wave24Util.dice(p), 12);
        long fwd = Wave24Rules.ticksForward(l.getDayTime(), roll);
        l.setDayTime(l.getDayTime() + fwd);
        RukhFx.burst(l, p.position().add(0, 1.2, 0), RukhFx.Alignment.LIGHT, 12);
        l.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
        Wave24Util.say(p, "Pocket Time: rolled " + roll + ". The world jumps " + (fwd / 1000) + " hours ahead, to " + Wave24Rules.clockText(l.getDayTime()) + ".", ChatFormatting.GOLD);
    }

    // ================================================================ Ring of Power
    /** Disintegrate (once a day, costs existence points): a ray that does 10d6 force damage; a creature it kills turns to dust and drops nothing but experience. */
    static void disintegrate(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel l = p.serverLevel();
        Wave24Util.Aim a = Wave24Util.aim(p, Wave24Rules.POWER_RANGE, e -> Wave24Util.touchable(p, e));
        Vec3 end = a.target() != null ? a.target().position().add(0, a.target().getBbHeight() * 0.5, 0) : a.end();
        RukhFx.stream(l, p.getEyePosition().add(0, -0.2, 0), end, RukhFx.Alignment.DARK, 6);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        LivingEntity t = a.target();
        if (t == null) {
            Wave24Util.bar(p, "Disintegrate: the ray strikes nothing.", ChatFormatting.GRAY);
            return;
        }
        t.getPersistentData().putLong(K_DUST, now(p));
        Fx.damageHeavy(p, t, Wave24Rules.disintegrate(Wave24Util.dice(p)), "minecraft:magic");
        if (t.isAlive()) t.getPersistentData().remove(K_DUST);
        else RukhFx.spray(l, RukhFx.Alignment.DARK, end.x, end.y, end.z, 12, 0.4, 0.5, 0.4, 0.1);
    }

    // ================================================================ Ring of Memories
    private static long[] trailOf(CompoundTag d, String key) { return d.contains(key, Tag.TAG_LONG_ARRAY) ? d.getLongArray(key) : new long[0]; }

    /** The wearer's second: adds a sample to the trail every forty ticks and drops anything older than a week. */
    static void sampleTrail(ServerPlayer p, CompoundTag d) {
        long now = now(p);
        if (d.getLong(K_TRAIL_AT) > now - Wave24Rules.TRAIL_EVERY_TICKS && d.getLong(K_TRAIL_AT) <= now) return;
        d.putLong(K_TRAIL_AT, now);
        long[] t = Wave24Rules.push(trailOf(d, K_TRAIL), now, p.getBlockX(), p.getBlockY(), p.getBlockZ());
        d.putLongArray(K_TRAIL, Wave24Rules.prune(t, now));
        d.putString("trail_dim", p.serverLevel().dimension().location().toString());
    }

    /** Remember: stores your recent journey (up to the last few minutes of it, all of it within a week) in the ring, if the ring holds no memory yet. Sneak to keep no recollection of it. */
    static void remember(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag d = Wave24Util.data(p);
        if (Wave24Rules.samples(trailOf(d, K_MEM)) > 0) {
            Wave24Util.bar(p, "Ring of Memories: the ring already holds a memory. Erase it first.", ChatFormatting.GRAY);
            return;
        }
        long[] t = Wave24Rules.prune(trailOf(d, K_TRAIL), now(p));
        if (Wave24Rules.samples(t) < 3) {
            Wave24Util.bar(p, "Ring of Memories: there is not enough of a journey to remember yet.", ChatFormatting.GRAY);
            return;
        }
        d.putLongArray(K_MEM, t);
        d.putString(K_MEM_DIM, d.getString("trail_dim"));
        d.putBoolean(K_MEM_SEALED, ctx.sneaking());
        d.putLongArray(K_TRAIL, new long[0]);
        RukhFx.gather(p.serverLevel(), p, RukhFx.Alignment.LIGHT, 8);
        Wave24Util.say(p, "Ring of Memories: " + Wave24Rules.samples(t) + " moments stored (" + Wave24Rules.span(t) / 20 + " seconds)." + (ctx.sneaking() ? " You keep no recollection of it, and no mind-reader can find it." : ""), ChatFormatting.AQUA);
    }

    /** Relive (once a day): a ten-second trance replays the memory as a ghostly trail of the place and path; you cannot move, and any damage breaks it. */
    static void relive(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag d = Wave24Util.data(p);
        long[] mem = trailOf(d, K_MEM);
        if (Wave24Rules.samples(mem) == 0) {
            Wave24Util.bar(p, "Ring of Memories: the ring holds no memory.", ChatFormatting.GRAY);
            return;
        }
        d.putLong(K_TRANCE_UNTIL, now(p) + Wave24Rules.TRANCE_TICKS);
        d.putInt(K_TRANCE_STEP, 0);
        Wave24Util.give(p, "minecraft:slowness", Wave24Rules.TRANCE_TICKS + 20, 9);
        Wave24Util.give(p, "minecraft:blindness", 30, 0);
        Wave24Util.say(p, "Ring of Memories: you slip into the memory. You are deaf to the world around you.", ChatFormatting.AQUA);
    }

    /** One second of the trance: shows the next slice of the memory's path (sparser the farther you are from where it happened). */
    static void trancePass(ServerPlayer p, CompoundTag d) {
        long until = d.getLong(K_TRANCE_UNTIL);
        if (until <= 0) return;
        long now = now(p);
        if (now >= until) {
            endTrance(p, d, true);
            return;
        }
        long[] mem = trailOf(d, K_MEM);
        int n = Wave24Rules.samples(mem);
        int step = d.getInt(K_TRANCE_STEP);
        d.putInt(K_TRANCE_STEP, step + 1);
        if (n == 0) return;
        String memDim = d.getString(K_MEM_DIM);
        double far = memDim.equals(p.serverLevel().dimension().location().toString()) ? Math.sqrt(p.distanceToSqr(mem[1], mem[2], mem[3])) : 1000.0;
        int stride = Wave24Rules.clarityStride(far);
        int[] range = Wave24Rules.tranceRange(n, step);
        ServerLevel l = p.serverLevel();
        Vec3 first = new Vec3(mem[1] + 0.5, mem[2] + 1.0, mem[3] + 0.5);
        boolean here = far < 64.0;
        Vec3 prev = null;
        for (int i = range[0]; i < range[1]; i += stride) {
            Vec3 at = new Vec3(mem[i * Wave24Rules.TRAIL_STRIDE + 1] + 0.5, mem[i * Wave24Rules.TRAIL_STRIDE + 2] + 1.0, mem[i * Wave24Rules.TRAIL_STRIDE + 3] + 0.5);
            // near the place the memory plays out on the spot; from afar it is a small replay floating in front of you
            Vec3 shown = here ? at : p.position().add(0, 2.4, 0).add(at.subtract(first).scale(0.2));
            RukhFx.burst(l, shown, RukhFx.Alignment.LIGHT, 2);
            if (prev != null) RukhFx.stream(l, prev, shown, RukhFx.Alignment.LIGHT, 2);
            prev = shown;
        }
        Wave24Util.give(p, "minecraft:slowness", 40, 9);
    }

    /** The trance ends: by time (the summary of where and how long ago) or because something hurt you. */
    static void endTrance(ServerPlayer p, CompoundTag d, boolean complete) {
        d.remove(K_TRANCE_UNTIL);
        d.remove(K_TRANCE_STEP);
        var slow = Wave24Util.effect("minecraft:slowness");
        if (slow.isPresent()) p.removeEffect(slow.get());
        long[] mem = trailOf(d, K_MEM);
        if (!complete) {
            Wave24Util.say(p, "Ring of Memories: the pain tears you out of the memory.", ChatFormatting.RED);
            return;
        }
        if (Wave24Rules.samples(mem) == 0) return;
        String where = d.getBoolean(K_MEM_SEALED) ? "somewhere you have chosen not to recall" : "near " + mem[1] + " " + mem[2] + " " + mem[3] + " (" + d.getString(K_MEM_DIM) + ")";
        long ago = Math.max(0L, now(p) - mem[(Wave24Rules.samples(mem) - 1) * Wave24Rules.TRAIL_STRIDE]) / 1200;
        Wave24Util.say(p, "Ring of Memories: the memory ends. It began " + where + ", about " + ago + " minutes ago, and lasted " + Wave24Rules.span(mem) / 20 + " seconds.", ChatFormatting.AQUA);
    }

    /** Erase: the stored memory is gone for good, making room for another. */
    static void erase(ActCtx ctx) {
        ServerPlayer p = ctx.player();
        CompoundTag d = Wave24Util.data(p);
        boolean had = Wave24Rules.samples(trailOf(d, K_MEM)) > 0;
        d.putLongArray(K_MEM, new long[0]);
        d.remove(K_MEM_DIM);
        d.remove(K_MEM_SEALED);
        Wave24Util.bar(p, had ? "Ring of Memories: the memory is erased." : "Ring of Memories: there was nothing to erase.", ChatFormatting.GRAY);
    }

    static boolean trancing(Player p) { return Wave24Util.running(p, K_TRANCE_UNTIL); }

    static boolean raging(Player p) { return Wave24Util.running(p, K_RAGE_UNTIL); }

    /** Is the toggle of a relic act on for the player? */
    static boolean on(Player p, String relicId, String act) { return RelicEngine.isOn(p, relicId, act); }
}
