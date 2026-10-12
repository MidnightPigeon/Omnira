package com.mcmagic.omnira.alchemy.catalysis;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class CatalysisGrowth {
    private static final java.util.concurrent.ConcurrentMap<Item,ItemStack> ORES=new java.util.concurrent.ConcurrentHashMap<>();
    @net.neoforged.bus.api.SubscribeEvent public static void tagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent event){ORES.clear();}
    public enum Yield implements net.minecraft.util.StringRepresentable {
        FIXED,CROP,TREE,FRUIT,FUNGUS;
        public static final com.mojang.serialization.Codec<Yield> CODEC=net.minecraft.util.StringRepresentable.fromEnum(Yield::values);
        public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public record Plan(int stages,BlockState plant,ItemStack product,Yield yield) {
        public List<ItemStack> harvest(ServerLevel level,BlockPos pos,ItemStack substrate,ItemStack seed){
            if(yield==Yield.FIXED)return List.of(product.copy());
            if(yield==Yield.TREE)return CatalysisTreeGrowth.harvest(level,plant,Block.byItem(substrate.getItem()).defaultBlockState());
            if(yield==Yield.FUNGUS)return products(CatalysisTreeGrowth.harvest(level,plant,Block.byItem(substrate.getItem()).defaultBlockState()),seed,2);
            var mature=yield==Yield.FRUIT?(plant.is(Blocks.MELON_STEM)?Blocks.MELON:Blocks.PUMPKIN).defaultBlockState():plant;
            if(yield==Yield.CROP&&plant.getBlock() instanceof CropBlock crop)mature=crop.getStateForAge(crop.getMaxAge());
            return doubledProducts(Block.getDrops(mature,level,pos,null,null,ItemStack.EMPTY),seed);
        }
        public BlockState visual(int stage){
            for(var family:crystalFamilies())if(plant.is(family[4]))return stages==5&&stage==1?Blocks.AIR.defaultBlockState():family[Math.clamp(stages==5?stage-1:stage,1,4)].defaultBlockState();
            var age=age(plant);if(age==null)return plant;
            int min=Collections.min(age.getPossibleValues()),max=Collections.max(age.getPossibleValues());
            if(stages==4){
                int index=Math.clamp(stage-1,0,3);
                if(plant.is(Blocks.PITCHER_CROP))return plant.setValue(age,index);
                if(plant.is(Blocks.TORCHFLOWER_CROP))return plant.setValue(age,index/2);
                if(plant.getBlock() instanceof CocoaBlock)return plant.setValue(age,index<2?0:index-1);
            }
            return plant.setValue(age,min+Math.round(Math.clamp(stage-1,0,stages-1)*(float)(max-min)/(stages-1)));
        }
    }
    static Block[][] crystalFamilies(){return com.mcmagic.omnira.block.CrystalFamilies.all();}
    public static List<ItemStack> doubledProducts(List<ItemStack> drops,ItemStack seed){
        return products(drops,seed,2);
    }
    public static List<ItemStack> products(List<ItemStack> drops,ItemStack seed,int multiplier){
        var result=new ArrayList<ItemStack>();
        // Edible planting items remain produce even when the loot also includes a rare secondary item.
        boolean separateSeed=seed.getItem() instanceof ItemNameBlockItem&&!seed.has(net.minecraft.core.component.DataComponents.FOOD)
                &&drops.stream().anyMatch(s->!s.is(seed.getItem()));
        for(var stack:drops){if(stack.isEmpty()||separateSeed&&stack.is(seed.getItem()))continue;int n=stack.getCount()*multiplier;
            while(n>0){int count=Math.min(n,stack.getMaxStackSize());result.add(stack.copyWithCount(count));n-=count;}}
        return result;
    }
    public static IntegerProperty age(BlockState state){return state.getProperties().stream().filter(p->p instanceof IntegerProperty&&p.getName().equals("age")).map(p->(IntegerProperty)p).findFirst().orElse(null);}
    private static boolean tagged(BlockState state,String tag){return state.is(TagKey.create(Registries.BLOCK,ResourceLocation.parse(tag)));}
    public static boolean acceptsSubstrate(Level level,ItemStack stack){
        return stack.getItem() instanceof BlockItem||level!=null&&level.getRecipeManager().getAllRecipesFor(CatalysisContent.TYPE.get())
                .stream().anyMatch(holder->holder.value().substrate().test(stack));
    }
    public static Plan resolve(Level level,ItemStack substrate,ItemStack seed,FluidStack fluid){
        if(substrate.isEmpty()||seed.isEmpty()||fluid.isEmpty())return null;
        var soil=Block.byItem(substrate.getItem()).defaultBlockState();var plant=Block.byItem(seed.getItem()).defaultBlockState();
        var input=new CatalysisRecipe.Input(substrate,seed,fluid);
        for(var holder:level.getRecipeManager().getAllRecipesFor(CatalysisContent.TYPE.get()))if(holder.value().matches(input,level)){
            var recipe=holder.value();
            for(var family:crystalFamilies())if(soil.is(family[0])&&seed.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))plant=family[4].defaultBlockState();
            return new Plan(4,plant,recipe.result(),recipe.yield());
        }
        if(!(substrate.getItem() instanceof BlockItem))return null;
        boolean dirt=soil.is(BlockTags.DIRT)||soil.is(Blocks.FARMLAND);
        if(fluid.getFluid()==com.mcmagic.omnira.mire.MireContent.SOURCE.get()&&plant.is(TimeNatureContent.SAPLING.get())
                &&soil.is(com.mcmagic.omnira.time.TemporalSoils.SILTS))return new Plan(4,plant,ItemStack.EMPTY,Yield.TREE);
        if(fluid.getFluid()==com.mcmagic.omnira.space.sea.VoidSeaContent.SOURCE.get()&&soil.is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get())){
            var ore=ore(seed);return ore.isEmpty()?null:new Plan(4,Block.byItem(ore.getItem()).defaultBlockState(),ore,Yield.FIXED);
        }
        if(fluid.getFluid()==Fluids.WATER&&plant.is(com.mcmagic.omnira.registry.ShadowWoodContent.SAPLING.get())
                &&soil.is(com.mcmagic.omnira.time.TemporalSoils.SHADOW))return new Plan(4,plant,ItemStack.EMPTY,Yield.TREE);
        if(fluid.getFluid()!=Fluids.WATER||!dirt)return null;
        if(plant.is(Blocks.BAMBOO))return new Plan(4,plant,new ItemStack(Items.BAMBOO,8),Yield.FIXED);
        if((plant.is(BlockTags.SAPLINGS)||plant.is(Blocks.AZALEA)||plant.is(Blocks.FLOWERING_AZALEA))
                &&!plant.is(TimeNatureContent.SAPLING.get())&&!BuiltInRegistries.BLOCK.getKey(plant.getBlock()).getNamespace().equals("omnira"))
            return new Plan(4,plant,ItemStack.EMPTY,Yield.TREE);
        if(plant.is(Blocks.SWEET_BERRY_BUSH))return new Plan(4,plant.setValue(SweetBerryBushBlock.AGE,3),ItemStack.EMPTY,Yield.CROP);
        if(plant.getBlock() instanceof StemBlock){
            var fruit=plant.is(Blocks.MELON_STEM)?Blocks.MELON:plant.is(Blocks.PUMPKIN_STEM)?Blocks.PUMPKIN:null;
            if(fruit!=null)return new Plan(4,plant,ItemStack.EMPTY,Yield.FRUIT);
        }
        var age=age(plant);if(age==null||!(plant.getBlock() instanceof CropBlock||tagged(plant,"minecraft:crops")||tagged(plant,"c:crops")))return null;
        int min=Collections.min(age.getPossibleValues()),max=Collections.max(age.getPossibleValues());
        if(max<=min||!supports(level,soil,plant))return null;
        return new Plan(4,plant.setValue(age,max),ItemStack.EMPTY,Yield.CROP);
    }
    private static boolean supports(Level level,BlockState soil,BlockState plant){
        // Evaluate survival in a read-only local view with farmland as the cultivated dirt surface.
        var root=new BlockPos(0,64,0);var cultivated=soil.is(BlockTags.DIRT)?Blocks.FARMLAND.defaultBlockState():soil;
        var view=(LevelReader)java.lang.reflect.Proxy.newProxyInstance(LevelReader.class.getClassLoader(),new Class<?>[]{LevelReader.class},(proxy,method,args)->{
            return switch(method.getName()){
                case "getBlockState"->((BlockPos)args[0]).equals(root.below())?cultivated:((BlockPos)args[0]).equals(root)?plant:Blocks.AIR.defaultBlockState();
                case "getRawBrightness","getMaxLocalRawBrightness","getBrightness","getMaxLightLevel"->15;
                case "canSeeSky","hasChunk","hasChunkAt","hasChunksAt"->true;
                case "getFluidState"->Fluids.EMPTY.defaultFluidState();
                case "getBlockEntity"->null;
                case "isClientSide"->false;
                default->{if(method.isDefault())yield java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args);yield method.invoke(level,args);}
            };
        });
        try{return plant.canSurvive(view,root);}catch(RuntimeException e){return false;}
    }
    public static ItemStack ore(ItemStack mineral){
        return ORES.computeIfAbsent(mineral.getItem(),item->findOre(new ItemStack(item))).copy();
    }
    private static ItemStack findOre(ItemStack mineral){
        var families=mineral.getTags().map(TagKey::location).filter(id->id.getNamespace().equals("c")&&
                (id.getPath().startsWith("gems/")||id.getPath().startsWith("ingots/")||id.getPath().startsWith("raw_materials/")||id.getPath().startsWith("dusts/")))
                .map(id->id.getPath().substring(id.getPath().indexOf('/')+1)).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        if(mineral.is(Items.COAL))families.add("coal");if(mineral.is(Items.REDSTONE))families.add("redstone");if(mineral.is(Items.LAPIS_LAZULI))families.add("lapis");
        for(var family:families){var tag=TagKey.create(Registries.ITEM,ResourceLocation.parse("c:ores/"+family));
            var options=BuiltInRegistries.ITEM.stream().filter(item->item instanceof BlockItem&&item.builtInRegistryHolder().is(tag))
                    .sorted(Comparator.comparing(item->{var id=BuiltInRegistries.ITEM.getKey(item);return (id.getPath().startsWith("deepslate_")?"2":id.getNamespace().equals("minecraft")?"0":"1")+id;})).toList();
            if(!options.isEmpty())return new ItemStack(options.getFirst());}
        return ItemStack.EMPTY;
    }
    private CatalysisGrowth(){}
}
