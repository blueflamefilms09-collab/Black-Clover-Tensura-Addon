package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the four monsters of the Dungeon (the Minotaur, the Black Goliath, the Amphisbaena, Udaeus) are one kind of fight with four faces, and what they share
 * is a lot: a table of moves that are rows of data ({@link DmSpec}) which one executor draws and strikes ({@link DmShapes}), the Dungeon Level (at the Gates of
 * the fight a monster whose Level the party has reached "Levels up": a stronger leveled form, more damage and speed, one more move; see {@link DungeonLevel}), the
 * stun of a crash, a busy time while a long effect runs, the called monsters, the quest hook. This base class holds all of it on top of the arc layer, so a
 * monster class is only its table, its own mechanic (the hunt, the black hide, the red head, the rib guard) and its custom moves. It never changes a block,
 * heals nothing, and never applies hard control (the Howl is a slow and a weakness, bounded by the balance law).
 *
 * <p>How the Dungeon Level ties to the Gates: the framework opens the Gates at a quarter of the monster's health (100% power, a 60-tick transformation, the second health
 * bar). {@link #onGatesOpened} measures the party then; with enough Level the monster takes 1 to 3 steps (the leveled form {@code arc_<id>_form2} is the form the Gates
 * open into, so the model swaps only when it really Levelled up), otherwise the Gates only enrage it and the form stays as it was.
 */
public abstract class DanMachiBoss extends ArcBoss {
    /** The id of the permanent speed modifier that a Level-Up gives. */
    static final ResourceLocation LEVEL_SPEED = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "dm_level_speed");
    /** The share of the attack attribute that the plain blow does (the real hits are the telegraphed moves). */
    static final float MELEE_SHARE = 0.5f;

    static {
        DanMachiQuests.register();
    }

    private int steps;
    private long busyUntil, stunUntil;
    private DmShapes.Aim aim;
    /** True from the first monster this boss calls until none is left (so a boss that never calls does not search the world for them every second). */
    private boolean called;
    private Map<String, DmSpec> specs;

    protected DanMachiBoss(EntityType<? extends DanMachiBoss> type, Level level) {
        super(type, level);
    }

    // ================================================================ what a monster supplies
    /** Its moves, in the order of its list (new rows are only ever appended). */
    protected abstract List<DmSpec> table();

    /** The monster's own Dungeon Level (the Minotaur 2 ... Udaeus 6). */
    protected abstract int baseLevel();

    /** What its effects are made of. */
    protected abstract DmFx.Style style();

    /** Runs a {@link DmSpec.Shape#CUSTOM} row; returns the ticks its effect keeps going (the monster starts nothing else meanwhile). */
    protected int runCustom(DmSpec s, LivingEntity target) { return 0; }

    /** The telegraph of a custom row (every third tick of the wind-up). The default is a ring round the body of the row's reach. */
    protected void tellCustom(DmSpec s, LivingEntity target, int left, int total) {
        if (level() instanceof ServerLevel sl) Tells.ring(sl, position(), Math.max(2.0, s.reach()), style().tint(this), 1.1f, 36);
    }

    /** May the monster use this row now? The default: a leveled row only once it has Levelled up. A monster adds its own conditions. */
    protected boolean usable(DmSpec s) { return !s.leveled() || leveled(); }

    /** The factor on its damage to one victim (the Minotaur's prey takes more). */
    protected float victimFactor(LivingEntity victim) { return 1.0f; }

    /** Forgets what the fight taught the monster (a new fight, or an abandoned one). */
    protected void resetMechanics() {}

    /** Once a second while the fight lasts. */
    protected void perSecond(long now) {}

    // ================================================================ the Dungeon Level
    /** Level-Up steps taken at the Gates (0 = it has not Levelled up). */
    public int steps() { return steps; }

    public boolean leveled() { return steps > 0; }

    /** The Level it fights at now: its own plus its steps. */
    public int dungeonLevel() { return DungeonLevel.bossLevelAfter(baseLevel(), steps); }

    @Override
    protected void onGatesOpened() {
        super.onGatesOpened();
        int party = PartyLevels.partyLevel(this);
        steps = DungeonLevel.steps(party, baseLevel(), DanMachiConfig.levelMargin(), DanMachiConfig.maxSteps());
        applyLevelSpeed();
        if (steps > 0) announce(DmText.levelUp(bossName(), baseLevel(), dungeonLevel(), steps));
        else if (DanMachiConfig.maxSteps() > 0) announce(DmText.holds(bossName(), party, baseLevel(), DanMachiConfig.levelMargin()));
    }

    /** The Gates open into the leveled form only when the monster really Levelled up; otherwise the body stays as it was. */
    @Override protected int gatesForm() { return steps > 0 ? FORM_HYBRID : FORM_BASE; }

    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.08f : 1.0f; }

    @Override protected float damageScale() { return (float) DungeonLevel.damageFactor(steps, DanMachiConfig.damagePerStep()); }

    private void applyLevelSpeed() {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(LEVEL_SPEED);
        double more = DungeonLevel.speedFactor(steps, DanMachiConfig.speedPerStep()) - 1.0;
        if (more > 0.0) speed.addOrReplacePermanentModifier(new AttributeModifier(LEVEL_SPEED, more, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override public String barTitle() { return DmText.bar(bossName(), dungeonLevel(), power(), gatesOpen(), leveled()); }

    /** The framework announces the Gates in a devil's voice; the Dungeon's monsters announce them in their own. */
    @Override
    public void announce(String text) {
        super.announce(text != null && text.startsWith("The Gates of the Tree of Qliphoth") ? "The Dungeon stirs: " + bossName() + " draws its full strength!" : text);
    }

    // ================================================================ moves
    @Override
    protected final List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (DmSpec s : table()) out.add(toMove(s));
        return out;
    }

    private Move toMove(DmSpec s) {
        Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks()).range(s.minRange(), s.maxRange())
                .weight(s.weight()).recovery(s.recovery()).tell((boss, target, left, total) -> tell(s, target, left, total));
        if (s.ultimate()) b.ultimate();
        if (s.crowd()) b.crowd();
        return b.run((boss, target) -> cast(s, target));
    }

    private void tell(DmSpec s, LivingEntity target, int left, int total) {
        if (s.shape() == DmSpec.Shape.CUSTOM) {
            if (TellClock.first(left, total)) aim = DmShapes.lock(this, s, target);
            tellCustom(s, target, left, total);
        } else {
            DmShapes.tell(this, s, target, left, total);
        }
    }

    private void cast(DmSpec s, LivingEntity target) {
        int ticks = s.shape() == DmSpec.Shape.CUSTOM ? runCustom(s, target) : DmShapes.run(this, s, target);
        busyFor(ticks + 2);
        aim = null;
    }

    boolean calledAny() { return called; }

    void markCalled() { called = true; }

    void forgetCalled() { called = false; }

    /** The row with this id, or null. */
    protected final DmSpec spec(String id) {
        if (specs == null) {
            Map<String, DmSpec> m = new HashMap<>();
            for (DmSpec s : table()) m.put(s.id(), s);
            specs = m;
        }
        return specs.get(id);
    }

    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (t < busyUntil || t < stunUntil) return -1;
        List<Move> allowed = new ArrayList<>(bk.size());
        int[] map = new int[bk.size()];
        for (int i = 0; i < bk.size(); i++) {
            DmSpec sp = spec(bk.get(i).id());
            if (sp == null || usable(sp)) {
                map[allowed.size()] = i;
                allowed.add(bk.get(i));
            }
        }
        if (allowed.isEmpty()) return -1;
        int k = super.chooseMove(allowed, s, t);
        return k < 0 ? -1 : map[k];
    }

    /** The plain blow is half the attack attribute, and none at all while the monster is busy or stunned (a stunned bull must not gore). */
    @Override
    protected float meleeDamage() {
        if (busy() || stunned()) return 0f;
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * MELEE_SHARE;
    }

    @Override
    public boolean strike(LivingEntity victim, float base, boolean magic) {
        return super.strike(victim, base * victimFactor(victim), magic);
    }

    // ================================================================ busy, stunned, aim
    boolean busy() { return tickCount < busyUntil; }

    boolean stunned() { return tickCount < stunUntil; }

    void busyFor(int ticks) { busyUntil = Math.max(busyUntil, tickCount + Math.max(0, ticks)); }

    DmShapes.Aim aim() { return aim; }

    void setAim(DmShapes.Aim a) { aim = a; }

    /** The nearest foe within 48 blocks, or null. */
    LivingEntity nearestFoe() {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : foesAround(position(), 48.0)) {
            double d = e.distanceToSqr(this);
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    /** Holds the body where it is this tick: the walking goal may not move a monster that is casting a long effect or stunned. */
    void holdStill() {
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
    }

    /** The monster rammed a wall: it is stunned (and takes more damage) for as long as {@link ChargeMath#crashStun} says, and the wall is shown cracking. */
    void crash(Vec3 wall) {
        int ticks = ChargeMath.crashStun(power());
        stunUntil = Math.max(stunUntil, tickCount + ticks);
        busyFor(ticks);
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (level() instanceof ServerLevel sl) {
            DmFx.spotStrike(this, style(), wall, 3.0);
            DmFx.burst(this, style(), wall, 30);
            sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.6f, 0.5f);
        }
        announce(DmText.crashed(bossName()));
    }

    /** Takes {@link ChargeMath#CRASH_BONUS} while it is stunned. */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        float a = super.filterDamage(source, amount);
        return stunned() ? a * ChargeMath.CRASH_BONUS : a;
    }

    // ================================================================ the fight
    @Override
    protected void bossTick() {
        super.bossTick();
        if (!(level() instanceof ServerLevel sl)) return;
        long t = tickCount;
        if (t < busyUntil || t < stunUntil) holdStill();
        if (t < stunUntil && t % 10 == 0) Tells.burst(sl, position().add(0, getBbHeight() + 0.3, 0), style().tint(this), 5);
        if (t % 20 == 7) {
            DmAdds.sweep(this, false);
            perSecond(t);
        }
    }

    @Override
    protected void onFightStart() {
        super.onFightStart();
        steps = 0;
        applyLevelSpeed();
        resetMechanics();
        int party = PartyLevels.partyLevel(this);
        announce("Dungeon Level: " + bossName() + " is " + DungeonLevel.label(baseLevel()) + (party > 0 ? ", your party is " + DungeonLevel.label(party) : "")
                + ". At the Gates a strong party makes it Level up.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        DmAdds.sweep(this, true);
        if (!won) {
            steps = 0;
            applyLevelSpeed();
            stunUntil = 0;
            busyUntil = 0;
            resetMechanics();
        }
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DmSteps", steps);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        steps = Math.max(0, tag.getInt("DmSteps"));
        called = true;                  // a reloaded fight may have monsters standing that it called: the first sweep finds out
    }
}
