package com.mcmagic.omnira.myriad;

import java.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** The server, floating hints and JEI use the same datapack recipes. */
public record WorldDevelopmentRecipe(MiniatureWorldBlockEntity.Stage from, MiniatureWorldBlockEntity.Stage to,
        List<Material> materials, int duration, net.minecraft.resources.ResourceLocation series) implements Recipe<WorldDevelopmentRecipe.Input> {
    public static final net.minecraft.resources.ResourceLocation MYRIAD = net.minecraft.resources.ResourceLocation.parse("omnira:myriad_confluence");
    public WorldDevelopmentRecipe(MiniatureWorldBlockEntity.Stage from,MiniatureWorldBlockEntity.Stage to,List<Material> materials,int duration){this(from,to,materials,duration,MYRIAD);}
    public record Material(Optional<Ingredient> ingredient, boolean food, int count, boolean fullStack) {
        public Material(Optional<Ingredient> ingredient,boolean food,int count){this(ingredient,food,count,false);}
        public Material {
            if (food == ingredient.isPresent()) throw new IllegalArgumentException("Specify an ingredient OR food");
        }
        public boolean test(ItemStack stack) { return !stack.isEmpty() && com.mcmagic.omnira.recipe.RecipeInputSafety.empty(stack)
                && (food ? stack.has(DataComponents.FOOD) : ingredient.orElseThrow().test(stack)); }
        public int requiredCount(ItemStack stack){return fullStack?stack.getMaxStackSize():count;}
        public List<ItemStack> examples() {
            return (food ? net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(this::test)
                    : Arrays.stream(ingredient.orElseThrow().getItems())).map(s -> s.copyWithCount(requiredCount(s))).toList();
        }
        public static final Codec<Material> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient").forGetter(Material::ingredient),
                Codec.BOOL.optionalFieldOf("food",false).forGetter(Material::food),
                Codec.intRange(1,64).fieldOf("count").forGetter(Material::count),
                Codec.BOOL.optionalFieldOf("full_stack",false).forGetter(Material::fullStack)).apply(i,Material::new));
    }
    private static final Codec<MiniatureWorldBlockEntity.Stage> STAGE = Codec.STRING.xmap(MiniatureWorldBlockEntity.Stage::valueOf, Enum::name);
    public static final MapCodec<WorldDevelopmentRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            STAGE.fieldOf("from").forGetter(WorldDevelopmentRecipe::from), STAGE.fieldOf("to").forGetter(WorldDevelopmentRecipe::to),
            Material.CODEC.listOf().fieldOf("materials").forGetter(WorldDevelopmentRecipe::materials),
            Codec.intRange(20,1200).optionalFieldOf("duration",100).forGetter(WorldDevelopmentRecipe::duration),
            net.minecraft.resources.ResourceLocation.CODEC.optionalFieldOf("series",MYRIAD).forGetter(WorldDevelopmentRecipe::series)).apply(i,WorldDevelopmentRecipe::new));
    public WorldDevelopmentRecipe {
        materials = List.copyOf(materials);
        if (materials.isEmpty() || materials.size()>9 || from==to) throw new IllegalArgumentException("Invalid world development");
    }
    public static final StreamCodec<RegistryFriendlyByteBuf,WorldDevelopmentRecipe> STREAM = new StreamCodec<>() {
        public WorldDevelopmentRecipe decode(RegistryFriendlyByteBuf b) { return CODEC.codec().parse(b.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), b.readNbt()).getOrThrow(); }
        public void encode(RegistryFriendlyByteBuf b,WorldDevelopmentRecipe r) { b.writeNbt((net.minecraft.nbt.CompoundTag)CODEC.codec().encodeStart(b.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),r).getOrThrow()); }
    };
    public record Input(List<ItemStack> stacks) implements RecipeInput {
        public ItemStack getItem(int slot) { return stacks.get(slot); }
        public int size() { return stacks.size(); }
    }
    /** Small integral max-flow avoids double-counting overlapping tags, food and exact ingredients. */
    public int[] allocation(List<ItemStack> stacks) {
        return allocateStacks(stacks,0,new int[stacks.size()]);
    }
    private int[] allocateStacks(List<ItemStack> stacks,int row,int[] used){
        for(int r=row;r<materials.size();r++){
            var requirement=materials.get(r);if(!requirement.fullStack())continue;
            for(int s=0;s<stacks.size();s++){
                var stack=stacks.get(s);int amount=requirement.requiredCount(stack);
                if(!requirement.test(stack)||stack.getCount()-used[s]<amount)continue;
                used[s]+=amount;var result=allocateStacks(stacks,r+1,used);used[s]-=amount;
                if(result!=null)return result;
            }
            return null;
        }
        return allocateCounts(stacks,used);
    }
    private int[] allocateCounts(List<ItemStack> stacks,int[] used){
        int n=stacks.size(), m=materials.size(), sink=1+n+m, size=sink+1, total=0;
        int[][] capacity=new int[size][size], flow=new int[size][size];
        for(int s=0;s<n;s++)capacity[0][1+s]=stacks.get(s).getCount()-used[s];
        for(int r=0;r<m;r++){
            var requirement=materials.get(r);if(requirement.fullStack())continue;
            capacity[1+n+r][sink]=requirement.count(); total+=requirement.count();
            for(int s=0;s<n;s++)if(requirement.test(stacks.get(s)))capacity[1+s][1+n+r]=64;
        }
        int sent=0;
        while(true){
            int[] parent=new int[size]; Arrays.fill(parent,-1); parent[0]=0; var queue=new ArrayDeque<Integer>();queue.add(0);
            while(!queue.isEmpty()&&parent[sink]<0){int a=queue.remove();for(int b=1;b<size;b++)if(parent[b]<0&&capacity[a][b]>flow[a][b]){parent[b]=a;queue.add(b);}}
            if(parent[sink]<0)break;
            int amount=Integer.MAX_VALUE;
            for(int b=sink;b!=0;b=parent[b])amount=Math.min(amount,capacity[parent[b]][b]-flow[parent[b]][b]);
            for(int b=sink;b!=0;b=parent[b]){flow[parent[b]][b]+=amount;flow[b][parent[b]]-=amount;}
            sent+=amount;
        }
        if(sent!=total)return null;
        int[] result=new int[n];for(int s=0;s<n;s++)result[s]=used[s]+flow[0][1+s];return result;
    }
    public boolean matches(Input input,Level level){return allocation(input.stacks())!=null;}
    public ItemStack assemble(Input input,HolderLookup.Provider provider){return getResultItem(provider);}
    public ItemStack getResultItem(HolderLookup.Provider provider){return to.product();}
    public boolean canCraftInDimensions(int x,int y){return true;}
    public boolean isSpecial(){return true;}
    public RecipeType<?> getType(){return MiniatureWorldContent.TYPE.get();}
    public RecipeSerializer<?> getSerializer(){return MiniatureWorldContent.SERIALIZER.get();}
}
