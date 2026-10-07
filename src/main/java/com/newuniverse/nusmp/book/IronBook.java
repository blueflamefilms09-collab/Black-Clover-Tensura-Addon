package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** Iron Magic: iron bullets, falling blades, shackles, the guardian bell and the iron god of war. */
public class IronBook extends GrimoireBook {
    static final int COLOR = 0xFFB0B8C8;

    private final List<BookPage> pages = List.of(
            BookPage.starter("iron_strike", "Iron Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.IRON_FX1, null, ElementBook.NONE)),
            BookPage.starter("iron_bullet", "Iron Bullet", ElementBook.bolt(10f, 1.8, 0.6, 16, false, 2.5, VfxShape.IRON_FX1, VfxShape.IRON_FX3,
                    ElementBook.all(ElementBook.knock(0.9), IronArts.heavy(40, 1)))).withAnim("out"),
            BookPage.starter("iron_shrapnel", "Iron Shrapnel", ElementBook.cone(6f, 7, 0.5, VfxShape.IRON_FX1, IronArts.splinters())).withAnim("sweep"),
            BookPage.mid("iron_sword_rain", "Iron Sword Rain", IronArts::swordRain).withAnim("up"),
            BookPage.mid("iron_shackles", "Iron Shackles", IronArts::shackles),
            BookPage.mid("iron_charge", "Iron Charge", ElementBook.dash(8f, 9, false, VfxShape.IRON_FX1,
                    ElementBook.all(ElementBook.knock(1.2), IronArts.heavy(40, 1)))),
            BookPage.mid("iron_fist", "Iron Fist", ElementBook.empower(400, 4f, false, VfxShape.IRON_FX3,
                    () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0))),
            BookPage.mid("iron_wall", "Iron Wall", IronArts::wall),
            BookPage.mid("immovable_guardian_deity", "Immovable Guardian Deity", IronArts::guardian),
            BookPage.zone("rust_field", "Rust Field", IronArts::rustField),
            BookPage.signature("iron_god_of_war", "Iron God of War", IronArts::godOfWar).withAnim("signature"),
            BookPage.daily("iron_anvil", "Colossus Anvil", IronArts::anvil).withAnim("slam").withCooldown(6000));

    public IronBook() { super(MagicType.IRON, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
