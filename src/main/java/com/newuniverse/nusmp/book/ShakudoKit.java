package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.ShakudoPropState;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0: what Shakudo, Scale, Spike, Nail and Hair Magic share on the server (the five were generic wiki attributes; their grimoires now have their own
 * armour, constructs, effects and spells, each in its own {@code <Name>Spells} class):
 * <ul>
 *   <li>which magics are "mine" and the mapping of the wiki spell shapes (WIKI_MAGIC_CAST / FIELD / BURST that the five generic pages ask for) onto the
 *       magic's own effects {@code <NAME>_FX1 / _FX2 / _FX3} ({@link #shape}, called by ElementBook.vfx);</li>
 *   <li>the temporary armour: the render layer (Aura.SHAKUDO ... HAIR, the pieces forge in and shatter out on the client) and its gameplay, one table of
 *       numbers per magic ({@link #armour}, {@link #guard}): a share of every blow is softened, attackers take the magic's own answer (Shakudo staggers
 *       them, Scale softens projectiles further, Spike cuts them for half of what they dealt, Nail pins their feet, Hair binds them and mends its wearer);</li>
 *   <li>small helpers for the spells: the span of a summon, dismissing the caster's earlier constructs, the flat look vector.</li>
 * </ul>
 * The armour state lives in the wearer's persistent data (end game time, share, thorns, kind), so it survives nothing but the session, as before.
 */
public final class ShakudoKit {
    private ShakudoKit() {}

    private static final String UNTIL = "nusmp_sh_armor_until", CUT = "nusmp_sh_armor_cut", THORNS = "nusmp_sh_armor_thorns", KIND = "nusmp_sh_armor_kind";
    private static boolean hooked;

    public static boolean mine(MagicType m) {
        return m == MagicType.SHAKUDO || m == MagicType.SCALE || m == MagicType.SPIKE || m == MagicType.NAIL || m == MagicType.HAIR;
    }

    /** The render layer slot of a magic's temporary armour (null if it is not one of the five). */
    public static Aura aura(MagicType m) {
        return switch (m) {
            case SHAKUDO -> Aura.SHAKUDO;
            case SCALE -> Aura.SCALE;
            case SPIKE -> Aura.SPIKE;
            case NAIL -> Aura.NAIL;
            case HAIR -> Aura.HAIR;
            default -> null;
        };
    }

    /** The magic's own effect n (1 cast, 2 zone, 3 impact). */
    public static VfxShape fx(MagicType m, int n) {
        String name = m.name() + "_FX" + n;
        try { return VfxShape.valueOf(name); } catch (IllegalArgumentException e) { return VfxShape.WIKI_MAGIC_CAST; }
    }

    /** ElementBook.vfx: the generic pages of the five ask for the wiki shapes; they get the magic's own. Any other shape (a circle ...) is left alone. */
    public static VfxShape shape(MagicType m, VfxShape asked) {
        return switch (asked) {
            case WIKI_MAGIC_CAST -> fx(m, 1);
            case WIKI_MAGIC_FIELD -> fx(m, 2);
            case WIKI_MAGIC_BURST -> fx(m, 3);
            default -> asked;
        };
    }

    /**
     * ElementBook.empower (the Guard page): sets the magic's own armour layer instead of the generic wiki one. Returns true when the magic is one of the five.
     * The aura lasts at least 40 ticks so the last piece is on before the shatter window opens.
     */
    public static boolean armour(ServerPlayer p, MagicType m, int ticks) {
        Aura a = aura(m);
        if (a == null) return false;
        PlayerAuras.set(p, a, Math.max(40, ticks));
        return true;
    }

    // ------------------------------------------------------------------ the armour's gameplay
    /** Registers the damage hook once (called from the five Props.init()). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            var data = victim.getPersistentData();
            long now = victim.level().getGameTime();
            if (data.getLong(UNTIL) <= now || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            String kind = data.getString(KIND);
            float cut = data.getFloat(CUT);
            boolean projectile = e.getSource().is(DamageTypeTags.IS_PROJECTILE);
            if (projectile && kind.equals("SCALE")) cut = Math.min(0.8f, cut + 0.2f);               // scales turn arrows and bolts
            if (kind.equals("SHAKUDO") && !projectile) cut = Math.min(0.8f, cut + 0.05f);
            float before = e.getAmount();
            e.setAmount(before * (1f - cut));
            if (!(e.getSource().getDirectEntity() instanceof LivingEntity att) || att == victim || e.getSource().is(DamageTypes.THORNS)) return;
            float thorns = data.getFloat(THORNS);
            switch (kind) {
                case "SPIKE" -> att.hurt(victim.damageSources().thorns(victim), Math.min(thorns + 14f, before * 0.5f + thorns));
                case "NAIL" -> {
                    att.hurt(victim.damageSources().thorns(victim), thorns);
                    att.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(att) ? 20 : 50, 2));
                }
                case "HAIR" -> att.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(att) ? 20 : 60, 1));
                case "SHAKUDO" -> {
                    att.hurt(victim.damageSources().thorns(victim), thorns);
                    Vec3 out = att.position().subtract(victim.position()).multiply(1, 0, 1);
                    if (out.lengthSqr() > 1e-4) {
                        att.setDeltaMovement(att.getDeltaMovement().add(out.normalize().scale(BalanceLaw.isBoss(att) ? 0.1 : 0.45)).add(0, 0.1, 0));
                        att.hurtMarked = true;
                    }
                }
                default -> { if (thorns > 0) att.hurt(victim.damageSources().thorns(victim), thorns); }
            }
        });
        AttributeEvents.playerTick(p -> {                                                              // Hair armour mends its wearer
            var data = p.getPersistentData();
            long now = p.level().getGameTime();
            if (now % 40 != 0 || data.getLong(UNTIL) <= now || !data.getString(KIND).equals("HAIR") || p.getHealth() >= p.getMaxHealth()) return;
            BalanceLaw.heal(p, 1.5f * (float) Math.min(3.0, BalanceLaw.epScale(p)));
        });
    }

    /** Puts a magic's armour on the caster: share of every blow softened, thorns, the render layer, Resistance. Returns false (nothing spent) while it still holds. */
    public static boolean guard(GrimoireBook b, ServerPlayer p, MagicType m, int ticks, float cut, float thorns, int resistance) {
        Aura a = aura(m);
        if (a == null) return false;
        if (PlayerAuras.active(p, a) && p.getPersistentData().getLong(UNTIL) > p.level().getGameTime() + 60) {
            GrimoireBook.fail(p, "Your " + m.displayName.replace(" Magic", "").toLowerCase() + " armour still holds.");
            return false;
        }
        int t = span(ticks, p);
        var d = p.getPersistentData();
        d.putLong(UNTIL, p.level().getGameTime() + t);
        d.putFloat(CUT, cut);
        d.putFloat(THORNS, thorns);
        d.putString(KIND, m.name());
        PlayerAuras.set(p, a, Math.max(40, t));
        if (resistance >= 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, t, resistance));
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), fx(m, 3), p, p.position().add(0, 1, 0), b.color, 40, 1.4f);
        return true;
    }

    // ------------------------------------------------------------------ helpers for the spells
    /** Ticks of a summon or an armour, longer for a mighty caster (up to +50 percent). */
    public static int span(int base, ServerPlayer p) { return (int) (base * Math.min(1.5, 1 + 0.04 * (BalanceLaw.epScale(p) - 1))); }

    /** The constructs of this kind the caster already has are ended (a new cast replaces them). */
    public static void dismiss(ServerPlayer p, PropKind... kinds) {
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(96),
                x -> p.getUUID().equals(x.ownerId()) && contains(kinds, x.kind())))
            e.expire();
    }

    private static boolean contains(PropKind[] kinds, PropKind k) {
        for (PropKind x : kinds) if (x == k) return true;
        return false;
    }

    /** The caster's horizontal look direction (never zero). */
    public static Vec3 flatLook(ServerPlayer p) {
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        return look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
    }

    /** EP scale clamped 1..9, the number the spells read as "how mighty". */
    public static float mighty(ServerPlayer p) { return (float) Math.min(9.0, Math.max(1.0, BalanceLaw.epScale(p))); }

    /** The first living target in front of the caster within range, else null. */
    public static LivingEntity foe(ServerPlayer p, double range) { return GrimoireBook.target(p, range); }

    public static ServerLevel level(ServerPlayer p) { return p.serverLevel(); }

    /** Spawns a construct of the caster's (null if the world refused it); {@code target} may be null. The variant rides in the synced word's bits 18 to 21. */
    public static MagicPropEntity spawn(ServerPlayer p, PropKind kind, Vec3 at, float yaw, float scale, int life, int variant, LivingEntity target) {
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), kind, at, yaw, scale, life, ShakudoPropState.pack(variant, 0, p.level().getGameTime()), p);
        if (e != null && target != null) e.setTarget(target);
        return e;
    }

    /** A construct's scale: the base, a little bigger for a mighty caster. */
    public static float scaleFor(ServerPlayer p, float base) { return base + 0.02f * (mighty(p) - 1f); }

    /** Raw damage factor of a cast: the book's size (Spirit Dive, build-up) times the EP scale. */
    public static float k(ManasSkillInstance i, ServerPlayer p) { return GrimoireBook.size(i, p) * EnergyBridge.scale(p); }
}
