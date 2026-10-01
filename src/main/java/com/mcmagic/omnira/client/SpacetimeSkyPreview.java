package com.mcmagic.omnira.client;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Offline projection of the same vertices and colors submitted by the game renderer. */
public final class SpacetimeSkyPreview {
    private record Vertex(float x, float y, float z, int color) {}
    private static List<Vertex> vertices(double time, float forest) {
        return vertices(time,forest,0);
    }
    private static List<Vertex> vertices(double time, float forest,float ruins) {
        List<Vertex> result = new ArrayList<>();
        SpacetimeSkyMesh.render(time,forest,ruins,(x,y,z,c)->result.add(new Vertex(x,y,z,c)));
        return result;
    }

    public static void main(String[] args) throws Exception {
        var normal = vertices(0,0);
        var later = vertices(3,0);
        var forest = vertices(0,1);
        var ruins = vertices(0,0,1);
        if(normal.equals(ruins)||forest.equals(ruins))throw new AssertionError("Missing separate ruins tint");
        for(int i=0;i<normal.size();i++){
            var a=normal.get(i);var b=ruins.get(i);
            if(a.x!=b.x||a.y!=b.y||a.z!=b.z||a.color>>>24!=b.color>>>24)throw new AssertionError("Tint changed geometry or alpha");
        }
        if (normal.size() % 4 != 0 || normal.size() != later.size()) throw new AssertionError("Invalid quads");
        int moving = 0, glyph = 0;
        for (int i=0;i<normal.size();i++) {
            Vertex a=normal.get(i), b=later.get(i);
            if (!Float.isFinite(a.x+a.y+a.z)) throw new AssertionError("Invalid vertex");
            if (a.color != b.color) moving++;
            if (a.y == 80) {
                glyph++;
                if (a.x!=b.x || a.y!=b.y || a.z!=b.z) throw new AssertionError("Moon moved");
            }
        }
        if (moving<100 || glyph<100 || normal.equals(forest)) throw new AssertionError("Missing animation or forest tint");
        BufferedImage image=new BufferedImage(1280,1530,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,1280,1530);
        for(int row=0;row<3;row++) for(int col=0;col<2;col++) {
            int x=col*640,y=row*510;
            g.drawImage(project(row==0?normal:row==1?forest:ruins,col==0?35:90),x,y+30,null);
            g.setColor(Color.WHITE);g.drawString((row==0?"BASE / STILLED EXPANSE":row==1?"FLEETING WOODS":"EVENTIDE RUINS")+(col==0?" - AURORA":" - ZENITH"),x+12,y+20);
        }
        g.dispose();File file=new File(args[0]);file.getParentFile().mkdirs();ImageIO.write(image,"png",file);
        ImageIO.write(project(later,35),"png",new File(file.getParentFile(),"spacetime-sky-t3.png"));
        System.out.println("Sky mesh verified: "+normal.size()/4+" quads, "+glyph/4+" fixed moon pixels, "+moving+" animated vertices; wrote "+file);
    }

    private static BufferedImage project(List<Vertex> vertices, double pitchDegrees) {
        BufferedImage image=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(35,57,88));g.fillRect(0,0,640,480);
        double pitch=Math.toRadians(pitchDegrees),s=Math.sin(pitch),c=Math.cos(pitch);
        for(int i=0;i<vertices.size();i+=4) {
            int[] x=new int[4],y=new int[4];boolean visible=true;
            for(int j=0;j<4;j++) {
                Vertex v=vertices.get(i+j);double depth=v.y*s+v.z*c;
                if(depth<=.1){visible=false;break;}
                x[j]=(int)Math.round(320+v.x/depth*300);
                y[j]=(int)Math.round(240-(v.y*c-v.z*s)/depth*300);
            }
            if(visible){g.setColor(new Color(vertices.get(i).color,true));g.fillPolygon(x,y,4);}
        }
        g.dispose();return image;
    }
}
