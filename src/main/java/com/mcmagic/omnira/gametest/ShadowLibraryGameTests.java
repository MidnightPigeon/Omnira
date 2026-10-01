package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_shadow_library")
@PrefixGameTestTemplate(false)
public final class ShadowLibraryGameTests {
    @GameTest(template="spell_arena") public static void lanternAttachmentVariants(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,3,4));
        var chain=com.mcmagic.omnira.block.ShadowLanternBlock.CHAIN;
        var hanging=DreamContent.SHADOW_LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING,true);
        level.setBlockAndUpdate(p.above(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(p,hanging);
        h.assertTrue(!level.getBlockState(p).getValue(chain),"Ceiling should retain top plate");
        level.setBlockAndUpdate(p.above(),Blocks.CHAIN.defaultBlockState());
        h.assertTrue(level.getBlockState(p).getValue(chain),"Chain support should extend link");
        level.setBlockAndUpdate(p.above(),Blocks.OAK_FENCE.defaultBlockState());
        h.assertTrue(level.getBlockState(p).getValue(chain),"Narrow support should use link");
        level.setBlockAndUpdate(p.above(),DreamContent.SPIRITUAL_CRYSTAL_BLOCK.get().defaultBlockState());
        h.assertTrue(!level.getBlockState(p).getValue(chain),"Full crystal ceiling should restore plate");
        level.setBlockAndUpdate(p,hanging.setValue(LanternBlock.WATERLOGGED,true));
        level.setBlockAndUpdate(p.above(),Blocks.CHAIN.defaultBlockState());
        h.assertTrue(level.getBlockState(p).getValue(chain) && level.getBlockState(p).getValue(LanternBlock.WATERLOGGED),"Attachment change lost waterlogging");
        level.setBlockAndUpdate(p.above(),Blocks.AIR.defaultBlockState());
        h.assertTrue(!level.getBlockState(p).is(DreamContent.SHADOW_LANTERN.get()),"Unsupported lamp did not detach");
        level.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(p.above(),Blocks.CHAIN.defaultBlockState());
        level.setBlockAndUpdate(p,DreamContent.SHADOW_LANTERN.get().defaultBlockState());
        h.assertTrue(!level.getBlockState(p).getValue(chain),"Standing lamp acquired ceiling chain");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bookshelfPowerAndObstruction(GameTestHelper h) {
        var table=h.absolutePos(new BlockPos(4,2,4));var offset=new BlockPos(2,0,0);var shelf=table.offset(offset);
        h.getLevel().setBlockAndUpdate(table,Blocks.ENCHANTING_TABLE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(table.east(),Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(shelf,DreamContent.SHADOW_BOOKSHELF.get().defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(shelf).getEnchantPowerBonus(h.getLevel(),shelf)==2,"Bookshelf not twice vanilla power");
        h.assertTrue(EnchantingTableBlock.isValidBookShelf(h.getLevel(),table,offset),"Bookshelf ignored by enchanting table");
        h.getLevel().setBlockAndUpdate(table.east(),Blocks.STONE.defaultBlockState());
        h.assertTrue(!EnchantingTableBlock.isValidBookShelf(h.getLevel(),table,offset),"Bookshelf bypassed obstruction");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void tieredLootAndDamagedTools(GameTestHelper h) {
        var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO))).create(LootContextParamSets.CHEST);
        for(String room:new String[]{"stacks","research","sealed"}) {
            var table=h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","chests/shadow_library_"+room)));
            int books=0,treasures=0,cores=0,tools=0,light=0,dark=0,both=0,manuscripts=0;
            for(int seed=1;seed<=2000;seed++) {
                var loot=table.getRandomItems(params,net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed));int thisBooks=0;boolean manuscript=false,hasLight=false,hasDark=false;
                for(var stack:loot) {
                    if(stack.is(Items.ENCHANTED_BOOK)) {
                        books++;thisBooks++;
                        var ench=stack.get(DataComponents.STORED_ENCHANTMENTS);
                        h.assertTrue(ench!=null && !ench.isEmpty(),"Empty enchanted book");
                        if(ench.keySet().stream().anyMatch(e->e.is(EnchantmentTags.TREASURE)))treasures++;
                    }
                    if(stack.is(ModItems.ANALYSIS_CRYSTAL.get())) {
                        tools++;int remaining=stack.getMaxDamage()-stack.getDamageValue();
                        h.assertTrue(remaining>=25 && remaining<=64,"Analysis crystal not damaged to 20-50 percent");
                    }
                    if(stack.is(ModItems.DREAM_SPELL_CORE.get()))cores++;
                    if(stack.is(ModItems.LIGHT_MICROCORE.get())) {light++;hasLight=true;}
                    if(stack.is(ModItems.DARK_MICROCORE.get())) {dark++;hasDark=true;}
                    if(stack.is(ModItems.SHADOW_MANUSCRIPT.get()))manuscript=true;
                }
                if(hasLight && hasDark)both++;
                if(manuscript)manuscripts++;
                if(room.equals("sealed"))h.assertTrue(thisBooks>=2 && thisBooks<=3 && manuscript,"Sealed room missing guaranteed books/manuscripts");
            }
            h.assertTrue(books>0 && treasures>0,"Books or treasure enchantments missing: "+room);
            if(room.equals("stacks"))h.assertTrue(manuscripts>300 && manuscripts<500,"Stacks manuscript chance differs from 20 percent");
            double fraction=(double)treasures/books;
            if(room.equals("sealed"))h.assertTrue(fraction>.5 && fraction<.7 && cores>10 && cores<80,"Sealed loot rarity incorrect");
            else h.assertTrue(fraction<.4 && cores==0,"Lower-tier room leaked sealed rewards");
            if(room.equals("research")) {
                h.assertTrue(tools>50 && tools<160,"Research crystal chance incorrect");
                h.assertTrue(light>50 && light<160 && dark>50 && dark<160 && both>0,"Microcores must roll independently at 5 percent each");
            }
            else h.assertTrue(tools==0,"Analysis crystal outside research room");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void lanternAndSurfaceEntrance(GameTestHelper h) {
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(4,2,4));
        var lantern=DreamContent.SHADOW_LANTERN.get().defaultBlockState();
        h.assertTrue(lantern.getLightEmission(level,p)==12 && !lantern.hasBlockEntity(),"Lantern light/performance contract changed");
        level.setBlockAndUpdate(p.below(),DreamContent.SHADOW_ROCK.get().defaultBlockState());
        h.assertTrue(lantern.canSurvive(level,p),"Standing lantern lacks support");
        level.setBlockAndUpdate(p.above(),DreamContent.SHADOW_ROCK.get().defaultBlockState());
        h.assertTrue(lantern.setValue(LanternBlock.HANGING,true).canSurvive(level,p),"Hanging lantern lacks support");
        for(int depth:new int[]{0,1,12,48}) {
            var roof=new BlockPos(1,20,3);
            var plan=com.mcmagic.omnira.world.structure.ShadowLibraryEntrance.plan(roof,20+depth);
            h.assertTrue(plan.size()==5*(depth+1),"Shaft must retain a cross-shaped footprint");
            for(int y=20;y<20+depth;y++) {
                var center=new BlockPos(1,y,3);
                h.assertTrue(plan.get(center).is(Blocks.LADDER) && plan.get(center.west()).is(DreamContent.SHADOW_ROCK_BRICKS.get()),"Broken ladder/support");
            }
            h.assertTrue(plan.get(roof.above(depth)).is(ShadowWoodContent.TRAPDOOR.get()),"Surface hatch missing");
            h.assertTrue(plan.keySet().stream().noneMatch(pos->pos.getY()>20+depth),"Entrance rises above ground");
        }
        var recipe=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira","shadow_lantern"));
        h.assertTrue(recipe.isPresent(),"Lantern recipe missing");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void templatesAndSpawnerRules(GameTestHelper h) {
        for(String name:new String[]{"stacks","research","sealed","corridor","connected_sample"}) {
            var template=h.getLevel().getStructureManager().get(ResourceLocation.fromNamespaceAndPath("omnira","authored/shadow_library/"+name));
            h.assertTrue(template.isPresent(),"Missing room template: "+name);
            h.assertTrue(template.get().getSize().getY()==5,"Room must have three blocks of interior height");
            if(name.equals("sealed")) {
                var tag=template.get().save(new net.minecraft.nbt.CompoundTag());int spawners=0;
                for(var block:tag.getList("blocks",10)) {
                    var nbt=((net.minecraft.nbt.CompoundTag)block).getCompound("nbt");
                    if(!nbt.getString("id").equals("minecraft:mob_spawner"))continue;
                    spawners++;
                    var data=net.minecraft.world.level.SpawnData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,nbt.getCompound("SpawnData")).getOrThrow();
                    h.assertTrue(data.getEntityToSpawn().getString("id").equals("omnira:shadow_ghost") && data.getCustomSpawnRules().isPresent(),"Spawner uses natural-only spawn restrictions");
                }
                h.assertTrue(spawners==1,"Sealed room must have exactly one ghost spawner");
            }
        }
        h.succeed();
    }
}
