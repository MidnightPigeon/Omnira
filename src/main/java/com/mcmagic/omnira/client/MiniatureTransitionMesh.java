package com.mcmagic.omnira.client;

import java.util.*;
import com.google.gson.*;
import com.mcmagic.omnira.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** Cached cells and neighbours; only the current animation frontier exposes internal faces. */
final class MiniatureTransitionMesh {
    private static final int[][] NEIGHBOURS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private record Cell(float x,float y,float z,int color,boolean terrain,int mask,ResourceLocation texture,int[] neighbours){}
    private final List<Cell> cells=new ArrayList<>();
    private final Map<ResourceLocation,List<Integer>> groups=new LinkedHashMap<>();
    private final List<Integer> surface=new ArrayList<>();
    private final float[] amounts;
    private final float[][] ranks;
    MiniatureTransitionMesh(JsonArray parts){
        var positions=new HashMap<Integer,Integer>();
        for(var element:parts){
            var p=element.getAsJsonObject();var a=p.getAsJsonArray("a");
            float x=a.get(0).getAsFloat()+.0625F,y=a.get(1).getAsFloat()+.0625F,z=a.get(2).getAsFloat()+.0625F;
            var texture=ResourceLocation.parse(p.get("texture").getAsString());
            int index=cells.size(),mask=p.get("faces").getAsInt();
            cells.add(new Cell(x,y,z,(int)Long.parseLong(p.get("color").getAsString(),16),p.get("layer").getAsString().equals("terrain"),mask,texture,new int[6]));
            groups.computeIfAbsent(texture,k->new ArrayList<>()).add(index);if(mask!=0)surface.add(index);
            positions.put(key(x,y,z),index);
        }
        for(var cell:cells)for(int f=0;f<6;f++){var d=NEIGHBOURS[f];cell.neighbours[f]=positions.getOrDefault(key(cell.x+d[0]*.125F,cell.y+d[1]*.125F,cell.z+d[2]*.125F),-1);}
        amounts=new float[cells.size()];
        ranks=new float[MiniatureWorldTransition.Preset.values().length*2][cells.size()];
        for(var preset:MiniatureWorldTransition.Preset.values())for(int in=0;in<2;in++)for(int i=0;i<cells.size();i++){
            var c=cells.get(i);ranks[preset.ordinal()*2+in][i]=MiniatureWorldTransition.rank(preset,in==1,c.x,c.y,c.z,c.terrain);
        }
    }
    private static int key(float x,float y,float z){return Math.round(x*8)<<16|Math.round(y*8)<<8|Math.round(z*8);}
    void draw(MiniatureWorldTransition.Preset preset,float progress,boolean incoming,boolean gate,float offset,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        draw(preset,progress,incoming,gate,false,offset,pose,buffers,light,overlay);
    }
    void swallow(float progress,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        draw(MiniatureWorldTransition.Preset.WAVE,progress,false,false,true,0,pose,buffers,light,overlay);
    }
    private void draw(MiniatureWorldTransition.Preset preset,float progress,boolean incoming,boolean gate,boolean swallow,float offset,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        for(int i=0;i<cells.size();i++){
            var c=cells.get(i);amounts[i]=swallow?1-MiniatureWorldTransition.riftCapture(progress,c.x):gate?MiniatureWorldTransition.gateAmount(progress,c.x,c.y+offset,c.z):MiniatureWorldTransition.atRank(ranks[preset.ordinal()*2+(incoming?1:0)][i],progress,incoming);
        }
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        for(var group:groups.entrySet()){
            var texture=group.getKey();var sprite=atlas.apply(texture);
            boolean glass=texture.getNamespace().equals("omnira")||texture.getPath().contains("water")||texture.getPath().contains("lava");
            var out=buffers.getBuffer(glass?CrystalGlassLayer.ATLAS:RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
            int lit=texture.getPath().contains("glow")||texture.getPath().contains("lava")?LightTexture.FULL_BRIGHT:light;
            for(int index:group.getValue()){
                var c=cells.get(index);float amount=amounts[index];if(amount<=0)continue;
                boolean vertical=gate||preset==MiniatureWorldTransition.Preset.GROW;
                float cx=swallow?2.5F+(c.x-2.5F)*amount:c.x,cy=swallow?2.5F+(c.y-2.5F)*amount:c.y,cz=swallow?2.5F+(c.z-2.5F)*amount:c.z;
                float radius=vertical?.0625F:.0625F*amount,x=cx-radius,X=cx+radius,z=cz-radius,Z=cz+radius;
                float y=vertical?cy-.0625F:cy-radius,Y=vertical?y+.125F*amount:cy+radius;
                for(int f=0;f<6;f++){
                    int neighbour=c.neighbours[f];if(amount==1&&neighbour>=0&&amounts[neighbour]==1)continue;
                    for(int v:FACES[f]){
                        float px=(v==1||v==2||v==5||v==6)?X:x,py=(v==2||v==3||v==6||v==7)?Y:y,pz=v>=4?Z:z;
                        float u=f==2||f==3?(pz-z)/.125F:(px-x)/.125F,uv=f>=4?(pz-z)/.125F:(py-y)/.125F;
                        out.addVertex(pose.last(),px,py,pz).setColor(c.color).setUv(sprite.getU(u),sprite.getV(uv)).setOverlay(overlay).setLight(lit)
                                .setNormal(pose.last(),NEIGHBOURS[f][0],NEIGHBOURS[f][1],NEIGHBOURS[f][2]);
                    }
                }
            }
        }
    }
    private Cell sample(int index){return surface.isEmpty()?null:cells.get(surface.get(Math.floorMod(index*73,surface.size())));}
    void grains(MiniatureTransitionMesh next,float progress,PoseStack pose,MultiBufferSource buffers){
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<64;i++){
            var a=sample(i);var b=next==null?null:next.sample(i+7);if(a==null&&b==null)continue;
            float x=a==null?2.5F:a.x,y=a==null?1.2F:a.y,z=a==null?2.5F:a.z;
            var p=MiniatureWorldTransition.grain(progress,i,x,y,z,b==null?x:b.x,b==null?y:b.y,b==null?z:b.z);
            if(p.size()>0)StellarVisuals.cube(pose,out,p.x(),p.y(),p.z(),p.size(),i%3==0?0xFFE9D6FF:0xFFBDEBF5);
        }
    }
}
