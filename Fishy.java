import tester.Tester;
import javalib.funworld.*;
import javalib.worldimages.*;
import java.awt.Color;
import java.util.Random;

//wish list:
/*
 * - fix bg fish color thing?
 * - include argument exceptions for invalid types (negative points, illegal sizes, etc.
 * - verify that circle fish are okay
 * - testing!
 * - clean up 
 */

/* KL NOTES & CHANGES:
 * - added TODO stubs to every method that still needs testing
 * - added background width/height to utils class and imported to each relevant class by calling u.bgheight, etc.
 *     to make changing the world size easy
 * - tested all Utils methods
 * - tested IFishPredicate methods
 * - tested all IFish methods
 * - ?? we'll never have yellow bg fish because we stop them from spawning above level 6 - do we want to fix this?
 * - i changed the end game text's color to black - it was hard to read when it was on top of red fish
 * - i changed the comparePositions function to use the distance function to be more accurate 
 * - i added the getPoints method to the Utils class to scale the spawning of the bgfish - small fish are now more likely to spawn than big ones.
 *    this fixes the issue we were having with there being way too many big fish, covering up all the small ones!
 */

//a class to represent the world of a Fish Game
class FishWorldFun extends World {
  //standard height / width for Fish game stored in Utils class for convenience.
  Utils u = new Utils();
  int width = u.BACKGROUND_WIDTH;
  int height = u.BACKGROUND_HEIGHT;
  Player player;
  ILoFish bgfish;

  // constructor
  FishWorldFun(Player player, ILoFish bgfish) {
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
          this.bgfish.filterByPlayer(new ByEaten(), newPlayer).growByAll(newPlayer),
          this.bgfish.filterByPlayer(new ByNotEaten(), newPlayer));
    }
  }

  /*
   * On Tick:
   * - decide if we're going to add a fish
   *    - generate a random fish size and direction
   * - move each active BGFish by 5 pixels in it's current direction
   * - checks if Player is eating any of the BGFish
   */
  //TODO : test
  public World onTick() {
    ILoFish newBGFish = this.bgfish.doOnTick();
    return new FishWorldFun(
        newBGFish.filterByPlayer(new ByEaten(), this.player).growByAll(this.player),
        newBGFish.filterByPlayer(new ByNotEaten(), this.player));
  }

  //The entire background image for this world 
  public WorldImage ocean = new RectangleImage(this.width, this.height, OutlineMode.SOLID,
      Color.BLUE);

  //TODO : test
  // produce the image of this world by adding the player and bgfish to the
  // background image
  public WorldScene makeScene() {
    WorldScene bg = this.getEmptyScene().placeImageXY(this.ocean, this.width / 2, this.height / 2);
    return this.bgfish.placeFish(bg).placeImageXY(this.player.drawFish(), this.player.x,
        this.player.y); 
  }

  // produce the last image of this world by adding text to the image
  public WorldScene lastScene(String s) {
    return this.makeScene().placeImageXY(new TextImage(s, Color.BLACK), 100, 40);
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

  //TODO : test
  // places all of the fish in the list on the given image
  WorldScene placeFish(WorldScene bg);

  // did player get eaten by any of the bgfish?
  boolean playerEaten(Player player);

  //TODO : test
  // determines whether or not to add a new fish to the world and moves every bgfish in the list
  ILoFish doOnTick();

  // grows the player by every bgfish in the list
  Player growByAll(Player player);

  // filters the list by the given predicate and player fish
  ILoFish filterByPlayer(IFishPredicate pred, Player player);
}

// a class to represent an empty list of Fish
class MtLoFish implements ILoFish {

  // moves every fish in the list
  public ILoFish moveAllFish() {
    return this;
  }

  //TODO : test
  // places all of the fish on the list onto bg
  public WorldScene placeFish(WorldScene bg) {
    return bg;
  }

  // did the Player fish get eaten by any of the BGFish?
  public boolean playerEaten(Player player) {
    return false;
  }

  //TODO : test
  // determines if a new fish is created and moves the fish in this
  public ILoFish doOnTick() {
    if (new Random().nextInt(50) % 5 == 0) {
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

// a class to represent a non-empty list of BGFish
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

  //TODO : test
  // places all of the fish on the list onto bg
  public WorldScene placeFish(WorldScene bg) {
    return this.rest.placeFish(bg).placeImageXY(this.first.drawFish(), this.first.x, this.first.y);
  }

  // did the Player fish get eaten by any of the BGFish?
  public boolean playerEaten(Player player) {
    return (!player.canEat(this.first) && player.comparePositions(this.first))
        || this.rest.playerEaten(player);
  }

  //TODO : test
  // determines if a new fish is created and moves the fish in this
  public ILoFish doOnTick() {
    if (new Random().nextInt(50) % 5 == 0) {
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
class ByEaten implements IFishPredicate {
  // will first eat second?
  public boolean apply(AFish first, AFish second) {
    return first.canEat(second) && first.comparePositions(second);
  }
}

//a class to ask if the first fish has not eaten the second
class ByNotEaten implements IFishPredicate {
  //will first not eat second?
  public boolean apply(AFish first, AFish second) {
    return !(first.canEat(second) && first.comparePositions(second));
  }
}

//represents a fish in the game
interface IFish {
  Utils u = new Utils();
  int BACKGROUND_WIDTH = u.BACKGROUND_WIDTH;
  int BACKGROUND_HEIGHT = u.BACKGROUND_HEIGHT;

  // can this fish eat that fish?
  boolean canEat(AFish that);

  // are this fish and that fish touching?
  boolean comparePositions(AFish that);

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
    return Math.sqrt(Math.pow(this.x - that.x, 2) 
        + Math.pow(this.y - that.y, 2)) <= this.size + that.size;
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
    boolean right = (new Random().nextInt(2) == 0);
    this.x = u.placeX(right);
    this.y = new Random().nextInt(BACKGROUND_HEIGHT);
    this.size = u.getSize(points);
    this.points = u.getPoints(new Random().nextInt(11));
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
  int BACKGROUND_WIDTH = 400;
  int BACKGROUND_HEIGHT = 200;
  
  // sets the color for fish based on points
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

  // sets the size of a fish based on it's point value
  /* 1: 0- 50
   * 2: 51 - 100
   * 3: 101 - 150
   * 4: 151 - 200
   * 5: 201 - 250
   * 6: 251 - 300
   * 7: 301 - 350
   * 8: 351 - 400
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
      return this.BACKGROUND_WIDTH;
    }
  }
  
  // out of 11: for 0-2, size 1; 3-5 size 2; 6-7 size 3; 8 size 4; 9 size 5; 10 size 6;
  // weighs the probability for randomly generating bgfish point values
  int getPoints(int generator) {
    if (generator <= 2) {
      return 25;
    }
    else if (generator <= 5) {
      return 75;
    }
    else if (generator <= 7) {
      return 125;
    }
    else if (generator == 8) {
      return 175;
    }
    else if (generator == 9) {
      return 225;
    }
    else {
      return 275;
    }
  }
}

class ExamplesFish {
  //background : width 400, height 200 
  //examples of Player fish : ALL HAVE COLOR = GREEN
  Player playerStart = new Player(); // radius 20, posn(200, 100), pts = 51
  Player pWin = new Player(100, 100, 400); // radius 80 posn(100, 100) pts = 400 -> you win? 
  Player pOffRight = new Player(430, 100, 125); // radius 30, posn(430, 100), pts = 125 -> off - screen?
  Player pOffLeft = new Player(-30, 100, 125); // radius 30, posn(-30, 100), pts = 125 -> off - screen?
  Player pOffUp = new Player(100, -30, 125); // radius 30, posn(100, -30), pts = 125 -> off - screen?
  Player pOffDown = new Player(100, 230, 125); // radius 30, posn(100, 230), pts = 125 -> off - screen?
  Player p1 = new Player(100, 100, 50); // radius 10, posn(100, 100), pts = 50
  Player p2 = new Player(100, 100, 100); // radius 20, posn(100, 100), pts = 100
  Player p3 = new Player(100, 100, 150); // radius 30, posn(100, 100), pts = 150
  Player p4 = new Player(100, 100, 200); // radius 40, posn(100, 100), pts = 200
  Player p5 = new Player(100, 100, 250); // radius 50, posn(100, 100), pts = 250
  Player p6 = new Player(100, 100, 300); // radius 60, posn(100, 100), pts = 300
  Player p7 = new Player(100, 100, 350); // radius 70, posn(100, 100), pts = 350
  
  //examples of BGFish
  BGFish bgSize1 = new BGFish(100, 100, 25, true); //radius 10, posn(100, 100), pts = 25, traveling right, isOff F, color = Orange
  BGFish bgSize2 = new BGFish(420, 100, 75, true); //radius 20, posn(420, 100) pts = 75, traveling right, isOff T, color = Orange
  BGFish bgSize3 = new BGFish(-25, 100, 125, false); // radius 30, posn(-25, 100), pts = 125, traveling left, isOff F, color = Red
  BGFish bgSize4 = new BGFish(300, 100, 125, false); // radius 30, posn(-25, 100), pts = 125, traveling left, isOff F, color = Red
  BGFish bgSize6 = new BGFish(100, 140, 275, false); // radius 60, posn(-25, 100), pts = 275, traveling left, isOff F, color = Magenta
  BGFish bgSize8 = new BGFish(300, 100, 375, false); // radius 80, posn(-25, 100), pts = 375, traveling left, isOff F, color = Yellow
  
  //for checking closeness to player of size 20 fish sitting at (100, 100)
  BGFish bg1Edge = new BGFish(100, 130, 25, true); //radius 10, posn(90, 110), pts = 25, traveling right, isOff F, color = Orange
  BGFish bg1On = new BGFish(95, 105, 25, true); //radius 10, posn(95, 105), pts = 25, traveling right, isOff F, color = Orange
  BGFish bg1Off = new BGFish(140, 140, 25, false); //radius 10, posn(140, 140), pts = 25, traveling left, isOff F, color = Orange
  BGFish bg2Edge = new BGFish(140, 100, 100, true); //radius 20, posn(120, 80), pts = 100, traveling right, isOff F, color = Orange
  BGFish bg2On = new BGFish(95, 105, 100, false); //radius 20, posn(95, 105), pts = 100, traveling left, isOff F, color = Orange
  BGFish bg2Off = new BGFish(70, 70, 100, true); //radius 20, posn(70, 70), pts = 100, traveling right, isOff F, color = Orange
  BGFish bg3Edge = new BGFish(100, 150, 150, false); //radius 30, posn(130, 130), pts = 150, traveling left, isOff F, color = Red
  BGFish bg3On = new BGFish(95, 105, 150, true); //radius 30, posn(95, 105), pts = 150, traveling right, isOff F, color = Red
  BGFish bg3Off = new BGFish(40, 150, 150, true); //radius 30, posn(40, 150), pts = 150, traveling right, isOff F, color = Red
      
  //ILoFish examples
  ILoFish mt = new MtLoFish();
  ILoFish bgList1 = new ConsLoFish(this.bg1Edge, this.mt);
  ILoFish bgList2 = new ConsLoFish(this.bg2Edge, this.bgList1);
  ILoFish bgList3 = new ConsLoFish(this.bg3Edge, this.bgList2);
  ILoFish bgList4 = new ConsLoFish(this.bg2On, this.bgList3);
  ILoFish bgList5 = new ConsLoFish(this.bg3Off, this.bgList4);
  
  ILoFish bgList5NotEatenp2 = new ConsLoFish(this.bg3Off, 
      new ConsLoFish(this.bg2On, 
          new ConsLoFish(this.bg3Edge, 
              new ConsLoFish(this.bg2Edge, this.mt))));
  
  ILoFish bgList5NotEatenp3 = new ConsLoFish(this.bg3Off, 
      new ConsLoFish(this.bg3Edge, this.mt));
  
  ILoFish bgList5Eatenp3 = new ConsLoFish(this.bg2On, 
      new ConsLoFish(this.bg2Edge, new ConsLoFish(this.bg1Edge, this.mt)));
  
  //examples for moving fish 
  ILoFish bgList1BeforeMoved = new ConsLoFish(new BGFish(60, 150, 150, true), this.mt);
  ILoFish bgList1Moved = new ConsLoFish(new BGFish(65, 150, 150, true), this.mt);
  ILoFish bgList2BeforeMoved = new ConsLoFish(new BGFish(100, 150, 100, false), this.bgList1BeforeMoved);
  ILoFish bgList2Moved = new ConsLoFish(new BGFish(95, 150, 100, false), this.bgList1Moved);
  ILoFish bgList3BeforeMoved = new ConsLoFish(new BGFish(370, 150, 150, true), this.bgList2BeforeMoved);
  ILoFish bgList3Moved = new ConsLoFish(new BGFish(375, 150, 150, true), this.bgList2Moved);
  ILoFish bgList4BeforeMoved = new ConsLoFish(new BGFish(-30, 150, 150, false), this.bgList3BeforeMoved);
  ILoFish bgList5BeforeMoved = new ConsLoFish(new BGFish(100, 150, 150, true), this.bgList4BeforeMoved);
  ILoFish bgList5Moved = new ConsLoFish(new BGFish(105, 150, 150, true), this.bgList3Moved);
  
  //World examples
  FishWorldFun emptyWorld = new FishWorldFun(p1, mt);
  FishWorldFun world1 = new FishWorldFun(p2, bgList1);
  FishWorldFun worldWin = new FishWorldFun(pWin, bgList5);
  FishWorldFun worldEaten = new FishWorldFun(p1, bgList5);
  FishWorldFun worldEats = new FishWorldFun(p6, bgList1);
  FishWorldFun worldEatsAfter = new FishWorldFun(new Player(105, 100, 305), mt);


  //Utils examples
  Utils u = new Utils();
  
  //IFishPredicate examples
  IFishPredicate byEaten = new ByEaten();
  IFishPredicate byNotEaten = new ByNotEaten();
  
//  //the ocean 
//  //The entire background image for this world 
//  public WorldScene ocean = new RectangleImage(400, 200, OutlineMode.SOLID,
//      Color.BLUE);
//    
  
  boolean testWorld(Tester t) {
  //run the game
  FishWorldFun w = new FishWorldFun(this.playerStart, this.mt);
  return false; //w.bigBang(400, 200, 0.3);
  }
  
  /*
   * BEGIN FISHWORLDFUN TESTING
   */
  
  //test onKeyEvent method
  boolean testOnKeyEvent(Tester t) {
    return t.checkExpect(this.emptyWorld.onKeyEvent("x"), this.emptyWorld.endOfWorld("Goodbye"))
        && t.checkExpect(this.world1.onKeyEvent("up"), new FishWorldFun(new Player(100, 95, 100), this.bgList1))
        && t.checkExpect(this.world1.onKeyEvent("left"), new FishWorldFun(new Player(95, 100, 100), this.bgList1))
        && t.checkExpect(this.world1.onKeyEvent("down"), new FishWorldFun(new Player(100, 105, 105), this.mt))
        && t.checkExpect(this.world1.onKeyEvent("right"), new FishWorldFun(new Player(105, 100, 100), this.bgList1))
        && t.checkExpect(this.world1.onKeyEvent("d"), new FishWorldFun(new Player(100, 100, 105), this.mt))
//        && t.checkExpect(this.worldEaten.onKeyEvent("up"), this.worldWin.makeScene().placeImageXY(
//            new TextImage("You've been eaten! You lose!", Color.BLACK), 100, 40))
//        && t.checkExpect(this.worldWin.onKeyEvent(" "), this.worldWin.makeScene().placeImageXY(
//            new TextImage("You win! You're king of the fish.", Color.BLACK), 100, 40)) // should this be passing?
        && t.checkExpect(this.worldEats.onKeyEvent( "right"), this.worldEatsAfter);
  }
  
  //test onTick method
  
  
  //test makeScene method 
//  boolean testMakeScene(Tester t) {
//    return t.checkExpect(this, expected)
//  }
  
  
  //test lastScene method
  boolean testLastScene(Tester t) {
    return t.checkExpect(this.emptyWorld.lastScene("still swimming"),
        this.emptyWorld.makeScene().placeImageXY(new TextImage("still swimming", Color.BLACK), 100, 40))
        && t.checkExpect(this.worldWin.lastScene("You win! You're king of the fish."), 
            this.worldWin.makeScene().placeImageXY(
                new TextImage("You win! You're king of the fish.", Color.BLACK), 100, 40))
        && t.checkExpect(this.worldEaten.lastScene("You win! You're king of the fish."), 
            this.worldWin.makeScene().placeImageXY(
                new TextImage("You've been eaten! You lose!", Color.BLACK), 100, 40));
  }
  
  //test worldEnds method
  boolean testWorldEnds(Tester t) {
    return t.checkExpect(this.emptyWorld.worldEnds(), new WorldEnd(false, this.emptyWorld.makeScene()))
        && t.checkExpect(this.worldWin.worldEnds(),
            new WorldEnd(true, this.worldWin.lastScene("You win! You're king of the fish.")))
        && t.checkExpect(this.world1.worldEnds(), new WorldEnd(false, this.world1.makeScene()))
        && t.checkExpect(this.worldEaten.worldEnds(), 
            new WorldEnd(true, this.worldEaten.lastScene("You've been eaten! You lose!")));
  }
  
  /*
   * BEGIN ILOFISH METHOD TESTING
   */
  
  // test filterByPlayer method
  boolean testFilterByPlayer(Tester t) {
    return t.checkExpect(this.mt.filterByPlayer(this.byEaten, this.p2), this.mt)
        && t.checkExpect(this.mt.filterByPlayer(this.byNotEaten, this.p2), this.mt)
        && t.checkExpect(this.bgList1.filterByPlayer(this.byEaten, this.p2), this.bgList1)
        && t.checkExpect(this.bgList1.filterByPlayer(this.byNotEaten, this.p2), this.mt)
        && t.checkExpect(this.bgList5.filterByPlayer(this.byNotEaten, this.p2), this.bgList5NotEatenp2)
        && t.checkExpect(this.bgList5.filterByPlayer(this.byEaten, this.p2), this.bgList1)
        && t.checkExpect(this.bgList5.filterByPlayer(this.byNotEaten, this.p3), this.bgList5NotEatenp3)
        && t.checkExpect(this.bgList5.filterByPlayer(this.byEaten, this.p3), this.bgList5Eatenp3);
  }
  
  //test growByAll method 
  boolean testGrowByAll(Tester t) {
    return t.checkExpect(this.mt.growByAll(this.p1),this.p1) // tests empty
        && t.checkExpect(this.bgList2.growByAll(this.p2), new Player(100, 100, 125))
        && t.checkExpect(this.bgList5.growByAll(this.p2), new Player(100, 100, 205)) // even eats fish bigger than it 
        && t.checkExpect(this.bgList3.growByAll(p7), new Player(100, 100, 405)); // points above 400
  }
  
//  //test doOnTick method
//  FishWorldFun(Random rand) { 
//    this.rand = rand;
//    return 
//  }
  
  //test playerEaten method 
  boolean testPlayerEaten(Tester t) {
    return t.checkExpect(this.mt.playerEaten(p3), false) // empty
        && t.checkExpect(this.bgList2.playerEaten(p2), true) // same position
        && t.checkExpect(this.bgList1.playerEaten(p2), false) // just touching, but not eaten bc size 
        && t.checkExpect(this.bgList1.playerEaten(p2), false) // touching and sizes the same 
        && t.checkExpect(this.bgList2.playerEaten(p2), true) // just touching x,y and player is smaller 
        && t.checkExpect(this.bgList5.playerEaten(p1), true); // eaten by fish  deeper in list
  }
  
//  //test placeFish method  
//  boolean testPlaceFish(Tester t) {
//    WorldImage ocean = new RectangleImage(400, 200, OutlineMode.SOLID,
//        Color.BLUE);
//   WorldScene bg = this.getEmptyScene().placeImageXY(ocean, 200, 100);
//    return t.checkExpect(this.mt.placeFish(bg), bg)
//        && t.checkExpect(this.bgList1.placeFish(bg), bg.placeImageXY(this.bg1Edge.drawFish(), 90, 110))
//        && t.checkExpect(this.bgList1.placeFish(bg), 
//           bg.placeImageXY(this.bg1Edge.drawFish(), 90, 110).placeImageXY(this.bg2Edge.drawFish(), 120, 80));
        
    
  
  
  //test moveAllFish method 
  boolean testMoveAllFish(Tester t) {
    return t.checkExpect(this.mt.moveAllFish(), this.mt)
        && t.checkExpect(this.bgList1BeforeMoved.moveAllFish(), this.bgList1Moved)
        && t.checkExpect(this.bgList2BeforeMoved.moveAllFish(), this.bgList2Moved)
        && t.checkExpect(this.bgList3BeforeMoved.moveAllFish(), this.bgList3Moved)
        && t.checkExpect(this.bgList4BeforeMoved.moveAllFish(), this.bgList3Moved)
        && t.checkExpect(this.bgList5BeforeMoved.moveAllFish(), this.bgList5Moved);
  }
  
  /*
   * BEGIN IFISH METHOD TESTING
   */
  
  //test comparePositions method
  boolean testComparePositions(Tester t) {
    return t.checkExpect(this.p2.comparePositions(this.bg1Edge), true)
        && t.checkExpect(this.p2.comparePositions(this.bg1Off), false)
        && t.checkExpect(this.p2.comparePositions(this.bg1On), true)
        && t.checkExpect(this.p2.comparePositions(this.bg2Edge), true)
        && t.checkExpect(this.p2.comparePositions(this.bgSize3), false);
  }
  
  // test canEat method
  boolean testCanEat(Tester t) {
    return t.checkExpect(this.p2.canEat(this.bg1Edge), true)
        && t.checkExpect(this.p2.canEat(this.bg2Edge), false)
        && t.checkExpect(this.p2.canEat(this.bg3Edge), false);
  }
  
  // test drawFish method
  boolean testDrawFish(Tester t) {
    return t.checkExpect(this.p2.drawFish(), new CircleImage(20, OutlineMode.SOLID, Color.GREEN))
        && t.checkExpect(this.p7.drawFish(), new CircleImage(70, OutlineMode.SOLID, Color.GREEN))
        && t.checkExpect(this.bg1Edge.drawFish(), new CircleImage(10, OutlineMode.SOLID, Color.ORANGE))
        && t.checkExpect(this.bg3Edge.drawFish(), new CircleImage(30, OutlineMode.SOLID, Color.RED))
        && t.checkExpect(this.bgSize6.drawFish(), new CircleImage(60, OutlineMode.SOLID, Color.MAGENTA))
        && t.checkExpect(this.bgSize8.drawFish(), new CircleImage(80, OutlineMode.SOLID, Color.YELLOW));
  }
  
  /*
   * BEGIN PLAYER METHOD TESTING
   */
  
  // test movePlayer method
  boolean testMovePlayer(Tester t) {
    return t.checkExpect(this.p2.movePlayer("up"), new Player(100, 95, 100)) 
        && t.checkExpect(this.pOffUp.movePlayer("up"), new Player(100, u.BACKGROUND_HEIGHT, 125))
        && t.checkExpect(this.p2.movePlayer( "down"), new Player(100, 105, 100)) 
        && t.checkExpect(this.pOffDown.movePlayer( "down"), new Player(100, 0, 125))
        && t.checkExpect(this.p2.movePlayer("left"), new Player(95, 100, 100)) 
        && t.checkExpect(this.pOffLeft.movePlayer( "left"), new Player(u.BACKGROUND_WIDTH, 100, 125))
        && t.checkExpect(this.p2.movePlayer("right"), new Player(105, 100, 100)) 
        && t.checkExpect(this.pOffRight.movePlayer("right"), new Player(0, 100, 125));
  }
  
  // test getNewPosition method
  boolean testGetNewPosition(Tester t) {
    return t.checkExpect(this.p2.getNewPosition(20, "up"), 20) 
        && t.checkExpect(this.p2.getNewPosition(-20, "up"), -20)
        && t.checkExpect(this.p2.getNewPosition(-40, "up"), u.BACKGROUND_HEIGHT) 
        && t.checkExpect(this.p2.getNewPosition(20, "down"), 20) 
        && t.checkExpect(this.p2.getNewPosition(u.BACKGROUND_HEIGHT + 20, "down"), u.BACKGROUND_HEIGHT + 20)
        && t.checkExpect(this.p2.getNewPosition(u.BACKGROUND_HEIGHT + 40, "down"), 0)
        && t.checkExpect(this.p2.getNewPosition(20, "left"), 20) 
        && t.checkExpect(this.p2.getNewPosition(-20, "left"), -20)
        && t.checkExpect(this.p2.getNewPosition(-40, "left"), u.BACKGROUND_WIDTH) 
        && t.checkExpect(this.p2.getNewPosition(20, "right"), 20) 
        && t.checkExpect(this.p2.getNewPosition(u.BACKGROUND_WIDTH + 20, "right"), u.BACKGROUND_WIDTH + 20)
        && t.checkExpect(this.p2.getNewPosition(u.BACKGROUND_WIDTH + 40, "right"), 0);
  }
  
  // test grow method
  boolean testGrow(Tester t) {
    return t.checkExpect(this.p2.grow(this.bg1Edge), new Player(100, 100, 105))
        && t.checkExpect(this.p6.grow(this.bg3Edge), new Player(100, 100, 330))
        && t.checkExpect(this.playerStart.grow(this.bg1Edge), new Player(200, 100, 56));
  }
  
  // test gameWon method
  boolean testGameWon(Tester t) {
    return t.checkExpect(this.playerStart.gameWon(), false)
        && t.checkExpect(this.pWin.gameWon(), true);
  }
  
  /*
   * BEGIN BGFISH METHOD TESTING
   */
  
  // test moveBGFish method
  boolean testMoveBGFish(Tester t) {
    return t.checkExpect(this.bg1Edge.moveBGFish(), new BGFish(105, 130, 25, true))
        && t.checkExpect(this.bg1Off.moveBGFish(), new BGFish(135, 140, 25, false));
  }
  
  /*
   * BEGIN IFISHPREDICATE METHOD TESTING
   */
  
  //test apply method
  boolean testApply(Tester t) {
    return t.checkExpect(this.byEaten.apply(this.p2, this.bg1Edge), true)
        && t.checkExpect(this.byEaten.apply(this.p2, this.bg1Off), false)
        && t.checkExpect(this.byEaten.apply(this.p2, this.bg1On), true)
        && t.checkExpect(this.byEaten.apply(this.p2, this.bg2Edge), false)
        && t.checkExpect(this.byEaten.apply(this.p2, this.bg2Off), false)
        && t.checkExpect(this.byEaten.apply(this.p2, this.bg2On), false)
        && t.checkExpect(this.byEaten.apply(this.p2, this.bg3Edge), false)
        && t.checkExpect(this.byEaten.apply(this.p2, this.bg3Off), false)
        && t.checkExpect(this.byEaten.apply(this.p2, this.bg3On), false)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg1Edge), false)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg1Off), true)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg1On), false)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg2Edge), true)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg2Off), true)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg2On), true)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg3Edge), true)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg3Off), true)
        && t.checkExpect(this.byNotEaten.apply(this.p2, this.bg3On), true);
  }
  
  /*
   * BEGIN UTILS METHOD TESTING
   */
  
  //test the getColor method
  boolean testGetColor(Tester t) {
   return t.checkExpect(this.u.getColor(0), Color.ORANGE)
       && t.checkExpect(this.u.getColor(50), Color.ORANGE)
       && t.checkExpect(this.u.getColor(100), Color.ORANGE)
       && t.checkExpect(this.u.getColor(150), Color.RED)
       && t.checkExpect(this.u.getColor(200), Color.RED)
       && t.checkExpect(this.u.getColor(250), Color.MAGENTA)
       && t.checkExpect(this.u.getColor(300), Color.MAGENTA)
       && t.checkExpect(this.u.getColor(350), Color.YELLOW)
       && t.checkExpect(this.u.getColor(400), Color.YELLOW);
 }
  
  //test the getSize method
  boolean testGetSize(Tester t) {
   return t.checkExpect(this.u.getSize(0), 10)
       && t.checkExpect(this.u.getSize(50), 10)
       && t.checkExpect(this.u.getSize(100), 20)
       && t.checkExpect(this.u.getSize(150), 30)
       && t.checkExpect(this.u.getSize(200), 40)
       && t.checkExpect(this.u.getSize(250), 50)
       && t.checkExpect(this.u.getSize(300), 60)
       && t.checkExpect(this.u.getSize(350), 70)
       && t.checkExpect(this.u.getSize(400), 80);
 }
  
  //test the placeX method
  boolean testCheckBoundGreater(Tester t) {
   return t.checkExpect(this.u.checkBoundGreater(4, 5, 100), 4)
       && t.checkExpect(this.u.checkBoundGreater(4, 4, 100), 4)
       && t.checkExpect(this.u.checkBoundGreater(4, 3, 100), 100);
 }
  
  //test the placeX method
  boolean testCheckBoundLesser(Tester t) {
   return t.checkExpect(this.u.checkBoundLesser(4, 5, 100), 100)
       && t.checkExpect(this.u.checkBoundLesser(4, 4, 100), 4)
       && t.checkExpect(this.u.checkBoundLesser(4, 3, 100), 4);
 }
 
  // test the placeX method
  boolean testPlaceX(Tester t) {
    return t.checkExpect(this.u.placeX(true), 0)
        && t.checkExpect(this.u.placeX(false), 400);
  }
  
  // test the getPoints method
  boolean testGetPoints(Tester t) {
    return t.checkExpect(this.u.getPoints(0), 25)
        && t.checkExpect(this.u.getPoints(1), 25)
        && t.checkExpect(this.u.getPoints(2), 25)
        && t.checkExpect(this.u.getPoints(3), 75)
        && t.checkExpect(this.u.getPoints(4), 75)
        && t.checkExpect(this.u.getPoints(5), 75)
        && t.checkExpect(this.u.getPoints(6), 125)
        && t.checkExpect(this.u.getPoints(7), 125)
        && t.checkExpect(this.u.getPoints(8), 175)
        && t.checkExpect(this.u.getPoints(9), 225)
        && t.checkExpect(this.u.getPoints(10), 275)
        && t.checkExpect(this.u.getPoints(20), 275);
  }
}
  
