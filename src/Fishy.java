import tester.Tester;
import javalib.funworld.*;
import javalib.worldimages.*;
import java.awt.Color;
import java.util.Random;

//wish list:
/*
 * - include argument exceptions for invalid types (negative points, illegal sizes, etc.
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
    /*
     * TEMPLATE:
     * Fields:
     * 
     * this.player ... Player
     * this.bgfish ... ILoFish
     * 
     * Methods:
     * this.onKeyEvent(String) ... World
     * this.onTick() ... World
     * this.makeScene() ... WorldScene
     * this.lastScene(String) ... WorldScene
     * this.WorldEnds() ... WorldEnd
     * 
     * Methods for fields:
     * see template for Player
     * see template for ILoFish
     */
    super();
    this.player = player;
    this.bgfish = bgfish;
  }

//Move the Blob when the player presses a key and checks if the player can eat any of the bgfish
  public World onKeyEvent(String ke) {
    if (ke.equals("x"))
      return this.endOfWorld("Goodbye");
    else {
      Player newPlayer = this.player.movePlayer(ke);
      return new FishWorldFun(
          this.bgfish.filterByPlayer(new byEaten(), newPlayer).growByAll(newPlayer),
          this.bgfish.filterByPlayer(new byNotEaten(), newPlayer));
    }
  }

  /*
   * On Tick:
   * - decide if we're going to add a fish
   *    - generate a random fish size and direction
   * - move each active BGFish by 5 pixels in it's current direction
   * - checks if Player is eating any of the BGFish
   */
  public World onTick() {
    ILoFish newBGFish = this.bgfish.doOnTick();
    return new FishWorldFun(
        newBGFish.filterByPlayer(new byEaten(), this.player).growByAll(this.player),
        newBGFish.filterByPlayer(new byNotEaten(), this.player));
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

  // checks whether the player has been eaten or has won
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

  // grows the player by every bgfish in the list
  Player growByAll(Player player);

  // filters the list by the given predicate
  ILoFish filterByPlayer(IFishPredicate pred, Player player);
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
    if (new Random().nextInt(50) % 3 == 0) {
      return new ConsLoFish(new BGFish(), this.moveAllFish());
    }
    else {
      return this.moveAllFish();
    }
  }

  // grows player by every fish in the list
  public Player growByAll(Player player) {
    return player;
  }

  // filter the list by the predicate applied to player
  public ILoFish filterByPlayer(IFishPredicate pred, Player player) {
    return this;
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

  // determines if a new fish is created and moves the fish in this
  public ILoFish doOnTick() {
    if (new Random().nextInt(50) % 3 == 0) {
      return new ConsLoFish(new BGFish(), this.moveAllFish());
    }
    else {
      return this.moveAllFish();
    }
  }

  // grows the player by every bgfish in the list
  public Player growByAll(Player player) {
    return this.rest.growByAll(player.grow(this.first));
  }

  // filters the list by the given predicate applied to player
  public ILoFish filterByPlayer(IFishPredicate pred, Player player) {
    if (pred.apply(player, this.first)) {
      return new ConsLoFish(this.first, this.rest.filterByPlayer(pred, player));
    }
    else {
      return this.rest.filterByPlayer(pred, player);
    }
  }
}

//an interface to hold predicates for list abstractions
interface IFishPredicate {
  // asks a question about two Fish
  boolean apply(AFish first, AFish second);
}

// a class to ask if the first fish has eaten the second
class byEaten implements IFishPredicate {
  // will first eat second?
  public boolean apply(AFish first, AFish second) {
    return first.canEat(second) && first.comparePositions(second);
  }
}

//a class to ask if the first fish has not eaten the second
class byNotEaten implements IFishPredicate {
//will first not eat second?
  public boolean apply(AFish first, AFish second) {
    return !(first.canEat(second) && first.comparePositions(second));
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
    this.color = Color.GREEN;
  }

  // convenience constructor for a new game
  Player() {
    super(BACKGROUND_WIDTH / 2, BACKGROUND_HEIGHT / 2, 51); 
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
      return u.checkBoundLesser(input, - this.size, BACKGROUND_HEIGHT);
    }
    else if (dir.equals("down")) {
      return u.checkBoundGreater(input, BACKGROUND_HEIGHT + this.size, 0);
    }
    else if (dir.equals("right")) {
      return u.checkBoundGreater(input, BACKGROUND_WIDTH + this.size, 0);
    }
    else if (dir.equals("left")) {
      return u.checkBoundLesser(input, - this.size, BACKGROUND_WIDTH);
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
    boolean right = (new Random().nextInt(10) % 2 == 0);
    this.x = u.placeX(right);
    this.y = new Random().nextInt(BACKGROUND_HEIGHT);
    this.size = u.getSize(points);
    this.points = new Random().nextInt(301);
    this.isRight = right;
    this.isOff = (x < 0 - size || x > BACKGROUND_WIDTH + size);
    this.color = u.getColor(points);

  }

  // moves the BGFish by 5 pixels in the direction it is going
  public BGFish moveBGFish() {
    if (this.isRight) {
      return new BGFish(this.x + 5, this.y, this.points, this.isRight);
    }
    else {
      return new BGFish(this.x - 5, this.y, this.points, this.isRight);
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
      return Color.YELLOW;
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
    int scale = 10;
    if (points <= 50) {
      return 1 * scale;
    }
    else if (points <= 100) {
      return 2 * scale;
    }
    else if (points <= 150) {
      return 3 * scale;
    }
    else if (points <= 200) {
      return 4 * scale;
    }
    else if (points <= 250) {
      return 5 * scale;
    }
    else if (points <= 300) {
      return 6 * scale;
    }
    else if (points <= 350) {
      return 7 * scale;
    }
    else {
      return 8 * scale;
    }
  }

  // if input goes outside of bound, returns newPosition
  int checkBoundGreater(int input, int bound, int newPosition) {
    if (input > bound) {
      return newPosition;
    }
    else {
      return input;
    }
  }
  
//if input goes outside of bound, returns newPosition
 int checkBoundLesser(int input, int bound, int newPosition) {
   if (input < bound) {
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

class ExamplesFish {
  //background : width 400, height 200 
  //examples of Player fish : ALL HAVE COLOR = GREEN
  Player playerStart = new Player(); // radius 20, posn(200, 100), pts = 51
  Player p1 = new Player(40, 200, 350); // radius 70, posn(40, 200), pts = 350
  Player p2 = new Player(100, 100, 400); // radius 80 posn(100, 100) pts = 400 -> you win? 
  Player p3 = new Player(100, 100, 100); // radius 20, posn(100, 100), pts = 100
  Player pOffRight = new Player(430, 100, 125); // radius 30, posn(430, 100), pts = 125 -> off - screen?
  Player pOffLeft = new Player(-30, 100, 125); // radius 30, posn(-30, 100), pts = 125 -> off - screen?
  Player pOffUp = new Player(100, -30, 125); // radius 30, posn(100, -30), pts = 125 -> off - screen?
  Player offBelow = new Player(100, 230, 125); // radius 30, posn(100, 230), pts = 125 -> off - screen?
  
  //examples of BGFish
  BGFish bgSize1 = new BGFish(100, 100, 25, true); //radius 10, posn(100, 100), pts = 25, traveling right, isOff F, color = Orange
  BGFish bgSize2Right = new BGFish(82, 100, 75, true);
  BGFish bgSize2 = new BGFish(420, 100, 75, true); //radius 20, posn(420, 100) pts = 75, traveling right, isOff T, color = Orange
  BGFish bgSize3 = new BGFish(-25, 100, 125, false); // radius 30, posn(-25, 100), pts = 125, traveling left, isOff F, color = Red
  BGFish bgSize4 = new BGFish(300, 100, 125, false); // radius 30, posn(-25, 100), pts = 125, traveling left, isOff F, color = Red
      
  //ILoFish examples
  ILoFish mt = new MtLoFish();
  
  boolean testWorld(Tester t) {
//run the game
  FishWorldFun w = new FishWorldFun(this.playerStart, new ConsLoFish(this.bgSize1,
                                                        new ConsLoFish(this.bgSize2Right ,this.mt)));
  return w.bigBang(400, 200, 0.3);
  }
}