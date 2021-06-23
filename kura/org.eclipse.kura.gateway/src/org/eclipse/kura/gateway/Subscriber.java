package org.eclipse.kura.gateway;

import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.ServletException;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.ssl.SslManagerServiceOptions;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

//Readme https://www.eclipse.org/forums/index.php/t/1077639/

public class Subscriber implements ConfigurableComponent {

	private static final Logger s_logger = LoggerFactory.getLogger(Subscriber.class);

	private static final String APP_ID = "org.eclipse.kura.gateway.Subscriber";

	private static final Object[] str = null;
	/* new */
	public ConfigurationService m_configurationService;

	public Map<String, Object> m_properties;

	public static String coapServerSD_IP;

	public static String coapServerSD_URI;
	public  CoapGatewayServer coapServer = null; 
	// ----------------------------------------------------------------

	private static BundleContext s_context;

	private HttpService m_httpService;
	// ----------------------------------------------------------------

	public void setConfigurationService(ConfigurationService configurationService) {
		this.m_configurationService = configurationService;
	}

	public void unsetConfigurationService(ConfigurationService configurationService) {
		this.m_configurationService = null;
	}

	// ----------------------------------------------------------------
	public void setHttpService(HttpService httpService) {
		this.m_httpService = httpService;
	}

	public void unsetHttpService(HttpService httpService) {
		this.m_httpService = null;
	}
	// ----------------------------------------------------------------
	/* new */

	protected void activate(ComponentContext componentContext, BundleContext context, Map<String, Object> properties) {
		s_logger.info("Bundle " + APP_ID + " has started!");
		this.m_properties = properties;
		// ----------------------------------------------------------------
		s_context = context;

		HttpContext httpCtx = new OpenHttpContext(m_httpService.createDefaultHttpContext());
		try {
			// m_httpService.registerResources("/enerlife", "www", httpCtx);
			m_httpService.registerResources("/site", "webapp/old-index.html", httpCtx);
			m_httpService.registerResources("/static", "webapp/static", httpCtx);
			m_httpService.registerResources("/angular", "webapp/angular", httpCtx);
			m_httpService.registerResources("/dashboard", "webapp/index.html", httpCtx);

			// add below new services
			m_httpService.registerServlet("/api/status", new StatusServlet(), null, httpCtx);
		} catch (NamespaceException e) {
			s_logger.error("No http", e);
		} catch (ServletException e) {
			s_logger.error("No servlet", e);
		}
		Thread thread = null;
		try {
			coapServer = new CoapGatewayServer();
			thread = new Thread(coapServer);
			thread.start();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// ----------------------------------------------------------------
	}

	/*
	 * protected void activate(ComponentContext componentContext, Map<String,
	 * Object> properties) { s_logger.info("Bundle " + APP_ID +
	 * " has started with config!"); // updated(properties);
	 * 
	 * }
	 */
	protected void deactivate(ComponentContext componentContext, BundleContext context) {
		s_logger.info("Bundle " + APP_ID + " has stopped!");
		// ----------------------------------------------------------------
		m_httpService.unregister("/site");
		m_httpService.unregister("/static");
		m_httpService.unregister("/angular");
		m_httpService.unregister("/angular-site");
		m_httpService.unregister("/api/status");
		// ----------------------------------------------------------------
		coapServer.stop();
		
	}

	public void updated(Map<String, Object> properties) {
		this.m_properties = properties;
		String broker = (String) this.m_properties.get("broker.name");
		String clientId = (String) this.m_properties.get("clientId.name");
		String topic = (String) this.m_properties.get("topic.name");
		this.coapServerSD_IP = (String) this.m_properties.get("coapSDIP.name");
		this.coapServerSD_URI = (String) this.m_properties.get("coapSDURI.name");

		s_logger.info(
				"Updated coapServerSD_IP: " + coapServerSD_IP + " ," + " coapServerSD_URI." + coapServerSD_URI + ".");
		doDemo(broker, clientId, topic);

	}

	public void doDemo(String broker, String clientId, String topic) {
		MemoryPersistence persistence = new MemoryPersistence();

		try {
			MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);

			sampleClient.setCallback(new MqttCallback() {
				public void connectionLost(Throwable cause) {
				}

				public void messageArrived(String topic, MqttMessage message) throws Exception {
					/*
					 * String names[]; names = new String[] {"Ankit","Bohra","Xyz"};
					 */
					s_logger.info("Topic: " + topic.toString() + " ----- Message: " + message.toString());
					StatusServlet sendtoStatusServlet = new StatusServlet();
					JSONObject rawMessage = new JSONObject(message.toString());
					sendtoStatusServlet.jsonHandle(rawMessage);
					// runClient(1,topic.toString(), message.toString());
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0) {
					// TODO Auto-generated method stub
					s_logger.info("Message: " + arg0.toString());
				}
			});

			sampleClient.connect(connOpts);
			s_logger.info("SUBSCRIBER: subscribe");
			sampleClient.subscribe(topic);

		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	public void runClient(int i, String strT, String strM) {

		if (i == 1) {
			URI uri = null; // URI parameter of the request
			// connect to karaf server
			try {
				uri = new URI("coap://130.149.154.38:5683/Registration");
				System.out.println("Establishing connection to Directory Service...");
			} catch (Exception e) {
				System.err.println("Invalid URI: " + e.getMessage());
				// System.exit(-1);
			}
			// declare relevant functions
			CoapClient client = null;
			CoapResponse response = null;

			try {
				client = new CoapClient(uri);
				// send topic
				client.post(strT, MediaTypeRegistry.TEXT_PLAIN);
				// send message
				response = client.post(strM, MediaTypeRegistry.TEXT_PLAIN);

			} catch (Exception e) {
				s_logger.info("Invalid response " + e.getMessage());
				// System.exit(-1);
			}
			if (response != null) {

				System.out.println(response.getCode());
				System.out.println(response.getOptions());
				s_logger.info(response.getResponseText());
				s_logger.info(" ");
				System.out.println(System.lineSeparator() + "ADVANCED" + System.lineSeparator());
				// access advanced API with access to more details through
				// .advanced()
				System.out.println(Utils.prettyPrint(response));
			} else {
				System.out.println("No response received.");
			}
		}

	}

}