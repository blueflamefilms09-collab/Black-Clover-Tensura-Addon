package com.newuniverse.nusmp.arc.sds;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.boss.rules.BossHealGuard;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Why: Ban, the Fox's Sin of Greed, cannot die in the series because he drank from the Fountain of Youth, and he takes what is not his. A boss that cannot die
 * cannot be beaten, and the boss rules of this mod allow no free healing, so the immortality is a RULE of the fight with a price and a counter
 * ({@link FountainOfYouth}): three lives of shrinking size (100%, 60%, 45% of his health); when one ends he does not die, he falls, nothing hurts him and
 * the Fountain works for ten seconds; then he rises through the framework's one sanctioned second wind (the scripted refill the Gates and Lucius's stages use)
 * with the next life. The party's moment is that window: anti-magic, or a holy, soul or severing blow, cuts the Fountain and the next life is only 40% as big.
 * The three lives are laid end to end on the framework's power ladder, so the Gates open exactly when he rises for the last time. His Snatch
 * ({@link SnatchLedger}) takes a capped share of a player's attack for eight seconds (and gives him some of it), and a snatched player who lands three real
 * blows within four seconds takes it back. Super Concentration is a short buff that anti-magic cuts. The strikes are in {@link BanMoves}, the numbers in
 * {@link BanTable}. Server side only; nothing here has run in a game yet. (The generic renderer has no lying-down pose: the fall is shown with effects, the
 * chat, an action bar countdown and the emptied boss bar.)
 */
public class BanBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = ArcRoster.register(SdsCharacters.BAN);
    /** How the client draws him: the numbers of {@code tools/arc/manifest.json} for the model {@code ban} and its larger second form (wings). */
    public static final ArcLook LOOK = new ArcLook("ban", 2.12f, 1.31f, 2.33f, 0.61f, 1.37f, 1.45f, true);
    public static final DeferredHolder<EntityType<?>, EntityType<BanBoss>> TYPE =
            ArcEntities.boss(CHARACTER.id(), BanBoss::new, 0.62f, 2.12f, LOOK, BanBoss::createAttributes);

    private static final int GREEN = BanMoves.GREEN, PALE = BanMoves.PALE, WARN = BanMoves.WARN;
    private static final ResourceLocation RESERVOIR = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "sds_ban_reservoir");
    /** The two hidden moves that put him in the framework's recovery while he lies (so he starts nothing and walks nowhere): the window after life 1 and after life 2. */
    private static final String FALL_1 = "fountain_fall_1", FALL_2 = "fountain_fall_2";
    private static final int RISE_TICKS = 70, FOCUS_TICKS = 160;
    private static final double COMBO_REACH = 4.2, COMBO_HALF = Math.toRadians(52), SWEEP_RADIUS = 5.0, LASH_LENGTH = 11.0, LASH_HALF_WIDTH = 0.9,
            STEP_HALF_WIDTH = 1.1, HUNTER_RADIUS = 2.4, GEYSER_RADIUS = 2.4, HUNT_HALF_WIDTH = 1.2, HUNT_RADIUS = 2.2;

    // ---- the fight's state
    private final FountainOfYouth fountain;
    private final SnatchLedger snatch;
    private Vec3 aim = new Vec3(0, 0, 1);
    private double lockedLength;
    private List<Vec3> marks = List.of();
    private String lockedFor = "";
    private boolean fallStarted;
    private long focusUntil, lastQuip, lastTell;
    private final java.util.Set<String> hinted = new java.util.HashSet<>();

    public BanBoss(EntityType<BanBoss> type, Level level) {
        super(type, level);
        this.fountain = new FountainOfYouth(SdsaConfig.fountainLives());
        this.snatch = new SnatchLedger(SdsaConfig.snatchCap());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), 4.0);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    /** What he has taken from the party makes him hit harder (up to +32%), and so does the Concentration (+15%). */
    @Override
    protected float damageScale() {
        long now = level().getGameTime();
        return (float) (1.0 + (snatch == null ? 0.0 : snatch.bonus(now)) + (tickCount < focusUntil ? 0.15 : 0.0));
    }

    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }

    /** The fox likes a few blocks: close enough for the staff, far enough to Snatch. */
    @Override protected double preferredRange(double suggested) { return Math.max(2.5, Math.min(8.0, suggested)); }

    /** The Last Life wears the second model (the wings); it is the same size. */
    @Override protected int gatesForm() { return FORM_HYBRID; }

    @Override protected float formScale(int form) { return 1.0f; }

    /** With the Fountain on, the Gates come with the last rise (which refills him); with it off they refill him as for every boss. */
    @Override protected boolean refillOnGates() { return fountain != null && fountain.lives() == 1 && super.refillOnGates(); }

    // ================================================================ the three lives on one power ladder
    /** The size of the virtual bar the power curve reads, in the units of this life's real health (so a blow that crosses a line is cut by the right amount). */
    @Override
    protected float powerMaxHealth() {
        if (fountain == null) return super.powerMaxHealth();
        double band = fountain.top(fountain.life()) - fountain.bottom(fountain.life());
        return (float) (getMaxHealth() / Math.max(0.05, band));
    }

    /** The health the power curve reads: this life's band of the ladder, filled by the share of health he has left in it (empty while he lies, full while he is refilled). */
    @Override
    protected float powerHealth() {
        if (fountain == null) return super.powerHealth();
        return (float) (powerMaxHealth() * fountain.powerFraction(getHealth() / getMaxHealth(), refilling()));
    }

    /**
     * A fatal blow that ends a life is not a death: {@link FountainOfYouth#fall} turns it into the window (decided here, in {@code die}, because that is the one
     * door every fatal hit uses, before the death event, the loot and the bar end). The last life, a kill command and the void die for real.
     */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !dead && fall(source)) return;
        super.die(source);
    }

    /** A health of zero that never went through {@code die} (a mod calling {@code setHealth(0)}) is the same fatal blow. */
    @Override
    public void baseTick() {
        if (!level().isClientSide && !dead && fountain != null && fightActive() && getHealth() <= 0f && !isRemoved() && !fountain.falling()) fall(null);
        super.baseTick();
    }

    private void applyReservoir() {
        AttributeInstance max = getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return;
        double k = fountain.reservoir() - 1.0;
        if (Math.abs(k) < 1e-9) max.removeModifier(RESERVOIR);
        else max.addOrReplacePermanentModifier(new AttributeModifier(RESERVOIR, k, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // ================================================================ the bar and the lines
    @Override
    public String barTitle() {
        if (fountain == null) return CHARACTER.name() + " " + power() + "%";
        return CHARACTER.name() + " - Life " + fountain.life() + "/" + fountain.lives() + (fountain.falling() ? " (the Fountain works)" : "") + " " + power() + "%";
    }

    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80) return "Now we're talking. Keep counting, if you can.";
        if (now >= 60) return "You've got some bite. I like it.";
        return "Not bad. Not bad at all.";
    }

    /** The framework announces the Gates in the devil's voice; the party reads his own. */
    @Override
    public void announce(String text) {
        super.announce(SdsaStrikes.QLIPHOTH_GATES.equals(text) ? "The Fountain's light takes Ban over: his Last Life begins!" : text);
    }

    /** A teaching line about a move, said the first time it comes in a fight and not again (the chat is not a wall of text). */
    private void hint(String id, String text) {
        if (hinted.add(id)) announce(text);
    }

    private void quip(String... lines) {
        if (tickCount - lastQuip < 140) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    /** Pushes the new title into the boss bar of everyone who watches (the framework's own refresh is private; starting to see him rebuilds the title). */
    private void refreshTitle(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 64 * 64 && fighters().contains(p.getUUID())) {
                startSeenByPlayer(p);
                return;
            }
        }
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        fountain.reset();
        snatch.clear();
        hinted.clear();
        fallStarted = false;
        focusUntil = 0;
        applyReservoir();
        if (fountain.lives() > 1) {
            announce("Fountain of Youth: Ban does not die. When a life ends he falls and nothing can hurt him for " + FountainOfYouth.windowTicks(1) / 20
                    + " s; then he rises with the next life (" + fountain.lives() + " lives, each smaller). In that window anti-magic, holy, soul or severing blows cut the Fountain: his next life is only "
                    + Math.round(FountainOfYouth.CUT_FACTOR * 100) + "% as big.");
        }
        announce("Snatch: Ban takes a share of your attack for 8 s and uses some of it himself. Land three real blows on him within 4 s to take it back.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        snatch.clear();
        marks = List.of();
        lockedFor = "";
        if (!won) {                                                    // an abandoned fight: back to the first life at full health, which is the framework's reset, not a second wind
            fountain.reset();
            applyReservoir();
            fallStarted = false;
            focusUntil = 0;
            dpsBudget().bypass(() -> BossHealGuard.allow(() -> setHealth(getMaxHealth())));
        }
        super.onFightEnd(won);
    }

    @Override
    protected void onGatesOpened() {
        announce("The Fountain's light takes Ban over: his Last Life. No more lives, no more falls. Finish him.");
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            if (fountain.falling()) {
                if (fountain.windowOver(now) && state() != STATE_RECOVERING) rise(sl, now);
                else if (tickCount % 10 == 0) windowEffects(sl, now);
            }
            if (tickCount % 20 == 0 && fightActive() && !fountain.falling()) snatchUpkeep(sl, now);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Ban's tick failed: {}", e.toString());
        }
    }

    // ================================================================ the Fountain of Youth
    private boolean fall(DamageSource source) {
        if (!(level() instanceof ServerLevel sl) || fountain == null) return false;
        boolean admin = source != null && (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD));
        long now = sl.getGameTime();
        if (!fountain.fall(now, admin)) return false;
        dpsBudget().bypass(() -> BossHealGuard.allow(() -> setHealth(1f)));          // alive at once: a body at 0 health is taken by the game within a second
        interruptCast();
        marks = List.of();
        lockedFor = "";
        fallStarted = false;
        focusUntil = 0;
        snatch.clear();                                                // he lets go of what he took
        Vec3 c = position();
        SdsaStrikes.fx(sl, VfxShape.TREE_CANOPY, c, c.add(0, 1, 0), GREEN, 60, 6.0f, true);
        SdsaStrikes.fx(sl, VfxShape.TREE_ROOTS, c, c.add(0, 1, 0), GREEN, 50, 4.0f, true);
        SdsaStrikes.sound(sl, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.5f);
        announce("Ban's life " + fountain.life() + " of " + fountain.lives() + " ends. He falls, and the Fountain begins to work: nothing can hurt him for "
                + FountainOfYouth.windowTicks(fountain.life()) / 20 + " s. Anti-magic, holy, soul or severing blows cut it; the next life will be "
                + Math.round(fountain.nextReservoir() * 100) + "% of his health.");
        say(fountain.life() == 1 ? "Ha... that one hurt. Now it's my turn to take." : "Still counting?");
        refreshTitle(sl);
        return true;
    }

    private void windowEffects(ServerLevel sl, long now) {
        Vec3 c = position();
        Tells.ring(sl, c, 3.0 + 0.5 * Math.sin(now * 0.3), GREEN, 1.2f, 40);
        Tells.gather(sl, c.add(0, 1.0, 0), 1.0, PALE, 6);
        SdsaStrikes.fx(sl, VfxShape.TREE_ROOTS, c, c.add(0, 1, 0), GREEN, 14, 3.0f, false);
        if (tickCount % 20 == 0) {
            int left = fountain.windowLeft(now) / 20;
            SdsaStrikes.bar(sl, c, 48, Component.literal(fountain.cut() ? "The Fountain is cut. Ban rises in " + left + " s, with a much smaller life."
                    : "The Fountain works: Ban rises in " + left + " s. Anti-magic, holy, soul or severing blows cut it.").withStyle(fountain.cut() ? ChatFormatting.AQUA : ChatFormatting.GREEN));
        }
    }

    /** The next life: the reservoir shrinks, the sanctioned refill fills it (once per life), a clean start. */
    private void rise(ServerLevel sl, long now) {
        boolean cut = fountain.cut();
        fountain.rise();
        applyReservoir();
        if (!refillForMode("fountain_" + fountain.life(), RISE_TICKS)) {            // spent or busy: the same scripted fill, directly
            dpsBudget().bypass(() -> BossHealGuard.allow(() -> setHealth(getMaxHealth())));
        }
        for (MobEffectInstance debuff : new ArrayList<>(getActiveEffects())) {
            if (debuff.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) removeEffect(debuff.getEffect());
        }
        fallStarted = false;
        Vec3 c = position();
        SdsaStrikes.fx(sl, VfxShape.TREE_CANOPY, c, c.add(0, 1, 0), PALE, 50, 5.0f, true);
        SdsaStrikes.sound(sl, c, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.7f);
        announce("Ban rises: life " + fountain.life() + " of " + fountain.lives() + (cut ? ", cut short to " : " with ") + Math.round(fountain.reservoir() * 100) + "% of his health.");
        say(cut ? "Tch... you cut it. It still won't be enough." : "Round " + fountain.life() + ". Let's see what you've got left.");
        refreshTitle(sl);
    }

    // ================================================================ the damage that reaches him
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            long now = sl.getGameTime();
            Entity src = source.getEntity();
            boolean anti = DanteMoves.antiMagicHit(source);
            if (fountain.falling()) {                                   // nothing hurts him while the Fountain works, but a cutting blow still lands its effect
                if (FountainOfYouth.cutsFountain(SdsaStrikes.typePath(source), anti) && fountain.cutNow()) {
                    SdsaStrikes.fx(sl, VfxShape.SPACE_SHATTER, position().add(0, 1, 0), position().add(0, 2, 0), GREEN, 20, 2.5f, true);
                    SdsaStrikes.sound(sl, position(), SoundEvents.SHIELD_BREAK, 2.0f, 0.6f);
                    announce("The Fountain is cut! Ban's next life will only be " + Math.round(fountain.nextReservoir() * 100) + "% of his health.");
                    credit(src, "sds.fountain_cut");
                }
                return 0f;
            }
            if (anti && tickCount < focusUntil) {
                focusUntil = 0;
                announce("Anti-magic breaks Ban's Concentration.");
            }
            float a = amount;
            if (src instanceof ServerPlayer p) {
                UUID id = p.getUUID();
                if (snatch.hitBack(id, now, amount, getMaxHealth())) {
                    SdsaStrikes.fx(sl, VfxShape.TREE_DRAIN, position().add(0, 1.2, 0), p.position().add(0, 1.2, 0), PALE, 16, 1.0f, true);
                    p.displayClientMessage(Component.literal("You wrench your strength back from Ban.").withStyle(ChatFormatting.GREEN), true);
                    credit(p, "sds.snatch_retaken");
                }
                a = (float) (a * snatch.damageFactor(id, now));
            }
            return a;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Ban's guard failed: {}", e.toString());
            return amount;
        }
    }

    private static void credit(Entity who, String key) {
        try {
            if (who instanceof ServerPlayer p) NusmpEvents.fire(p, key);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] the quest hook {} failed: {}", key, e.toString());
        }
    }

    /** While he lies he deals nothing (the plain blow and every move go through here). */
    @Override
    public boolean strike(LivingEntity victim, float base, boolean magic) {
        if (fountain != null && fountain.falling()) return false;
        return super.strike(victim, base, magic);
    }

    // ================================================================ the Snatch
    private boolean trySnatch(ServerLevel sl, LivingEntity victim) {
        if (!(victim instanceof ServerPlayer p) || fountain.falling()) return false;
        long now = sl.getGameTime();
        double want = SnatchLedger.takeOf(PowerCurve.stage(power()));
        double got = snatch.steal(p.getUUID(), now, want);
        if (got <= 0.0) {
            if (tickCount - lastTell > 20) {
                lastTell = tickCount;
                p.displayClientMessage(Component.literal("Ban already holds all he can of your strength.").withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        SdsaStrikes.fx(sl, VfxShape.TREE_DRAIN, p.position().add(0, 1.2, 0), position().add(0, 1.2, 0), GREEN, 20, 1.2f, true);
        SdsaStrikes.sound(sl, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.4f);
        p.displayClientMessage(Component.literal("Snatch: Ban takes " + Math.round(snatch.taken(p.getUUID(), now) * 100) + "% of your attack for "
                + SnatchLedger.DURATION / 20 + " s. Land " + SnatchLedger.RETAKE_HITS + " real blows on him within " + SnatchLedger.RETAKE_WINDOW / 20 + " s to take it back.")
                .withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** Once a second: a thread from every snatched player to him, so everyone can see whose strength he holds. */
    private void snatchUpkeep(ServerLevel sl, long now) {
        if (snatch.victims(now) == 0) return;
        for (LivingEntity e : foes(60)) {
            if (e instanceof ServerPlayer p && snatch.taken(p.getUUID(), now) > 0.0)
                SdsaStrikes.fx(sl, VfxShape.TREE_DRAIN, p.position().add(0, 1.2, 0), position().add(0, 1.2, 0), GREEN, 20, 0.7f, false);
        }
    }

    /** One hand of the Snatch: a fast shot along the floor that offers the first foe it touches to the ledger. */
    private void shootHand(ServerLevel sl, Vec3 start, Vec3 dir) {
        SdsaStrikes.fx(sl, VfxShape.THREAD_LINE, start, start.add(dir.scale(14)), GREEN, 10, 1.0f, true);
        SdsaStrikes.shoot(this, sl, start, dir, 2.2, 12, 0.8, BanTable.SNATCH_DAMAGE, TensuraDamageTypes.ENERGY_DRAIN,
                (e, at) -> {
                    Tells.burst(sl, at, GREEN, 10);
                    trySnatch(sl, e);
                },
                p -> Tells.gather(sl, p, 0.1, GREEN, 2));
    }

    // ================================================================ choosing the next move
    /** While he lies he plays the hidden move that holds him still (once); while he is refilled he starts nothing; otherwise the scheduler's draw. */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (fountain.falling()) {
            if (fallStarted) return -1;
            int idx = moveIndex(fountain.life() <= 1 ? FALL_1 : FALL_2);
            if (idx >= 0) fallStarted = true;
            return idx;
        }
        if (refilling()) return -1;
        return super.chooseMove(bk, s, t);
    }

    // ================================================================ the moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (BanTable.Spec s : BanTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            final String id = s.id();
            final Move.MoveRun run = runOf(id);
            b.tell((bs, t, left, total) -> {
                try {
                    tellOf(id, t, left, total);
                } catch (RuntimeException e) {
                    LogUtils.getLogger().warn("[nusmp] Ban's tell of {} failed: {}", id, e.toString());
                }
            });
            out.add(b.run((bs, t) -> {
                if (!(level() instanceof ServerLevel)) return;
                run.run(bs, t);
            }));
        }
        // the two hidden moves: never drawn (their power is out of reach), played by chooseMove while he lies; the recovery is the window
        out.add(Move.builder(FALL_1, "The Fountain").minPower(1000).windUp(0).cooldown(0).range(0, 1000).mana(0)
                .recovery(FountainOfYouth.windowTicks(1)).run((bs, t) -> { }));
        out.add(Move.builder(FALL_2, "The Fountain").minPower(1000).windUp(0).cooldown(0).range(0, 1000).mana(0)
                .recovery(FountainOfYouth.windowTicks(2)).run((bs, t) -> { }));
        return out;
    }

    // ---------------------------------------------------------------- locking what the wind-up shows
    private static boolean first(int left, int total) { return left >= total - 3; }

    private Vec3 faceOf(LivingEntity target) {
        return SdsaStrikes.unitFlat(target.position().subtract(position()), SdsaStrikes.unitFlat(getViewVector(1f), new Vec3(0, 0, 1)));
    }

    private void lockAim(String id, LivingEntity target) {
        aim = faceOf(target);
        lockedFor = id;
    }

    private Vec3 takeAim(String id, LivingEntity target) {
        Vec3 a = lockedFor.equals(id) ? aim : faceOf(target);
        lockedFor = "";
        return a;
    }

    private void lockMarks(String id, List<Vec3> list) {
        marks = list;
        lockedFor = id;
    }

    private List<Vec3> takeMarks(String id, LivingEntity target) {
        List<Vec3> m = lockedFor.equals(id) && !marks.isEmpty() ? marks : List.of(ZogratisBoss.ground(level(), target.position()));
        marks = List.of();
        lockedFor = "";
        return m;
    }

    /** The chest-height points of up to {@code n} foes, the target first (where the Snatch hands fly). */
    private List<Vec3> handMarks(LivingEntity target, int n) {
        List<Vec3> out = new ArrayList<>();
        out.add(target.getBoundingBox().getCenter());
        for (LivingEntity e : foes(30)) {
            if (out.size() >= n) break;
            if (e != target) out.add(e.getBoundingBox().getCenter());
        }
        return out;
    }

    private int dashes() { return gatesOpen() ? 5 : 4; }

    // ---------------------------------------------------------------- the telegraphs
    private void tellOf(String id, LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        boolean f = first(left, total);
        Vec3 chest = position().add(0, getBbHeight() * 0.6, 0);
        switch (id) {
            case BanTable.COMBO -> {
                if (f) lockAim(id, target);
                SdsaStrikes.tellCone(sl, position(), aim, COMBO_REACH, COMBO_HALF, GREEN);
                Tells.gather(sl, chest.add(aim.scale(0.9)), 0.4, PALE, 3);
            }
            case BanTable.SWEEP -> {
                Tells.ring(sl, position(), SWEEP_RADIUS * (total - left) / Math.max(1, total), GREEN, 1.3f, 40);
                if (left % 6 == 0) Tells.ring(sl, position(), SWEEP_RADIUS, WARN, 1.1f, 48);
                Tells.gather(sl, chest, 0.9, PALE, 4);
            }
            case BanTable.LASH -> {
                if (f) lockAim(id, target);
                SdsaStrikes.tellStrip(sl, position(), aim, LASH_LENGTH, LASH_HALF_WIDTH, GREEN);
                Tells.gather(sl, chest, 0.5, PALE, 3);
            }
            case BanTable.SNATCH -> {
                if (f) lockMarks(id, handMarks(target, 1));
                for (Vec3 m : marks) {
                    Tells.line(sl, chest, m, GREEN);
                    Tells.ring(sl, m.subtract(0, 0.9, 0), 1.2, WARN, 1.1f, 20);
                }
            }
            case BanTable.BARRAGE -> {
                if (f) {
                    lockMarks(id, handMarks(target, Math.min(3, tuning().castSlots())));
                    hint(id, "Snatch Barrage: a hand flies at each of you. Sidestep the threads.");
                }
                for (Vec3 m : marks) {
                    Tells.line(sl, chest, m, GREEN);
                    Tells.ring(sl, m.subtract(0, 0.9, 0), 1.2, WARN, 1.1f, 20);
                }
            }
            case BanTable.FOX_STEP -> {
                if (f) {
                    lockAim(id, target);
                    lockedLength = Math.max(6.0, Math.min(24.0, distanceTo(target) + 1.0));
                }
                SdsaStrikes.tellStrip(sl, position(), aim, lockedLength + 2.0, STEP_HALF_WIDTH, GREEN);
                Tells.gather(sl, chest, 0.5, PALE, 3);
            }
            case BanTable.HUNTER_FEST -> {
                if (f) lockMarks(id, List.of(ZogratisBoss.ground(sl, target.position())));
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, HUNTER_RADIUS, WARN, 1.4f, 32);
                    Tells.ring(sl, m, Math.max(0.5, HUNTER_RADIUS * left / Math.max(1, total)), PALE, 1.0f, 20);
                    Tells.column(sl, m, 4, GREEN);
                    Tells.line(sl, chest, m.add(0, 1, 0), GREEN);
                }
            }
            case BanTable.CONCENTRATION -> {
                Tells.gather(sl, chest, 0.9, PALE, 8);
                Tells.ring(sl, position(), 1.6, GREEN, 1.1f, 24);
            }
            case BanTable.GEYSER -> {
                if (f) lockMarks(id, SdsaStrikes.spread(this, target, 2 + tuning().castSlots(), GEYSER_RADIUS));
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, GEYSER_RADIUS, WARN, 1.3f, 32);
                    Tells.ring(sl, m, Math.max(0.5, GEYSER_RADIUS * left / Math.max(1, total)), PALE, 1.0f, 20);
                    Tells.column(sl, m, 5, GREEN);
                }
            }
            case BanTable.HUNT -> {
                if (f) {
                    lockMarks(id, SdsaStrikes.spread(this, target, 3, HUNT_RADIUS));
                    announce("Fox's Hunt! Ban will run the marked circuit again and again, and every blow he lands Snatches. Stay off the lines.");
                    sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.8f);
                }
                Vec3 prev = position().add(0, 0.2, 0);
                for (Vec3 m : marks) {
                    Tells.line(sl, prev, m.add(0, 0.2, 0), GREEN);
                    Tells.ring(sl, m, 1.8, WARN, 1.2f, 28);
                    prev = m.add(0, 0.2, 0);
                }
                if (marks.size() > 1) Tells.line(sl, prev, marks.get(0).add(0, 0.2, 0), GREEN);
                Tells.gather(sl, chest, 1.8, PALE, 8);
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- what each move does
    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case BanTable.COMBO -> (b, t) -> {
                BanMoves.combo(this, (ServerLevel) level(), takeAim(id, t), COMBO_REACH, COMBO_HALF, BanTable.COMBO_DAMAGE, fountain::falling);
                quip("Heh.", "Too easy.");
            };
            case BanTable.SWEEP -> (b, t) -> BanMoves.sweep(this, (ServerLevel) level(), SWEEP_RADIUS, BanTable.SWEEP_DAMAGE);
            case BanTable.LASH -> (b, t) -> {
                BanMoves.lash(this, (ServerLevel) level(), takeAim(id, t), LASH_LENGTH, LASH_HALF_WIDTH, BanTable.LASH_DAMAGE);
                quip("Longer than it looks, right?");
            };
            case BanTable.SNATCH -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                Vec3 chest = position().add(0, getBbHeight() * 0.6, 0);
                for (Vec3 m : takeMarks(id, t)) shootHand(sl, chest, dirTo(chest, m, faceOf(t)));
                quip("Mine.", "I'll take that.");
            };
            case BanTable.BARRAGE -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                Vec3 chest = position().add(0, getBbHeight() * 0.6, 0);
                List<Vec3> all = takeMarks(id, t);
                for (Vec3 m : all) shootHand(sl, chest, dirTo(chest, m, faceOf(t)));
                if (all.size() == 1) {                                  // alone: three hands in a fan
                    Vec3 d = dirTo(chest, all.get(0), faceOf(t));
                    shootHand(sl, chest, SdsaStrikes.rotateY(d, 0.18));
                    shootHand(sl, chest, SdsaStrikes.rotateY(d, -0.18));
                }
                quip("Everything you've got is mine!");
            };
            case BanTable.FOX_STEP -> (b, t) -> {
                BanMoves.foxStep(this, (ServerLevel) level(), takeAim(id, t), lockedLength <= 0 ? 10 : lockedLength, STEP_HALF_WIDTH, BanTable.STEP_DAMAGE);
                quip("Behind you.");
            };
            case BanTable.HUNTER_FEST -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                BanMoves.hunterFest(this, sl, takeMarks(id, t).get(0), HUNTER_RADIUS, BanTable.HUNTER_DAMAGE, e -> trySnatch(sl, e));
                say("Hunter Fest!");
            };
            case BanTable.CONCENTRATION -> (b, t) -> {
                focusUntil = tickCount + FOCUS_TICKS;
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FOCUS_TICKS, 0));
                if (level() instanceof ServerLevel sl) VfxSpawn.sendFollowing(sl, VfxShape.SPIRIT_AURA, this, position().add(0, 1, 0), 0xFF000000 | GREEN, FOCUS_TICKS, 1.4f);
                announce("Super Concentration: Ban is faster and hits 15% harder for " + FOCUS_TICKS / 20 + " s. Anti-magic breaks it.");
                quip("Let me focus...");
            };
            case BanTable.GEYSER -> (b, t) -> BanMoves.geysers(this, (ServerLevel) level(), takeMarks(id, t), GEYSER_RADIUS, BanTable.GEYSER_DAMAGE);
            case BanTable.HUNT -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                BanMoves.hunt(this, sl, takeMarks(id, t), dashes(), HUNT_HALF_WIDTH, BanTable.HUNT_DAMAGE, e -> trySnatch(sl, e), fountain::falling);
                say("Fox's Hunt!");
            };
            default -> (b, t) -> { };
        };
    }

    /** The unit direction from {@code from} to {@code to}, or {@code fallback} when they are the same point. */
    private static Vec3 dirTo(Vec3 from, Vec3 to, Vec3 fallback) {
        Vec3 d = to.subtract(from);
        return d.lengthSqr() < 1e-4 ? fallback : d.normalize();
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        FountainOfYouth.Saved s = fountain.save(level().getGameTime());
        tag.putInt("BLife", s.life());
        tag.putDouble("BReservoir", s.reservoir());
        tag.putBoolean("BFalling", s.falling());
        tag.putBoolean("BCut", s.cut());
        tag.putInt("BWindow", s.windowLeft());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("BLife")) return;
        fountain.load(new FountainOfYouth.Saved(tag.getInt("BLife"), tag.getDouble("BReservoir"), tag.getBoolean("BFalling"), tag.getBoolean("BCut"), tag.getInt("BWindow")),
                level().getGameTime());
        applyReservoir();
    }

    /** For the op-only debug view: the Fountain and the Snatch in one line. */
    public String debug() {
        long now = level().getGameTime();
        return String.format(Locale.ROOT, "life %d/%d reservoir %.2f falling %s cut %s window %d borrowed %.2f", fountain.life(), fountain.lives(), fountain.reservoir(),
                fountain.falling(), fountain.cut(), fountain.windowLeft(now), snatch.borrowed(now));
    }
}
