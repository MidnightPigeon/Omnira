package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.guidance.ConstructionPattern;
import com.mcmagic.omnira.myriad.WorldComponent;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spacetime.CorridorRitual;
import com.mcmagic.omnira.world.dimension.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

public final class RitualLayout {
    static boolean sword(String scene){return scene.equals("sword_shaping");}
    public static boolean supports(String scene){
        return switch(scene){
            case "dream_ritual","affinity_ritual","resonance_ritual","corridor_ritual","cleansing_ritual",
                    "genesis_ritual","world_forming_ritual","sword_shaping" -> true;
            default -> false;
        };
    }
    static int[][] anchors(String scene){return sword(scene)?SwordShapingRitual.ANCHORS:DreamRitual.ANCHORS;}
    static Item offering(String scene,int i){
        return switch(scene){
            case "world_forming_ritual" -> WorldComponent.values()[i].item.get();
            case "genesis_ritual" -> GenesisRitual.ingredients().get(i);
            case "corridor_ritual" -> CorridorRitual.offering(i);
            case "affinity_ritual" -> AffinityRitual.ingredients().get(i);
            case "resonance_ritual" -> ModItems.RESONANCE_CORE.get();
            case "cleansing_ritual" -> ModItems.INFUSED_SPIRITUAL_CRYSTAL.get();
            default -> ModItems.SPIRITUAL_CRYSTAL.get();
        };
    }
    static ConstructionPattern pattern(String scene){
        var center=switch(scene){
            case "sword_shaping" -> ModBlocks.PURE_VESSEL.get();
            case "dream_ritual","resonance_ritual","cleansing_ritual" -> ModBlocks.RITUAL_ENERGY_CORE.get();
            default -> ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get();
        };
        var cells=new ArrayList<ConstructionPattern.Cell>();
        cells.add(new ConstructionPattern.Cell(BlockPos.ZERO,center.defaultBlockState(),center.defaultBlockState()));
        var state=ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState();
        for(var a:anchors(scene))cells.add(new ConstructionPattern.Cell(new BlockPos(a[0],0,a[1]),state,state));
        return new ConstructionPattern(List.copyOf(cells));
    }

    // Assign the remaining offerings around items already placed, including interchangeable ingredients.
    private final String scene;
    private final Ingredient[] ingredients;
    private final ItemStack[] contents;
    private final int[] assigned;
    private final boolean[] complete;
    RitualLayout(String scene,Level level){
        this.scene=scene;
        int size=anchors(scene).length;
        ingredients=new Ingredient[size];contents=new ItemStack[size];
        assigned=new int[size];complete=new boolean[size];
        var recipe=sword(scene)?GuideRecipes.swordExample(level):Optional.<com.mcmagic.omnira.recipe.SwordShapingRecipe>empty();
        for(int i=0;i<size;i++){
            assigned[i]=i;
            ingredients[i]=sword(scene)?recipe.isPresent()?recipe.get().offerings().get(i):Ingredient.EMPTY:Ingredient.of(offering(scene,i));
        }
    }
    ItemStack display(int i,int ticks){
        var items=ingredients[assigned[i]].getItems();
        return items.length==0?ItemStack.EMPTY:items[Math.floorMod(ticks/25,items.length)];
    }
    boolean complete(int i){return complete[i];}
    void update(Level level,BlockPos center){
        var offsets=anchors(scene);
        boolean changed=false;
        for(int i=0;i<offsets.length;i++){
            var pos=center.offset(offsets[i][0],0,offsets[i][1]);
            var stack=level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof CrystalPedestalBlockEntity p?p.getItem(0):ItemStack.EMPTY;
            if(contents[i]==null || !ItemStack.isSameItemSameComponents(contents[i],stack)){
                contents[i]=stack.copy();changed=true;
            }
        }
        if(!changed)return;
        if(scene.equals("corridor_ritual")){
            int direct=0,rotated=0;
            int[] alternate=new int[offsets.length];
            for(int i=0;i<offsets.length;i++){
                for(int j=0;j<offsets.length;j++)if(offsets[j][0]==-offsets[i][1] && offsets[j][1]==offsets[i][0])alternate[i]=j;
                if(ingredients[i].test(contents[i]))direct++;
                if(ingredients[alternate[i]].test(contents[i]))rotated++;
            }
            for(int i=0;i<offsets.length;i++){
                assigned[i]=rotated>direct?alternate[i]:i;
                complete[i]=ingredients[assigned[i]].test(contents[i]);
            }
            return;
        }
        int[] owner=new int[contents.length];Arrays.fill(owner,-1);Arrays.fill(complete,false);
        for(int i=0;i<contents.length;i++)if(!contents[i].isEmpty())
            complete[i]=assign(i,owner,new boolean[contents.length]);
        for(int j=0;j<owner.length;j++)if(owner[j]>=0)assigned[owner[j]]=j;
        int next=0;
        for(int i=0;i<contents.length;i++)if(!complete[i]){
            while(owner[next]>=0)next++;
            assigned[i]=next++;
            complete[i]=ingredients[assigned[i]].isEmpty();
        }
    }
    private boolean assign(int slot,int[] owner,boolean[] seen){
        for(int j=0;j<ingredients.length;j++){
            if(seen[j] || !ingredients[j].test(contents[slot]))continue;
            seen[j]=true;
            if(owner[j]<0 || assign(owner[j],owner,seen)){owner[j]=slot;return true;}
        }
        return false;
    }
    static int pick(PoseStack pose,Vector3f cursor,int[][] anchors){
        var inverse=new Matrix4f(pose.last().pose()).invert();
        var from=new Vec3(inverse.transformPosition(cursor.x,cursor.y,cursor.z+1000,new Vector3f()));
        var to=new Vec3(inverse.transformPosition(cursor.x,cursor.y,cursor.z-1000,new Vector3f()));
        int result=-1;
        double distance=Double.POSITIVE_INFINITY;
        for(int i=0;i<anchors.length;i++){
            int x=anchors[i][0],z=anchors[i][1];
            var hit=new AABB(x,0,z,x+1,1.4,z+1).clip(from,to);
            if(hit.isPresent() && from.distanceToSqr(hit.get())<distance){
                distance=from.distanceToSqr(hit.get());result=i;
            }
        }
        return result;
    }
    static void highlight(PoseStack pose,MultiBufferSource buffers){
        LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),new AABB(-.4,-.4,-.4,.4,.4,.4),.65F,.95F,1,.85F);
    }
}
