package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

/**
 * "Fall Down" — NS-Shaft style vertical descent game.
 * Art: Brackeys platformer pack (tiles, slimes, coins, audio) + Mana Seed character base (player).
 *
 * This class only runs the application lifecycle and the READY/PLAY/PAUSE/OVER
 * state machine. Gameplay lives in {@link GameWorld}, drawing in {@link GameRenderer},
 * resources in {@link Assets}, and depth-based atmosphere in {@link Theme}.
 */
public class Main extends ApplicationAdapter {

    public enum State { READY, PLAY, PAUSE, OVER }

    State state = State.READY;
    int menuIndex = 0;

    Assets assets;
    GameWorld world;
    GameRenderer renderer;
    Preferences prefs;
    int bestInf, bestCla;

    @Override
    public void create() {
        assets = new Assets();
        world = new GameWorld(assets);
        renderer = new GameRenderer(assets);

        prefs = Gdx.app.getPreferences("2d-fall");
        bestInf = prefs.getInteger("best_INFINITE", prefs.getInteger("best", 0));
        bestCla = prefs.getInteger("best_CLASSIC", 0);

        world.reset(GameWorld.Mode.INFINITE);
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);
        handleInput();
        if (state == State.PLAY) {
            world.update(dt);
            if (world.dead) gameOver();
        }
        renderer.render(world, state, menuIndex, bestInf, bestCla);
    }

    private void handleInput() {
        switch (state) {
            case READY:
                if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)
                    || Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                    menuIndex = 1 - menuIndex;
                    assets.sndTap.play(0.5f);
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    GameWorld.Mode chosen = menuIndex == 0 ? GameWorld.Mode.INFINITE : GameWorld.Mode.CLASSIC;
                    if (chosen != world.mode) world.reset(chosen);
                    state = State.PLAY;
                    assets.music.play();
                }
                break;
            case PLAY:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                    state = State.PAUSE;
                    assets.music.pause();
                }
                break;
            case PAUSE:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    state = State.PLAY;
                    assets.music.play();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    assets.music.stop();
                    world.reset(world.mode);
                    state = State.READY;
                }
                break;
            case OVER:
                if (Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    world.reset(world.mode);
                    state = State.PLAY;
                    assets.music.play();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    world.reset(world.mode);
                    state = State.READY;
                }
                break;
        }
    }

    private void gameOver() {
        state = State.OVER;
        assets.music.stop();
        int score = world.score();
        boolean newBest = world.mode == GameWorld.Mode.INFINITE ? score > bestInf : score > bestCla;
        if (newBest) {
            if (world.mode == GameWorld.Mode.INFINITE) bestInf = score; else bestCla = score;
            prefs.putInteger("best_" + world.mode.name(), score);
            prefs.flush();
            assets.sndPowerUp.play(0.7f);
        } else {
            assets.sndJump.play(0.5f);
        }
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        assets.dispose();
    }
}
