import tester.Tester;
import javalib.funworld.*;
import javalib.worldimages.*;
import java.awt.Color;
import java.util.Random;

//wish list:
/*
 * - figure out how to keep track of BGFish (list?)
 * - make actual fish images
 * - verify that circle fish are okay
 * - add orientation
 * - check syntax for interface variables
 * - add color util
 */

/*
 * On Tick:
 * - decide if we're going to add a fish
 * - generate a random fish size and direction
 * - move each active BGFish by 5 pixels in it's current direction
 */

interface IFish {
  int backgroundWidth = 200;
  int backgroundHeight = 400;
}

abstract class AFish implements IFish {
  int x;
  int y;
  int size; // radius of the "fish"
  Color color;
  int points;

  AFish(int x, int y, int size) {
    Utils u = new Utils();
    this.x = x;
    this.y = y;
    this.size = size;
    this.color = u.getColor(size);
  }
}

class Player extends AFish {
  Player(int x, int y, int size, int points) {
    super(x, y, size);
    this.points = points;
  }

  // convenience constructor for a new game
  Player() {
    super(backgroundWidth / 2, backgroundHeight / 2, 1);
    this.points = 0;
  }

  // moves the player by five pixels in the direction of the ke
  public Player movePlayer(String ke) {
    if (ke.equals("right")) {
      return new Player(this.getNewPosition(this.x + 5, "right"), this.y, this.size, this.points);
    }
    else if (ke.equals("left")) {
      return new Player(this.getNewPosition(this.x - 5, "left"), this.y, this.size, this.points);
    }
    else if (ke.equals("up")) {
      return new Player(this.x, this.getNewPosition(this.y - 5, "up"), this.size, this.points);
    }
    else if (ke.equals("down")) {
      return new Player(this.x, this.getNewPosition(this.y + 5, "down"), this.size, this.points);
    }
    else
      return this;
  }
 
  //wraps position of fish if it goes out of bounds
  int getNewPosition(int input, String dir) {
    if (dir.equals("up")) {
      if (input < -this.size) {
        return backgroundHeight;
      }
      else {
        return input;
      }
    }
    else if (dir.equals("down")) {
      if (input > backgroundHeight + this.size) {
        return 0;
      }
      else {
        return input;
      }
    }
    else if (dir.equals("right")) {
      if (input > backgroundWidth + this.size) {
        return 0;
      }
      else {
        return input;
      }
    }
    else if (dir.equals("left")) {
      if (input < -this.size) {
        return backgroundWidth;
      }
      else {
        return input;
      }
    }
    else
      return input;
  }
}

class BGFish extends AFish {
  boolean isRight; // is the BGFish moving in the rightward direction?

  BGFish(int x, int y, int size, boolean isRight) {
    super(x, y, size);
    this.points = size * 5;
    this.isRight = isRight;
  }

  // continues to move the BGFish in the direction it is facing
  public BGFish moveBGFish() {
    if (this.isRight) {
      return new BGFish(this.x + 5, this.y, this.size, this.isRight);
    }
    else {
      return new BGFish(this.x - 5, this.y, this.size, this.isRight);
    }
  }
}

class Utils {
  // selects color for fish based on size
  // TODO
  Color getColor(int size) {
    return Color.blue;
  }

  // is outside bound?
  boolean outsideBound(int input, int bound) {
    return input > bound;
  }
}