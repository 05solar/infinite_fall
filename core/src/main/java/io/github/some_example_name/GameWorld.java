package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Simulation state: the player, platforms, slimes, coins, camera scroll,
 * procedural generation, and all gameplay rules. No rendering here.
 */
public class GameWorld {

    public enum Mode { INFINITE, CLASSIC }

    // ---- world constants (pixels) ----
    public static final float WORLD_W = 240, WORLD_H = 400;
    public static final int TILE = 16;
    public static final float WALL = TILE;         // side wall thickness
    public static final int MAX_HP = 5;

    static final float GRAVITY = 620f, MAX_FALL = 380f, MAX_FALL_CLASSIC = 185f;
    static final float MOVE_SPEED = 130f, MOVE_SPEED_CLASSIC = 100f;
    static final float JUMP_SPEED = 240f;          // ~46px jump height
    static final float FREE_FALL_LIMIT = 190f;     // free-fall distance (~2 floors) before fall damage
    static final float ICE_RESPONSE = 2.6f;        // how quickly velocity answers input on ice (1/s)

    final Assets assets;

    public Mode mode = Mode.INFINITE;
    public final Array<Platform> platforms = new Array<>();

    // player state (hitbox bottom-left + velocity)
    public static final float PW = 12, PH = 18;
    public float px, py, pvx, pvy;
    public boolean grounded, facingLeft;
    public Platform standingOn;
    public float animTime, invincible, fallDist;
    public int hp, coins;
    public float startY, maxDepth;                 // maxDepth in pixels descended
    public boolean dead;

    public float camY, autoSpeed;
    float genY;                                    // y of the last generated platform/floor

    public GameWorld(Assets assets) {
        this.assets = assets;
    }

    public int score() {
        return (int) (maxDepth / TILE) + coins * 5;
    }

    public int depthMetres() {
        return (int) (maxDepth / TILE);
    }

    // ------------------------------------------------------------------ reset / generation

    public void reset(Mode mode) {
        this.mode = mode;
        platforms.clear();
        hp = MAX_HP;
        coins = 0;
        maxDepth = 0;
        invincible = 0;
        fallDist = 0;
        animTime = 0;
        pvx = pvy = 0;
        facingLeft = false;
        standingOn = null;
        dead = false;

        float y0 = WORLD_H * 0.45f;
        if (mode == Mode.INFINITE) {
            Platform start = new Platform();
            float w = 6 * TILE;
            start.bounds.set(WORLD_W / 2f - w / 2f, y0, w, TILE);
            start.visited = true;
            platforms.add(start);
            px = WORLD_W / 2f - PW / 2f;
        } else {
            // starting floor: hazard-free, one hole
            int before = platforms.size;
            makeFloor(y0, 0);
            Platform widest = null;
            for (int i = before; i < platforms.size; i++) {
                Platform pf = platforms.get(i);
                pf.visited = true;
                pf.icy = false;
                pf.spikeFrom = pf.spikeTo = 0;
                pf.slime = null;
                pf.coin = false;
                if (widest == null || pf.bounds.width > widest.bounds.width) widest = pf;
            }
            px = widest.bounds.x + widest.bounds.width / 2f - PW / 2f;
        }

        py = y0 + TILE;
        startY = py;
        grounded = true;

        camY = WORLD_H / 2f;
        autoSpeed = 32f;
        genY = y0;
        generateDownTo(camY - WORLD_H);            // pre-fill below the first screen
    }

    void generateDownTo(float bottomY) {
        while (genY > bottomY) {
            float depthM = (startY - genY) / TILE;
            float diff = Theme.difficulty(depthM);

            if (mode == Mode.INFINITE) {
                genY -= MathUtils.random(52f, 76f) + diff * 26f;        // wider gaps deeper
                makeBar(genY, depthM, diff);
            } else {
                genY -= MathUtils.random(64f, 88f);                     // floor spacing (jump is ~46px)
                makeFloor(genY, depthM);
            }
        }
    }

    /** INFINITE mode: one floating bar platform. */
    private void makeBar(float y, float depthM, float diff) {
        int tilesWide = MathUtils.random(3, diff > 0.6f ? 5 : 6);       // narrower platforms deeper
        float spikeP = 0.14f + diff * 0.16f;                            // up to 30%
        float slimeP = 0.16f + diff * 0.16f;                            // up to 32%

        Platform pf = new Platform();
        float w = tilesWide * TILE;
        float x = MathUtils.random(WALL, WORLD_W - WALL - w);
        pf.bounds.set(x, y, w, TILE);
        pf.tier = Theme.tierAt(depthM);

        float roll = MathUtils.random();
        if (roll < spikeP) {
            pf.spikeFrom = x;
            pf.spikeTo = x + w;
        } else if (roll < spikeP + slimeP && tilesWide >= 4) {
            addSlime(pf, depthM, diff);
        } else if (MathUtils.randomBoolean(0.28f)) {
            addCoin(pf, pf.bounds.x + pf.bounds.width / 2f - TILE / 2f);
        }
        platforms.add(pf);
    }

    /**
     * CLASSIC mode: a full-width floor of square blocks with 1-2 holes.
     * Each solid run becomes one Platform segment that may be icy, carry a
     * spike patch, a slime, or a coin.
     */
    private void makeFloor(float y, float depthM) {
        float diff = Theme.difficulty(depthM);
        int cells = (int) ((WORLD_W - 2 * WALL) / TILE);                // 13 interior tiles

        // fewer, narrower holes deeper = a more punishing single route
        int holeCount = MathUtils.random() < 0.45f + 0.35f * diff ? 1 : 2;
        int holeW = (diff > 0.5f && MathUtils.randomBoolean()) ? 2 : 3;

        boolean[] hole = new boolean[cells];
        for (int hIdx = 0; hIdx < holeCount; hIdx++) {
            for (int tries = 0; tries < 20; tries++) {
                int hStart = MathUtils.random(0, cells - holeW);
                boolean clear = true;                                    // keep holes 2+ cells apart
                for (int i = Math.max(0, hStart - 2); i < Math.min(cells, hStart + holeW + 2); i++)
                    if (hole[i]) { clear = false; break; }
                if (clear) {
                    for (int i = hStart; i < hStart + holeW; i++) hole[i] = true;
                    break;
                }
            }
        }

        float iceP = 0.15f + diff * 0.30f;                              // up to 45% of runs are ice
        float spikeP = 0.25f + diff * 0.30f;                            // up to 55% carry a spike patch
        float slimeP = 0.20f + diff * 0.22f;                            // up to 42%

        int runStart = 0;
        for (int i = 0; i <= cells; i++) {
            boolean solid = i < cells && !hole[i];
            if (solid) continue;
            int runLen = i - runStart;
            if (runLen > 0) {
                Platform pf = new Platform();
                float x = WALL + runStart * TILE;
                pf.bounds.set(x, y, runLen * TILE, TILE);
                pf.blocks = true;
                pf.tier = Theme.tierAt(depthM);
                pf.icy = MathUtils.random() < iceP;

                if (MathUtils.random() < spikeP && runLen >= 2) {
                    // patch of 1-3 tiles, never the whole run: a safe cell always exists
                    int patchW = MathUtils.random(1, Math.min(diff > 0.6f ? 3 : 2, runLen - 1));
                    int patchStart = MathUtils.random(0, runLen - patchW);
                    pf.spikeFrom = x + patchStart * TILE;
                    pf.spikeTo = pf.spikeFrom + patchW * TILE;
                }
                if (MathUtils.random() < slimeP && runLen >= 3) addSlime(pf, depthM, diff);
                else if (MathUtils.random() < 0.18f)
                    addCoin(pf, x + MathUtils.random(0, runLen - 1) * TILE);
                platforms.add(pf);
            }
            runStart = i + 1;
        }
    }

    private void addSlime(Platform pf, float depthM, float diff) {
        Slime s = new Slime();
        s.purple = depthM > 40 && MathUtils.random() < (depthM - 40) / 100f;
        s.speed = (s.purple ? 48f : 26f) + diff * (s.purple ? 42f : 26f);
        s.minX = pf.bounds.x;
        s.maxX = pf.bounds.x + pf.bounds.width - Slime.W;
        s.x = MathUtils.random(s.minX, s.maxX);
        s.y = pf.bounds.y + pf.bounds.height;
        s.dir = MathUtils.randomBoolean() ? 1 : -1;
        pf.slime = s;
    }

    private void addCoin(Platform pf, float coinX) {
        pf.coin = true;
        pf.coinX = coinX;
        pf.coinY = pf.bounds.y + pf.bounds.height + 10;
    }

    // ------------------------------------------------------------------ update

    public void update(float dt) {
        if (invincible > 0) invincible -= dt;

        // ---- input ----
        float moveSpeed = mode == Mode.CLASSIC ? MOVE_SPEED_CLASSIC : MOVE_SPEED;
        float targetVx = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            targetVx = -moveSpeed;
            facingLeft = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            targetVx = moveSpeed;
            facingLeft = false;
        }

        // slippery ice: velocity only slowly answers input, so you slide past holes
        boolean onIce = grounded && standingOn != null && standingOn.icy;
        if (onIce) pvx += (targetVx - pvx) * Math.min(1f, ICE_RESPONSE * dt);
        else pvx = targetVx;

        // jump (classic mode only)
        if (mode == Mode.CLASSIC && grounded
            && (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.W))) {
            pvy = JUMP_SPEED;
            grounded = false;
            standingOn = null;
            assets.sndJump.play(0.45f);
        }

        // ---- physics ----
        float maxFall = mode == Mode.CLASSIC ? MAX_FALL_CLASSIC : MAX_FALL;
        pvy = Math.max(pvy - GRAVITY * dt, -maxFall);
        float oldBottom = py;
        px = MathUtils.clamp(px + pvx * dt, WALL, WORLD_W - WALL - PW);
        py += pvy * dt;

        grounded = false;
        Platform landedOn = null;
        if (pvy <= 0) {
            for (Platform pf : platforms) {
                float top = pf.bounds.y + pf.bounds.height;
                if (oldBottom >= top - 0.001f && py <= top
                    && px + PW > pf.bounds.x && px < pf.bounds.x + pf.bounds.width) {
                    py = top;
                    pvy = 0;
                    grounded = true;
                    landedOn = pf;
                    break;
                }
            }
        }
        standingOn = landedOn;

        // diving past floors without ever landing drains HP
        if (grounded || pvy > 0) {
            fallDist = 0;                          // landing or any upward motion resets the free-fall
        } else {
            fallDist += -pvy * dt;
            if (fallDist > FREE_FALL_LIMIT) {
                fallDist -= FREE_FALL_LIMIT;       // keep draining on every further stretch of free-fall
                damage();
            }
        }

        if (landedOn != null) {
            boolean onSpikes = landedOn.hasSpikes()
                && px + PW > landedOn.spikeFrom && px < landedOn.spikeTo;
            if (!landedOn.visited) {
                landedOn.visited = true;
                if (!onSpikes) {
                    if (hp < MAX_HP) hp++;
                    assets.sndTap.play(0.5f);
                }
            }
            if (onSpikes && invincible <= 0) {
                damage();
                pvy = 150; // bounce off the spikes
                grounded = false;
                standingOn = null;
            }
        }

        // ---- slimes ----
        for (Platform pf : platforms) {
            Slime s = pf.slime;
            if (s == null) continue;
            s.animTime += dt;
            s.x += s.speed * s.dir * dt;
            if (s.x <= s.minX) { s.x = s.minX; s.dir = 1; }
            if (s.x >= s.maxX) { s.x = s.maxX; s.dir = -1; }
            if (invincible <= 0
                && px < s.x + Slime.W && px + PW > s.x
                && py < s.y + Slime.H && py + PH > s.y) {
                damage();
                pvy = 170;
                grounded = false;
                standingOn = null;
            }
        }

        // ---- coins ----
        for (Platform pf : platforms) {
            if (!pf.coin) continue;
            if (px < pf.coinX + TILE && px + PW > pf.coinX && py < pf.coinY + TILE && py + PH > pf.coinY) {
                pf.coin = false;
                coins++;
                assets.sndCoin.play(0.6f);
            }
        }

        // ---- camera ----
        float depthM = maxDepth / TILE;
        if (mode == Mode.INFINITE) {
            // auto scroll, ramping hard with depth, plus follow a diving player
            autoSpeed = Math.min(32f + depthM * 0.55f, 160f);
            camY -= autoSpeed * dt;
            if (py < camY - 70) camY = py + 70;
        } else {
            // classic: camera only ever follows the player down
            if (py < camY - 70) camY = py + 70;
        }

        float camTop = camY + WORLD_H / 2f;
        if (mode == Mode.INFINITE) {
            // ---- ceiling spikes ----
            if (py + PH > camTop - 14 && invincible <= 0) {
                damage();
                py = camTop - 14 - PH;
                pvy = -60;
            }
            // fully crushed above the ceiling
            if (py > camTop) {
                hp = 0;
                dead = true;
            }
        }

        maxDepth = Math.max(maxDepth, startY - py);

        // ---- world management ----
        generateDownTo(camY - WORLD_H);
        float killY = camTop + 60;
        for (int i = platforms.size - 1; i >= 0; i--)
            if (platforms.get(i).bounds.y > killY) platforms.removeIndex(i);

        animTime += dt;
    }

    private void damage() {
        if (invincible > 0) return;
        hp--;
        invincible = 1.2f;
        assets.sndHurt.play(0.7f);
        if (hp <= 0) dead = true;
    }
}
