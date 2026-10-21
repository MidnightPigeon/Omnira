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
        var sheet=new BufferedImage(2560,640,BufferedImage.TYPE_INT_ARGB);
        var g=sheet.createGraphics();g.setColor(new java.awt.Color(0x33434D));g.fillRect(0,0,2560,640);
        var faceClass=Class.forName("com.mcmagic.omnira.client.renderer.CruiseOrbRenderer$GeometryVerification$Face");
        var constructor=faceClass.getDeclaredConstructors()[0];constructor.setAccessible(true);
        var verifier=Class.forName("com.mcmagic.omnira.client.renderer.CruiseOrbRenderer$GeometryVerification");
        var raster=verifier.getDeclaredMethod("raster",BufferedImage.class,BufferedImage.class,faceClass,int.class,int.class,int.class,int.class);raster.setAccessible(true);
        var skin=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);
        var sg=skin.createGraphics();sg.setColor(new java.awt.Color(0x87949E));sg.fillRect(0,0,64,32);sg.dispose();
        for(int row=0;row<2;row++)for(int view=0;view<8;view++){
            var faces=new ArrayList<Face>();
            var pose=new PoseStack();pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(view==0?180:view==2?90:view==3?0:205));
            pose.translate(0,.75,0);pose.scale(1,-1,1);
            var base=new HumanoidModel<LivingEntity>(LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0),0),64,32).bakeRoot());
            configure(base,view);capture(base,pose,faces,skin);
            int skinFaces=faces.size();
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
                model.riding=view==5;
                String name=row==0?"crystal_armor":"crystal_dress";
                var texture=ImageIO.read(assets.resolve("textures/models/armor/"+name+"_layer_"+(slot==EquipmentSlot.LEGS?2:1)+".png").toFile());
                try{capture(model,pose,faces,texture);}catch(AssertionError failure){throw new AssertionError(slot+" / view "+view+": "+failure.getMessage(),failure);}
            }
            if(row==0){
                var armor=faces.subList(skinFaces,faces.size());
                for(int i=0;i<armor.size();i++){
                    for(int j=i+1;j<armor.size();j++)if(overlap(armor.get(i),armor.get(j)))throw new AssertionError("Coplanar armor overlap in view "+view+": "+i+" / "+j+" "+Arrays.toString(armor.get(i).vertices)+" / "+Arrays.toString(armor.get(j).vertices));
                    for(var bodyFace:faces.subList(0,skinFaces))if(overlap(armor.get(i),bodyFace))throw new AssertionError("Armor touches skin plane in view "+view+": "+i);
                }
            }
            faces.sort(Comparator.comparingDouble(f->f.vertices[2]+f.vertices[7]+f.vertices[12]+f.vertices[17]));
            for(var face:faces)raster.invoke(null,sheet,face.texture,constructor.newInstance(face.vertices,null,face.tint),view*320,row*320,320,320);
            g.setColor(java.awt.Color.WHITE);g.drawString((row==0?"Plate":"Dress")+" / "+new String[]{"Front","Oblique","Side","Rear","Walking","Seated","Crouched","Raised arms"}[view],view*320+12,row*320+20);
        }
        g.dispose();ImageIO.write(sheet,"PNG",output.toFile());
        var itemSheet=new BufferedImage(1280,480,BufferedImage.TYPE_INT_ARGB);
        var ig=itemSheet.createGraphics();ig.setColor(new java.awt.Color(0x33434D));ig.fillRect(0,0,1280,480);
        String[][] icons={{"crystal_helmet","crystal_chestplate","crystal_leggings","crystal_boots"},
                {"crystal_sunhat","crystal_top","crystal_skirt","crystal_stocking_shoes"}};
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
        if(view==4){m.leftLeg.xRot=.5F;m.rightLeg.xRot=-.5F;m.leftArm.xRot=-.4F;m.rightArm.xRot=.4F;}
        if(view==5){m.leftLeg.xRot=m.rightLeg.xRot=-1.4137F;m.leftLeg.yRot=-.314F;m.rightLeg.yRot=.314F;}
        if(view==6){m.body.xRot=.5F;m.body.y=3.2F;m.head.y=4.2F;m.leftLeg.z=m.rightLeg.z=4;m.leftLeg.y=m.rightLeg.y=12.2F;m.leftArm.y=m.rightArm.y=5.2F;}
        if(view==7){m.leftArm.xRot=m.rightArm.xRot=-1.8F;m.leftArm.zRot=-.2F;m.rightArm.zRot=.2F;}
    }
    private static void capture(HumanoidModel<?> model,PoseStack pose,List<Face> output,BufferedImage texture){
        int start=output.size();
        captureVertices(c->model.renderToBuffer(pose,c,0,0,-1),output,texture);
        if(model instanceof CrystalPlateModel){
            var faces=output.subList(start,output.size());
            if(faces.isEmpty()||faces.size()>400)throw new AssertionError("Plate face budget: "+faces.size());
            double area=0,accent=0;var unique=new HashSet<String>();
            for(var face:faces){
                var vertices=face.vertices;var points=new ArrayList<String>();
                for(int i=0;i<4;i++)points.add(String.format(java.util.Locale.ROOT,"%.5f,%.5f,%.5f",vertices[i*5],vertices[i*5+1],vertices[i*5+2]));
                Collections.sort(points);if(!unique.add(String.join(";",points)))throw new AssertionError("Duplicate plate surface: "+points);
                double width=distance(vertices,0,5)*16,height=distance(vertices,5,10)*16;
                if(width<.001||height<.001)throw new AssertionError("Degenerate plate surface");
                for(int i=0;i<4;i++){
                    int a=i*5,b=(i+1)%4*5;
                    double uv=Math.hypot((vertices[a+3]-vertices[b+3])*texture.getWidth(),(vertices[a+4]-vertices[b+4])*texture.getHeight());
                    if(Math.abs(distance(vertices,a,b)*16-uv)>.001)throw new AssertionError("Plate UV density differs across faces");
                }
                double u=0,v=0;for(int i=0;i<4;i++){u+=vertices[i*5+3]/4;v+=vertices[i*5+4]/4;}
                int rgba=texture.getRGB(Math.min(texture.getWidth()-1,(int)(u*texture.getWidth())),Math.min(texture.getHeight()-1,(int)(v*texture.getHeight())));
                area+=width*height;if((rgba&255)-((rgba>>>16)&255)>25)accent+=width*height;
            }
            if(accent/area>.1)throw new AssertionError("Blue decorations dominate plate area");
        }
    }
    private static double distance(float[] vertices,int a,int b){
        double x=vertices[a]-vertices[b],y=vertices[a+1]-vertices[b+1],z=vertices[a+2]-vertices[b+2];return Math.sqrt(x*x+y*y+z*z);
    }
    /** Compare actual rendered polygons, including partial overlaps across slots and moving limbs. */
    private static boolean overlap(Face left,Face right){
        var a=left.vertices;var b=right.vertices;
        double ax=a[5]-a[0],ay=a[6]-a[1],az=a[7]-a[2],bx=a[15]-a[0],by=a[16]-a[1],bz=a[17]-a[2];
        double[] n={ay*bz-az*by,az*bx-ax*bz,ax*by-ay*bx};
        double length=Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);if(length<1e-9)return false;
        for(int k=0;k<3;k++)n[k]/=length;
        for(int i=0;i<4;i++)if(Math.abs(n[0]*(b[i*5]-a[0])+n[1]*(b[i*5+1]-a[1])+n[2]*(b[i*5+2]-a[2]))>1e-6)return false;
        int axis=Math.abs(n[0])>Math.abs(n[1])?0:1;if(Math.abs(n[2])>Math.abs(n[axis]))axis=2;
        int u=(axis+1)%3,v=(axis+2)%3;
        var polygon=new ArrayList<double[]>();var clip=new ArrayList<double[]>();
        for(int i=0;i<4;i++){polygon.add(new double[]{a[i*5+u],a[i*5+v]});clip.add(new double[]{b[i*5+u],b[i*5+v]});}
        double sign=Math.signum(area(clip));
        for(int edge=0;edge<4&&!polygon.isEmpty();edge++){
            var start=clip.get(edge);var end=clip.get((edge+1)%4);var next=new ArrayList<double[]>();
            var previous=polygon.getLast();double old=side(start,end,previous)*sign;
            for(var point:polygon){
                double now=side(start,end,point)*sign;
                if((old>=0)!=(now>=0)){
                    double t=old/(old-now);next.add(new double[]{previous[0]+t*(point[0]-previous[0]),previous[1]+t*(point[1]-previous[1])});
                }
                if(now>=0)next.add(point);previous=point;old=now;
            }
            polygon=next;
        }
        return Math.abs(area(polygon))>1e-7;
    }
    private static double side(double[] a,double[] b,double[] p){return (b[0]-a[0])*(p[1]-a[1])-(b[1]-a[1])*(p[0]-a[0]);}
    private static double area(List<double[]> polygon){
        double area=0;for(int i=0;i<polygon.size();i++){var a=polygon.get(i);var b=polygon.get((i+1)%polygon.size());area+=a[0]*b[1]-a[1]*b[0];}return area/2;
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
