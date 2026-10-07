package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.entity.PaintedConstructEntity;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 0.49 Game Magic (new; the wiki's user is Gifso of the Spade Kingdom): "a magic that oversees and manipulates events within an
 * area, like a game". It can be cast without a grimoire, but at full strength the place has to be prepared: magic poured into it
 * beforehand. Here that preparation is the <b>Game Board</b>, and everything else plays on it by D&amp;D rules (d20 + your EP
 * modifier, natural 1 / natural 20).
 * <ul>
 *   <li><b>Game Board</b> (preparation): a 16-block board of glowing squares where you stand, for two minutes. Every 10 s each
 *       piece on it rolls a saving throw: foes who fail take a random curse, allies who succeed a random blessing. Spells cast on
 *       your board are 50% stronger.</li>
 *   <li><b>Monster Toy</b> (the wiki's offensive spell): a game-piece monster rises on the board, its size rolled on a d20 (a
 *       random beast, a chimera, or on a high roll a giant). It fights and casts spells for you (summon AI).</li>
 *   <li><b>Temple Shuffle</b> (the wiki's supplementary spell): every foe on the board is moved to a random square, and your
 *       allies close in round you. Needs your board.</li>
 *   <li><b>Initiative</b>: you and every foe near roll d20; the ones you beat lose their turn (slowed and their magic jammed),
 *       and if you beat them all you take the next move (speed).</li>
 *   <li><b>Dungeon Master's Verdict</b> (signature): every foe on the board makes a DC 12 saving throw; failures take 4d6
 *       (x your EP), a natural 1 doubles it; a natural 20 on your own roll makes it a critical (everything fails).</li>
 *   <li><b>Trap Squares</b>: six hidden squares on your board; a foe who steps on one is snared and hurt.</li>
 * </ul>
 */
public class GameBook extends GrimoireBook {
    static final int GOLD = 0xFFE8C04A, RED = 0xFFFF4A4A, GREEN = 0xFF6AFF8A;
    static final String K_BOARD = "nusmp_game_board";
    static final double BOARD_R = 16;

    private final List<BookPage> pages = List.of(
            BookPage.starter("game_board", "Game Board", GameBook::board).withCooldown(1200),
            BookPage.starter("monster_toy", "Monster Toy", GameBook::monsterToy).withCooldown(600),
            BookPage.mid("temple_shuffle", "Temple Shuffle", GameBook::templeShuffle).withCooldown(400),
            BookPage.mid("initiative", "Initiative", GameBook::initiative).withCooldown(500),
            BookPage.signature("dm_verdict", "Dungeon Master's Verdict", GameBook::verdict).withCooldown(900),
            BookPage.mid("trap_squares", "Trap Squares", GameBook::traps).withCooldown(600));

    public GameBook() { super(MagicType.GAME, GOLD); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    // ---------------------------------------------------------------- the board
    /** The caster's board centre, or null if none is live in this dimension. */
    static Vec3 board(ServerPlayer p) {
        var t = p.getPersistentData().getCompound(K_BOARD);
        if (t.isEmpty() || p.level().getGameTime() >= t.getLong("Until") || !t.getString("Dim").equals(p.level().dimension().location().toString())) return null;
        return new Vec3(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"));
    }

    static boolean onBoard(Vec3 board, LivingEntity e) { return board != null && e.position().distanceToSqr(board) <= BOARD_R * BOARD_R; }

    /** Spells on your own board are 50% stronger. */
    static float boardBonus(ServerPlayer p, Vec3 at) {
        Vec3 b = board(p);
        return b != null && at.distanceToSqr(b) <= BOARD_R * BOARD_R ? 1.5f : 1f;
    }

    static int d20(ServerPlayer p) { return 1 + p.getRandom().nextInt(20); }

    static void say(ServerPlayer p, String s, ChatFormatting c) { p.displayClientMessage(Component.literal(s).withStyle(c), true); }

    static void showRoll(ServerPlayer p, Vec3 to, int face) {
        Vec3 from = p.getEyePosition().add(p.getViewVector(1f).scale(0.8)).add(0, 0.3, 0);
        long seed = (p.getRandom().nextLong() & ~31L) | face;
        VfxSpawn.send(p.serverLevel(), new VfxPayload(VfxShape.DICE_D20.ordinal(), from, to, face == 20 ? 0xFFFFC040 : face == 1 ? 0xFFFF3030 : GOLD, 40, 0.8f, -1, seed));
    }

    static boolean board(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        var t = new net.minecraft.nbt.CompoundTag();
        t.putDouble("X", p.getX()); t.putDouble("Y", p.getY()); t.putDouble("Z", p.getZ());
        t.putLong("Until", level.getGameTime() + 2400);
        t.putString("Dim", level.dimension().location().toString());
        p.getPersistentData().put(K_BOARD, t);
        Vec3 c = p.position();
        VfxSpawn.send(level, VfxShape.GAME_BOARD, c.add(0, 0.05, 0), c, GOLD, 2400, (float) BOARD_R);
        level.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.5f, 0.6f);
        say(p, "The board is set. Let the game begin.", ChatFormatting.GOLD);
        // every 10 s: saving throws for every piece on the board
        SpellRuntime.zone(level, 2400, 200, age -> {
            Vec3 bc = board(p);
            if (bc == null || !p.isAlive()) return;
            int mod = EnergyBridge.modifier(p);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(bc, bc).inflate(BOARD_R), x -> x.isAlive() && onBoard(bc, x))) {
                boolean ally = e == p || e.isAlliedTo(p);
                int roll = 1 + p.getRandom().nextInt(20);
                if (ally && roll + mod >= 10) bless(e, roll);
                else if (!ally && roll < 10 + mod) curse(e, roll);
            }
        });
        return true;
    }

    /** A random blessing (a natural 20 gives two). */
    static void bless(LivingEntity e, int roll) {
        var pick = List.of(MobEffects.MOVEMENT_SPEED, MobEffects.DAMAGE_BOOST, MobEffects.DAMAGE_RESISTANCE, MobEffects.REGENERATION, MobEffects.JUMP, MobEffects.LUCK);
        for (int k = 0; k < (roll == 20 ? 2 : 1); k++)
            e.addEffect(new MobEffectInstance(pick.get(e.getRandom().nextInt(pick.size())), 200, roll >= 15 ? 1 : 0));
    }

    /** A random curse (a natural 1 gives two, one of them a Tensura status). */
    static void curse(LivingEntity e, int roll) {
        var pick = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN, MobEffects.BLINDNESS, MobEffects.UNLUCK, MobEffects.GLOWING);
        e.addEffect(new MobEffectInstance(pick.get(e.getRandom().nextInt(pick.size())), 160, roll <= 3 ? 1 : 0));
        if (roll == 1) EnergyBridge.effect(e, e.getRandom().nextBoolean() ? "fragility" : "silence", 100, 0);
    }

    // ---------------------------------------------------------------- spells
    static boolean monsterToy(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = GrimoireBook.aim(p, 16);
        int roll = d20(p) + EnergyBridge.modifier(p);
        showRoll(p, at, Math.min(20, roll));
        PaintedConstructEntity.Kind kind = roll >= 18 ? PaintedConstructEntity.Kind.GIANT : roll >= 8 ? PaintedConstructEntity.Kind.BEAST : PaintedConstructEntity.Kind.KNIGHT;
        float power = (0.8f + roll / 20f) * boardBonus(p, at) * EnergyBridge.scale(p);
        PaintStudio.Paint paint = PaintStudio.Paint.values()[p.getRandom().nextInt(PaintStudio.Paint.values().length)];
        SpellRuntime.later(p.serverLevel(), 20, () -> {
            var e = PaintedConstructEntity.spawn(p, kind, at, p.getYRot() + 180, paint, power, 900);
            if (e != null) e.setCustomName(Component.literal("Monster Toy: " + e.getCustomName().getString().replace("Painted ", "")));
        });
        say(p, "Monster Toy: rolled " + roll + "!", ChatFormatting.GOLD);
        return true;
    }

    static boolean templeShuffle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 bc = board(p);
        if (bc == null) { fail(p, "Temple Shuffle needs your Game Board (cast it first)."); return false; }
        ServerLevel level = p.serverLevel();
        int moved = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(bc, bc).inflate(BOARD_R), x -> x.isAlive() && x != p && onBoard(bc, x))) {
            Vec3 to;
            if (e.isAlliedTo(p)) to = p.position().add(p.getRandom().nextGaussian() * 1.5, 0, p.getRandom().nextGaussian() * 1.5);
            else {
                double a = p.getRandom().nextDouble() * Math.PI * 2, r = Math.sqrt(p.getRandom().nextDouble()) * (BOARD_R - 1);
                to = bc.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            }
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(to));
            if (Math.abs(top.getY() - bc.y) > 6) continue;                          // only to squares on the board's level
            VfxSpawn.send(level, VfxShape.SPATIAL_RIFT, e.position(), e.position().add(0, 1, 0), GOLD, 12, 0.6f);
            e.teleportTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
            VfxSpawn.send(level, VfxShape.SPATIAL_RIFT, e.position(), e.position().add(0, 1, 0), GOLD, 12, 0.6f);
            moved++;
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 0.8f);
        say(p, "Temple Shuffle: " + moved + " pieces moved.", ChatFormatting.GOLD);
        return true;
    }

    static boolean initiative(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int mine = d20(p) + EnergyBridge.modifier(p), beaten = 0;
        List<LivingEntity> foes = GrimoireBook.around(p, p.position(), 12);
        if (foes.isEmpty()) { fail(p, "No one to roll against."); return false; }
        showRoll(p, p.position().add(p.getViewVector(1f).scale(3)), Math.min(20, mine));
        for (LivingEntity t : foes) {
            int theirs = 1 + p.getRandom().nextInt(20);
            if (mine > theirs) {
                beaten++;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                EnergyBridge.effect(t, "silence", 60, 0);
                VfxSpawn.send(p.serverLevel(), VfxShape.KOTO_SHATTER, t.getBoundingBox().getCenter(), t.position(), GOLD, 16, 0.5f);
            }
        }
        if (beaten == foes.size()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 2));
        say(p, "Initiative " + mine + ": you act before " + beaten + " of " + foes.size() + ".", ChatFormatting.GOLD);
        return true;
    }

    static boolean verdict(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 bc = board(p);
        Vec3 c = bc != null ? bc : p.position();
        double r = bc != null ? BOARD_R : 8;
        int mine = d20(p);
        boolean crit = mine == 20;
        showRoll(p, c.add(0, 2, 0), mine);
        float pw = EnergyBridge.scale(p) * (bc != null ? 1.5f : 1f);
        int hit = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            int save = 1 + p.getRandom().nextInt(20);
            if (!crit && save >= 12) continue;
            int dmg = 0;
            for (int k = 0; k < 4; k++) dmg += 1 + p.getRandom().nextInt(6);
            if (save == 1) dmg *= 2;
            b.hurtAs(i, p, t, mode, dmg * pw * (crit ? 1.5f : 1f), TensuraDamageTypes.LIGHT_ELEMENTAL);
            VfxSpawn.send(p.serverLevel(), VfxShape.LIGHTNING_SPEAR, t.position().add(0, 8, 0), t.position(), GOLD, 10, 0.8f);
            hit++;
        }
        say(p, crit ? "NATURAL 20 - the Dungeon Master's verdict is final! (" + hit + ")" : "Verdict: " + hit + " failed their saving throw.", ChatFormatting.GOLD);
        return true;
    }

    static boolean traps(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 bc = board(p);
        if (bc == null) { fail(p, "Trap Squares needs your Game Board (cast it first)."); return false; }
        ServerLevel level = p.serverLevel();
        for (int k = 0; k < 6; k++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2, r = 2 + p.getRandom().nextDouble() * (BOARD_R - 3);
            Vec3 sq = bc.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            SpellRuntime.zone(level, 1200, 5, age -> {
                for (LivingEntity t : GrimoireBook.around(p, sq, 1.2)) {
                    if (t.getPersistentData().getLong("nusmp_game_trapped") > level.getGameTime()) continue;
                    t.getPersistentData().putLong("nusmp_game_trapped", level.getGameTime() + 60);
                    b.hurtAs(i, p, t, mode, 6f * EnergyBridge.scale(p), TensuraDamageTypes.MAGIC_GENERIC);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                    VfxSpawn.send(level, VfxShape.GAME_BOARD, sq.add(0, 0.06, 0), sq, RED, 20, 1f);
                    level.playSound(null, BlockPos.containing(sq), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, 0.6f);
                }
            });
        }
        say(p, "Six squares are trapped. Only you know which.", ChatFormatting.GOLD);
        return true;
    }
}
