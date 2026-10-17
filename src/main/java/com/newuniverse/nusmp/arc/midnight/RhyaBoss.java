package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.TowerMath;
import com.newuniverse.nusmp.entity.zogratis.VolleyBudget;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.wizard.BornMagic;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Rhya is the Disloyal of the Third Eye, an elf in a human body whose Copy Magic makes another mage's grimoire his own; the story shows him throwing
 * Yami's dark slash, a water javelin, Fana's fire and Patolli's swords as if he had always known them, and vanishing to strike from behind. The fight is a
 * mirror held up to the party, and three mechanics belong only to him.
 * <ul>
 *   <li><b>Copy Magic</b> ({@link ImitationMemory}): the magic that hits him, or the magic of the grimoire he touches, becomes the one he answers with, in its
 *       colours and with its rider, and a magic he knows hurts him less (a blow of it deals 70%) for twenty-four seconds. The colour round him tells the party
 *       what he holds. The answer is to change what you throw: a sword, a skill or another magic is not copied, and alternating two magics beats a memory of one.</li>
 *   <li><b>The Invisible Scout</b>: he vanishes and appears behind a foe, but the ring where he will appear is drawn on the floor first, and any blow shows
 *       him at once.</li>
 *   <li><b>The Third Eye</b> ({@link ThirdEyeBond}, the Gates): his eye opens at full power and he holds two stolen magics at once, and when Fana or Vetto
 *       falls he rages with the rest of the trio.</li>
 * </ul>
 * Everything he copies from a grimoire is drawn from the mod's own palette and shapes (the Tower tables for the bolt, the lance and the nova, the mod's Avidya
 * slash, water javelin, seal chains and light blade effects for the story's copies). He changes no block, heals nothing, and holds a foe only through the
 * balance law's shared control rule.
 */
public class RhyaBoss extends MidnightBoss {
    private static final int VIOLET = 0xB8A0FF, GOLD = 0xFFD35A, DARK = 0x7A3FB0, WATER = 0x4FA8FF, FIRE = 0xFF7A2A, LIGHT = 0xFFE8A0;
    /** The ring of his touch: his reach, and a little more so the drawn edge is the struck edge. */
    private static final double TOUCH_RING = RhyaMath.TOUCH_REACH + 0.4;
    private static final String K_BOOK = "nusmp_copy_book", K_AT = "nusmp_copy_at";

    public static final ArcCharacter CHARACTER = ArcCharacter.boss("rhya", "Rhya")
            .title("the Disloyal of the Third Eye").source("Black Clover").saga(ArcSaga.ELF_REINCARNATION).tier(2)
            .colors(0x8E7BE0, 0xFFD35A).magic("copy").stats(1450, 12, 0.34, 5)
            .floors(Polarity.ABYSSAL, 40, 65)
            .themes("vampire_castle", "spatial", "shadow", "cursed_archive", "feywild_grove")
            .after("vetto", "fana")
            .charter(0.25, "analytical_appraisal", "analyze", "invisible", "magic_sense", "heavenly_eye", "thought_acceleration", "spatial_motion",
                    "water_blade", "shadow_bind")
            .lines("Such a fine grimoire. Lend it to me... I only ever borrow things.",
                    "You wanted to see what hides under my hair? Look, then. It sees you back.",
                    "A thousand spells I took, and not one of them was ever mine to keep.")
            .build();

    public static final DeferredHolder<EntityType<?>, EntityType<RhyaBoss>> TYPE = ArcEntities.boss("rhya", RhyaBoss::new, 0.55f, 2.0f,
            new ArcLook("rhya", 2.0f, 1.21f, 2.15f, 0.44f, 1.12f, 0.79f, true), () -> attributesOf(CHARACTER, 2.0));

    static {
        ThirdEyeBond.install();
    }

    private final ImitationMemory memory = new ImitationMemory(RhyaMath.memoryCapacity(false));
    /** The magic the move being wound up will answer with (chosen by its first tell, read by its strike), and the tick it was chosen. */
    private MagicType pending;
    private long pendingAt;
    private int salt, bookSalt;
    private String cachedBook;
    private MagicType cachedMagic;
    private long invisUntil;
    private boolean memoryHinted, touchHinted, scoutHinted;

    public RhyaBoss(EntityType<RhyaBoss> type, Level level) { super(type, level); }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ looks and numbers
    @Override protected String word() { return "Imitation"; }

    @Override protected String gatesWord() { return "Third Eye"; }

    @Override protected String gatesAnnouncement() { return "Rhya's Third Eye opens: he holds two stolen magics at once!"; }

    @Override protected ChatFormatting chatColor() { return ChatFormatting.LIGHT_PURPLE; }

    /** A fighter of the middle distance who closes in to touch and slips away again. */
    @Override protected double preferredRange(double suggested) { return Math.max(4.0, Math.min(12.0, suggested)); }

    // ================================================================ words
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "I have learned all of you by now.";
        if (now >= 60 && old < 60) return "Every spell you show me becomes mine.";
        if (now >= 40 && old < 40) return "Show me another. I like collecting.";
        return "I borrow " + now + "% of your strength.";
    }

    @Override
    protected void fightBegins() {
        memory.clear();
        memory.setCapacity(RhyaMath.memoryCapacity(false));
        pending = null;
        salt = 0;
        invisUntil = 0;
        memoryHinted = false;
        touchHinted = false;
        scoutHinted = false;
        announce("Rhya copies the magic that hits him and answers in its colours, and a magic he knows hurts him less. Change what you throw: a sword, a skill or another magic is not copied.");
    }

    @Override
    protected void fightEnds(boolean won) {
        reveal();
        memory.clear();
        pending = null;
    }

    @Override
    protected void onGatesOpened() {
        memory.setCapacity(RhyaMath.memoryCapacity(true));
        if (level() instanceof ServerLevel sl) {
            Vec3 eye = position().add(0, getBbHeight() + 0.3, 0);
            fx(VfxShape.EYE_FX3, eye, eye.add(0, 1, 0), GOLD, 50, 1.6f);
            MidnightFx.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5f, 0.4f);
        }
        announce("The Third Eye under his hair is open: Rhya holds two stolen magics at once, so alternating two of yours no longer beats his memory.");
    }

    // ================================================================ Copy Magic: what hits him is learned
    /**
     * A blow of a grimoire page teaches him its magic. The page stamps the target ({@code CopyMemory}, before it hurts) with the book it came from; a blow
     * of anything else (a weapon, a skill of another kind) carries no stamp and is neither learned nor resisted. The first blow of a new magic lands whole,
     * every later one of a magic he still remembers is cut to 70%.
     */
    @Override
    protected float onIncoming(DamageSource source, float amount) {
        if (invisUntil != 0) reveal();                                         // any blow shows him
        MagicType m = hitMagic();
        if (m == null) return amount;
        double factor = memory.factorAgainst(m.name(), tickCount);
        boolean fresh = !memory.knows(m.name(), tickCount);
        memory.learn(m.name(), tickCount);
        if (fresh) learned(m);
        return (float) (amount * factor);
    }

    /** The magic of the grimoire page that is hitting him this very tick, or null for a blow of any other kind. */
    private MagicType hitMagic() {
        CompoundTag d = getPersistentData();
        if (!d.contains(K_BOOK) || level().getGameTime() - d.getLong(K_AT) > 1) return null;
        String id = d.getString(K_BOOK);
        if (!id.equals(cachedBook)) {
            cachedBook = id;
            cachedMagic = magicOfBook(id);
        }
        return cachedMagic;
    }

    private static MagicType magicOfBook(String id) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            var skill = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            return skill instanceof GrimoireBook b ? b.magicNow() : null;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** The magic of the grimoire a player carries, or null (not a player, no grimoire, or the book could not be read). */
    private static MagicType grimoireMagic(LivingEntity e) {
        if (!(e instanceof Player p)) return null;
        try {
            return GrimoirePages.grimoireOf(p).map(i -> i.getSkill() instanceof GrimoireBook b ? b.magicNow() : null).orElse(null);
        } catch (RuntimeException | LinkageError ex) {
            return null;
        }
    }

    private static int rgbOf(MagicType m) { return BornMagic.colorOf(m) & 0xFFFFFF; }

    /** He has taken a magic: the party reads which, in the colour of it, and learns what it costs them. */
    private void learned(MagicType m) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 c = position().add(0, getBbHeight() + 0.4, 0);
        fx(VfxShape.IMITATION_FX2, c, c.add(0, 1, 0), rgbOf(m), 24, 1.2f);
        MidnightFx.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, 0.7f);
        MidnightFx.bar(sl, position(), 56, Component.literal("Rhya copied " + m.displayName + ": it hurts him less for " + MidnightText.seconds(ImitationMemory.WINDOW) + " s")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        if (!memoryHinted) {
            memoryHinted = true;
            announce("Rhya has copied " + m.displayName + ". He answers in its colours now, and blows of it deal 70% for 24 seconds: use another magic, a weapon or a skill.");
        }
    }

    /** The magic a move answers with: the one he holds in {@code slot} (0 the newest), or, holding none, the next of the four from the story. */
    private MagicType chooseMagic(int slot) {
        List<String> known = memory.known(tickCount);
        if (!known.isEmpty()) {
            MagicType m = MagicType.lookup(known.get(Math.min(Math.max(0, slot), known.size() - 1)));
            if (m != null) return m;
        }
        MagicType d = MagicType.lookup(RhyaMath.defaultFor(salt++));
        return d != null ? d : MagicType.FLAME;
    }

    /** The first tell of a move picks its magic and names it on the action bar; later tells (and the strike) keep it. */
    private MagicType pend(int slot, String label) {
        if (pending == null || tickCount - pendingAt > 60) {
            pending = chooseMagic(slot);
            pendingAt = tickCount;
            if (level() instanceof ServerLevel sl) {
                MidnightFx.bar(sl, position(), 56, Component.literal(label + ": " + pending.displayName).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return pending;
    }

    /** The magic the strike uses: the one the tell chose, or, if no tell ran, a fresh choice. */
    private MagicType taken(int slot) {
        MagicType m = pending != null && tickCount - pendingAt <= 120 ? pending : chooseMagic(slot);
        pending = null;
        return m;
    }

    /** Second slot for the moves that go off after the first at the Gates. */
    private int secondSlot() { return gatesOpen() ? 1 : 0; }

    // ================================================================ every tick
    @Override
    protected void tickMidnight() {
        if (!(level() instanceof ServerLevel sl)) return;
        int cap = RhyaMath.memoryCapacity(gatesOpen());
        if (memory.capacity() != cap) memory.setCapacity(cap);
        if (invisUntil != 0 && tickCount >= invisUntil) reveal();
        if (tickCount % 10 == 0 && isAlive()) {                                  // the colour he holds, readable from across the arena
            List<String> known = memory.known(tickCount);
            for (int i = 0; i < known.size(); i++) {
                MagicType m = MagicType.lookup(known.get(i));
                if (m != null) Tells.gather(sl, position().add(0, 1.0 + 0.5 * i, 0), 0.7, rgbOf(m), 2);
            }
        }
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MidnightSpec s : RhyaTable.specs()) out.add(build(s, tellOf(s.id()), runOf(s.id())));
        return out;
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case RhyaTable.COPY_TOUCH -> (b, t, left, total) -> tellTouch(t, left, total);
            case RhyaTable.BOLTS -> (b, t, left, total) -> tellBolts(t, left);
            case RhyaTable.LANCE -> (b, t, left, total) -> tellBeam(t, left, "Imitation: Lance", RhyaMath.LANCE_LENGTH, secondSlot(), true);
            case RhyaTable.AVIDYA -> (b, t, left, total) -> tellAvidya(t, left);
            case RhyaTable.JAVELIN -> (b, t, left, total) -> tellBeam(t, left, "Aqua Javelin", RhyaMath.JAVELIN_LENGTH, 0, false);
            case RhyaTable.SCOUT -> (b, t, left, total) -> tellScout(t, left, total);
            case RhyaTable.NOVA -> (b, t, left, total) -> tellNova(t, left, total);
            case RhyaTable.SEAL -> (b, t, left, total) -> tellSeal(t, left, total);
            case RhyaTable.SALAMANDER_COPY -> (b, t, left, total) -> tellBreath(t, left);
            case RhyaTable.LIGHT_COPY -> (b, t, left, total) -> tellLight(t, left);
            case RhyaTable.SPELLBOOK -> (b, t, left, total) -> tellBook(left, total);
            default -> null;
        };
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case RhyaTable.COPY_TOUCH -> (b, t) -> runTouch(t);
            case RhyaTable.BOLTS -> (b, t) -> runBolts(t);
            case RhyaTable.LANCE -> (b, t) -> runLance(t);
            case RhyaTable.AVIDYA -> (b, t) -> runAvidya(t);
            case RhyaTable.JAVELIN -> (b, t) -> runJavelin(t);
            case RhyaTable.SCOUT -> (b, t) -> runScout(t);
            case RhyaTable.NOVA -> (b, t) -> runNova(t);
            case RhyaTable.SEAL -> (b, t) -> runSeal(t);
            case RhyaTable.SALAMANDER_COPY -> (b, t) -> runBreath(t);
            case RhyaTable.LIGHT_COPY -> (b, t) -> runLight(t);
            case RhyaTable.SPELLBOOK -> (b, t) -> runBook(t);
            default -> (b, t) -> { };
        };
    }

    private Vec3 flatAimAt(LivingEntity t) { return MidnightFx.flatDir(position(), t.position(), getViewVector(1f)); }

    private Vec3 lockedAim(LivingEntity t) { return aim.lengthSqr() > 1e-4 ? aim.multiply(1, 0, 1).normalize() : flatAimAt(t); }

    /** The point on his body a flat breath leaves from. */
    private Vec3 mouth(Vec3 dir) { return position().add(0, 1.3, 0).add(dir.multiply(1, 0, 1).normalize().scale(0.8)); }

    // ---------------------------------------------------------------- Copy Magic: Touch
    private void tellTouch(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
            Vec3 stop = d.length() > RhyaMath.TOUCH_REACH ? ground(t.position().subtract(d.normalize().scale(RhyaMath.TOUCH_REACH * 0.8))) : position();
            marks = List.of(stop);
            if (!touchHinted) {
                touchHinted = true;
                announce("Rhya reaches for your grimoire: Copy Magic. If he touches it he learns your magic and resists it for 24 seconds. Leave the ring.");
            }
        }
        if (marks.isEmpty()) return;
        Vec3 end = marks.get(0);
        Tells.line(sl, position().add(0, 1, 0), end.add(0, 1, 0), VIOLET);
        Tells.ring(sl, end, TOUCH_RING, MidnightFx.WARN, 1.3f, 32);
        Tells.gather(sl, hand(), 0.5, GOLD, 4);
    }

    private void runTouch(LivingEntity t) {
        Vec3 end = marks.isEmpty() ? ground(t.position()) : marks.get(0);
        marks = List.of();
        fx(VfxShape.MIRROR_STEP, position(), position(), VIOLET, 12, 1.8f);
        Vec3 at = blink(end) ? end : position();
        swing(InteractionHand.MAIN_HAND);
        MagicType got = null;
        LivingEntity touched = null;
        for (LivingEntity e : MidnightFx.inRing(this, at, TOUCH_RING)) {
            if (!MidnightFx.hit(this, e, RhyaTable.TOUCH, true)) continue;
            MidnightFx.shove(e, at, 0.5, 0.1);
            if (touched == null || got == null) {
                MagicType here = grimoireMagic(e);
                if (touched == null || here != null) {
                    touched = e;
                    got = here;
                }
            }
        }
        if (touched != null) {
            MagicType m = got != null ? got : MagicType.lookup(RhyaMath.defaultFor(salt++));      // a foe with no grimoire: he lifts one of the four from the story
            if (m == null) m = MagicType.FLAME;
            memory.learn(m.name(), tickCount);
            learned(m);
            MidnightFx.rider(m.hitEffect(), touched, this);
        }
        MidnightFx.sound(level(), at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, 1.5f);
        quip("Thank you.", "I will borrow this.", "Such a pretty page.");
    }

    // ---------------------------------------------------------------- Imitation: Bolts
    private void tellBolts(LivingEntity t, int left) {
        if (!(level() instanceof ServerLevel sl)) return;
        MagicType m = pend(0, "Imitation: Bolts");
        Vec3 from = hand();
        if (left > RhyaMath.BOLT_LOCK || aim.lengthSqr() < 1e-4) aim = t.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        Vec3 dir = aim.normalize();
        int color = left <= RhyaMath.BOLT_LOCK ? MidnightFx.WARN : rgbOf(m);
        for (double a : MidnightMath.fan(RhyaMath.boltCount(power()), RhyaMath.BOLT_SPREAD)) {
            Tells.line(sl, from, MidnightFx.clipWall(sl, this, from, from.add(MidnightFx.rotateY(dir, a).scale(30))), color);
        }
        Tells.gather(sl, from, 0.6, rgbOf(m), 4);
    }

    private void runBolts(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        final MagicType m = taken(0);
        final int rgb = rgbOf(m);
        final Vec3 from = hand();
        final Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : t.getBoundingBox().getCenter().subtract(from).normalize();
        final VolleyBudget budget = new VolleyBudget(RhyaMath.BOLT_HITS_PER_FOE);
        int n = RhyaMath.boltCount(power());
        double[] fan = MidnightMath.fan(n, RhyaMath.BOLT_SPREAD);
        for (int i = 0; i < n; i++) {
            final Vec3 d = MidnightFx.rotateY(dir, fan[i]);
            Runnable fire = MidnightFx.safe("copied bolt", () -> {
                fx(VfxShape.IMITATION_FX1, from, MidnightFx.clipWall(sl, this, from, from.add(d.scale(RhyaMath.BOLT_SPEED * RhyaMath.BOLT_LIFE))), rgb, 14, 0.9f);
                MidnightBolt.launch(this, from, d.scale(RhyaMath.BOLT_SPEED), RhyaMath.BOLT_LIFE, RhyaMath.BOLT_RADIUS, rgb, false, budget,
                        (bolt, v) -> {
                            if (MidnightFx.hit(this, v, TowerMath.baseDamage(TowerMath.Shape.BOLT), true)) MidnightFx.rider(m.hitEffect(), v, this);
                        }, null, null);
            });
            if (i == 0) fire.run();
            else later(i * 2, fire);
        }
        MidnightFx.sound(sl, from, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 1.2f);
        quip("Yours, I think.", "Here is your spell back.");
    }

    // ---------------------------------------------------------------- a beam along a line: Imitation: Lance and Aqua Javelin
    /** The line follows the foe until {@link RhyaMath#LANCE_LOCK} ticks are left, then freezes in the warning colour. */
    private void tellBeam(LivingEntity t, int left, String label, double length, int slot, boolean copied) {
        if (!(level() instanceof ServerLevel sl)) return;
        int base = copied ? rgbOf(pend(slot, label)) : WATER;               // the lance wears the colours of the magic he holds; the javelin is always water
        Vec3 from = hand();
        if (left > RhyaMath.LANCE_LOCK || aim.lengthSqr() < 1e-4) aim = t.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(aim.normalize().scale(length)));
        boolean locked = left <= RhyaMath.LANCE_LOCK;
        ZogratisVfx.ray(sl, from, end, locked ? 0.9 : 0.4, 8, locked ? MidnightFx.WARN : base, true);
        Tells.line(sl, from, end, locked ? MidnightFx.WARN : base);
    }

    private void runLance(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        final MagicType m = taken(secondSlot());
        final int rgb = rgbOf(m);
        Vec3 from = hand();
        Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : t.getBoundingBox().getCenter().subtract(from).normalize();
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(dir.scale(RhyaMath.LANCE_LENGTH)));
        ZogratisVfx.ray(sl, from, end, 1.6, 10, rgb, false);
        fx(VfxShape.IMITATION_FX2, from, end, rgb, 12, 1.2f);
        MidnightFx.sound(sl, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 1.6f);
        for (LivingEntity e : MidnightFx.inLane(this, from, end, RhyaMath.LANCE_HALF_WIDTH)) {
            if (MidnightFx.hit(this, e, TowerMath.baseDamage(TowerMath.Shape.LANCE), true)) MidnightFx.rider(m.hitEffect(), e, this);
        }
    }

    private void runJavelin(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = hand();
        Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : t.getBoundingBox().getCenter().subtract(from).normalize();
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(dir.scale(RhyaMath.JAVELIN_LENGTH)));
        fx(VfxShape.WATER_JAVELIN, from, end, WATER, 10, 1.4f);
        MidnightFx.sound(sl, from, SoundEvents.TRIDENT_THROW.value(), 1.6f, 0.9f);
        for (LivingEntity e : MidnightFx.inLane(this, from, end, RhyaMath.JAVELIN_HALF_WIDTH)) {
            if (MidnightFx.hit(this, e, RhyaTable.JAVELIN_HIT, true)) MidnightFx.shove(e, from, 0.8, 0.1);
        }
        quip("A good one. Yami's, was it?", "Water... why not.");
    }

    // ---------------------------------------------------------------- Dark Cloaked Avidya Slash (copied from Yami)
    private void tellAvidya(LivingEntity t, int left) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (left > 8 || aim.lengthSqr() < 1e-4) aim = flatAimAt(t);
        Vec3 dir = aim.multiply(1, 0, 1).normalize();
        Vec3 from = position().add(0, 0.2, 0);
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(dir.scale(RhyaMath.AVIDYA_LENGTH)));
        Vec3 side = MidnightFx.rotateY(dir, Math.PI / 2).scale(RhyaMath.AVIDYA_HALF_WIDTH);
        int color = left <= 8 ? MidnightFx.WARN : DARK;
        Tells.line(sl, from.add(side), end.add(side), color);
        Tells.line(sl, from.subtract(side), end.subtract(side), color);
        Tells.line(sl, end.add(side), end.subtract(side), color);
        Tells.gather(sl, hand(), 0.5, DARK, 4);
    }

    private void runAvidya(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 dir = lockedAim(t);
        Vec3 from = position().add(0, 0.2, 0);
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(dir.scale(RhyaMath.AVIDYA_LENGTH)));
        fx(VfxShape.DARK_SLASH_AVIDYA, hand(), hand().add(dir.scale(RhyaMath.AVIDYA_LENGTH)), DARK, 16, 3.4f);
        MidnightFx.sound(sl, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.8f, 0.5f);
        MidnightFx.sound(sl, position(), SoundEvents.WITHER_SHOOT, 0.8f, 0.6f);
        swing(InteractionHand.MAIN_HAND);
        for (LivingEntity e : MidnightFx.inLane(this, from, end, RhyaMath.AVIDYA_HALF_WIDTH)) {
            if (MidnightFx.hit(this, e, RhyaTable.AVIDYA_HIT, true)) MidnightFx.shove(e, position(), 0.7, 0.15);
        }
        quip("Yami's blade. It cuts well.");
    }

    // ---------------------------------------------------------------- Invisible Scout
    private void tellScout(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            Vec3 face = t.getViewVector(1f).multiply(1, 0, 1);
            double[] b = MidnightMath.behind(t.getX(), t.getZ(), face.x, face.z, RhyaMath.SCOUT_BEHIND);
            Vec3 spot = ZogratisBoss.groundOrNull(level(), new Vec3(b[0], t.getY(), b[1]));
            marks = List.of(spot != null ? spot : ground(t.position()));
            if (!scoutHinted) {
                scoutHinted = true;
                announce("Rhya is about to vanish. The ring on the ground is where he will appear: step out of it. Any blow shows him at once.");
            }
        }
        if (left <= 12 && invisUntil == 0) vanish();
        if (marks.isEmpty()) return;
        Vec3 end = marks.get(0);
        Tells.ring(sl, end, RhyaMath.SCOUT_RADIUS, MidnightFx.WARN, 1.3f, 40);
        Tells.ring(sl, end, Math.max(0.6, RhyaMath.SCOUT_RADIUS * left / Math.max(1, total)), VIOLET, 1.1f, 24);
        Tells.column(sl, end, 3, GOLD);
    }

    private void vanish() {
        invisUntil = tickCount + 60;
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
        fx(VfxShape.MIRROR_STEP, position(), position(), VIOLET, 14, 1.8f);
    }

    /** He shows himself again: the effect ends, whether the scout landed, a blow found him, the time ran out or the fight ended. */
    private void reveal() {
        invisUntil = 0;
        if (hasEffect(MobEffects.INVISIBILITY)) removeEffect(MobEffects.INVISIBILITY);
    }

    private void runScout(LivingEntity t) {
        Vec3 end = marks.isEmpty() ? ground(t.position()) : marks.get(0);
        marks = List.of();
        Vec3 at = blink(end) ? end : position();
        reveal();
        fx(VfxShape.MIRROR_STEP, at, at, VIOLET, 14, 2.0f);
        MidnightFx.sound(level(), at, SoundEvents.PLAYER_ATTACK_STRONG, 1.6f, 1.4f);
        swing(InteractionHand.MAIN_HAND);
        for (LivingEntity e : MidnightFx.inRing(this, at, RhyaMath.SCOUT_RADIUS)) {
            if (MidnightFx.hit(this, e, RhyaTable.SCOUT_HIT, false)) MidnightFx.shove(e, at, 0.9, 0.2);
        }
        quip("Behind you.", "Did you lose me?");
    }

    // ---------------------------------------------------------------- Imitation: Nova
    private void tellNova(LivingEntity t, int left, int total) {
        MagicType m = pend(secondSlot(), "Imitation: Nova");
        if (first(left, total)) marks = spotsAround(t, RhyaMath.novaCount(power()), 4.0);
        drawMarks(marks, RhyaMath.NOVA_RADIUS, left, total, rgbOf(m));
    }

    private void runNova(LivingEntity t) {
        final MagicType m = taken(secondSlot());
        final int rgb = rgbOf(m);
        List<Vec3> spots = marks.isEmpty() ? List.of(ground(t.position())) : List.copyOf(marks);
        marks = List.of();
        for (Vec3 c : spots) {
            fx(VfxShape.IMITATION_FX3, c, c.add(0, 1, 0), rgb, 18, (float) (RhyaMath.NOVA_RADIUS * 0.5));
            for (LivingEntity e : MidnightFx.inRing(this, c, RhyaMath.NOVA_RADIUS)) {
                if (MidnightFx.hit(this, e, TowerMath.baseDamage(TowerMath.Shape.NOVA), true)) {
                    MidnightFx.rider(m.hitEffect(), e, this);
                    MidnightFx.shove(e, c, 0.6, 0.2);
                }
            }
        }
        MidnightFx.sound(level(), position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.8f);
    }

    // ---------------------------------------------------------------- Seal Magic: Chains
    private void tellSeal(LivingEntity t, int left, int total) {
        if (first(left, total)) marks = spotsAround(t, RhyaMath.sealCount(power()), 5.0);
        drawMarks(marks, RhyaMath.SEAL_RADIUS, left, total, VIOLET);
    }

    private void runSeal(LivingEntity t) {
        List<Vec3> spots = marks.isEmpty() ? List.of(ground(t.position())) : List.copyOf(marks);
        marks = List.of();
        boolean gate = MidnightFx.controlAllowed(this);
        for (Vec3 c : spots) {
            fx(VfxShape.SEAL_CHAINS, c, c, VIOLET, 30, (float) RhyaMath.SEAL_RADIUS);
            for (LivingEntity e : MidnightFx.inRing(this, c, RhyaMath.SEAL_RADIUS)) {
                if (MidnightFx.hit(this, e, RhyaTable.SEAL_HIT, true) && gate) MidnightFx.root(e, 25);
            }
        }
        MidnightFx.sound(level(), position(), SoundEvents.CHAIN_PLACE, 2f, 0.6f);
        quip("Stay.", "Do hold still.");
    }

    // ---------------------------------------------------------------- Copied: Salamander's Breath (Fana's fire)
    private void tellBreath(LivingEntity t, int left) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (left > 8 || aim.lengthSqr() < 1e-4) aim = flatAimAt(t);
        MidnightFx.coneTell(sl, position().add(0, 0.2, 0), aim, RhyaMath.COPY_BREATH_REACH, RhyaMath.COPY_BREATH_HALF_ANGLE, left <= 8 ? MidnightFx.WARN : FIRE);
        Tells.gather(sl, position().add(0, 1.2, 0), 0.5, FIRE, 4);
    }

    private void runBreath(LivingEntity t) {
        Vec3 dir = lockedAim(t);
        Vec3 from = mouth(dir);
        fx(VfxShape.FLAME_TRAIL, from, from.add(dir.scale(RhyaMath.COPY_BREATH_REACH)), FIRE, 14, 1.5f);
        MidnightFx.sound(level(), from, SoundEvents.BLAZE_SHOOT, 1.8f, 0.7f);
        for (LivingEntity e : MidnightFx.inCone(this, position(), dir, RhyaMath.COPY_BREATH_REACH, RhyaMath.COPY_BREATH_HALF_ANGLE)) {
            if (MidnightFx.hit(this, e, RhyaTable.BREATH_HIT, true)) {
                e.igniteForSeconds(2);
                MidnightFx.shove(e, position(), 0.5, 0);
            }
        }
        quip("Fana's fire. She would be furious.");
    }

    // ---------------------------------------------------------------- Copied: Light Sword of Judgment (Patolli's swords)
    private void tellLight(LivingEntity t, int left) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = hand();
        if (left > RhyaMath.LIGHT_COPY_LOCK || aim.lengthSqr() < 1e-4) aim = t.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        Vec3 dir = aim.normalize();
        int color = left <= RhyaMath.LIGHT_COPY_LOCK ? MidnightFx.WARN : LIGHT;
        for (double a : MidnightMath.fan(RhyaMath.lightCopyBlades(power()), RhyaMath.LIGHT_COPY_SPREAD)) {
            Tells.line(sl, from, MidnightFx.clipWall(sl, this, from, from.add(MidnightFx.rotateY(dir, a).scale(32))), color);
        }
        Tells.gather(sl, from, 0.6, LIGHT, 4);
    }

    private void runLight(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        final Vec3 from = hand();
        final Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : t.getBoundingBox().getCenter().subtract(from).normalize();
        final VolleyBudget budget = new VolleyBudget(2);
        int n = RhyaMath.lightCopyBlades(power());
        double[] fan = MidnightMath.fan(n, RhyaMath.LIGHT_COPY_SPREAD);
        for (int i = 0; i < n; i++) {
            final Vec3 d = MidnightFx.rotateY(dir, fan[i]);
            Runnable fire = MidnightFx.safe("copied sword", () -> {
                fx(VfxShape.LIGHT_BLADE, from, MidnightFx.clipWall(sl, this, from, from.add(d.scale(48))), LIGHT, 10, 1.0f);
                MidnightBolt.launch(this, from, d.scale(RhyaMath.LIGHT_COPY_SPEED), 14, 0.55, LIGHT, false, budget,
                        (bolt, v) -> {
                            if (MidnightFx.hit(this, v, RhyaTable.LIGHT_HIT, true)) fx(VfxShape.LIGHT_FLARE, v.position().add(0, 1, 0), v.position().add(0, 1, 0), LIGHT, 12, 0.7f);
                        }, null, null);
            });
            if (i == 0) fire.run();
            else later(i, fire);
        }
        MidnightFx.sound(sl, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.9f);
        quip("Patolli's light. Quite fast, isn't it?");
    }

    // ---------------------------------------------------------------- Stolen Spellbook (the ultimate)
    /** The magic of the {@code i}-th salvo: the magics he holds first, then the four from the story, so the salvos never share one colour for long. */
    private MagicType bookMagic(int i) {
        List<String> known = memory.known(tickCount);
        String key = i < known.size() ? known.get(i) : RhyaMath.defaultFor(bookSalt + i);
        MagicType m = MagicType.lookup(key);
        return m != null ? m : MagicType.FLAME;
    }

    private void tellBook(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            bookSalt = salt++;
            announce("Rhya opens the Stolen Spellbook: " + RhyaMath.BOOK_SALVOS + " salvos, each in another stolen magic. Every impact is marked on the ground first. Keep moving.");
            fx(VfxShape.MAGIC_CIRCLE, position().add(0, 0.05, 0), position().add(0, 1, 0), GOLD, total + RhyaMath.BOOK_GAP * RhyaMath.BOOK_SALVOS, 2.2f);
        }
        MagicType m = bookMagic((total - left) / 10);
        Tells.gather(sl, hand(), 0.9, rgbOf(m), 6);
        Tells.ring(sl, position(), 3 + (total - left) * 0.12, rgbOf(m), 1.2f, 40);
    }

    private void runBook(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        pending = null;
        final LivingEntity target = t;
        for (int k = 0; k < RhyaMath.BOOK_SALVOS; k++) {
            final int salvo = k;
            later(k * RhyaMath.BOOK_GAP, MidnightFx.safe("book salvo", () -> {
                LivingEntity tg = target.isAlive() ? target : otherFoe(null, 40);
                if (tg == null) return;
                final MagicType m = bookMagic(salvo);
                final int rgb = rgbOf(m);
                List<Vec3> spots = spotsAround(tg, Math.max(1, Math.min(5, foes(30).size() + 1)), 4.0);
                fx(VfxShape.IMITATION_FX2, hand(), hand().add(0, 1, 0), rgb, 12, 1.0f);
                MidnightFx.sound(sl, position(), SoundEvents.BOOK_PAGE_TURN, 1.6f, 0.8f);
                markedStrike(spots, RhyaMath.BOOK_RADIUS, RhyaMath.BOOK_WARN, rgb, c -> {
                    fx(VfxShape.IMITATION_FX3, c, c.add(0, 1, 0), rgb, 14, 1.4f);
                    for (LivingEntity e : MidnightFx.inRing(this, c, RhyaMath.BOOK_RADIUS)) {
                        if (MidnightFx.hit(this, e, RhyaTable.BOOK_NOVA, true)) MidnightFx.rider(m.hitEffect(), e, this);
                    }
                });
            }));
        }
        say("Every page of it, for you!");
    }
}
