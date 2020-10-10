import tester.Tester;
import javalib.funworld.*;
import javalib.worldimages.*;
import java.awt.Color;
import java.util.Random;

//wish list:
/*
 * - a way grow + delete fish when player eats, and do this for the whole list of bgfish
 *   - on tick 
 *   - on key
 * - make the player fish start out big enough to eat one type of fish
 * - verify that circle fish are okay
 * - testing!
 * - templates & clean up 
 */

//a class to represent the world of a Fish Game
class FishWorldFun extends World {
  int width = 400;
  int height = 200;
  Player player;
  ILoFish bgfish;

  // constructor
  FishWorldFun(Player player, ILoFish bgfish) {
    super();
    this.player = player;
    this.bgfish = bgfish;
  }

//Move the Blob when the player presses a key 
  public World onKeyEvent(String ke) {
    if (ke.equals("x"))
      return this.endOfWorld("Goodbye");
    else
      return new FishWorldFun(this.player.movePlayer(ke), this.bgfish);
  }

  // TODO
  /*
   * On Tick:
   * - decide if we're going to add a fish
   * - generate a random fish size and direction
   * - move each active BGFish by 5 pixels in it's current direction
   */
  public World onTick() {
    return new FishWorldFun(this.player, this.bgfish.doOnTick());
  }

//The entire background image for this world 
  public WorldImage ocean = new RectangleImage(this.width, this.height, OutlineMode.SOLID,
      Color.BLUE);

  // produce the image of this world by adding the player and bgfish to the
  // background image
  public WorldScene makeScene() {
    WorldScene bg = this.getEmptyScene().placeImageXY(this.ocean, this.width / 2, this.height / 2);
    return this.bgfish.placeFish(bg).placeImageXY(this.player.drawFish(), this.player.x,
        this.player.y);
  }

  // produce the last image of this world by adding text to the image
  public WorldScene lastScene(String s) {
    return this.makeScene().placeImageXY(new TextImage(s, Color.red), 100, 40);
  }

  // Check whether the Blob is out of bounds, or fell into the black hole in
  // the middle.
  public WorldEnd worldEnds() {
    // if the player fish gets eaten by a bigger bgfish, stop
    if (this.bgfish.playerEaten(this.player)) {
      return new WorldEnd(true, this.lastScene("You've been eaten! You lose!"));
    }
    // you win if you are the biggest fish in the pond
    if (this.player.gameWon()) {
      return new WorldEnd(true, this.lastScene("You win! You're king of the fish."));
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }
}

//represents an arbitrary list of BGFish 
interface ILoFish {
  // moves every fish in the list
  ILoFish moveAllFish();

  // places all of the fish in the list on the given image
  WorldScene placeFish(WorldScene bg);

  // did player get eaten by any of the bgfish?
  boolean playerEaten(Player player);

  //
  ILoFish doOnTick();
  
  //
  World doOnTick2(Player player);
}

// a class to represent an empty list of Fish
class MtLoFish implements ILoFish {

  // moves every fish in the list
  public ILoFish moveAllFish() {
    return this;
  }

  // places all of the fish on the list onto bg
  public WorldScene placeFish(WorldScene bg) {
    return bg;
  }

  // did the Player fish get eaten by any of the BGFish?
  public boolean playerEaten(Player player) {
    return false;
  }

  // determines if a new fish is created and moves the fish in this
  public ILoFish doOnTick() {
    if (new Random().nextInt(2) == 1) {
      return new ConsLoFish(new BGFish(), this.moveAllFish());
    }
    else {
      return this.moveAllFish();
    }
  }

  @Override
  public World doOnTick2(Player player) {
    return new FishWorldFun(player, this);
  }

}

// a class to represent a non-empty list of Fish
class ConsLoFish implements ILoFish {
  BGFish first;
  ILoFish rest;

  ConsLoFish(BGFish first, ILoFish rest) {
    this.first = first;
    this.rest = rest;
  }

  // moves every fish in the list
  public ILoFish moveAllFish() {
    BGFish movedFish = this.first.moveBGFish();
    if (movedFish.isOff) {
      return this.rest.moveAllFish();
    }
    else {
      return new ConsLoFish(movedFish, this.rest.moveAllFish());
    }
  }

  // places all of the fish on the list onto bg
  public WorldScene placeFish(WorldScene bg) {
    return this.rest.placeFish(bg).placeImageXY(this.first.drawFish(), this.first.x, this.first.y);
  }

  // did the Player fish get eaten by any of the BGFish?
  public boolean playerEaten(Player player) {
    return (!player.canEat(this.first) && player.comparePositions(this.first))
        || this.rest.playerEaten(player);
  }
  
  //TODO
  //determines if a new fish is created and moves the fish in this
  public ILoFish doOnTick() {
    if (new Random().nextInt(2) == 1) {
      return new ConsLoFish(new BGFish(), this.moveAllFish());
    }
    else {
      return this.moveAllFish();
    }
  }

  //TODO
  public World doOnTick2(Player player) {
    if (player.canEat(this.first) && player.comparePositions(this.first)) {
      return new FishWorldFun(player.grow(this.first), this.rest);
    } else {
      return new FishWorldFun(player, new ConsLoFish(this.first, this));
    }
  }
}

//represents a fish in the game
interface IFish {
  int BACKGROUND_WIDTH = 400;
  int BACKGROUND_HEIGHT = 200;

  //
  boolean comparePositions(AFish that);

  //
  boolean canEat(AFish that);

  // draws an image to represent the fish
  WorldImage drawFish();
}

abstract class AFish implements IFish {
  int x;
  int y;
  int size; // radius of the "fish"
  int points;

  AFish(int x, int y, int points) {
    Utils u = new Utils();
    this.x = x;
    this.y = y;
    this.size = u.getSize(points);
    this.points = points;
  }

  // can this fish eat that fish?
  public boolean canEat(AFish that) {
    return this.size > that.size;
  }

  // are this fish and that fish touching?
  public boolean comparePositions(AFish that) {
    return Math.abs(this.x - that.x) < this.size + that.size
        && Math.abs(this.y - that.y) < this.size + that.size;
  }

  // draws the image to represent the fish
  public abstract WorldImage drawFish();

}

class Player extends AFish {
  Color color;

  Player(int x, int y, int points) {
    super(x, y, points);
    this.color = Color.green;
  }

  // convenience constructor for a new game
  Player() {
    super(BACKGROUND_WIDTH / 2, BACKGROUND_HEIGHT / 2, 0);
  }

  // moves the player by five pixels in the direction of the ke
  public Player movePlayer(String ke) {
    if (ke.equals("right")) {
      return new Player(this.getNewPosition(this.x + 5, "right"), this.y, this.points);
    }
    else if (ke.equals("left")) {
      return new Player(this.getNewPosition(this.x - 5, "left"), this.y, this.points);
    }
    else if (ke.equals("up")) {
      return new Player(this.x, this.getNewPosition(this.y - 5, "up"), this.points);
    }
    else if (ke.equals("down")) {
      return new Player(this.x, this.getNewPosition(this.y + 5, "down"), this.points);
    }
    else
      return this;
  }

  // wraps position of fish if it goes out of bounds
  int getNewPosition(int input, String dir) {
    Utils u = new Utils();
    if (dir.equals("up")) {
      return u.checkBound(-input, this.size, BACKGROUND_HEIGHT);
    }
    else if (dir.equals("down")) {
      return u.checkBound(input, BACKGROUND_HEIGHT + this.size, 0);
    }
    else if (dir.equals("right")) {
      return u.checkBound(input, BACKGROUND_WIDTH + this.size, 0);
    }
    else if (dir.equals("left")) {
      return u.checkBound(-input, this.size, BACKGROUND_WIDTH);
    }
    else
      return input;
  }

  // grows the player fish in size when it eats a BGFish
  Player grow(BGFish that) {
    return new Player(this.x, this.y, this.points + that.points / 5);
  }

  // creates an image to represent the fish
  public WorldImage drawFish() {
    return new CircleImage(this.size, OutlineMode.SOLID, this.color);
  }

  // have you won?
  boolean gameWon() {
    return this.points >= 400;
  }
}

class BGFish extends AFish {
  boolean isRight; // is the BGFish moving in the rightward direction?
  boolean isOff; // is the BGFish off the screen?
  Color color;

  Utils u = new Utils();

  BGFish(int x, int y, int points, boolean isRight) {
    super(x, y, points);
    this.color = u.getColor(points);
    this.isRight = isRight;
    this.isOff = (x < 0 - size || x > BACKGROUND_WIDTH + size);
  }

  // for making a new random BGFish
  BGFish() {
    super(0, 0, 0);
    this.x = u.placeX(this.isRight);
    this.y = new Random().nextInt(BACKGROUND_HEIGHT);
    this.size = u.getSize(points);
    this.points = new Random().nextInt(301);
    this.isRight = new Random().nextInt(2) == 0;
    this.isOff = (x < 0 - size || x > BACKGROUND_WIDTH + size);
    this.color = u.getColor(points);

  }

  // moves the BGFish by 5 pixels in the direction it is going
  public BGFish moveBGFish() {
    if (this.isRight) {
      return new BGFish(this.x + 5, this.y, this.size, this.isRight);
    }
    else {
      return new BGFish(this.x - 5, this.y, this.size, this.isRight);
    }
  }

  // creates an image to represent the fish
  public WorldImage drawFish() {
    return new CircleImage(this.size, OutlineMode.SOLID, this.color);
  }
}

class Utils {
  // selects color for fish based on points
  Color getColor(int points) {
    if (points <= 100) {
      return Color.ORANGE;
    }
    else if (points <= 200) {
      return Color.RED;
    }
    else if (points <= 300) {
      return Color.MAGENTA;
    }
    else {
      return Color.BLUE;
    }
  }

  // sets the size based on the point value
  /* 1: 0- 50
   * 2: 51 - 100
   * 3: 101 - 150
   * 4: 151 - 200
   * 5: 201 - 250
   * 6: 251 - 300
   * 7: 301 - 350
   * 8: 351 - 400
   * TODO : scale for display purposes
   */
  int getSize(int points) {
    if (points <= 50) {
      return 1;
    }
    else if (points <= 100) {
      return 2;
    }
    else if (points <= 150) {
      return 3;
    }
    else if (points <= 200) {
      return 4;
    }
    else if (points <= 250) {
      return 5;
    }
    else if (points <= 300) {
      return 6;
    }
    else if (points <= 350) {
      return 7;
    }
    else {
      return 8;
    }
  }

  // if input goes outside of bound, returns newPosition
  int checkBound(int input, int bound, int newPosition) {
    if (input > bound) {
      return newPosition;
    }
    else {
      return input;
    }
  }

  // places the starting point of the fish at the very left or very right of the
  // scene
  int placeX(boolean isRight) {
    if (isRight) {
      return 0;
    }
    else {
      return 400;
    }
  }
}