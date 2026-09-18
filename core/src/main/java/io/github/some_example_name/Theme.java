package io.github.some_example_name;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * Depth-based atmosphere: three visual tiers that also drive difficulty.
 * tier 0: grass (surface), tier 1: sandstone ruins, tier 2: abyss (dark stone, red tint).
 */
public final class Theme {

    public static final float TIER1_START = 45, TIER1_END = 75;   // fade range in metres
    public static final float TIER2_START = 110, TIER2_END = 150;

    public static final Color BG0 = new Color(0.078f, 0.082f, 0.122f, 1f);
    public static final Color BG1 = new Color(0.048f, 0.036f, 0.078f, 1f);
    public static final Color BG2 = new Color(0.045f, 0.010f, 0.018f, 1f);
    public static final Color TINT0 = new Color(1f, 1f, 1f, 1f);
    public static final Color TINT1 = new Color(0.85f, 0.80f, 0.92f, 1f);
    public static final Color TINT2 = new Color(0.85f, 0.52f, 0.52f, 1f);

    private Theme() {
    }

    /** 0..1 fade into tier 1 at the given depth in metres. */
    public static float tier1Fade(float depthM) {
        return MathUtils.clamp((depthM - TIER1_START) / (TIER1_END - TIER1_START), 0f, 1f);
    }

    /** 0..1 fade into tier 2 at the given depth in metres. */
    public static float tier2Fade(float depthM) {
        return MathUtils.clamp((depthM - TIER2_START) / (TIER2_END - TIER2_START), 0f, 1f);
    }

    /** Discrete tier used to pick tiles: 0 grass, 1 sandstone, 2 abyss. */
    public static int tierAt(float depthM) {
        if (depthM >= (TIER2_START + TIER2_END) / 2f) return 2;
        if (depthM >= (TIER1_START + TIER1_END) / 2f) return 1;
        return 0;
    }

    /** Overall 0..1 difficulty ramp; reaches 1 at 150m. */
    public static float difficulty(float depthM) {
        return MathUtils.clamp(depthM / 150f, 0f, 1f);
    }
}
