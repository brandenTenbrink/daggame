package com.game.battleofpixels.Scenes;

import java.util.Vector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.game.battleofpixels.R;
import com.game.battleofpixels.Camera;
import com.game.battleofpixels.Constants;
import com.game.battleofpixels.Cursor;
import com.game.battleofpixels.Map;
import com.game.battleofpixels.MessageHandler;
import com.game.battleofpixels.MsgType;
import com.game.battleofpixels.Player;
import com.game.battleofpixels.Preferences;
import com.game.battleofpixels.Regulator;
import com.game.battleofpixels.RenderInitData;
import com.game.battleofpixels.Tile;
import com.game.battleofpixels.AI.Blackboard;
import com.game.battleofpixels.DagActivity.SceneType;
import com.game.battleofpixels.InputDevice.AIInputDevice;
import com.game.battleofpixels.InputDevice.BallInputDevice;
import com.game.battleofpixels.InputDevice.InputDevice;
import com.game.battleofpixels.InputDevice.JoystickInputDevice;
import com.game.battleofpixels.InputDevice.TouchInputDevice;
import com.game.battleofpixels.MessageHandler.MsgReceiver;
import com.game.battleofpixels.PowerUp.PowerUp;
import com.game.battleofpixels.PowerUp.PowerUpManager;
import com.game.battleofpixels.ViewData.MapsImageAdapter;

/**
 * A specific scene for the "Play" screen.
 * @author Ying
 *
 */
public class PlayScene extends Scene 
{
	/**
	 * Specifies the state of the plays scene
	 * @author Ying
	 *
	 */
	public enum LogicState
	{
		/**
		 * When the state is created but not yet ready
		 */
		UNINITIALIZED,
		
		/**
		 * State is ready to run, have fun kids!
		 */
		PLAYING,
		
		/**
		 * Game paused
		 */
		PAUSED
	}
	
	/**
	 * Current state of the game
	 */
	private LogicState gameState;
	
	/**
	 * Game map reference
	 */
	private Map map;
	
	/**
	 * Flag that indicates whether to show the tilemap
	 */
	private boolean mShowTileMap;
	
	/**
	 * List of all the players in the game
	 */
	private Vector<Player> players;
	
	/**
	 * Sends a message when a trackball message arrives via the handler
	 */
	public Handler trackballEvent;
	
	/**
	 * Sends a message when a touch message arrives via the handler
	 */
	public Handler touchEvent;
	
	/**
	 * Sends a message when a touch message arrives via the handler
	 */
	public Handler leftJoystickEvent;
	
	/**
	 * Sends a message when a touch message arrives via the handler
	 */
	public Handler rightJoystickEvent;
	
	/**
	 * Keeps the gameplay fps stable.
	 */
	private Regulator gameplayRegulator;
	
	/**
	 * Keeps the update of the zoom funcion much lower than gameplay.
	 */
	private Regulator cameraZoomRegulator;
	
	/**
	 * Keeps the update of the message manager at a fixed speed
	 */
	private Regulator messageManagerRegulator;
	
	/**
	 * Current map file ID
	 */
	private int mapFile;
	
	/**
	 * Current tile map file ID
	 */
	private int tileMapFile;
	
	/**
	 * Manager for all the PowerUps available
	 */
	private PowerUpManager powerUpManager;
	
	/**
	 * Used for keeping track of how much each update cycle takes
	 */
	private long startTime = 0;
	
	/**
	 * Initializes and sets the handler callback.
	 */
	public PlayScene()
	{
		super();
		
		int currentMap = Preferences.Get().multiplayerGame ? Preferences.Get().multiCurrentMap : Preferences.Get().singleCurrentMap;
		
		this.gameState = LogicState.UNINITIALIZED;
		this.players = new Vector<Player>();
		this.trackballEvent = null;
		this.touchEvent = null;
		this.leftJoystickEvent = null;
		this.rightJoystickEvent = null;
		this.gameplayRegulator = new Regulator(60);
		this.cameraZoomRegulator = new Regulator(1);
		this.messageManagerRegulator = new Regulator(1);
		this.mapFile =  MapsImageAdapter.getImageID(currentMap);
		this.tileMapFile = MapsImageAdapter.getTilemapID(currentMap);
		this.powerUpManager = new PowerUpManager(this);
		
		CreatePlayers();
		
		this.map = null;

		this.handler = new Handler() 
		{
	        public void handleMessage(Message msg) 
	        {
	        	if(msg.what == MsgType.TOUCH_EVENT.ordinal())
	        	{       		
	        		if( touchEvent != null )
	        		{
	        			// If there is some input event registered to the touch events
	        			// send him the message. Otherwise we ignore it.
	        			touchEvent.sendMessage(touchEvent.obtainMessage(MsgType.TOUCH_EVENT.ordinal(), msg.obj));
	        		}
	        		if( leftJoystickEvent != null)
	        		{
	        			// If there is some input event registered to the touch events
	        			// send him the message. Otherwise we ignore it.
	        			leftJoystickEvent.sendMessage(leftJoystickEvent.obtainMessage(MsgType.TOUCH_EVENT.ordinal(), msg.obj));
	        		}
	        		if( rightJoystickEvent != null)
	        		{
	        			// If there is some input event registered to the touch events
	        			// send him the message. Otherwise we ignore it.
	        			rightJoystickEvent.sendMessage(rightJoystickEvent.obtainMessage(MsgType.TOUCH_EVENT.ordinal(), msg.obj));
	        		}
	        	}
	        	else if(msg.what == MsgType.REPLY_WCS_TRANSFORM_REQUEST.ordinal())
				{
	        		if( touchEvent != null )
	        		{
	        			touchEvent.sendMessage(touchEvent.obtainMessage(MsgType.REPLY_WCS_TRANSFORM_REQUEST.ordinal(), msg.obj));
	        		}
				}	        	
	        	else if(msg.what == MsgType.TRACKBALL_EVENT.ordinal())
				{	        		
	        		if( trackballEvent != null )
	        		{
	        			// If there is some input event registered to the trackball events
	        			// send him the message. Otherwise we ignore it.
	        			trackballEvent.sendMessage(trackballEvent.obtainMessage(MsgType.TRACKBALL_EVENT.ordinal(), msg.obj));
	        		}
				}
	        	else if (msg.what == MsgType.SCENE_CALL_START.ordinal())
	        	{
	        		Start();
	        	}
	        	else if(msg.what == MsgType.STOP_SCENE.ordinal())
	        	{
	        		runScene = false;
	        	}    	
	        	else if(msg.what == MsgType.RENDERER_INITIALIZATION_DONE.ordinal())
	        	{
	        		// The renderer is done, so start has been done as well.
	        		if(gameState == LogicState.UNINITIALIZED)
	        		{
	        			Camera.Get().SetCameraZLimits();
	        			
	        			// If a traceview trace is required, start tracing
	        			if(Constants.TracertProfiler)
	        			{
	        				Debug.startMethodTracing("trace");
	        			}
	        			
	        			gameState = LogicState.PLAYING;
	        		}
	        	}
	        	else if(msg.what == MsgType.PAUSE_GAME.ordinal())
	        	{
	        		if(gameState == LogicState.PLAYING)
	        		{
	        			gameState = LogicState.PAUSED;
	        		}
	        	}
	        	else if(msg.what == MsgType.UNPAUSE_GAME.ordinal())
	        	{
	        		Log.i("PlayScene", "Unpause");
	        		if(gameState == LogicState.PAUSED)
	        		{
	        			gameState = LogicState.PLAYING;
	        		}
	        	}
	        	// Asking the PowerUpManager to remove a specific PowerUp
	        	else if(msg.what == MsgType.STOP_DISPLAYING_POWERUP.ordinal())
	        	{
	        		if(powerUpManager != null)
	        		{
	        			powerUpManager.RemovePoweUp((PowerUp) msg.obj);
	        		}
	        	}
	        }
	    };	  
	    
	    //Log.i("PlayScene", "PlayScene constructed");
	}

	@Override
	public void End() 
	{
		
	}

	/**
	 * Initializes the play scene data.
	 * Loaded async to allow the loading dialog to function.
	 */
	@Override
	public void Start() 
	{
		final RenderInitData renderInitData = new RenderInitData();
		
		// A thread for background loading of the map
		Thread mapLoadThread = new Thread() 
		{
            public void run() 
            {
            	map = new Map(refActivity,mapFile, tileMapFile);
        		renderInitData.SetMap(map);
        		
        		renderInitData.SetPlayers(players);
        		
        		renderInitData.SetCursorBitmap(CreateBitmap(R.drawable.cursor3));
        		renderInitData.SetCombatBitmap(CreateBitmap(R.drawable.smoke));
        		renderInitData.SetCursorShadowBitmap(CreateBitmap(R.drawable.cursorshadow));
        		renderInitData.SetJoysticMainBitmap(CreateBitmap(R.drawable.joystickmain));
        		renderInitData.SetJoystickSmallBitmap(CreateBitmap(R.drawable.joysticksecondary));
        		renderInitData.SetPowerUpBitmap(CreateBitmap(R.drawable.powerup2));
        		renderInitData.SetPowerUpBitmap(CreateBitmap(R.drawable.powerup3));
        		renderInitData.SetPowerUpBitmap(CreateBitmap(R.drawable.powerup4));
        		renderInitData.SetPowerUpShadowBitmap(CreateBitmap(R.drawable.powerupshadow));
        		renderInitData.SetPowerUpShadowBitmap(CreateBitmap(R.drawable.powerupshadow3));
        		renderInitData.SetPowerUpShadowBitmap(CreateBitmap(R.drawable.powerupshadow4));
        		
            	// Set the initial pos for all the cursors
        		for(int i= 0; i < players.size(); i++)
        		{
        			players.elementAt(i).SetCursorInitialPos();
        			players.elementAt(i).SetInitialTile(map);
        		}
        		
        		// Send all the cursors
        		Vector<Cursor> cursors = new Vector<Cursor>();
        		for(int i = 0; i < players.size(); i++)
        		{
        			cursors.add(players.elementAt(i).GetCursor());
        		}
        		renderInitData.SetCursors(cursors);
        		
        		// Set blackboard data for the AI
        		Blackboard.players = players;
        		Blackboard.map = map;
        		Blackboard.powerUpManager = powerUpManager;
        		
        		// Set the tile index vector
        		Tile.InitIndexVector(players.size());
        		
        		// Done initializing logic, get the word out to the renderer.
        		MessageHandler.Get().Send(MsgReceiver.RENDERER, MsgType.INITIALIZE_RENDERER, renderInitData); 
        		//Log.i("PlayScene", "Start function finished");
            }
        };
        mapLoadThread.start();		
	}
	
	private Bitmap CreateBitmap(int resource)
	{
		/*
		 // This will tell the BitmapFactory to not scale based on the device's pixel density:
	    BitmapFactory.Options opts = new BitmapFactory.Options();
	    opts.inScaled = false;
	    
	    // We need to flip the textures vertically:
	    Matrix flip = new Matrix();
	    flip.postScale(1f, -1f);
	    
	    // Load up, and flip the texture:
	    Bitmap temp = BitmapFactory.decodeResource(this.refActivity.getResources(), resource, opts);
	    Bitmap bmp = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), flip, true);
	    temp.recycle();
	    
	    return bmp;
	    */
		return BitmapFactory.decodeResource(this.refActivity.getResources(), resource);
	}

	/**
	 * Updates the game each update step until the thread is stopped
	 */
	@Override
	public void Update() 
	{
		if(Constants.RenderTakeExtraLogicCycles)
		{
			startTime = System.currentTimeMillis();
		}
		
		
		
		// Logic not dependent on game state
		if(Constants.OnScreenProfiler)
		{
			MessageHandler.Get().Send(MsgReceiver.ACTIVITY, MsgType.UPDATE_LOGIC_PROFILER);
		}
		
		// Update the message handler.
		if(messageManagerRegulator.IsReady())
		{
			MessageHandler.Get().Update();
		}
		
		// Logic only to run in playing (un-paused) mode
		if(SceneReady())
		{		
			Gameplay();
		}
		
		if(Constants.RenderTakeExtraLogicCycles)
		{
			// Takes the time left to complete a cycle at approximately
			// 70 fps, and sets the thread to sleep that time, so we don't 
			// over-tax the system
			long totalTime = System.currentTimeMillis() - startTime;
			//Log.i("A", ""+ totalTime);
			
			if( totalTime < 14)
			{
				try {
		             Thread.sleep(14 - totalTime);
		         } 
				catch (InterruptedException e) 
		         {
		             // Interruptions here are no big deal.
		         }
			}
		}
	}
	
	/**
	 * Logic to be called only when the gameState is "Playing"
	 * Normal game logic goes here, as it will automatically stop updating
	 * when the game state changes.
	 */
	private void Gameplay()
	{
		if(!gameplayRegulator.IsReady())
		{
			return;
		}
		
		Tile.ShuffleIndexVector();
		
		PrepareUpdate();
		
		for(int i = 0; i < this.players.size(); i++)
		{
			this.players.elementAt(i).Update();			
		}
		
		if(Camera.Get() != null)
		{
			if(cameraZoomRegulator.IsReady())
			{
				Camera.Get().ZoomOnPlayers(players);
			}
			
			Camera.Get().Update();
		}
		
		if(	(Preferences.Get().multiplayerGame && Preferences.Get().multiPowerups) ||
			(!Preferences.Get().multiplayerGame && Preferences.Get().singlePowerups))
		{
			this.powerUpManager.Update();
		}
		
		this.map.Update(this.players);
		
		if(GameHasBeenWon())
		{
			MessageHandler.Get().Send(MsgReceiver.ACTIVITY, MsgType.ACTIVITY_CHANGE_SCENE, SceneType.GAMEOVER_SCENE.ordinal());
		}
	}
	
	/**
	 * Prepares everything that needs to be prepared before the update step
	 */
	private void PrepareUpdate()
	{
		for(int i = 0; i < this.players.size(); i++)
		{
			this.players.elementAt(i).Prepare();			
		}
	}
	
	/**
	 * Creates the list of players and their InputDevices
	 */
	private void CreatePlayers()
	{
		if(Preferences.Get().multiplayerGame)
		{
			// Multiplayer game
			
			// Player 1 gets to choose input device
			Player player1 = null;
			Player player2 = null;
			InputDevice player1ID = null;
			InputDevice player2ID = null;
			
			/* Dissabled for now
			switch (Preferences.Get().multiControlMode) 
			{ 
				case 0:
					// Touch and Trackball
					player1ID = new TouchInputDevice(this);
					player2ID = new BallInputDevice(this);
					break;
				
				case 1:
					// Dual joystick
					player1ID = new JoystickInputDevice(this, JoystickInputDevice.LEFT);
					player2ID = new JoystickInputDevice(this, JoystickInputDevice.RIGHT);
					break;
	
				default:
					Log.e("PlayScene", "Input device requested for player not implemented yet!");				
					break;
			}
			*/
			player1ID = new JoystickInputDevice(this, JoystickInputDevice.LEFT);
			player2ID = new JoystickInputDevice(this, JoystickInputDevice.RIGHT);
			
			player1 = new Player(0, player1ID, true, Preferences.Get().multiPlayer1Color);
			this.players.add(player1);
			
			player2 = new Player(1, player2ID, true, Preferences.Get().multiPlayer2Color);
			this.players.add(player2);
			
			// Add all the opponents
			Player newPlayer = null;
			int j = 0;
			for(int i = 0; i < Preferences.Get().multiNumberOpponents; i++ )
			{
				boolean done = false;
				for(; j < Constants.MaxPlayers && !done; j++){
					if(j != Preferences.Get().multiPlayer1Color && j != Preferences.Get().multiPlayer2Color){
						newPlayer = new Player(i+2, new AIInputDevice(this), false, j);
						this.players.add(newPlayer);
						done = true;
					}	
				}
			}
			
		}
		else
		{
			// Single player game
			Player newPlayer;
			
			// Add player 1, input device checked from Preferences
			InputDevice inputDevice = null;
			
			switch (Preferences.Get().singleControlMode) 
			{
				case 0:
					// Touch mode
					inputDevice = new TouchInputDevice(this);
					break;
				
				case 1:
					// Trackball
					inputDevice = new BallInputDevice(this);
					break;
				
				case 2:
					// Joystick
					inputDevice = new JoystickInputDevice(this, JoystickInputDevice.LEFT);
					break;
	
				default:
					Log.e("PlayScene", "Input device requested for player not implemented yet!");				
					break;
			}
			newPlayer = new Player(0, inputDevice, true , Preferences.Get().singlePlayer1Color);
			this.players.add(newPlayer);
			
			boolean done = false;
			int j = 0;
			for(int i = 0; i < Preferences.Get().singleNumberOpponents; i++ )
			{
				done = false;
				for(; j < Constants.MaxPlayers && !done; j++){
					if(j != Preferences.Get().singlePlayer1Color){
						newPlayer = new Player(i+1, new AIInputDevice(this), false, j);
						this.players.add(newPlayer);
						done = true;
					}	
				}
			}		
			
		}
	}
	
	/**
	 * Checks if the scene is ready to execute gameplay
	 * @return True if it is, false if it isn't
	 */
	public boolean SceneReady()
	{
		return (gameState == LogicState.PLAYING); 
	}
	
	/**
	 * Gets the vector of players
	 * @return The player vector
	 */
	public Vector<Player> GetPlayers() { return this.players; }

	/**
	 * Determines if the game is over now
	 * @return True if the game is over, false if it isn't
	 */
	private boolean GameHasBeenWon()
	{
		boolean gameWon = false;
		int playersWithNullPoints = 0;
		int winnerPlayer = -1;
		boolean humanPlayersLeft = false;
		
		// Check everyone's points
		for(int i= 0; i < this.players.size(); i++)
		{
			if(players.elementAt(i).GetTotalDensity() == 0)
			{
				playersWithNullPoints++;
			}
			else
			{
				if(players.elementAt(i).IsHuman()) // At least 1 human left
				{	
					humanPlayersLeft = true;
				}
				winnerPlayer = players.elementAt(i).GetID();
			}
		}
		
		// Everyone has 0 points except 1 player, game over
		if(playersWithNullPoints == (this.players.size() - 1))
		{
			Preferences.Get().winnerPlayerColorIndex = GetPlayerColorIndex(winnerPlayer);
			gameWon = true;
		}
		else if(!humanPlayersLeft) // No humans left, get the AI with the most density
		{
			int minDensity = 0;
			for(int i = 0; i < players.size(); i++)
			{
				if(players.elementAt(i).GetTotalDensity() > minDensity)
				{
					minDensity = players.elementAt(i).GetTotalDensity();
					winnerPlayer = players.elementAt(i).GetID();
				}
			}
			
			Preferences.Get().winnerPlayerColorIndex = GetPlayerColorIndex(winnerPlayer);
			
			gameWon = true;
		}
		
		return gameWon;
	}
	
	private int GetPlayerColorIndex(int id)
	{
		int index = -1;
		for(int j = 0; j < players.size(); j++)
		{
			if(id == players.elementAt(j).GetID())
			{
				index= players.elementAt(j).GetColorIndex();
			}
		}
		return index;
	}
	
	/**
	 * Gets the map.
	 * @return The map.
	 */
	public Map GetMap() {return this.map; }
}
