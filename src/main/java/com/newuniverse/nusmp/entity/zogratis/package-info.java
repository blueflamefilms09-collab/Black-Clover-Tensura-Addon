/**
 * The Zogratis family: Dante, Vanica and Zenon (the Dark Triad) and Lucius, rulers of the Spade Kingdom, each bound to a
 * highest-ranking devil. {@link com.newuniverse.nusmp.entity.zogratis.ZogratisBoss} is the shared fight framework.
 *
 * <h2>Writing a boss</h2>
 * <ol>
 *   <li>Extend {@code ZogratisBoss}; constructor {@code (EntityType<X> type, Level level)}; a static {@code createAttributes()}
 *       (start from {@code ZogratisBoss.bossAttributes}); {@code bossName()}, {@code devil()} and {@code moves()}.</li>
 *   <li>Build moves with {@code Move.builder(id, label)...run((boss, target) -> ...)}. Ids are unique per boss. The wind-up is the
 *       telegraph (never under 8 ticks once a move has one); give each big move its own {@code .tell(...)} or accept the default
 *       ring; an ultimate ({@code .ultimate()}) leaves the boss exposed for 60 ticks.</li>
 *   <li>Read the power from {@code boss.power()} and {@code boss.tuning()} (damage, volley size {@code castSlots()}, disciple cap);
 *       hurt with {@code boss.strike(victim, base, magic)} or {@code boss.bossDamage(victim, base)}, never with a raw
 *       {@code victim.hurt}. Hits are capped at the pvp share of a player's max health, so keep multi-hit moves to small hits.</li>
 *   <li>Follow-ups: {@code boss.later(ticks, runnable)} (skipped when the boss is gone) or {@code SpellRuntime.zone}, checking
 *       {@code boss.isAlive()} inside.</li>
 *   <li>World changes: only through {@code boss.tryChange} / {@code tryDestroy} (a few per tick) or {@code enqueueBlockChange}
 *       (anything big). They do nothing unless the griefBlocks rule is on, are recorded in the {@code BlockLedger}, and are put
 *       back when the boss dies, is removed or the fight is abandoned. Never call {@code setBlock} yourself.</li>
 *   <li>Hooks: {@code onPowerChanged}, {@code onGatesOpened}, {@code bossTick}, {@code onFightStart}, {@code onFightEnd};
 *       {@code gatesForm()} chooses the 100% form; {@code filterDamage} lets a boss dodge or absorb a hit.</li>
 * </ol>
 * The pure classes ({@code PowerCurve}, {@code ManaPool}, {@code MoveSpec}, {@code MoveBook}, {@code MoveScheduler},
 * {@code LedgerQueue}, {@code DiscipleStats}, {@code DamageMath}, {@code DevilStyle}) have no Minecraft types and are unit tested.
 */
package com.newuniverse.nusmp.entity.zogratis;
