/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import net.jafama.FastMath;

import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds and initialises resources utilised globally.
 *
 * @author Toni Sagrista
 */
public class GlobalResources {
    private static final Log logger = Logger.getLogger(GlobalResources.class);

    public static ShaderProgram spriteShader;
    /** Global all-purpose sprite batch **/
    public static SpriteBatch spriteBatch;
    /** Cursors **/
    public static Cursor linkCursor, resizeXCursor, resizeYCursor;
    /** The global skin **/
    public static Skin skin;

    private static Vector3d aux = new Vector3d();

    /** GOOGLE COLORS **/

    public static float[] gGreen = new float[] { 0f / 255f, 135f / 255f, 68f / 255f, 1f };
    public static Color gGreenC =getCol(gGreen);
    public static float[] gBlue = new float[] { 0f / 255f, 87f / 255f, 231f / 255f, 1f };
    public static Color gBlueC =getCol(gBlue);
    public static float[] gRed = new float[] { 214f / 255f, 45f / 255f, 32f / 255f, 1f };
    public static Color gRedC =getCol(gRed);
    public static float[] gYellow = new float[] { 255f / 255f, 167f / 255f, 0f / 255f, 1f };
    public static Color gYellowC =getCol(gYellow);
    public static float[] gWhite = new float[] { 255f / 255f, 255f / 255f, 255f / 255f, 1f };
    public static Color gWhiteC =getCol(gWhite);
    public static float[] gPink = new float[] { 255f / 255f, 102f / 255f, 255f / 255f, 1f };
    public static Color gPinkC =getCol(gPink);

    private static Color getCol(float[] c){
        return new Color(c[0], c[1], c[2], c[3]);
    }

    public static void initialize(AssetManager manager) {
        // Sprite shader
        spriteShader = new ShaderProgram(Gdx.files.internal("shader/spritebatch.vertex.glsl"), Gdx.files.internal("shader/spritebatch.fragment.glsl"));
        // Sprite batch
        spriteBatch = new SpriteBatch(1000, spriteShader);

        updateSkin();

    }

    public static void updateSkin() {
        initCursors();
        FileHandle fh = Gdx.files.internal("skins/" + GlobalConf.program.UI_THEME + "/" + GlobalConf.program.UI_THEME + ".json");
        skin = new Skin(fh);
    }

    private static void initCursors(){
        // Create skin right now, it is needed.
        if (GlobalConf.program.UI_THEME.endsWith("-x2")) {
            GlobalConf.updateScaleFactor(2.0f);
            linkCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-link-x2.png")), 8, 0);
            resizeXCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizex-x2.png")), 16, 16);
            resizeYCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizey-x2.png")), 16, 16);
        } else {
            GlobalConf.updateScaleFactor(1.0f);
            linkCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-link.png")), 4, 0);
            resizeXCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizex.png")), 8, 8);
            resizeYCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizey.png")), 8, 8);
        }

    }

    public static void doneLoading(AssetManager manager) {
    }

    /**
     * Converts this double to the string representation of a distance
     *
     * @param d In internal units
     * @return An array containing the float number and the string units
     */
    public static Pair<Double, String> doubleToDistanceString(double d) {
        d = d * Constants.U_TO_KM;
        if (Math.abs(d) < 1f) {
            // m
            return new Pair<>((d * 1000), "m");
        }
        if (Math.abs(d) < Nature.AU_TO_KM) {
            // km
            return new Pair<>(d, "km");
        } else if (Math.abs(d) < Nature.PC_TO_KM) {
            // AU
            return new Pair<>(d * Nature.KM_TO_AU, "AU");
        } else {
            // pc
            return new Pair<>((d * Nature.KM_TO_PC), "pc");
        }
    }

    /**
     * Converts the double to the string representation of a velocity (always in
     * seconds)
     *
     * @param d In internal units
     * @return Array containing the number and the units
     */
    public static Pair<Double, String> doubleToVelocityString(double d) {
        Pair<Double, String> res = doubleToDistanceString(d);
        res.setSecond(res.getSecond().concat("/s"));
        return res;
    }

    /**
     * Converts this float to the string representation of a distance
     *
     * @param f In internal units
     * @return An array containing the float number and the string units
     */
    public static Pair<Float, String> floatToDistanceString(float f) {
        Pair<Double, String> result = doubleToDistanceString((double) f);
        return new Pair<>(result.getFirst().floatValue(), result.getSecond());
    }

    /**
     * Transforms the given double array into a float array by casting each of
     * its numbers
     *
     * @param array The array of doubles
     * @return The array of floats
     */
    public static float[] toFloatArray(double[] array) {
        float[] res = new float[array.length];
        for (int i = 0; i < array.length; i++)
            res[i] = (float) array[i];
        return res;
    }

    /**
     * Computes whether a body with the given position is visible by a camera
     * with the given direction and angle. Coordinates are assumed to be in the
     * camera-origin system
     *
     * @param point     The position of the body in the reference system of the camera
     *                  (i.e. camera is at origin)
     * @param coneAngle The cone angle of the camera
     * @param dir       The direction
     * @return True if the body is visible
     */
    public static boolean isInView(Vector3d point, float coneAngle, Vector3d dir) {
        return FastMath.acos(point.dot(dir) / point.len()) < coneAngle;
    }

    /**
     * Computes whether a body with the given position is visible by a camera
     * with the given direction and angle. Coordinates are assumed to be in the
     * camera-origin system
     *
     * @param point     The position of the body in the reference system of the camera
     *                  (i.e. camera is at origin)
     * @param len       The point length
     * @param coneAngle The cone angle of the camera
     * @param dir       The direction
     * @return True if the body is visible
     */
    public static boolean isInView(Vector3d point, double len, float coneAngle, Vector3d dir) {
        return FastMath.acos(point.dot(dir) / len) < coneAngle;
    }

    /**
     * Computes whether any of the given points is visible by a camera with the
     * given direction and the given cone angle. Coordinates are assumed to be
     * in the camera-origin system
     *
     * @param points    The array of points to check
     * @param coneAngle The cone angle of the camera (field of view)
     * @param dir       The direction
     * @return True if any of the points is in the camera view cone
     */
    public static boolean isAnyInView(Vector3d[] points, float coneAngle, Vector3d dir) {
        boolean inview = false;
        int size = points.length;
        for (int i = 0; i < size; i++) {
            inview = inview || FastMath.acos(points[i].dot(dir) / points[i].len()) < coneAngle;
        }
        return inview;
    }

    /**
     * Compares a given buffer with another buffer.
     *
     * @param buf       Buffer to compare against
     * @param compareTo Buffer to compare to (content should be ASCII lowercase if
     *                  possible)
     * @return True if the buffers compare favourably, false otherwise
     */
    public static boolean equal(String buf, char[] compareTo, boolean ignoreCase) {
        if (buf == null || compareTo == null || buf.length() == 0)
            return false;
        char a, b;
        int len = Math.min(buf.length(), compareTo.length);
        if (ignoreCase) {
            for (int i = 0; i < len; i++) {
                a = buf.charAt(i);
                b = compareTo[i];
                if (a == b || (a - 32) == b)
                    continue; // test a == a or A == a;
                return false;
            }
        } else {
            for (int i = 0; i < len; i++) {
                a = buf.charAt(i);
                b = compareTo[i];
                if (a == b)
                    continue; // test a == a
                return false;
            }
        }
        return true;
    }

    public static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

    public static int nthIndexOf(String text, char needle, int n) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == needle) {
                n--;
                if (n == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Gets all the files with the given extension in the given file handle f.
     *
     * @param f          The directory to get all the files
     * @param l          The list with re results
     * @param extensions The allowed extensions
     * @return The list l
     */
    public static Array<FileHandle> listRec(FileHandle f, Array<FileHandle> l, String... extensions) {
        if (f.exists()) {
            if (f.isDirectory()) {
                FileHandle[] partial = f.list();
                for (FileHandle fh : partial) {
                    l = listRec(fh, l, extensions);
                }

            } else {
                if (endsWithAny(f.name(), extensions)) {
                    l.add(f);
                }
            }
        }

        return l;
    }

    private static boolean endsWithAny(String str, String... extensions){
        for(String ext: extensions){
            if(str.endsWith(ext))
                return true;
        }
        return false;
    }

    public static Array<FileHandle> listRec(FileHandle f, Array<FileHandle> l, FilenameFilter filter) {
        if (f.exists()) {
            if (f.isDirectory()) {
                FileHandle[] partial = f.list();
                for (FileHandle fh : partial) {
                    l = listRec(fh, l, filter);
                }

            } else {
                if (filter.accept(null, f.name())) {
                    l.add(f);
                }
            }
        }

        return l;
    }

    /**
     * Recursively count files in a directory
     * @param dir The directory
     * @return The number of files
     * @throws IOException
     */
    public static long fileCount(Path dir) throws IOException {
        return fileCount(dir, null);
    }

    /**
     * Count files matching a certain ending in a directory, recursively
     * @param dir The directory
     * @return The number of files
     * @throws IOException
     */
    public static long fileCount(Path dir, String[] extensions) throws IOException {
        return Files.walk(dir)
                .parallel()
                .filter(p -> (!p.toFile().isDirectory() && endsWith(p.toFile().getName(), extensions)))
                .count();
    }

    /**
     * Returns true if the string ends with any of the endings
     * @param s The string
     * @param endings The endings
     * @return True if the string ends with any of the endings
     */
    public static boolean endsWith(String s, String[] endings){
        if(endings == null){
            return true;
        }
        for(String ending : endings){
            if(s.endsWith(ending))
                return true;
        }
        return false;
    }

    public static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Comparator.comparing(Map.Entry::getValue));

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Converts a texture to a pixmap by drawing it to a frame buffer and
     * getting the data
     *
     * @param tex The texture to convert
     * @return The resulting pixmap
     */
    public static Pixmap textureToPixmap(TextureRegion tex) {

        //width and height in pixels
        int width = tex.getRegionWidth();
        int height = tex.getRegionWidth();

        //Create a SpriteBatch to handle the drawing.
        SpriteBatch sb = new SpriteBatch(1000, GlobalResources.spriteShader);

        //Set the projection matrix for the SpriteBatch.
        Matrix4 projectionMatrix = new Matrix4();

        //because Pixmap has its origin on the topleft and everything else in LibGDX has the origin left bottom
        //we flip the projection matrix on y and move it -height. So it will end up side up in the .png
        projectionMatrix.setToOrtho2D(0, -height, width, height).scale(1, -1, 1);

        //Set the projection matrix on the SpriteBatch
        sb.setProjectionMatrix(projectionMatrix);

        //Create a frame buffer.
        FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);

        //Call begin(). So all next drawing will go to the new FrameBuffer.
        fb.begin();

        //Set up the SpriteBatch for drawing.
        sb.begin();

        //Draw all the tiles.
        sb.draw(tex, 0, 0, width, height);

        //End drawing on the SpriteBatch. This will flush() any sprites remaining to be drawn as well.
        sb.end();

        //Then retrieve the Pixmap from the buffer.
        Pixmap pm = ScreenUtils.getFrameBufferPixmap(0, 0, width, height);

        //Close the FrameBuffer. Rendering will resume to the normal buffer.
        fb.end();

        //Dispose of the resources.
        fb.dispose();
        sb.dispose();

        return pm;
    }

    /**
     * Inverts a map
     *
     * @param map The map to invert
     * @return The inverted map
     */
    public static final <T, U> Map<U, List<T>> invertMap(Map<T, U> map) {
        HashMap<U, List<T>> invertedMap = new HashMap<>();

        for (T key : map.keySet()) {
            U newKey = map.get(key);

            invertedMap.computeIfAbsent(newKey, k -> new ArrayList<>());
            invertedMap.get(newKey).add(key);

        }

        return invertedMap;
    }

    /** Gets the angle in degrees between the two vectors **/
    public static float angle2d(Vector3 v1, Vector3 v2) {
        return (float) (MathUtilsd.radiansToDegrees * FastMath.atan2(v2.y - v1.y, v2.x - v1.x));
    }

    public static synchronized Vector3d applyRelativisticAberration(Vector3d pos, ICamera cam) {
        // Relativistic aberration
        if (GlobalConf.runtime.RELATIVISTIC_ABERRATION) {
            Vector3d cdir = aux;
            if (cam.getVelocity() != null)
                cdir.set(cam.getVelocity()).nor();
            else
                cdir.set(1, 0, 0);

            double vc = cam.getSpeed() / Constants.C_KMH;
            if (vc > 0) {
                cdir.scl(-1);
                double costh_s = cdir.dot(pos) / pos.len();
                double th_s = Math.acos(costh_s);

                double costh_o = (costh_s - vc) / (1 - vc * costh_s);
                double th_o = Math.acos(costh_o);

                pos.rotate(cdir.crs(pos).nor(), Math.toDegrees(th_o - th_s));
            }
        }
        return pos;
    }

    /**
     * Converts bytes to a human readable format
     *
     * @param bytes The bytes
     * @param si    Whether to use SI units or binary
     * @return The size in a human readable form
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private static String generateMD5(FileInputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            FileChannel channel = inputStream.getChannel();
            ByteBuffer buff = ByteBuffer.allocate(2048);
            while (channel.read(buff) != -1) {
                buff.flip();
                md.update(buff);
                buff.clear();
            }
            byte[] hashValue = md.digest();
            return new String(hashValue);
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error(e);
            return null;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    /**
     * Attempts to calculate the size of a file or directory.
     *
     * <p>
     * Since the operation is non-atomic, the returned value may be inaccurate.
     * However, this method is quick and does its best.
     */
    public static long size(Path path) throws IOException {

        final AtomicLong size = new AtomicLong(0);

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {

                System.out.println("skipped: " + file + " (" + exc + ")");
                // Skip folders that can't be traversed
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                if (exc != null)
                    System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                // Ignore errors traversing a folder
                return FileVisitResult.CONTINUE;
            }
        });

        return size.get();
    }

    /**
     * Parses the string and creates a string array. The string is a list of whitespace-separated
     * tokens, each surrounded by double qutotes '"':
     * str = '"a" "bc" "d" "efghi"'
     * @param str The string
     * @return The resulting array
     */
    public static String[] parseWhitespaceSeparatedList(String str) {
        if(str == null || str.isEmpty())
            return null;

        List<String> l = new ArrayList<String>();
        int n = str.length();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < n; i++) {
            char c = str.charAt(i);
            if(c == '"'){
                if(inString){
                    l.add(current.toString());
                    current = new StringBuilder();
                    inString = false;
                } else {
                    inString = true;
                }
            } else {
                if(inString)
                    current.append(c);
            }
        }
        return l.toArray(new String[l.size()]);
    }

    /**
     * Converts the string array into a whitespace-separated string
     * where each element is double quoted.
     * @param l The string array
     * @return The resulting string
     */
    public static String toWhitespaceSeparatedList(String[] l){
        if(l == null || l.length == 0)
            return null;

        StringBuilder sb = new StringBuilder();
        for(String s : l){
            sb.append("\"").append(s).append("\" ");
        }
        return sb.toString().trim();
    }

}
