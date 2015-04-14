package net.xicp.zyl.me.flappybird;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public class FlappybirdGame extends ApplicationAdapter {
	private static final float BIRD_JUMP_IMPULSE = 350;
	private static final float GRAVITY = -20;
	private static final float BIRD_VELOCITY_X = 200;
	private static final float BIRD_START_Y = 300;
	private static final float BIRD_START_X = 100;
	private static final int GROUND_WIDTH = 336;
	private static final int GROUND_HIGHT = 76;
	private static final int PIPE_GAP_Y = 100;
	private static final int PIPE_GAP_X = 200;
	private static final int PIPE_LENGTH_AT_LEAST = 70;
	SpriteBatch batch;
	TextureRegion ground;
	Sprite sbird;
	Texture background;
	OrthographicCamera camera;
	OrthographicCamera uiCamera;
	Animation bird;
	Vector2 birdVelocity = new Vector2();
	Vector2 birdPosition = new Vector2();
	Vector2 gravity = new Vector2();
	private float birdStateTime;
	private int groundOffsetX;
	private Array<Pipe> pipes = new Array<Pipe>();
	Rectangle rect1 = new Rectangle();
	Rectangle rect2 = new Rectangle();
	TextureRegion gameover;
	TextureRegion ready;
	TextureRegion tutorial;
	GameState gameState = GameState.Tutorial;
	TextureRegion[] digital = new TextureRegion[10];
	TextureRegion[] digital_small = new TextureRegion[10];
	TextureRegion gameOverPanel;
	TextureRegion newTexture;
	int score = 0;
	private float birdRotation;
	private static final String perferencesStr = "net.xicp.zyl.me.flappybird.settings";
	private int highestScore;
	private boolean newHigh = false;
	Sound wing,die,hit,point,swooshing;
	private boolean isDie;
	
	@Override
	public void create() {
		wing = Gdx.audio.newSound(Gdx.files.internal("sounds/Wing.wav"));
		point = Gdx.audio.newSound(Gdx.files.internal("sounds/Point.wav"));
		hit =  Gdx.audio.newSound(Gdx.files.internal("sounds/Hit.wav"));
		die =  Gdx.audio.newSound(Gdx.files.internal("sounds/Die.wav"));
		swooshing = Gdx.audio.newSound(Gdx.files.internal("sounds/Swooshing.wav"));
		batch = new SpriteBatch();
		ground = new TextureRegion(new Texture("images/ground.png"),
				GROUND_WIDTH, GROUND_HIGHT);
		gameOverPanel = new TextureRegion(new Texture("images/gameover-panel.png"),349,177);
		newTexture = new TextureRegion(new Texture("images/gameover-panel.png"),0,182,48,20);
		background = new Texture("images/bg1.png");
		final Texture texture_ui = new Texture("images/ui.png");
		gameover = new TextureRegion(texture_ui, 0, 95, texture_ui.getWidth(),
				62);
		ready = new TextureRegion(texture_ui, texture_ui.getWidth(), 80);
		tutorial = new TextureRegion(texture_ui, 0,174,texture_ui.getWidth(), 147);
		for(int i = 0;i < 10;i++)
		{
			digital[i] = new TextureRegion(new Texture("images/digital.png"),i > 4 ? (i - 5) * 40 :40 * i,i > 4 ? 60 : 0,40,60);
		}
		for(int i = 0;i < 10;i++)
		{
			digital_small[i] = new TextureRegion(new Texture("images/digital-small.png"),i > 4 ? (i - 5) * 25 :25 * i,i > 4 ? 32 : 0,25,32);
		}
		TextureRegion keyframe[] = new TextureRegion[3];
		final Texture texture = new Texture("images/birds.png");
		int bird_width = 34;
		int bird_height = 24;
		int pre_gap = 8;
		int mid_gap = 18;
		int pre_gap_y = 11;
		keyframe[0] = new TextureRegion(texture, pre_gap, pre_gap_y,
				bird_width, bird_height);
		keyframe[1] = new TextureRegion(texture,
				pre_gap + bird_width + mid_gap, pre_gap_y, bird_width,
				bird_height);
		keyframe[2] = new TextureRegion(texture, pre_gap + bird_width * 2
				+ mid_gap * 2 + 1, pre_gap_y, bird_width, bird_height);
		bird = new Animation( 0.05f, keyframe);
		bird.setPlayMode(PlayMode.LOOP);
		gravity.y = GRAVITY;
		camera = new OrthographicCamera();
		camera.setToOrtho(false, background.getWidth(), background.getHeight());
		camera.update();
		uiCamera = new OrthographicCamera();
		uiCamera.setToOrtho(false, background.getWidth(), background.getHeight());
		uiCamera.update();
		resetWorld();
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		updateWorld();
		drawWorld();

	}

	private void resetWorld() {
		birdVelocity.set(BIRD_VELOCITY_X, 0);
		birdPosition.set(BIRD_START_X, BIRD_START_Y);
		gravity.set(0, GRAVITY);
		groundOffsetX = 0;
		score = 0;
		pipes.clear();
		camera.position.x = BIRD_START_X;
		camera.position.y = BIRD_START_Y;
		for (int i = 0; i < 5; i++) {
			int two_pipe_max_length = background.getHeight() - GROUND_HIGHT;
			int pipe_up_length = MathUtils.random((two_pipe_max_length
					- PIPE_GAP_Y - PIPE_LENGTH_AT_LEAST) / 2)
					+ PIPE_LENGTH_AT_LEAST;
			Texture texture_pipeup = new Texture("images/pipeup.png");
			TextureRegion pipeup = new TextureRegion(texture_pipeup,
					texture_pipeup.getWidth(), pipe_up_length);
			TextureRegion pipedown = new TextureRegion(texture_pipeup,
					texture_pipeup.getWidth(), two_pipe_max_length
							- pipe_up_length - PIPE_GAP_Y);
			pipedown.flip(false, true);
			pipes.add(new Pipe(new Vector2(500 + PIPE_GAP_X * i, background
					.getHeight() - pipedown.getRegionHeight()), pipedown, false,true));
			pipes.add(new Pipe(new Vector2(500 + PIPE_GAP_X * i, GROUND_HIGHT),
					pipeup, false,false));
		}
	}

	private void updateWorld() {
		float deltaTime = Gdx.graphics.getDeltaTime();
		birdRotation = -90 * (Math.abs(birdVelocity.y / GRAVITY * deltaTime));
		if(birdVelocity.y > 0)
		{
			birdStateTime += deltaTime;
		}
		if (gameState != GameState.Ready && gameState != GameState.Tutorial)
		{
			birdVelocity.add(gravity);
			birdPosition.mulAdd(birdVelocity, deltaTime);
		}
		camera.position.x = birdPosition.x + background.getWidth() / 2
				- BIRD_START_X;
		if (camera.position.x + background.getWidth() / 2 - groundOffsetX > background
				.getWidth() * 2) {
			groundOffsetX += background.getWidth();
		}
		if (Gdx.input.justTouched()) {
			if (gameState == GameState.Ready)
			{
				gameState = GameState.Start;
				if(newHigh)
				{
					highestScore = score;
					newHigh = false;
				}
			}
			if (gameState == GameState.Start)
			{
				if(birdPosition.y +  bird.getKeyFrame(0).getRegionHeight() < background.getHeight())
				{
					birdVelocity.set(BIRD_VELOCITY_X, BIRD_JUMP_IMPULSE);
				}
				wing.play();
			}
			if(gameState == GameState.GameOver)
			{
				gameState = GameState.Ready;
				resetWorld();
			}
			if(gameState == GameState.Tutorial)
				gameState = GameState.Ready;
		}
		for (Pipe pipe : pipes) {
			if (camera.position.x - background.getWidth() / 2 > pipe.position.x
					+ pipe.image.getRegionWidth()) {
				pipe.position.x += 4 * (PIPE_GAP_X + pipe.image
						.getRegionWidth());
				pipe.counted = false;
			}
			rect1.set(birdPosition.x, birdPosition.y,
					bird.getKeyFrames()[0].getRegionWidth(),
					bird.getKeyFrames()[0].getRegionHeight());
			rect2.set(pipe.position.x, pipe.position.y,
					pipe.image.getRegionWidth(), pipe.image.getRegionHeight());
			if (rect1.overlaps(rect2)) {
				playHitAndDie();
				gameState = GameState.GameOver;
				birdVelocity.x = 0;
			}
			if(birdPosition.x >  pipe.position.x && pipe.counted == false && pipe.isDown) 
			{
				pipe.counted = true;
				score++;
				point.play();
			}
		}
		if (birdPosition.y <= GROUND_HIGHT) {
			playHitAndDie();
			gameState = GameState.GameOver;
			birdVelocity.x = 0;
			birdVelocity.y = 0;
			birdPosition.y = GROUND_HIGHT;
			gravity.y = 0;
			birdRotation = -90;
		}
	}

	private void playHitAndDie() {
		isDie = true;
		if(birdVelocity.x != 0)
		{
			hit.play();
			Timer t = new Timer();
			t.scheduleTask(new Task() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					die.play();
				}
			}, 1.0f);
		}
	}

	private void drawWorld() {
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(background, camera.position.x - background.getWidth() / 2, 0);
		batch.draw(ground, 0, 0);
		batch.draw(ground, groundOffsetX, 0);
		batch.draw(ground, groundOffsetX + GROUND_WIDTH, 0);
		batch.draw(ground, groundOffsetX + GROUND_WIDTH * 2, 0);
		batch.draw(ground, groundOffsetX + GROUND_WIDTH * 3, 0);
		batch.draw(ground, groundOffsetX + GROUND_WIDTH * 4, 0);
		batch.draw(ground, groundOffsetX + GROUND_WIDTH * 5, 0);
		for (Pipe pipe : pipes) {
			batch.draw(pipe.image, pipe.position.x, pipe.position.y);
		}
		batch.draw(bird.getKeyFrame(birdStateTime), birdPosition.x,
				birdPosition.y,bird.getKeyFrame(0).getRegionWidth() / 2,bird.getKeyFrame(0).getRegionHeight() / 2,bird.getKeyFrame(birdStateTime).getRegionWidth(),bird.getKeyFrame(birdStateTime).getRegionHeight(),1,1,birdRotation );
		batch.end();

		uiCamera.update();
		batch.setProjectionMatrix(uiCamera.combined);
		batch.begin();
		if (gameState == GameState.GameOver)
		{
			batch.draw(gameOverPanel,background.getWidth() / 2- gameOverPanel.getRegionWidth()/2,background.getHeight() / 2 - gameOverPanel.getRegionHeight() / 2);
			Preferences p = Gdx.app.getPreferences(perferencesStr);
			String olderScore = p.getString("score","0");
			highestScore = Integer.parseInt(olderScore);
			drawDigtalCenterY(batch,digital_small,score+"",background.getHeight() / 2 + digital_small[0].getRegionHeight() / 2 - 10);
			drawDigtalCenterY(batch,digital_small,highestScore+"",background.getHeight() / 2 - digital_small[0].getRegionHeight()/2 -40);
			if(score > highestScore)
			{
				if(newHigh == false)
				{
					p.putString("score", score+"");
					p.flush();
				}
				newHigh = true;
			}
			if(newHigh)
			{
				batch.draw(newTexture,background.getWidth() / 2 + String.valueOf(score).length() * (digital_small[0].getRegionWidth() / 2),background.getHeight() / 2 - newTexture.getRegionHeight() / 2 + 20);
			}
			batch.draw(
					gameover,
				background.getWidth() / 2 - gameover.getRegionWidth() / 2,
				background.getHeight() / 2 + gameOverPanel.getRegionHeight() / 2);
		}
		if (gameState == GameState.Ready)
			batch.draw(
					ready,
				background.getWidth() / 2 - ready.getRegionWidth() / 2,
				background.getHeight() / 2 - ready.getRegionHeight()
							/ 2);
		if (gameState == GameState.Tutorial)
			batch.draw(
					tutorial,
				background.getWidth() / 2 - tutorial.getRegionWidth() / 2,
				background.getHeight() / 2 - tutorial.getRegionHeight()
							/ 2);
		drawDigtalCenterUp(batch,digital_small,score+"");
		batch.end();
	}
	
	private void drawDigtalCenter(SpriteBatch batch,TextureRegion digital[],String str)
	{
		for(int i =0;i < str.length();i++)
		{
			int index = str.charAt(str.length() - 1 - i) - '0';
			batch.draw(digital[index],background.getWidth() / 2 - digital[index].getRegionWidth() / 2 * (1 + i * 2) + digital[index].getRegionWidth() * (str.length() - 1) / 2,
				background.getHeight() / 2 - digital[index].getRegionHeight() / 2);
		}
	}
	private void drawDigtalCenterUp(SpriteBatch batch,TextureRegion digital[],String str)
	{
		for(int i =0;i < str.length();i++)
		{
			int index = str.charAt(str.length() - 1 - i) - '0';
			batch.draw(digital[index],background.getWidth() / 2 - digital[index].getRegionWidth() / 2 * (1 + i * 2) + digital[index].getRegionWidth() * (str.length() - 1) / 2,
				background.getHeight() - digital[index].getRegionHeight());
		}
	}
	private void drawDigtalCenterY(SpriteBatch batch,TextureRegion digital[],String str,int y)
	{
		for(int i =0;i < str.length();i++)
		{
			int index = str.charAt(str.length() - 1 - i) - '0';
			batch.draw(digital[index],background.getWidth() / 2 - digital[index].getRegionWidth() / 2 * (1 + i * 2) + digital[index].getRegionWidth() * (str.length() - 1) / 2,
					y);
		}
	}
	class Pipe {
		Vector2 position = new Vector2();
		TextureRegion image;
		boolean counted;
		boolean isDown;
		
		public Pipe(Vector2 position, TextureRegion image, boolean counted,boolean isDown) {
			super();
			this.position = position;
			this.image = image;
			this.counted = counted;
			this.isDown = isDown;
		}
	}

	static enum GameState {
		Ready, Start, GameOver,Tutorial
	}
}
