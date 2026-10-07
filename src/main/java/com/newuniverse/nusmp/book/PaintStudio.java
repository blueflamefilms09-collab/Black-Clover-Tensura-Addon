package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.entity.PaintedConstructEntity;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

/**
 * Painting Magic remake (0.44): the palette & brush, the painter's mood, the paint's element and how it counters.
 *
 * <ul>
 *   <li><b>Palette & brush:</b> summoning a Painting grimoire manifests a wooden palette (off hand) and a mana brush (main hand);
 *       stowing it, dying or dropping them makes them dissolve. They are bound to the painter (never duplicated, never kept by
 *       anyone else). The palette picks the paint ({@link Paint}); the brush lays it down.</li>
 *   <li><b>Resources:</b> brushwork is physical, so it draws on <b>Aura</b> (falling back to magicules); pages and living
 *       illustrations spend <b>magicules</b> through Tensura as every page does.</li>
 *   <li><b>Mood:</b> -100..100. Joy feeds imagination (landing hits, kills, eating, friends nearby), frustration stifles it
 *       (being hurt, low health, Tensura fear or insanity). It scales Painting damage x0.7..x1.3 and size x0.8..x1.2.</li>
 *   <li><b>Scaling:</b> EP, armour and equipment through {@link MirrorWorks#power}, mapped to x0.9..x1.5, times the mood.</li>
 *   <li><b>Element counter:</b> the paint turns into any element. Holding the palette with the paint that counters an incoming
 *       element takes 40% off it; Counter Palette answers the last element that hit you automatically.</li>
 * </ul>
 */
public final class PaintStudio {
    private PaintStudio() {}

    static final String K_PAINT = "nusmp_paint", K_MOOD = "nusmp_mood", K_MOOD_HIT = "nusmp_mood_hit", K_MOOD_HURT = "nusmp_mood_hurt",
            K_LAST = "nusmp_paint_last", K_COUNTER_UNTIL = "nusmp_paint_counter_until", K_COUNTER_EL = "nusmp_paint_counter_el",
            OWNER = "PaintOwner";

    /** What the paint can become. Order = custom model data - 1 (item model variants): append only. */
    public enum Paint {
        INK("Ink", 0xFF3A7BFF), FIRE("Fire", 0xFFFF5A3A), WATER("Water", 0xFF4AA8FF), ICE("Ice", 0xFF8AE6FF),
        WIND("Wind", 0xFF7CF0B0), EARTH("Earth", 0xFFB0864A), LIGHTNING("Lightning", 0xFFFFE65A);

        public final String label;
        public final int color;
        Paint(String label, int color) { this.label = label; this.color = color; }

        /** The paint that answers this element. */
        public Paint counter() {
            return switch (this) {
                case FIRE -> WATER;
                case WATER -> ICE;
                case ICE -> FIRE;
                case WIND -> EARTH;
                case EARTH -> WIND;
                case LIGHTNING -> EARTH;
                case INK -> INK;
            };
        }

        public static Paint of(int i) { Paint[] v = values(); return v[Math.floorMod(i, v.length)]; }
    }

    // ================================================================ paint, mood, power
    public static Paint paint(Player p) { return Paint.of(p.getPersistentData().getInt(K_PAINT)); }

    public static void setPaint(ServerPlayer p, Paint paint, boolean fx) {
        p.getPersistentData().putInt(K_PAINT, paint.ordinal());
        if (!fx) return;
        VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_SHIFT, offHand(p), offHand(p), paint.color, 14, 0.6f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.8f, 1.4f);
        p.displayClientMessage(Component.literal("Paint: " + paint.label).withColor(paint.color & 0xFFFFFF), true);
    }

    public static int mood(Player p) { return Mth.clamp(p.getPersistentData().getInt(K_MOOD), -100, 100); }

    public static void addMood(ServerPlayer p, int delta) {
        if (!isPainter(p) || delta == 0) return;
        int before = mood(p), after = Mth.clamp(before + delta, -100, 100);
        p.getPersistentData().putInt(K_MOOD, after);
        if (before < 40 && after >= 40) {
            p.displayClientMessage(Component.literal("✦ Inspired! Your imagination runs free.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.PAINT_SHIFT, p, p.position().add(0, 1.2, 0), 0xFFFFC84A, 14, 0.9f);
        } else if (before > -40 && after <= -40) {
            p.displayClientMessage(Component.literal("Frustration clouds your colours...").withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }

    /** Joy x1.3 .. frustration x0.7. */
    public static float moodMult(Player p) { return 1 + mood(p) / 100f * 0.3f; }

    /** EP, armour and equipment (MirrorWorks.power, 0.8..2.4) mapped to x0.9..x1.5. */
    public static float epFactor(LivingEntity e) { return 0.9f + (MirrorWorks.power(e) - 0.8f) / 1.6f * 0.6f; }

    /** Damage / durability multiplier of everything painted. */
    public static float power(Player p) { return epFactor(p) * moodMult(p); }

    /** Size multiplier: the page's own size (mastery, Mana Zone) times the mood's x0.8..x1.2. */
    public static float size(ManasSkillInstance i, ServerPlayer p) { return GrimoireBook.size(i, p) * (1 + mood(p) / 100f * 0.2f); }

    public static boolean isPainter(Player p) { return GrimoireSlot.holdsOwn(p, MagicType.PAINTING); }

    static String moodWord(Player p) {
        int m = mood(p);
        return m >= 70 ? "Joyful" : m >= 40 ? "Inspired" : m > -40 ? "Calm" : m > -70 ? "Frustrated" : "Furious";
    }

    // ================================================================ palette & brush (manifested items)
    public static boolean isManifest(ItemStack s) { return s.is(NUItems.PAINT_BRUSH.get()) || s.is(NUItems.PAINT_PALETTE.get()); }

    public static UUID ownerOf(ItemStack s) {
        CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return t.hasUUID(OWNER) ? t.getUUID(OWNER) : null;
    }

    static ItemStack make(ServerPlayer p, ItemStack s) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putUUID(OWNER, p.getUUID()));
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(paint(p).ordinal() + 1));
        return s;
    }

    /** The grimoire was summoned: the palette and brush appear in the hands (or the inventory if the hands are busy). */
    public static void onSummon(ServerPlayer p, MagicType magic) {
        if (magic != MagicType.PAINTING) return;
        Inventory inv = p.getInventory();
        boolean hasBrush = false, hasPalette = false;
        for (int k = 0; k < inv.getContainerSize(); k++) {
            ItemStack s = inv.getItem(k);
            if (!p.getUUID().equals(ownerOf(s))) continue;
            hasBrush |= s.is(NUItems.PAINT_BRUSH.get());
            hasPalette |= s.is(NUItems.PAINT_PALETTE.get());
        }
        if (!hasPalette) {
            ItemStack pal = make(p, new ItemStack(NUItems.PAINT_PALETTE.get()));
            if (p.getOffhandItem().isEmpty()) p.setItemInHand(InteractionHand.OFF_HAND, pal);
            else if (!inv.add(pal)) GrimoireBook.fail(p, "No room for your palette.");
        }
        if (!hasBrush) {
            ItemStack brush = make(p, new ItemStack(NUItems.PAINT_BRUSH.get()));
            if (p.getMainHandItem().isEmpty()) p.setItemInHand(InteractionHand.MAIN_HAND, brush);
            else if (!inv.add(brush)) GrimoireBook.fail(p, "No room for your brush.");
        }
        Vec3 hand = offHand(p);
        VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_PALETTE, hand, hand.add(p.getLookAngle().scale(-0.4)).add(0, 0.8, 0), 0xFFC08A55, 30, 1f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.HONEY_BLOCK_PLACE, SoundSource.PLAYERS, 1f, 1.2f);
    }

    /** The grimoire was put away (or lost): the palette and brush dissolve. */
    public static void onDismiss(ServerPlayer p) { vanish(p); }

    static void vanish(ServerPlayer p) {
        Inventory inv = p.getInventory();
        boolean any = false;
        for (int k = 0; k < inv.getContainerSize(); k++) {
            ItemStack s = inv.getItem(k);
            if (isManifest(s) && ownerOf(s) != null) { inv.setItem(k, ItemStack.EMPTY); any = true; }
        }
        if (any) VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_SPLAT, p.position(), p.position().add(0, 1, 0), paint(p).color, 30, 0.8f);
    }

    /** Where the off hand (the palette) is, for effects. */
    static Vec3 offHand(Player p) {
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        return p.position().add(0, 1.0, 0).add(fwd.scale(0.4)).add(left.scale(0.4));
    }

    static Vec3 brushTip(Player p) {
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(right.scale(0.3)).add(0, -0.35, 0);
    }

    // ---------------------------------------------------------------- the items' actions
    /** Brush: one sweep of the current paint. Brushwork draws on aura (magicules if the aura is spent). */
    public static boolean brushStroke(ServerPlayer p) {
        if (!isPainter(p)) { GrimoireBook.fail(p, "Only a Painting mage can make this brush move."); return false; }
        if (!pay(p, 0.01, 4)) { GrimoireBook.fail(p, "Not enough aura or magicules to lift the brush."); return false; }
        Paint paint = paint(p);
        float pw = power(p);
        Vec3 from = brushTip(p), dir = p.getLookAngle();
        Vec3 end = GrimoireBook.aim(p, 22);
        VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_TRAIL, from, end, paint.color, 16, 0.35f + 0.1f * pw);
        SpellRuntime.bolt(p, from, dir.scale(1.8), 0.7, 12, false, null,
                (bolt, t) -> {
                    hurt(p, t, 5f * pw);
                    applyPaint(p, t, paint, pw);
                    if (p.level().getGameTime() - p.getPersistentData().getLong(K_MOOD_HIT) > 10) {
                        p.getPersistentData().putLong(K_MOOD_HIT, p.level().getGameTime());
                        addMood(p, 2);
                    }
                },
                (bolt, at) -> { if (paint == Paint.INK) VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_SPLAT, at, at.add(0, 1, 0), paint.color, 40, 0.8f); });
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1f, 1.1f + p.getRandom().nextFloat() * 0.2f);
        p.displayClientMessage(Component.literal(paint.label + "  ·  " + moodWord(p) + " " + (moodMult(p) >= 1 ? "+" : "")
                + Math.round((moodMult(p) - 1) * 100) + "%").withColor(paint.color & 0xFFFFFF), true);
        return true;
    }

    /** Palette: the next paint; sneaking, the paint that counters the last element that hit you. */
    public static void paletteUse(ServerPlayer p) {
        if (!isPainter(p)) { GrimoireBook.fail(p, "The colours won't mix for you."); return; }
        if (p.isShiftKeyDown()) {
            Paint last = lastElement(p);
            if (last == null) { GrimoireBook.fail(p, "Nothing to answer yet: no element has struck you."); return; }
            setPaint(p, last.counter(), true);
            return;
        }
        setPaint(p, Paint.of(paint(p).ordinal() + 1), true);
    }

    /** Spends {@code frac} of max aura (at least {@code floor}); falls back to magicules. No Tensura data: free. */
    static boolean pay(ServerPlayer p, double frac, double floor) {
        if (p.isCreative()) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        double auraCost = Math.max(floor, EnergyHelper.getMaxAura(p) * frac), mpCost = Math.max(floor, EnergyHelper.getMaxMagicule(p) * frac);
        if (ex.getAura() >= auraCost) ex.setAura(ex.getAura() - auraCost);
        else if (ex.getMagicule() >= mpCost) ex.setMagicule(ex.getMagicule() - mpCost);
        else return false;
        ex.markDirty();
        return true;
    }

    static void hurt(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p)) return;
        t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(t, raw, 0.5));
    }

    /** What each paint does to what it touches (with Tensura's effects where they exist). */
    public static void applyPaint(ServerPlayer p, LivingEntity t, Paint paint, float pw) {
        if (t == p || t.isAlliedTo(p)) return;
        switch (paint) {
            case INK -> {                                                                          // sticky ink: slow, magic jammed
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                MirrorWorks.tensuraEffect(t, "silence", 40, 0);
            }
            case FIRE -> t.igniteForSeconds(3 + pw);
            case WATER -> {                                                                        // washes the mana out: energy drain
                Vec3 away = t.position().subtract(p.position()).normalize();
                t.knockback(0.5, -away.x, -away.z);
                var ex = TensuraStorages.getExistenceFrom(t);
                if (ex != null) { ex.setMagicule(Math.max(0, ex.getMagicule() - EnergyHelper.getMaxMagicule(t) * 0.03)); ex.markDirty(); }
            }
            case ICE -> {
                t.setTicksFrozen(Math.max(t.getTicksFrozen(), 160));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            case WIND -> {
                Vec3 away = t.position().subtract(p.position()).normalize();
                t.knockback(1.0 + 0.3 * pw, -away.x, -away.z);
            }
            case EARTH -> {                                                                        // resistance shredded
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                MirrorWorks.tensuraEffect(t, "fragility", 100, 0);
            }
            case LIGHTNING -> {                                                                    // strikes twice, cuts the spirit
                t.invulnerableTime = 0;
                hurt(p, t, 2f * pw);
                var ex = TensuraStorages.getExistenceFrom(t);
                if (ex != null && ex.getSpiritualHealth() > 2) { ex.setSpiritualHealth(ex.getSpiritualHealth() - 1); ex.markDirty(); }
            }
        }
    }

    // ================================================================ element reading
    static Paint lastElement(Player p) {
        CompoundTag d = p.getPersistentData();
        return d.contains(K_LAST) ? Paint.of(d.getInt(K_LAST)) : null;
    }

    /** The element a blow carries, if the paint can read it. */
    static Paint elementOf(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FIRE)) return Paint.FIRE;
        if (s.is(DamageTypeTags.IS_FREEZING)) return Paint.ICE;
        if (s.is(DamageTypeTags.IS_LIGHTNING)) return Paint.LIGHTNING;
        if (s.is(DamageTypeTags.IS_DROWNING)) return Paint.WATER;
        if (s.getEntity() instanceof Player a) {
            MagicType m = GrimoireSummon.floatingMagic(a);
            if (m != null) return switch (m) {
                case FLAME, EXPLOSION, MAGMA -> Paint.FIRE;
                case WATER, MERCURY, MIST -> Paint.WATER;
                case ICE -> Paint.ICE;
                case WIND, STAR -> Paint.WIND;
                case STORM, LIGHTNING -> Paint.LIGHTNING;
                case EARTH, PLANT, SAND -> Paint.EARTH;
                case PAINTING -> Paint.INK;
                default -> null;
            };
        }
        return null;
    }

    static boolean holdsPalette(Player p) {
        return p.getOffhandItem().is(NUItems.PAINT_PALETTE.get()) || p.getMainHandItem().is(NUItems.PAINT_PALETTE.get());
    }

    // ================================================================ pages (new in 0.44)
    /** Living Illustration: paint a knight (sneak: a giant) on the ground before you; it stands up and fights for 30 s. */
    static boolean livingIllustration(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        PaintedConstructEntity.Kind kind = p.isShiftKeyDown() ? PaintedConstructEntity.Kind.GIANT : PaintedConstructEntity.Kind.KNIGHT;
        int cap = 3 + (mood(p) >= 50 ? 1 : 0);
        if (PaintedConstructEntity.countOf(p) >= cap) { GrimoireBook.fail(p, "Your canvas is full (" + cap + " illustrations)."); return false; }
        Vec3 at = GrimoireBook.aim(p, 7);
        Vec3 flat = p.getLookAngle().multiply(1, 0, 1).normalize();
        at = at.subtract(flat.scale(0.6));
        b.castCircle(p, 1.1f);
        illustrate(p, kind, at, paint(p), 600);
        return true;
    }

    /** Counter Palette: the paint turns into what answers the last element that hit you; for 6 s that element barely touches you. */
    static boolean counterPalette(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Paint last = lastElement(p);
        if (last == null) { GrimoireBook.fail(p, "Nothing to answer yet: no element has struck you."); return false; }
        Paint answer = last.counter();
        setPaint(p, answer, true);
        p.getPersistentData().putLong(K_COUNTER_UNTIL, p.level().getGameTime() + 120);
        p.getPersistentData().putInt(K_COUNTER_EL, last.ordinal());
        Vec3 fwd = p.getLookAngle().multiply(1, 0, 1).normalize(), side = new Vec3(-fwd.z, 0, fwd.x);
        Vec3 c = p.position().add(fwd.scale(1.6)).add(0, 1, 0);
        for (int k = -1; k <= 1; k++)
            VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_TRAIL, c.add(side.scale(-1.4)).add(0, k * 0.5, 0), c.add(side.scale(1.4)).add(0, k * 0.5, 0), answer.color, 120, 0.5f);
        p.displayClientMessage(Component.literal("The paint turns to " + answer.label + " against " + last.label + ".").withColor(answer.color & 0xFFFFFF), true);
        return true;
    }

    /** Painted Menagerie (0.44 replacement): three painted beasts leap off the canvas in the current paint. */
    static boolean menagerie(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.3f);
        for (int k = 0; k < 3; k++) {
            double a = Math.PI * 2 * k / 3 + p.getYRot() * Mth.DEG_TO_RAD;
            illustrate(p, PaintedConstructEntity.Kind.BEAST, p.position().add(Math.cos(a) * 2, 0, Math.sin(a) * 2), paint(p), 600);
        }
        return true;
    }

    /** Paints one living illustration at 'at' facing away from the painter: the drawing stands up, then the construct steps out. */
    static void illustrate(ServerPlayer p, PaintedConstructEntity.Kind kind, Vec3 at, Paint paint, int life) {
        ServerLevel level = p.serverLevel();
        Vec3 away = at.subtract(p.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) away = p.getLookAngle().multiply(1, 0, 1);
        away = away.normalize();
        float h = kind == PaintedConstructEntity.Kind.GIANT ? 3.6f : kind == PaintedConstructEntity.Kind.BEAST ? 1.4f : 2f;
        long seed = (level.random.nextLong() & ~3L) | kind.ordinal();
        VfxSpawn.send(level, new VfxPayload(VfxShape.PAINT_EMERGE.ordinal(), at, at.add(away.scale(-1)), paint.color, 30, h, -1, seed));
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.2f, 0.8f);
        Vec3 face = away;
        SpellRuntime.later(level, 20, () -> {
            if (!p.isAlive()) return;
            PaintedConstructEntity.spawn(p, kind, at, (float) (Mth.atan2(-face.x, face.z) * Mth.RAD_TO_DEG), paint, power(p), life);
            level.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.2f, 0.7f);
        });
    }

    // ================================================================ events
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        Inventory inv = p.getInventory();
        boolean out = GrimoireSummon.floatingMagic(p) == MagicType.PAINTING;
        boolean brush = false, palette = false;
        for (int k = 0; k < inv.getContainerSize(); k++) {
            ItemStack s = inv.getItem(k);
            if (!isManifest(s)) continue;
            UUID owner = ownerOf(s);
            if (owner == null) { s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(paint(p).ordinal() + 1)); continue; }   // creative copies
            boolean isBrush = s.is(NUItems.PAINT_BRUSH.get());
            // someone else's, the grimoire is away, or a duplicate: it dissolves
            if (!owner.equals(p.getUUID()) || !out || (isBrush ? brush : palette)) { inv.setItem(k, ItemStack.EMPTY); continue; }
            if (isBrush) brush = true; else palette = true;
            s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(paint(p).ordinal() + 1));
        }
        if (!isPainter(p)) return;
        // the mood settles toward calm; fear and despair weigh on it, friends and health lift it
        int m = mood(p), drift = m > 0 ? -1 : m < 0 ? 1 : 0;
        if (Math.abs(m) > 50) drift *= 2;
        int d = drift;
        for (String bad : new String[]{"fear", "insanity"})
            if (MirrorWorks.tensuraEffect(bad).map(p::hasEffect).orElse(false)) d -= 3;
        if (p.getHealth() < p.getMaxHealth() * 0.3f) d -= 1;
        if (!p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8), x -> x != p && x.isAlliedTo(p)).isEmpty()) d += 1;
        if (d != 0) addMood(p, d);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (e.getEntity() instanceof ServerPlayer p && isPainter(p)) {
            Paint el = elementOf(src);
            long now = p.level().getGameTime();
            if (el != null) {
                p.getPersistentData().putInt(K_LAST, el.ordinal());
                if (p.getPersistentData().getLong(K_COUNTER_UNTIL) > now && p.getPersistentData().getInt(K_COUNTER_EL) == el.ordinal()) {
                    e.setAmount(e.getAmount() * 0.3f);                                   // Counter Palette: answered
                    VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_SPLAT, p.position().add(0, 1, 0), p.position().add(0, 2, 0), paint(p).color, 20, 0.7f);
                } else if (holdsPalette(p) && paint(p) == el.counter()) {
                    e.setAmount(e.getAmount() * 0.6f);                                   // the right colour on the palette
                    VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_SPLAT, p.position().add(0, 1, 0), p.position().add(0, 2, 0), paint(p).color, 16, 0.5f);
                }
            }
            if (now - p.getPersistentData().getLong(K_MOOD_HURT) > 10) {                 // being hurt frustrates
                p.getPersistentData().putLong(K_MOOD_HURT, now);
                addMood(p, -Math.max(1, Math.round(e.getAmount() * 1.5f)));
            }
        }
        if (src.getEntity() instanceof ServerPlayer a && a != e.getEntity() && isPainter(a)) {
            long now = a.level().getGameTime();
            if (now - a.getPersistentData().getLong(K_MOOD_HIT) > 10) {
                a.getPersistentData().putLong(K_MOOD_HIT, now);
                addMood(a, 1);
            }
        }
    }

    public static void onDeath(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer a && a != e.getEntity()) addMood(a, 8);
    }

    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getItem().has(DataComponents.FOOD)) addMood(p, 5);
    }
}
