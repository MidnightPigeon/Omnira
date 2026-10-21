package com.mcmagic.omnira.client.renderer;

/** Fixed faceted sphere; resource-pack sprites and fluid tint are resolved at draw time. */
final class LiquidOrbMesh {
    record Vertex(float x,float y,float z,float u,float v) {}
    private static final Vertex[] VERTICES=build();
    static int size(){return VERTICES.length;}
    static Vertex vertex(int index){return VERTICES[index];}
    private static Vertex[] build(){
        var vertices=new Vertex[6*8*4];int index=0;
        for(int ring=0;ring<6;ring++)for(int segment=0;segment<8;segment++){
            double lo=-Math.PI/2+ring*Math.PI/6,hi=lo+Math.PI/6;
            double a=segment*Math.PI/4,b=a+Math.PI/4;
            double[][] corners={{lo,a},{hi,a},{hi,b},{lo,b}};
            for(var corner:corners){
                double latitude=corner[0],longitude=corner[1];
                vertices[index++]=new Vertex((float)(Math.cos(latitude)*Math.cos(longitude)),
                        (float)Math.sin(latitude),(float)(Math.cos(latitude)*Math.sin(longitude)),
                        (float)(longitude/(Math.PI*2)),(float)(.5-latitude/Math.PI));
            }
        }
        return vertices;
    }
    private LiquidOrbMesh(){}
}
