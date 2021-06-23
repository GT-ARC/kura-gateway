package org.eclipse.kura.coapserver;


import java.net.SocketException;

import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

public class CoapServer {
	private static final org.slf4j.Logger s_logger = LoggerFactory.getLogger(CoapServer.class);

    private static final String APP_ID = "org.eclipse.kura.coapserver.CoapServer";

    protected void activate(ComponentContext componentContext) {

        s_logger.info("Bundle " + APP_ID + " has started!");

        s_logger.debug(APP_ID + ": This is a debug message.");
		  try {
	          // create server
	          CoapGatewayServer server = new CoapGatewayServer();
	          // add endpoints on all IP addresses
	          server.addEndpoints();
	          server.start();
	          s_logger.info("Coap Server is Started");
	      } catch (SocketException e) {
	          System.err.println("Failed to initialize server: " + e.getMessage());
	      }
        
    }

    protected void deactivate(ComponentContext componentContext) {

        s_logger.info("Bundle " + APP_ID + " has stopped!");

    }
}
