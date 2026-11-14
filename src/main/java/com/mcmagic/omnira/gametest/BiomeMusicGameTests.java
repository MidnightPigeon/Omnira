package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("omnira_biome_music") @PrefixGameTestTemplate(false)
public final class BiomeMusicGameTests {
    @GameTest(template="spell_arena") public static void spaceDiscUsesNativePlaybackAndSave(GameTestHelper h){
        var stack=new ItemStack(ModItems.MUSIC_DISC_SPACE.get());
        checkDisc(h,stack.getItem(),"po_sui_shi_jie",1500,3);
        h.assertTrue(com.mcmagic.omnira.registry.ModCreativeModeTabs.contents(com.mcmagic.omnira.registry.ModCreativeModeTabs.Category.MISC).stream().anyMatch(s->s.is(stack.getItem())),"Space disc missing from creative catalog");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bothMachinesConvertEveryTaggedDisc(GameTestHelper h){
        var level=h.getLevel();var discs=new java.util.ArrayList<Item>();
        for(var holder:net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(net.neoforged.neoforge.common.Tags.Items.MUSIC_DISCS))discs.add(holder.value());
        h.assertTrue(discs.contains(Items.MUSIC_DISC_CAT)&&discs.contains(Items.MUSIC_DISC_PIGSTEP)&&discs.contains(ModItems.MUSIC_DISC_SPACE.get())&&discs.contains(ModItems.MUSIC_DISC_MI_MENG_YE.get()),"Common disc tag misses vanilla or Omnira inputs");
        for(boolean infusion:new boolean[]{false,true}){
            var relative=new BlockPos(infusion?8:3,3,3);var block=(infusion?com.mcmagic.omnira.reversal.ReversalContent.INFUSER:com.mcmagic.omnira.reversal.ReversalContent.MACHINE).get();h.setBlock(relative,block);
            var machine=(com.mcmagic.omnira.reversal.ReversalBlockEntity)h.getBlockEntity(relative);var expected=infusion?ModItems.MUSIC_DISC_SPACE.get():ModItems.MUSIC_DISC_LIU_NIAN.get();
            for(var disc:discs){
                var input=new ItemStack(disc);h.assertTrue(machine.canPlaceItem(0,input),"Machine rejected tagged disc: "+disc);
                machine.setItem(0,input);machine.setItem(1,ItemStack.EMPTY);machine.setItem(2,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:spatial_crystal_shard"))));
                for(int tick=0;tick<machine.duration()-1;tick++)machine.tick();
                h.assertTrue(machine.getItem(1).isEmpty()&&!machine.getItem(0).isEmpty(),"Disc converted early");int energy=machine.energy();machine.tick();
                h.assertTrue(machine.getItem(0).isEmpty()&&machine.getItem(1).is(expected)&&machine.getItem(1).getCount()==1&&machine.energy()==energy-1,"Disc output, count or energy changed: "+disc);
            }
            machine.setItem(0,new ItemStack(Items.MUSIC_DISC_CAT));machine.setItem(1,new ItemStack(Items.STONE));machine.setItem(2,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:spatial_crystal_shard"))));machine.tick();int energy=machine.energy();
            h.assertTrue(energy>0,"Blocked-output test lacks energy");
            for(int tick=0;tick<machine.duration()+1;tick++)machine.tick();
            h.assertTrue(machine.energy()==energy&&machine.getItem(0).is(Items.MUSIC_DISC_CAT)&&machine.getItem(1).is(Items.STONE),"Blocked output consumed a disc or energy");
            h.assertTrue(!machine.canPlaceItem(0,new ItemStack(Items.STONE)),"Disc recipe accepts unrelated items");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void mirrorDiscAndBuddingRockConvertShapelessly(GameTestHelper h){
        var rock=com.mcmagic.omnira.registry.DreamContent.DREAM_CRYSTAL_BEDROCK.get().asItem();var disc=ModItems.MUSIC_DISC_MI_MENG_YE.get();
        checkConversion(h,"music_disc_mi_meng_ye_from_bedrock",rock,disc);
        checkConversion(h,"dream_crystal_bedrock_from_disc",disc,rock);h.succeed();
    }
    private static void checkConversion(GameTestHelper h,String id,Item source,Item result){
        var level=h.getLevel();var holder=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("omnira",id)).orElseThrow();
        h.assertTrue(holder.value() instanceof net.minecraft.world.item.crafting.ShapelessRecipe,"Disc conversion is not vanilla shapeless crafting");
        var recipe=(net.minecraft.world.item.crafting.CraftingRecipe)holder.value();
        for(int size=1;size<=3;size++)for(int slot=0;slot<size*size;slot++){
            var stacks=new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(size*size,ItemStack.EMPTY));stacks.set(slot,new ItemStack(source));
            var input=net.minecraft.world.item.crafting.CraftingInput.of(size,size,stacks);
            var selected=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,level).orElseThrow();
            h.assertTrue(selected.id().equals(holder.id())&&recipe.matches(input,level),"Disc conversion failed at grid position "+size+"/"+slot);
            var output=recipe.assemble(input,level.registryAccess());
            h.assertTrue(output.is(result)&&output.getCount()==1&&recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),"Conversion changed 1:1 amount or returned an ingredient");
            if(result==ModItems.MUSIC_DISC_MI_MENG_YE.get())h.assertTrue(output.has(DataComponents.JUKEBOX_PLAYABLE),"Crafted disc lost native playback");
            if(size>1){stacks.set((slot+1)%(size*size),new ItemStack(Items.STONE));h.assertTrue(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(size,size,stacks),level),"Disc conversion ignored extra input");}
        }
    }
    @GameTest(template="spell_arena") public static void timeMusicIsBoundOnlyToUpperBiomes(GameTestHelper h){
        var biomes=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var selected=List.of("temporal_continent","stilled_wastes","fleeting_woods","reversion_mire","epochal_cliffs","recurrence_garden","eventide_ruins");
        var spatial=List.of("folded_caverns","dislocated_geodes","inverted_abyss","void_starsea","phase_crystal_grove","topological_labyrinth");
        for(var biome:biomes){
            var id=biomes.getKey(biome);if(!id.getNamespace().equals("omnira"))continue;
            var music=biome.getBackgroundMusic();
            boolean time=music.isPresent()&&music.get().getEvent().value().getLocation().toString().equals("omnira:music.time");
            h.assertTrue(time==selected.contains(id.getPath()),"Time music assigned to wrong biome: "+id);
            boolean mirror=music.isPresent()&&music.get().getEvent().value().getLocation().toString().equals("omnira:music.mirror_dream");
            h.assertTrue(mirror==id.getPath().equals("mirror_dream_border"),"Mirror dream music assigned to wrong biome: "+id);
            boolean space=music.isPresent()&&music.get().getEvent().value().getLocation().toString().equals("omnira:music.space");
            h.assertTrue(space==spatial.contains(id.getPath()),"Space music assigned to wrong biome: "+id);
            boolean light=music.isPresent()&&music.get().getEvent().value().getLocation().toString().equals("omnira:music.light");
            h.assertTrue(light==id.getPath().equals("dawn_crystal_fields"),"Light music assigned to wrong biome: "+id);
            boolean shadow=music.isPresent()&&music.get().getEvent().value().getLocation().toString().equals("omnira:music.shadow");
            h.assertTrue(shadow==id.getPath().equals("shadow_woodland"),"Shadow music assigned to wrong biome: "+id);
            if(time||mirror||space||light||shadow){
                int minimum=time||space?600:12000,maximum=time||space?1200:24000;
                h.assertTrue(music.get().getMinDelay()==minimum&&music.get().getMaxDelay()==maximum&&music.get().replaceCurrentMusic(),"Wrong background interval for "+id);
            }
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void discUsesNativeJukeboxPlaybackAndSave(GameTestHelper h){
        checkDisc(h,ModItems.MUSIC_DISC_LIU_NIAN.get(),"liu_nian",1340,1);h.succeed();
    }
    @GameTest(template="spell_arena") public static void mirrorDiscUsesNativeJukeboxPlaybackAndSave(GameTestHelper h){
        checkDisc(h,ModItems.MUSIC_DISC_MI_MENG_YE.get(),"mi_meng_ye",924,2);h.succeed();
    }
    private static void checkDisc(GameTestHelper h,Item item,String id,int duration,int comparator){
        checkDiscSound(h,item,"omnira:music_disc."+id,duration,comparator);
    }
    @GameTest(template="spell_arena") public static void weaverDiscUsesNativePlayback(GameTestHelper h){
        checkDisc(h,ModItems.MUSIC_DISC_WEAVER_SPIRIT.get(),"weaver_spirit",1552,4);h.succeed();
    }
    private static void checkDiscSound(GameTestHelper h,Item item,String sound,int duration,int comparator){
        var level=h.getLevel();var stack=new ItemStack(item);
        h.assertTrue(stack.getMaxStackSize()==1&&stack.has(DataComponents.JUKEBOX_PLAYABLE)&&stack.is(net.neoforged.neoforge.common.Tags.Items.MUSIC_DISCS),"Disc lacks native playback or compatibility tag");
        var song=JukeboxSong.fromStack(level.registryAccess(),stack).orElseThrow().value();
        h.assertTrue(song.soundEvent().value().getLocation().toString().equals(sound)&&song.lengthInTicks()==duration&&song.comparatorOutput()==comparator,"Disc registry or authored duration incorrect");
        var pos=h.absolutePos(new BlockPos(5,3,5));level.setBlockAndUpdate(pos,Blocks.JUKEBOX.defaultBlockState());
        var box=(JukeboxBlockEntity)level.getBlockEntity(pos);
        try{
            h.assertTrue(box.canPlaceItem(0,stack),"Native jukebox rejects disc");box.setTheItem(stack);
            h.assertTrue(box.getSongPlayer().isPlaying()&&box.getComparatorOutput()==comparator,"Native playback failed to start");
            var restored=new JukeboxBlockEntity(pos,level.getBlockState(pos));restored.setLevel(level);
            restored.loadWithComponents(box.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
            h.assertTrue(restored.getTheItem().is(item)&&restored.getSongPlayer().isPlaying(),"Disc playback state lost on save");
            for(int i=0;i<duration+21;i++)box.getSongPlayer().tick(level,box.getBlockState());
            h.assertTrue(!box.getSongPlayer().isPlaying()&&box.getTheItem().is(item),"Disc failed to finish cleanly or was consumed");
            h.assertTrue(box.splitTheItem(1).is(item)&&box.getTheItem().isEmpty(),"Disc could not be removed");
        }finally{level.removeBlock(pos,false);}
    }
    @GameTest(template="spell_arena") public static void weaverDiscRoundTrip(GameTestHelper h){
        var level=h.getLevel();
        var recipes=level.getRecipeManager();
        var disc=ModItems.MUSIC_DISC_WEAVER_SPIRIT.get().getDefaultInstance();
        var input=new net.minecraft.world.item.crafting.SingleRecipeInput(disc);
        var analysis=recipes.getRecipeFor(com.mcmagic.omnira.registry.ModRecipes.ANALYSIS_TYPE.get(),input,level).orElseThrow().value();
        var cloud=analysis.assemble(input,level.registryAccess());
        h.assertTrue(cloud.getCount()==1 && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cloud.getItem()).equals(ResourceLocation.parse("omnira:world_generation_probability_cloud")),"Wrong analysis output");
        for(var tool:List.of(ModItems.ANALYSIS_CRYSTAL.get(),ModItems.PHILOSOPHERS_STONE.get())){
            var grid=net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(disc.copy(),tool.getDefaultInstance()));
            var craft=recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level).orElseThrow().value();
            h.assertTrue(ItemStack.matches(cloud,craft.assemble(grid,level.registryAccess())),"Wrong tool conversion");
            var rest=craft.getRemainingItems(grid);
            h.assertTrue(rest.get(0).isEmpty() && rest.get(1).is(tool) && rest.get(1).getDamageValue()==(tool==ModItems.ANALYSIS_CRYSTAL.get()?1:0),"Wrong analysis tool wear");
        }
        var slots=new net.minecraft.world.SimpleContainer(7);
        slots.setItem(6,cloud);
        var assembly=new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(slots);
        var reverse=recipes.getRecipeFor(com.mcmagic.omnira.registry.ModRecipes.ASSEMBLY_TYPE.get(),assembly,level).orElseThrow().value();
        h.assertTrue(ItemStack.matches(disc,reverse.assemble(assembly,level.registryAccess())),"Core assembly did not restore playable disc");
        h.succeed();
    }
    private BiomeMusicGameTests(){}
}
