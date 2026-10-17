package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathMath;
import com.newuniverse.nusmp.entity.zogratis.SpellSeal;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Amphisbaena is the Monster Rex of the 27th floor, the Water City: a two-headed white water dragon whose blue head breathes a flame that burns through
 * steel and whose red head breathes a mist that dissipates magic (see {@code docs/arc/amphisbaena.md}). The fight is built on that: the red head is the thing
 * to cut. When it rears up to breathe the mist, the blows that land on the boss wear it down (physical blows once, anti-magic blows twice, spells not at all,
 * {@link HeadCut}), and once it is off the mist moves are gone, so the red mist (a cloud that seals the grimoire of anyone in it) is a race against the fight.
 * The fight itself tears the head off at {@value HeadCut#FORCE_CUT_POWER}% power, so it never depends on the mechanic being found. The Water City Call brings
 * monsters, the Twin Calamity is both heads at once, and a Leveled form adds the Blue Flame Cyclone. It changes no block, heals nothing, applies no hard control.
 */
public class AmphisbaenaBoss extends DanMachiBoss {
    public static final ArcCharacter CHARACTER = ArcRoster.register(DanMachiRoster.AMPHISBAENA);

    public static final DeferredHolder<EntityType<?>, EntityType<AmphisbaenaBoss>> TYPE = ArcEntities.boss("amphisbaena", AmphisbaenaBoss::new, 1.7f, 4.09f,
            new ArcLook("amphisbaena", 4.09f, 2.17f, 3.96f, 0.82f, 3.79f, 1.58f, true), AmphisbaenaBoss::createAttributes);

    private static final DmFx.Style STYLE = new DmFx.Style(DmFx.Look.TIDE, 0x4AB0FF, 0x9AD8FF);
    /** The red of the mist (0xRRGGBB), and how long a cloud of it stands. */
    private static final int MIST_RGB = 0xD04040, MIST_TICKS = 110;
    private static final String KEY_CUT = "DmHeadCut";

    private final HeadCut heads = new HeadCut();

    public AmphisbaenaBoss(EntityType<AmphisbaenaBoss> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(1900, 20, 0.27, 8, 3); }

    @Override public ArcCharacter character() { return CHARACTER; }

    @Override protected List<DmSpec> table() { return AmphisbaenaTable.specs(); }

    @Override protected int baseLevel() { return 5; }

    @Override protected DmFx.Style style() { return STYLE; }

    @Override protected double preferredRange(double suggested) { return Math.max(4.0, Math.min(12.0, suggested)); }

    /** The mist moves are gone once the red head is cut; everything else follows the base rule (a leveled row only after the Level-Up). */
    @Override protected boolean usable(DmSpec s) { return super.usable(s) && !(heads.cut() && AmphisbaenaTable.needsRedHead(s.id())); }

    /** True once the red head is off. */
    public boolean redHeadCut() { return heads.cut(); }

    // ================================================================ sounds
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDER_DRAGON_HURT; }

    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }

    // ================================================================ the red head
    /** While the red head is reared, the blows count against it: physical once, anti-magic twice, spells not at all. */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        float a = super.filterDamage(source, amount);
        if (a <= 0f || heads.cut()) return a;
        LivingEntity who = DmBlows.attacker(source);
        if (heads.onHit(tickCount, DmBlows.headKind(source), a, getMaxHealth())) {
            cutHead();
        } else if (heads.windowOpen(tickCount) && who instanceof ServerPlayer sp && tickCount % 6 == 0) {
            int percent = (int) Math.min(99, Math.round(100.0 * heads.worn() / (HeadCut.CUT_FRACTION * getMaxHealth())));
            sp.displayClientMessage(Component.literal("The red head reels: " + percent + "%. Blows count, spells do not."), true);
        }
        return a;
    }

    private void cutHead() {
        if (level() instanceof ServerLevel sl) {
            Vec3 head = position().add(0, getBbHeight() * 0.9, 0);
            DmFx.burst(this, STYLE, head, 40);
            VfxSpawn.send(sl, VfxShape.BOSS_TIDE_SPOUT, position(), position().add(0, 1, 0), 0xFF000000 | MIST_RGB, 30, 3.0f);
            sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_HURT, SoundSource.HOSTILE, 3.0f, 0.6f);
        }
        announce(DmText.headCut(bossName()));
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        super.onPowerChanged(old, now);
        if (HeadCut.mustCut(now) && heads.forceCut()) cutHead();
    }

    @Override
    protected void perSecond(long now) {
        if (HeadCut.mustCut(power()) && heads.forceCut()) cutHead();     // a reloaded fight keeps its power but not its memory
    }

    @Override protected void resetMechanics() { heads.reset(); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(KEY_CUT, heads.cut());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean(KEY_CUT)) heads.forceCut();
    }

    // ================================================================ its own moves
    @Override
    protected int runCustom(DmSpec s, LivingEntity target) {
        return switch (s.id()) {
            case AmphisbaenaTable.TWIN_BITE -> twinBite(s, target);
            case AmphisbaenaTable.RED_MIST_BREATH -> redMist(s, target);
            case AmphisbaenaTable.WATER_CITY_CALL -> waterCityCall();
            case AmphisbaenaTable.SCORCH_FIELD -> scorchField(s, target);
            case AmphisbaenaTable.TWIN_CALAMITY -> calamity(s, target);
            default -> 0;
        };
    }

    @Override
    protected void tellCustom(DmSpec s, LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        boolean first = TellClock.first(left, total);
        Vec3 at = position();
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        switch (s.id()) {
            case AmphisbaenaTable.TWIN_BITE -> DmFx.coneTell(this, STYLE, at, a.fx, a.fz, 55.0, s.reach(), first ? total : 0);
            case AmphisbaenaTable.RED_MIST_BREATH -> {
                if (first) {
                    heads.open(tickCount, total + HeadCut.LINGER_TICKS);
                    announce("The red head rears up to breathe the mist! Strike it with blows, not spells.");
                }
                Vec3 spot = new Vec3(a.tx, a.ty, a.tz);
                if (first) DmFx.spotTell(this, STYLE, spot, s.reach(), total);
                else Tells.ring(sl, spot, s.reach(), MIST_RGB, 1.2f, 28);
                DmFx.gather(this, new DmFx.Style(DmFx.Look.TIDE, MIST_RGB, MIST_RGB), at.add(0, getBbHeight() * 0.9, 0), 0.8, 8);
            }
            case AmphisbaenaTable.WATER_CITY_CALL -> {
                DmShapes.groundRing(this, STYLE, at, 8.0, first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.8, 0), 1.2, 8);
            }
            case AmphisbaenaTable.SCORCH_FIELD -> {
                Vec3 spot = new Vec3(a.tx, a.ty, a.tz);
                if (first) DmFx.spotTell(this, STYLE, spot, s.reach(), total);
                else Tells.ring(sl, spot, s.reach(), STYLE.tint(this), 1.2f, 28);
            }
            case AmphisbaenaTable.TWIN_CALAMITY -> {
                DmShapes.groundRing(this, STYLE, at, s.reach(), first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.9, 0), 1.6, 12);
                if (!heads.cut()) DmFx.gather(this, new DmFx.Style(DmFx.Look.TIDE, MIST_RGB, MIST_RGB), at.add(0, getBbHeight() * 0.9, 0), 1.6, 12);
            }
            default -> super.tellCustom(s, target, left, total);
        }
    }

    /** The Twin Bite: two bites six ticks apart, one after the other, each a cone in front of the heads. */
    private int twinBite(DmSpec s, LivingEntity target) {
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        var budget = DmShapes.budget();
        bite(s, a, budget);
        later(6, DmSafe.task("second bite", () -> {
            if (isAlive()) bite(s, a, budget);
        }));
        return 8;
    }

    private void bite(DmSpec s, DmShapes.Aim a, GoliathMath.DamageBudget budget) {
        Vec3 at = position();
        DmFx.coneStrike(this, STYLE, at.add(0, 0.2, 0), a.fx, a.fz, s.reach());
        if (level() instanceof ServerLevel sl) DmShapes.sound(sl, at, SoundEvents.PLAYER_ATTACK_CRIT, 1.6f, 0.5f);
        DmShapes.coneHit(this, at, a.fx, a.fz, s.reach(), 55.0, (float) s.damage(), 0.8, 0.0, budget, false);
    }

    /**
     * The Red Mist Breath: a lance of mist to where the target stood when the wind-up began, and a cloud there. While the cloud stands, the grimoire of everyone in
     * it is sealed ({@value SpellSeal#SEALED_FLAG}), unless they are users of anti-magic. It ends when the red head is cut.
     */
    private int redMist(DmSpec s, LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        Vec3 spot = ZogratisBoss.ground(sl, new Vec3(a.tx, a.ty, a.tz));
        Vec3 mouth = position().add(0, getBbHeight() * 0.9, 0);
        VfxSpawn.send(sl, VfxShape.POISON_BREATH, mouth, spot.add(0, 1, 0), 0xFF000000 | MIST_RGB, 28, 1.4f);
        VfxSpawn.send(sl, VfxShape.MIST_MAGIC, spot, spot.add(0, 2.5, 0), 0xFF000000 | MIST_RGB, MIST_TICKS, (float) s.reach());
        DmShapes.sound(sl, mouth, SoundEvents.ENDER_DRAGON_GROWL, 1.6f, 0.8f);
        Set<Integer> told = new HashSet<>();
        double radius = s.reach();
        SpellRuntime.zone(sl, MIST_TICKS, 2, DmSafe.step("red mist", age -> {
            if (!isAlive() || isRemoved() || heads.cut()) return;
            long now = sl.getGameTime();
            if (age % 6 == 0) Tells.gather(sl, spot.add(0, 1.2, 0), radius * 0.6, MIST_RGB, 10);
            for (LivingEntity e : foesAround(spot, radius + 1.0)) {
                if (!(e instanceof Player p) || Math.hypot(p.getX() - spot.x, p.getZ() - spot.z) > radius + p.getBbWidth() / 2.0) continue;
                if (AntiMagic.isUser(p)) continue;
                p.getPersistentData().putLong(SpellSeal.SEALED_FLAG, now + 6);
                if (p instanceof ServerPlayer sp && told.add(p.getId())) sp.displayClientMessage(Component.literal("The red mist eats your magic. Leave it, or cut the red head."), true);
            }
        }));
        return 10;
    }

    /** The Water City Call: the monsters of the drowned city come, as many as the cap of the power (and of the config) leaves room for. */
    private int waterCityCall() {
        if (!(level() instanceof ServerLevel sl)) return 0;
        int cap = AddPlan.cap(power(), DanMachiConfig.maxAdds());
        int n = AddPlan.toSummon(cap, DmAdds.alive(this), tuning().castSlots());
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.5f, 0.9f);
        if (DmAdds.summon(this, DmAdds.Kind.WATER, n, position(), 8.0, STYLE) > 0) announce("The Water City answers: its monsters rise from the flood!");
        return 10;
    }

    /** The Scorch Field: the blue flame burns on the spot where the target stood, for four seconds; it ignores armour. */
    private int scorchField(DmSpec s, LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        Vec3 spot = ZogratisBoss.ground(sl, new Vec3(a.tx, a.ty, a.tz));
        int ticks = 80;
        VfxSpawn.send(sl, VfxShape.FIRE_WILD, spot, spot.add(0, 1, 0), 0xFF000000 | STYLE.rgb(), ticks, (float) s.reach());
        DmShapes.sound(sl, spot, SoundEvents.BLAZE_SHOOT, 1.6f, 0.5f);
        var budget = DmShapes.budget();
        SpellRuntime.zone(sl, ticks, 1, DmSafe.step("scorch field", age -> {
            if (!isAlive() || isRemoved()) return;
            if (age % 10 != 5) return;
            for (LivingEntity e : foesAround(spot, s.reach() + 1.5)) {
                if (Math.hypot(e.getX() - spot.x, e.getZ() - spot.z) > s.reach() + e.getBbWidth() / 2.0 || !DmShapes.inBand(e.getY() - spot.y)) continue;
                DmShapes.land(this, e, (float) s.damage(), 0.0, 0.0, 0.0, 0.0, budget, true);
            }
        }));
        return ticks;
    }

    /**
     * The Twin Calamity, its ultimate: both heads breathe at once, a pair of beams that turn all the way round the room, and (while the red head is on) the mist
     * seals the grimoire of everyone within its reach for as long as it lasts; then a ring of water runs out. With the red head cut it is the blue flame alone.
     */
    private int calamity(DmSpec s, LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        boolean red = !heads.cut();
        double yaw = DmGeometry.yawOf(a.fx, a.fz);
        int ticks = DmShapes.sweep(this, yaw, 360.0, 14.0, s.reach(), 12, (float) s.damage(), red ? 2 : 1, true);
        if (red) {
            Vec3 centre = position();
            double reach = s.reach();
            SpellRuntime.zone(sl, ticks, 2, DmSafe.step("calamity mist", age -> {
                if (!isAlive() || isRemoved() || heads.cut()) return;
                long now = sl.getGameTime();
                for (LivingEntity e : foesAround(centre, reach)) {
                    if (e instanceof Player p && !AntiMagic.isUser(p)) p.getPersistentData().putLong(SpellSeal.SEALED_FLAG, now + 6);
                }
            }));
        }
        later(ticks, DmSafe.task("calamity ring", () -> {
            if (isAlive()) DmShapes.waves(this, position(), s.reach(), 9f, 1.4, 1, 0);
        }));
        return ticks + 24;
    }
}
