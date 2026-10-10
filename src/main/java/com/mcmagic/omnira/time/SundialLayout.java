package com.mcmagic.omnira.time;

/** Screen/menu coordinates refer to slot interiors. */
public final class SundialLayout {
    public static final int WIDTH=200, HEIGHT=244;
    public static final int INPUT_X=55, INPUT_Y=58, CHOICE_X=129, CHOICE_Y=58;
    public static final int FUEL_X=2, FUEL_Y=124, OUTPUT_X=92, OUTPUT_Y=124;
    public static final int ENERGY_X=6, ENERGY_Y=39, ENERGY_WIDTH=8, ENERGY_HEIGHT=72;
    public static final int PROGRESS_X=78, PROGRESS_Y=64, PROGRESS_WIDTH=44, PROGRESS_HEIGHT=5;
    public static final int INVENTORY_X=19, INVENTORY_Y=162, HOTBAR_Y=220;
    public static int choiceSide(int count){
        int side=1;
        while((long)side*side<count+1L)side+=2;
        return side;
    }
    public static int choiceCell(int index,int side){return index>=side*side/2?index+1:index;}
    public static int choiceIndex(int cell,int side){return cell==side*side/2?-1:cell>side*side/2?cell-1:cell;}
    public static float choiceScale(int count){
        int radius=Math.min(Math.min(CHOICE_X+8,WIDTH-CHOICE_X-8),Math.min(CHOICE_Y+8,151-CHOICE_Y-8))-5;
        return Math.min(1f,radius*2f/(choiceSide(count)*20));
    }
    private SundialLayout() {}
}
