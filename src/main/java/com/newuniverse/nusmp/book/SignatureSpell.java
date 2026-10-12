package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.grimoire.FusionDesign;
import com.newuniverse.nusmp.mana.Signature;
import com.newuniverse.nusmp.mana.Signature.Shape;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import com.newuniverse.nusmp.wizard.BornMagic;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.16.0: the custom spells of a wizard whose magic is their own.
 * <ul>
 *   <li><b>Signature Spell</b> (every grimoire, the last family page): named after the wizard's signature ("Ashen Flame"), its shape (a
 *       lance, a nova, a wave, an orb, a rain or a spiral) is the wizard's own ({@link Signature}), its damage and riders come from the
 *       grimoire's attribute.</li>
 *   <li><b>Born spells</b> (a fused grimoire only, two more pages): the same machinery in the next two shapes, heavier, built from <i>both</i>
 *       parents: both riders hit, both palettes are drawn. A mage with two attributes gets three spells that nobody else has.</li>
 * </ul>
 * The look is procedural: the server sends one of the {@code BORN_*} shapes with the first parent's colour, the second parent's colour and
 * a style packed into the payload's seed, and the client builds the effect (vfx.client.layer.BornMagicLayer). No asset exists for a
 * particular pair; any pair of attributes can be drawn.
 */
public final class SignatureSpell {
    private SignatureSpell() {}

    public static final String SIG_ID = "signature_spell", BORN2_ID = "born_spell_2", BORN3_ID = "born_spell_3";

    public static boolean isSig(String id) { return SIG_ID.equals(id) || BORN2_ID.equals(id) || BORN3_ID.equals(id); }

    /** The two parents written on the grimoire ("FLAME:WATER"), or null for a single-attribute book. */
    public static MagicType[] parents(ManasSkillInstance i) {
        String[] p = FusionDesign.parents(i.getOrCreateTag().getString("Fusion"));
        return p == null ? null : new MagicType[]{MagicType.byName(p[0]), MagicType.byName(p[1])};
    }

    public static boolean fused(ManasSkillInstance i) { return parents(i) != null; }

    /** The label a fused book's spells carry: the born magic's name, or the pair ("Fire-Water"). */
    private static String label(ManasSkillInstance i) {
        CompoundTag t = i.getOrCreateTag();
        if (t.contains("BornName")) return t.getString("BornName");
        MagicType[] p = parents(i);
        return p == null ? "" : p[0].word + "-" + p[1].word;
    }

    private static int offset(String id) { return BORN2_ID.equals(id) ? 1 : BORN3_ID.equals(id) ? 2 : 0; }

    /**
     * The name a signature page shows in the wheel and the book: the wizard's own spell name, never the generic one. Read from the grimoire
     * instance (so the client sees it too): {@code SigName} and {@code SigShape} are written when the signature is made and on every cast.
     */
    public static String pageName(ManasSkillInstance i, BookPage page) {
        if (!isSig(page.id())) return page.name();
        CompoundTag t = i.getOrCreateTag();
        String sig = t.getString("SigName");
        if (SIG_ID.equals(page.id())) return sig.isEmpty() ? page.name() : sig + " (Signature)";
        Shape[] sh = Shape.values();
        return label(i) + " " + sh[Math.floorMod(t.getInt("SigShape") + offset(page.id()), sh.length)].label();
    }

    /** Remembers the wizard's signature on the grimoire instance, where the client can read it. */
    public static void stamp(ManasSkillInstance i, ServerPlayer p) {
        CompoundTag t = i.getOrCreateTag();
        t.putString("SigName", Signature.name(p));
        t.putInt("SigShape", Signature.shape(p).ordinal());
        i.markDirty();
    }

    // ---------------------------------------------------------------- casting
    public static boolean cast(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode) {
        BookPage page = book.page(mode);
        if (page == null) return false;
        Signature.ensure(p, book.magic);
        stamp(inst, p);
        int k = offset(page.id());
        Shape[] sh = Shape.values();
        Shape shape = sh[(Signature.shape(p).ordinal() + k) % sh.length];
        MagicType a = book.magic, b = null;
        MagicType[] par = parents(inst);
        if (par != null) { a = par[0]; b = par[1]; }
        float raw = (k == 2 ? 14f : k == 1 ? 10f : 8f) * (b != null ? 1.25f : 1f);
        return perform(book, p, shape, a, b, raw);
    }

    /** One hit: damage through the balance law, then each parent's rider (a fused spell carries both). */
    private static void strike(ServerPlayer p, LivingEntity t, float raw, MagicType a, MagicType b) {
        if (t == p || !t.isAlive()) return;
        t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, raw, 0.6));
        WikiBooks.rider(a).apply(t, p);
        if (b != null) WikiBooks.rider(b).apply(t, p);
    }

    private static VfxPayload fx(VfxShape shape, Vec3 from, Vec3 to, MagicType a, MagicType b, int ticks, float power, int flags, ServerPlayer p) {
        int ca = BornMagic.colorOf(a), cb = b == null ? lighten(ca) : BornMagic.colorOf(b);
        long seed = ((long) cb << 32) | ((p.getRandom().nextInt() & 0xFFFFFF00L) | (flags & 0xFF));
        return new VfxPayload(shape.ordinal(), from, to, ca, ticks, power, -1, seed);
    }

    private static int lighten(int argb) {
        int r = argb >> 16 & 255, g = argb >> 8 & 255, bl = argb & 255;
        return 0xFF000000 | Math.min(255, r + (255 - r) / 2) << 16 | Math.min(255, g + (255 - g) / 2) << 8 | Math.min(255, bl + (255 - bl) / 2);
    }

    /** Style bits sent with an effect: the low three bits pick the motif (from the wizard's signature), bit 3 = an orb head, bit 4 = a corkscrew. */
    private static int style(ServerPlayer p, int extra) { return (Signature.has(p) ? Math.abs(Signature.name(p).hashCode()) % 6 : 0) | extra; }

    private static boolean perform(GrimoireBook book, ServerPlayer p, Shape shape, MagicType a, MagicType b, float raw) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        book.castCircle(p, 1.3f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2f, b != null ? 1.4f : 1.1f);
        int st = style(p, 0);
        switch (shape) {
            case LANCE -> {
                Vec3 end = eye.add(look.scale(20));
                VfxSpawn.send(sl, fx(VfxShape.BORN_BEAM, eye.add(look.scale(0.6)), end, a, b, 12, 1f, st, p));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.4)) strike(p, t, raw, a, b);
            }
            case NOVA -> {
                Vec3 c = p.position();
                VfxSpawn.send(sl, fx(VfxShape.BORN_NOVA, c, c.add(0, 1, 0), a, b, 24, 6f, st, p));
                for (LivingEntity t : GrimoireBook.around(p, c, 6)) strike(p, t, raw, a, b);
            }
            case WAVE -> {
                VfxSpawn.send(sl, fx(VfxShape.BORN_WAVE, eye.add(look.scale(0.5)), eye.add(look.scale(10)), a, b, 16, 1f, st, p));
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 10)) {
                    Vec3 d = t.getBoundingBox().getCenter().subtract(eye);
                    if (d.lengthSqr() > 1e-6 && d.normalize().dot(look) > 0.5) strike(p, t, raw, a, b);
                }
            }
            case ORB -> {
                Vec3 start = eye.add(look.scale(1.2)), vel = look.scale(0.7);
                VfxSpawn.send(sl, fx(VfxShape.BORN_BEAM, start, start.add(vel.scale(30)), a, b, 30, 2.4f, st | 8, p));
                SpellRuntime.bolt(p, start, vel, 1.0, 30, false, null, (bolt, t) -> { }, (bolt, at) -> {
                    VfxSpawn.send(sl, fx(VfxShape.BORN_NOVA, at, at.add(0, 1, 0), a, b, 20, 4f, st, p));
                    for (LivingEntity t : GrimoireBook.around(p, at, 4)) strike(p, t, raw, a, b);
                });
            }
            case RAIN -> {
                Vec3 c = GrimoireBook.aim(p, 20);
                var rnd = p.getRandom();
                for (int k = 0; k < 7; k++) {
                    Vec3 at = c.add(rnd.nextGaussian() * 2.2, 0, rnd.nextGaussian() * 2.2);
                    SpellRuntime.later(sl, 3 + k * 3, () -> {
                        VfxSpawn.send(sl, fx(VfxShape.BORN_RAIN, at.add(0, 9, 0), at, a, b, 14, 1.8f, st, p));
                        for (LivingEntity t : GrimoireBook.around(p, at, 1.8)) strike(p, t, raw * 0.8f, a, b);
                    });
                }
            }
            case SPIRAL -> {
                for (int k = -1; k <= 1; k++) {
                    double yaw = Math.toRadians(18 * k);
                    Vec3 dir = new Vec3(look.x * Math.cos(yaw) - look.z * Math.sin(yaw), look.y, look.x * Math.sin(yaw) + look.z * Math.cos(yaw)).normalize();
                    Vec3 start = eye.add(dir.scale(0.8)), vel = dir.scale(0.85);
                    VfxSpawn.send(sl, fx(VfxShape.BORN_BEAM, start, start.add(vel.scale(22)), a, b, 22, 1.1f, st | 16, p));
                    SpellRuntime.bolt(p, start, vel, 0.9, 22, true, null, (bolt, t) -> strike(p, t, raw * 0.7f, a, b), null);
                }
            }
        }
        return true;
    }

    /** Every page this book adds for a wizard (used by GrimoireBook to build its page list). */
    public static List<BookPage> pages() {
        List<BookPage> l = new ArrayList<>();
        l.add(new BookPage(SIG_ID, "Signature Spell", "Signature Spell", 14, 350, 200, SignatureSpell::cast));
        l.add(new BookPage(BORN2_ID, "Fusion Spell", "Fusion Spell", 20, 600, 300, SignatureSpell::cast));
        l.add(new BookPage(BORN3_ID, "Born Spell", "Born Spell", 35, 1200, 600, SignatureSpell::cast));
        return l;
    }
}
