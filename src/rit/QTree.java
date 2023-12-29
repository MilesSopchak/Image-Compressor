package rit;

import java.io.*;
import java.util.*;

/**
 * This class represents the Quadtree data structure used to compress raw
 * grayscale images and uncompress back.  Conceptually, the tree is
 * a collection of rit.QTNode's.  A rit.QTNode either holds a grayscale image
 * value (0-255), or QUAD_SPLIT, meaning the node is split into four
 * sub-nodes that are equally sized sub-regions that divide up the
 * current space.
 * <p>
 * To learn more about quadtrees:
 * https://en.wikipedia.org/wiki/Quadtree
 *
 * @author RIT CS
 * @author Miles Sopchak
 */
public class QTree {
    /**
     * the value of a node that indicates it is spplit into 4 sub-regions
     */
    public final static int QUAD_SPLIT = -1;

    /**
     * the root node in the tree
     */
    private QTNode root;

    /**
     * the square dimension of the tree
     */
    private int DIM;

    /**
     * the raw image
     */
    private int image[][];

    /**
     * the size of the raw image
     */
    private int rawSize;

    /**
     * the size of the compressed image
     */
    private int compressedSize;

    /**
     * Create an initially empty tree.
     */
    public QTree() {
        this.root = null;
        this.DIM = 0;
        this.image = null;
        this.rawSize = 0;
        this.compressedSize = 0;
    }

    /**
     * Get the images square dimension.
     *
     * @return the square dimension
     */
    public int getDim() {
        return this.DIM;
    }

    /**
     * Get the raw image.
     *
     * @return the raw image
     */
    public int[][] getImage() {
        return this.image;
    }

    /**
     * Get the size of the raw image.
     *
     * @return raw image size
     */
    public int getRawSize() {
        return this.rawSize;
    }

    /**
     * Get the size of the compressed image.
     *
     * @return compressed image size
     */
    public int getCompressedSize() {
        return this.compressedSize;
    }

    /**
     * Write the uncompressed image to the output file.  This routine is meant to be
     * called from a client after it has been uncompressed
     *
     * @param outFile the name of the file to write the uncompressed image to
     * @throws IOException any errors involved with writing the file out
     * @throws QTException if the file has not been uncompressed yet
     * @rit.pre client has called uncompress() to uncompress the input file
     */
    public void writeUncompressed(String outFile) throws IOException, QTException {
        BufferedWriter b = new BufferedWriter(new FileWriter(outFile));
        for(int[] i: image)
        {
            for(int j: i)
            {
                b.write(Integer.toString(j) + "\n");
            }
        }
        b.flush();
        b.close();
    }

    /**
     * A private helper routine for parsing the compressed image into
     * a tree of nodes.  When parsing through the values, there are
     * two cases:
     * <p>
     * 1. The value is a grayscale color (0-255).  In this case
     * return a node containing the value.
     * <p>
     * 2. The value is QUAD_SPLIT.  The node must be split into
     * four sub-regions.  Each sub-region is attained by recursively
     * calling this routine.  A node containing these four sub-regions
     * is returned.
     *
     * @param values the values in the compressed image
     * @return a node that encapsulates this portion of the compressed
     * image
     * @throws QTException if there are not enough values in the
     *                     compressed image
     */
    private QTNode parse(List<Integer> values) throws QTException {
        if(values.get(0) != -1)
        {
            int temp = values.remove(0);
            return new QTNode(temp);
        }
        values.remove(0);
        return new QTNode(-1,parse(values),parse(values),parse(values),parse(values));
    }

    /**
     * This is the core routine for uncompressing an image stored in a tree
     * into its raw image (a 2-D array of grayscale values (0-255).
     * It is called by the public uncompress routine.
     * The main idea is that we are working with a tree whose root represents the
     * entire 2^n x 2^n image.  There are two cases:
     * <p>
     * 1. The node is not split.  We can write out the corresponding
     * "block" of values into the raw image array based on the size
     * of the region
     * <p>
     * 2. The node is split.  We must recursively call ourselves with the
     * the four sub-regions.  Take note of the pattern for representing the
     * starting coordinate of the four sub-regions of a 4x4 grid:
     * - upper left: (0, 0)
     * - upper right: (0, 1)
     * - lower left: (1, 0)
     * - lower right: (1, 1)
     * We can generalize this pattern by computing the offset and adding
     * it to the starting row and column in the appropriate places
     * (there is a 1).
     *
     * @param node  the node to uncompress
     * @param size  the size of the square region this node represents
     * @param start the starting coordinate this row represents in the image
     */
    private void uncompress(QTNode node, int size, Coordinate start) {
        if (node.getVal() != -1)
        {
            for(int x = 0; x < size; x++)
            {
                for(int y = 0; y < size; y++)
                {
                    image[start.getRow() + y][start.getCol() + x] = node.getVal();
                }
            }
        }
        else
        {
            uncompress(node.getUpperLeft(), size/2, start);
            uncompress(node.getUpperRight(), size/2, new Coordinate(start.getRow(), start.getCol() + (size/2)));
            uncompress(node.getLowerLeft(), size/2, new Coordinate(start.getRow() + (size/2), start.getCol()));
            uncompress(node.getLowerRight(), size/2, new Coordinate(start.getRow() + (size/2), start.getCol() + (size/2)));
        }
    }

    /**
     * Uncompress a RIT compressed file.  This is the public facing routine
     * meant to be used by a client to uncompress an image for displaying.
     * <p>
     * The file is expected to be 2^n x 2^n pixels.  The first line in
     * the file is its size (number of values).  The remaining lines are
     * the values in the compressed image, one per line, of "size" lines.
     * <p>
     * Once this routine completes, the raw image of grayscale values (0-255)
     * is stored internally and can be retrieved by the client using getImage().
     *
     * @param filename the name of the compressed file
     * @throws IOException if there are issues working with the compressed file
     * @throws QTException if there are issues parsing the data in the file
     */
    public void uncompress(String filename) throws IOException, QTException {
        Scanner in = new Scanner(new File(filename));
        int side = 0;
        if(in.hasNextLine()) {side = (int)Math.sqrt(Integer.parseInt(in.nextLine()));}
        else {throw new QTException("Error Uncompressing. Not enough data.");}
        DIM = side;
        List<Integer> data = new ArrayList<Integer>();
        int size = 1;
        while (in.hasNextLine())
        {
            int i = 0;
            try {i = Integer.parseInt(in.nextLine());}
            catch (NumberFormatException e)
            {throw new QTException("Error Uncompressing. File not formatted correctly");}
            if(i == -1) {size+=4;}
            data.add(i);
        }
        if(data.size() == 0 || data.size() != size) {throw new QTException("Error Uncompressing. Not enough data.");}
        this.image = new int[side][side];
        root = parse(data);
        uncompress(root,side,new Coordinate(0,0));
    }

    /**
     * The private writer is a recursive helper routine that writes out the
     * compressed image.  It goes through the tree in preorder fashion
     * writing out the values of each node as they are encountered.
     *
     * @param node   the current node in the tree
     * @param writer the writer to write the node data out to
     * @throws IOException if there are issues with the writer
     */
    private void writeCompressed(QTNode node, BufferedWriter writer) throws IOException {
        if(node.getVal() != -1)
        {
            writer.write(Integer.toString(node.getVal()) + "\n");
            compressedSize++;
        }
        else
        {
            writer.write("-1\n");
            compressedSize++;
            writeCompressed(node.getUpperLeft(), writer);
            writeCompressed(node.getUpperRight(), writer);
            writeCompressed(node.getLowerLeft(), writer);
            writeCompressed(node.getLowerRight(), writer);
        }
    }

    /**
     * Write the compressed image to the output file.  This routine is meant to be
     * called from a client after it has been compressed
     *
     * @param outFile the name of the file to write the compressed image to
     * @throws IOException any errors involved with writing the file out
     * @throws QTException if the file has not been compressed yet
     * @rit.pre client has called compress() to compress the input file
     */
    public void writeCompressed(String outFile) throws IOException, QTException {
        BufferedWriter b = new BufferedWriter(new FileWriter(outFile));
        b.write(Integer.toString((int)Math.pow(image[0].length,2)) + "\n");
        writeCompressed(root, b);
        compressedSize++;
        b.flush();
        b.close();
    }

    /**
     * Check to see whether a region in the raw image contains the same value.
     * This routine is used by the private compress routine so that it can
     * construct the nodes in the tree.
     *
     * @param start the starting coordinate in the region
     * @param size  the size of the region
     * @return whether the region can be compressed or not
     */
    private boolean canCompressBlock(Coordinate start, int size) {
        for(int i = 0; i < size; i++)
        {
            for(int j = 0; j < size; j++)
            {
                if(image[i + start.getRow()][j + start.getCol()] != image[start.getRow()][start.getCol()]) {return false;}
            }
        }
        return true;
    }

    /**
     * This is the core compression routine.  Its job is to work over a region
     * of the image and compress it.  It is a recursive routine with two cases:
     * <p>
     * 1. The entire region represented by this image has the same value, or
     * we are down to one pixel.  In either case, we can now create a node
     * that represents this.
     * <p>
     * 2. If we can't compress at this level, we need to divide into 4
     * equally sized sub-regions and call ourselves again.  Just like with
     * uncompressing, we can compute the starting point of the four sub-regions
     * by using the starting point and size of the full region.
     *
     * @param start the start coordinate for this region
     * @param size  the size this region represents
     * @return a node containing the compression information for the region
     */
    private QTNode compress(Coordinate start, int size) {
        if(canCompressBlock(start,size)) {return new QTNode(image[start.getRow()][start.getCol()]);}
        size/=2;
        return new QTNode(-1, compress(start,size), compress(new Coordinate(start.getRow(), start.getCol() + size), size), compress(new Coordinate(start.getRow() + size, start.getCol()), size), compress(new Coordinate(start.getRow() + size, start.getCol() + size), size));
    }

    /**
     * Compress a raw image into the RIT format.  This routine is meant to be
     * called by a client.  It is expected to be passed a file which represents
     * the raw image.  It is ASCII formatted and contains a series of grayscale
     * values (0-255).  There is one value per line, and 2^n x 2^n total lines.
     *
     * @param inputFile the raw image file name
     * @throws IOException if there are issues working with the file
     */
    public void compress(String inputFile) throws IOException {
        List<Integer> data = new LinkedList<Integer>();
        Scanner in = new Scanner(new File(inputFile));
        while(in.hasNextLine()) {data.add(Integer.parseInt(in.nextLine()));}
        rawSize = data.size();
        int side = (int)Math.sqrt(data.size());
        image = new int[side][side];
        for(int i = 0; i < side; i++)
        {
            for(int j = 0; j < side; j++)
            {
                image[i][j] = data.get(0);
                data.remove(0);
            }
        }
        root = compress(new Coordinate(0,0),side);
    }

    /**
     * A preorder (parent, left, right) traversal of a node.  It returns
     * a string which is empty if the node is null.  Otherwise
     * it returns a string that concatenates the current node's value
     * with the values of the 4 sub-regions (with spaces between).
     * This is a recursive process starting with the root and is similar
     * to how parsing works.
     *
     * @param node the node being traversed on
     * @return the string of the node
     */
    private String preorder(QTNode node) {
        if(node.getVal() != -1) {return String.valueOf(node.getVal());}
        return "-1 " + preorder(node.getUpperLeft()) + "\s" + preorder(node.getUpperRight()) + "\s" + preorder(node.getLowerLeft()) + "\s" + preorder(node.getLowerRight());
    }

    /**
     * Returns a string which is a preorder traversal of the tree.
     *
     * @return the qtree string representation
     */
    @Override
    public String toString() {
        return "QTree: " + preorder(this.root);
    }
}