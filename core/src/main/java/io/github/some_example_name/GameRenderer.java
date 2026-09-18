package io.github.some_example_name;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import static io.github.some_example_name.GameWorld.PH;
import static io.github.some_example_name.GameWorld.PW;
import static io.github.some_example_name.GameWorld.TILE;
import static io.github.some_example_name.GameWorld.WALL;
import static io.github.some_example_name.GameWorld.WORLD_H;
import static io.github.some_example_name.GameWorld.WORLD_W;

/** Draws the world, the player, and every HUD/menu overlay. No game logic here. */
public class GameRenderer {

    final Assets assets;
    final SpriteBatch batch = new SpriteBatch();
    final OrthographicCamera cam = new OrthographicCamera();
    final FitViewport viewport = new FitViewport(WORLD_W, WORLD_H, cam);
    final GlyphLayout layout = new GlyphLayout();
    final Color bgColor = new Color(), envTint = new Color();

    public GameRenderer(Assets assets) {
        this.assets = assets;
    }

    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    public void render(GameWorld world, Main.State state, int menuIndex, int bestInf, int bestCla) {
        // environment colors fade with the camera's depth
        float envDepthM = Math.max(0, (world.startY - world.camY) / TILE);
        float f1 = Theme.tier1Fade(envDepthM), f2 = Theme.tier2Fade(envDepthM);
        bgColor.set(Theme.BG0).lerp(Theme.BG1, f1).lerp(Theme.BG2, f2);
        envTint.set(Theme.TINT0).lerp(Theme.TINT1, f1).lerp(Theme.TINT2, f2);
        int envTier = Theme.tierAt(envDepthM);

        ScreenUtils.clear(bgColor);
        cam.position.set(WORLD_W / 2f, world.camY, 0);
        cam.update();
        viewport.apply();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        float camBottom = world.camY - WORLD_H / 2f;
        float camTop = world.camY + WORLD_H / 2f;

        // side walls (tile choice + tint darken with depth)
        batch.setColor(envTint);
        TextureRegion wall = assets.wallTiles[envTier];
        int firstRow = MathUtils.floor(camBottom / TILE);
        int lastRow = MathUtils.ceil(camTop / TILE);
        for (int r = firstRow; r <= lastRow; r++) {
            batch.draw(wall, 0, r * TILE, TILE, TILE);
            batch.draw(wall, WORLD_W - TILE, r * TILE, TILE, TILE);
        }

        // platforms (darkened by the same environment tint as the walls)
        for (Platform pf : world.platforms) {
            drawPlatform(pf);
            if (pf.hasSpikes())
                for (float sx = pf.spikeFrom; sx < pf.spikeTo; sx += TILE)
                    batch.draw(assets.texSpike, sx, pf.bounds.y + pf.bounds.height, TILE, 8);
        }
        batch.setColor(Color.WHITE);

        // coins stay bright so they pop against the dark
        for (Platform pf : world.platforms)
            if (pf.coin)
                batch.draw(assets.coinAnim.getKeyFrame(world.animTime + pf.bounds.x), pf.coinX, pf.coinY);

        // slimes
        for (Platform pf : world.platforms) {
            Slime s = pf.slime;
            if (s == null) continue;
            TextureRegion fr = (s.purple ? assets.slimePAnim : assets.slimeGAnim).getKeyFrame(s.animTime);
            if (s.dir < 0) batch.draw(fr, s.x + Slime.W + 4, s.y - 2, -24, 24);
            else batch.draw(fr, s.x - 4, s.y - 2, 24, 24);
        }

        drawPlayer(world, state);

        if (world.mode == GameWorld.Mode.INFINITE) {
            // ceiling spikes (flipped down)
            batch.setColor(envTint);
            for (float x = TILE; x < WORLD_W - TILE; x += TILE)
                batch.draw(assets.texSpike, x, camTop, TILE, -10);
            batch.setColor(envTint.r * 0.3f, envTint.g * 0.3f, envTint.b * 0.35f, 1f);
            batch.draw(assets.texPixel, 0, camTop - 2, WORLD_W, 4);
            batch.setColor(Color.WHITE);
        }

        drawHud(world, state, menuIndex, bestInf, bestCla, camBottom, camTop);
        batch.end();
    }

    private void drawPlatform(Platform pf) {
        float x = pf.bounds.x, y = pf.bounds.y;
        int tiles = (int) (pf.bounds.width / TILE);
        if (pf.blocks) {
            // classic floors: a row of square blocks; ice blocks keep their own color
            TextureRegion block = pf.icy ? assets.iceTile : assets.blockTop[pf.tier];
            for (int i = 0; i < tiles; i++)
                batch.draw(block, x + i * TILE, y, TILE, TILE);
        } else {
            // thin bar from platforms.png; hazard bars are blue, otherwise tier color
            int row = pf.hasSpikes() ? 3 : pf.tier;
            batch.draw(assets.platTiles[row][0], x, y);
            for (int i = 1; i < tiles - 1; i++)
                batch.draw(assets.platTiles[row][1], x + i * TILE, y);
            batch.draw(assets.platTiles[row][3], x + (tiles - 1) * TILE, y);
        }
    }

    private void drawPlayer(GameWorld world, Main.State state) {
        // blink while invincible
        if (world.invincible > 0 && ((int) (world.invincible * 12) % 2 == 0) && state == Main.State.PLAY) return;

        TextureRegion[] frame;
        if (!world.grounded) frame = world.pvy > 40 ? assets.jumpFrame : assets.fallFrame;
        else if (Math.abs(world.pvx) > 5)
            frame = (world.facingLeft ? assets.walkW : assets.walkE).getKeyFrame(world.animTime);
        else frame = assets.idleFrame;

        // 64x64 cell drawn at half scale (32x32); feet sit ~5px above the cell bottom at this scale
        float dx = world.px + PW / 2f - 16;
        float dy = world.py - 5;
        for (TextureRegion r : frame) batch.draw(r, dx, dy, 32, 32);
    }

    private void drawHud(GameWorld world, Main.State state, int menuIndex, int bestInf, int bestCla,
                         float camBottom, float camTop) {
        float camY = world.camY;
        BitmapFont font = assets.font, fontBig = assets.fontBig;

        // hearts
        for (int i = 0; i < GameWorld.MAX_HP; i++)
            batch.draw(i < world.hp ? assets.texHeart : assets.texHeartEmpty,
                WALL + 4 + i * 9, camTop - 22, 7, 6);

        // depth + coins
        String txt = world.depthMetres() + "m";
        layout.setText(font, txt);
        font.draw(batch, txt, WORLD_W - WALL - 4 - layout.width, camTop - 15);
        batch.draw(assets.coinAnim.getKeyFrame(world.animTime), WORLD_W - WALL - 4 - 30 - layout.width, camTop - 26, 12, 12);
        String ctxt = "" + world.coins;
        font.draw(batch, ctxt, WORLD_W - WALL - 4 - 16 - layout.width, camTop - 16);

        if (state == Main.State.READY) {
            dim(camBottom, 0.35f);
            centerText(fontBig, "FALL DOWN", camY + 110);

            String inf = (menuIndex == 0 ? "> " : "  ") + "INFINITE MODE";
            String cla = (menuIndex == 1 ? "> " : "  ") + "CLASSIC MODE";
            font.setColor(menuIndex == 0 ? Color.WHITE : Color.GRAY);
            centerText(font, inf, camY + 60);
            font.setColor(menuIndex == 1 ? Color.WHITE : Color.GRAY);
            centerText(font, cla, camY + 44);
            font.setColor(Color.WHITE);
            centerText(font, menuIndex == 0 ? "THE SHAFT NEVER STOPS" : "FIND THE HOLES, MIND THE ICE",
                camY + 20);

            centerText(font, "UP/DOWN SELECT, SPACE START", camY - 30);
            centerText(font, "MOVE A/D  JUMP SPACE", camY - 46);
            centerText(font, "BEST  INF " + bestInf + "  CLA " + bestCla, camY - 70);
        } else if (state == Main.State.PAUSE) {
            dim(camBottom, 0.55f);
            centerText(fontBig, "PAUSED", camY + 20);
            centerText(font, "ESC RESUME / M MENU", camY - 10);
        } else if (state == Main.State.OVER) {
            dim(camBottom, 0.55f);
            int score = world.score();
            int best = world.mode == GameWorld.Mode.INFINITE ? bestInf : bestCla;
            centerText(fontBig, "GAME OVER", camY + 50);
            centerText(font, "DEPTH " + world.depthMetres() + "m", camY + 20);
            centerText(font, "COINS " + world.coins, camY + 8);
            centerText(font, "SCORE " + score, camY - 8);
            centerText(font, score >= best ? "NEW BEST!" : "BEST " + best, camY - 24);
            centerText(font, "R RETRY / M MENU", camY - 55);
        }
    }

    private void dim(float camBottom, float alpha) {
        batch.setColor(0, 0, 0, alpha);
        batch.draw(assets.texPixel, 0, camBottom, WORLD_W, WORLD_H);
        batch.setColor(Color.WHITE);
    }

    private void centerText(BitmapFont f, String text, float y) {
        layout.setText(f, text);
        f.draw(batch, text, (WORLD_W - layout.width) / 2f, y);
    }

    public void dispose() {
        batch.dispose();
    }
}
