package io.github.some_example_name;

import com.badlogic.gdx.math.Rectangle;

/**
 * One standable piece of the world.
 * INFINITE mode: a floating thin bar. CLASSIC mode: a run of square blocks
 * between two holes of a full-width floor ({@code blocks} = true).
 */
public class Platform {
    public final Rectangle bounds = new Rectangle();
    public boolean blocks;                         // draw as square blocks (classic) vs thin bar (infinite)
    public boolean icy;                            // slippery surface
    public float spikeFrom, spikeTo;               // absolute x range of the spike patch (empty if from >= to)
    public boolean visited;                        // first landing already rewarded
    public int tier;                               // visual theme at this platform's depth
    public Slime slime;
    public boolean coin;
    public float coinX, coinY;

    public boolean hasSpikes() {
        return spikeTo > spikeFrom;
    }
}
