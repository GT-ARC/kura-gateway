package org.eclipse.kura.gateway;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//CoAP libraries
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.json.JSONObject;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;

public class CoapClientKura implements ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(StatusServlet.class);

	private static final String APP_ID = "org.eclipse.kura.gateway.Subscriber";

	public void coapClientKura(JSONObject sendMessage) {
		URI uri = null; // URI parameter of the requests

		try {
			/*Subscriber subscriber_xml = new Subscriber();
			String coapServerSD_IP = subscriber_xml.coapServerSD_IP;
			String coapServerSD_URI = subscriber_xml.coapServerSD_URI;
			s_logger.info("Posting to..." + "coap://" + coapServerSD_IP + ":5683/" + coapServerSD_URI);
			uri = new URI("coap://" + coapServerSD_IP + ":5683/" + coapServerSD_URI);*/
			
			String coapServerSD_IP = "172.24.1.138";
			String coapServerSD_URI = "coapSD";
			s_logger.info("Posting to..." + "coap://" + coapServerSD_IP + ":5683/" + coapServerSD_URI);
			uri = new URI("coap://" + coapServerSD_IP + ":5683/" + coapServerSD_URI);
		} catch (Exception e) {
			System.err.println("Invalid URI: " + e.getMessage());
			// System.exit(-1);
		}

		CoapClient client = null;
		/* start1 */
		CoapResponse response = null;

		try {
			client = new CoapClient(uri);
			// send JSON message to CoAP server
			response = client.post(sendMessage.toString(), MediaTypeRegistry.TEXT_PLAIN);
			s_logger.info("Message is posted to CoAP server. That is: " + sendMessage.toString());
		} catch (Exception e) {
			s_logger.info("Invalid response " + e.getMessage());
			// System.exit(-1);
		}
		return;
	}

	public void coapClientSendAgent(JSONObject sendtoAgent) {
		URI uri = null; // URI parameter of the requests

		try {
			Subscriber subscriber_xml = new Subscriber();
			String coapServerSD_IP_PORT_ID = subscriber_xml.coapServerSD_IP;
			String coapServerSD_IP = null;
			String coapServerSD_PORT = null;
			String coapServerSD_ID = null;
			String coapServerSD_URI = subscriber_xml.coapServerSD_URI;

			// split w.r.t ';' character to obtain ip-type pairs
			//String[] pairs = coapServerSD_IP_PORT_ID.split(";");
			
			//String[] pairs = CoapGatewayServer.pairs;
			for (int m = 0; m <CoapGatewayServer.pairs.length; m++) {
				// split w.r.t ':' to split ip-type pairs
				s_logger.info(CoapGatewayServer.pairs[m]);
				if(!CoapGatewayServer.pairs[m].equalsIgnoreCase("")) {
					String[] pairs_temp = CoapGatewayServer.pairs[m].split(":");
					if (Objects.equals(pairs_temp[2], sendtoAgent.getString("UUID"))) {
						coapServerSD_IP = pairs_temp[0].toString();
						coapServerSD_PORT = pairs_temp[1].toString();
						coapServerSD_ID = pairs_temp[2].toString();
						s_logger.info("Agent with ID=" + coapServerSD_IP + " is deteceted. Data of the sensor with ID=" + coapServerSD_ID + " will be sent to the Agent.");
					}
				}
				
			}

			if(Objects.nonNull(coapServerSD_IP)) {
				s_logger.info("Posting to..." + "coap://" + coapServerSD_IP + ":" + coapServerSD_PORT + "/" + coapServerSD_URI);
				uri = new URI("coap://" + coapServerSD_IP + ":" + coapServerSD_PORT + "/" + coapServerSD_URI);
			}
			else {
				s_logger.info("No agent found requesting the data of the sensor with ID=" + coapServerSD_ID);
			}

		} catch (Exception e) {
			System.err.println("Invalid URI: ");
			// System.exit(-1);
		}

		CoapClient client = null;
		/* start1 */
		CoapResponse response = null;

		try {
			client = new CoapClient(uri);
			// send JSON message to CoAP server
			sendtoAgent.put("Type", "Output");
			response = client.post(sendtoAgent.toString(), MediaTypeRegistry.TEXT_PLAIN);
			s_logger.info("Message is posted to the Agent. That is: " + sendtoAgent.toString());
		} catch (Exception e) {
			s_logger.info("Invalid response ");
			// System.exit(-1);
		}
		return;
	}
}

