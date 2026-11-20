package com.newuniverse.nusmp.arc.magi;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.sds.SdsaStrikes;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: Sinbad, King of the Seven Seas (Magi), as an arc boss ({@link ArcBoss}: telegraphed moves, the power curve, the per-second damage budget, the boss bar, the arena and its leash, the
 * story credit, the spawn egg, the boss-registry and buff rows). His rule (canon): he wears only ONE djinn at a time and switches fast.
 * <ul>
 * <li>He starts unequipped (sword slashes, the Magoi Thrust). <b>Djinn Equip</b> is a move: he picks the djinn that fits the situation ({@link SinbadEquip#pick}) after a coloured pillar
 *     tells which one, and holds it for 20 s (12 s once the Gates are open). While a djinn is on, only that djinn's moves and his sword moves are chosen ({@link SinbadEquip#allowed});
 *     his colour in the telegraphs and the boss bar's name tell what is coming. Djinn unlock with the power of the fight ({@link SinbadTable#unlockPower}).</li>
 * <li>Passives: Magoi Manipulation (magic damage taken x0.75) and the "flow of destiny" foresight (a 12% chance to sidestep one hit, at most every 10 s, never while he casts).</li>
 * <li>Once the Gates are open the finale Seven Seas Sovereign exists (power 90+, an ultimate).</li>
 * </ul>
 * Damage is the framework's: {@code strike} (power tuning, the victim's EP factor, the balance law, the per-second budget). No new scaling is added. The swap heal of the first draft
 * (3% of his health on a switch) was left out: the boss rules ({@code BossHealGuard}) drop every heal of a boss in combat.
 * <p>Cost per tick: nothing beyond the framework's; his own tick is one comparison plus, every 40 ticks, one effect packet. Every move is bounded (see {@link SinbadMoves}).
 * Server side only; nothing here has run in a game yet.
 */
public class SinbadBoss extends ArcBoss {
    public static final String ID = "sinbad_boss";

    public static final ArcCharacter CHARACTER = MagiCharacters.SINBAD;

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** The bosses of this class made on the server and not yet removed (the cap and the logout cleanup read it). A boss joins at construction, so a burst of summonings in one tick cannot pass the cap. */
    private static final Set<SinbadBoss> LIVE = ConcurrentHashMap.newKeySet();

    private SinbadDjinn equip = SinbadDjinn.NONE;
    private SinbadDjinn pendingDjinn;
    private long equipUntil, nextForesight;
    private final Map<String, List<Vec3>> lockedSpots = new HashMap<>();
    private Vec3 aim = new Vec3(0.0, 0.0, 1.0);
    private String aimFor = "";

    public SinbadBoss(EntityType<? extends SinbadBoss> type, Level level) {
        super(type, level);
        this.xpReward = 3000;
        if (!level.isClientSide) LIVE.add(this);
    }

    /** His base attributes: 3,200 health, 24 melee, 0.32 speed, 15 armour (the framework's tuning and the buff row of tier 3 come on top). */
    public static AttributeSupplier.Builder attributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), 10.0);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ the live set (cap, cleanup)
    /** How many of him stand in loaded chunks (server side). */
    public static int liveCount() {
        prune();
        return LIVE.size();
    }

    /** Drops the removed ones and the ones that were made but never put into the world (a failed summoning). */
    private static void prune() {
        LIVE.removeIf(b -> b.isRemoved() || (b.tickCount == 0 && b.level() instanceof ServerLevel s && s.getEntity(b.getId()) != b));
    }

    /** The live bosses (a copy; for the command and the cleanup events). */
    public static List<SinbadBoss> live() {
        prune();
        return List.copyOf(LIVE);
    }

    /** Forgets everything (the server is stopping). */
    public static void clearLive() { LIVE.clear(); }

    // ================================================================ the equip state
    SinbadDjinn equip() { return equip; }

    /** The id of the djinn he wears ("none", "baal", ...), for the command and the self-test. */
    public String equipId() { return equip.id; }

    private long now() { return level().getGameTime(); }

    /** Puts a djinn on (or {@link SinbadDjinn#NONE} off) for one equip time. {@code announce} is false for the finale's rapid switching and for clean-ups. */
    void setEquip(ServerLevel sl, SinbadDjinn d, boolean announce) {
        if (d == equip) return;
        equip = d;
        equipUntil = now() + SinbadEquip.equipTicks(gatesOpen());
        if (d != SinbadDjinn.NONE) {
            VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, this, position(), d.argb, 30, 2.0F);
            sl.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, getSoundSource(), 1.0F, 1.4F);
            if (announce) say((d.tribe.isEmpty() ? "" : "Spirit of " + d.tribe + ", ") + d.displayName + "! Dwell in my body - Djinn Equip!");
        }
        refreshTitle(sl);
    }

    /** The bar's name carries the djinn; the framework rebuilds it when a player starts to see him, so one such call updates the shared bar for everyone. */
    private void refreshTitle(ServerLevel sl) {
        for (java.util.UUID id : fighters()) {
            if (sl.getPlayerByUUID(id) instanceof ServerPlayer p && p.distanceToSqr(this) < 64 * 64) {
                startSeenByPlayer(p);
                return;
            }
        }
    }

    // ================================================================ spots and aim locked between the tell and the hit
    void lockSpots(String id, List<Vec3> spots) { lockedSpots.put(id, spots); }

    /** The spots of a move still being wound up (they stay until the move lands). */
    List<Vec3> peekSpots(String id) { return lockedSpots.getOrDefault(id, List.of()); }

    List<Vec3> takeSpots(String id, LivingEntity fallbackTarget) {
        List<Vec3> s = lockedSpots.remove(id);
        return s != null && !s.isEmpty() ? s : List.of(fallbackTarget.position());
    }

    private void lockAim(String id, LivingEntity t) {
        aim = SinbadMoves.face(this, t);
        aimFor = id;
    }

    private Vec3 takeAim(String id, LivingEntity t) {
        Vec3 a = aimFor.equals(id) ? aim : SinbadMoves.face(this, t);
        aimFor = "";
        return a;
    }

    // ================================================================ framework hooks
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7F; }

    /** Baal and Focalor fight at range, the rest up close. */
    @Override
    protected double preferredRange(double suggested) {
        return (equip == SinbadDjinn.BAAL || equip == SinbadDjinn.FOCALOR) ? Math.max(8.0, suggested) : Math.max(2.5, Math.min(6.0, suggested));
    }

    /** Magoi Manipulation (magic damage taken x0.75) and the foresight sidestep (not while he casts, at most every 10 s). Not called while he is exposed after an ultimate. */
    @Override
    protected float mitigate(DamageSource source, float amount) {
        long t = now();
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && castingMoveOrNull() == null && t >= nextForesight && random.nextFloat() < SinbadTable.FORESIGHT_CHANCE) {
            nextForesight = t + SinbadTable.FORESIGHT_COOLDOWN;
            if (level() instanceof ServerLevel sl) SdsaStrikes.fx(sl, VfxShape.SPATIAL_RIFT, position().add(0.0, 1.0, 0.0), position().add(0.0, 1.5, 0.0), SinbadDjinn.NONE.rgb(), 10, 0.8F, true);
            say("I already saw that coming.");
            return 0.0F;
        }
        return isMagic(source) ? amount * SinbadTable.MAGOI_MAGIC_TAKEN : amount;
    }

    private static boolean isMagic(DamageSource source) {
        return source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.typeHolder().unwrapKey().map(k -> "tensura".equals(k.location().getNamespace())).orElse(false);
    }

    @Override
    protected void bossTick() {
        if (level().isClientSide) return;
        LIVE.add(this);
        if (!(level() instanceof ServerLevel sl) || isDeadOrDying()) return;
        try {
            if (equip != SinbadDjinn.NONE && now() >= equipUntil && castingMoveOrNull() == null) setEquip(sl, SinbadDjinn.NONE, false);
            if (equip != SinbadDjinn.NONE && tickCount % 40 == 0) VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, this, position(), equip.argb, 40, 1.0F);
        } catch (RuntimeException e) {
            LOG.warn("[anime_multiverse] Sinbad's tick failed: {}", e.toString());
        }
    }

    @Override
    protected void onFightStart() {
        announce("Sinbad holds seven Metal Vessels, but he can wear only one Djinn Equip at a time. Watch his colour and the name on his bar: they tell which djinn's attacks are coming.");
    }

    @Override
    protected void onGatesOpened() {
        announce("Sinbad, the King Vessel, pushes his djinn to the limit: equips are shorter, and the Seven Seas answer him.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        equip = SinbadDjinn.NONE;
        pendingDjinn = null;
        lockedSpots.clear();
        aimFor = "";
        super.onFightEnd(won);
    }

    @Override
    public void remove(RemovalReason reason) {
        LIVE.remove(this);
        super.remove(reason);
    }

    @Override
    public String barTitle() {
        return CHARACTER.name() + (equip == SinbadDjinn.NONE ? "" : " [" + equip.displayName + "]") + " " + power() + "%" + (gatesOpen() ? " - Gates open" : "");
    }

    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80) return "Seven djinn answer my call. Can you keep up?";
        return now >= 50 ? "Not bad. Let me get a little more serious." : "Relax, I won't go all out yet.";
    }

    /**
     * Only the equipped djinn's moves (plus the sword moves) may be chosen: the framework's own weighted draw (range, crowd, cooldown, mana, ultimate pacing) runs on the allowed moves
     * only, and its answer is mapped back to the full list. The equip move is allowed when nothing is on or the equip has under three seconds left.
     */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        long left = equipUntil - now();
        boolean gates = gatesOpen();
        List<Move> ok = new ArrayList<>(bk.size());
        for (Move m : bk) if (SinbadEquip.allowed(SinbadTable.byId(m.id()), equip, gates, left)) ok.add(m);
        int i = super.chooseMove(ok, s, t);
        return i < 0 ? -1 : bk.indexOf(ok.get(i));
    }

    // ================================================================ the moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (SinbadTable.Spec s : SinbadTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            final String id = s.id();
            final Move.MoveRun run = runOf(id);
            b.tell((bs, target, left, total) -> {
                try {
                    tellOf(id, target, left, total);
                } catch (RuntimeException e) {
                    LOG.warn("[anime_multiverse] Sinbad's tell of {} failed: {}", id, e.toString());
                }
            });
            out.add(b.run((bs, target) -> {
                if (!(level() instanceof ServerLevel)) return;
                try {
                    run.run(bs, target);
                } catch (RuntimeException e) {
                    LOG.warn("[anime_multiverse] Sinbad's move {} failed: {}", id, e.toString());
                }
            }));
        }
        return out;
    }

    private static boolean first(int left, int total) { return left >= total - 3; }

    /** The telegraph is drawn on the first call and then every {@code every} ticks (the framework calls it every third tick). */
    private static boolean pulse(int left, int total, int every) { return first(left, total) || left % every == 0; }

    /** Telegraphs: called every third tick of the wind-up. The first call locks the aim and the spots; later calls redraw them in the move's colour. */
    private void tellOf(String id, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl) || t == null) return;
        SinbadTable.Spec spec = SinbadTable.byId(id);
        boolean start = first(left, total);
        int rgb = (spec == null || spec.djinn() == SinbadDjinn.NONE ? equip : spec.djinn()).rgb();
        switch (id) {
            case SinbadTable.SWORD_SLASH -> {
                if (start) lockAim(id, t);
                SdsaStrikes.tellCone(sl, position(), aim, 6.0, Math.toRadians(65.0), rgb);
            }
            case SinbadTable.SABER_FLURRY -> {
                if (start) lockAim(id, t);
                SdsaStrikes.tellCone(sl, position(), aim, 6.5, Math.toRadians(50.0), rgb);
            }
            case SinbadTable.MAGOI_THRUST, SinbadTable.BARARAQ_SAIQA, SinbadTable.RENDING_DARKNESS -> {
                if (start) lockAim(id, t);
                if (pulse(left, total, 6)) {
                    double len = SinbadTable.MAGOI_THRUST.equals(id) ? 10.0 : SinbadTable.BARARAQ_SAIQA.equals(id) ? 28.0 : 22.0;
                    SdsaStrikes.tellStrip(sl, SinbadMoves.chest(this), aim, len, 1.4, rgb);
                }
            }
            case SinbadTable.DJINN_EQUIP -> {
                if (start) pendingDjinn = SinbadEquip.pick(distanceTo(t), foes(20.0).size(), power(), equip);
                if (pendingDjinn != null && pulse(left, total, 6)) Tells.column(sl, position(), 4.0, pendingDjinn.rgb());
            }
            case SinbadTable.BARARAQ -> {
                if (start) SinbadMoves.bararaqMark(this, t);
                if (pulse(left, total, 9)) SinbadMoves.tellSpots(sl, peekSpots(id), 2.5, rgb);
            }
            case SinbadTable.INQERAD_SAIQA -> {
                if (start) {
                    SinbadMoves.inqeradMark(this, t);
                    say("Bararaq... Inqerad-Saiqa!");
                }
                if (pulse(left, total, 9)) {
                    for (Vec3 p : peekSpots(id)) {
                        Tells.ring(sl, p, 3.0, 0xFFD54F, 1.3f, 16);
                        Tells.column(sl, p, 6.0, 0xFFD54F);
                    }
                    Tells.ring(sl, position(), 22.0, rgb, 1.3f, 48);
                }
            }
            case SinbadTable.COMMAND_SCREAM -> {
                if (start) announce("Sinbad (Zepar) draws breath for a commanding scream. Look away!");
                if (pulse(left, total, 9)) Tells.ring(sl, position(), 16.0, rgb, 1.3f, 40);
            }
            case SinbadTable.FORAZ_ZORA -> {
                if (start) SinbadMoves.forazZoraMark(this, sl, t);
                if (pulse(left, total, 9)) SinbadMoves.tellSpots(sl, peekSpots(id), 5.0, rgb);
            }
            case SinbadTable.VEPAR_ISTELLA -> {
                if (start) SinbadMoves.veparIstellaMark(this, t);
                if (pulse(left, total, 9)) SinbadMoves.tellSpots(sl, peekSpots(id), 1.6, rgb);
            }
            case SinbadTable.STAGNATION -> {
                if (pulse(left, total, 9)) Tells.ring(sl, position(), 9.0, rgb, 1.3f, 32);
            }
            case SinbadTable.GARUFOR_KIRESTAL, SinbadTable.RUKH_INTRUSION -> {
                if (pulse(left, total, 6)) Tells.column(sl, t.position(), 2.5, rgb);
            }
            case SinbadTable.SEVEN_SEAS -> {
                if (start) {
                    announce("Sinbad raises all seven Metal Vessels at once!");
                    say("Seven Seas, answer your king!");
                }
            }
            default -> { }
        }
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case SinbadTable.SWORD_SLASH -> (b, t) -> SinbadMoves.swordSlash(this, (ServerLevel) level(), takeAim(id, t));
            case SinbadTable.MAGOI_THRUST -> (b, t) -> SinbadMoves.magoiThrust(this, (ServerLevel) level(), takeAim(id, t));
            case SinbadTable.DJINN_EQUIP -> (b, t) -> {
                SinbadDjinn next = pendingDjinn != null && pendingDjinn != equip ? pendingDjinn : SinbadEquip.pick(distanceTo(t), foes(20.0).size(), power(), equip);
                pendingDjinn = null;
                setEquip((ServerLevel) level(), next, true);
            };
            case SinbadTable.BARARAQ -> (b, t) -> SinbadMoves.bararaqFire(this, (ServerLevel) level(), t);
            case SinbadTable.BARARAQ_SAIQA -> (b, t) -> SinbadMoves.bararaqSaiqa(this, (ServerLevel) level(), takeAim(id, t));
            case SinbadTable.INQERAD_SAIQA -> (b, t) -> SinbadMoves.inqeradFire(this, (ServerLevel) level(), t);
            case SinbadTable.GARUFOR_ZAIRU -> (b, t) -> SinbadMoves.garuforZairu(this, (ServerLevel) level(), t);
            case SinbadTable.GARUFOR_KIRESTAL -> (b, t) -> SinbadMoves.garuforKirestal(this, (ServerLevel) level(), t);
            case SinbadTable.STAGNATION -> (b, t) -> SinbadMoves.stagnation(this, (ServerLevel) level());
            case SinbadTable.COMMAND_SCREAM -> (b, t) -> SinbadMoves.commandScream(this, (ServerLevel) level());
            case SinbadTable.RUKH_INTRUSION -> (b, t) -> SinbadMoves.rukhIntrusion(this, (ServerLevel) level(), t);
            case SinbadTable.FORAZ_ZORA -> (b, t) -> SinbadMoves.forazZoraFire(this, (ServerLevel) level(), t);
            case SinbadTable.GALE_STEP -> (b, t) -> SinbadMoves.galeStep(this, (ServerLevel) level(), t);
            case SinbadTable.VEPAR_ISTELLA -> (b, t) -> SinbadMoves.veparIstellaFire(this, (ServerLevel) level(), t);
            case SinbadTable.SABER_FLURRY -> (b, t) -> SinbadMoves.saberFlurry(this, (ServerLevel) level(), t);
            case SinbadTable.RENDING_DARKNESS -> (b, t) -> SinbadMoves.rendingDarkness(this, (ServerLevel) level(), takeAim(id, t));
            case SinbadTable.SEVEN_SEAS -> (b, t) -> SinbadMoves.sevenSeas(this, (ServerLevel) level(), t);
            default -> (b, t) -> { };
        };
    }

    // ================================================================ save / load
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SinbadEquip", equip.id);
        tag.putLong("SinbadEquipUntil", equipUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        equip = SinbadDjinn.byId(tag.getString("SinbadEquip"));
        equipUntil = tag.getLong("SinbadEquipUntil");
    }

    // ================================================================ test seams (package-private, never used by the game)
    /** The self-test puts a djinn on directly. */
    void equipForTest(ServerLevel sl, SinbadDjinn d) { setEquip(sl, d, false); }

    /** Would the move filter let this move through right now? */
    boolean allowsForTest(String moveId) {
        return SinbadEquip.allowed(SinbadTable.byId(moveId), equip, gatesOpen(), equipUntil - now());
    }

    /** How many sets of spots are still locked (they must be empty after a fight ends). */
    int lockedForTest() { return lockedSpots.size(); }
}
