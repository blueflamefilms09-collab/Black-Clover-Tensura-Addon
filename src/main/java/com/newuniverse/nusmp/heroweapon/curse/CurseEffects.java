package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: this is where the seven sins take what they take, and the owner's rule is that it is bounded and never lethal. Wrath burns the wielder's blood each second
 * but never below a floor that is at least one heart; Lust drags the wielder toward the nearest foe (sneaking resists it, and it never pulls over a drop, into
 * lava, fire or a wall); Envy takes one of the wielder's techniques for a few seconds and eats one of their potion boons; Greed weighs them down with the
 * treasure they carry and charges extra for techniques; Gluttony empties their stomach, but stops at a floor so it can never starve them, and feeds on kills;
 * Sloth slows their body and holds the next technique back. Pride's downside (help does not heal) lives in the heal handler and its narrowed sight on the client.
 * The permanent tax of the Embrace is also applied here, as a max-health modifier that never leaves less than the configured hearts. Every number comes from
 * {@link SinMath} and {@link CurseTuning} (tested); this class reads the world, asks them, and applies the answer. Attribute modifiers are TRANSIENT (they are
 * projections of the saved curse, re-applied after a login or a respawn, and gone with the bond). The only memory is the Envy and Sloth timers, a server-thread
 * cache cleared when a player leaves and when the server stops.
 */
final class CurseEffects {
    private CurseEffects() {}

    static final ResourceLocation TAX_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_curse_tax");
    static final ResourceLocation BURDEN_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_curse_burden");
    static final ResourceLocation TORPOR_SPEED_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_curse_torpor_speed");
    static final ResourceLocation TORPOR_ATTACK_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_curse_torpor_attack");

    /** What Envy took and until when, and when it takes again. */
    private static final class Envy {
        String lost = "";
        long lostUntil;
        long nextAt;
    }

    private static final Map<UUID, Envy> ENVY = new HashMap<>();
    private static final Map<UUID, Long> LAST_TECHNIQUE = new HashMap<>();

    /** The colour of a sin as an effect colour (0xAARRGGBB). */
    static int argb(Sin sin) { return 0xFF000000 | sin.rgb(); }

    // ------------------------------------------------------------------ the tax and the modifiers
    /**
     * Makes the wielder's maximum health match the tax: {@code taxHearts} hearts off, but never leaving less than {@code minHearts} hearts. {@code force} recomputes
     * from scratch; without it a modifier that is already right is left alone (this runs every few seconds). Health is lowered to the new maximum if needed.
     */
    static void ensureTax(ServerPlayer p, int taxHearts, int minHearts, boolean force) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(TAX_ID);
        if (taxHearts <= 0) {
            if (cur != null) {
                inst.removeModifier(TAX_ID);
                clampHealth(p);
            }
            return;
        }
        if (!force && cur != null) {
            double estimate = CurseMachine.appliedTaxHp(taxHearts, inst.getValue() - cur.amount(), minHearts);
            if (Math.abs(cur.amount() + estimate) < 1.0e-6) return;
        }
        if (cur != null) inst.removeModifier(TAX_ID);
        double hp = CurseMachine.appliedTaxHp(taxHearts, inst.getValue(), minHearts);
        if (hp > 0.0) inst.addTransientModifier(new AttributeModifier(TAX_ID, -hp, AttributeModifier.Operation.ADD_VALUE));
        clampHealth(p);
    }

    private static void clampHealth(ServerPlayer p) {
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Sets or removes one transient modifier, leaving it alone when it is already the right size. */
    private static void setModifier(AttributeInstance inst, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeModifier cur = inst.getModifier(id);
        if (Math.abs(amount) < 1.0e-9) {
            if (cur != null) inst.removeModifier(id);
            return;
        }
        if (cur != null && cur.operation() == op && Math.abs(cur.amount() - amount) < 1.0e-6) return;
        if (cur != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    /** The speed modifiers of Greed (treasure) and Sloth (torpor) for the sin worn now (null = none): set when worn, removed when not. */
    static void ensureSinModifiers(ServerPlayer p, Sin worn, CurseTuning t) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED), hit = p.getAttribute(Attributes.ATTACK_SPEED);
        double burden = worn == Sin.GREED ? SinMath.burden(treasureScore(p), t.greedBurdenFull, t.greedMaxSlow, t.intensity(Sin.GREED)) : 0.0;
        double slow = worn == Sin.SLOTH ? SinMath.slow(t.slothSlow, t.intensity(Sin.SLOTH)) : 0.0;
        double slowHit = worn == Sin.SLOTH ? SinMath.slow(t.slothAttackSlow, t.intensity(Sin.SLOTH)) : 0.0;
        if (speed != null) {
            setModifier(speed, BURDEN_ID, -burden, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            setModifier(speed, TORPOR_SPEED_ID, -slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        if (hit != null) setModifier(hit, TORPOR_ATTACK_ID, -slowHit, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Removes every modifier the curse puts on a player (the weapon was released, the system was switched off). */
    static void clearModifiers(ServerPlayer p) {
        AttributeInstance health = p.getAttribute(Attributes.MAX_HEALTH), speed = p.getAttribute(Attributes.MOVEMENT_SPEED), hit = p.getAttribute(Attributes.ATTACK_SPEED);
        if (health != null && health.hasModifier(TAX_ID)) {
            health.removeModifier(TAX_ID);
            clampHealth(p);
        }
        if (speed != null) {
            speed.removeModifier(BURDEN_ID);
            speed.removeModifier(TORPOR_SPEED_ID);
        }
        if (hit != null) hit.removeModifier(TORPOR_ATTACK_ID);
    }

    /** The weight of the treasure in the inventory (an ingot or a gem 1, a block 9 ...), see {@link SinMath#treasureWeight}. */
    static int treasureScore(ServerPlayer p) {
        long score = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            int w = SinMath.treasureWeight(BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
            if (w > 0) score += (long) w * st.getCount();
        }
        return (int) Math.min(Integer.MAX_VALUE, score);
    }

    // ------------------------------------------------------------------ once a second
    /** The downside of the sin worn, for this second. */
    static void second(ServerPlayer p, HeroForm form, Sin sin, CurseTuning t, long now) {
        if (sin == null) return;
        double k = t.intensity(sin);
        if (k <= 0.0) return;
        switch (sin) {
            case WRATH -> wrath(p, t, k, now);
            case ENVY -> envy(p, form, t, now);
            case GLUTTONY -> gluttony(p, t, k);
            default -> { }
        }
    }

    private static void wrath(ServerPlayer p, CurseTuning t, double k, long now) {
        double max = p.getMaxHealth(), hp = p.getHealth();
        double take = SinMath.drain(hp, SinMath.wrathFloor(max, t.wrathFloorShare), SinMath.wrathWant(max, t.wrathDrainShare, t.wrathDrainMin, k));
        if (take <= 0.0) return;
        p.setHealth((float) (hp - take));
        if ((now / 20L) % 3L == 0L) {
            ServerLevel level = p.serverLevel();
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.PLAYERS, 0.4f, 0.9f);
            Vec3 at = p.position().add(0.0, 0.2, 0.0);
            VfxSpawn.send(level, VfxShape.FIRE_BURST, at, at, argb(Sin.WRATH), 16, 0.7f);
        }
    }

    private static void gluttony(ServerPlayer p, CurseTuning t, double k) {
        float add = SinMath.foodExhaust(p.getFoodData().getFoodLevel(), t.gluttonyFoodFloor, t.gluttonyExhaust, k);
        if (add > 0f) p.causeFoodExhaustion(add);
    }

    /** The techniques the form lists, once each, in the order of the input slots. */
    private static List<String> techniqueIds(HeroForm form) {
        List<String> out = new ArrayList<>();
        for (HeroTrigger trigger : HeroTrigger.values()) {
            String id = form.skill(trigger);
            if (id != null && !id.isEmpty() && !out.contains(id)) out.add(id);
        }
        return out;
    }

    private static void envy(ServerPlayer p, HeroForm form, CurseTuning t, long now) {
        Envy e = ENVY.computeIfAbsent(p.getUUID(), id -> new Envy());
        long interval = Math.max(60L, t.envyIntervalSeconds * 20L);
        if (e.nextAt == 0L) {
            e.nextAt = now + interval;
            return;
        }
        if (now < e.nextAt) return;
        e.nextAt = now + interval;
        List<String> ids = techniqueIds(form);
        int pick = SinMath.envyPick(ids.size(), p.getRandom().nextDouble());
        if (pick >= 0) {
            e.lost = ids.get(pick);
            e.lostUntil = now + Math.max(20L, t.envyLockSeconds * 20L);
            HeroSkill skill = HeroLogics.skill(e.lost);
            CurseMessages.bar(p, CurseMessages.colored(Sin.ENVY, CurseMessages.tr(CurseText.ENVY_TAKE, skill != null ? skill.name() : e.lost, t.envyLockSeconds)));
            Vec3 at = p.position().add(0.0, 1.0, 0.0);
            VfxSpawn.send(p.serverLevel(), VfxShape.MIRROR_SHATTER, at, at, argb(Sin.ENVY), 20, 0.7f);
        }
        stripBoon(p);
    }

    /** Envy eats one vanilla potion boon of the wielder (never another mod's effect, which may be a skill's own state). */
    private static void stripBoon(ServerPlayer p) {
        List<MobEffectInstance> boons = new ArrayList<>();
        for (MobEffectInstance m : p.getActiveEffects()) {
            MobEffect eff = m.getEffect().value();
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(eff);
            if (eff.isBeneficial() && id != null && "minecraft".equals(id.getNamespace())) boons.add(m);
        }
        if (boons.isEmpty()) return;
        MobEffectInstance gone = boons.get(p.getRandom().nextInt(boons.size()));
        Component name = gone.getEffect().value().getDisplayName();
        p.removeEffect(gone.getEffect());
        CurseMessages.chat(p, CurseMessages.colored(Sin.ENVY, CurseMessages.tr(CurseText.ENVY_STRIP, name)));
    }

    // ------------------------------------------------------------------ every half second: Lust
    /** Lust drags the wielder a step toward the nearest foe, unless they sneak, cannot be pulled safely, or the foe is out of range or already in reach. */
    static void lust(ServerPlayer p, CurseTuning t) {
        double k = t.intensity(Sin.LUST);
        if (k <= 0.0 || p.isShiftKeyDown() || !p.onGround() || p.isPassenger() || p.isFallFlying() || p.getAbilities().flying || p.isInWater() || p.isInLava() || p.onClimbable()) return;
        ServerLevel level = p.serverLevel();
        AABB box = p.getBoundingBox().inflate(t.lustRange, 3.0, t.lustRange);
        List<Mob> foes = level.getEntitiesOfClass(Mob.class, box, m -> isFoe(p, m));
        Mob nearest = OverrunMath.nearest(foes, m -> p.distanceToSqr(m), t.lustRange * t.lustRange);
        if (nearest == null) return;
        double[] v = SinMath.pull(p.getX(), p.getZ(), nearest.getX(), nearest.getZ(), t.lustMinDistance, t.lustRange, t.lustPull * k);
        if (v == null || !safeStep(level, p.position(), v[0], v[1])) return;
        Vec3 m = p.getDeltaMovement();
        double nx = m.x + v[0], nz = m.z + v[1];
        if (Math.hypot(nx, nz) > 0.45) return;
        p.setDeltaMovement(nx, m.y, nz);
        p.hurtMarked = true;
    }

    /** A living thing Lust may drag the wielder toward: a hostile mob, or one that has set its eyes on them; never an ally, a pet, a servant or a mob with no mind. */
    private static boolean isFoe(ServerPlayer p, Mob m) {
        if (!m.isAlive() || m.isNoAi() || m.isAlliedTo(p)) return false;
        if (m instanceof OwnableEntity pet && pet.getOwnerUUID() != null) return false;
        if (m.getTags().contains("nusmp_servant")) return false;
        return m instanceof Enemy || m.getType().getCategory() == MobCategory.MONSTER || m.getTarget() == p;
    }

    /** True when stepping about 1.2 blocks along (vx, vz) lands on ground within two blocks below, with no wall at the feet and nothing harmful in the way. */
    static boolean safeStep(ServerLevel level, Vec3 from, double vx, double vz) {
        double len = Math.hypot(vx, vz);
        if (len < 1.0e-6) return false;
        BlockPos feet = BlockPos.containing(from.x + vx / len * 1.2, from.y, from.z + vz / len * 1.2);
        for (int i = 0; i <= 2; i++) {
            BlockPos pos = feet.below(i);
            BlockState st = level.getBlockState(pos);
            if (isHarmful(st)) return false;
            boolean solid = !st.getCollisionShape(level, pos).isEmpty();
            if (i == 0 && solid) return false;
            if (i > 0 && solid) return true;
        }
        return false;
    }

    private static boolean isHarmful(BlockState st) {
        return st.is(Blocks.LAVA) || st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE) || st.is(Blocks.CACTUS) || st.is(Blocks.MAGMA_BLOCK) || st.is(Blocks.SWEET_BERRY_BUSH)
                || st.is(Blocks.CAMPFIRE) || st.is(Blocks.SOUL_CAMPFIRE) || st.is(Blocks.WITHER_ROSE) || st.is(Blocks.POWDER_SNOW) || st.getFluidState().is(FluidTags.LAVA);
    }

    // ------------------------------------------------------------------ the dispatcher's hooks
    /** A technique was cast with a curse form worn: Greed charges its surcharge, Sloth starts its lock. */
    static void onTechnique(ServerPlayer p, Sin sin, HeroSkill skill, CurseTuning t, long now) {
        LAST_TECHNIQUE.put(p.getUUID(), now);
        if (sin != Sin.GREED) return;
        double extra = skill.magoiShare() * t.greedSurcharge * t.intensity(Sin.GREED);
        if (extra <= 0.0) return;
        HeroCtx c = HeroCtx.of(p);
        if (c != null) c.pay(extra);
    }

    /** The reason the curse refuses this technique now, or null: the weapon holds the wielder, Envy took it, or Sloth holds the hand back. */
    static Component refuses(ServerPlayer p, HeroSkill skill, Sin worn, boolean overrun, CurseTuning t, long now) {
        if (overrun && t.overrunBlocksTechniques) return CurseMessages.tr(CurseText.OVERRUN_REFUSE);
        if (worn == Sin.SLOTH && t.intensity(Sin.SLOTH) > 0.0 && SinMath.slothLocked(now, LAST_TECHNIQUE.getOrDefault(p.getUUID(), 0L), t.slothLockTicks)) {
            return CurseMessages.colored(Sin.SLOTH, CurseMessages.tr(CurseText.SLOTH_REFUSE));
        }
        if (worn == Sin.ENVY && t.intensity(Sin.ENVY) > 0.0) {
            Envy e = ENVY.get(p.getUUID());
            if (e != null && now < e.lostUntil && skill.id().equals(e.lost)) return CurseMessages.colored(Sin.ENVY, CurseMessages.tr(CurseText.ENVY_REFUSE, skill.name()));
        }
        return null;
    }

    /** The wielder killed something with Gluttony worn: a little food and a little health come back (the healing law caps the heal). */
    static void gluttonyFeed(ServerPlayer p, CurseTuning t) {
        double k = t.intensity(Sin.GLUTTONY);
        if (k <= 0.0) return;
        int food = SinMath.feed(p.getFoodData().getFoodLevel(), (int) Math.round(t.gluttonyKillFood * k));
        if (food > 0) p.getFoodData().eat(food, 0.2f);
        float heal = (float) (p.getMaxHealth() * t.gluttonyKillHeal * k);
        if (heal > 0f) BalanceLaw.heal(p, heal);
    }

    // ------------------------------------------------------------------ memory
    /** Forgets the timers of a player who left. */
    static void forget(UUID id) {
        ENVY.remove(id);
        LAST_TECHNIQUE.remove(id);
    }

    /** Forgets every timer (the server stopped). */
    static void clear() {
        ENVY.clear();
        LAST_TECHNIQUE.clear();
    }
}
