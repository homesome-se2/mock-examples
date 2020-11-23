package com.homesome.model;

public class GadgetGroup {

    private String groupName;
    private int[] gadgetIDs;

    public GadgetGroup(String groupName, int[] gadgetIDs) {
        this.groupName = groupName;
        this.gadgetIDs = gadgetIDs;
    }

    public String toHoSoProtocol() {
        String group = groupName;
        for(int gadgetID : gadgetIDs) {
            group = String.format("%s:%s", group, gadgetID);
        }
        return group;
    }
}

