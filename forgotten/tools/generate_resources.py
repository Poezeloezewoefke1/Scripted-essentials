#!/usr/bin/env python3
"""Generates the mod's assets and data from the tables below.

    python3 forgotten/tools/generate_resources.py

Writes textures, models, blockstates, item definitions, the English language file, loot
tables, tags, crafting recipes, Paradox Engine recipes and advancements into
mod/src/main/resources. The output is committed; rerun after editing the tables.
"""
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(__file__))
import textures as T  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..', 'mod', 'src', 'main', 'resources')
NS = 'forgotten'
ASSETS = os.path.join(ROOT, 'assets', NS)
DATA = os.path.join(ROOT, 'data', NS)
MC_DATA = os.path.join(ROOT, 'data', 'minecraft')

# --------------------------------------------------------------------------- concepts

CONCEPTS = {
    'gravity': ('#5b6cff', 'Gravity'), 'memory': ('#c9a2ff', 'Memory'), 'distance': ('#3fd0c9', 'Distance'),
    'identity': ('#ff7ad9', 'Identity'), 'causality': ('#ffb347', 'Causality'), 'hunger': ('#b5482f', 'Hunger'),
    'silence': ('#2c2c3a', 'Silence'), 'possibility': ('#7dff7a', 'Possibility'), 'finality': ('#e8e3c8', 'Finality'),
    'recursion': ('#4bc94b', 'Recursion'), 'absence': ('#8a8a9a', 'Absence'), 'witness': ('#f5f5ff', 'Witness'),
}

# --------------------------------------------------------------------------- items
# id: (English name, description, texture factory)

ITEMS = {
    'lens': ('The Lens', 'Use to see how well this place remembers itself. Look at a formation to study the word it spells.',
             lambda: T.lens(1)),
    'architect_codex': ('Architect Codex', 'Use to read your rank, your debt, and the words you know. Crouch and use on a formation you built to speak it.',
                        lambda: T.book(2, '#4b2a7a')),
    'memory_siphon': ('Memory Siphon', 'Draws a Memory out of a block, a creature, water, or the open sky high above the world.',
                      lambda: T.stick_tool(3, '#7fd8ff', head_shape='needle')),
    'memory': ('Memory', 'What something remembers about being itself. The Paradox Engine runs on these.',
               lambda: T.orb(4, '#b48cff', glyph=4)),
    'folded_memory': ('Folded Memory', 'A stack refined into a memory of itself. Weighs nothing. Unfolds only in the original world.',
                      lambda: T.orb(5, '#ffd27a', glyph=5)),
    'coherence_shard': ('Coherence Shard', 'Stable paradox. What is left when something is put back the way it was. The Mender\'s currency.',
                        lambda: T.shard(6, '#7fe3ff')),
    'entropy_shard': ('Entropy Shard', 'Unstable paradox. What comes off the world when it is pulled apart. The Unmaker\'s currency.',
                      lambda: T.shard(7, '#ff5a7a')),
    'corrupted_paradox': ('Corrupted Paradox', 'Paradox that has started to contradict itself. Powerful, and it does not like being held.',
                          lambda: T.shard(8, '#3a0a3a')),
    'paradox_fragment': ('Paradox Fragment', 'A piece of a contradiction. Found where the world makes mistakes. Contradict a Memory with it to distil a Concept.',
                         lambda: T.shard(9, '#c36bff')),
    'unremembered_thing': ('Unremembered Thing', 'The world made this without remembering what it was. Four of them make a Paradox Fragment.',
                           lambda: T.glitch_item(10)),
    'unwritten_fragment': ('Unwritten Fragment', 'Chipped from something that did not exist firmly enough to break. Only during a Reality Storm.',
                           lambda: T.shard(11, '#f0f0f0')),
    'memory_of_the_first_world': ('Memory of the First World', 'The first thing the world forgot. Every Paradox Engine is built around one.',
                                  lambda: T.orb(12, '#8ad0ff', glyph=12)),
    'patch_notes': ('Patch Notes', 'Read to apply a fix the world wrote for itself. Restores integrity around you.',
                    lambda: T.scroll(13, ink='#2f7aa8')),
    'stitcher': ('The Stitcher', 'The Mender\'s tool. Rewrites a glitched creature or block back into what it should be.',
                 lambda: T.stick_tool(14, '#7fe3ff', head_shape='needle')),
    'destabilizer': ('Destabilizer', 'The Unmaker\'s first tool. Drives a chunk\'s integrity down and harvests the Entropy.',
                     lambda: T.stick_tool(15, '#ff5a7a', head_shape='fork')),
    'rule_scroll': ('Rule Scroll', 'A rule reality has been convinced of, waiting to be applied.',
                    lambda: T.scroll(16)),
    'lullaby_shard': ('Lullaby Shard', 'Forms in the rock beneath places where you sleep often.',
                      lambda: T.shard(17, '#9fb7ff')),
    'lullaby': ('Lullaby', 'Sing the world back toward sleep. For a while it forgets less, and leaves you alone.',
                lambda: T.orb(18, '#9fb7ff', glyph=18)),
    'block_of_air': ('Block of Air', 'Bought from the Draft. Use on a block to make it air. It usually works.',
                     lambda: T.block_icon(19, '#e8f4ff')),
    'concept_of_tuesday': ('The Concept of Tuesday', 'Bought from the Draft. Use it and it is Tuesday.',
                           lambda: T.orb(20, '#ffe066', glyph=20)),
    'unwritten_death': ('Unwritten Death', 'Drink: for five minutes the world rejects the events that would kill you. Each rejection is a debt.',
                        lambda: T.bottle(21, '#1b0f2b')),
    'silicon': ('Silicon', 'Sand that remembers being thought about. Blast sand to make it.',
                lambda: T.block_icon(22, '#6d7a8a')),
    'silicon_wafer': ('Silicon Wafer', 'The medium a Compiler Chest writes programs onto.',
                      lambda: T.chip(23)),
}
for cid, (color, name) in CONCEPTS.items():
    ITEMS['concept_' + cid] = ('Concept: ' + name, '', (lambda c=color, s=sum(map(ord, cid)): T.orb(s, c, glyph=s)))

# --------------------------------------------------------------------------- blocks
# id: dict(name, desc, model, textures..., loot, tools, layer)
#   model: cube | pillar | unremembered
#   loot:  self | none | ('item', id, min, max) | ('silk', id, min, max)

BLOCKS = {
    'paradox_engine': dict(name='Paradox Engine', desc='Subject, Contradiction, Result. It does not craft. It edits reality.',
                           model='cube_bottom_top', top=lambda: T.rune_block(101, '#2a1640', '#c97bff'),
                           side=lambda: T.circuit(102, '#1d0f2e', '#d48bff'), bottom=lambda: T.noise_block(103, '#3a2a2a'),
                           loot='self', tools=['pickaxe'], level='iron'),
    'reality_anchor': dict(name='Reality Anchor', desc='Pins the integrity of nearby chunks at full. The world remembers this place.',
                           model='cube', all=lambda: T.rune_block(104, '#0f2a33', '#5fe8ff'), loot='self', tools=['pickaxe'], level='iron'),
    'quiet': dict(name='Quiet', desc='A harvested absence of sound. Nothing makes noise near it.',
                  model='cube', all=lambda: T.noise_block(105, '#0b0b10', '#1a1a24', 0.05, 0.2), loot='self', tools=[]),
    'possibility_eater': dict(name='Possibility Eater', desc='Hunger + Possibility. Eats outcomes before they happen: monsters do not spawn and explosions do not go off nearby.',
                              model='cube', all=lambda: T.glitch_block(106, '#4a1050', '#ff6af0'), loot='self', tools=['pickaxe'], level='iron'),
    'final_block': dict(name='Final Block', desc='Identity + Finality. It will always be what it is. Nothing can transform it.',
                        model='cube', all=lambda: T.rune_block(107, '#0a0a0a', '#e8e3c8'), loot='self', tools=['pickaxe'], level='diamond'),
    'unremembered_block': dict(name='Unremembered Thing', desc='The world made this without deciding what it is. It changes its mind.',
                               model='unremembered', loot=('item', 'forgotten:unremembered_thing', 1, 1), tools=['pickaxe']),
    'null_block': dict(name='Null', desc='Past the addresses the world can hold. Things that pass through may stop existing.',
                       model='cube', all=lambda: T.missing(108), loot='none', tools=[], layer='cutout'),
    'sky_fragment': dict(name='Sky Fragment', desc='A patch of sky, somewhere sky should not be.',
                         model='cube', all=lambda: T.sky(109), loot=('silk', 'forgotten:sky_fragment', 1, 1), tools=[]),
    'volatile_log': dict(name='Volatile Log', desc='The trunk of a tree a creeper became. It still remembers exploding.',
                         model='pillar', side=lambda: T.log_side(110, '#4d7a2b', '#9be36b'),
                         end=lambda: T.log_top(111, '#4d7a2b', '#b6ff8a'), loot='self', tools=['axe']),
    'lullaby_ore': dict(name='Lullaby Ore', desc='Forms where someone sleeps often.',
                        model='cube', all=lambda: T.ore_block(112, '#7d7d7d', '#9fb7ff'),
                        loot=('ore', 'forgotten:lullaby_shard', 1, 2), tools=['pickaxe'], level='stone'),
    'deepslate_lullaby_ore': dict(name='Deepslate Lullaby Ore', desc='Forms where someone sleeps often.',
                                  model='cube', all=lambda: T.ore_block(113, '#4a4a50', '#9fb7ff'),
                                  loot=('ore', 'forgotten:lullaby_shard', 1, 2), tools=['pickaxe'], level='stone'),
    'marrowglass': dict(name='Marrowglass', desc='From the Underdream. It knows what shape it should be and grows toward it.',
                        model='cube', all=lambda: T.glass(114, '#f4eedc', alpha=150, border='#d8ccaa'), loot='self', tools=[], layer='translucent'),
    'scarred_netherrack': dict(name='Scarred Netherrack', desc='The Nether remembers pain. This ground keeps replaying what happened here.',
                               model='cube', all=lambda: T.glitch_block(115, '#5e1a1a', '#ffb070'),
                               loot=('self_plus', 'forgotten:concept_memory', 0.15), tools=['pickaxe'], level='diamond'),
    'unwritten_stone': dict(name='Unwritten Stone', desc='It does not exist firmly enough to break - except during a Reality Storm.',
                            model='cube', all=lambda: T.static(116, '#c8c8d0', '#ffffff'),
                            loot=('item', 'forgotten:unwritten_fragment', 1, 1), tools=['pickaxe'], level='diamond'),
    'singularity': dict(name='Singularity', desc='Gravity + Recursion. Gravity applied to itself until it pulls on everything nearby.',
                        model='cube', all=lambda: T.rune_block(117, '#000000', '#5b6cff'), loot='self', tools=['pickaxe'], level='diamond'),
    'data_block': dict(name='Data Block', desc='A block of the Stack: memory that glows where it is being read.',
                       model='cube', all=lambda: T.circuit(118), loot='self', tools=['pickaxe']),
    'memory_block': dict(name='Memory Block', desc='A block of the Stack: memory nobody is reading.',
                         model='cube', all=lambda: T.circuit(119, '#050508', '#1f3a2a'), loot='self', tools=['pickaxe']),
}

# --------------------------------------------------------------------------- recipes

SHAPED = {
    'lens': ([' A ', 'ASA', ' A '], {'A': 'minecraft:amethyst_shard', 'S': 'minecraft:spyglass'}, 1),
    'memory_siphon': (['G', 'C', 'S'], {'G': 'minecraft:glass_bottle', 'C': 'minecraft:copper_ingot', 'S': 'minecraft:stick'}, 1),
    'paradox_engine': (['AMA', 'CTR', 'OOO'], {'A': 'minecraft:amethyst_block', 'M': 'forgotten:memory_of_the_first_world',
                                                'C': 'minecraft:clock', 'T': 'minecraft:crafting_table', 'R': 'minecraft:rotten_flesh',
                                                'O': 'minecraft:obsidian'}, 1),
    'reality_anchor': (['CCC', 'CLC', 'OOO'], {'C': 'forgotten:coherence_shard', 'L': 'minecraft:lodestone', 'O': 'minecraft:obsidian'}, 1),
    'stitcher': (['C', 'I', 'S'], {'C': 'forgotten:coherence_shard', 'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'}, 1),
    'destabilizer': (['P', 'I', 'S'], {'P': 'forgotten:paradox_fragment', 'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'}, 1),
    'silicon_wafer': (['SSS', 'RAR'], {'S': 'forgotten:silicon', 'R': 'minecraft:redstone', 'A': 'minecraft:amethyst_shard'}, 3),
}
SHAPELESS = {
    'architect_codex': (['minecraft:book', 'minecraft:amethyst_shard', 'minecraft:compass'], 1),
    'patch_notes': (['minecraft:paper', 'forgotten:coherence_shard'], 2),
    'coherence_shard': (['forgotten:paradox_fragment', 'minecraft:amethyst_shard', 'minecraft:quartz'], 2),
    'entropy_shard': (['forgotten:paradox_fragment', 'minecraft:gunpowder', 'minecraft:redstone'], 2),
    'paradox_fragment': (['forgotten:unremembered_thing'] * 4, 1),
    'lullaby': (['forgotten:lullaby_shard', 'forgotten:lullaby_shard', 'minecraft:note_block'], 1),
}
BLASTING = {
    'silicon': ('minecraft:sand', 0.2, 100),
}


def mem(source):
    return {'item': 'forgotten:memory', 'memory': source}


def c(name):
    return 'forgotten:concept_' + name


PARADOX = {
    'singularity': dict(subject=c('gravity'), contradiction=c('recursion'), result={'id': 'forgotten:singularity'}, debt=20, rank=3, ordered=False),
    'final_block': dict(subject=c('identity'), contradiction=c('finality'), result={'id': 'forgotten:final_block', 'count': 4}, debt=8, rank=2, ordered=False),
    'quiet': dict(subject=c('silence'), contradiction=c('absence'), result={'id': 'forgotten:quiet', 'count': 4}, debt=3, rank=1, ordered=False),
    'possibility_eater': dict(subject=c('hunger'), contradiction=c('possibility'), result={'id': 'forgotten:possibility_eater'}, debt=15, rank=3, ordered=False),
    'stone_remembers_falling_upward': dict(subject=mem('minecraft:stone'), contradiction=c('gravity'),
                                           result={'id': 'forgotten:rule_scroll', 'data': 'lift'}, debt=20, rank=3),
    'water_remembers_the_sky': dict(subject=mem('minecraft:water'), contradiction=mem('forgotten:sky'),
                                    result={'id': 'forgotten:rule_scroll', 'data': 'sky_water'}, debt=25, rank=3),
    'death_forgets_you': dict(subject=mem('minecraft:respawn_anchor'), contradiction=mem('forgotten:king_of_yesterday'),
                              result={'id': 'forgotten:unwritten_death'}, debt=40, rank=4),
    'anchor_from_finality': dict(subject='forgotten:coherence_shard', contradiction=c('finality'), result={'id': 'forgotten:reality_anchor'}, debt=0, rank=1),
    'sky_fragment': dict(subject=mem('forgotten:sky'), contradiction='minecraft:glass', result={'id': 'forgotten:sky_fragment', 'count': 4}, debt=2, rank=1),
    'marrowglass': dict(subject=mem('minecraft:glass'), contradiction=c('identity'), result={'id': 'forgotten:marrowglass', 'count': 8}, debt=5, rank=2),
    'volatile_log': dict(subject=mem('minecraft:creeper'), contradiction='minecraft:oak_log', result={'id': 'forgotten:volatile_log', 'count': 4}, debt=6, rank=2),
    'lullaby': dict(subject='forgotten:lullaby_shard', contradiction=c('silence'), result={'id': 'forgotten:lullaby', 'count': 2}, debt=1, rank=1),
    'unremembered_block': dict(subject='forgotten:unremembered_thing', contradiction=c('identity'), result={'id': 'forgotten:unremembered_block', 'count': 4}, debt=4, rank=2),
    'data_block': dict(subject=mem('minecraft:redstone_block'), contradiction=c('recursion'), result={'id': 'forgotten:data_block', 'count': 8}, debt=3, rank=2),
    'corrupted_paradox': dict(subject='forgotten:paradox_fragment', contradiction=c('absence'), catalyst='forgotten:entropy_shard',
                              result={'id': 'forgotten:corrupted_paradox'}, debt=10, rank=3),
    'unwritten_fragment': dict(subject='forgotten:unwritten_stone', contradiction=c('possibility'), result={'id': 'forgotten:unwritten_fragment'}, debt=15, rank=4),
    'memory_block': dict(subject=mem('minecraft:obsidian'), contradiction=c('memory'), result={'id': 'forgotten:memory_block', 'count': 8}, debt=2, rank=2),
    'patch_from_memory': dict(subject=mem('minecraft:book'), contradiction='forgotten:coherence_shard', result={'id': 'forgotten:patch_notes', 'count': 4}, debt=0, rank=1),
    'entropy_from_absence': dict(subject=c('absence'), contradiction='minecraft:gunpowder', result={'id': 'forgotten:entropy_shard', 'count': 4}, debt=4, rank=1),
    'coherence_from_finality': dict(subject=c('finality'), contradiction='minecraft:quartz', result={'id': 'forgotten:coherence_shard', 'count': 4}, debt=0, rank=1),
}

# --------------------------------------------------------------------------- advancements
# id: (title, description, icon, parent)

ADVANCEMENTS = {
    'something_is_wrong': ('Something Is Wrong', 'Witness the world make a mistake.', 'forgotten:unremembered_thing', None),
    'lens': ('Through the Lens', 'Look at the seams of the world.', 'forgotten:lens', 'something_is_wrong'),
    'first_world_fragment': ('Memory Fragment Discovered', 'Approach the first impossible thing.', 'forgotten:memory_of_the_first_world', 'something_is_wrong'),
    'paradox_engine': ('A Reason To Believe You', 'Contradict reality with the Paradox Engine.', 'forgotten:paradox_engine', 'first_world_fragment'),
    'first_word': ('The World Speaks', 'Decode a word of the world\'s language.', 'forgotten:architect_codex', 'lens'),
    'rank_2': ('Patchmaker', 'Reach Architect rank 2.', 'forgotten:patch_notes', 'lens'),
    'rank_3': ('Rewriter', 'Reach Architect rank 3.', 'forgotten:rule_scroll', 'rank_2'),
    'rank_4': ('Architect', 'Reach Architect rank 4.', 'forgotten:reality_anchor', 'rank_3'),
    'rank_5': ('Paradox Lord', 'Reach Architect rank 5.', 'forgotten:singularity', 'rank_4'),
}

LANG_EXTRA = {
    'itemGroup.forgotten': 'The World That Forgot Itself',
    'container.forgotten.paradox_engine': 'Paradox Engine',
    'item.forgotten.memory.of': 'Memory of %s',
    'item.forgotten.folded_memory.of': 'Folded Memory of %s %s',
    'memory.forgotten.sky': 'the Sky',
    'memory.forgotten.king_of_yesterday': 'the King of Yesterday',
    'item.forgotten.rule_scroll.lift': 'Rule: Stone Remembers Falling Upward',
    'item.forgotten.rule_scroll.lift.desc': 'Use on a structure you built to lift it into the sky. On natural stone, lifts a small outcrop.',
    'item.forgotten.rule_scroll.sky_water': 'Rule: Water Remembers Being a Cloud',
    'item.forgotten.rule_scroll.sky_water.desc': 'Use on a lake. It rises into a floating sphere, fish and all.',
}


# =========================================================================== writers

def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def save_png(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def item_model(item_id, texture=None, parent='minecraft:item/generated', model=None):
    if model is None:
        write_json(f'{ASSETS}/models/item/{item_id}.json',
                   {'parent': parent, 'textures': {'layer0': texture or f'{NS}:item/{item_id}'}})
        model = f'{NS}:item/{item_id}'
    write_json(f'{ASSETS}/items/{item_id}.json', {'model': {'type': 'minecraft:model', 'model': model}})


def block_loot(block_id, loot):
    entry = None
    pool_conditions = [{'condition': 'minecraft:survives_explosion'}]
    if loot == 'none':
        return
    if loot == 'self':
        entry = {'type': 'minecraft:item', 'name': f'{NS}:{block_id}'}
        pools = [{'rolls': 1, 'bonus_rolls': 0, 'entries': [entry], 'conditions': pool_conditions}]
    elif loot[0] == 'item':
        _, item, lo, hi = loot
        entry = {'type': 'minecraft:item', 'name': item,
                 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
        pools = [{'rolls': 1, 'bonus_rolls': 0, 'entries': [entry], 'conditions': pool_conditions}]
    elif loot[0] == 'silk':
        entry = {'type': 'minecraft:item', 'name': loot[1], 'conditions': [silk()]}
        pools = [{'rolls': 1, 'bonus_rolls': 0, 'entries': [entry]}]
    elif loot[0] == 'ore':
        _, item, lo, hi = loot
        pools = [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': f'{NS}:{block_id}', 'conditions': [silk()]},
            {'type': 'minecraft:item', 'name': item, 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}},
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                {'function': 'minecraft:explosion_decay'}]}]}]}]
    elif loot[0] == 'self_plus':
        _, item, chance = loot
        pools = [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{block_id}'}], 'conditions': pool_conditions},
                 {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': item}],
                  'conditions': [{'condition': 'minecraft:random_chance', 'chance': chance}]}]
    else:
        raise ValueError(loot)
    write_json(f'{DATA}/loot_table/blocks/{block_id}.json',
               {'type': 'minecraft:block', 'pools': pools, 'random_sequence': f'{NS}:blocks/{block_id}'})


def silk():
    return {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [
        {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}


def main():
    for sub in ('textures', 'models', 'blockstates', 'items'):
        shutil.rmtree(os.path.join(ASSETS, sub), ignore_errors=True)
    for sub in ('recipe', 'loot_table', 'forgotten_paradox', 'advancement'):
        shutil.rmtree(os.path.join(DATA, sub), ignore_errors=True)

    lang = dict(LANG_EXTRA)

    for item_id, (name, desc, tex) in ITEMS.items():
        save_png(tex(), f'{ASSETS}/textures/item/{item_id}.png')
        item_model(item_id)
        lang[f'item.{NS}.{item_id}'] = name
        if desc:
            lang[f'item.{NS}.{item_id}.desc'] = desc

    tool_tags = {}
    level_tags = {}
    layers = {}
    for block_id, spec in BLOCKS.items():
        lang[f'block.{NS}.{block_id}'] = spec['name']
        lang[f'block.{NS}.{block_id}.desc'] = spec['desc']
        model = spec['model']
        if model == 'cube':
            save_png(spec['all'](), f'{ASSETS}/textures/block/{block_id}.png')
            write_json(f'{ASSETS}/models/block/{block_id}.json',
                       {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{block_id}'}})
            write_json(f'{ASSETS}/blockstates/{block_id}.json', {'variants': {'': {'model': f'{NS}:block/{block_id}'}}})
        elif model == 'cube_bottom_top':
            for part in ('top', 'side', 'bottom'):
                save_png(spec[part](), f'{ASSETS}/textures/block/{block_id}_{part}.png')
            write_json(f'{ASSETS}/models/block/{block_id}.json',
                       {'parent': 'minecraft:block/cube_bottom_top',
                        'textures': {p: f'{NS}:block/{block_id}_{p}' for p in ('top', 'side', 'bottom')}})
            write_json(f'{ASSETS}/blockstates/{block_id}.json', {'variants': {'': {'model': f'{NS}:block/{block_id}'}}})
        elif model == 'pillar':
            save_png(spec['side'](), f'{ASSETS}/textures/block/{block_id}.png')
            save_png(spec['end'](), f'{ASSETS}/textures/block/{block_id}_top.png')
            tex = {'side': f'{NS}:block/{block_id}', 'end': f'{NS}:block/{block_id}_top'}
            write_json(f'{ASSETS}/models/block/{block_id}.json', {'parent': 'minecraft:block/cube_column', 'textures': tex})
            write_json(f'{ASSETS}/models/block/{block_id}_horizontal.json',
                       {'parent': 'minecraft:block/cube_column_horizontal', 'textures': tex})
            write_json(f'{ASSETS}/blockstates/{block_id}.json', {'variants': {
                'axis=x': {'model': f'{NS}:block/{block_id}_horizontal', 'x': 90, 'y': 90},
                'axis=y': {'model': f'{NS}:block/{block_id}'},
                'axis=z': {'model': f'{NS}:block/{block_id}_horizontal', 'x': 90}}})
        elif model == 'unremembered':
            variants = {}
            palettes = [('#4b2a5a', '#ff61d8'), ('#2a5a4b', '#61ffd8'), ('#5a4b2a', '#ffd861'), ('#2a2a5a', '#6161ff'),
                        ('#5a2a2a', '#ff6161'), ('#3a3a3a', '#f0f0f0'), ('#203a20', '#80ff80'), ('#101010', '#f800f8')]
            for i, (base, glitch) in enumerate(palettes):
                save_png(T.glitch_block(200 + i, base, glitch), f'{ASSETS}/textures/block/{block_id}_{i}.png')
                write_json(f'{ASSETS}/models/block/{block_id}_{i}.json',
                           {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{block_id}_{i}'}})
                variants[f'identity={i}'] = {'model': f'{NS}:block/{block_id}_{i}'}
            write_json(f'{ASSETS}/blockstates/{block_id}.json', {'variants': variants})
            write_json(f'{ASSETS}/models/block/{block_id}.json', {'parent': f'{NS}:block/{block_id}_0'})
        else:
            raise ValueError(model)
        item_model(block_id, model=f'{NS}:block/{block_id}')
        block_loot(block_id, spec['loot'])
        for tool in spec.get('tools', []):
            tool_tags.setdefault(tool, []).append(f'{NS}:{block_id}')
        if spec.get('level'):
            level_tags.setdefault(spec['level'], []).append(f'{NS}:{block_id}')
        if spec.get('layer'):
            layers[block_id] = spec['layer']

    for tool, values in tool_tags.items():
        write_json(f'{MC_DATA}/tags/block/mineable/{tool}.json', {'replace': False, 'values': values})
    for level, values in level_tags.items():
        write_json(f'{MC_DATA}/tags/block/needs_{level}_tool.json', {'replace': False, 'values': values})

    for rid, (pattern, key, count) in SHAPED.items():
        write_json(f'{DATA}/recipe/{rid}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc',
                                                  'pattern': pattern, 'key': key, 'result': {'id': f'{NS}:{rid}', 'count': count}})
    for rid, (ingredients, count) in SHAPELESS.items():
        write_json(f'{DATA}/recipe/{rid}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                                  'ingredients': ingredients, 'result': {'id': f'{NS}:{rid}', 'count': count}})
    for rid, (ingredient, xp, time) in BLASTING.items():
        write_json(f'{DATA}/recipe/{rid}_from_blasting.json', {'type': 'minecraft:blasting', 'category': 'misc',
                                                                'ingredient': ingredient, 'experience': xp, 'cookingtime': time,
                                                                'result': {'id': f'{NS}:{rid}'}})

    for rid, spec in PARADOX.items():
        write_json(f'{DATA}/forgotten_paradox/{rid}.json', spec)

    for aid, (title, desc, icon, parent) in ADVANCEMENTS.items():
        adv = {'criteria': {'done': {'trigger': 'minecraft:impossible'}},
               'display': {'title': {'translate': f'advancements.{NS}.{aid}.title'},
                           'description': {'translate': f'advancements.{NS}.{aid}.description'},
                           'icon': {'id': icon}, 'frame': 'goal' if aid.startswith('rank_5') else 'task'},
               'requirements': [['done']]}
        if parent:
            adv['parent'] = f'{NS}:{parent}'
        else:
            adv['display']['background'] = 'minecraft:gui/advancements/backgrounds/stone'
            adv['display']['announce_to_chat'] = False
        write_json(f'{DATA}/advancement/{aid}.json', adv)
        lang[f'advancements.{NS}.{aid}.title'] = title
        lang[f'advancements.{NS}.{aid}.description'] = desc

    write_json(f'{ASSETS}/lang/en_us.json', dict(sorted(lang.items())))
    write_json(os.path.join(os.path.dirname(__file__), 'render_layers.json'), layers)
    print(f'{len(ITEMS)} items, {len(BLOCKS)} blocks, {len(PARADOX)} paradox recipes, '
          f'{len(SHAPED) + len(SHAPELESS) + len(BLASTING)} crafting recipes, {len(ADVANCEMENTS)} advancements')


if __name__ == '__main__':
    main()
