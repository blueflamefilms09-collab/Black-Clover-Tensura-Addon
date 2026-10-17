package com.newuniverse.nusmp.arc.early;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Mars is the Diamond Kingdom's weapon of the Dungeon Exploration Arc: an orphan with mage stones set into his body to amplify his magic, who made crystal
 * clones of himself that multiplied, cloaked himself in Nemean Armor, raised the Heavy Armored Titan, and was beaten by a rookie's anti-magic and a wind
 * spirit's blast that shattered his crystal. The fight is built on what sets him apart ({@link MarsTable}).
 * <ul>
 *   <li><b>Mage stones</b> ({@link StoneLedger}): three stones set in his body, each adding a tenth to his damage. At each step of his power (40, 60, 80%)
 *       and at the Gates one of them surfaces and glows for twelve seconds; blows he takes meanwhile (anti-magic counts double) are counted towards cracking it.
 *       A cracked stone is gone for good and staggers him; a stone not cracked in time sinks back and keeps its tenth.</li>
 *   <li><b>Nemean Armor</b> ({@link FrontGuard}): while the crystal stands over the front of his body, blows from within 65 degrees of the way he faces are cut
 *       to 40%; from the side or behind they land in full, and anti-magic cuts straight through. Walk round him.</li>
 *   <li><b>Crystal Doubles</b> (from 40%): fangs erupt at the corners of a triangle (a square from 80%) round the target, all at once. The middle is the safe
 *       place, and so is anywhere outside the ring.</li>
 *   <li><b>Heavy Armored Titan</b> (the ultimate, from 60%): a locked circle, then two rings that run out of it. Leave the circle, then jump the rings. He is
 *       open for three seconds after it.</li>
 * </ul>
 * He changes no block, spawns nothing but crystal wisps (no entity), heals nothing (the fire of the canon that mends him is not used); the framework's Gates give
 * him the one refill.
 */
public class MarsBoss extends EarlyBoss {
    public static final ArcCharacter CHARACTER = EarlyRoster.MARS;
    public static final DeferredHolder<EntityType<?>, EntityType<MarsBoss>> TYPE = ArcEntities.boss(CHARACTER.id(), MarsBoss::new, 0.62f, 1.94f,
            new ArcLook(CHARACTER.id(), 1.94f, 1.16f, 2.06f, 0.52f, 1.25f, 1.13f, true), MarsBoss::createAttributes);

    private static final int RGB = CHARACTER.color(), ACCENT = CHARACTER.accent(), WARN = 0xFF4A4A;
    private static final int ARGB = 0xFF000000 | RGB, ARGB_ACCENT = 0xFF000000 | ACCENT;
    private static final String SAVED_STONES = "MarsStones";

    private final StoneLedger stones = new StoneLedger(MarsTable.STONES);
    private final Window armor = new Window();
    private long lastGuardNote;

    public MarsBoss(EntityType<MarsBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), CHARACTER.armor() * 0.5);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    @Override protected ChatFormatting chatColor() { return ChatFormatting.LIGHT_PURPLE; }
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.1f : 1.0f; }
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }
    /** The stones make him a swordsman: he wants to be within a few blocks. */
    @Override protected double preferredRange(double suggested) { return Math.max(3, Math.min(8, suggested)); }
    /** Every intact stone adds a tenth to every blow of his. */
    @Override protected float damageScale() { return (float) stones.amplification(); }

    private boolean gates() { return gatesOpen(); }
    private Vec3 chest() { return position().add(0, getBbHeight() * 0.7, 0); }
    private Vec3 facing() { return EarlyMoves.flat(getViewVector(1f), new Vec3(0, 0, 1)); }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "More! The stones want more!";
        if (now >= 60 && old < 60) return "You are not what I was told to expect. Good.";
        if (now >= 40 && old < 40) return "A stone wakes. Let me show you what I am.";
        return "Orders are orders.";
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        stones.reset();
        armor.close();
        forgetMarks();
        announce("Mars has three mage stones set in his body; each adds a tenth to his damage. When one glows, hit him hard to crack it (anti-magic counts double).");
        announce("His Nemean Armor turns blows from the front. Walk round him.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        if (!won) stones.reset();                    // an abandoned fight resets him whole, like his health
        armor.close();
        super.onFightEnd(won);
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        for (int step : new int[]{40, 60, 80}) if (old < step && now >= step) surfaceStone();
    }

    @Override
    protected void onGatesOpened() {
        announce("The stones in Mars's body burn white. His crystal grows, and one more stone shows.");
        if (level() instanceof ServerLevel sl) VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, position(), position().add(0, 1, 0), ARGB_ACCENT, 40, 3.0f);
        later(70, EarlyMoves.safe("gates stone", this::surfaceStone));
    }

    /** One more stone shows on his body, if one is left and none is showing. */
    private void surfaceStone() {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        if (!stones.surface(sl.getGameTime(), MarsTable.STONE_WINDOW, MarsTable.stoneDamage(getMaxHealth()))) return;
        announce("A mage stone surfaces on Mars's chest and glows: hit him for " + Math.round(MarsTable.stoneDamage(getMaxHealth())) + " within "
                + MarsTable.STONE_WINDOW / 20 + " seconds to crack it!");
        VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, chest(), chest().add(0, 0.5, 0), ARGB_ACCENT, 24, 1.4f);
        EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.6f);
    }

    @Override
    protected void bossTick() {
        super.bossTick();
        if (!(level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (tickCount % 6 == 0) {
            if (stones.exposed(now)) Tells.gather(sl, chest(), 0.5, 0xFFFFFF, 7);
            if (armor.active(now)) {
                Vec3 f = facing();
                Tells.gather(sl, chest().add(f.scale(0.8)), 0.45, ACCENT, 5);
            }
        }
    }

    // ================================================================ the stones and the armor
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        try {
            if (level() instanceof ServerLevel sl && stones.exposed(sl.getGameTime())) {
                if (stones.hit(sl.getGameTime(), landed(source, amount), antiMagic(source)) == StoneLedger.Event.BROKEN) {
                    announce("A mage stone cracks and falls from Mars! His strength fails, and he staggers.");
                    say("The stone... it hurts!");
                    interruptCast();
                    forgetMarks();
                    stagger(MarsTable.STONE_STAGGER, MarsTable.STAGGER_BONUS);
                    VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, chest(), chest().add(0, 0.5, 0), ARGB_ACCENT, 28, 2.0f);
                    EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_BREAK, 2.5f, 0.7f);
                }
            }
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Mars's stones failed: {}", e.toString());
        }
        return super.filterDamage(source, amount);
    }

    /** The crystal over his front turns the blows that come from there. */
    @Override
    protected float mitigate(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            long now = sl.getGameTime();
            if (!armor.active(now)) return amount;
            Entity attacker = source.getEntity();
            Vec3 from = attacker != null ? attacker.position() : source.getSourcePosition();
            if (from == null) return amount;
            Vec3 to = from.subtract(position());
            double angle = EarlyGeometry.angleBetween(facing().x, facing().z, to.x, to.z);
            double factor = FrontGuard.factor(true, angle, antiMagic(source));
            if (factor < 1.0 && attacker instanceof LivingEntity who && now - lastGuardNote > 40) {
                lastGuardNote = now;
                EarlyMoves.tell(who, "message.nusmp.arc.early.mars.guarded");
                EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
            }
            return (float) (amount * factor);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Mars's armor failed: {}", e.toString());
            return amount;
        }
    }

    // ---- saved: the stones that were cracked stay cracked across a reload
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(SAVED_STONES, stones.intact());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(SAVED_STONES)) stones.restore(tag.getInt(SAVED_STONES));
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() { return buildAll(MarsTable.rows(), this::runOf, this::tellOf); }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case MarsTable.HARPE -> this::runHarpe;
            case MarsTable.SLASH -> this::runSlash;
            case MarsTable.LANCE -> this::runLance;
            case MarsTable.FIELD -> this::runField;
            case MarsTable.ARMOR -> this::runArmor;
            case MarsTable.RUSH -> this::runRush;
            case MarsTable.DOUBLES -> this::runDoubles;
            case MarsTable.NOVA -> this::runNova;
            case MarsTable.PUPPETS -> this::runPuppets;
            case MarsTable.TITAN -> this::runTitan;
            default -> (b, t) -> { };
        };
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case MarsTable.HARPE -> this::tellHarpe;
            case MarsTable.SLASH -> this::tellSlash;
            case MarsTable.LANCE -> (b, t, left, total) -> tellLane(t, left, total, MarsTable.LANCE_LENGTH, MarsTable.LANCE_HALF, 0.5);
            case MarsTable.FIELD -> (b, t, left, total) -> tellDisc(t, left, total, MarsTable.FIELD_RADIUS, 0.55, true);
            case MarsTable.ARMOR -> (b, t, left, total) -> {
                if (level() instanceof ServerLevel sl) {
                    Tells.ring(sl, position(), 1.8, ACCENT, 1.2f, 20);
                    Tells.gather(sl, chest(), 0.8, ACCENT, 6);
                }
            };
            case MarsTable.RUSH -> (b, t, left, total) -> tellLane(t, left, total, MarsTable.RUSH_LENGTH, MarsTable.RUSH_HALF, 0.5);
            case MarsTable.DOUBLES -> this::tellDoubles;
            case MarsTable.NOVA -> (b, t, left, total) -> tellRing(MarsTable.NOVA_RADIUS, left, total);
            case MarsTable.PUPPETS -> this::tellPuppets;
            case MarsTable.TITAN -> this::tellTitan;
            default -> null;
        };
    }

    /** A ring on the spot the target stood on when the wind-up began (followed for the first part, then locked and red). */
    private void tellDisc(LivingEntity t, int left, int total, double radius, double follow, boolean sigil) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, follow);
        boolean locked = (total - left) / (double) Math.max(1, total) >= follow;
        if (sigil && first(left, total)) VfxSpawn.send(sl, VfxShape.CRYSTAL_FX2, m, m.add(0, 1, 0), ARGB, total + 2, (float) radius);
        Tells.ring(sl, m, radius, locked ? WARN : ACCENT, 1.4f, (int) Math.min(72, 14 + radius * 5));
        Tells.ring(sl, m, Math.max(0.6, radius * left / Math.max(1, total)), RGB, 1.1f, 24);
        Tells.column(sl, m, 4, ACCENT);
    }

    private void tellRing(double radius, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), Math.max(1.5, radius * (total - left) / Math.max(1, total)), RGB, 1.3f, 40);
        if (left % 6 == 0) Tells.ring(sl, position(), radius, WARN, 1.1f, 48);
        Tells.gather(sl, position().add(0, 1, 0), 0.8, ACCENT, 4);
    }

    /** Two parallel dust lines along the aim, locked partway through the wind-up. */
    private void tellLane(LivingEntity t, int left, int total, double length, double half, double follow) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, follow);
        Vec3 dir = EarlyMoves.flat(m.subtract(position()), getViewVector(1f)), o = chest(), end = o.add(dir.scale(length));
        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(half);
        boolean locked = (total - left) / (double) Math.max(1, total) >= follow;
        Tells.line(sl, o.add(side), end.add(side), locked ? WARN : ACCENT);
        Tells.line(sl, o.subtract(side), end.subtract(side), locked ? WARN : ACCENT);
        Tells.gather(sl, o, 0.5, ACCENT, 3);
    }

    // ---------------------------------------------------------------- Harpe
    private void tellHarpe(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (Vec3 s : harpeStarts(MarsTable.harpe(tuning().castSlots()))) Tells.line(sl, s, t.getBoundingBox().getCenter(), RGB);
        Tells.gather(sl, chest(), 1.0, ACCENT, 6);
    }

    /** The blades manifest in an arc round his shoulders. */
    private List<Vec3> harpeStarts(int n) {
        List<Vec3> out = new ArrayList<>();
        Vec3 f = facing();
        for (double a : EarlyGeometry.fan(n, 2.4)) {
            double[] r = EarlyGeometry.rotate(-f.z, f.x, a);
            out.add(chest().add(r[0] * 1.4, 0.3, r[1] * 1.4).add(f.scale(0.6)));
        }
        return out;
    }

    private void runHarpe(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        int n = MarsTable.harpe(tuning().castSlots());
        List<Vec3> starts = harpeStarts(n);
        swing(InteractionHand.MAIN_HAND);
        for (int i = 0; i < n; i++) {
            final Vec3 from = starts.get(i);
            later(i * 2, EarlyMoves.safe("harpe blade", () -> {
                if (!isAlive()) return;
                Vec3 aim = t.isAlive() ? t.getBoundingBox().getCenter().subtract(from) : getViewVector(1f);
                if (aim.lengthSqr() < 1e-4) aim = getViewVector(1f);
                Vec3 dir = aim.normalize(), end = EarlyMoves.stopAtWall(sl, from, from.add(dir.scale(30)), this);
                VfxSpawn.send(sl, VfxShape.CRYSTAL_FX1, from, end, ARGB, (int) Math.max(6, from.distanceTo(end) / 1.5), 0.9f);
                EarlyMoves.sound(sl, from, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                EarlyMoves.bolt(this, from, dir, 1.5, 22, 0.6, MarsTable.HARPE_HIT, false,
                        (lv, where, victim) -> VfxSpawn.send(lv, VfxShape.CRYSTAL_FX3, where, where.add(0, 0.5, 0), ARGB, 10, 0.5f));
            }));
        }
        quip("Harpe.", "Every blade is aimed.");
    }

    // ---------------------------------------------------------------- Crystal Slash
    private void tellSlash(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 f = facing(), o = position().add(0, 1.0, 0);
        for (double a : new double[]{-MarsTable.SLASH_HALF_ANGLE, 0, MarsTable.SLASH_HALF_ANGLE}) {
            double[] r = EarlyGeometry.rotate(f.x, f.z, a);
            Tells.line(sl, o, o.add(r[0] * MarsTable.SLASH_REACH, 0, r[1] * MarsTable.SLASH_REACH), a == 0 ? WARN : RGB);
        }
        Tells.gather(sl, o, 0.6, ACCENT, 4);
    }

    private void runSlash(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        swing(InteractionHand.MAIN_HAND);
        Vec3 f = EarlyMoves.flat(t.position().subtract(position()), getViewVector(1f));
        Vec3 tip = position().add(f.scale(MarsTable.SLASH_REACH * 0.6)).add(0, 1.0, 0);
        VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, tip, tip.add(f), ARGB, 16, 0.9f);
        EarlyMoves.sound(sl, position(), SoundEvents.GLASS_BREAK, 1.0f, 1.3f);
        for (LivingEntity e : EarlyMoves.inCone(this, position(), f, MarsTable.SLASH_REACH, MarsTable.SLASH_HALF_ANGLE)) {
            if (strike(e, MarsTable.SLASH_HIT, false)) EarlyMoves.shove(e, position(), 0.9);
        }
        quip("Cut.", "Too slow.");
    }

    // ---------------------------------------------------------------- Crystal Lance
    private void runLance(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = chest(), dir = EarlyMoves.flat(takeMark(t).subtract(position()), getViewVector(1f));
        Vec3 to = EarlyMoves.stopAtWall(sl, from, from.add(dir.scale(MarsTable.LANCE_LENGTH)), this);
        VfxSpawn.send(sl, VfxShape.CRYSTAL_FX1, from, to, ARGB, 8, 1.7f);
        EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.6f, 1.2f);
        swing(InteractionHand.MAIN_HAND);
        for (LivingEntity e : EarlyMoves.inCorridor(this, from, to, MarsTable.LANCE_HALF)) {
            if (strike(e, MarsTable.LANCE_HIT, false)) EarlyMoves.shove(e, from, 0.6);
        }
        quip("Pierce.");
    }

    // ---------------------------------------------------------------- Crystal Field
    private void runField(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, m, m.add(0, 1, 0), ARGB, 26, 2.0f);
        EarlyMoves.sound(sl, m, SoundEvents.AMETHYST_BLOCK_BREAK, 2f, 0.8f);
        for (LivingEntity e : EarlyMoves.inCircle(this, m, MarsTable.FIELD_RADIUS)) {
            if (!strike(e, MarsTable.FIELD_HIT, false)) continue;
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.5, 0));
            e.hurtMarked = true;
        }
        quip("Rise.", "The ground is crystal now.");
    }

    // ---------------------------------------------------------------- Nemean Armor
    private void runArmor(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        armor.open(sl.getGameTime(), MarsTable.ARMOR_TICKS);
        VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, chest(), chest().add(facing()), ARGB, 24, 1.4f);
        EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.0f);
        announce("Mars cloaks his front in Nemean Armor: blows from the front are cut to 40%. Strike from the side or behind, or cut it with anti-magic.");
        quip("Try the front, then.");
    }

    // ---------------------------------------------------------------- Crystal Rush
    private void runRush(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dir = EarlyMoves.flat(takeMark(t).subtract(position()), getViewVector(1f));
        Vec3 end = EarlyMoves.stopAtWall(sl, chest(), chest().add(dir.scale(MarsTable.RUSH_LENGTH)), this);
        double len = Math.max(3, end.subtract(chest()).multiply(1, 0, 1).length()), speed = 1.7;
        VfxSpawn.send(sl, VfxShape.CRYSTAL_FX1, chest(), end, ARGB, 8, 1.2f);
        EarlyMoves.charge(this, dir, speed, (int) Math.ceil(len / speed), MarsTable.RUSH_HALF, MarsTable.RUSH_HIT, 1.0);
        quip("Through you.");
    }

    // ---------------------------------------------------------------- Crystal Doubles
    /** The corners of the polygon of fangs round the target; the first is turned away from Mars so the way back to him is open. */
    private List<Vec3> doublesAt(Vec3 m) {
        int n = MarsTable.doubles(PowerCurve.stage(power()));
        double phase = Math.atan2(position().z - m.z, position().x - m.x) + Math.PI / n;
        List<Vec3> out = new ArrayList<>();
        for (double[] p : EarlyGeometry.polygon(m.x, m.z, MarsTable.DOUBLES_RING, n, phase)) out.add(new Vec3(p[0], m.y, p[1]));
        return out;
    }

    private void tellDoubles(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.5);
        boolean locked = (total - left) / (double) Math.max(1, total) >= 0.5;
        for (Vec3 c : doublesAt(m)) {
            Vec3 floor = ground(sl, c);
            if (first(left, total)) VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, floor, floor.add(0, 1, 0), ARGB, total + 2, 0.6f);
            Tells.ring(sl, floor, MarsTable.DOUBLES_FANG, locked ? WARN : ACCENT, 1.3f, 24);
            Tells.column(sl, floor, 3, ACCENT);
        }
        Tells.ring(sl, m, 0.9, 0xFFFFFF, 1.0f, 12);
        if (first(left, total)) announce("Mars's doubles rise round " + t.getName().getString() + ": the middle of the corners is safe, and so is anywhere outside.");
    }

    private void runDoubles(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        List<Vec3> fangs = new ArrayList<>();
        for (Vec3 c : doublesAt(m)) fangs.add(ground(sl, c));
        EarlyMoves.batchStrike(this, fangs, MarsTable.DOUBLES_FANG, 1, MarsTable.DOUBLE_HIT, false, RGB, ACCENT,
                (lv, victim, centre) -> {
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.45, 0));
                    victim.hurtMarked = true;
                },
                (lv, centre) -> {
                    VfxSpawn.send(lv, VfxShape.CRYSTAL_FX3, centre, centre.add(0, 1, 0), ARGB, 24, 1.2f);
                    EarlyMoves.sound(lv, centre, SoundEvents.AMETHYST_BLOCK_BREAK, 1.4f, 1.0f);
                });
        quip("There are many of me.", "Which one is real?");
    }

    // ---------------------------------------------------------------- Shatter Nova
    private void runNova(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, position(), position().add(0, 1, 0), ARGB, 28, 2.4f);
        EarlyMoves.sound(sl, position(), SoundEvents.GLASS_BREAK, 2f, 0.7f);
        EarlyMoves.ringWave(this, position(), MarsTable.NOVA_RADIUS, MarsTable.NOVA_HIT, 1.2, false, RGB);
        quip("Shatter.", "Jump!");
    }

    // ---------------------------------------------------------------- Talos Puppet Cluster
    private void tellPuppets(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        int n = 0;
        for (LivingEntity e : foes(30)) {
            if (++n > MarsTable.puppets(gates())) break;
            Tells.line(sl, chest(), e.getBoundingBox().getCenter(), RGB);
        }
        Tells.gather(sl, chest(), 1.0, ACCENT, 6);
    }

    private void runPuppets(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        List<Vec3> starts = new ArrayList<>();
        int n = MarsTable.puppets(gates());
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + getRandom().nextDouble();
            Vec3 p = ground(sl, position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5)).add(0, 0.9, 0);
            starts.add(p);
            VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, p, p.add(0, 1, 0), ARGB, 20, 0.7f);
        }
        EarlyMoves.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.7f);
        EarlyMoves.wisps(this, starts, 0.22, 90, MarsTable.PUPPET_BLAST, MarsTable.PUPPET_HIT, false, ACCENT, null,
                (lv, where) -> {
                    VfxSpawn.send(lv, VfxShape.CRYSTAL_FX3, where, where.add(0, 1, 0), ARGB, 20, 1.0f);
                    EarlyMoves.sound(lv, where, SoundEvents.GLASS_BREAK, 1.2f, 1.1f);
                });
        quip("March.", "The puppets do not tire.");
    }

    // ---------------------------------------------------------------- Heavy Armored Titan (the ultimate)
    private void tellTitan(ZogratisBoss b, LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = aimMark(t, left, total, 0.4);
        boolean locked = (total - left) / (double) Math.max(1, total) >= 0.4;
        if (first(left, total)) {
            announce("Mars raises the Heavy Armored Titan: leave the circle before it falls, then jump the two rings that run out of it!");
            VfxSpawn.send(sl, VfxShape.CRYSTAL_FX2, m, m.add(0, 1, 0), ARGB, total + 6, (float) MarsTable.TITAN_RADIUS);
            EarlyMoves.sound(sl, position(), SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        }
        Tells.ring(sl, m, MarsTable.TITAN_RADIUS, locked ? WARN : ACCENT, 1.5f, 72);
        Tells.ring(sl, m, Math.max(1.0, MarsTable.TITAN_RADIUS * left / Math.max(1, total)), RGB, 1.2f, 32);
        Tells.column(sl, m, 6, ACCENT);
        Tells.gather(sl, chest(), 1.2, ACCENT, 8);
    }

    private void runTitan(ZogratisBoss b, LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 m = takeMark(t);
        VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, m, m.add(0, 1, 0), ARGB, 32, 4.5f);
        EarlyMoves.sound(sl, m, SoundEvents.WITHER_SPAWN, 3f, 0.6f);
        for (LivingEntity e : EarlyMoves.inCircle(this, m, MarsTable.TITAN_RADIUS)) {
            if (strike(e, MarsTable.TITAN_HIT, false)) EarlyMoves.shove(e, m, 1.4);
        }
        for (int k = 1; k <= 2; k++) {
            final int wave = k;
            later(wave * MarsTable.TITAN_GAP, EarlyMoves.safe("titan ring", () -> {
                VfxSpawn.send(sl, VfxShape.CRYSTAL_FX3, m, m.add(0, 1, 0), ARGB, 20, 2.5f);
                EarlyMoves.ringWave(this, m, MarsTable.TITAN_RINGS, MarsTable.TITAN_AFTER, 0.9, false, wave == 2 ? WARN : RGB);
            }));
        }
        say("The Heavy Armored Titan!");
    }
}
