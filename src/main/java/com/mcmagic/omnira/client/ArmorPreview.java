package com.mcmagic.omnira.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/** Offline development-only contact sheet of the actual model geometry and UVs. */
public final class ArmorPreview {
    private record Face(float[] vertices,BufferedImage texture,int tint) {}
    public static void main(String[] args) throws Exception {
        var assets=Path.of(args[0]);var output=Path.of(args[1]);
        Files.createDirectories(output.getParent());
        var sheet=new BufferedImage(1920,640,BufferedImage.TYPE_INT_ARGB);
        var g=sheet.createGraphics();g.setColor(new java.awt.Color(0x33434D));g.fillRect(0,0,1920,640);
        var faceClass=Class.forName("com.mcmagic.omnira.client.renderer.CruiseOrbRenderer$GeometryVerification$Face");
        var constructor=faceClass.getDeclaredConstructors()[0];constructor.setAccessible(true);
        var verifier=Class.forName("com.mcmagic.omnira.client.renderer.CruiseOrbRenderer$GeometryVerification");
        var raster=verifier.getDeclaredMethod("raster",BufferedImage.class,BufferedImage.class,faceClass,int.class,int.class,int.class,int.class);raster.setAccessible(true);
        var skin=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);
        var sg=skin.createGraphics();sg.setColor(new java.awt.Color(0x87949E));sg.fillRect(0,0,64,32);sg.dispose();
        for(int row=0;row<2;row++)for(int view=0;view<6;view++){
            var faces=new ArrayList<Face>();
            var pose=new PoseStack();pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(view==0 || view==5?205:view==3?25:90));
            pose.translate(0,.75,0);pose.scale(1,-1,1);
            var base=new HumanoidModel<LivingEntity>(LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0),0),64,32).bakeRoot());
            configure(base,view);capture(base,pose,faces,skin);
            for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                HumanoidModel<LivingEntity> model=row==0?new CrystalPlateModel(slot):new CrystalDressModel(slot);
                configure(model,view);model.setAllVisible(false);
                switch(slot){
                    case HEAD -> {model.head.visible=true;model.hat.visible=true;}
                    case CHEST -> {model.body.visible=true;model.leftArm.visible=true;model.rightArm.visible=true;}
                    case LEGS -> {model.body.visible=true;model.leftLeg.visible=true;model.rightLeg.visible=true;}
                    case FEET -> {model.leftLeg.visible=true;model.rightLeg.visible=true;}
                    default -> {}
                }
                model.riding=view==2 || view==5;
                String name=row==0?"crystal_armor":"crystal_dress";
                var texture=ImageIO.read(assets.resolve("textures/models/armor/"+name+"_layer_"+(slot==EquipmentSlot.LEGS?2:1)+".png").toFile());
                capture(model,pose,faces,texture);
            }
            faces.sort(Comparator.comparingDouble(f->f.vertices[2]+f.vertices[7]+f.vertices[12]+f.vertices[17]));
            for(var face:faces)raster.invoke(null,sheet,face.texture,constructor.newInstance(face.vertices,null,face.tint),view*320,row*320,320,320);
            g.setColor(java.awt.Color.WHITE);g.drawString((row==0?"Plate":"Dress")+" / "+new String[]{"Standing","Walking","Seated side","Rear","Crouched","Seated front"}[view],view*320+12,row*320+20);
        }
        g.dispose();ImageIO.write(sheet,"PNG",output.toFile());
        var itemSheet=new BufferedImage(1280,480,BufferedImage.TYPE_INT_ARGB);
        var ig=itemSheet.createGraphics();ig.setColor(new java.awt.Color(0x33434D));ig.fillRect(0,0,1280,480);
        String[][] icons={{"crystal_helmet","crystal_chestplate","crystal_leggings","crystal_boots"},
                {"crystal_sunhat","crystal_blouse","crystal_skirt","crystal_stocking_shoes"}};
        ig.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(int row=0;row<2;row++)for(int col=0;col<4;col++){
            var icon=ImageIO.read(assets.resolve("textures/item/"+icons[row][col]+".png").toFile());
            ig.drawImage(icon,col*320+64,row*240+24,192,192,null);
        }
        ig.dispose();ImageIO.write(itemSheet,"PNG",output.resolveSibling("armor-items-preview.png").toFile());
        System.out.println("PASS: actual armor meshes, all four slots, standing/walking/seated UV preview: "+output);
        var broomSheet=new BufferedImage(1200,480,BufferedImage.TYPE_INT_ARGB);
        var bg=broomSheet.createGraphics();bg.setColor(new java.awt.Color(0x33434D));bg.fillRect(0,0,1200,480);
        var broomTexture=ImageIO.read(assets.resolve("textures/entity/crystal_broom.png").toFile());
        for(int view=0;view<3;view++){
            var faces=new ArrayList<Face>();var pose=new PoseStack();
            pose.scale(2,2,2);pose.translate(0,-.25,0);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(view==0?90:view==1?45:0));
            if(view==2)pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(75));
            pose.translate(0,0,.25);
            capturePart(com.mcmagic.omnira.client.renderer.CrystalBroomRenderer.model(),pose,faces,broomTexture);
            faces.sort(Comparator.comparingDouble(f->f.vertices[2]+f.vertices[7]+f.vertices[12]+f.vertices[17]));
            for(var face:faces)raster.invoke(null,broomSheet,face.texture,constructor.newInstance(face.vertices,null,face.tint),view*400,0,400,480);
            bg.setColor(java.awt.Color.WHITE);bg.drawString(new String[]{"Broom / Side","Broom / Oblique","Broom / Top"}[view],view*400+12,24);
        }
        bg.dispose();ImageIO.write(broomSheet,"PNG",output.resolveSibling("crystal-broom-preview.png").toFile());
    }
    private static void configure(HumanoidModel<?> m,int view){
        m.young=false;
        if(view==1){m.leftLeg.xRot=.5F;m.rightLeg.xRot=-.5F;m.leftArm.xRot=-.4F;m.rightArm.xRot=.4F;}
        if(view==2 || view==5){m.leftLeg.xRot=m.rightLeg.xRot=-1.4137F;m.leftLeg.yRot=-.314F;m.rightLeg.yRot=.314F;}
        if(view==4){m.body.xRot=.5F;m.body.y=3.2F;m.head.y=4.2F;m.leftLeg.z=m.rightLeg.z=4;m.leftLeg.y=m.rightLeg.y=12.2F;m.leftArm.y=m.rightArm.y=5.2F;}
    }
    private static void capture(HumanoidModel<?> model,PoseStack pose,List<Face> output,BufferedImage texture){
        captureVertices(c->model.renderToBuffer(pose,c,0,0,-1),output,texture);
    }
    private static void capturePart(net.minecraft.client.model.geom.ModelPart model,PoseStack pose,List<Face> output,BufferedImage texture){
        captureVertices(c->model.render(pose,c,0,0),output,texture);
    }
    private static void captureVertices(java.util.function.Consumer<VertexConsumer> render,List<Face> output,BufferedImage texture){
        class Collector implements VertexConsumer {
            float[] quad=new float[20];int count,tint=-1;
            void flush(){if(count==4){output.add(new Face(quad,texture,tint));quad=new float[20];count=0;}}
            public VertexConsumer addVertex(float x,float y,float z){flush();int i=count++*5;quad[i]=x;quad[i+1]=y;quad[i+2]=z;return this;}
            public VertexConsumer setUv(float u,float v){
                if(u<0 || v<0 || u>1 || v>1)throw new AssertionError("Armor UV outside atlas");
                quad[(count-1)*5+3]=u;quad[(count-1)*5+4]=v;return this;
            }
            public VertexConsumer setColor(int r,int g,int b,int a){return this;}
            public VertexConsumer setUv1(int u,int v){return this;}
            public VertexConsumer setUv2(int u,int v){return this;}
            public VertexConsumer setNormal(float x,float y,float z){int shade=(int)(255*(.65+.2*Math.abs(y)+.35*Math.abs(z)));shade=Math.min(255,shade);tint=0xFF000000|shade<<16|shade<<8|shade;return this;}
        }
        var collector=new Collector();render.accept(collector);collector.flush();
    }
}
