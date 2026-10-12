package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.wizard.BornMagic;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Grimoire Tower Access: Lucius links to the grimoire towers of the Clover Kingdom and casts any spell of any magic that was ever
 * recorded. The library is read from the mod's own grimoire books (every non-restricted {@link MagicType}, every family page that costs
 * mana), so the name the party sees on the screen, "The towers answer: Flame Magic - Flame Bullet", is a real page. A mob has no
 * grimoire to open, so the page is cast as the spell of its tier ({@link TowerMath#shapeFor}): a bolt, a lance, a nova, a lingering
 * field or a rain of bolts, in the colours of the magic, with the rider of the magic on every hit. A deck makes the towers answer with
 * every magic before any repeats.
 * <p>
 * Fairness: a bolt flies and can be sidestepped; a lance, a nova, a field and a rain are all <i>marked</i> on the ground for at least
 * fourteen ticks before they land, and what is marked is where it lands, whoever moves.
 */
final class TowerSpells {
    private TowerSpells() {}

    /** One page of one magic. */
    record Entry(MagicType magic, String page, double cost) {
        String announcement() { return TowerMath.announcement(magic.displayName, page); }
        int colorArgb() { return BornMagic.colorOf(magic); }
        int rgb() { return colorArgb() & 0xFFFFFF; }
    }

    private static List<List<Entry>> byMagic;
    private static TowerMath.Deck deck;

    /** The library, grouped by magic (built once, on first use). */
    static synchronized List<List<Entry>> library() {
        if (byMagic == null) {
            List<List<Entry>> out = new ArrayList<>();
            for (MagicType m : MagicType.values()) {
                if (m.restricted()) continue;
                List<Entry> pages = new ArrayList<>();
                try {
                    GrimoireBook book = NUSkills.bookFor(m);
                    if (book != null) {
                        for (int i = 0; i < book.familyCount(); i++) {
                            BookPage p = book.page(i);
                            if (p != null && p.costPercent() >= 5) pages.add(new Entry(m, p.name(), p.costPercent()));
                        }
                    }
                } catch (Throwable t) {
                    LogUtils.getLogger().warn("[nusmp] the grimoire towers could not read the pages of {}: {}", m.displayName, t.toString());
                }
                if (pages.isEmpty()) {                                  // a book that cannot be read still answers, in the four tiers
                    pages.add(new Entry(m, m.word + " Bolt", 8));
                    pages.add(new Entry(m, m.word + " Lance", 12));
                    pages.add(new Entry(m, m.word + " Nova", 20));
                    pages.add(new Entry(m, m.word + " Rain", 35));
                }
                out.add(pages);
            }
            byMagic = out;
            deck = new TowerMath.Deck(out.size(), System.nanoTime());
        }
        return byMagic;
    }

    /** The next answer of the towers: the next magic from the deck, a random page of it. */
    static synchronized Entry draw(RandomSource rng) {
        List<List<Entry>> lib = library();
        List<Entry> pages = lib.get(deck.next() % lib.size());
        return pages.get(rng.nextInt(pages.size()));
    }

    /** Announces the page to everyone within 64 blocks: the action bar names it, the chat keeps it. */
    static void announce(LuciusBoss b, Entry e) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        LuciusFx.bar(sl, b.position(), 64, Component.literal(e.announcement()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    // ================================================================ casting
    /**
     * Casts 'e' at 'target' as the spell of its tier. 'salt' picks between a nova and a lingering field for a zone page. Everything is
     * marked or aimed from the position of the foes now; the volley size follows the boss's power.
     */
    static void cast(LuciusBoss b, Entry e, LivingEntity target, int salt) {
        if (!(b.level() instanceof ServerLevel sl) || target == null) return;
        TowerMath.Shape shape = TowerMath.shapeFor(e.cost(), salt);
        int n = TowerMath.count(shape, b.tuning().castSlots());
        float base = TowerMath.baseDamage(shape);
        switch (shape) {
            case BOLT -> bolts(b, sl, e, target, n, base);
            case LANCE -> lances(b, sl, e, target, n, base);
            case NOVA -> novas(b, sl, e, target, n, base);
            case FIELD -> field(b, sl, e, target, base);
            case RAIN -> rain(b, sl, e, target, n, base, TowerMath.markTicks(TowerMath.Shape.RAIN));
        }
    }

    private static void hit(LuciusBoss b, LivingEntity v, float base, Entry e) {
        if (b.strike(v, base, true)) LuciusFx.rider(e.magic().hit, v, b);
    }

    private static Vec3 hand(LuciusBoss b) { return b.position().add(0, b.getBbHeight() * 0.75, 0); }

    /** A marked impact: a ring and a column on the ground for 'lead' ticks, then 'impact' runs (skipped if the boss is gone). */
    private static void marked(LuciusBoss b, ServerLevel sl, Vec3 at, double radius, int lead, int rgb, Runnable impact) {
        final Vec3 c = at;
        final boolean small = radius < 3;                          // a rain of marks stays light: no inner ring, a short column
        SpellRuntime.zone(sl, lead, 4, LuciusFx.safe("tower mark", age -> {
            if (!b.isAlive()) return;
            Tells.ring(sl, c, radius, rgb, 1.3f, (int) Math.min(64, 8 + radius * 6));
            if (!small) Tells.ring(sl, c, Math.max(0.5, radius * (lead - age) / (double) Math.max(1, lead)), LuciusFx.PALE, 1.1f, 20);
            Tells.column(sl, c, small ? 3.0 : 3.5, rgb);
        }));
        b.later(lead, LuciusFx.safe("tower impact", impact));
    }

    // ---------------------------------------------------------------- bolt
    private static void bolts(LuciusBoss b, ServerLevel sl, Entry e, LivingEntity target, int n, float base) {
        final int a = e.colorArgb(), rgb = e.rgb();
        for (int i = 0; i < n; i++) {
            final double off = n == 1 ? 0 : (i - (n - 1) / 2.0) * 0.16;
            b.later(i * 2, LuciusFx.safe("tower bolt", () -> {
                Vec3 from = hand(b);
                Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter().subtract(from) : b.getViewVector(1f);
                Vec3 dir = LuciusFx.rotateY(aim, off);
                LuciusFx.born(sl, VfxShape.BORN_BEAM, from, from.add(dir.normalize().scale(9)), a, LuciusFx.GOLD_ARGB, 12, 0.6f, 0);
                new LuciusFx.Shot(b, from, dir, 0.9, 40, 0.55, 0.07, target, rgb, v -> b.isFoe(v), v -> hit(b, v, base, e)).launch(sl);
            }));
        }
        LuciusFx.sound(sl, b.position(), SoundEvents.EVOKER_CAST_SPELL, 1.4f, 1.1f);
    }

    // ---------------------------------------------------------------- lance
    private static void lances(LuciusBoss b, ServerLevel sl, Entry e, LivingEntity target, int n, float base) {
        final int a = e.colorArgb(), rgb = e.rgb();
        List<LivingEntity> foes = b.foes(34);
        for (int i = 0; i < n; i++) {
            LivingEntity at = i == 0 || foes.isEmpty() ? target : foes.get((i - 1) % foes.size());
            if (at == null || !at.isAlive()) at = target;
            final Vec3 from = hand(b);
            final Vec3 dir = at.getBoundingBox().getCenter().subtract(from).normalize();
            final Vec3 end = LuciusFx.clipWall(sl, b, from, from.add(dir.scale(26)));
            final int lead = TowerMath.markTicks(TowerMath.Shape.LANCE);
            SpellRuntime.zone(sl, lead, 3, LuciusFx.safe("tower lance mark", age -> {
                if (b.isAlive()) Tells.line(sl, from, end, rgb);
            }));
            b.later(lead + i * 3, LuciusFx.safe("tower lance", () -> {
                LuciusFx.born(sl, VfxShape.BORN_BEAM, from, end, a, LuciusFx.GOLD_ARGB, 14, 1.1f, 8);
                LuciusFx.sound(sl, from, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 1.8f);
                for (LivingEntity v : b.foesAround(from.add(end.subtract(from).scale(0.5)), from.distanceTo(end) / 2 + 2)) {
                    if (ChronoMath.distToSegment(v.getX(), v.getZ(), from.x, from.z, end.x, end.z) <= 1.4 + v.getBbWidth() / 2
                            && v.getY() > from.y - 3.5 && v.getY() < from.y + 3.5) hit(b, v, base, e);
                }
            }));
        }
        LuciusFx.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 1.3f);
    }

    // ---------------------------------------------------------------- nova
    private static void novas(LuciusBoss b, ServerLevel sl, Entry e, LivingEntity target, int n, float base) {
        final int a = e.colorArgb(), rgb = e.rgb();
        final double r = TowerMath.radius(TowerMath.Shape.NOVA);
        List<LivingEntity> foes = b.foes(36);
        for (int i = 0; i < n; i++) {
            LivingEntity at = i == 0 || foes.isEmpty() ? target : foes.get((i - 1) % foes.size());
            if (at == null || !at.isAlive()) at = target;
            final Vec3 c = ZogratisBoss.ground(sl, at.position());
            marked(b, sl, c, r, TowerMath.markTicks(TowerMath.Shape.NOVA), rgb, () -> {
                LuciusFx.born(sl, VfxShape.BORN_NOVA, c.add(0, 0.2, 0), c.add(0, 1, 0), a, LuciusFx.GOLD_ARGB, 22, (float) r, 4);
                LuciusFx.sound(sl, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4f, 0.7f);
                for (LivingEntity v : b.foesAround(c, r + 1)) {
                    double dx = v.getX() - c.x, dz = v.getZ() - c.z;
                    if (dx * dx + dz * dz <= r * r && Math.abs(v.getY() - c.y) <= 4) hit(b, v, base, e);
                }
            });
        }
    }

    // ---------------------------------------------------------------- field
    private static void field(LuciusBoss b, ServerLevel sl, Entry e, LivingEntity target, float base) {
        final int a = e.colorArgb(), rgb = e.rgb();
        final double r = TowerMath.radius(TowerMath.Shape.FIELD);
        final Vec3 c = ZogratisBoss.ground(sl, target.position());
        final int life = 60;
        marked(b, sl, c, r, TowerMath.markTicks(TowerMath.Shape.FIELD), rgb, () -> {
            LuciusFx.born(sl, VfxShape.BORN_WAVE, c.add(0, 0.2, 0), c.add(0, 1.2, 0), a, LuciusFx.GOLD_ARGB, life, (float) r, 2);
            SpellRuntime.zone(sl, life, 4, LuciusFx.safe("tower field", age -> {
                if (!b.isAlive()) return;
                if (age % 8 == 0) Tells.ring(sl, c, r, rgb, 1.2f, 40);
                if (age % 20 != 0) return;
                for (LivingEntity v : b.foesAround(c, r + 1)) {
                    double dx = v.getX() - c.x, dz = v.getZ() - c.z;
                    if (dx * dx + dz * dz <= r * r && Math.abs(v.getY() - c.y) <= 4) hit(b, v, base, e);
                }
            }));
        });
    }

    // ---------------------------------------------------------------- rain
    /** 'n' bolts fall on spots round the foes (the target first), each marked for 'lead' ticks. */
    static void rain(LuciusBoss b, ServerLevel sl, Entry e, LivingEntity target, int n, float base, int lead) {
        final int a = e.colorArgb(), rgb = e.rgb();
        final double r = TowerMath.radius(TowerMath.Shape.RAIN);
        List<LivingEntity> foes = b.foes(40);
        for (int i = 0; i < n; i++) {
            LivingEntity at = i == 0 || foes.isEmpty() ? target : foes.get(i % foes.size());
            if (at == null || !at.isAlive()) at = target;
            double ang = b.getRandom().nextDouble() * Math.PI * 2, dist = i == 0 ? 0 : 1.5 + b.getRandom().nextDouble() * 4.5;
            final Vec3 c = ZogratisBoss.ground(sl, at.position().add(Math.cos(ang) * dist, 0, Math.sin(ang) * dist));
            b.later(i * 2, LuciusFx.safe("tower rain", () -> marked(b, sl, c, r, lead, rgb, () -> {
                LuciusFx.born(sl, VfxShape.BORN_RAIN, c.add(0, 9, 0), c, a, LuciusFx.GOLD_ARGB, 14, 1.0f, 0);
                LuciusFx.sound(sl, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.9f);
                for (LivingEntity v : b.foesAround(c, r + 1)) {
                    double dx = v.getX() - c.x, dz = v.getZ() - c.z;
                    if (dx * dx + dz * dz <= r * r && Math.abs(v.getY() - c.y) <= 4) hit(b, v, base, e);
                }
            })));
        }
    }

    // ================================================================ the ultimate
    /**
     * The Grimoire Tower Storm: {@code salvos} answers of the towers, 18 ticks apart, each a different magic named on the action bar and
     * each a rain of marked bolts (every second one a ring of novas on the party): a long, readable barrage.
     */
    static void storm(LuciusBoss b, LivingEntity target, int salvos) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        for (int k = 0; k < salvos; k++) {
            final int salvo = k;
            b.later(k * 18, LuciusFx.safe("tower storm", () -> {
                LivingEntity t = target.isAlive() ? target : b.getTarget();
                if (t == null || !t.isAlive()) return;
                Entry e = draw(b.getRandom());
                LuciusFx.bar(sl, b.position(), 64, Component.literal(e.announcement()).withStyle(ChatFormatting.GOLD));
                LuciusFx.born(sl, VfxShape.BORN_NOVA, b.position().add(0, 0.2, 0), b.position().add(0, 1, 0), e.colorArgb(), LuciusFx.GOLD_ARGB, 16, 3f, 1);
                if (salvo % 2 == 0) rain(b, sl, e, t, TowerMath.count(TowerMath.Shape.RAIN, b.tuning().castSlots()), LuciusTable.STORM_BOLT, 16);
                else novas(b, sl, e, t, TowerMath.count(TowerMath.Shape.NOVA, b.tuning().castSlots()), LuciusTable.STORM_BOLT);
            }));
        }
    }
}
