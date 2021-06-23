package org.eclipse.kura.gateway;
/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 *    Kai Hudalla (Bosch Software Innovations GmbH) - add endpoints for all IP addresses
 ******************************************************************************/

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Objects;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.network.Endpoint;
//import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class CoapGatewayServer extends CoapServer implements Runnable {
	private static final Logger s_logger = LoggerFactory.getLogger(CoapGatewayServer.class);

	private static final int COAP_PORT = 5683; //NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
	public static String[] pairs = new String[20];
	int agentCounter = 0;
    /*
     * Application entry point.
     */
    /**
     * Add individual endpoints listening on default CoAP port on all IPv4 addresses of all network interfaces.
     */
    public  void addEndpoints() {
        for (InetAddress addr : getInterfaces()) {
                // only binds to IPv4 addresses and localhost
                if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
                        InetSocketAddress bindToAddress = new InetSocketAddress(addr, COAP_PORT);
                        s_logger.info("Coap Server is CoapGatewayServer:"+bindToAddress.toString());
                        addEndpoint(new CoAPEndpoint(bindToAddress));
                }
        }
    }
 
    public static Collection<InetAddress> getInterfaces() {

		Collection<InetAddress> interfaces = new LinkedList<InetAddress>();
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			while (nets.hasMoreElements()) {
				Enumeration<InetAddress> inetAddresses = nets.nextElement().getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					interfaces.add(inetAddresses.nextElement());
				}
			}
		} catch (SocketException e) {
			//LOGGER.log(Level.SEVERE, "Could not fetch all interface addresses", e);
		}
		return interfaces;
	
    }

    /*
     * Constructor for a new Hello-World server. Here, the resources
     * of the server are initialized.
     */
    public CoapGatewayServer() throws SocketException {
    	s_logger.info("Coap Server is CoapGatewayServer");
        // provide an instance of a Hello-World resource
        add(new HelloWorldResource());
        // 
        for(int i = 0 ; i < pairs.length; i ++) {
        	pairs[i] = "";
        }
    }

    /*
     * Definition of the Hello-World Resource
     */
    class HelloWorldResource extends CoapResource {
        
        public HelloWorldResource() {
            // set resource identifier
            super("coapKura");
            //s_logger.info("Coap Server is HelloWorldResource");
            // set display name
            getAttributes().setTitle("coapKura Mqtt Publish Center");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
        	s_logger.info("Coap Server is handleGET");
            exchange.respond("Welcome to MQTT Publisher!");
        }
        public void handlePOST(CoapExchange exchange) {
        	s_logger.info("Coap Server is handlePOST -> requester: " + exchange.getSourceAddress());
        	s_logger.info("Coap Server is handlePOST -> message: " + exchange.getRequestText());
        	JSONObject rawMessage = new JSONObject(exchange.getRequestText());
        	String Type = rawMessage.getString("Type");
        	//message type : removal
        	if(Objects.equals("Removal", Type)) {
        		//pass topic and json to mqtt publisher
        		System.out.println("Got a Removal send request");
        		mqttPub("Removal/", rawMessage);
        		exchange.respond("Message with topic " + Type + " and payload " + rawMessage.toString() + " is sent to broker.");
        	}
        	//message type : KeepAlive
        	else if(Objects.equals("KeepAlive", Type)) {
        		//pass topic and json to mqtt publisher
        		System.out.println("Got a KeepAlive send request");
        		mqttPub("KeepAlive/", rawMessage);
        		exchange.respond("Message with topic " + Type + " and payload " + rawMessage.toString() + " is sent to broker.");
        	}
        	//message type : Deactivation
        	else if(Objects.equals("Deactivation", Type)) {
        		//pass topic and json to mqtt publisher
        		System.out.println("Got a Deactivation send request");
        		mqttPub("Deactivation/", rawMessage);
        		exchange.respond("Message with topic " + Type + " and payload " + rawMessage.toString() + " is sent to broker.");
        	} 
        	else if(Objects.equals("Agent-Registration", Type)) {
        	 
        		s_logger.info("Got a Agent-Registration send request");
        		
        		if (!this.checkAgentRegistration(rawMessage.getString("IP"), rawMessage.getString("Port"))) {
        			for (int m = 0; m < pairs.length; m++) {
        				if(pairs[m].equalsIgnoreCase("")) {
        					pairs[m] = rawMessage.getString("IP") + ":" + rawMessage.getString("Port") + ":" + rawMessage.getString("DeviceId");
        					s_logger.info(pairs[m]);
        					m = pairs.length;
        				}
        			}	
        		}
        		exchange.respond("Message with topic " + Type + " and payload " + rawMessage.toString() + " is sent to broker.");
        		
        	} else if(Objects.equals("Agent-Deregistration", Type)) {
        		s_logger.info("Got a Agent-Deregistration send request");
    			for (int m = 0; m < pairs.length; m++) {
    				if(!pairs[m].equalsIgnoreCase("")) {
    					String[] pairs_temp = pairs[m].split(":");
    					if(pairs_temp[0].equalsIgnoreCase( rawMessage.getString("IP")) && pairs_temp[1].equalsIgnoreCase(rawMessage.getString("Port"))) {
    						s_logger.info(pairs[m]);
        					pairs[m] = "";
    					}
    				}
    			}
        		exchange.respond("Message with topic " + Type + " and payload " + rawMessage.toString() + " is sent to broker.");
        	}
        }
    	public boolean checkAgentRegistration(String ip, String port){
			for(int m = 0; m < pairs.length; m++) {
				if(!pairs[m].equalsIgnoreCase("")) {
					String[] pairs_temp = pairs[m].split(":");
					if(pairs_temp[m].equalsIgnoreCase(ip) && pairs_temp[m].equalsIgnoreCase(port)) {
						return true;
					}	
				}
			}
			return false;
		}
		
        public void mqttPub(String topic, JSONObject rawMessage) {
        	//String topic        = "MQTT Examples";
            String content      = rawMessage.toString();
            //String IP = rawMessage.getJSONObject("Content").getString("DeviceIP");
            int qos             = 2;
            String broker       = "tcp://"+"172.24.1.51"+":1883";
            String clientId     = "JavaSample";
            MemoryPersistence persistence = new MemoryPersistence();

            try {
                MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                System.out.println("Connecting to broker: "+broker);
                sampleClient.connect(connOpts);
                System.out.println("Connected");
                System.out.println("Publishing message: "+content);
                System.out.println("Publishing topic: "+topic);
                MqttMessage message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                System.out.println("Message published");
                sampleClient.disconnect();
                System.out.println("Disconnected");
            } catch(MqttException me) {
                System.out.println("reason "+me.getReasonCode());
                System.out.println("msg "+me.getMessage());
                System.out.println("loc "+me.getLocalizedMessage());
                System.out.println("cause "+me.getCause());
                System.out.println("excep "+me);
                me.printStackTrace();
            }
            return;
        	
        }
        		
    }

	@Override
	public void run() {
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
}
