package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashSet;
import java.util.List;

/**
 * Why: the Black Goliath is the Dungeon's own answer to a god's power (the irregular Goliath of the 17th and 18th floors, a stone giant as black as basalt, faster
 * and far stronger than the Goliath of the floor boss, see {@code docs/arc/black_goliath.md}). It is the second monster of the saga and the one that teaches that
 * a blow can be the wrong kind of blow: its Black Hide halves every plain swing and arrow ({@link HideState}), so a party must hit hard, or hit with spells, and
 * three heavy blows within six seconds break the hide for a while. It calls monsters with a roar, frightens with another, cracks the floor into spokes with
 * gaps to stand in, and ends the fight with the Dungeon's Wrath. When the party is strong enough the Gates Level it up and the Titan Slam joins. It does not
 * rebuild the stone Goliath of the floors (that boss is its own class); it changes no block and heals nothing.
 */
public class BlackGoliathBoss extends DanMachiBoss {
    public static final ArcCharacter CHARACTER = ArcRoster.register(DanMachiRoster.BLACK_GOLIATH);

    public static final DeferredHolder<EntityType<?>, EntityType<BlackGoliathBoss>> TYPE = ArcEntities.boss("black_goliath", BlackGoliathBoss::new, 1.7f, 3.91f,
            new ArcLook("black_goliath", 3.91f, 2.05f, 3.73f, 0.86f, 1.73f, 1.50f, true), BlackGoliathBoss::createAttributes);

    private static final DmFx.Style STYLE = new DmFx.Style(DmFx.Look.STONE, 0xFF7A3A, 0xFF4A1E);
    /** Ticks between two rings of the Dungeon's Wrath, and the warning the spokes get before they split the floor. */
    private static final int WRATH_GAP = 14, SPOKE_WARN = 22;

    private final HideState hide = new HideState();
    private long lastGlance = -1000;

    public BlackGoliathBoss(EntityType<BlackGoliathBoss> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(1800, 24, 0.26, 12, 4); }

    @Override public ArcCharacter character() { return CHARACTER; }

    @Override protected List<DmSpec> table() { return BlackGoliathTable.specs(); }

    @Override protected int baseLevel() { return 5; }

    @Override protected DmFx.Style style() { return STYLE; }

    /** A giant fights from a few blocks off: close enough for its fist, never on top of whoever it fights. */
    @Override protected double preferredRange(double suggested) { return Math.max(3.0, Math.min(9.0, suggested)); }

    // ================================================================ sounds
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WARDEN_HURT; }

    @Override protected SoundEvent getDeathSound() { return SoundEvents.WARDEN_DEATH; }

    // ================================================================ the Black Hide
    /** A plain blow does half while the hide holds; three heavy ones within six seconds break it; a spell or a skill is never cut. */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        float a = super.filterDamage(source, amount);
        if (a <= 0f) return a;
        boolean plain = DmBlows.plain(source);
        HideState.Result r = hide.onHit(tickCount, plain, a, getMaxHealth());
        LivingEntity who = DmBlows.attacker(source);
        if (r.brokeHide()) {
            announce(DmText.hideBroken(bossName()));
            DmFx.burst(this, STYLE, position().add(0, getBbHeight() * 0.6, 0), 40);
        } else if (r.splitPlate()) {
            if (who instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal(DmText.plate(hide.platesSplit(tickCount), HideState.PLATES)), true);
            DmFx.burst(this, STYLE, position().add(0, getBbHeight() * 0.6, 0), 12);
        } else if (plain && r.factor() < 1f && who instanceof ServerPlayer sp && tickCount - lastGlance >= 40) {
            lastGlance = tickCount;
            sp.displayClientMessage(Component.literal(DmText.glance()), true);
        }
        return a * r.factor();
    }

    @Override
    protected void perSecond(long now) {
        if (hide.broken(now) && level() instanceof ServerLevel) DmFx.burst(this, STYLE, position().add(0, getBbHeight() * 0.6, 0), 10);
    }

    @Override protected void resetMechanics() { hide.reset(); }

    // ================================================================ its own moves
    @Override
    protected int runCustom(DmSpec s, LivingEntity target) {
        return switch (s.id()) {
            case BlackGoliathTable.WAILING_HOWL -> howl(s);
            case BlackGoliathTable.MONSTER_CALL -> monsterCall();
            case BlackGoliathTable.DUNGEONS_WRATH -> wrath(s, target);
            default -> 0;
        };
    }

    @Override
    protected void tellCustom(DmSpec s, LivingEntity target, int left, int total) {
        boolean first = TellClock.first(left, total);
        Vec3 at = position();
        switch (s.id()) {
            case BlackGoliathTable.WAILING_HOWL -> {
                DmShapes.groundRing(this, STYLE, at, HowlMath.radius(s.reach(), power()), first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.8, 0), 1.2, 8);
            }
            case BlackGoliathTable.MONSTER_CALL -> {
                DmShapes.groundRing(this, STYLE, at, 7.0, first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.8, 0), 1.2, 8);
            }
            case BlackGoliathTable.DUNGEONS_WRATH -> {
                DmShapes.groundRing(this, STYLE, at, s.reach(), first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.5, 0), 2.0, 12);
            }
            default -> super.tellCustom(s, target, left, total);
        }
    }

    /** The Wailing Howl: a ring that shouts and frightens the fighters below its Level (a slow and a weakness, bounded by the balance law). */
    private int howl(DmSpec s) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        Vec3 at = position();
        double radius = HowlMath.radius(s.reach(), power());
        DmFx.ringStrike(this, STYLE, at, radius, 20);
        DmFx.burst(this, STYLE, at.add(0, getBbHeight() * 0.8, 0), 30);
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0f, 0.6f);
        DmShapes.frighten(this, at, radius, dungeonLevel(), power());
        for (LivingEntity e : foesAround(at, radius)) strike(e, BlackGoliathTable.HOWL_SHOUT, false);
        return 8;
    }

    /** The Monster Call: the roar brings the monsters of the floor, as many as the cap of the power (and of the config) leaves room for. */
    private int monsterCall() {
        if (!(level() instanceof ServerLevel sl)) return 0;
        int cap = AddPlan.cap(power(), DanMachiConfig.maxAdds());
        int n = AddPlan.toSummon(cap, DmAdds.alive(this), tuning().castSlots());
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.5f, 0.8f);
        if (DmAdds.summon(this, DmAdds.Kind.DUNGEON, n, position(), 7.0, STYLE) > 0) announce("The roar brings the monsters of the floor!");
        return 10;
    }

    /**
     * The Dungeon's Wrath, its ultimate: three rings that run across the room (jump them), then six cracks split the floor from its feet, shown for a second
     * first. The gaps between the cracks are wide enough to stand in. It is left exposed afterwards.
     */
    private int wrath(DmSpec s, LivingEntity target) {
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        DmShapes.waves(this, position(), s.reach(), BlackGoliathTable.WRATH_RING, 1.2, 3, WRATH_GAP);
        int warn = 2 * WRATH_GAP + 8;
        double length = 22.0;
        double[][] dirs = DmGeometry.spokes(6, a.phase);
        later(warn, DmSafe.task("wrath warning", () -> {
            Vec3 at = position();
            for (double[] d : dirs) DmFx.laneTell(this, STYLE, at, at.add(d[0] * length, 0, d[1] * length), 2.6, SPOKE_WARN);
        }));
        later(warn + SPOKE_WARN, DmSafe.task("wrath cracks", () -> {
            if (!isAlive()) return;
            DmShapes.spokesAt(this, dirs, length, 2.6, BlackGoliathTable.WRATH_SPOKE, 1.2, new HashSet<Integer>(), DmShapes.budget());
        }));
        return warn + SPOKE_WARN + 6;
    }
}
