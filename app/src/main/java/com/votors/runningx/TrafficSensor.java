package com.votors.runningx;

public class TrafficSensor
{
    private float x;
    private float y;
    private float z;
    public TrafficSensor()
    {
    }
    public TrafficSensor(float x,float y,float z)
    {
        this.x=x;
        this.y=y;
        this.z=z;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public void setY(float y) {

        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getZ() {
        return z;
    }

    public float getY() {

        return y;
    }
}
