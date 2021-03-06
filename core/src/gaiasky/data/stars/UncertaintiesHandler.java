/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.stars;

import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.IStarFocus;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.util.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

public class UncertaintiesHandler implements IObserver {
    private static final boolean PRELOAD = true;
    private static UncertaintiesHandler singleton;

    public static UncertaintiesHandler getInstance() {
        if (singleton == null) {
            singleton = new UncertaintiesHandler();
        }
        return singleton;
    }

    private final String path;
    private final Set<Long> sourceIds;
    private final Array<ParticleGroup> particleGroups;
    private final double[][] colors;
    private int coloridx = 0;

    private UncertaintiesHandler() {
        path = "/media/tsagrista/Daten/Gaia/Coryn-data/data3/";

        particleGroups = new Array<ParticleGroup>();
        colors = new double[][] { { 0, 1, 0, 1 }, { 1, 0, 0, 1 }, { 0, 0, 1, 1 }, { 1, 1, 0, 1 }, { 1, 0, 1, 1 }, { 0, 1, 1, 1 }, { 0.5, 1, 1, 1 } };

        // Generate set with starids for which we have uncertainties
        sourceIds = new HashSet<Long>();
        Path dir = Paths.get(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{csv}")) {
            for (Path entry : stream) {
                String fname = entry.getFileName().toString();
                int pos = fname.lastIndexOf(".");
                if (pos > 0) {
                    fname = fname.substring(0, pos);
                    Long id = Long.parseLong(fname);
                    sourceIds.add(id);

                    if (PRELOAD) {
                        ParticleGroup pg = load(id);
                        GaiaSky.instance.sg.getRoot().addChild(pg, true);
                        particleGroups.add(pg);
                    }
                }
            }
        } catch (NoSuchFileException e) {
            Logger.getLogger(this.getClass()).error("Directory " + path + " not found");
        } catch (IOException e) {
            Logger.getLogger(this.getClass()).error(e);
        }

        EventManager.instance.subscribe(this, Events.SHOW_UNCERTAINTIES, Events.HIDE_UNCERTAINTIES);
    }

    public boolean containsStar(Long id) {
        return sourceIds != null && sourceIds.contains(id);
    }

    public boolean containsUncertainties() {
        return particleGroups.size > 0;
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case SHOW_UNCERTAINTIES:
            if (data[0] instanceof IStarFocus) {
                final IStarFocus s = (IStarFocus) data[0];
                GaiaSky.postRunnable(new Runnable() {

                    @Override
                    public void run() {
                        ParticleGroup pg = load(s.getCandidateId());

                        GaiaSky.instance.sg.getRoot().addChild(pg, true);
                        particleGroups.add(pg);
                    }

                });

            }
            break;
        case HIDE_UNCERTAINTIES:
            GaiaSky.postRunnable(new Runnable() {
                @Override
                public void run() {
                    for (ParticleGroup pg : particleGroups) {
                        GaiaSky.instance.sg.getRoot().removeChild(pg, true);
                    }
                    particleGroups.clear();
                }
            });
            break;
        default:
            break;
        }

    }

    private ParticleGroup load(long sid) {
        String source_id = String.valueOf(sid);
        ParticleGroup pg = new ParticleGroup();
        pg.setColor(colors[coloridx]);
        coloridx = (coloridx + 1) % colors.length;
        pg.setSize(3.5d);
        pg.setProfiledecay(0.3);
        pg.setName("");
        pg.setLabelcolor(new double[] { 1, 1, 1, 0 });
        pg.setLabelposition(new double[] { 0, 0, 0 });
        pg.setCt("Others");
        pg.setParent("Universe");
        pg.setProvider("gaiasky.data.group.UncertaintiesProvider");
        pg.setDatafile(path + source_id + ".csv");
        pg.initialize();
        return pg;
    }

}
