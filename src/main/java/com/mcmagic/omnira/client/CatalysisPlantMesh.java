package com.mcmagic.omnira.client;

import java.util.ArrayList;

/** Fixed cell layouts; keep covered cells because leaf transparency can expose them. */
final class CatalysisPlantMesh {
    record Cell(int x,int y,int z,boolean foliage) {}
    static final CatalysisPlantMesh TREE=tree(false),CONIFER=tree(true);
    static final CatalysisPlantMesh NETHER_FUNGUS=fungus(true,false),RED_MUSHROOM=fungus(false,true),BROWN_MUSHROOM=fungus(false,false);
    private final Cell[] cells;
    private CatalysisPlantMesh(ArrayList<Cell> cells){this.cells=cells.toArray(Cell[]::new);}
    int size(){return cells.length;}
    Cell cell(int index){return cells[index];}
    private static CatalysisPlantMesh tree(boolean conifer){
        var cells=new ArrayList<Cell>();
        for(int y=0;y<6;y++){
            int radius=y<2?-1:y==5?0:conifer?(y==2?2:1):y==4?1:2;
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                boolean trunk=x==0&&z==0&&y<4;
                boolean leaf=radius>=0&&Math.abs(x)<=radius&&Math.abs(z)<=radius&&(radius<2||Math.abs(x)+Math.abs(z)<4);
                if(trunk||leaf)cells.add(new Cell(x,y,z,!trunk));
            }
        }
        return new CatalysisPlantMesh(cells);
    }
    private static CatalysisPlantMesh fungus(boolean nether,boolean red){
        var cells=new ArrayList<Cell>();
        for(int y=0;y<7;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            boolean trunk=x==0&&z==0&&y<6;
            boolean cap=nether?y>=4&&y<=6&&(y==6?Math.abs(x)<=1&&Math.abs(z)<=1:Math.abs(x)+Math.abs(z)<4)
                    :red?y>=4&&y<=6&&(y==6?Math.abs(x)<=1&&Math.abs(z)<=1:Math.max(Math.abs(x),Math.abs(z))==2&&Math.abs(x)+Math.abs(z)<4)
                    :y==6&&Math.abs(x)+Math.abs(z)<4;
            if(trunk||cap)cells.add(new Cell(x,y,z,cap));
        }
        return new CatalysisPlantMesh(cells);
    }
}
