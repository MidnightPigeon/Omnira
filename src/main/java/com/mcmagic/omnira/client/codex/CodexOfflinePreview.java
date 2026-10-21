package com.mcmagic.omnira.client.codex;

import com.mcmagic.omnira.client.MiniatureCodexScene;
import com.mcmagic.omnira.spell.visual.SpellVolumeGeometry;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Standalone asset/pose check. Compile with Gson and MiniatureCodexScene; no game startup. */
public final class CodexOfflinePreview {
    private record Painted(float[][] vertices,int color,boolean unlit) {
        Painted(float[][] vertices,int color){this(vertices,color,false);}
    }
    private record Polygon(int[] x,int[] y,float depth,int color) {}
    public static void main(String[] args) throws Exception {
        Path resources=Path.of(args.length>0?args[0]:"src/main/resources");
        Path output=Path.of(args.length>1?args[1]:"build/previews/codex-heart.png");
        boolean verify=args.length<3||!args[2].equals("--preview-only");
        var base=read(resources,"block/myriad_codex");var book=read(resources,"item/myriad_codex_book");var heart=CodexGeometry.heart();
        var port=read(resources,"block/mana_engine_shaft");
        if(verify){
        for(int step=0;step<=500;step++){
            var boxes=MiniatureCodexScene.boxes(step/100F,80);
            require(boxes.size()<400,"Miniature scene exceeds geometry budget");
            var names=new HashSet<String>();
            for(var b:boxes){
                require(names.add(b.id()),"Duplicate miniature component");
                require(b.width()>0&&b.height()>0&&b.depth()>0&&Float.isFinite(b.y()),"Degenerate miniature box");
                require(b.x()-b.width()/2>.3&&b.x()+b.width()/2<4.7&&b.z()-b.depth()/2>.3&&b.z()+b.depth()/2<4.7&&b.y()-b.height()/2>.3&&b.y()+b.height()/2<4,"Miniature escaped frame");
            }
            require(names.contains("ground_2_2")&&names.contains("house_door")&&names.contains("hill_2"),"Lost world anchor");
            require((step>448)==names.contains("pedestal_foot"),"Pedestal timing changed");
        }
        var white=ImageIO.read(resources.resolve("assets/omnira/textures/block/myriad_codex_solid.png").toFile());
        require(white!=null,"Missing white texture");
        for(int x=0;x<white.getWidth();x++)for(int y=0;y<white.getHeight();y++)require(white.getRGB(x,y)==0xFFFFFFFF,"Palette texture is not opaque white");
        require(heart.size()>160&&heart.size()<400&&heart==CodexGeometry.heart(),"Pixel heart missing or rebuilt per draw");
        var surfaces=new HashSet<String>();
        for(var face:heart){
            require(Math.abs(face.nx())+Math.abs(face.ny())+Math.abs(face.nz())==1,"Non-pixel diagonal heart face");
            var points=new ArrayList<String>();
            for(var p:face.vertices()){
                for(float n:p)require(Math.abs(n*16-Math.round(n*16))<.0001,"Heart vertex off pixel grid");
                points.add(Arrays.toString(p));
            }
            Collections.sort(points);require(surfaces.add(points.toString()),"Buried or duplicate heart face");
        }
        require(CodexGeometry.particle().size()==6,"Link particle missing a side");
        require(SpellVolumeGeometry.ball()==SpellVolumeGeometry.ball()&&SpellVolumeGeometry.ring()==SpellVolumeGeometry.ring(),"Burst geometry rebuilt");
        int legacyRing=0,legacyBall=0;
        for(int x=-4;x<4;x++)for(int z=-4;z<4;z++){double r=(x+.5)*(x+.5)+(z+.5)*(z+.5);if(r>=9&&r<=16)legacyRing++;}
        int[][] directions={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for(int x=-2;x<2;x++)for(int y=-2;y<2;y++)for(int z=-2;z<2;z++)if(voxel(x,y,z))for(var d:directions)if(!voxel(x+d[0],y+d[1],z+d[2]))legacyBall++;
        require(SpellVolumeGeometry.ring().size()==legacyRing&&SpellVolumeGeometry.ball().size()==legacyBall,"Burst source masks changed");
        int thinRing=0;
        for(int x=-8;x<8;x++)for(int z=-8;z<8;z++){double r=(x+.5)*(x+.5)+(z+.5)*(z+.5);if(r>=49&&r<=64)thinRing++;}
        require(SpellVolumeGeometry.thinRing().size()==thinRing,"Thin preview annulus mask changed");
        for(double length:new double[]{.001,.2,1,7,32,1000}){
            int samples=CodexGeometry.linkSamples(length);
            require(samples>0&&samples<=128,"Unbounded link particle count");
            for(int i=0;i<samples;i++){
                double f=CodexGeometry.linkFraction(i,samples);
                require(f>0&&f<1,"Link escaped its endpoints");
                for(int t=0;t<1200;t+=5){int alpha=CodexGeometry.linkAlpha(76,t,i);require(alpha>=45&&alpha<=76,"Invalid link pulse alpha");}
            }
        }
        for(var left:book)if(left.tint()==2)for(var p:left.vertices()){
            boolean reflected=false;
            for(var right:book)if(right.tint()==2)for(var q:right.vertices())
                if(Math.abs(p[0]+q[0]-1)<.0001&&Math.abs(p[1]-q[1])<.0001&&Math.abs(p[2]-q[2])<.0001)reflected=true;
            require(reflected,"Book trim has no mirrored counterpart");
        }
        for(var model:List.of(base,book,heart))for(var f:model){
            var p=f.vertices();float[] u=sub(p[1],p[0]),v=sub(p[2],p[0]);
            require(length(cross(u,v))>1e-6,"Degenerate asset face");
            require(f.tint()>=0&&f.tint()<=10,"Invalid material tint");
            for(var point:p)for(float coordinate:point)require(Float.isFinite(coordinate)&&coordinate>-.1&&coordinate<1.1,"Unbounded model vertex");
        }
        for(int t=0;t<=1200;t+=5)for(boolean busy:new boolean[]{false,true}){
            for(var f:book)for(var p:f.vertices()){
                var point=CodexGeometry.onBook(p[0],p[1],p[2],t,busy);
                require(point.x()>0&&point.x()<1&&point.y()>.68&&point.y()<1.3&&point.z()>0&&point.z()<1,"Animated book escaped floating envelope");
            }
            for(var b:MiniatureCodexScene.boxes(CodexGeometry.evolution(t,busy),t)){
                var point=CodexGeometry.onBook(.5F+(b.x()-2.5F)*.11F,.16F+b.y()*.11F,.5F+(b.z()-2.5F)*.11F,t,busy);
                require(point.x()>.25&&point.x()<.75&&point.z()>.25&&point.z()<.75&&point.y()<1.5,"Tiny world escaped page envelope");
            }
        }
        double low=1,high=0;
        for(int t=0;t<=240;t++){low=Math.min(low,CodexGeometry.bookY(t,false));high=Math.max(high,CodexGeometry.bookY(t,false));}
        require(high-low>.065&&high<.87,"Idle book bob is missing or too high");
        require(CodexGeometry.onBook(.5F,0,0,0,false).y()<CodexGeometry.onBook(.5F,0,1,0,false).y(),"Book does not lean toward its front");
        for(int fade=0;fade<=10;fade++)require(Math.abs(CodexGeometry.opacity(fade)-(1-fade/10F))<.0001,"Fade steps disagree with server");
        for(int diameter=7;diameter<=15;diameter+=2){
            float radius=(diameter-1)/2F;var range=CodexGeometry.range(diameter);
            require(range.size()==legacyBall+3*thinRing,"Codex does not use burst shell and three thin pixel annuli");
            int rings=0;
            for(var face:range){
                if(face.ring())rings++;
                var f=face.geometry();require(Math.abs(f.nx())+Math.abs(f.ny())+Math.abs(f.nz())==1,"Smooth/sloped range face remains");
                for(var p:f.vertices())for(float coordinate:p){
                    float unit=(coordinate-.5F)/radius;
                    require(Math.abs(unit)<=1&&Math.abs(unit*8-Math.round(unit*8))<.0001,"Range lost pixel scale/bounds");
                }
            }
            require(rings==3*thinRing,"Missing thin pixel annulus");
        }
        for(int i=0;i<33;i++){
            var cached=CodexGeometry.world(i/8F);
            require(cached==CodexGeometry.world(i/8F),"World mesh cache missed on repeated draw");
            for(var face:cached.faces()){
                var f=face.geometry();
                require(Math.abs(f.nx()*f.nx()+f.ny()*f.ny()+f.nz()*f.nz()-1)<.0001,"Invalid cached normal");
            }
        }
        for(int diameter=7;diameter<=15;diameter+=2)require(CodexGeometry.range(diameter)==CodexGeometry.range(diameter),"Range mesh rebuilt per draw");
        }
        var image=new BufferedImage(1600,860,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();
        graphics.setColor(new Color(0x20272C));graphics.fillRect(0,0,image.getWidth(),image.getHeight());
        String[] titles={"CODEX / IDLE","CODEX / HEART HOME","CODEX / BUSY","REAR INPUT SOCKET","PIXEL HEART / FRONT","PIXEL HEART / REAR","HEART / FADE 9","RANGE + DOTTED LINK / D15"};
        for(int cell=0;cell<8;cell++){
            double time=cell==0?0:cell==2?50:200;boolean busy=cell==2;var geometry=new ArrayList<Painted>();
            if(cell<4){
                add(geometry,base,time,busy,false);
                // Native port geometry; palette approximation only, runtime uses the actual faceted atlas.
                for(var face:port){var points=new float[4][3];for(int i=0;i<4;i++){var p=face.vertices()[i];points[i]=new float[]{1-p[0],p[1],1-p[2]};}geometry.add(new Painted(points,0xA8B7DBEA));}
                add(geometry,book,time,busy,true);
                for(var face:CodexGeometry.world(CodexGeometry.evolution(time,busy)).faces())geometry.add(miniatureFace(face,time,busy));
                if(cell==1)for(var face:heart){var points=rotateHeart(face.vertices(),time);for(var p:points){p[0]=.5F+(p[0]-.5F)*.42F;p[1]=(float)CodexGeometry.homeHeartY()+(p[1]-.5F)*.42F;p[2]=.5F+(p[2]-.5F)*.42F;}geometry.add(new Painted(points,CodexGeometry.color(face.tint(),time,false)));}
            }else{
                int fade=cell==6?9:0;
                for(var face:heart){int c=CodexGeometry.color(face.tint(),time,false);c=(c&0xFFFFFF)|(Math.round(255*CodexGeometry.opacity(fade))<<24);geometry.add(new Painted(rotateHeart(face.vertices(),time),c));}
                if(cell==7){
                    for(var face:CodexGeometry.range(15))geometry.add(new Painted(face.geometry().vertices(),
                            (face.ring()?CodexGeometry.RANGE_RING_ALPHA:CodexGeometry.RANGE_SHELL_ALPHA)<<24|0xA7E4DE,true));
                    int count=CodexGeometry.linkSamples(6.5);
                    for(int i=0;i<count;i++){
                        float f=(float)CodexGeometry.linkFraction(i,count),x=-6+6.5F*f,y=.5F,z=.5F;
                        for(var face:CodexGeometry.particle()){
                            var points=new float[4][3];for(int v=0;v<4;v++){var p=face.vertices()[v];points[v]=new float[]{x+(p[0]-.5F)*.045F,y+(p[1]-.5F)*.045F,z+(p[2]-.5F)*.045F};}
                            geometry.add(new Painted(points,CodexGeometry.linkAlpha(76,time,i)<<24|0xC9E5DF));
                        }
                    }
                }
            }
            double angle=cell==3||cell==5?.7:2.8;float scale=cell<4?220:cell==7?23:490;
            draw(graphics,geometry,cell,angle,scale);
            graphics.setColor(new Color(0xE3EBEB));graphics.setFont(new Font(Font.MONOSPACED,Font.PLAIN,16));graphics.drawString(titles[cell],cell%4*400+18,cell/4*430+30);
        }
        graphics.dispose();Files.createDirectories(output.toAbsolutePath().getParent());ImageIO.write(image,"png",output.toFile());
        rangePreview(output.resolveSibling("codex-range.png"));
        miniaturePreview(output.resolveSibling("myriad-codex.png"));
        System.out.println((verify?"Codex assets: "+heart.size()+" pixel-grid exterior faces, six-sided bounded link dots, 482 idle/busy poses, range radii and 11 fade steps passed. ":"Preview generated; checks deferred. ")+output.toAbsolutePath());
    }
    private static void miniaturePreview(Path output)throws IOException{
        var image=new BufferedImage(1600,860,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(new Color(0x283642));g.fillRect(0,0,1600,860);
        float[] stages={0,1,2,3,4,4.4F,4.7F,4.9F};
        String[] labels={"WORLD","MATTER","FORM","TIME","LIFE","WORLD ON PAGES","CRYSTAL PEDESTAL","LIFT BEFORE RESULT"};
        int[][] ids={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
        for(int cell=0;cell<stages.length;cell++){
            var geometry=new ArrayList<Painted>();var boxes=MiniatureCodexScene.boxes(stages[cell],80);
            for(var b:boxes)for(int side=0;side<6;side++){
                var spans=side==2||side==3?MiniatureCodexScene.sideSpans(b,boxes,side==2?-1:1):List.of(new MiniatureCodexScene.Span(b.y()-b.height()/2,b.y()+b.height()/2));
                for(var span:spans){
                    float x=b.x()-2,z=b.z()-2,w=b.width()/2,d=b.depth()/2,lo=span.bottom()-.4F,hi=span.top()-.4F;
                    float[][] vertices={{x-w,lo,z-d},{x+w,lo,z-d},{x+w,hi,z-d},{x-w,hi,z-d},{x-w,lo,z+d},{x+w,lo,z+d},{x+w,hi,z+d},{x-w,hi,z+d}};
                    var f=ids[side];geometry.add(new Painted(new float[][]{vertices[f[0]],vertices[f[1]],vertices[f[2]],vertices[f[3]]},b.color()));
                }
            }
            draw(g,geometry,cell,cell==6?2.2:.65,83);
            g.setColor(new Color(0xE3EBEB));g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,16));g.drawString(labels[cell],cell%4*400+18,cell/4*430+30);
        }
        g.dispose();ImageIO.write(image,"png",output.toFile());
    }
    private static void rangePreview(Path output)throws IOException{
        var image=new BufferedImage(1600,520,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        for(int cell=0;cell<4;cell++){
            int diameter=cell<2?7:15;var geometry=new ArrayList<Painted>();
            g.setColor(new Color(0xA1C5FA));g.fillRect(cell*400,0,400,260);
            g.setColor(new Color(0x5F773B));g.fillRect(cell*400,260,400,260);
            for(var face:CodexGeometry.range(diameter))geometry.add(new Painted(face.geometry().vertices(),
                    (face.ring()?CodexGeometry.RANGE_RING_ALPHA:CodexGeometry.RANGE_SHELL_ALPHA)<<24|0xA7E4DE,true));
            draw(g,geometry,cell,cell%2==0?.3:1.1,diameter==7?44:19);
            g.setColor(new Color(0x263C49));g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
            g.drawString("BURST PIXEL RANGE / DIAMETER "+diameter,cell*400+15,28);
        }
        g.dispose();ImageIO.write(image,"png",output.toFile());
    }
    private static Painted miniatureFace(CodexGeometry.PaintedFace face,double time,boolean busy){
        var f=face.geometry();
        var points=new float[4][3];for(int i=0;i<4;i++){
            var p=f.vertices()[i];var q=CodexGeometry.onBook(.5F+(p[0]-2.5F)*.11F,.16F+p[1]*.11F,.5F+(p[2]-2.5F)*.11F,time,busy);points[i]=new float[]{q.x(),q.y(),q.z()};
        }return new Painted(points,face.color());
    }
    private static List<CodexGeometry.Face> read(Path resources,String path) throws IOException {
        try(var reader=Files.newBufferedReader(resources.resolve("assets/omnira/models/"+path+".json"))){return CodexGeometry.model(reader);}
    }
    private static void add(List<Painted> target,List<CodexGeometry.Face> faces,double time,boolean busy,boolean book){
        for(var face:faces){var points=new float[4][3];for(int i=0;i<4;i++){
            var p=face.vertices()[i];var q=book?CodexGeometry.onBook(p[0],p[1],p[2],time,busy):new CodexGeometry.Point(p[0],p[1],p[2]);points[i]=new float[]{q.x(),q.y(),q.z()};
        }target.add(new Painted(points,CodexGeometry.color(face.tint(),time,busy)));}
    }
    private static float[][] rotateHeart(float[][] vertices,double time){
        double a=Math.toRadians(time*.7%360),pulse=1+Math.sin(time*.06)*.025;var result=new float[4][3];
        for(int i=0;i<4;i++){double x=(vertices[i][0]-.5)*pulse,z=(vertices[i][2]-.5)*pulse;result[i]=new float[]{.5F+(float)(x*Math.cos(a)+z*Math.sin(a)),.5F+(float)((vertices[i][1]-.5)*pulse),.5F+(float)(z*Math.cos(a)-x*Math.sin(a))};}return result;
    }
    private static void draw(Graphics2D g,List<Painted> faces,int cell,double angle,float scale){
        var polygons=new ArrayList<Polygon>();
        for(var face:faces){int[] xs=new int[4],ys=new int[4];float depth=0;
            for(int i=0;i<4;i++){var p=project(face.vertices()[i],cell,angle,scale);
                xs[i]=(int)p[0];ys[i]=(int)p[1];depth+=p[2]/4;
            }
            var n=cross(sub(face.vertices()[1],face.vertices()[0]),sub(face.vertices()[2],face.vertices()[0]));float shade=face.unlit()?1:.72F+.28F*Math.max(0,n[1]/length(n));int c=face.color();
            int shaded=c&0xFF000000|((int)((c>>>16&255)*shade)<<16)|((int)((c>>>8&255)*shade)<<8)|(int)((c&255)*shade);
            polygons.add(new Polygon(xs,ys,depth,shaded));
        }
        polygons.sort(Comparator.comparingDouble(Polygon::depth));for(var polygon:polygons){g.setColor(new Color(polygon.color(),true));g.fillPolygon(polygon.x(),polygon.y(),4);}
    }
    private static double[] project(float[] p,int cell,double angle,float scale){
        double x=p[0]-.5,y=p[1]-(cell<4?.55:.5),z=p[2]-.5,rx=x*Math.cos(angle)-z*Math.sin(angle),rz=x*Math.sin(angle)+z*Math.cos(angle),elevation=.48;
        return new double[]{cell%4*400+200+rx*scale,cell/4*430+(cell<4?285:260)+(-y*Math.cos(elevation)+rz*Math.sin(elevation))*scale,rz*Math.cos(elevation)+y*Math.sin(elevation)};
    }
    private static float[] sub(float[] a,float[] b){return new float[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};}
    private static boolean voxel(int x,int y,int z){return (x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5)<=3;}
    private static float[] cross(float[] a,float[] b){return new float[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static float length(float[] a){return (float)Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private CodexOfflinePreview(){}
}
