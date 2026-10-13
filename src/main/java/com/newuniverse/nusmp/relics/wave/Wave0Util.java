package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wave 0: the small Minecraft-side helpers every part of the remake shares (persisted player data, per-day counters, dice, saving throws,
 * conditions that are saved against again every second, safe extra damage). The numbers live in {@link Wave0Rules}.
 */
final class Wave0Util {
    private Wave0Util() {}

    static final String K_SPOKEN = "nusmp_w0_";

    // ---------------------------------------------------------------- data
    /** The player's data that survives death and relogs (the same compound the Devil pacts use). */
    static CompoundTag persisted(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    static Wave0Rules.Dice dice(LivingEntity e) {
        net.minecraft.util.RandomSource r = e.getRandom();
        return r::nextInt;
    }

    static int roll(LivingEntity e, int n, int sides) { return Wave0Rules.roll(dice(e), n, sides); }

    /** The day counter (Minecraft day) a "once until dawn" feature runs on. */
    static long today(Player p) { return Wave0Rules.day(p.level().getDayTime()); }

    private static Wave0Rules.Uses usesOf(Player p, String key) {
        CompoundTag d = persisted(p);
        if (!d.contains(K_SPOKEN + key + "_day")) return null;
        return new Wave0Rules.Uses(d.getLong(K_SPOKEN + key + "_day"), d.getInt(K_SPOKEN + key + "_used"));
    }

    /** Uses left today of a feature limited to {@code max} a day. */
    static int usesLeft(Player p, String key, int max) { return Wave0Rules.usesLeft(usesOf(p, key), today(p), max); }

    /** Spends one use of a per-day feature; false (and nothing spent) when none is left. */
    static boolean takeDaily(Player p, String key, int max) {
        Wave0Rules.Uses next = Wave0Rules.takeUse(usesOf(p, key), today(p), max);
        if (next == null) return false;
        CompoundTag d = persisted(p);
        d.putLong(K_SPOKEN + key + "_day", next.day());
        d.putInt(K_SPOKEN + key + "_used", next.used());
        return true;
    }

    // ---------------------------------------------------------------- stack data
    static int stackInt(ItemStack s, String key, int dflt) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null) return dflt;
        CompoundTag t = d.copyTag();
        return t.contains(key) ? t.getInt(key) : dflt;
    }

    static long stackLong(ItemStack s, String key, long dflt) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null) return dflt;
        CompoundTag t = d.copyTag();
        return t.contains(key) ? t.getLong(key) : dflt;
    }

    static void edit(ItemStack s, Consumer<CompoundTag> change) { CustomData.update(DataComponents.CUSTOM_DATA, s, change); }

    /** The stack of this item in the main hand, else the off hand, else empty. */
    static ItemStack held(ServerPlayer p, Supplier<? extends Item> item) {
        Item it = item.get();
        if (p.getMainHandItem().is(it)) return p.getMainHandItem();
        if (p.getOffhandItem().is(it)) return p.getOffhandItem();
        return ItemStack.EMPTY;
    }

    static boolean holds(ServerPlayer p, Supplier<? extends Item> item) { return !held(p, item).isEmpty(); }

    // ---------------------------------------------------------------- who is who
    /**
     * A foe, by the same test the core's own helpers use: a hostile creature or one hunting the player, or another player they may harm (never
     * a creative or spectating one, an ally, a pet or an armour stand).
     */
    static boolean fair(ServerPlayer p, LivingEntity t) {
        if (t == p || !t.isAlive() || t.isSpectator() || t instanceof ArmorStand || t.isAlliedTo(p)) return false;
        if (t instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (t instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
        if (t instanceof Enemy || t.getType().getCategory() == MobCategory.MONSTER) return true;
        return t instanceof Mob m && m.getTarget() == p;
    }

    /** Anything a weapon swing may hit: a foe, or any other creature that is not an ally, a pet, a creative or spectating player or an armour stand. */
    static boolean targetable(ServerPlayer p, LivingEntity t) {
        if (t == p || !t.isAlive() || t.isSpectator() || t instanceof ArmorStand || t.isAlliedTo(p)) return false;
        if (t instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        return !(t instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID()));
    }

    static boolean isUndead(LivingEntity t) { return t.getType().is(EntityTypeTags.UNDEAD); }

    static boolean isConstruct(LivingEntity t) {
        if (t instanceof AbstractGolem || t instanceof ArmorStand) return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(t.getType());
        return id != null && Wave0Rules.isConstructPath(id.getPath());
    }

    /** The "critical hit" of this mod: a blow struck while falling. */
    static boolean crit(ServerPlayer p) { return p.fallDistance > 0 && !p.onGround() && !p.isInWater(); }

    static boolean hasBuff(LivingEntity t) {
        for (MobEffectInstance i : t.getActiveEffects()) if (i.getEffect().value().isBeneficial()) return true;
        return false;
    }

    /** Takes one beneficial effect off; true if there was one. */
    static boolean stripOne(LivingEntity t) {
        for (MobEffectInstance i : new ArrayList<>(t.getActiveEffects())) {
            if (i.getEffect().value().isBeneficial()) { t.removeEffect(i.getEffect()); return true; }
        }
        return false;
    }

    /** A creature that stands facing away from the wielder (they are in its back arc). */
    static boolean behind(ServerPlayer p, LivingEntity t) {
        Vec3 face = t.getViewVector(1f).multiply(1, 0, 1);
        Vec3 to = p.position().subtract(t.position()).multiply(1, 0, 1);
        if (face.lengthSqr() < 1e-4 || to.lengthSqr() < 1e-4) return false;
        return face.normalize().dot(to.normalize()) < -0.3;
    }

    static boolean surprised(ServerPlayer p, LivingEntity t) {
        boolean mob = t instanceof Mob;
        LivingEntity tg = mob ? ((Mob) t).getTarget() : null;
        return Wave0Rules.surprised(t.isSleeping(), behind(p, t), mob, tg != null, tg == p);
    }

    // ---------------------------------------------------------------- saving throws
    static int saveMod(LivingEntity t) {
        return Wave0Rules.saveMod(t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), t.getMaxHealth(),
                t.getAttributeValue(Attributes.ARMOR_TOUGHNESS), BalanceLaw.isBoss(t));
    }

    /** Does the target pass a save against this DC? {@code mode} is 1 for advantage, -1 for disadvantage, 0 for a plain roll. */
    static boolean saves(LivingEntity t, int dc, int mode) {
        double c = Wave0Rules.saveChance(dc, saveMod(t));
        if (mode > 0) c = Wave0Rules.advantage(c);
        else if (mode < 0) c = Wave0Rules.disadvantage(c);
        return Wave0Rules.passes(dice(t), c);
    }

    /**
     * A condition the target saves against: first save, then it takes hold; at the end of every second the target saves again and the
     * condition ends on a success (or after {@code rounds}). {@code apply} is called each round it holds, so it can refresh a short effect.
     */
    static void condition(LivingEntity t, int dc, int rounds, int firstSave, Consumer<LivingEntity> apply, Consumer<LivingEntity> end) {
        if (!(t.level() instanceof ServerLevel sl) || !t.isAlive()) return;
        if (saves(t, dc, firstSave)) return;
        apply.accept(t);
        next(sl, t, dc, Math.max(0, rounds - 1), apply, end);
    }

    private static void next(ServerLevel sl, LivingEntity t, int dc, int left, Consumer<LivingEntity> apply, Consumer<LivingEntity> end) {
        SpellRuntime.later(sl, 20, () -> {
            if (!t.isAlive() || t.isRemoved()) return;
            if (left <= 0 || saves(t, dc, 0)) { end.accept(t); return; }
            apply.accept(t);
            next(sl, t, dc, left - 1, apply, end);
        });
    }

    // ---------------------------------------------------------------- the conditions the cards name
    /** Restrained: rooted to the spot (Slowness VII, the navigation stopped) while the ivy holds. */
    static void restrain(LivingEntity t, int baseTicks, int dc, int firstSave) {
        int len = BalanceLaw.controlTicks(t, baseTicks);
        condition(t, dc, Wave0Rules.rounds(len), firstSave, x -> {
            x.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 6));
            Fx.addEffect(x, "tensura:webbed", 24, 0);                                                // Tensura's own binding, where Tensura is there
            if (x instanceof Mob m) m.getNavigation().stop();
        }, x -> {
            MobEffectInstance cur = x.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (cur != null && cur.getAmplifier() >= 6) x.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        });
    }

    static void blind(LivingEntity t, int baseTicks, int dc) {
        int len = BalanceLaw.controlTicks(t, baseTicks);
        condition(t, dc, Wave0Rules.rounds(len), 0, x -> {
            x.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 24, 0));
            Fx.addEffect(x, "tensura:flashed_blindness", 24, 0);
        }, x -> x.removeEffect(MobEffects.BLINDNESS));
    }

    /** Disadvantage on attack rolls and ability checks: Weakness II and Mining Fatigue. */
    static void sicken(LivingEntity t, int baseTicks, int dc) {
        int len = Math.min(baseTicks, BalanceLaw.isBoss(t) ? 40 : 160);
        condition(t, dc, Wave0Rules.rounds(len), 0, x -> {
            x.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 24, 1));
            x.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24, 0));
        }, x -> {
            x.removeEffect(MobEffects.WEAKNESS);
            x.removeEffect(MobEffects.DIG_SLOWDOWN);
        });
    }

    /** Frightened of the wielder: a mob drops its target and runs, a player is slowed. Bosses are only weakened. */
    static void frighten(ServerPlayer p, LivingEntity t, int baseTicks, int dc, int firstSave) {
        if (BalanceLaw.isBoss(t)) { t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0)); return; }
        int len = BalanceLaw.controlTicks(t, baseTicks);
        condition(t, dc, Wave0Rules.rounds(len), firstSave, x -> {
            if (x instanceof Mob m) {
                m.setTarget(null);
                Vec3 away = x.position().subtract(p.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) {
                    Vec3 to = x.position().add(away.normalize().scale(10));
                    m.getNavigation().moveTo(to.x, to.y, to.z, 1.4);
                }
            }
            x.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, x instanceof Player ? 1 : 0));
            x.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 24, 0));
            Fx.addEffect(x, "tensura:fear", 24, 0);
        }, x -> { });
    }

    static final String CHARM_BY = "nusmp_w0_charm_by", CHARM_UNTIL = "nusmp_w0_charm_until";

    /** Charmed by the wielder: for the time it lasts the creature will not turn on them (see Wave0Hooks). */
    static void charm(ServerPlayer p, LivingEntity t, int baseTicks, int dc, int firstSave) {
        if (BalanceLaw.isBoss(t)) return;
        int len = BalanceLaw.controlTicks(t, baseTicks);
        condition(t, dc, Wave0Rules.rounds(len), firstSave, x -> {
            CompoundTag d = x.getPersistentData();
            d.putUUID(CHARM_BY, p.getUUID());
            d.putLong(CHARM_UNTIL, x.level().getGameTime() + 30);
            if (x instanceof Mob m && m.getTarget() == p) m.setTarget(null);
        }, x -> x.getPersistentData().putLong(CHARM_UNTIL, 0));
    }

    // ---------------------------------------------------------------- damage
    private static boolean busy;

    /** True while one of this wave's own extra blows is being dealt (so it does not trigger the hooks again). */
    static boolean busy() { return busy; }

    /** An extra blow on top of a hit: through the core's damage rules, the hurt timer cleared, and no second shove. */
    static void extra(ServerPlayer p, LivingEntity t, float amount, String damageTypeId) {
        if (busy || amount <= 0f || !t.isAlive()) return;
        busy = true;
        try {
            Vec3 v = t.getDeltaMovement();
            t.invulnerableTime = 0;
            Fx.damage(p, t, amount, damageTypeId);
            t.setDeltaMovement(v);
        } finally {
            busy = false;
        }
    }

    // ---------------------------------------------------------------- messages and effects
    static void note(ServerPlayer p, String text, ChatFormatting colour) {
        p.displayClientMessage(Component.literal(text).withStyle(colour), true);
    }

    static void send(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int argb, int ticks, float power) {
        VfxSpawn.send(sl, shape, from, to, argb, ticks, power);
    }

    /** May be laid hands on: the wielder, allies, any player, villagers, player-made golems, anyone's pet. */
    static boolean healable(ServerPlayer p, LivingEntity x) {
        if (x == p || x.isAlliedTo(p) || x instanceof Player || x instanceof AbstractVillager) return true;
        if (x instanceof IronGolem g && g.isPlayerCreated()) return true;
        return x instanceof OwnableEntity o && o.getOwner() instanceof Player;
    }

    private static final String[] CURSES = {"poison", "curse", "wither", "hex", "infection", "corrosion", "plague", "disease", "unluck", "rot", "misfortune", "doom"};

    static boolean hasHarmful(LivingEntity a) {
        for (MobEffectInstance e : a.getActiveEffects()) if (!e.getEffect().value().isBeneficial()) return true;
        return false;
    }

    /** Takes off one affliction (the first harmful effect); true if there was one. */
    static boolean cleanseOne(LivingEntity a) {
        for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) {
            if (!e.getEffect().value().isBeneficial()) { a.removeEffect(e.getEffect()); return true; }
        }
        return false;
    }

    /** Takes off every curse and poison, and the mod's own misfortune and sealing; true if anything was lifted. */
    static boolean cleanseCurses(LivingEntity a) {
        boolean any = false;
        for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) {
            if (e.getEffect().value().isBeneficial()) continue;
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value());
            String path = id == null ? "" : id.getPath();
            for (String word : CURSES) {
                if (path.contains(word)) { a.removeEffect(e.getEffect()); any = true; break; }
            }
        }
        long now = a.level().getGameTime();
        CompoundTag pd = a.getPersistentData();
        for (String flag : new String[]{"nusmp_misfortune_until", "nusmp_sealed_until"}) {
            if (pd.getLong(flag) > now) { pd.putLong(flag, 0); any = true; }
        }
        return any;
    }

    /** Hands an item by registry id to the player (to the ground if the bag is full); a missing item (an absent mod) is skipped. */
    static void give(ServerPlayer p, String itemId, int count) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || count <= 0) return;
        var opt = BuiltInRegistries.ITEM.getOptional(id);
        if (opt.isEmpty()) return;
        ItemStack s = new ItemStack(opt.get(), count);
        if (!p.getInventory().add(s)) p.drop(s, false);
    }
}
