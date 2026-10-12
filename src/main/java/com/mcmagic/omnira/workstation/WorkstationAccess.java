package com.mcmagic.omnira.workstation;

public interface WorkstationAccess {
    CrystalWorkstationEntity workstation();
    default float shaftSpeed(){return workstation().rpm;}
}
