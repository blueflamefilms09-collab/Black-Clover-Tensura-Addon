"""The quest line: groups, reward tables and the chapter modules (single source of truth, read by gen_quests.py).

Why: one entry point that assembles the book in a fixed order. Each spec_q_*.py module adds its chapters; the reward tables are defined
here because chapters of every module share them.
"""
import spec_qlib as Q

import spec_q_journey
import spec_q_arcs
import spec_q_bosses


def build_registry():
    reg = Q.Registry()
    reg.group("journey", "The Journey", "nusmp:grimoire")
    reg.group("arcs", "The Anime Arcs", "minecraft:white_banner")
    reg.group("bosses", "Bosses and Challenges", "minecraft:wither_skeleton_skull")
    define_tables(reg)
    spec_q_journey.build(reg)
    spec_q_arcs.build(reg)
    spec_q_bosses.build(reg)
    return reg


def define_tables(reg):
    reg.table("journey_cache", "Journey Cache", [
        ("minecraft:emerald", 3, 20), ("minecraft:gold_ingot", 4, 20), ("minecraft:experience_bottle", 8, 15),
        ("nusmp:magic_stone", 4, 15), ("minecraft:lapis_lazuli", 16, 10), ("minecraft:ender_pearl", 4, 10),
        ("minecraft:diamond", 1, 8), ("minecraft:golden_apple", 2, 6), ("minecraft:enchanted_golden_apple", 1, 1)])
    reg.table("magic_cache", "Mage's Cache", [
        ("minecraft:amethyst_shard", 8, 20), ("minecraft:lapis_lazuli", 24, 20), ("minecraft:glowstone_dust", 16, 15),
        ("minecraft:blaze_powder", 8, 15), ("minecraft:experience_bottle", 12, 15), ("nusmp:magic_stone", 8, 12),
        ("minecraft:ender_eye", 2, 8), ("minecraft:echo_shard", 1, 3)])
    reg.table("dungeon_cache", "Delver's Cache", [
        ("nusmp:magic_stone", 12, 25), ("minecraft:iron_ingot", 8, 15), ("minecraft:gold_ingot", 6, 15),
        ("minecraft:diamond", 2, 8), ("minecraft:golden_apple", 2, 8), ("minecraft:honey_bottle", 4, 12),
        ("minecraft:experience_bottle", 10, 12), ("minecraft:ominous_trial_key", 1, 2)])
    reg.table("arc_cache", "Arc Cache", [
        ("minecraft:emerald", 8, 20), ("minecraft:diamond", 2, 14), ("minecraft:experience_bottle", 16, 18),
        ("minecraft:golden_apple", 3, 10), ("minecraft:netherite_scrap", 1, 3), ("minecraft:totem_of_undying", 1, 1),
        ("nusmp:magic_stone", 16, 14)])
    reg.table("boss_trophy", "Boss Trophy", [
        ("minecraft:diamond", 4, 20), ("minecraft:netherite_scrap", 2, 12), ("minecraft:experience_bottle", 32, 20),
        ("minecraft:enchanted_golden_apple", 1, 6), ("minecraft:totem_of_undying", 1, 4), ("minecraft:nether_star", 1, 1),
        ("nusmp:magic_stone", 24, 15)])
    reg.table("legend_trophy", "Trophy of Legends", [
        ("minecraft:netherite_ingot", 1, 10), ("minecraft:nether_star", 1, 6), ("minecraft:enchanted_golden_apple", 2, 10),
        ("minecraft:diamond", 8, 20), ("minecraft:experience_bottle", 64, 20), ("minecraft:totem_of_undying", 2, 6)])
    reg.table("choice_boon", "Choose a Boon", [
        ("minecraft:diamond", 6, 1), ("minecraft:golden_apple", 8, 1), ("minecraft:experience_bottle", 48, 1),
        ("minecraft:netherite_scrap", 4, 1), ("minecraft:ender_pearl", 16, 1)])
    reg.table("choice_mage", "The Mage's Path", [
        ("minecraft:lapis_lazuli", 48, 1), ("minecraft:amethyst_shard", 32, 1), ("minecraft:experience_bottle", 32, 1),
        ("minecraft:echo_shard", 2, 1), ("minecraft:glowstone_dust", 32, 1), ("minecraft:ender_eye", 8, 1)])
    reg.table("choice_knight", "The Knight's Path", [
        ("minecraft:diamond", 8, 1), ("minecraft:netherite_scrap", 4, 1), ("minecraft:golden_apple", 6, 1),
        ("minecraft:shield", 1, 1), ("minecraft:totem_of_undying", 1, 1), ("minecraft:iron_ingot", 32, 1)])
    reg.table("choice_delver", "The Delver's Path", [
        ("nusmp:magic_stone", 48, 1), ("minecraft:ender_pearl", 16, 1), ("minecraft:honey_bottle", 8, 1),
        ("minecraft:recovery_compass", 1, 1), ("minecraft:ominous_trial_key", 2, 1), ("minecraft:golden_apple", 4, 1)])
