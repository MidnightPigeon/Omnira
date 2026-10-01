package com.mcmagic.omnira.item.staff;

/** A physical press grants at most one cast; held-use repeats grant none. */
public final class StaffUseClick {
    private boolean held;
    private boolean pending;
    public void begin() {pending=!held;held=true;}
    public boolean consume() {boolean value=pending;pending=false;return value;}
    public void release() {held=false;pending=false;}
}
