package com.mcmagic.omnira.client;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.registry.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;
import java.util.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ReactionPoolClient {
    private static final int[][] WORKING_GLYPH={{0,0},{0,1},{0,2},{-1,1},{1,-1}};
    private record Mesh(float[][] vertices,int[][] faces,String material,int color,String motion,float[] pivot){}
    private static final List<Mesh> MESHES=new ArrayList<>();
    private static final List<float[][]> LIQUID_SHELL=new ArrayList<>();
    private static final List<List<float[]>> LIQUID_PROFILES=new ArrayList<>();
    private static final Map<ReactionPoolBlockEntity,Motion> MOTIONS=new WeakHashMap<>();
    private static final class Motion {double last,phase,left,right;boolean initialized;}
    static {
        for(var entry:ReactionPoolGeometry.DATA.getAsJsonArray("parts")){
            var p=entry.getAsJsonObject();var vertices=p.getAsJsonArray("vertices");var faces=p.getAsJsonArray("faces");
            float[][] v=new float[vertices.size()][3];int[][] f=new int[faces.size()][4];float[] pivot=new float[3];
            for(int i=0;i<v.length;i++)for(int j=0;j<3;j++)v[i][j]=vertices.get(i).getAsJsonArray().get(j).getAsFloat()/16;
            for(int i=0;i<f.length;i++)for(int j=0;j<4;j++)f[i][j]=faces.get(i).getAsJsonArray().get(j).getAsInt();
            if(p.has("pivot"))for(int j=0;j<3;j++)pivot[j]=p.getAsJsonArray("pivot").get(j).getAsFloat()/16;
            MESHES.add(new Mesh(v,f,p.get("material").getAsString(),(int)Long.parseLong(p.get("color").getAsString(),16),p.has("motion")?p.get("motion").getAsString():"",pivot));
        }
        var liquid=ReactionPoolGeometry.DATA.getAsJsonObject("liquid");var shell=liquid.getAsJsonObject("shell");var vertices=shell.getAsJsonArray("vertices");
        for(var entry:shell.getAsJsonArray("faces")){
            float[][] face=new float[4][3];for(int i=0;i<4;i++)for(int a=0;a<3;a++)face[i][a]=vertices.get(entry.getAsJsonArray().get(i).getAsInt()).getAsJsonArray().get(a).getAsFloat()/16;
            LIQUID_SHELL.add(face);
        }
        for(var entry:liquid.getAsJsonArray("profiles")){
            var rows=new ArrayList<float[]>();for(var row:entry.getAsJsonArray()){
                float[] r=new float[4];for(int i=0;i<4;i++)r[i]=row.getAsJsonArray().get(i).getAsFloat()/16;rows.add(r);
            }LIQUID_PROFILES.add(rows);
        }
    }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(ModMenuTypes.REACTION_POOL.get(),com.mcmagic.omnira.client.screen.ReactionPoolScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(ModBlockEntityTypes.REACTION_POOL.get(),context->new BlockEntityRenderer<BlockEntity>(){
            @Override public void render(BlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                if(!(be instanceof ReactionPoolBlockEntity pool)||!pool.controller()||pool.getLevel()==null)return;
                pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(-pool.getBlockState().getValue(ReactionPoolBlock.FACING).toYRot()));pose.translate(-.5,0,-.5);
                draw(pool,pool.getLevel().getGameTime()+partial,pose,buffers,light,overlay);pose.popPose();
            }
            @Override public AABB getRenderBoundingBox(BlockEntity be){return new AABB(be.getBlockPos()).inflate(1.3).expandTowards(0,1.3,0);}
        });
    }
    private static Motion motion(ReactionPoolBlockEntity pool,double time){
        if(pool==null)return new Motion();var m=MOTIONS.computeIfAbsent(pool,k->new Motion());
        double elapsed=m.initialized?Math.clamp(time-m.last,0,5):0,speed=pool.powered()?Math.min(8,pool.efficiency()):0;
        m.phase+=elapsed*speed;
        if(net.neoforged.fml.ModList.get().isLoaded("create")){
            m.left=CreateReactionPoolClient.shaftAngle(pool,3);
            m.right=CreateReactionPoolClient.shaftAngle(pool,5);
        }else if(pool.electricalPowered()){m.left=0;m.right=0;}
        else{m.left=(m.left+elapsed*speed*2.4)%360;m.right=m.left;}
        m.initialized=true;m.last=time;return m;
    }
    public static void renderPreview(double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        draw(null,time,pose,buffers,light,overlay);
    }
    private static void draw(ReactionPoolBlockEntity pool,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();var atlas=mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);var motion=motion(pool,time);double phase=motion.phase;boolean working=pool!=null&&pool.powered();
        for(var mesh:MESHES){
            pose.pushPose();float angle=switch(mesh.motion){case "drive"->(float)(phase*1.5%360);case "counter"->(float)(-phase*2.1%360);case "left"->(float)motion.left;case "right"->(float)motion.right;default->0;};
            if(!mesh.motion.isEmpty()){pose.translate(mesh.pivot[0],mesh.pivot[1],mesh.pivot[2]);pose.mulPose((mesh.motion.equals("left")||mesh.motion.equals("right")?Axis.XP:Axis.YP).rotationDegrees(angle));pose.translate(-mesh.pivot[0],-mesh.pivot[1],-mesh.pivot[2]);}
            var sprite=atlas.apply(ResourceLocation.parse("omnira:block/reaction_pool_"+mesh.material));
            boolean electric=pool!=null&&pool.electricalPowered()&&(mesh.motion.equals("left")||mesh.motion.equals("right"));
            for(var f:mesh.faces)quad(pose,buffers,sprite,mesh.vertices[f[0]],mesh.vertices[f[1]],mesh.vertices[f[2]],mesh.vertices[f[3]],electric?0xFFE0FAFF:mesh.color,electric||working&&mesh.material.equals("glow")?LightTexture.FULL_BRIGHT:light,overlay,64);
            pose.popPose();
        }
        panel(pool,pose,buffers,phase,working,pool!=null&&pool.reacting(),light,overlay);
        if(pool==null)return;
        double height=ReactionPoolGeometry.LIQUID_BOTTOM;boolean wet=pool.fluids.total()>0;boolean bottom=true;
        for(var fluid:pool.fluids.snapshot().reversed()){
            double top=height+(ReactionPoolGeometry.LIQUID_TOP-ReactionPoolGeometry.LIQUID_BOTTOM)*fluid.getAmount()/pool.capacity();
            var ext=IClientFluidTypeExtensions.of(fluid.getFluid());int color=ext.getTintColor(fluid);if((color>>>24)==0)color|=0xFF000000;
            int packed=LightTexture.pack(Math.max(fluid.getFluidType().getLightLevel(fluid),LightTexture.block(light)),LightTexture.sky(light));
            liquid(pose,buffers,atlas.apply(ext.getStillTexture(fluid)),height,top,color,packed,overlay,bottom);height=top;bottom=false;
        }
        MultiBufferSource sorted=CrystalGlassLayer.contents(buffers);
        for(int i=0;i<9;i++){
            var stack=pool.items.getStackInSlot(i);if(stack.isEmpty())continue;
            double y=wet?height+.13+Math.sin(time*.045+i)*.02:ReactionPoolGeometry.VESSEL_FLOOR+.115;
            item(stack,.5+(i%3-1)*.54,y,.5+(i/3-1)*.54,wet?time*.4+i*35:i*35,.23F,pool,pose,sorted,light,overlay,i);
        }
        double age=time-pool.outputItemTick();var output=pool.outputItem();
        if(!output.isEmpty()&&age>=0&&age<18){var p=ReactionPoolGeometry.outputPoint(false,age/18);item(output,p[0],p[1],p[2],age*12,.15F,pool,pose,sorted,light,overlay,20);}
        age=time-pool.outputFluidTick();var fluid=pool.outputFluid();
        if(!fluid.isEmpty()&&age>=0&&age<18){var ext=IClientFluidTypeExtensions.of(fluid.getFluid());int color=ext.getTintColor(fluid);if((color>>>24)==0)color|=0xFF000000;
            for(int i=0;i<4;i++){double progress=age/18-i*.06;if(progress<0)continue;var p=ReactionPoolGeometry.outputPoint(true,progress);
                box(pose,buffers,atlas.apply(ext.getStillTexture(fluid)),p[0]-.065,p[1]-.065,p[2]-.065,p[0]+.065,p[1]+.065,p[2]+.065,color,light,overlay);}}
        if(working){
            var out=buffers.getBuffer(CrystalGlassLayer.COLOR);
            for(int i=0;i<4;i++){
                double a=phase*.018+i*Math.PI/2;
                pose.pushPose();pose.translate(.5+Math.cos(a)*.47,.43,.5+Math.sin(a)*.47);pose.mulPose(Axis.YP.rotationDegrees((float)(90-Math.toDegrees(a))));
                for(int[] xy:WORKING_GLYPH)cube(out,pose,xy[0]*.02F,xy[1]*.02F,0,.008F,0xC4DEF7FF);pose.popPose();
            }
            if(pool.reacting())for(int i=0;i<24;i++){
                double cycle=(time*.014+i/24.0)%1,a=time*.035+i*Math.PI/6,r=.4+cycle*.5;
                cube(out,pose,(float)(.5+r*Math.cos(a)),(float)(height+.1+cycle*.7),(float)(.5+r*Math.sin(a)),.018F,i%2==0?0xDED1F4E8:0xDED6ECFF);
                double angle=phase*.05+i*Math.PI/12;
                double x=Math.cos(angle),z=Math.sin(angle),radius=1.42/Math.max(Math.max(Math.abs(x),Math.abs(z)),(Math.abs(x)+Math.abs(z))/Math.sqrt(2));
                cube(out,pose,(float)(.5+x*radius),1.92F,(float)(.5+z*radius),.019F,i%2==0?0xE0BAF8D6:0xE0BDE5FF);
            }
        }
    }
    private static void item(ItemStack stack,double x,double y,double z,double angle,float scale,ReactionPoolBlockEntity pool,PoseStack pose,MultiBufferSource buffers,int light,int overlay,int seed){
        pose.pushPose();pose.translate(x,y,z);pose.mulPose(Axis.YP.rotationDegrees((float)angle));pose.scale(scale,scale,scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,buffers,pool.getLevel(),seed);pose.popPose();
    }
    private static void panel(ReactionPoolBlockEntity pool,PoseStack pose,MultiBufferSource buffers,double phase,boolean working,boolean reacting,int light,int overlay){
        var data=ReactionPoolGeometry.DATA.getAsJsonObject("panel");var at=data.getAsJsonArray("center");
        pose.pushPose();pose.translate(at.get(0).getAsDouble()/16,at.get(1).getAsDouble()/16,at.get(2).getAsDouble()/16);
        pose.mulPose(Axis.XP.rotationDegrees(data.get("tilt").getAsFloat()));float z=data.get("front").getAsFloat()/16;
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);int ink=working?0xE5E9FFFF:0xBA89BED4;
        for(int i=0;i<3;i++)cube(out,pose,-(.22F+i*.024F),.05F,z,.011F,ink);
        float pulse=working?(float)(.5+.5*Math.sin(phase*.07)):0;
        MultiBufferSource sorted=CrystalGlassLayer.contents(buffers);
        for(int i=0;i<4;i++){
            var stack=pool==null?ItemStack.EMPTY:pool.upgrades.getStackInSlot(i);float x=.21F+i%2*.13F,y=.047F+(i/2==0?.072F:-.072F);
            int color=0x887A9AAE;var kind=com.mcmagic.omnira.block.entity.OrbUpgrades.kind(stack);if(kind!=null)color=0xE0000000|kind.displayColor;
            for(int side=-1;side<=1;side+=2){cube(out,pose,x+side*.052F,y,z,.008F,color);cube(out,pose,x,y+side*.05F,z,.008F,color);}
            if(!stack.isEmpty())item(stack,x,y,z-.014,0,.095F,pool,pose,sorted,LightTexture.FULL_BRIGHT,overlay,30+i);
        }
        if(pool!=null)panelLiquid(pool,pose,buffers,light,overlay);
        if(working)cube(out,pose,(float)(Math.sin(phase*.04)*.10),.16F,z,.01F,(Math.round(120+100*pulse)<<24)|0xDDF9FF);
        pose.popPose();
    }
    private static void panelLiquid(ReactionPoolBlockEntity pool,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        double base=.75/16-1.95/16,centerY=.75/16,centerZ=-.1/16,unit=1.95/4.1/16,radius=1.95/16;
        for(var fluid:pool.fluids.snapshot().reversed()){
            double top=base+2*radius*fluid.getAmount()/pool.capacity();
            var ext=IClientFluidTypeExtensions.of(fluid.getFluid());int color=ext.getTintColor(fluid);if((color>>>24)==0)color|=0xFF000000;
            var sprite=atlas.apply(ext.getStillTexture(fluid));int packed=LightTexture.pack(Math.max(fluid.getFluidType().getLightLevel(fluid),LightTexture.block(light)),LightTexture.sky(light));
            // Only the union's outer faces are drawn, including partial cells at each liquid level.
            for(int y=-4;y<4;y++)for(int x=-4;x<4;x++)for(int z=-4;z<4;z++){
                double yy=centerY+y*unit,lo=Math.max(base,yy),hi=Math.min(top,yy+unit);if(hi<=lo)continue;
                if(!panelCell(x,y,z))continue;
                var vertices=points(x*unit,lo,centerZ+z*unit,(x+1)*unit,hi,centerZ+(z+1)*unit);
                int[][] neighbors={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
                for(int side=0;side<6;side++){
                    var n=neighbors[side];if(panelCell(x+n[0],y+n[1],z+n[2])&&!(side==4&&hi==top)&&!(side==5&&lo==base))continue;
                    var f=FACES[side];quad(pose,buffers,sprite,vertices[f[0]],vertices[f[1]],vertices[f[2]],vertices[f[3]],color,packed,overlay,16);
                }
            }
            base=top;
        }
    }
    private static boolean panelCell(int x,int y,int z){return (x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5)<=4.1*4.1;}
    private static void liquid(PoseStack pose,MultiBufferSource buffers,TextureAtlasSprite sprite,double low,double high,int color,int light,int overlay,boolean bottom){
        if(high-low<.000001)return;
        // Clip the authored voxel cavity, so layers cannot cut across the stepped crystal walls.
        for(var face:LIQUID_SHELL){
            float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;for(var p:face){min=Math.min(min,p[1]);max=Math.max(max,p[1]);}
            if(max<=low||min>=high)continue;
            float[][] clipped=new float[4][3];for(int i=0;i<4;i++)clipped[i]=new float[]{face[i][0],(float)Math.clamp(face[i][1],low,high),face[i][2]};
            quad(pose,buffers,sprite,clipped[0],clipped[1],clipped[2],clipped[3],color,light,overlay,16);
        }
        liquidCap(pose,buffers,sprite,high,true,color,light,overlay);
        if(bottom)liquidCap(pose,buffers,sprite,low,false,color,light,overlay);
    }
    private static void liquidCap(PoseStack pose,MultiBufferSource buffers,TextureAtlasSprite sprite,double height,boolean top,int color,int light,int overlay){
        int layer=Math.clamp((int)Math.floor((height-ReactionPoolGeometry.VESSEL_FLOOR)*16+(top?-.00001:.00001)),0,LIQUID_PROFILES.size()-1);
        for(var row:LIQUID_PROFILES.get(layer)){
            // Anchor every strip and layer to one X/Z atlas grid; winding must not rotate the animation.
            var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);var f=FACES[top?4:5];
            for(int tx=(int)Math.floor(row[0]);tx<Math.ceil(row[2]);tx++)for(int tz=(int)Math.floor(row[1]);tz<Math.ceil(row[3]);tz++){
                var v=points(Math.max(row[0],tx),height,Math.max(row[1],tz),Math.min(row[2],tx+1),height,Math.min(row[3],tz+1));
                for(int index:f){var p=v[index];out.addVertex(pose.last(),p[0],p[1],p[2]).setColor(color)
                        .setUv(sprite.getU(p[0]-tx),sprite.getV(p[2]-tz)).setOverlay(overlay).setLight(light).setNormal(pose.last(),0,top?1:-1,0);}
            }
        }
    }
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static float[][] points(double x,double y,double z,double xx,double yy,double zz){return new float[][]{{(float)x,(float)y,(float)z},{(float)xx,(float)y,(float)z},{(float)xx,(float)yy,(float)z},{(float)x,(float)yy,(float)z},{(float)x,(float)y,(float)zz},{(float)xx,(float)y,(float)zz},{(float)xx,(float)yy,(float)zz},{(float)x,(float)yy,(float)zz}};}
    private static void cube(VertexConsumer out,PoseStack pose,float x,float y,float z,float r,int color){com.mcmagic.omnira.client.renderer.StellarVisuals.cube(pose,out,x,y,z,r,color);}
    private static void box(PoseStack pose,MultiBufferSource buffers,TextureAtlasSprite sprite,double x,double y,double z,double xx,double yy,double zz,int color,int light,int overlay){var v=points(x,y,z,xx,yy,zz);for(var f:FACES)quad(pose,buffers,sprite,v[f[0]],v[f[1]],v[f[2]],v[f[3]],color,light,overlay,16);}
    private static void quad(PoseStack pose,MultiBufferSource buffers,TextureAtlasSprite sprite,float[] a,float[] b,float[] c,float[] d,int color,int light,int overlay,int pixels){
        var normal=new org.joml.Vector3f(b[0]-a[0],b[1]-a[1],b[2]-a[2]).cross(new org.joml.Vector3f(d[0]-a[0],d[1]-a[1],d[2]-a[2])).normalize();
        float width=new org.joml.Vector3f(d[0]-a[0],d[1]-a[1],d[2]-a[2]).length()*16/pixels,height=new org.joml.Vector3f(b[0]-a[0],b[1]-a[1],b[2]-a[2]).length()*16/pixels;
        int nx=Math.max(1,(int)Math.ceil(width)),ny=Math.max(1,(int)Math.ceil(height));var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(var uv:new float[][]{{x/width,y/height},{x/width,Math.min(y+1,height)/height},{Math.min(x+1,width)/width,Math.min(y+1,height)/height},{Math.min(x+1,width)/width,y/height}}){
            float u=uv[0],v=uv[1];float px=a[0]*(1-u)*(1-v)+b[0]*(1-u)*v+c[0]*u*v+d[0]*u*(1-v),py=a[1]*(1-u)*(1-v)+b[1]*(1-u)*v+c[1]*u*v+d[1]*u*(1-v),pz=a[2]*(1-u)*(1-v)+b[2]*(1-u)*v+c[2]*u*v+d[2]*u*(1-v);
            out.addVertex(pose.last(),px,py,pz).setColor(color).setUv(sprite.getU(u*width-x),sprite.getV(v*height-y)).setOverlay(overlay).setLight(light).setNormal(pose.last(),normal.x,normal.y,normal.z);
        }
    }
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){private BlockEntityWithoutLevelRenderer renderer;
        public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.translate(.5,.15,.5);pose.scale(.31F,.31F,.31F);pose.translate(-.5,0,-.5);draw(null,0,pose,buffers,light,overlay);pose.popPose();}};return renderer;}
    },ModItems.REACTION_POOL.get());}
    private ReactionPoolClient(){}
}
