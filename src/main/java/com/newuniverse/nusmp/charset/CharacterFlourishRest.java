package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterPagesRest;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why (owner: "make the character grimoire VFX look 10x cooler than the basic grimoire users"): every cast from a grimoire bound to one of the characters below gets, on top of the page's own
 * picture, two more layers of existing {@link VfxShape}s in the character's own colours (a ground / follow accent and a target accent): Charlotte's blue and red roses, Jack's green blades, Rill's paint,
 * Dorothy's dream, Zenon's bone and torn space, Vanica's blood, Dante's gravity, Lucius's souls and halo, Rades's corpses, Gadjah's gold lightning, Mars's crystal, Kahono's notes and Kiato's dance. A basic
 * grimoire gets nothing of it.
 *
 * <p>Cost (rule 11), all server side and bounded: at most 2 effect packets per cast, at most one flourish per player per 12 ticks (the stamp is on the player's own data, nothing static), nothing for a
 * page that costs nothing (Summon / Store / Draw / Mana Zone) and nothing for the pages {@code CharacterPagesRest} added (they already layer 4 to 6 shapes themselves); the packets go through
 * {@code VfxSpawn} (nearby players only, inside the per-tick effect budget). One aim query per cast. No entity, block, chunk or per-tick work. The pilots and the other builders' characters are not in
 * the table, so they are untouched. Switching {@code magic.characterSpellSets} off removes it.
 */
public final class CharacterFlourishRest {
    private CharacterFlourishRest() {}

    private static final String STAMP = "nusmp_flourish_rest";
    private static final int GAP = 12;

    /** The extra layers of a character (empty for everyone else): see {@link FlourishTable}. */
    public static List<FlourishTable.Layer> layersOf(String canon) { return FlourishTable.layersOf(canon); }

    /** The characters that have a flourish. */
    public static java.util.Set<String> characters() { return FlourishTable.characters(); }

    /** Called after every successful cast of a grimoire page (server only). */
    public static void after(ServerPlayer player, ManasSkillInstance instance, BookPage page) {
        if (page == null || page.costPercent() <= 0 || !CharacterSetConfig.enabled() || player.isRemoved() || !player.isAlive()) return;
        CompoundTag tag = instance.getTag();
        String canon = tag == null ? "" : tag.getString("Canon");
        List<FlourishTable.Layer> layers = FlourishTable.layersOf(canon);
        if (layers.isEmpty()) return;
        if (instance.getSkill() instanceof GrimoireBook b && b.getRegistryName() != null && CharacterPagesRest.isAdded(b.getRegistryName().getPath(), page.id())) return;      // those pages layer themselves
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(STAMP) > now) return;
        player.getPersistentData().putLong(STAMP, now + GAP);
        Vec3 eye = player.getEyePosition(), feet = player.position(), aim = GrimoireBook.aim(player, 18);
        for (FlourishTable.Layer l : layers) {
            VfxShape shape = VfxShape.valueOf(l.shape());
            switch (l.where()) {
                case FEET -> VfxSpawn.send(player.serverLevel(), shape, feet.add(0, 0.1, 0), feet.add(0, 1, 0), l.color(), l.ticks(), l.power());
                case AIM -> VfxSpawn.send(player.serverLevel(), shape, aim, aim.add(0, 1, 0), l.color(), l.ticks(), l.power());
                case LINE -> VfxSpawn.send(player.serverLevel(), shape, eye, aim, l.color(), l.ticks(), l.power());
                case FOLLOW -> VfxSpawn.sendFollowing(player.serverLevel(), shape, player, feet.add(0, 1, 0), l.color(), l.ticks(), l.power());
            }
        }
    }
}
