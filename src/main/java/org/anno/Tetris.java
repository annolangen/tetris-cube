package org.anno;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collection;

/**
 * Computes all solutions for the Tetris cube puzzle. Pre-computes all placements for each piece represented as a 
 * 64-bit long. Then explores all ways to pick a placement for each piece without intersecting. Tests intersection by
 * looking for non-zero result of AND-ing the bit masks. Avoids generating duplicates by omitting rotations for the 
 * first placed piece and duplicate placements that arise from the piece symmetries.
 *
 * Uses several representations: Corner Coordinates, Centered Coordinates, Index/Extent. In the Corner Coordinate 
 * system the big, transparent puzzle enclosure is divided in 4 x 4 x 4 cubes with coordinate values 0-3 in three 
 * dimensions. The pieces are defined by listing the corner coordinates of their constituent cubes. The Centered 
 * Coordinate system identifies the center of cubes {-3, -1, 1, 3} of an 8 x 8 x 8 enclosure relative to the center of 
 * the enclosure. This representation is suitable for computing rotations without floating point arithmetic. In the 
 * Index representation each 4 x 4 x 4 cube is assigned a number in the range [0-63]. The Extent representation sets 
 * a bit corresponding to an index. 
 *
 * @author anno.langen@gmail.com (Anno Langen)
 */
public class Tetris {

  enum Color { 
    RED, BLUE, YELLOW
  }

  /**
   * A colored piece of the puzzle. Characterized by some initial position, which is expressed by enumerating the 
   * Corner Coordinates of its constituent cubes. The bounding box of the initial position must include zero for all 
   * three dimensions - the rotation is arbitrary.
   */
  static class Piece {

    static final Piece[] ALL = {
        new Piece(Color.BLUE, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {0, 2, 0}, {2, 0, 1}}),
        new Piece(Color.BLUE, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {0, 2, 0}, {1, 0, 1}}),
        new Piece(Color.BLUE, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {2, 1, 0}, {3, 1, 0}}),
        new Piece(Color.BLUE, new int[][]{{1, 0, 0}, {2, 0, 0}, {2, 0, 1}, {1, 1, 0}, {0, 1, 0}}),
        new Piece(Color.RED, new int[][]{{0, 0, 0}, {1, 0, 0}, {0, 0, 1}, {1, 1, 0}, {2, 1, 0}, {1, 2, 0}}),
        new Piece(Color.RED, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {0, 2, 0}}),
        new Piece(Color.RED, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {1, 1, 0}}),
        new Piece(Color.RED, new int[][]{{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {0, 0, 1}}),
        new Piece(Color.YELLOW, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 0, 1}, {0, 1, 1}}),
        new Piece(Color.YELLOW, new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {1, 1, 0}, {1, 1, 1}}),
        new Piece(Color.YELLOW, new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {1, 1, 1}, {2, 1, 1}}),
        new Piece(Color.YELLOW, new int[][]{{0, 0, 1}, {1, 0, 1}, {2, 0, 1}, {1, 0, 2}, {1, 1, 1}, {1, 1, 0}})};

    final Color color;
    final long initialExtent;

    Piece(Color color, int[][] cubes) {
      long bits = 0;
      for (int[] cube : cubes) {
        bits += (1L << indexFromVector(cube));
      }
      this.color = color;
      initialExtent = bits;
    }

    /**
     * A specific placement of the parent piece. Occupies certain area of the 4 x 4 x 4 grid, which
     * is encoded with bit value 1 in the a 64 bit long {@link #extent}.
     */
    class Placement {

      final long extent;
      public Placement(long extent) {
        this.extent = extent;
      }

      @Override
      public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int y = 4; --y >= 0;) {
          appendFourRowLine(buf, y);
        }
        return buf.toString();
      }

      private void appendFourRowLine(StringBuilder buf, int y) {
        int start = y << 2;
        buf.append('\n');
        appendRow(buf, start);
        buf.append(' ');
        appendRow(buf, start + 16);
        buf.append(' ');
        appendRow(buf, start + 32);
        buf.append(' ');
        appendRow(buf, start + 48);
      }

      private void appendRow(StringBuilder buf, int start) {
        for (int i = start; i < start + 4; i++) {
          buf.append((extent & (1L << i)) != 0 ? 'X' : '_');
        }
      }

      public Piece getPiece() {
        return Piece.this;
      }

      @Override
      public boolean equals(Object o) {
        return this == o
            || o != null && getClass() == o.getClass() && extent == ((Placement) o).extent;
      }

      @Override
      public int hashCode() {
        return (int) (extent ^ (extent >>> 32));
      }
    }

    /**
     * Returns all placements resulting from shifting the initial extent in all three dimensions.
     */
    Collection<Placement> allShifts() {
      int[] max = getBoundingBox(initialExtent);
      Collection<Placement> result = new ArrayList<Placement>();
      long xbits = initialExtent;
      for (int xShift = 4 - max[0]; --xShift >= 0;) {
        long ybits = xbits;
        for (int y = 4 - max[1]; --y >= 0;) {
          long zbits = ybits;
          for (int z = 4 - max[2]; --z >= 0;) {
            result.add(new Placement(zbits));
            zbits = Transform.Z.apply(zbits);
          }
          ybits = Transform.Y.apply(ybits);
        }
        xbits = Transform.X.apply(xbits);
      }
      return result;
    }

    Collection<Placement> allPlacements() {
      Collection<Placement> result = new HashSet<Placement>();
      for (Placement placement : allShifts()) {
        addAllRotations(placement.extent, result);
      }
      return result;
    }

    private Collection<Placement> addAllRotations(long positions, Collection<Placement> result) {
      for (Transform transform : Transform.ROTATIONS) {
        result.add(newPlacement(transform.apply(positions)));
      }
      return result;
    }

    private Placement newPlacement(long extent) {
      return new Placement(extent);
    }

    private static int[] getBoundingBox(long extent) {
      int[] max = new int[3];
      for (int i = 64; --i >= 0; ) {
        if ((extent & (1L << i)) != 0) {
          int[] v = asVector(i);
          for (int d = 3; --d >= 0; ) {
            if (v[d] > max[d]) max[d] = v[d];
          }
        }
      }
      return max;
    }
  }

  static class Transform {

    /**
     * Permutation of the Index representation.
     */
    final int[] map;
    static final Transform X = new Transform(shift(0));
    static final Transform Y = new Transform(shift(1));
    static final Transform Z = new Transform(shift(2));
    static final Transform[] ROTATIONS = getAllRotations();

    public Transform(int[] map) {
      this.map = map;
    }

    public Transform(int[][] matrix) {
      map = new int[64];
      for (int i = 64; --i >= 0;) {
        map[i] = indexFromCenteredVector(matrixApply(matrix, asCenteredVector(i)));
      }
    }

    public long apply(long extent) {
      long result = 0;
      for (int i = 64; --i >= 0;) {
        if ((extent & (1L << i)) != 0) {
          result += (1L << map[i]);
        }
      }
      return result;
    }
  }

  static class Rotation extends Transform {

    final int[][] matrix;

    public Rotation(int[][] matrix) {
      super(matrix);
      this.matrix = new int[3][];
      for (int i = 3; --i >=0;) {
        this.matrix[i] = new int[3];
        System.arraycopy(matrix[i], 0, this.matrix[i], 0, 3);
      }
    }
  }

  static int[] matrixApply(int[][] m, int[] v) {
    return new int[]{dotProduct(m[0], v), dotProduct(m[1], v), dotProduct(m[2], v)};
  }

  static int dotProduct(int[] u, int[] v) {
    return u[0] * v[0] + u[1] * v[1] + u[2] * v[2];
  }

  static int[] asVector(int index) {
    int x = index & 3;
    int y = (index & 15) >> 2;
    int z = (index >> 4) & 3;
    return new int[]{x, y, z};
  }

  static int[] asCenteredVector(int index) {
    int[] v = asVector(index);
    return new int[]{2 * v[0] - 3, 2 * v[1] - 3, 2 * v[2] - 3};
  }

  static int indexFromVector(int... v) {
    return v[0] + (v[1] << 2) + (v[2] << 4);
  }

  static int indexFromCenteredVector(int[] v) {
    return ((v[0] + 3) >> 1) + ((v[1] + 3) << 1) + ((v[2] + 3) << 3);
  }

  /**
   * Returns the 2 x 3 x 2 x 2 = 24 possible three dimensional rotations. A rotation is characterized by a 3 x 3
   * matrix, where the images of the first two unit vectors are unit vectors along one of the coordinate axes and the
   * image of the third unit vector is determined by the cross product of the first two images. Note that the identity
   * transform is included in the 24 rotations.
   */
  private static Transform[] getAllRotations() {
    int[][] matrix = new int[3][];

    ArrayList<Transform> list = new ArrayList<Transform>(24);
    for (int i = 3; --i >= 0;) {
      addAllRotations(matrix, i, 1, list);
      addAllRotations(matrix, i, -1, list);
    }
    return list.toArray(new Transform[list.size()]);
  }

  private static void addAllRotations(int[][] matrix, int x, int sx, List<Transform> transforms) {
    matrix[0] = new int[3];
    matrix[0][x] = sx;
    for (int y = 3; --y >= 0;) {
      if (x != y) {
        transforms.add(getRotation(matrix, y, 1));
        transforms.add(getRotation(matrix, y, -1));
      }
    }
  }

  private static Rotation getRotation(int[][] matrix, int y, int sy) {
    matrix[1] = new int[3];
    matrix[1][y] = sy;
    matrix[2] = crossProduct(matrix[0], matrix[1]);
    return new Rotation(matrix);
  }

  /**
   * Returns vector that is perpendicular to the given two vectors with the direction determined by the right-hand rule.
   * See <a href='http://en.wikipedia.org/wiki/Cross_product'>Cross product</a> for more info.
   */
  static int[] crossProduct(int[] u, int[] v) {
    return new int[]{u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]};
  }

  /**
   * Returns an Index permutation corresponding to a unit shift along the given coordinate axis.
   */
  static int[] shift(int index) {
    int[] result = new int[64];
    for (int i = 64; --i >= 0;) {
      int[] v = asVector(i);
      v[index] += 1;
      result[i] = indexFromVector(v) % 64;
    }
    return result;
  }

  static class Solution {
    final List<Piece.Placement> placements = new ArrayList<Piece.Placement>();

    public Solution(List<Piece.Placement> placements) {
      this.placements.addAll(placements);
    }

    int[] getPieces() {
      int[] pieces = new int[64];
      for (int i = 0; i < placements.size(); i++) {
        Piece.Placement placement = placements.get(i);
        long bits = placement.extent;
        for (int j = 64; --j >= 0;) {
          if ((bits & (1L << j)) != 0) {
            pieces[j] = i;
          }
        }
      }
      return pieces;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder();
      int[] pieces = getPieces();
      for (int y = 4; --y >=0;) {
        buf.append('\n');
        for (int z = 0; z < 4; z++) {
          for (int x = 0; x < 4; x++) {
            buf.append(pieces[indexFromVector(x,y,z)]);
          }
          buf.append(' ');
        }
      }
      return buf.toString();
    }
  }

  public static void main(String[] args) {
    List<Piece.Placement[]> placementsList = new ArrayList<Piece.Placement[]>();
    Collection<Piece.Placement> shifts = Piece.ALL[0].allShifts();
    placementsList.add(shifts.toArray(new Piece.Placement[shifts.size()]));
    for (int i = 1; i < Piece.ALL.length; i++) {
      Collection<Piece.Placement> placements = Piece.ALL[i].allPlacements();
      placementsList.add(placements.toArray(new Piece.Placement[placements.size()]));
    }
    List<Solution> solutions = addAll(placementsList, new ArrayList<Piece.Placement>(), 0L, new ArrayList<Solution>());
    System.out.println(solutions);
  }

  private static List<Solution> addAll(List<Piece.Placement[]> placementsList, List<Piece.Placement> solution,
      long bits, List<Solution> solutions) {
    int n = solution.size();
    if (n == placementsList.size()) {
      solutions.add(new Solution(solution));
    } else {
      for (Piece.Placement placement : placementsList.get(n)) {
        if ((placement.extent & bits) == 0) {
          solution.add(placement);
          addAll(placementsList, solution, bits | placement.extent, solutions);
          solution.remove(n);
        }
      }
    }
    return solutions;
  }
}
