package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;

/**
 * Loads and owns every texture, animation, sound, and font.
 *
 * Sprite sheet facts (hand-derived, see DESIGN.md):
 * - Mana Seed page 1: 512x512, 64px cells; rows 0-3 = S,N,E,W (stand col 0, jump cols 5-7),
 *   rows 4-7 = walk cols 0-5. The player is a paper doll: base + outfit + hair layers.
 * - platforms.png: 16px bar rows 0 green, 1 sand, 2 orange, 3 blue; cols cap-L, mid, mid, cap-R.
 * - world_tileset.png terrain columns: 0 grass, 2 sandstone, 3 cracked stone, 6 ice, 8 dark stone
 *   (row 0 = surface tile, row 1 = fill).
 * - slime sheets: 24px cells, middle row = 4-frame bounce.
 */
public class Assets {

    static final int TILE = GameWorld.TILE;

    public final Texture texBase, texOutfit, texHair, texPlatforms, texTiles, texCoin, texSlimeG, texSlimeP;
    public final Texture texSpike, texHeart, texHeartEmpty, texPixel;

    // player animation frames are layered: [base, outfit, hair]
    public final Animation<TextureRegion[]> walkE, walkW;
    public final TextureRegion[] fallFrame, idleFrame, jumpFrame;

    public final TextureRegion[][] platTiles;
    public final TextureRegion[] wallTiles;        // per-tier wall fill tile
    public final TextureRegion[] blockTop;         // per-tier square terrain block (classic floors)
    public final TextureRegion iceTile;            // slippery block
    public final Animation<TextureRegion> coinAnim, slimeGAnim, slimePAnim;

    public final Sound sndCoin, sndHurt, sndTap, sndJump, sndPowerUp;
    public final Music music;
    public final BitmapFont font, fontBig;

    public Assets() {
        texBase = load("char/base.png");
        texOutfit = load("char/outfit.png");
        texHair = load("char/hair.png");
        texPlatforms = load("sprites/platforms.png");
        texTiles = load("sprites/world_tileset.png");
        texCoin = load("sprites/coin.png");
        texSlimeG = load("sprites/slime_green.png");
        texSlimeP = load("sprites/slime_purple.png");

        // ---- player (Mana Seed paper doll) ----
        TextureRegion[][] b = TextureRegion.split(texBase, 64, 64);
        TextureRegion[][] o = TextureRegion.split(texOutfit, 64, 64);
        TextureRegion[][] h = TextureRegion.split(texHair, 64, 64);
        walkE = new Animation<>(0.11f, layered(b, o, h, 6, 0, 6));
        walkE.setPlayMode(Animation.PlayMode.LOOP);
        walkW = new Animation<>(0.11f, layered(b, o, h, 7, 0, 6));
        walkW.setPlayMode(Animation.PlayMode.LOOP);
        jumpFrame = new TextureRegion[]{b[0][6], o[0][6], h[0][6]};   // south jump, rising frame
        fallFrame = new TextureRegion[]{b[0][7], o[0][7], h[0][7]};   // south jump, airborne frame
        idleFrame = new TextureRegion[]{b[0][0], o[0][0], h[0][0]};   // south stand

        // ---- brackeys tiles ----
        platTiles = TextureRegion.split(texPlatforms, TILE, TILE);
        TextureRegion[][] tiles = TextureRegion.split(texTiles, TILE, TILE);
        wallTiles = new TextureRegion[]{tiles[1][1], tiles[1][3], tiles[1][8]};
        blockTop = new TextureRegion[]{tiles[0][0], tiles[0][2], tiles[0][8]};
        iceTile = tiles[0][6];

        TextureRegion[][] c = TextureRegion.split(texCoin, TILE, TILE);
        coinAnim = new Animation<>(0.08f, c[0]);
        coinAnim.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[][] sg = TextureRegion.split(texSlimeG, 24, 24);
        slimeGAnim = new Animation<>(0.15f, sg[1]);
        slimeGAnim.setPlayMode(Animation.PlayMode.LOOP);
        TextureRegion[][] sp = TextureRegion.split(texSlimeP, 24, 24);
        slimePAnim = new Animation<>(0.12f, sp[1]);
        slimePAnim.setPlayMode(Animation.PlayMode.LOOP);

        // ---- procedural pixel art (no spike/heart sprites in the packs) ----
        texSpike = buildSpikeTexture();
        texHeart = buildHeartTexture(0xE43B44FF, 0x7C1B22FF);
        texHeartEmpty = buildHeartTexture(0x3A3F4BFF, 0x262B33FF);
        texPixel = buildPixelTexture();

        // ---- audio ----
        sndCoin = Gdx.audio.newSound(Gdx.files.internal("audio/coin.wav"));
        sndHurt = Gdx.audio.newSound(Gdx.files.internal("audio/hurt.wav"));
        sndTap = Gdx.audio.newSound(Gdx.files.internal("audio/tap.wav"));
        sndJump = Gdx.audio.newSound(Gdx.files.internal("audio/jump.wav"));
        sndPowerUp = Gdx.audio.newSound(Gdx.files.internal("audio/power_up.wav"));
        music = Gdx.audio.newMusic(Gdx.files.internal("audio/time_for_adventure.mp3"));
        music.setLooping(true);
        music.setVolume(0.35f);

        // ---- fonts ----
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/PixelOperator8.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 8;
        p.mono = true;
        font = gen.generateFont(p);
        p.size = 16;
        fontBig = gen.generateFont(p);
        gen.dispose();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        fontBig.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    private static Texture load(String path) {
        Texture t = new Texture(path);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    private static Array<TextureRegion[]> layered(TextureRegion[][] b, TextureRegion[][] o, TextureRegion[][] h,
                                                  int row, int colStart, int count) {
        Array<TextureRegion[]> frames = new Array<>();
        for (int c = colStart; c < colStart + count; c++)
            frames.add(new TextureRegion[]{b[row][c], o[row][c], h[row][c]});
        return frames;
    }

    /** Spike strip: two 8px-wide grey spikes on a 16x8 tile (Brackeys stone palette). */
    private static Texture buildSpikeTexture() {
        Pixmap pm = new Pixmap(TILE, 8, Pixmap.Format.RGBA8888);
        int outline = 0x3A3F4BFF, fill = 0xB8C0CCFF, shade = 0x8B94A6FF;
        for (int s = 0; s < 2; s++) {
            int left = s * 8;
            for (int row = 0; row < 8; row++) {    // row 0 = tip (top), row 7 = base (bottom)
                int half = row / 2 + 1;            // spike widens toward the base
                int x0 = left + 4 - half, x1 = left + 3 + half;
                for (int x = x0; x <= x1; x++) {
                    boolean edge = (x == x0 || x == x1 || row == 7);
                    pm.drawPixel(x, row, edge ? outline : (x <= left + 3 ? fill : shade));
                }
            }
        }
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    private static Texture buildHeartTexture(int fill, int outline) {
        String[] rows = {
            ".XX.XX.",
            "XXXXXXX",
            "XXXXXXX",
            ".XXXXX.",
            "..XXX..",
            "...X...",
        };
        Pixmap pm = new Pixmap(7, 6, Pixmap.Format.RGBA8888);
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++)
                if (rows[y].charAt(x) == 'X') {
                    boolean edge = y == 0 || y == rows.length - 1
                        || x == 0 || x == rows[y].length() - 1
                        || rows[y - 1].charAt(x) == '.' || (y + 1 < rows.length && rows[y + 1].charAt(x) == '.');
                    pm.drawPixel(x, y, edge ? outline : fill);
                }
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    private static Texture buildPixelTexture() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    public void dispose() {
        texBase.dispose(); texOutfit.dispose(); texHair.dispose();
        texPlatforms.dispose(); texTiles.dispose(); texCoin.dispose();
        texSlimeG.dispose(); texSlimeP.dispose();
        texSpike.dispose(); texHeart.dispose(); texHeartEmpty.dispose(); texPixel.dispose();
        sndCoin.dispose(); sndHurt.dispose(); sndTap.dispose(); sndJump.dispose(); sndPowerUp.dispose();
        music.dispose();
        font.dispose(); fontBig.dispose();
    }
}
