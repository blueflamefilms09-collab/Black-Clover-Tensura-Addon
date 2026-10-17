package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Wave 17: the bodies of the actives of the seventeen notebooks, pouches and charms. Costs and cooldowns are settled by the engine before a body runs;
 * a body that cannot do its thing says why and changes nothing (a single-use relic is used up only when its effect really happened). Server side only.
 */
public final class Wave17Acts {
    private Wave17Acts() {}

    private static final String BEADS = "w17_beads", SEL = "w17_sel", MARKS = "w17_marks", SWEETS = "w17_sweets", LIFE = "w17_life", LIFE_DAY = "w17_life_day";
    private static final int VIOLET = 0xB06BFF, HELL = 0xFF5A1A, ACID = 0x6FD04A;

    private static IntUnaryOperator roll(ServerPlayer p) { return n -> p.getRandom().nextInt(n); }

    private static void say(ServerPlayer p, String text) { Fx.say(p, text); }

    // ================================================================ Ancient Notebooks
    /** Consult the notebook (held for three seconds, the card's "one minute"): its bonus 1d4-1 decides how much it had to say, and what that is worth. */
    public static void consult(ActCtx c) {
        ServerPlayer p = c.player();
        Wave17Rules.Notebook nb = Wave17Rules.Notebook.byId(c.def().id());
        if (nb == null) return;
        int b = Wave17Rules.studyBonus(roll(p));
        int t = Wave17Rules.studyTicks(b);
        String what;
        switch (nb) {
            case ARCANIST -> {
                Fx.lendSkill(p, "tensura:magic_sense", t);
                if (b >= 2) Wave17Kit.give(p, "tensura:magicule_regeneration", t, 0);
                what = "the glyphs steady your sense of magic";
            }
            case BOTANIST -> {
                int luck = Wave17Rules.botanistLuck(b);
                if (luck >= 0) Wave17Kit.give(p, "minecraft:luck", t, luck);
                Fx.lendSkill(p, "tensura:poison_resistance", t);
                if (b >= 2) p.getFoodData().eat(2, 0.3f);
                what = "you know which plants feed and which kill";
            }
            case HISTORIAN -> {
                p.giveExperiencePoints(Wave17Rules.historianXp(b));
                Fx.lendSkill(p, "tensura:analytical_appraisal", t);
                what = "old accounts sharpen your eye for what things are";
            }
            case PHYSICIAN -> {
                Fx.heal(p, Wave17Rules.physicianHeal(b));
                Wave17Kit.give(p, "minecraft:regeneration", 200 + 100 * b, b >= 2 ? 1 : 0);
                p.removeEffect(MobEffects.POISON);
                what = "you dress your wounds the way the notes say";
            }
            default -> {                                                      // THEOLOGIAN
                Fx.shield(p, Wave17Rules.theologianWard(b), 1200);
                Fx.lendSkill(p, "tensura:holy_attack_resistance", t);
                if (b >= 2) Wave17Kit.give(p, "minecraft:resistance", t / 2, 0);
                what = "a remembered rite wards you";
            }
        }
        Fx.burst(p.serverLevel(), p.position(), 0xD8B86A);
        say(p, nb.check + " study: " + (b == 0 ? "the book had little to say, but " : "the book had +" + b + " to give and ") + what + " (" + (t / 1200) + " minutes).");
    }

    // ================================================================ Fast Travel Beads
    private record Dest(ResourceKey<Level> dim, BlockPos pos, String name) {}

    private static ListTag marks(ItemStack stack) { return Wave17Kit.tag(stack).getList(MARKS, Tag.TAG_COMPOUND); }

    private static Dest markAt(ListTag list, int i) {
        CompoundTag m = list.getCompound(i);
        ResourceLocation rl = ResourceLocation.tryParse(m.getString("Dim"));
        if (rl == null) return null;
        return new Dest(ResourceKey.create(Registries.DIMENSION, rl), new BlockPos(m.getInt("X"), m.getInt("Y"), m.getInt("Z")), m.getString("Name"));
    }

    /** The destination of a choice: 0 your bed, 1 where you last fell, then your marks. Null when there is none (no bed yet, never fallen). */
    private static Dest destination(ServerPlayer p, ListTag marks, int choice) {
        if (choice == 0) {
            BlockPos bed = p.getRespawnPosition();
            return bed == null ? null : new Dest(p.getRespawnDimension(), bed, "your bed");
        }
        if (choice == 1) {
            GlobalPos g = p.getLastDeathLocation().orElse(null);
            return g == null ? null : new Dest(g.dimension(), g.pos(), "where you last fell");
        }
        int i = Wave17Rules.savedIndex(choice, marks.size());
        return i < 0 ? null : markAt(marks, i);
    }

    private static String describe(Dest d) { return d.name() + " (" + d.pos().getX() + " " + d.pos().getY() + " " + d.pos().getZ() + ")"; }

    /** Beads on the chain; a fresh chain rolls its 3d10 the first time it is read. */
    private static int beads(ItemStack stack, ServerPlayer p) {
        CompoundTag t = Wave17Kit.tag(stack);
        if (t.contains(BEADS)) return t.getInt(BEADS);
        int n = Wave17Rules.rollBeads(roll(p));
        Wave17Kit.edit(stack, tag -> tag.putInt(BEADS, n));
        return n;
    }

    /** Break a bead: you and up to six who crouch beside you (and your tamed companions) arrive at the chosen place of this world that you have been to before. */
    public static void beadJump(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        ItemStack stack = Wave17Kit.live(c);
        if (Labyrinth.in(level)) { say(p, "The Dungeon swallows the road: the beads will not carry anyone in or out of it."); return; }
        ListTag marks = marks(stack);
        int choice = Math.floorMod(Wave17Kit.tag(stack).getInt(SEL), Wave17Rules.choiceCount(marks.size()));
        if (Wave17Rules.isNewMark(choice, marks.size())) { rememberPlace(c); return; }
        int left = beads(stack, p);
        if (left <= 0) { say(p, "The chain is bare: every bead is spent."); return; }
        Dest d = destination(p, marks, choice);
        if (d == null) { say(p, "Nowhere to go there yet: pick another destination (sneak and use), or mark the place you stand."); return; }
        if (d.dim() != level.dimension()) { say(p, describe(d) + " is on another plane: the beads only carry you across this one."); return; }
        level.getChunk(d.pos().getX() >> 4, d.pos().getZ() >> 4);                      // the place is loaded before anyone arrives
        Vec3 spot = Wave17Kit.safeSpot(level, p, d.pos());
        if (spot == null) { say(p, "There is no safe ground at " + describe(d) + ". The bead stays on the chain."); return; }
        List<LivingEntity> party = party(p);
        Fx.burst(level, p.position(), VIOLET);
        p.teleportTo(spot.x, spot.y, spot.z);
        p.fallDistance = 0;
        int[][] ring = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}};
        int carried = 0;
        for (LivingEntity o : party) {
            Vec3 at = Wave17Kit.safeSpot(level, o, d.pos().offset(ring[carried % ring.length][0], 0, ring[carried % ring.length][1]));
            if (at == null) continue;
            Fx.burst(level, o.position(), VIOLET);
            if (o instanceof ServerPlayer sp) sp.teleportTo(at.x, at.y, at.z);
            else {
                o.teleportTo(at.x, at.y, at.z);
                if (o instanceof Mob m) m.getNavigation().stop();
            }
            o.fallDistance = 0;
            carried++;
        }
        Fx.burst(level, spot, VIOLET);
        Fx.ring(level, spot, 2.0, VIOLET);
        int now = left - 1;
        Wave17Kit.edit(stack, t -> t.putInt(BEADS, now));
        say(p, "A bead cracks and the stars swallow you: " + describe(d) + (carried > 0 ? ", with " + carried + " companion" + (carried == 1 ? "" : "s") : "")
                + ". " + now + (now == 1 ? " bead" : " beads") + " left.");
    }

    /** Who comes along: players within six blocks who are crouching (they agree), your tamed animals and your relic summons; at most six. */
    private static List<LivingEntity> party(ServerPlayer p) {
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(Wave17Rules.COMPANION_RANGE), e -> {
            if (e == p || !e.isAlive() || e.isSpectator()) return false;
            if (e instanceof ServerPlayer o) return o.isShiftKeyDown() && !o.isPassenger();
            if (e instanceof TamableAnimal t) return t.isOwnedBy(p) && !t.isOrderedToSit();
            return e.getPersistentData().hasUUID(OWNER_KEY) && e.getPersistentData().getUUID(OWNER_KEY).equals(p.getUUID());
        });
        found.sort((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p)));
        return found.size() > Wave17Rules.MAX_COMPANIONS ? new ArrayList<>(found.subList(0, Wave17Rules.MAX_COMPANIONS)) : found;
    }

    /** The persistent-data key the core puts on a relic summon (it names the owner). */
    private static final String OWNER_KEY = "nusmp_relic_owner";

    /** Sneak and use: the next destination on the chain. */
    public static void chooseDestination(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = Wave17Kit.live(c);
        ListTag marks = marks(stack);
        int next = Wave17Rules.nextChoice(Wave17Kit.tag(stack).getInt(SEL), marks.size());
        Wave17Kit.edit(stack, t -> t.putInt(SEL, next));
        if (Wave17Rules.isNewMark(next, marks.size())) { say(p, "Destination: [mark the place you stand on] (use to save it)."); return; }
        Dest d = destination(p, marks, next);
        say(p, d == null ? "Destination: " + (next == 0 ? "your bed (you have none yet)" : "where you last fell (you have not fallen yet)") : "Destination: " + describe(d)
                + (d.dim() != p.serverLevel().dimension() ? " (another plane: out of reach from here)" : ""));
    }

    /** Remember this place: saves where you stand as a destination (at most six; a mark within eight blocks is replaced, else the oldest goes). */
    public static void rememberPlace(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = Wave17Kit.live(c);
        if (Labyrinth.in(p.serverLevel())) { say(p, "The Dungeon will not be remembered."); return; }
        ListTag list = marks(stack).copy();
        String dim = p.serverLevel().dimension().location().toString();
        BlockPos here = p.blockPosition();
        int near = -1;
        double nearD = Double.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag m = list.getCompound(i);
            if (!dim.equals(m.getString("Dim"))) continue;
            double dist = Math.sqrt(here.distSqr(new BlockPos(m.getInt("X"), m.getInt("Y"), m.getInt("Z"))));
            if (dist < nearD) { nearD = dist; near = i; }
        }
        int slot = Wave17Rules.markSlot(list.size(), near, nearD);
        CompoundTag mark = new CompoundTag();
        mark.putString("Dim", dim);
        mark.putInt("X", here.getX());
        mark.putInt("Y", here.getY());
        mark.putInt("Z", here.getZ());
        int where;
        if (slot >= 0 && slot < list.size()) { mark.putString("Name", list.getCompound(slot).getString("Name")); list.set(slot, mark); where = slot; }
        else {
            if (slot < 0) list.remove(0);
            mark.putString("Name", "mark " + (p.getRandom().nextInt(900) + 100));
            list.add(mark);
            where = list.size() - 1;
        }
        int choice = 2 + where;
        Wave17Kit.edit(stack, t -> { t.put(MARKS, list); t.putInt(SEL, choice); });
        Fx.ring(p.serverLevel(), p.position(), 1.5, VIOLET);
        say(p, "The beads remember this place (" + here.getX() + " " + here.getY() + " " + here.getZ() + ") and it is now your destination.");
    }

    // ================================================================ Madama Cupcake
    /** Eat it: three days of nourishment (hunger never falls under 17 of 20 while it lasts, so you can still eat). Used up. */
    public static void eatCupcake(ActCtx c) {
        ServerPlayer p = c.player();
        long now = Wave17Kit.clock(p.serverLevel());
        CompoundTag d = Wave17Kit.data(p);
        if (d.getLong("cupcake_until") > now) { say(p, "You are still fed by the last cupcake."); return; }
        d.putLong("cupcake_until", now + Wave17Rules.CUPCAKE_TICKS);
        p.getFoodData().setFoodLevel(Math.max(p.getFoodData().getFoodLevel(), 18));
        p.getFoodData().setSaturation(Math.max(p.getFoodData().getSaturationLevel(), 6f));
        p.removeEffect(MobEffects.HUNGER);
        Fx.burst(p.serverLevel(), p.position(), 0xC9A0FF);
        spend(c);
        say(p, "Sweet, light and strangely filling: you will not go hungry for three days, and there is still room to eat.");
    }

    /** Uses up the relic stack of the act (not in creative). */
    private static void spend(ActCtx c) {
        if (c.player().isCreative()) return;
        ItemStack s = Wave17Kit.live(c);
        if (s.isEmpty()) s = c.stack();
        s.shrink(1);
    }

    // ================================================================ pouches
    private static Wave17Vault.Vault vault(ActCtx c, String kind, int sides, int rows) {
        ServerPlayer p = c.player();
        return Wave17Vault.get(p.getServer()).of(Wave17Kit.live(c), p, kind, sides, rows);
    }

    /** Bag of Realistic Capacity: a fist-sized pouch of two rows. */
    public static void openRealistic(ActCtx c) { vault(c, Wave17Vault.KIND_POUCH, 1, Wave17Rules.POUCH_ROWS).open(c.player(), 0, "Bag of Realistic Capacity"); }

    /** Sneak and use: the pouch pulls in the loose items within six blocks (at most twelve at a time) that fit. */
    public static void gather(ActCtx c) {
        ServerPlayer p = c.player();
        Wave17Vault.Vault v = vault(c, Wave17Vault.KIND_POUCH, 1, Wave17Rules.POUCH_ROWS);
        List<ItemEntity> loose = p.serverLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(6.0), e -> e.isAlive() && !e.hasPickUpDelay());
        loose.sort((a, b) -> Double.compare(a.distanceToSqr(p), b.distanceToSqr(p)));
        int taken = 0, tried = 0;
        for (ItemEntity e : loose) {
            if (tried++ >= 12) break;
            ItemStack in = e.getItem();
            if (in.isEmpty() || Wave17Vault.storage(in) || Wave17Vault.uniqueRelic(in)) continue;
            int before = in.getCount();
            ItemStack rest = v.boxes[0].addItem(in.copy());
            if (rest.getCount() == before) continue;
            taken++;
            if (rest.isEmpty()) e.discard();
            else e.setItem(rest);
        }
        Fx.burst(p.serverLevel(), p.position(), 0xFFE0D0);
        say(p, taken == 0 ? "Nothing to gather, or the pouch is full." : "The pouch pulls in " + taken + " loose item" + (taken == 1 ? "" : "s") + ".");
    }

    /** Shared Pouches of Transfer: the one pocket dimension both of you reach (three rows). */
    public static void openShared(ActCtx c) { vault(c, Wave17Vault.KIND_SHARED, 1, Wave17Rules.BACKPACK_ROWS).open(c.player(), 0, "Shared Pouches of Transfer"); }

    public static void linkShared(ActCtx c) { link(c, Wave17Vault.KIND_SHARED, 1, Wave17Rules.BACKPACK_ROWS, "Shared Pouches of Transfer"); }

    /** Bags of Transferrence: your own bag (side 0). */
    public static void openNearBag(ActCtx c) { vault(c, Wave17Vault.KIND_TRANSFER, 2, Wave17Rules.POUCH_ROWS).open(c.player(), 0, "Near Bag"); }

    /** The far bag (side 1), which your companion reaches with /pouch far from anywhere. */
    public static void openFarBag(ActCtx c) { vault(c, Wave17Vault.KIND_TRANSFER, 2, Wave17Rules.POUCH_ROWS).open(c.player(), 1, "Far Bag"); }

    /** Speak the command words: the contents of the two bags trade places. */
    public static void transfer(ActCtx c) {
        ServerPlayer p = c.player();
        Wave17Vault.Vault v = vault(c, Wave17Vault.KIND_TRANSFER, 2, Wave17Rules.POUCH_ROWS);
        v.swapSides();
        Fx.burst(p.serverLevel(), p.position(), 0xFFD27F);
        say(p, "The words are spoken: the contents of the near and the far bag trade places. No distance matters.");
    }

    public static void linkTransfer(ActCtx c) { link(c, Wave17Vault.KIND_TRANSFER, 2, Wave17Rules.POUCH_ROWS, "Bags of Transferrence"); }

    /** Sneak and use (or the key) to hand the other pouch to someone: the nearest player within eight blocks who is crouching too (their consent) becomes the companion; no one there ends the link. */
    private static void link(ActCtx c, String kind, int sides, int rows, String title) {
        ServerPlayer p = c.player();
        Wave17Vault.Vault v = vault(c, kind, sides, rows);
        ServerPlayer best = null;
        double bestD = 64.0;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || !o.isAlive() || o.isSpectator() || !o.isShiftKeyDown()) continue;
            double d = o.distanceToSqr(p);
            if (d < bestD) { bestD = d; best = o; }
        }
        if (best != null) {
            v.setPartner(best);
            say(p, best.getGameProfile().getName() + " now holds the other half of the " + title + ": they reach it with /pouch from anywhere, while you carry the relic.");
            say(best, p.getGameProfile().getName() + " has linked you to their " + title + ". Type /pouch to see how to reach it.");
        } else if (v.partner != null) {
            String old = v.partnerName;
            v.setPartner(null);
            say(p, old + " no longer holds the other half of the " + title + ".");
        } else {
            say(p, "No one beside you agrees to share it: ask a friend within eight blocks to crouch, then use this again.");
        }
    }

    // ================================================================ Life's Emergency
    /** The stored life now (dawns applied and written back). A fresh cuff starts with one dawn's worth. */
    public static int lifeStored(ServerPlayer p, ItemStack stack, int gradeTier) {
        CompoundTag t = Wave17Kit.tag(stack);
        long today = Wave17Kit.today(p.serverLevel());
        int stored = t.contains(LIFE) ? t.getInt(LIFE) : Wave17Rules.LIFE_PER_DAWN;
        long last = t.contains(LIFE_DAY) ? t.getLong(LIFE_DAY) : today;
        int max = Wave17Rules.lifeMax(gradeTier);
        int now = Wave17Rules.lifeRoll(stored, last, today, max);
        if (!t.contains(LIFE) || now != stored || last != today) Wave17Kit.edit(stack, tag -> { tag.putInt(LIFE, now); tag.putLong(LIFE_DAY, today); });
        return now;
    }

    /** Empties the store (after it was spent) and starts a new cycle today. */
    public static void lifeSpent(ServerPlayer p, ItemStack stack) {
        long today = Wave17Kit.today(p.serverLevel());
        Wave17Kit.edit(stack, tag -> { tag.putInt(LIFE, 0); tag.putLong(LIFE_DAY, today); });
    }

    /** Read the cuff: how much life it has drawn in. */
    public static void readCuff(ActCtx c) {
        ServerPlayer p = c.player();
        int tier = c.def().grade().tier();
        int now = lifeStored(p, Wave17Kit.live(c), tier);
        int max = Wave17Rules.lifeMax(tier);
        float heal = Wave17Rules.lifeHeal(now, p.getMaxHealth());
        say(p, "The cuff holds " + now + " of " + max + " stored life (" + (int) heal + " health if you fall); it draws " + Wave17Rules.LIFE_PER_DAWN + " more each dawn.");
    }

    // ================================================================ Plastic Sheriff Badge
    /** "Halt! In the name of the law!": every foe within ten blocks tests your bluff (d20 + 3 against its resolve); those who believe you lose the will to fight. */
    public static void halt(ActCtx c) {
        ServerPlayer p = c.player();
        int believed = 0, doubted = 0;
        for (LivingEntity t : Fx.foesAround(p, 10.0)) {
            int dc = Wave17Rules.bluffDc(t.getMaxHealth());
            if (Wave17Rules.bluff(Wave17Rules.die(roll(p), 20), dc)) {
                Wave17Kit.give(t, "tensura:fear", 160, 0);
                Wave17Kit.give(t, "minecraft:slowness", 100, 0);
                if (t instanceof Mob m) m.setTarget(null);
                believed++;
            } else doubted++;
        }
        Fx.ring(p.serverLevel(), p.position(), 3.0, 0xD8DCE4);
        say(p, believed + doubted == 0 ? "\"Halt!\" There is no one to bluff." : "\"Halt! In the name of the law!\" " + believed + " believe the badge, " + doubted + " do not.");
    }

    // ================================================================ Urn of Black Pudding
    /** Throw the urn: it shatters on the first foe you are looking at (or where you look), acid on the target and its nonmagical armour, and a small black pudding fights for you. Used up. */
    public static void throwUrn(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity target = null;
        for (LivingEntity e : Fx.foesAround(p, Wave17Rules.URN_RANGE)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1.0e-4 || to.normalize().dot(look) < 0.94 || !p.hasLineOfSight(e)) continue;
            target = e;
            break;
        }
        Vec3 impact = target != null ? target.position() : eye.add(look.scale(Math.min(8.0, Wave17Rules.URN_RANGE)));
        if (target != null) {
            Fx.damage(p, target, Wave17Rules.urnDamage(roll(p)), "minecraft:magic");
            Wave17Kit.give(target, "tensura:corrosion", 200, 0);
            corrode(target);
        }
        RukhFx.spray(level, Wave17Kit.align(ACID), impact.x, impact.y + 0.8, impact.z, 40, 0.5, 0.5, 0.5, 0.05);
        RukhFx.spray(level, RukhFx.Alignment.DARK, impact.x, impact.y + 0.8, impact.z, 16, 0.4, 0.4, 0.4, 0.02);
        LivingEntity pudding = null;
        for (String type : new String[]{"tensura:slime", "minecraft:slime"}) {
            pudding = Fx.summon(p, type, Wave17Rules.PUDDING_TICKS);
            if (pudding != null) break;
        }
        if (pudding != null) {
            var hp = pudding.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(Wave17Rules.PUDDING_HEALTH);
            pudding.setHealth(Wave17Rules.PUDDING_HEALTH);
            Vec3 at = Wave17Kit.safeSpot(level, pudding, BlockPos.containing(impact));
            if (at != null) pudding.teleportTo(at.x, at.y, at.z);
        }
        spend(c);
        say(p, (target != null ? "The urn bursts on " + target.getName().getString() + ": acid" : "The urn shatters") + (pudding != null ? ", and a small black pudding slithers out to fight for you." : "."));
    }

    /** The acid takes a bite out of every nonmagical armour piece the target wears; a piece eaten through is gone. */
    private static void corrode(LivingEntity target) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = target.getItemBySlot(slot);
            if (s.isEmpty() || !s.isDamageableItem() || s.isEnchanted() || Relics.defOf(s) != null) continue;
            int now = Wave17Rules.corrode(s.getDamageValue(), s.getMaxDamage());
            if (now >= s.getMaxDamage()) {
                target.setItemSlot(slot, ItemStack.EMPTY);
                if (target instanceof ServerPlayer sp) sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Your armour is eaten through by the acid!").withStyle(ChatFormatting.RED), true);
            } else s.setDamageValue(now);
        }
    }

    // ================================================================ Crystal Dragon
    /** Wake the crystal dragon: an illusory dragon of shadow rises from the statue for eight seconds and breathes where you look. The statue turns to dust. */
    public static void wakeDragon(ActCtx c) {
        ServerPlayer p = c.player();
        if (!Wave17Dragon.start(p)) { say(p, "A dragon already answers you, or too many dragons are loose in the world."); return; }
        spend(c);
        say(p, "The crystal dragon unfolds into a huge shadowy dragon. Aim with your look: it breathes every second.");
    }

    // ================================================================ Martel's Magical Bag of Candy
    /** Eat a sweet and roll on the wild magic table. */
    public static void eatSweet(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack stack = Wave17Kit.live(c);
        CompoundTag t = Wave17Kit.tag(stack);
        int left = t.contains(SWEETS) ? t.getInt(SWEETS) : Wave17Rules.SWEETS_PER_BAG;
        if (left <= 0) { say(p, "The bag is empty: only crumbs of sugar."); return; }
        int now = left - 1;
        Wave17Kit.edit(stack, tag -> tag.putInt(SWEETS, now));
        for (Wave17Rules.Surge s : Wave17Rules.sweet(roll(p))) surge(p, s);
        say(p, "Crunch. " + now + (now == 1 ? " sweet" : " sweets") + " left in the bag.");
    }

    /** What one entry of the wild magic table does. */
    static void surge(ServerPlayer p, Wave17Rules.Surge s) {
        ServerLevel level = p.serverLevel();
        String note;
        switch (s) {
            case GLOW -> { Wave17Kit.give(p, "minecraft:glowing", 600, 0); note = "you glow like a lantern"; }
            case FEATHER -> { Wave17Kit.give(p, "minecraft:slow_falling", 1200, 0); note = "you are light as a feather"; }
            case LEAP -> { Wave17Kit.give(p, "minecraft:jump_boost", 600, 2); note = "your legs are springs"; }
            case LEVITATE -> {
                Wave17Kit.give(p, "minecraft:levitation", 60, 0);
                Wave17Kit.give(p, "minecraft:slow_falling", 240, 0);
                note = "you float up, up";
            }
            case HEAL -> { Fx.heal(p, 6f); note = "warm sugar mends you"; }
            case SWIFT -> { Wave17Kit.give(p, "minecraft:speed", 600, 1); note = "you are very, very quick"; }
            case QUEASY -> { Wave17Kit.give(p, "minecraft:nausea", 200, 0); note = "that one was a bad batch"; }
            case VANISH -> { Wave17Kit.give(p, "minecraft:invisibility", 1200, 0); note = "you fade from sight"; }
            case WARD -> { Wave17Kit.give(p, "minecraft:fire_resistance", 1200, 0); note = "a cool shell of sugar glass"; }
            case LESSON -> { p.giveExperiencePoints(15); note = "the wrapper holds a tiny lesson"; }
            case HOP -> {
                Vec3 at = Wave17Kit.safeSpot(level, p, p.blockPosition().offset(p.getRandom().nextInt(13) - 6, 0, p.getRandom().nextInt(13) - 6));
                if (at != null) { Fx.burst(level, p.position(), VIOLET); p.teleportTo(at.x, at.y, at.z); p.fallDistance = 0; note = "pop! you hop elsewhere"; }
                else note = "pop! but there was nowhere to hop";
            }
            case SNEEZE -> {
                for (LivingEntity f : Fx.foesAround(p, 5.0)) {
                    Vec3 away = f.position().subtract(p.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1.0e-4) f.push(away.normalize().x * 1.1, 0.35, away.normalize().z * 1.1);
                    f.hurtMarked = true;
                    Wave17Kit.give(f, "minecraft:slowness", 60, 1);
                }
                note = "a sugary sneeze shoves everything away";
            }
            case SLUGGISH -> { Wave17Kit.give(p, "minecraft:slowness", 200, 1); Wave17Kit.give(p, "minecraft:weakness", 200, 0); note = "your limbs turn to syrup"; }
            case SIGHT -> { Wave17Kit.give(p, "minecraft:night_vision", 6000, 0); note = "the dark turns to dusk"; }
            case MEND -> { Wave17Kit.give(p, "minecraft:regeneration", 200, 1); note = "you knit back together"; }
            case AEGIS -> { Fx.shield(p, 8f, 600); note = "a candy-coated shell hardens around you"; }
            case HASTE -> { Wave17Kit.give(p, "minecraft:haste", 1200, 1); note = "your hands blur"; }
            case SHOVE -> { Fx.dash(p, 6.0); note = "something shoves you forward"; }
            case FORTUNE -> { Wave17Kit.give(p, "minecraft:luck", 6000, 0); note = "luck is on your side"; }
            default -> note = "the sugar fizzes and nothing happens";
        }
        Fx.burst(level, p.position(), 0xFF9AD0);
        say(p, "Wild magic (" + s.label().toLowerCase(java.util.Locale.ROOT) + "): " + note + ".");
    }

    // ================================================================ The Devil's Due
    /** Pay the coin: hellfire, a horned devil at your side for five minutes, and the coin is gone. */
    public static void payTheDevil(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity devil = null;
        for (String type : Wave17Rules.DEVIL_TYPES) {
            devil = Fx.summon(p, type, Wave17Rules.DEVIL_TICKS);
            if (devil != null) break;
        }
        if (devil == null) { say(p, "The coin stays cold: nothing in hell answers here."); return; }
        ServerLevel level = p.serverLevel();
        RukhFx.spray(level, RukhFx.Alignment.LIGHT, devil.getX(), devil.getY() + 1.0, devil.getZ(), 60, 0.6, 0.9, 0.6, 0.08);
        Fx.ring(level, devil.position(), 2.5, HELL);
        Fx.burst(level, devil.position(), HELL);
        spend(c);
        say(p, "The coin erupts in hellfire and a horned devil steps out to fight at your side. It is yours for five minutes; the coin is gone.");
    }
}
