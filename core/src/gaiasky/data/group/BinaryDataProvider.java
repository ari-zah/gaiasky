/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.scenegraph.ParticleGroup.ParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads arrays of star beans from binary files, usually to go in an octree.
 *
 * @author tsagrista
 */
public class BinaryDataProvider extends AbstractStarGroupDataProvider {

    public BinaryDataProvider(){
        super();
    }


    @Override
    public List<ParticleRecord> loadData(String file, double factor, boolean compatibility) {
        logger.info(I18n.bundle.format("notif.datafile", file));
        loadDataMapped(file, factor, compatibility);
        logger.info(I18n.bundle.format("notif.nodeloader", list.size(), file));

        return list;
    }


    @Override
    public List<ParticleRecord> loadData(InputStream is, double factor, boolean compatibility) {
        list = readData(is, compatibility);
        return list;
    }

    public void writeData(List<ParticleRecord> data, OutputStream out) {
        writeData(data, out, true);
    }
    public void writeData(List<ParticleRecord> data, OutputStream out, boolean compat) {
        // Wrap the FileOutputStream with a DataOutputStream
        DataOutputStream data_out = new DataOutputStream(out);
        try {
            // Size of stars
            data_out.writeInt(data.size());
            for (ParticleRecord sb : data) {
                writeParticleRecord(sb, data_out, compat);
            }

        } catch (Exception e) {
            logger.error(e);
        } finally {
            try {
                data_out.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }

    }

    protected void writeParticleRecord(ParticleRecord sb, DataOutputStream out) throws IOException {
        writeParticleRecord(sb, out, true);
    }

    /**
     * Write the star bean to the output stream
     *
     * @param sb     The star bean
     * @param out    The output stream
     * @param compat Use compatibility with DR1/DR2 model (with tycho ids)
     * @throws IOException
     */
    protected void writeParticleRecord(ParticleRecord sb, DataOutputStream out, boolean compat) throws IOException {
        // Double
        for (int i = 0; i < ParticleRecord.I_APPMAG; i++) {
            out.writeDouble(sb.data[i]);
        }
        // Float
        for (int i = ParticleRecord.I_APPMAG; i < ParticleRecord.I_HIP; i++) {
            out.writeFloat((float) sb.data[i]);
        }
        // Int
        out.writeInt((int) sb.data[ParticleRecord.I_HIP]);

        if (compat) {
            // 3 integers, keep compatibility
            out.writeInt(-1);
            out.writeInt(-1);
            out.writeInt(-1);
        }

        // Long
        out.writeLong(sb.id);

        String namesConcat = sb.namesConcat();
        out.writeInt(namesConcat.length());
        out.writeChars(namesConcat);
    }

    public List<ParticleRecord> readData(InputStream in) {
        return readData(in, true);
    }

    public List<ParticleRecord> readData(InputStream in, boolean compat) {
        List<ParticleRecord> data = null;
        DataInputStream data_in = new DataInputStream(in);

        try {
            // Read size of stars
            int size = data_in.readInt();
            data = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                data.add(readParticleRecord(data_in, compat));
            }

        } catch (IOException e) {
            logger.error(e);
        } finally {
            try {
                data_in.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }

        return data;
    }

    protected ParticleRecord readParticleRecord(DataInputStream in) throws IOException {
        return readParticleRecord(in, true);
    }

    /**
     * Read a star bean from input stream
     *
     * @param in     Input stream
     * @param compat Use compatibility with DR1/DR2 model (with tycho ids)
     * @return The star bean
     * @throws IOException
     */
    protected ParticleRecord readParticleRecord(DataInputStream in, boolean compat) throws IOException {
        double[] data = new double[ParticleRecord.SIZE];
        // Double
        for (int i = 0; i < ParticleRecord.I_APPMAG; i++) {
            data[i] = in.readDouble();
            if (i < 6)
                data[i] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // Float
        for (int i = ParticleRecord.I_APPMAG; i < ParticleRecord.I_HIP; i++) {
            data[i] = in.readFloat();
            if (i == ParticleRecord.I_SIZE)
                data[i] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // Int
        data[ParticleRecord.I_HIP] = in.readInt();

        if (compat) {
            // Skip unused tycho numbers, 3 Integers
            in.readInt();
            in.readInt();
            in.readInt();
        }

        Long id = in.readLong();
        int nameLength = in.readInt();
        StringBuilder namesConcat = new StringBuilder();
        for (int i = 0; i < nameLength; i++)
            namesConcat.append(in.readChar());
        String[] names = namesConcat.toString().split(Constants.nameSeparatorRegex);
        return new ParticleRecord(data, id, names);
    }

    @Override
    public List<ParticleRecord> loadDataMapped(String file, double factor, boolean compat) {
        try {
            FileChannel fc = new RandomAccessFile(GlobalConf.data.dataFile(file), "r").getChannel();

            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            // Read size of stars
            int size = mem.getInt();
            list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(readParticleRecord(mem, factor, compat));
            }

            fc.close();

            return list;

        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public ParticleRecord readParticleRecord(MappedByteBuffer mem, double factor, boolean compat) {
        double[] data = new double[ParticleRecord.SIZE];
        // Double
        for (int i = 0; i < ParticleRecord.I_APPMAG; i++) {
            data[i] = mem.getDouble();
            if (i < 3)
                data[i] *= factor;
            if (i < 6)
                data[i] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // Float
        for (int i = ParticleRecord.I_APPMAG; i < ParticleRecord.I_HIP; i++) {
            data[i] = mem.getFloat();
            if (i == ParticleRecord.I_SIZE)
                data[i] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // Int
        data[ParticleRecord.I_HIP] = mem.getInt();

        if (compat) {
            // Skip unused tycho numbers, 3 Integers
            mem.getInt();
            mem.getInt();
            mem.getInt();
        }

        Long id = mem.getLong();
        int nameLength = mem.getInt();
        StringBuilder namesConcat = new StringBuilder();
        for (int i = 0; i < nameLength; i++)
            namesConcat.append(mem.getChar());
        String[] names = namesConcat.toString().split(Constants.nameSeparatorRegex);

        return new ParticleRecord(data, id, names);
    }

}
