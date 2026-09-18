package io.github.some_example_name;

/** A slime patrolling one platform segment. Hitbox is bottom-left anchored. */
public class Slime {
    public static final float W = 16, H = 12;

    public float x, y;
    public float minX, maxX, dir = 1, speed;
    public boolean purple;
    public float animTime;
}
