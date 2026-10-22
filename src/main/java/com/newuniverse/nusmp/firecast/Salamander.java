package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Fuegoleon Vermillion's persistent Salamander (docs/magic/fire_casting.md, section 3). A per-player "spirit link": a small Salamander (the red scaled lizard-dragon of the show) perches
 * on the caster's right shoulder while the link is on, and every Fire spell of Fuegoleon's grimoire leaves its mouth (the emitter point {@link FireCastRules#shoulderMouth}). The big pages (cost
 * tier 20 and up: the zone and signature pages) call the Salamander's big form, a huge winged dragon that hangs over the caster for {@link FireCastRules#DRAGON_TICKS} ticks and breathes the
 * spell from its mouth (the emitter becomes {@link FireCastRules#dragonMouth}); it may come at most once per {@link FireCastRules#DRAGON_COOLDOWN} ticks, one per caster and a few on the
 * whole server, linked or not. The link costs nothing per tick beyond one position nudge every five ticks (the clients place the spirit at the shoulder from the owner's own interpolated body, so
 * it does not lag) and a fire-resistance refresh every five seconds, like the elemental Salamander's aura for its master.
 *
 * <p>The link ends on its own at logout, death, dimension change and when the grimoire stops being Fuegoleon's ({@link FireCasting#forget}, {@link #upkeep}).</p>
 */
final class Salamander {
    private Salamander() {}

    static final int FIRE_COLOR = 0xFFFF6A1E;

    static boolean link(ServerPlayer p, FireCasting.State s) {
        ServerLevel sl = p.serverLevel();
        if (!FireCasting.SPIRITS.acquire(p.getUUID(), FireCastRules.SPIRITS_PER_PLAYER, FireCastConfig.maxSpirits())) {
            p.displayClientMessage(Component.translatableWithFallback("nusmp.firecast.spirit.full", "The Salamander cannot answer: too many spirits are out.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        double[] a = FireCastRules.shoulder(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.FIRECAST_2, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, 24000, FireCastRules.param(FireCastRules.V_SALAMANDER, 0), p);
        if (e == null) { FireCasting.SPIRITS.release(p.getUUID()); return false; }
        e.setTarget(p);
        s.spirit = e.getUUID();
        FireCasting.markLive(p);
        VfxSpawn.send(sl, VfxShape.FLAMEFX_BURST, new Vec3(a[0], a[1], a[2]), new Vec3(a[0], a[1] + 0.3, a[2]), FIRE_COLOR, 16, 0.5f);
        sl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 0.8f);
        p.displayClientMessage(Component.translatableWithFallback("nusmp.firecast.spirit.linked", "The Salamander takes your shoulder.").withStyle(ChatFormatting.GOLD), true);
        SalamanderMood.linked(p, s);
        return true;
    }

    static void unlink(ServerPlayer p, FireCasting.State s, boolean say) {
        ServerLevel sl = p.serverLevel();
        MagicPropEntity e = FireCasting.prop(sl, s.spirit);
        if (e != null && say) SalamanderMood.unlinking(p, s);                                  // its goodbye (a small whoosh), only for a link the player ended themselves
        if (e != null) e.discard();
        if (s.spirit != null) FireCasting.SPIRITS.release(p.getUUID());
        s.spirit = null;
        if (say) p.displayClientMessage(Component.translatableWithFallback("nusmp.firecast.spirit.unlinked", "The Salamander returns to its flames.").withStyle(ChatFormatting.GRAY), true);
    }

    /** The cast of one of Fuegoleon's Fire pages: the spell leaves the Salamander's mouth (the big form's, for the big pages). */
    static boolean cast(GrimoireBook book, BookPage page, ManasSkillInstance inst, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        FireCasting.State s = FireCasting.stateOf(p);
        long now = sl.getGameTime();
        Vec3 mouth = null;
        if (page.costPercent() >= FireCastRules.DRAGON_COST_PERCENT && summonDragon(p, s, now)) {
            double[] m = FireCastRules.dragonMouth(p.getX(), p.getY(), p.getZ(), p.getYRot());
            mouth = new Vec3(m[0], m[1], m[2]);
        } else if (s.spirit != null && FireCasting.prop(sl, s.spirit) != null) {
            double[] m = FireCastRules.shoulderMouth(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
            mouth = new Vec3(m[0], m[1], m[2]);
            MagicPropEntity sp = FireCasting.prop(sl, s.spirit);
            int stamp = (int) (now & 0xFFFFF);
            if (sp != null && !FireCastConfig.moods()) sp.setParam((sp.param() & FireCastRules.HIDDEN) | FireCastRules.param(FireCastRules.V_SALAMANDER, stamp == 0 ? 1 : stamp));      // the old way (moods off): the client plays the breathing clip for a second after this tick
        }
        if (FireCastConfig.moods() || FireCastConfig.sounds()) SalamanderMood.cast(p, s, page.costPercent());           // the pose, face and sound of the page's tier (the breath, with an alert, angry or roaring face)
        if (mouth == null) return page.cast().cast(book, inst, p, mode);
        FireCasting.flare(p, book, mouth, 0.5f);
        return FireCasting.withEmitter(p, mouth, null, () -> page.cast().cast(book, inst, p, mode));
    }

    /** Calls the big form for this cast if it is allowed (cooldown, one per caster, the server cap). */
    private static boolean summonDragon(ServerPlayer p, FireCasting.State s, long now) {
        if (now < s.dragonReadyAt || s.dragon != null) return false;
        if (!FireCasting.DRAGONS.acquire(p.getUUID(), FireCastRules.DRAGONS_PER_PLAYER, FireCastConfig.maxDragons())) return false;
        ServerLevel sl = p.serverLevel();
        double[] a = FireCastRules.dragonPosition(p.getX(), p.getY(), p.getZ(), p.getYRot());
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.FIRECAST_2, new Vec3(a[0], a[1], a[2]), p.getYRot(), 1.0f, FireCastRules.DRAGON_TICKS,
                SalamanderMood.dragonParam(now), p);
        if (e == null) { FireCasting.DRAGONS.release(p.getUUID()); return false; }
        e.setTarget(p);
        s.dragon = e.getUUID();
        s.dragonReadyAt = now + FireCastRules.DRAGON_COOLDOWN;
        FireCasting.markLive(p);
        SalamanderMood.dragonCame(p, s, e, now);                                                  // its own roar (nearby players, through the gate) and the dragon's mood clock
        if (!FireCastConfig.sounds()) sl.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.4f, 0.7f);      // sounds off: the plain Ravager roar it always had
        return true;
    }

    /** Once every few ticks for a player with live things: the link ends when the grimoire is no longer Fuegoleon's, the glow of the armour while linked, lost props are dropped. */
    static void upkeep(ServerPlayer p, FireCasting.State s) {
        ServerLevel sl = p.serverLevel();
        if (s.dragon != null && FireCasting.prop(sl, s.dragon) == null) { s.dragon = null; FireCasting.DRAGONS.release(p.getUUID()); }
        if (s.spirit == null) return;
        if (FireCasting.prop(sl, s.spirit) == null) { s.spirit = null; FireCasting.SPIRITS.release(p.getUUID()); return; }
        if (p.tickCount % 20 == 0 && (!FireCastConfig.enabled() || CharacterSpells.characterOf(p) != CanonBook.FUEGOLEON)) { unlink(p, s, false); return; }
        if (p.tickCount % 60 == 0) VfxSpawn.sendFollowing(sl, VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), FIRE_COLOR, 80, 0.7f);     // the spirit armour's glow while linked, one small effect every three seconds
    }
}
