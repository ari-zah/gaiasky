/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.script;

import gaia.cu9.ari.gaiaorbit.util.Logger;
import py4j.DefaultGatewayServerListener;
import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

/**
 * The scripting server of Gaia Sky, backed by a Py4J {@link py4j.GatewayServer}.
 */
public class ScriptingServer {
    private static final Logger.Log logger = Logger.getLogger(ScriptingServer.class);

    private static GatewayServer gatewayServer;
    private static GatewayServerListener listener;

    public static void initialize() {
        if (gatewayServer == null) {
            gatewayServer = new GatewayServer(EventScriptingInterface.instance());
            listener = new DefaultGatewayServerListener() {

                @Override
                public void connectionStarted(Py4JServerConnection gatewayConnection) {
                    logger.debug("Connection started");
                }

                @Override
                public void connectionStopped(Py4JServerConnection gatewayConnection) {
                    logger.debug("Connection stopped");
                }

                @Override
                public void serverPostShutdown() {
                    logger.debug("Post shutdown");
                }

                @Override
                public void serverPreShutdown() {
                    logger.debug("Pre shutdown");
                }

                @Override
                public void serverStarted() {
                    logger.info("Server started");
                }

                @Override
                public void serverStopped() {
                    logger.info("Server stopped");
                    initialize();
                }

                @Override
                public void connectionError(Exception e) {
                    logger.error(e);
                    initialize();
                }

                @Override
                public void serverError(Exception e) {
                    logger.error(e);
                    initialize();
                }
            };
            gatewayServer.addListener(listener);
        }
        try {
            gatewayServer.start();
        }catch(Exception e){
            logger.error("Could not initialize the Py4J gateway server, is there another instance of Gaia Sky running?");
            logger.error(e);
        }
    }

    public static void dispose() {
        if (gatewayServer != null) {
            if (listener != null)
                gatewayServer.removeListener(listener);
            gatewayServer.shutdown();
        }
    }
}
