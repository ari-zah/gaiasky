/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf.SceneConf.GraphicsQuality;
import gaiasky.util.Logger.Log;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;
import org.apfloat.Apfloat;
import org.lwjgl.opengl.GL30;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Holds and initialises resources utilised globally.
 *
 * @author Toni Sagrista
 */
public class GlobalResources {
    private static final Log logger = Logger.getLogger(GlobalResources.class);

    public static ShaderProgram shapeShader;
    public static ShaderProgram spriteShader;
    /** Global all-purpose sprite batch **/
    public static SpriteBatch spriteBatch, spriteBatchVR;

    public static ExtShaderProgram extSpriteShader;
    /** Sprite batch using int indices **/
    public static ExtSpriteBatch extSpriteBatch;
    /** Cursors **/
    public static Cursor linkCursor, resizeXCursor, resizeYCursor, emptyCursor;
    /** The global skin **/
    public static Skin skin;

    private static final Vector3d aux = new Vector3d();

    public static void initialize(AssetManager manager) {
        // Shape shader
        shapeShader = new ShaderProgram(Gdx.files.internal("shader/2d/shape.vertex.glsl"), Gdx.files.internal("shader/2d/shape.fragment.glsl"));
        if (!shapeShader.isCompiled()) {
            logger.info("ShapeRenderer shader compilation failed: " + shapeShader.getLog());
        }
        // Sprite shader
        spriteShader = new ShaderProgram(Gdx.files.internal("shader/2d/spritebatch.vertex.glsl"), Gdx.files.internal("shader/2d/spritebatch.fragment.glsl"));
        if (!spriteShader.isCompiled()) {
            logger.info("SpriteBatch shader compilation failed: " + spriteShader.getLog());
        }
        // Sprite batch - uses screen resolution
        spriteBatch = new SpriteBatch(500, spriteShader);

        // ExtSprite shader
        extSpriteShader = new ExtShaderProgram(Gdx.files.internal("shader/2d/spritebatch.vertex.glsl"), Gdx.files.internal("shader/2d/spritebatch.fragment.glsl"));
        if (!extSpriteShader.isCompiled()) {
            logger.info("ExtSpriteBatch shader compilation failed: " + spriteShader.getLog());
        }
        // Sprite batch
        extSpriteBatch = new ExtSpriteBatch(1000, extSpriteShader);

        updateSkin();

    }

    public static void updateSkin() {
        initCursors();
        FileHandle fh = Gdx.files.internal("skins/" + GlobalConf.program.UI_THEME + "/" + GlobalConf.program.UI_THEME + ".json");
        if (!fh.exists()) {
            // Default to dark-green
            logger.info("User interface theme '" + GlobalConf.program.UI_THEME + "' not found, using 'dark-green' instead");
            GlobalConf.program.UI_THEME = "dark-green";
            fh = Gdx.files.internal("skins/" + GlobalConf.program.UI_THEME + "/" + GlobalConf.program.UI_THEME + ".json");
        }
        skin = new Skin(fh);
        ObjectMap<String, BitmapFont> fonts = skin.getAll(BitmapFont.class);
        for(String key : fonts.keys()){
            fonts.get(key).getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        }
    }

    private static void initCursors() {
        // Create skin right now, it is needed.
        if (GlobalConf.program.UI_SCALE > 1.5) {
            linkCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-link-x2.png")), 8, 0);
            resizeXCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizex-x2.png")), 16, 16);
            resizeYCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizey-x2.png")), 16, 16);
        } else {
            linkCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-link.png")), 4, 0);
            resizeXCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizex.png")), 8, 8);
            resizeYCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-resizey.png")), 8, 8);
        }
        emptyCursor = Gdx.graphics.newCursor(new Pixmap(Gdx.files.internal("img/cursor-empty.png")), 0, 5);

    }

    public static void doneLoading(AssetManager manager) {
    }

    public static Pair<Double, String> doubleToDistanceString(Apfloat d) {
       return doubleToDistanceString(d.doubleValue());
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
        if (Math.abs(d) < 0.1 * Nature.AU_TO_KM) {
            // km
            return new Pair<>(d, "km");
        } else if (Math.abs(d) <  0.1 * Nature.PC_TO_KM) {
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
        Pair<Double, String> result = doubleToDistanceString(f);
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
     * @param len       The point length
     * @param coneAngle The cone angle of the camera
     * @param dir       The direction
     * @return True if the body is visible
     */
    public static boolean isInView(Vector3b point, double len, float coneAngle, Vector3d dir) {
        return FastMath.acos(point.tov3d().dot(dir) / len) < coneAngle;
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
     * Gets all the files with the given extension in the given path f.
     *
     * @param f          The directory to get all the files
     * @param l          The list with the results
     * @param extensions The allowed extensions
     * @return The list l
     */
    public static Array<Path> listRec(Path f, final Array<Path> l, String... extensions) {
        if (Files.exists(f)) {
            if (Files.isDirectory(f)) {
                try {
                    Stream<Path> partial = Files.list(f);
                    partial.forEachOrdered(p -> listRec(p, l, extensions));
                } catch (IOException e) {
                    logger.error(e);
                }

            } else {
                if (endsWithAny(f.getFileName().toString(), extensions)) {
                    l.add(f);
                }
            }
        }

        return l;
    }

    private static boolean endsWithAny(String str, String... extensions) {
        for (String ext : extensions) {
            if (str.endsWith(ext))
                return true;
        }
        return false;
    }

    /**
     * Deletes recursively all non-partial files from the path.
     * @param path The path to delete.
     * @throws IOException
     */
    public static void deleteRecursively(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .filter(p -> !p.toString().endsWith(".part") && !Files.isDirectory(p))
                .map(Path::toFile)
                .forEach(java.io.File::delete);
    }

    public static void copyFile(Path sourceFile, Path destFile, boolean ow) throws IOException {
        if (!Files.exists(destFile) || ow)
            Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public static Array<Path> listRec(Path f, final Array<Path> l, DirectoryStream.Filter<Path> filter) {
        if (Files.exists(f)) {
            if (Files.isDirectory(f)) {
                try {
                    Stream<Path> partial = Files.list(f);
                    partial.forEachOrdered(p -> listRec(p, l, filter));
                } catch (IOException e) {
                    logger.error(e);
                }

            } else {
                try {
                    if (filter.accept(f))
                        l.add(f);
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
        return l;
    }

    /**
     * Recursively count files in a directory
     *
     * @param dir The directory
     * @return The number of files
     * @throws IOException
     */
    public static long fileCount(Path dir) throws IOException {
        return fileCount(dir, null);
    }

    /**
     * Count files matching a certain ending in a directory, recursively
     *
     * @param dir The directory
     * @return The number of files
     * @throws IOException
     */
    public static long fileCount(Path dir, String[] extensions) throws IOException {
        return Files.walk(dir).parallel().filter(p -> (!p.toFile().isDirectory() && endsWith(p.toFile().getName(), extensions))).count();
    }

    /**
     * Returns true if the string ends with any of the endings
     *
     * @param s       The string
     * @param endings The endings
     * @return True if the string ends with any of the endings
     */
    public static boolean endsWith(String s, String[] endings) {
        if (endings == null) {
            return true;
        }
        for (String ending : endings) {
            if (s.endsWith(ending))
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
     * @param si    Whether to use SI units (1000-multiples) or binary (1024-multiples)
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
     *
     * @param str The string
     * @return The resulting array
     */
    public static String[] parseWhitespaceSeparatedList(String str) {
        if (str == null || str.isEmpty())
            return null;

        List<String> l = new ArrayList<String>();
        int n = str.length();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < n; i++) {
            char c = str.charAt(i);
            if (c == '"') {
                if (inString) {
                    l.add(current.toString());
                    current = new StringBuilder();
                    inString = false;
                } else {
                    inString = true;
                }
            } else {
                if (inString)
                    current.append(c);
            }
        }
        return l.toArray(new String[l.size()]);
    }

    /**
     * Converts the string array into a whitespace-separated string
     * where each element is double quoted.
     *
     * @param l The string array
     * @return The resulting string
     */
    public static String toWhitespaceSeparatedList(String[] l) {
        return toString(l, "\"", " ");
    }

    /**
     * Converts a string array into a string, optionally quoting each entry and with
     * a given separator.
     *
     * @param l         The list
     * @param quote     The quote string to use
     * @param separator The separator
     * @return The resulting string
     */
    public static String toString(String[] l, String quote, String separator) {
        if (l == null || l.length == 0)
            return null;

        if (quote == null)
            quote = "";
        if (separator == null)
            separator = "";

        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            sb.append(quote).append(s).append(quote + separator);
        }
        return sb.toString().trim();

    }

    public static String unpackAssetPath(String path, GraphicsQuality gq) {
        if (path.contains(Constants.STAR_SUBSTITUTE)) {
            // Start with current quality and scan to lower ones
            for (int i = gq.ordinal(); i >= 0; i--) {
                GraphicsQuality quality = GraphicsQuality.values()[i];
                String suffix = quality.suffix;

                String texSuffix = path.replace(Constants.STAR_SUBSTITUTE, suffix);
                if (GlobalConf.data.dataFileHandle(texSuffix).exists()) {
                    return texSuffix;
                }
            }
            // Try with no suffix
            String texNoSuffix = path.replace(Constants.STAR_SUBSTITUTE, "");
            if (GlobalConf.data.dataFileHandle(texNoSuffix).exists()) {
                return texNoSuffix;
            }
            // Try higher qualities
            int n = GraphicsQuality.values().length;
            for (int i = gq.ordinal(); i < n; i++) {
                GraphicsQuality quality = GraphicsQuality.values()[i];
                String suffix = quality.suffix;

                String texSuffix = path.replace(Constants.STAR_SUBSTITUTE, suffix);
                if (GlobalConf.data.dataFileHandle(texSuffix).exists()) {
                    return texSuffix;
                }
            }
            logger.error("Texture not found: " + path);
            return null;
        } else {
            return path;
        }
    }

    public static String unpackAssetPath(String tex) {
        return GlobalResources.unpackAssetPath(tex, GlobalConf.scene.GRAPHICS_QUALITY);
    }

    public static String unpackSkyboxSide(String skyboxLoc, String side) throws RuntimeException {
        FileHandle loc = GlobalConf.data.dataFileHandle(skyboxLoc);
        FileHandle[] files = loc.list();
        for (FileHandle file : files) {
            if (file.name().contains("_" + side + ".")) {
                // Found!
                return file.file().getAbsolutePath().replaceAll("\\\\", "/");
            }
        }
        throw new RuntimeException("Skybox side '" + side + "' not found in folder: " + skyboxLoc);
    }

    private static final IntBuffer buf = BufferUtils.newIntBuffer(16);

    public static synchronized String getGLExtensions() {
        String extensions = Gdx.gl.glGetString(GL30.GL_EXTENSIONS);
        if (extensions == null || extensions.isEmpty()) {
            Gdx.gl.glGetIntegerv(GL30.GL_NUM_EXTENSIONS, buf);
            int next = buf.get(0);
            String[] extensionsstr = new String[next];
            for (int i = 0; i < next; i++) {
                extensionsstr[i] = Gdx.gl30.glGetStringi(GL30.GL_EXTENSIONS, i);
            }
            extensions = TextUtils.arrayToStr(extensionsstr);
        } else {
            extensions = extensions.replaceAll(" ", "\n");
        }
        return extensions;
    }

    /**
     * Generates all combinations of all sizes of all the strings given in values
     *
     * @param values The input strings to combine
     * @return The combinations
     */
    public static String[] combinations(String[] values) {
        List<String> valueList = Arrays.asList(values);
        Array<String> combinations = new Array<>();
        // First, add empty
        combinations.add("");
        // Add all combinations
        int n = values.length;
        for (int nobj = 1; nobj <= n; nobj++) {
            // Iterate over all elements
            List<List<String>> res = combination(valueList, nobj);
            for (List<String> r : res) {
                // Concat all strings in r and add to combinations
                String value = "";
                for (String s : r)
                    value += s;
                combinations.add(value);
            }
        }
        return combinations.toArray(String.class);
    }

    /**
     * Generates all combinations of the given size using the elements in values.
     *
     * @param values The elements to combine
     * @param size   The size of the combinations
     * @param <T>    The type
     * @return The combinations
     */
    public static <T> List<List<T>> combination(List<T> values, int size) {

        if (0 == size) {
            return Collections.singletonList(Collections.emptyList());
        }

        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> combination = new LinkedList<List<T>>();

        T actual = values.iterator().next();

        List<T> subSet = new LinkedList<T>(values);
        subSet.remove(actual);

        List<List<T>> subSetCombination = combination(subSet, size - 1);

        for (List<T> set : subSetCombination) {
            List<T> newSet = new LinkedList<T>(set);
            newSet.add(0, actual);
            combination.add(newSet);
        }

        combination.addAll(combination(subSet, size));

        return combination;
    }

    public static String nObjectsToString(long objs) {
        if (objs > 1e9) {
            return String.format("%.1f", objs / 1.0e9) + " B";
        } else if (objs > 1e6) {
            return String.format("%.1f", objs / 1.0e6) + " M";
        } else if (objs > 1e3) {
            return String.format("%.1f", objs / 1.0e3) + " K";
        } else {
            return objs + "";
        }
    }
}
