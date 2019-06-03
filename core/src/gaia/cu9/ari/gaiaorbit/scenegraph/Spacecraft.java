/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.math.Intersectord;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * The spacecraft.
 * 
 * @author tsagrista
 *
 */
public class Spacecraft extends GenericSpacecraft implements ILineRenderable, IObserver {
    private static final Log logger = Logger.getLogger(Spacecraft.class);
    
    /** This is the power **/
    public static final double thrustLength = 1e12d;

    /** Max speed in relativistic mode **/
    private static final double relativisticSpeedCap = Constants.C_US * 0.99999;

    /**
     * Factor (adapt to be able to navigate small and large scale structures
     **/
    public static final double[] thrustFactor = new double[13];
    static {
        double val = 0.1;
        for (int i = 0; i < 13; i++) {
            thrustFactor[i] = val * Math.pow(10, i);
        }
    }

    /** Seconds to reach full power **/
    public double fullPowerTime = 0.5;

    /** Force, acceleration and velocity **/
    public Vector3d force, accel, vel;
    /** Direction and up vectors **/
    public Vector3d direction, up;

    public Pair<Vector3d, Vector3d> dirup;

    /** Float counterparts **/
    public Vector3 posf, directionf, upf;

    /** Engine thrust vector **/
    public Vector3d thrust;

    /** Mass in kg **/
    public double mass;

    /** Factor hack **/
    public double sizeFactor = 10d;

    /** Only the rotation matrix **/
    public Matrix4 rotationMatrix;

    /**
     * Index of the current engine power setting
     */
    public int thrustFactorIndex = 0;

    /** Instantaneous engine power, this is in [0..1] **/
    public double enginePower;

    /** Yaw, pitch and roll **/
    // power in each angle in [0..1]
    public double yawp, pitchp, rollp;
    // angular forces
    public double yawf, pitchf, rollf;
    // angular accelerations in deg/s^2
    public double yawa, pitcha, rolla;
    // angular velocities in deg/s
    public double yawv, pitchv, rollv;
    // angles in radians
    public double yaw, pitch, roll;

    // Are we in the process of stabilising or stopping the spaceship?
    public boolean leveling, stopping;

    /** Aux vectors **/
    private Quaternion qf;

    private boolean render;

    public Spacecraft() {
        super();
        ct = new ComponentTypes(ComponentType.Satellites);
        localTransform = new Matrix4();
        rotationMatrix = new Matrix4();
        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD);

        // position attributes
        force = new Vector3d();
        accel = new Vector3d();
        vel = new Vector3d();

        // orientation
        pos.set(1e7 * Constants.KM_TO_U, 0, 1e8 * Constants.KM_TO_U);
        direction = new Vector3d(1, 0, 0);
        up = new Vector3d(0, 1, 0);
        dirup = new Pair<Vector3d, Vector3d>(direction, up);

        posf = new Vector3();
        directionf = new Vector3(1, 0, 0);
        upf = new Vector3(0, 1, 0);

        // engine thrust direction
        // our spacecraft is a rigid solid so thrust is always the camera direction vector
        thrust = new Vector3d(direction).scl(thrustLength);
        enginePower = 0;

        // not stabilising
        leveling = false;

        qf = new Quaternion();
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if (mc != null) {
            mc.doneLoading(manager, localTransform, null);
        }
        // Broadcast me
        EventManager.instance.post(Events.SPACECRAFT_LOADED, this);

        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD, Events.SPACECRAFT_STABILISE_CMD, Events.SPACECRAFT_STOP_CMD, Events.SPACECRAFT_THRUST_DECREASE_CMD, Events.SPACECRAFT_THRUST_INCREASE_CMD, Events.SPACECRAFT_THRUST_SET_CMD);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case CAMERA_MODE_CMD:
            CameraMode mode = (CameraMode) data[0];
            if (mode == CameraMode.Spacecraft) {
                render = true;
            } else {
                render = false;
            }
            break;
        case SPACECRAFT_STABILISE_CMD:
            leveling = (Boolean) data[0];
            break;
        case SPACECRAFT_STOP_CMD:
            stopping = (Boolean) data[0];
            break;
        case SPACECRAFT_THRUST_DECREASE_CMD:
            decreaseThrustFactorIndex(true);
            break;
        case SPACECRAFT_THRUST_INCREASE_CMD:
            increaseThrustFactorIndex(true);
            break;
        case SPACECRAFT_THRUST_SET_CMD:
            setThrustFactorIndex((Integer) data[0], false);
            break;
        default:
            break;
        }

    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);

        if (render) {
            EventManager.instance.post(Events.SPACECRAFT_INFO, yaw % 360, pitch % 360, roll % 360, vel.len(), thrustFactor[thrustFactorIndex], enginePower, yawp, pitchp, rollp);
        }

    }

    protected void updateLocalTransform() {
        // Local transform
        try {
            localTransform.idt().setToLookAt(posf, directionf.add(posf), upf).inv();
            float sizeFac = (float) (sizeFactor * size);
            localTransform.scale(sizeFac, sizeFac, sizeFac);

            // Rotation for attitude indicator
            rotationMatrix.idt().setToLookAt(directionf, upf);
            rotationMatrix.getRotation(qf);
        } catch (Exception e) {
        }

    }

    public Vector3d computePosition(double dt, IFocus closest, double enginePower, Vector3d thrust, Vector3d direction, Vector3d force, Vector3d accel, Vector3d vel, Vector3d pos) {
        enginePower = Math.signum(enginePower);
        // Compute force from thrust
        thrust.set(direction).scl(thrustLength * thrustFactor[thrustFactorIndex] * enginePower);
        force.set(thrust);

        // Scale force if relativistic effects are on
        if (GlobalConf.runtime.RELATIVISTIC_ABERRATION) {
            double speed = vel.len();
            double scale = (relativisticSpeedCap - speed) / relativisticSpeedCap;
            force.scl(scale);
        }

        double friction = (GlobalConf.spacecraft.SC_HANDLING_FRICTION * 2e16) * dt;
        force.add(aux3d1.get().set(vel).scl(-friction));

        if (stopping) {
            double speed = vel.len();
            if (speed != 0) {
                enginePower = -1;
                thrust.set(vel).nor().scl(thrustLength * thrustFactor[thrustFactorIndex] * enginePower);
                force.set(thrust);
            }

            Vector3d nextvel = aux3d3.get().set(force).scl(1d / mass).scl(Constants.M_TO_U).scl(dt).add(vel);

            if (vel.angle(nextvel) > 90) {
                setEnginePower(0);
                force.scl(0);
                vel.scl(0);
                EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, false);
            }
        }

        // Compute new acceleration in m/s^2
        accel.set(force).scl(1d / mass);

        // Integrate other quantities
        // convert metres to internal units so we have the velocity in u/s
        Vector3d acc = aux3d1.get().set(accel).scl(Constants.M_TO_U);

        if (GlobalConf.spacecraft.SC_VEL_TO_DIRECTION) {
            double vellen = vel.len();
            vel.set(direction).nor().scl(vellen);
        }
        vel.add(acc.scl(dt));

        Vector3d velo = aux3d2.get().set(vel);
        // New position in auxd3
        Vector3d position = aux3d3.get().set(pos).add(velo.scl(dt));
        // Check collision!
        if (closest != null && closest != this && !this.copy) {
            double twoRadiuses = closest.getRadius() + this.getRadius();
            // d1 is the new distance to the centre of the object
            if (!vel.isZero() && Intersectord.distanceSegmentPoint(pos, position, closest.getPos()) < twoRadiuses) {
                logger.info( "Crashed against " + closest.getName() + "!");

                Array<Vector3d> intersections = Intersectord.intersectRaySphere(pos, position, closest.getPos(), twoRadiuses);

                if (intersections.size >= 1) {
                    pos.set(intersections.get(0));
                }

                stopAllMovement();
            } else if (pos.dst(closest.getPos()) < twoRadiuses) {
                Vector3d newpos = aux3d1.get().set(pos).sub(closest.getPos()).nor().scl(pos.dst(closest.getPos()));
                pos.set(newpos);
            } else {
                pos.set(position);
            }
        } else {
            pos.set(position);
        }

        return pos;
    }

    public double computeDirectionUp(double dt, Pair<Vector3d, Vector3d> pair) {
        // Yaw, pitch and roll
        yawf = yawp * GlobalConf.spacecraft.SC_RESPONSIVENESS;
        pitchf = pitchp * GlobalConf.spacecraft.SC_RESPONSIVENESS;
        rollf = rollp * GlobalConf.spacecraft.SC_RESPONSIVENESS;

        // Friction
        double friction = (GlobalConf.spacecraft.SC_HANDLING_FRICTION * 2e7) * dt;
        yawf -= yawv * friction;
        pitchf -= pitchv * friction;
        rollf -= rollv * friction;

        // accel
        yawa = yawf / mass;
        pitcha = pitchf / mass;
        rolla = rollf / mass;

        // vel
        yawv += yawa * dt;
        pitchv += pitcha * dt;
        rollv += rolla * dt;

        // pos
        double yawdiff = (yawv * dt) % 360d;
        double pitchdiff = (pitchv * dt) % 360d;
        double rolldiff = (rollv * dt) % 360d;

        Vector3d direction = pair.getFirst();
        Vector3d up = pair.getSecond();

        // apply yaw
        direction.rotate(up, yawdiff);

        // apply pitch
        Vector3d aux1 = aux3d1.get().set(direction).crs(up);
        direction.rotate(aux1, pitchdiff);
        up.rotate(aux1, pitchdiff);

        // apply roll
        up.rotate(direction, -rolldiff);

        return rolldiff;
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        if (yawv != 0 || pitchv != 0 || rollv != 0 || vel.len2() != 0 || render) {
            // We use the simulation time for the integration
            double dt = GaiaSky.instance.getT();
            // Poll keys
            pollKeys(Gdx.graphics.getDeltaTime());

            /** POSITION **/
            pos = computePosition(dt, camera.getClosest2(), enginePower, thrust, direction, force, accel, vel, pos);

            /**
             * SCALING FACTOR - counteracts double precision problems at very
             * large distances
             **/
            sizeFactor = MathUtilsd.lint(pos.len(), 100 * Constants.AU_TO_U, 5000 * Constants.PC_TO_U, 10, 10000);

            if (leveling) {
                // No velocity, we just stop Euler angle motions
                if (yawv != 0) {
                    yawp = -Math.signum(yawv) * MathUtilsd.clamp(Math.abs(yawv), 0, 1);
                }
                if (pitchv != 0) {
                    pitchp = -Math.signum(pitchv) * MathUtilsd.clamp(Math.abs(pitchv), 0, 1);
                }
                if (rollv != 0) {
                    rollp = -Math.signum(rollv) * MathUtilsd.clamp(Math.abs(rollv), 0, 1);
                }
                if (Math.abs(yawv) < 1e-3 && Math.abs(pitchv) < 1e-3 && Math.abs(rollv) < 1e-3) {
                    setYawPower(0);
                    setPitchPower(0);
                    setRollPower(0);

                    yawv = 0;
                    pitchv = 0;
                    rollv = 0;
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, false);
                }
            }

            double rolldiff = computeDirectionUp(dt, dirup);

            double len = direction.len();
            pitch = Math.asin(direction.y / len);
            yaw = Math.atan2(direction.z, direction.x);
            roll += rolldiff;

            pitch = Math.toDegrees(pitch);
            yaw = Math.toDegrees(yaw);
        }
        // Update float vectors
        aux3d1.get().set(pos).add(camera.getInversePos()).put(posf);
        direction.put(directionf);
        up.put(upf);

    }

    private void pollKeys(double dt) {
        double powerStep = dt / fullPowerTime;
        if (Gdx.input.isKeyPressed(Keys.W))
            setEnginePower(enginePower + powerStep);
        if (Gdx.input.isKeyPressed(Keys.S))
            setEnginePower(enginePower - powerStep);

        if (Gdx.input.isKeyPressed(Keys.A))
            setRollPower(rollp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.D))
            setRollPower(rollp - powerStep);

        if (Gdx.input.isKeyPressed(Keys.DOWN))
            setPitchPower(pitchp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.UP))
            setPitchPower(pitchp - powerStep);

        if (Gdx.input.isKeyPressed(Keys.LEFT))
            setYawPower(yawp + powerStep);
        if (Gdx.input.isKeyPressed(Keys.RIGHT))
            setYawPower(yawp - powerStep);
    }

    public void stopAllMovement() {
        setEnginePower(0);
        //vel.set(0, 0, 0);

        setYawPower(0);
        setPitchPower(0);
        setRollPower(0);

        //yawv = 0;
        //pitchv = 0;
        //rollv = 0;

        leveling = false;
        stopping = false;

    }

    /**
     * Sets the current engine power
     * 
     * @param enginePower
     *            The power in [-1..1]
     */
    public void setEnginePower(double enginePower) {
        this.enginePower = MathUtilsd.clamp(enginePower, -1, 1);
    }

    /**
     * Sets the current yaw power
     * 
     * @param yawp
     *            The yaw power in [-1..1]
     */
    public void setYawPower(double yawp) {
        this.yawp = MathUtilsd.clamp(yawp, -1, 1);
    }

    /**
     * Sets the current pitch power
     * 
     * @param pitchp
     *            The pitch power in [-1..1]
     */
    public void setPitchPower(double pitchp) {
        this.pitchp = MathUtilsd.clamp(pitchp, -1, 1);
    }

    /**
     * Sets the current roll power
     * 
     * @param rollp
     *            The roll power in [-1..1]
     */
    public void setRollPower(double rollp) {
        this.rollp = MathUtilsd.clamp(rollp, -1, 1);
    }

    public void increaseThrustFactorIndex(boolean broadcast) {
        thrustFactorIndex = (thrustFactorIndex + 1) % thrustFactor.length;
        logger.info( "Thrust factor: " + thrustFactor[thrustFactorIndex]);
        if (broadcast)
            EventManager.instance.post(Events.SPACECRAFT_THRUST_INFO, thrustFactorIndex);
    }

    public void decreaseThrustFactorIndex(boolean broadcast) {
        thrustFactorIndex = thrustFactorIndex - 1;
        if (thrustFactorIndex < 0)
            thrustFactorIndex = thrustFactor.length - 1;
        logger.info( "Thrust factor: " + thrustFactor[thrustFactorIndex]);
        if (broadcast)
            EventManager.instance.post(Events.SPACECRAFT_THRUST_INFO, thrustFactorIndex);
    }

    public void setThrustFactorIndex(int i, boolean broadcast) {
        assert i >= 0 && i < thrustFactor.length : "Index " + i + " out of range of thrustFactor vector: [0.." + (thrustFactor.length - 1);
        thrustFactorIndex = i;
        logger.info( "Thrust factor: " + thrustFactor[thrustFactorIndex]);
        if (broadcast)
            EventManager.instance.post(Events.SPACECRAFT_THRUST_INFO, thrustFactorIndex);
    }

    /**
     * Adds this entity to the necessary render lists after the distance to the
     * camera and the view angle have been determined.
     */
    protected void addToRenderLists(ICamera camera) {
        if (this.viewAngleApparent > TH_ANGLE_POINT * camera.getFovFactor()) {
            super.addToRenderLists(camera);
            if (GlobalConf.spacecraft.SC_SHOW_AXES)
                addToRender(this, RenderGroup.LINE);
        }
    }

    public void setModel(ModelComponent mc) {
        this.mc = mc;
    }

    /**
     * Sets the absolute size of this entity
     * 
     * @param size
     */
    public void setSize(Double size) {
        this.size = size.floatValue() * (float) Constants.KM_TO_U;
    }

    public void setSize(Long size) {
        this.size = (float) size * (float) Constants.KM_TO_U;
    }

    @Override
    public double getRadius() {
        return super.getRadius() * sizeFactor;
    }

    @Override
    public double getSize() {
        return super.getSize() * sizeFactor;
    }

    public void setMass(Double mass) {
        this.mass = mass;
    }

    public boolean isStopping() {
        return stopping;
    }

    public boolean isStabilising() {
        return leveling;
    }

    @Override
    public double getDistToCamera() {
        return distToCamera;
    }

    public Quaternion getRotationQuaternion() {
        return qf;
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    public void dispose() {
        super.dispose();
    }

    /** Model rendering. Spacecraft in spacecraft mode is not affected by the relativistic aberration **/
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        ICamera cam = GaiaSky.instance.getICamera();
        prepareShadowEnvironment();
        mc.touch();
        mc.setTransparency(alpha * fadeOpacity);
        if (cam.getMode().isSpacecraft())
            // In Spacecraft mode, we are not affected by relativistic aberration or Doppler shift
            mc.updateRelativisticEffects(cam, 0);
        else
            mc.updateRelativisticEffects(cam);
        modelBatch.render(mc.instance, mc.env);
    }

    /** Model opaque rendering for light glow pass. Do not render shadows **/
    @Override
    public void renderOpaque(IntModelBatch modelBatch, float alpha, double t) {
        ICamera cam = GaiaSky.instance.getICamera();
        mc.touch();
        mc.setTransparency(alpha * fadeOpacity);
        if (cam.getMode().isSpacecraft())
            // In Spacecraft mode, we are not affected by relativistic aberration or Doppler shift
            mc.updateRelativisticEffects(cam, 0);
        else
            mc.updateRelativisticEffects(cam);
        modelBatch.render(mc.instance, mc.env);
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        // Direction
        Vector3d d = aux3d1.get().set(direction);
        d.nor().scl(.5e-4 * sizeFactor);
        renderer.addLine(this, posf.x, posf.y, posf.z, posf.x + d.x, posf.y + d.y, posf.z + d.z, 1, 0, 0, 1);

        // Up
        Vector3d u = aux3d1.get().set(up);
        u.nor().scl(.2e-4 * sizeFactor);
        renderer.addLine(this, posf.x, posf.y, posf.z, posf.x + u.x, posf.y + u.y, posf.z + u.z, 0, 0, 1, 1);

    }

    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        Spacecraft copy = super.getSimpleCopy();
        copy.force.set(this.force);
        copy.accel.set(this.accel);
        copy.vel.set(this.vel);

        copy.fullPowerTime = this.fullPowerTime;

        copy.posf.set(this.posf);
        copy.direction.set(this.direction);
        copy.directionf.set(this.directionf);
        copy.up.set(this.up);
        copy.upf.set(this.upf);
        copy.thrust.set(this.thrust);

        copy.mass = this.mass;
        copy.sizeFactor = this.sizeFactor;

        copy.rotationMatrix.set(this.rotationMatrix);

        copy.thrustFactorIndex = this.thrustFactorIndex;

        copy.enginePower = this.enginePower;

        copy.yawp = this.yawp;
        copy.yawf = this.yawf;
        copy.yawa = this.yawa;
        copy.yawv = this.yawv;

        copy.pitchp = this.pitchp;
        copy.pitchf = this.pitchf;
        copy.pitcha = this.pitcha;
        copy.pitchv = this.pitchv;

        copy.rollp = this.rollp;
        copy.rollf = this.rollf;
        copy.rolla = this.rolla;
        copy.rollv = this.rollv;

        copy.leveling = this.leveling;
        copy.stopping = this.stopping;

        return (T) copy;
    }

    @Override
    protected float labelFactor() {
        return 0;
    }

    @Override
    protected boolean mustUpdatePosition(ITimeFrameProvider time) {
        return true;
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

    @Override
    public int getGlType() {
        return GL20.GL_LINES;
    }

}
