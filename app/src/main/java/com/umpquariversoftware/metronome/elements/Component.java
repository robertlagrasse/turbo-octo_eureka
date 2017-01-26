package com.umpquariversoftware.metronome.elements;

/**
 * Created by robert on 1/26/17.
 */

public class Component {

    private String name;
    private int resource;
    private String hexID;

    public String getHexID() {
        return hexID;
    }

    public void setHexID(String hexID) {
        this.hexID = hexID;
    }

    public Component() {
        // Empty Constructor
    }

    public Component(int resource){
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getResource() {
        return resource;
    }

    public void setResource(int resource) {
        this.resource = resource;
    }
}
