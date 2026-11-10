package com.mcmagic.omnira.client.renderer;

import java.util.*;

/** Shared by the renderer and offline preview; coordinates use 16 model pixels per block. */
public final class WeaverSpiritGeometry {
    public record Face(int material,int group,float[] vertices,float[] uv,int nx,int ny,int nz){}
    public record Gear(int group,int axis,float x,float y,float z,float speed,int radius){}
    public record SeamMote(float x,float y,float z,float size,int color,float alpha){}
    public static final List<Gear> GEARS=List.of(new Gear(1,0,21,-5,0,1.5F,4),new Gear(3,0,21,10,0,-2F,3),
            new Gear(4,2,8,-6,-18,1.2F,3),new Gear(5,2,8,5,18,-1.2F,3));
    public static final List<Face> FACES=build();
    public static final List<List<Face>> DEBRIS=debris();
    public static final List<List<Face>> CRYSTAL_DEBRIS=crystalDebris();
    public static final List<SeamMote> SEAM_MOTES=seamMotes();
    public static final List<Face> SEAM_PIXEL=seamPixel();
    public static final float SEAM_SPEED=.45F;
    public static final float EDGE_FADE_WIDTH=8;
    public static float edgeOpacity(float x){float t=Math.clamp(-x/EDGE_FADE_WIDTH,0,1);return t*t*(3-2*t);}
    private static List<SeamMote> seamMotes(){
        var result=new ArrayList<SeamMote>();
        for(int band=0;band<3;band++)for(int i=0;i<8;i++){
            float x=band*3;double radius=Math.sqrt(23*23-x*x),angle=i*Math.PI/4+band*Math.PI/12;
            result.add(new SeamMote(x,(float)(Math.cos(angle)*radius),(float)(Math.sin(angle)*radius),i%3==0?2:1,
                    band==1?0xE7FCFF:0xBCE5FA,band==1?.65F:.48F));
        }
        return List.copyOf(result);
    }
    private static List<Face> seamPixel(){var faces=new ArrayList<Face>();box(faces,2,0,-.5F,-.5F,-.5F,1,1,1);return List.copyOf(faces);}
    private static boolean inside(int x,int y,int z){return (x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5)<=144;}
    private static List<Face> build(){
        var faces=new ArrayList<Face>();
        // Merge coplanar voxel rows. The two hemispheres never carry overlapping interface faces.
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1})for(int slice=-12;slice<12;slice++){
            int u=(axis+1)%3,v=(axis+2)%3;
            for(int row=-12;row<12;row++){
                int start=-12;
                while(start<12){
                    int material=surface(axis,sign,slice,u,start,v,row);
                    if(material<0){start++;continue;}
                    int end=start+1;
                    while(end<12&&surface(axis,sign,slice,u,end,v,row)==material)end++;
                    plane(faces,material,0,axis,sign,(slice+(sign>0?1:0))*2,start*2,end*2,row*2,(row+1)*2);
                    start=end;
                }
            }
        }
        var mechanism=new ArrayList<Face>();appendMechanism(mechanism);
        // A light-emitting hemisphere shares the mechanical core's cut plane without a second cap.
        for(int x=-6;x<0;x++)for(int y=-6;y<6;y++)for(int z=-6;z<6;z++){
            if(!core(x,y,z))continue;int[] p={x,y,z};
            for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}){
                var q=p.clone();q[axis]+=sign;if(core(q[0],q[1],q[2])||axis==0&&q[0]==0)continue;
                int u=(axis+1)%3,v=(axis+2)%3;
                plane(faces,2,2,axis,sign,(p[axis]+(sign>0?1:0))*2,p[u]*2,(p[u]+1)*2,p[v]*2,(p[v]+1)*2);
            }
        }
        for(var g:GEARS)appendGear(mechanism,g);
        faces.addAll(mergeMechanism(mechanism));
        return List.copyOf(faces);
    }
    private record Sheet(int material,int group,int axis,int sign,int at){}
    private static List<Face> mergeMechanism(List<Face> source){
        var sheets=new LinkedHashMap<Sheet,SortedSet<Integer>>();
        for(var face:source){
            int axis=face.nx()!=0?0:face.ny()!=0?1:2,sign=axis==0?face.nx():axis==1?face.ny():face.nz();
            int u=(axis+1)%3,v=(axis+2)%3;float[] xyz=face.vertices();
            int u0=100,v0=100,u1=-100,v1=-100;
            for(int i=0;i<4;i++){u0=Math.min(u0,(int)xyz[i*3+u]);u1=Math.max(u1,(int)xyz[i*3+u]);v0=Math.min(v0,(int)xyz[i*3+v]);v1=Math.max(v1,(int)xyz[i*3+v]);}
            var cells=sheets.computeIfAbsent(new Sheet(face.material(),face.group(),axis,sign,(int)xyz[axis]),key->new TreeSet<>());
            for(int row=v0;row<v1;row++)for(int col=u0;col<u1;col++)cells.add((row+64)*128+col+64);
        }
        var result=new ArrayList<Face>();
        for(var entry:sheets.entrySet()){
            var s=entry.getKey();var cells=entry.getValue();
            while(!cells.isEmpty()){
                int key=cells.first(),width=1,height=1;
                while(cells.contains(key+width))width++;
                boolean extend=true;
                while(extend){for(int x=0;x<width;x++)if(!cells.contains(key+height*128+x)){extend=false;break;}if(extend)height++;}
                for(int y=0;y<height;y++)for(int x=0;x<width;x++)cells.remove(key+y*128+x);
                int u0=key%128-64,v0=key/128-64;
                plane(result,s.material(),s.group(),s.axis(),s.sign(),s.at(),u0,u0+width,v0,v0+height);
            }
        }
        return result;
    }
    private record Pixel(int x,int y,int z){
        Pixel next(int axis,int sign){return new Pixel(x+(axis==0?sign:0),y+(axis==1?sign:0),z+(axis==2?sign:0));}
        int at(int axis){return axis==0?x:axis==1?y:z;}
    }
    private record Strip(int style,int axis,int sign,int at,int row){}
    private static void appendMechanism(List<Face> faces){
        var pixels=new HashMap<Pixel,Integer>();
        // Open load-bearing frame, short bearing forks and only two small protective plates.
        for(int[] b:new int[][]{{0,-18,-3,2,36,2},{0,-18,1,2,36,2},{3,-16,-14,3,32,3},{4,-14,12,3,30,3},
                {4,15,-13,12,3,3},{4,-16,-13,12,3,3},{5,13,12,11,3,3},{5,-14,12,11,3,3},
                {15,-13,-7,3,29,2},{15,-13,5,3,29,2},{3,18,-4,5,4,8},{3,-22,-4,5,4,8},
                {15,-9,-5,3,2,10},{15,-3,-5,3,2,10},{15,6,-5,3,2,10},{15,12,-5,3,2,10},
                {8,15,-4,6,3,2},{8,15,2,6,3,2},{8,-19,-4,6,3,2},{8,-19,2,6,3,2},{13,11,-3,3,4,6},{13,-15,-3,3,4,6},
                {3,10,-9,2,5,7},{3,-15,3,2,5,7}})fill(pixels,0,0,b);
        // Continuous shafts terminate behind the exposed gear faces, never in front of the teeth.
        for(int[] b:new int[][]{{3,-6,-1,18,2,2},{3,9,-1,18,2,2},
                {7,-7,-18,2,2,19},{7,4,0,2,2,18},
                {4,-8,-2,11,2,2},{4,7,-2,11,2,2},
                {7,-8,-12,2,20,2},{7,-8,10,2,20,2}})fill(pixels,0,6,b);
        collar(pixels,0,17,-5,0);collar(pixels,0,17,10,0);
        collar(pixels,2,8,-6,-14);collar(pixels,2,8,5,14);
        // Slotted guide spanning the outer bearing and the back crossmember.
        fill(pixels,0,0,new int[]{15,-8,11,5,2,5});
        fill(pixels,0,0,new int[]{15,-4,11,5,2,5});
        fill(pixels,0,0,new int[]{14,-8,13,2,7,2});
        // A slender piston and its two clevises read as a component, not another full housing.
        fill(pixels,0,0,new int[]{10,-15,8,4,7,4});
        fill(pixels,3,0,new int[]{11,-8,9,2,16,2});
        fill(pixels,0,0,new int[]{10,8,8,4,3,4});
        fill(pixels,0,0,new int[]{7,9,11,7,2,2});
        fill(pixels,3,0,new int[]{-2,11,-3,6,2,6});
        fill(pixels,3,0,new int[]{-2,-14,-3,6,2,6});
        fill(pixels,2,0,new int[]{4,-3,-3,2,6,6});
        // Union the mirrored steel hemisphere with the frame, removing buried interface faces.
        for(int x=0;x<6;x++)for(int y=-6;y<6;y++)for(int z=-6;z<6;z++)if(core(-x-1,y,z))
            fill(pixels,0,7,new int[]{x*2,y*2,z*2,2,2,2});
        appendPixels(faces,pixels);
    }
    private static void appendPixels(List<Face> faces,Map<Pixel,Integer> pixels){
        // Union opaque stationary parts before emitting faces: no buried or coplanar box layers.
        var strips=new LinkedHashMap<Strip,SortedSet<Integer>>();
        for(var entry:pixels.entrySet())for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}){
            var p=entry.getKey();if(pixels.containsKey(p.next(axis,sign))||entry.getValue()/4==7&&axis==0&&sign<0&&p.x()==0)continue;
            int u=(axis+1)%3,v=(axis+2)%3;
            var strip=new Strip(entry.getValue(),axis,sign,p.at(axis)+(sign>0?1:0),p.at(v));
            strips.computeIfAbsent(strip,key->new TreeSet<>()).add(p.at(u));
        }
        for(var entry:strips.entrySet()){
            var s=entry.getKey();int start=Integer.MIN_VALUE,end=start;
            for(int col:entry.getValue()){
                if(start!=Integer.MIN_VALUE&&col!=end){plane(faces,s.style()%4,s.style()/4,s.axis(),s.sign(),s.at(),start,end,s.row(),s.row()+1);start=col;}
                if(start==Integer.MIN_VALUE)start=col;end=col+1;
            }
            plane(faces,s.style()%4,s.style()/4,s.axis(),s.sign(),s.at(),start,end,s.row(),s.row()+1);
        }
    }
    private static List<List<Face>> debris(){
        var fragments=new ArrayList<List<Face>>();
        for(var gear:GEARS){
            var faces=new ArrayList<Face>();
            for(var face:FACES)if(face.group()==gear.group()){
                var xyz=face.vertices().clone();
                for(int i=0;i<4;i++){xyz[i*3]-=gear.x();xyz[i*3+1]-=gear.y();xyz[i*3+2]-=gear.z();}
                faces.add(new Face(face.material(),face.group(),xyz,face.uv(),face.nx(),face.ny(),face.nz()));
            }
            fragments.add(List.copyOf(faces));
        }
        for(int kind=0;kind<3;kind++){
            var pixels=new HashMap<Pixel,Integer>();
            if(kind==0)collar(pixels,2,0,0,0);
            if(kind==1){
                fill(pixels,0,0,new int[]{-3,-3,-3,6,6,4});
                fill(pixels,3,0,new int[]{-1,-1,1,2,2,6});
                fill(pixels,0,0,new int[]{-3,-2,6,6,4,2});
            }
            if(kind==2){
                fill(pixels,0,0,new int[]{-4,-3,-2,2,6,4});
                fill(pixels,0,0,new int[]{2,-3,-2,2,6,4});
                fill(pixels,0,0,new int[]{-2,-3,-2,4,2,4});
                fill(pixels,3,0,new int[]{-4,1,-1,8,2,2});
            }
            var faces=new ArrayList<Face>();appendPixels(faces,pixels);fragments.add(List.copyOf(faces));
        }
        return List.copyOf(fragments);
    }
    private static List<List<Face>> crystalDebris(){
        var fragments=new ArrayList<List<Face>>();
        for(int kind=0;kind<3;kind++){
            var pixels=new HashMap<Pixel,Integer>();
            // One-pixel terraces form pointed prisms, a shard and a small crystal cluster.
            for(int y=-5;y<=5;y++){
                int radius=2;
                if(Math.abs(y)>=4)radius=1;
                int depth=kind==1?1:radius;
                fill(pixels,2,0,new int[]{-radius,y,-depth,radius*2,1,depth*2});
            }
            if(kind==2)for(int y=-4;y<=1;y++){
                int radius=y==1?1:2;
                fill(pixels,2,0,new int[]{2,y,-radius,radius*2,1,radius*2});
            }
            var faces=new ArrayList<Face>();appendPixels(faces,pixels);
            fragments.add(List.copyOf(mergeMechanism(faces)));
        }
        return List.copyOf(fragments);
    }
    private static void fill(Map<Pixel,Integer> pixels,int material,int group,int[] b){
        for(int x=b[0];x<b[0]+b[3];x++)for(int y=b[1];y<b[1]+b[4];y++)for(int z=b[2];z<b[2]+b[5];z++)
            pixels.put(new Pixel(x,y,z),group*4+material);
    }
    private static void collar(Map<Pixel,Integer> pixels,int axis,int x,int y,int z){
        int u=(axis+1)%3,v=(axis+2)%3;int[] center={x,y,z};
        for(int a=-3;a<3;a++)for(int b=-3;b<3;b++)if(a<-1||a>=1||b<-1||b>=1){
            int[] p=center.clone();p[u]+=a;p[v]+=b;
            for(int d=-1;d<=0;d++){p[axis]=center[axis]+d;pixels.put(new Pixel(p[0],p[1],p[2]),0);}
        }
    }
    private static boolean core(int x,int y,int z){return x<0&&(x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5)<=36;}
    private static boolean gear(int y,int z,int radius){
        double a=y+.5,b=z+.5,r=radius*2,d=a*a+b*b;
        double spokeAngle=Math.atan2(b,a)*4/Math.PI;
        boolean tooth=d<=r*r&&d>=(r-1.5)*(r-1.5)&&Math.abs(spokeAngle-Math.rint(spokeAngle))<.16;
        return tooth||d<=(r-1)*(r-1)&&(d>=(r-3)*(r-3)||Math.min(Math.abs(a),Math.abs(b))<=1);
    }
    private static void appendGear(List<Face> faces,Gear gear){
        int axis=gear.axis(),u=(axis+1)%3,v=(axis+2)%3;float[] center={gear.x(),gear.y(),gear.z()};
        for(int y=-gear.radius()*2;y<gear.radius()*2;y++)for(int z=-gear.radius()*2;z<gear.radius()*2;z++)if(gear(y,z,gear.radius())){
            float[] from=center.clone();from[axis]-=1;from[u]+=y;from[v]+=z;
            for(int side=0;side<3;side++)for(int sign:new int[]{-1,1}){
                if(side!=axis&&gear(y+(side==u?sign:0),z+(side==v?sign:0),gear.radius()))continue;
                int a=(side+1)%3,b=(side+2)%3;
                plane(faces,3,gear.group(),side,sign,from[side]+(sign>0?(side==axis?2:1):0),from[a],from[a]+(a==axis?2:1),from[b],from[b]+(b==axis?2:1));
            }
        }
    }
    private static int surface(int axis,int sign,int slice,int u,int col,int v,int row){
        int[] p=new int[3];p[axis]=slice;p[u]=col;p[v]=row;
        if(p[0]>=0||!inside(p[0],p[1],p[2]))return -1;
        int x=p[0];p[axis]+=sign;
        if(inside(p[0],p[1],p[2]))return -1;
        return 1;
    }
    private static void box(List<Face> faces,int material,int group,float x,float y,float z,float w,float h,float d){
        float[] from={x,y,z},size={w,h,d};
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}){
            int u=(axis+1)%3,v=(axis+2)%3;
            plane(faces,material,group,axis,sign,from[axis]+(sign>0?size[axis]:0),from[u],from[u]+size[u],from[v],from[v]+size[v]);
        }
    }
    private static void plane(List<Face> faces,int material,int group,int axis,int sign,float at,float u0,float u1,float v0,float v1){
        int u=(axis+1)%3,v=(axis+2)%3;
        // Keep the fade local: a long merged quad must not interpolate across the entire hemisphere.
        if(material==1&&u==0)for(float cut=-EDGE_FADE_WIDTH;cut<0;cut+=2)if(u0<cut&&u1>cut){
            plane(faces,material,group,axis,sign,at,u0,cut,v0,v1);plane(faces,material,group,axis,sign,at,cut,u1,v0,v1);return;
        }
        float[] xyz=new float[12],uv=new float[8];
        float[][] corners=sign>0?new float[][]{{u0,v0},{u1,v0},{u1,v1},{u0,v1}}:new float[][]{{u0,v0},{u0,v1},{u1,v1},{u1,v0}};
        for(int i=0;i<4;i++){
            xyz[i*3+axis]=at;xyz[i*3+u]=corners[i][0];xyz[i*3+v]=corners[i][1];
            if(material<2){
                uv[i*2]=((material==1?64:0)+corners[i][0]+24)/128F;
                uv[i*2+1]=(v==1?24-corners[i][1]:corners[i][1]+24)/64F;
            }else{
                uv[i*2]=((material==3?24:0)+corners[i][0]-u0)/128F;
                uv[i*2+1]=(48+corners[i][1]-v0)/64F;
            }
        }
        int[] normal=new int[3];normal[axis]=sign;
        faces.add(new Face(material,group,xyz,uv,normal[0],normal[1],normal[2]));
    }
    private WeaverSpiritGeometry(){}
}
