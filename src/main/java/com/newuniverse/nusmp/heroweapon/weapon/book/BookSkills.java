package com.newuniverse.nusmp.heroweapon.weapon.book;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the seven techniques of the Vassal Book, so the whole weapon reads as a list: the two readers that take the book's pages in order (Read Composition for
 * the offence chapter, Read Verse for the support chapter), the turn of a page, the burst of a lit page (Paper Splits Rock), the pages that wrap a foe and burst
 * (Life Force Burst), the cage (Constriction) and the white room (Libraria). The loop is the book's: read the pages in the order they are written (the action
 * bar always shows what comes next), turn a page that does not suit the moment, and when every page of both chapters has been read, the volume is complete and
 * Libraria falls. A page that finds no target is not read, so nothing is spent. Every number comes from {@link BookNumbers}, every word from {@link BookText};
 * damage and heals go through {@code HeroCtx} (the balance law) and nothing changes a block or spawns an entity.
 */
final class BookSkills {
    private BookSkills() {}

    private static final int PAPER = 0xFFF1E6C8, GOLD = 0xFFFFE9A0, WHITE = 0xFFFFFFFF;
    private static final int PAPER_RGB = 0xF1E6C8, WHITE_RGB = 0xFFFFFF;

    static List<HeroSkill> all() {
        List<HeroSkill> l = new ArrayList<>();
        l.add(readComposition());
        l.add(readVerse());
        l.add(turnPage());
        l.add(paperBurst());
        l.add(lifeForceBurst());
        l.add(constriction());
        l.add(libraria());
        return List.copyOf(l);
    }

    private static SimpleSkill make(String id, SimpleSkill.Body body) {
        Budget.Spec s = BookNumbers.spec(id);
        return SimpleSkill.of(id, BookText.skillName(id), BookText.skillDesc(id), s.cooldown(), s.cost(), body);
    }

    private static boolean revised(HeroCtx c) { return c.form.has(BookNumbers.P_REVISED); }

    private static boolean grand(HeroCtx c) { return c.form.has(BookNumbers.P_GRAND); }

    /** The action bar line after a page: what was read, what comes next in each chapter, how much of the volume is read, and whether Libraria is open. */
    private static void announce(HeroCtx c, Volume v, String readPage) {
        String line = "Read " + BookText.pageName(readPage) + " | next: " + BookText.pageName(v.offense().next()) + " / " + BookText.pageName(v.support().next())
                + " | " + v.progress() + (v.complete(grand(c) ? 1 : 0) ? " | Libraria is open" : "");
        c.say(line);
    }

    // ------------------------------------------------------------------ the two readers
    private static HeroSkill readComposition() {
        return make(BookNumbers.READ_COMPOSITION, c -> {
            Volume v = BookState.volume(c);
            String page = v.offense().next();
            if (page == null) {
                c.say("The offence chapter is empty.");
                return false;
            }
            if (!BookPages.cast(c, page, revised(c))) return false;
            v.offense().draw();
            v.noteRead(false);
            announce(c, v, page);
            return true;
        });
    }

    private static HeroSkill readVerse() {
        return make(BookNumbers.READ_VERSE, c -> {
            Volume v = BookState.volume(c);
            String page = v.support().next();
            if (page == null) {
                c.say("The support chapter is empty.");
                return false;
            }
            if (!BookPages.cast(c, page, revised(c))) return false;
            v.support().draw();
            v.noteRead(true);
            announce(c, v, page);
            return true;
        });
    }

    // ------------------------------------------------------------------ the turn of a page
    private static HeroSkill turnPage() {
        return make(BookNumbers.TURN_PAGE, c -> {
            Volume v = BookState.volume(c);
            String skipped = v.turn();
            if (skipped == null) {
                c.say("There is no page to turn.");
                return false;
            }
            c.sound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f);
            c.say("Turned past " + BookText.pageName(skipped) + " | next: " + BookText.pageName(v.offense().next()) + " / " + BookText.pageName(v.support().next())
                    + " | " + v.progress());
            return true;
        });
    }

    // ------------------------------------------------------------------ Paper Splits Rock
    private static HeroSkill paperBurst() {
        return make(BookNumbers.PAPER_BURST, c -> {
            Vec3 at = GlassKit.ground(c, c.aim(BookNumbers.BURST_AIM), 8.0);
            if (GlassKit.foes(c, at, BookNumbers.BURST_RADIUS + 0.5, 1).isEmpty()) {
                c.say("There are no foes at the page you aim at.");
                return false;
            }
            double raw = BookNumbers.pageRaw(BookNumbers.BURST_RAW, 1, revised(c));
            // the tell: a ring on the ground and the page's circle charging
            GlassKit.ring(c, at, BookNumbers.BURST_RADIUS, PAPER_RGB);
            c.fx(VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at, PAPER, BookNumbers.BURST_DELAY + 6, (float) BookNumbers.BURST_RADIUS);
            c.sound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.7f);
            GlassKit.later(c, BookNumbers.BURST_DELAY, () -> {
                for (LivingEntity e : GlassKit.foes(c, at, BookNumbers.BURST_RADIUS, BookNumbers.BURST_TARGETS)) {
                    if (GlassKit.strike(c, e, raw)) GlassKit.push(c, e, at, BookNumbers.BURST_SHOVE);
                }
                c.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.4f);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Life Force Burst
    private static HeroSkill lifeForceBurst() {
        return make(BookNumbers.LIFE_FORCE_BURST, c -> {
            LivingEntity target = GlassKit.focus(c, BookNumbers.LIFE_RANGE);
            if (target == null) {
                c.say("No foe on your crosshair.");
                return false;
            }
            Vec3 wrappedAt = target.position();
            double raw = BookNumbers.heavyRaw(BookNumbers.LIFE_RAW, revised(c));
            // the tell: the pages wrap the foe and a ring marks the spot; running away before the burst frees it
            c.fx(VfxShape.THREAD_COCOON, target.getBoundingBox().getCenter(), target.getBoundingBox().getCenter(), PAPER, BookNumbers.LIFE_DELAY, 1.4f);
            GlassKit.ring(c, wrappedAt, BookNumbers.LIFE_LEASH * 0.5, PAPER_RGB);
            c.sound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.5f);
            GlassKit.later(c, BookNumbers.LIFE_DELAY, () -> {
                if (!target.isAlive() || target.position().distanceTo(wrappedAt) > BookNumbers.LIFE_LEASH
                        || target.distanceTo(c.player) > BookNumbers.LIFE_RANGE + BookNumbers.LIFE_LEASH) {
                    c.say("The pages unravelled; the foe slipped away.");
                    return;
                }
                Vec3 centre = target.getBoundingBox().getCenter();
                GlassKit.strike(c, target, raw);
                c.fx(VfxShape.MAGIC_CIRCLE_EXPLOSION, centre, centre, GOLD, 16, 1.6f);
                c.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 1.2f);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Constriction
    private static HeroSkill constriction() {
        return make(BookNumbers.CONSTRICTION, c -> BookPages.cage(c, revised(c)));
    }

    // ------------------------------------------------------------------ Libraria
    private static HeroSkill libraria() {
        return make(BookNumbers.LIBRARIA, c -> {
            Volume v = BookState.volume(c);
            boolean grand = grand(c);
            if (!v.complete(grand ? 1 : 0)) {
                c.say("The volume is not complete: " + v.progress() + " pages read.");
                return false;
            }
            Vec3 feet = c.player.position();
            if (GlassKit.foes(c, feet, BookNumbers.LIB_RADIUS, 1).isEmpty()) {
                c.say("There are no foes within the library's walls.");
                return false;
            }
            v.consume();
            int volleys = BookNumbers.volleys(grand);
            double raw = BookNumbers.volleyRaw(grand);
            // the tell: the room turns white round you, a ring marks its walls
            GlassKit.ring(c, feet, BookNumbers.LIB_RADIUS, WHITE_RGB);
            c.fx(VfxShape.SEAL_CUBE, feet.add(0.0, BookNumbers.LIB_RADIUS * 0.5, 0.0), feet.add(0.0, BookNumbers.LIB_RADIUS * 0.5, 0.0), WHITE,
                    BookNumbers.LIB_DELAY + volleys * BookNumbers.LIB_INTERVAL, (float) (BookNumbers.LIB_RADIUS * 2.0));
            c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.6f);
            c.sound(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.4f);
            c.say("Libraria");
            GlassKit.later(c, BookNumbers.LIB_DELAY, () -> GlassKit.pulses(c, volleys, BookNumbers.LIB_INTERVAL, i -> {
                List<LivingEntity> foes = GlassKit.foes(c, c.player.position(), BookNumbers.LIB_RADIUS, BookNumbers.LIB_TARGETS);
                for (LivingEntity e : foes) GlassKit.strike(c, e, raw);
                for (int k = 0; k < Math.min(3, foes.size()); k++) BookPages.volleyShow(c, i + k, foes.get(k));
                c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.6f);
            }));
            return true;
        }).asUltimate();
    }
}
