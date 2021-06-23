package org.eclipse.kura.gateway;

import de.gtarc.chariot.connectionapi.Connection;
import de.gtarc.chariot.connectionapi.ConnectionException;
import de.gtarc.chariot.connectionapi.ConnectionListener;
import de.gtarc.chariot.connectionapi.ConnectionStatus;
import de.gtarc.chariot.connectionapi.DeviceConnection;
import de.gtarc.chariot.connectionapi.impl.MqttConnectionImpl;
import de.gtarc.chariot.deviceapi.DeviceProperty;
import de.gtarc.chariot.deviceapi.Property;
import de.gtarc.chariot.deviceapi.impl.DeviceBuilder;
import de.gtarc.chariot.deviceapi.impl.DevicePropertyImpl;
import de.gtarc.chariot.messageapi.AbstractMessage;
import de.gtarc.chariot.messageapi.AbstractPayload;
import de.gtarc.chariot.messageapi.IMessage;
import de.gtarc.chariot.messageapi.impl.MessageBuilder;
import de.gtarc.chariot.messageapi.impl.PayloadDevice;
import de.gtarc.chariot.messageapi.impl.PayloadDeviceProperty;

import de.gtarc.chariot.registrationapi.client.RegistrationClient;
import de.gtarc.chariot.registrationapi.client.util.RegistrationResult;
import de.gtarc.chariot.registrationapi.messages.payload.PayloadResponse;
import de.gtarc.commonapi.utils.Location;


import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/***
 * The purpose of this class is to establish a bridge between iolite devices and chariot agent devices.
 * IOLITE device registration to CHARIOT Register Agent starts with the inclusion of its iolite driver. 
 * TODO: https://gitlab.dai-labor.de/chariot/agent-services/blob/develop-updateTo0.0.5/colorSensorAgent/src/main/java/de/gtarc/chariot/colorsensoragent/DeviceBean.java 
 * @author cemakpolat
 */

public class DeviceManager implements ConnectionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceManager.class);
	private static final String reId = "a1f114f5-f47f-449d-b579-6605bba061c8"; // unique, does not change

	// MQTT configurations
	private String host = "tcp://0.0.0.0:1883"; // Broker address
	private static String username = "plbwvpgf";
	private static String password = "mJTPb6z12Bag";
	private String clientId = reId; //UUID.fromString(reId);
	// connection
	private MqttConnectionImpl reConn = null; 
	// topics 
	private String topic = "re/" + reId + "/"; // register this topic to receive messages from devices
	private String deviceTopic = "devices/";

	// received messages, TODO: Export them to another class
	private static final String SUCCESS = "Success"; 

	
	private static DeviceManager dmanager = new DeviceManager();
	public static DeviceManager getInstance() {
		return dmanager;
	}

	List<DeviceStatus> devicesStatus = Collections.synchronizedList(new ArrayList<DeviceStatus>());
	Device ioliteDevice = null;

	public DeviceManager() {
		try {
			this.initiateREConnection();
		} catch (ConnectionException e1) {
			LOGGER.debug("MQTT connection failed");
			e1.printStackTrace();
		}
	}

	/**
	 * Add IOLITE device when it is connected
	 *
	 * @param comingDevice
	 */
	public void addDevice(Device comingDevice) {
		synchronized (devicesStatus) {
			// Must be in synchronized block

			for (DeviceStatus status : devicesStatus) {
				// 
//				if (status.kuraDevice.getIdentifier().equalsIgnoreCase(comingDevice.getIdentifier())) {
//					LOGGER.debug("device exists with the id of " + comingDevice.getIdentifier() + " driver-id:"
//							+ comingDevice.getDriverId());
//					return;
//				}
				
			}
			try {
				// initiate registration
				boolean succeded = false;
				for (int i = 0; i < 5; i++) {
					succeded = this.registerDevice(comingDevice);
					if (succeded)
						break;
					Thread.sleep(1000);
				}
				if (!succeded) {
					LOGGER.error("Device with ID:" + "<device-identifier>" + " could not be registered!");
				}
			} catch (ConnectionException | InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Remove deice from the system when the device i
	 *
	 * @param leavingDevice
	 * @throws ConnectionException
	 */
	public void removeDevice(Device leavingDevice) throws ConnectionException {
		synchronized (devicesStatus) {
			for (DeviceStatus ds : this.devicesStatus) {
//				if (ds.kuraDevice.getIdentifier().equalsIgnoreCase(leavingDevice.getIdentifier())) {
//					try {
//						boolean succeded = this.unregisterDevice(ds.chDevice);
//						if (succeded) {
//							// leavingDevice.detach(new
//							// GenericDeviceObserver(leavingDevice.getIdentifier()));
//							//ds.kuraDevice.detach(new GenericDeviceObserver(leavingDevice.getIdentifier()));
//							devicesStatus.remove(ds);
//						}
//					} catch (InterruptedException | ExecutionException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
			}
		}
	}
	
	/**
	 * Register IOLITE device as CHARIOT Device using registration api
	 * @param comingDevice
	 * @return
	 * @throws ConnectionException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public boolean registerDevice(Device comingDevice)
			throws ConnectionException, InterruptedException, ExecutionException, TimeoutException {

		de.gtarc.chariot.deviceapi.Device device = createCHARIOTDevice(comingDevice);

		DeviceConnection conn = new MqttConnectionImpl(host, username, password, "registration-" + clientId);
		conn.connect();
		device.setConnectionHandler(conn);

		RegistrationClient registrationHandler = new RegistrationClient();
		Future<RegistrationResult> result = registrationHandler.registerREDevice(device, reId.toString(), "", "c",
				device.getVendor(), device.getType().toString());

		AbstractMessage message = result.get(5, TimeUnit.SECONDS).result;
		String response = ((PayloadResponse) message.getPayload()).response;
		if (response.equalsIgnoreCase(SUCCESS)) {
			conn.disconnect();
			System.out.println("\n\nDevice with UUID " + device.getUUIdentifier() + " is registered!\n\n");
			LOGGER.info("Device with UUID " + device.getUUIdentifier() + " is registered!");
			
			//comingDevice.attach(new GenericDeviceObserver(comingDevice.getIdentifier())); // observe any change in the device property
			
			devicesStatus.add(new DeviceStatus(comingDevice, device, "/device/" + device.getUUIdentifier(),
					DeviceState.REGISTERED)); // add the device and chariot device to keep update all together
			return true;
		} else {
			LOGGER.info("Device Registration with UUID " + device.getUUIdentifier() + " is failed!");
		}
		return false;
	}
	
	/**
	 * Initiate RE connection with CHARIOT MQTT Broker
	 * @throws ConnectionException
	 */
	
	public void initiateREConnection() throws ConnectionException {
		LOGGER.debug("CONNECT TO MQTT");
		reConn = new MqttConnectionImpl(host, username, password, clientId);
		reConn.connect();
		reConn.subscribeTopic(topic);
		reConn.addConnectionListener(this);
	}

	/**
	 * The whole IOLITE device data model is updated
	 *
	 * @param device
	 */
	public de.gtarc.chariot.deviceapi.Device convertPayloadDeviceToDevice(String id, PayloadDevice pdevice) {
		for (DeviceStatus ds : this.devicesStatus) {
			de.gtarc.chariot.deviceapi.Device device = ds.chDevice;
			if(pdevice.getId().equalsIgnoreCase(id)) {
				device.setFriendlyName(pdevice.getName());
				device.setName(pdevice.getId());
				device.setType(pdevice.getObjectType());
				//device.setDeviceStatus(new DeviceStatus()); // TODO: add here the LOCATION
				for(PayloadDeviceProperty prop: pdevice.getProperties()) {
					device.addMandatoryProperties(new DevicePropertyImpl(Long.parseLong(prop.getTimestamp()),prop.getKey(),prop.getType(), prop.getValue().toString(), prop.getUnit(), prop.getWritable().toString(), prop.getMin(), prop.getMax()));	
				}
				return device;
			}
		}	
		return null;
	}
	
	/**
	 * Device property change sent by IOLITE device
	 * 
	 * @param changedDevice
	 * @throws ConnectionException
	 */
//	public void updateDeviceProperty(String identifier, Field property, Value value) {
//
//		for (DeviceStatus ds : this.devicesStatus) {
//			Device device = ds.kuraDevice;
//			if (device.getIdentifier().equalsIgnoreCase(identifier)) {
//				LOGGER.debug("device " + identifier + " property is being updated");
//				for (Field prop : device.getProperties()) {
//					if (prop.getName().equalsIgnoreCase(property.getName())) {
//						device.setValue(property.getName(), value.asString().toString());
//					}
//				}
//			}
//			ds.kuraDevice = device;
//			ds.chDevice = this.createCHARIOTDevice(device);
//			ds.chDevice.setConnectionHandler(this.reConn);
//			this.updateDeviceProperty(ds.chDevice, property.getName(), value.asString().toString() );
//		}
	//}
	
	/**
	 * Update device property
	 * @param identifier
	 * @param property
	 * @param value
	 */
//	public void updateDeviceAttribute(String identifier, Field property, Value value) {
//		
//		LOGGER.info("Device updateDeviceAttribute is updated",identifier);
//		for (DeviceStatus ds : this.devicesStatus) {
//			Device device = ds.kuraDevice;
////			if (device.getIdentifier().equalsIgnoreCase(identifier)) {
////				LOGGER.debug("device " + identifier + " property is being updated");
////				for (Field prop : device.getProperties()) {
////					if (prop.getName().equalsIgnoreCase(property.getName())) {
////						device.setValue(property.getName(), value.asString().toString());
////					}
////				}
////			}
//			ds.kuraDevice = device;
//
//			ds.chDevice = this.createCHARIOTDevice(device);
//			ds.chDevice.setConnectionHandler(this.reConn);
//			
//			this.updateDeviceProperty(ds.chDevice, property.getName(), value.asString().toString() );
//		}
//	}
	/**
	 * Update device name
	 * @param identifier
	 * @param device
	 */
	public void updateDeviceName(String identifier, Device device) {
		LOGGER.info("Device name is updated",identifier);
//		for (DeviceStatus ds : this.devicesStatus) {
//			if (ds.kuraDevice.getIdentifier().equalsIgnoreCase(identifier)) {
//				LOGGER.debug("device " + identifier + " property is being updated");
//				ds.kuraDevice.setName(device.getName());
//				ds.chDevice.setName(device.getName());
//				
//			}
//			this.updateDeviceDataInCHARIOTMiddleware(ds.chDevice);
//		}
		
	}

	/**
	 * Update device location
	 * @param device
	 */
	public void updateDeviceLocation(Device device) {
//		
//		LOGGER.info("Device Location is updated",device.getIdentifier());
//		
//		LOGGER.info("Device name is updated",device.getIdentifier());
//		for (DeviceStatus ds : this.devicesStatus) {
//			if (ds.kuraDevice.getIdentifier().equalsIgnoreCase(device.getIdentifier())) {
//				LOGGER.debug("device " + device.getIdentifier() + " property is being updated");
//				ds.kuraDevice.setAttribute("placeId",device.getPlaceId().get());
//				ds.chDevice.setDeviceLocation(new Location());
//				this.updateDeviceDataInCHARIOTMiddleware(ds.chDevice);
//				return;
//			}
//			
//		}
	}
	
	/**
	 * Update device representation in the CHARIOT Middleware
	 * @param device
	 */
	private void updateDeviceDataInCHARIOTMiddleware(de.gtarc.chariot.deviceapi.Device device) {
		// convert the chdevice to payload device
		PayloadDeviceProperty[] properties =  new PayloadDeviceProperty[device.getMandatoryProperties().size()];
		int counter = 0;
		for(Property prop : device.getMandatoryProperties()) {
			DeviceProperty dprop = (DeviceProperty)prop;
			properties[counter] = new PayloadDeviceProperty("", dprop.getKey(),dprop.getType(), 
					dprop.getValue().toString(), dprop.getUnit(), dprop.getWritable(),dprop.getMin(), dprop.getMax()); 
			counter++;
		}
		// TODO: location object is not recognized by payload, check
		AbstractPayload payload = new PayloadDevice(device.getType(),device.getUUIdentifier().toString(),device.getName(),device.getFriendlyName(),"groupId","securitykey",reId.toString(),"ip",device.getVendor(),
				null,// new Location("id","type","name",1, new Position("lat","lng"), new Indoorposition("12","lat","lng")) 
				properties);
		this.sendMessage(payload, topic, device.getUUIdentifier().toString());
	}
	
	/**
	 * Update device property representation in the CHARIOT Middleware
	 * @param device
	 * @param propertyName
	 * @param propertyValue
	 */
	public void updateDeviceProperty(de.gtarc.chariot.deviceapi.Device device, String propertyName, String propertyValue) {

		PayloadDeviceProperty deviceProperty = null;
		for (Property prop : device.getMandatoryProperties()) {
			DeviceProperty dprop = (DeviceProperty) prop;
			if (prop.getKey().equalsIgnoreCase(propertyName)) {
				deviceProperty = new PayloadDeviceProperty("", dprop.getKey(), dprop.getType(), propertyValue,
						dprop.getUnit(), dprop.getWritable(), dprop.getMin(), dprop.getMax());
			}
		}

		AbstractPayload payload = new PayloadDeviceProperty(deviceProperty.getTimestamp(), deviceProperty.getKey(),
				deviceProperty.getValue().toString(), deviceProperty.getType(), deviceProperty.getUnit(),
				deviceProperty.getWritable());
		this.sendMessage(payload, this.topic, device.getUUIdentifier().toString());
	}
	/**
	 * Transmit the created payload message to the device itself
	 * @param payload
	 * @param reTopic
	 * @param deviceId
	 */
	public void sendMessage(AbstractPayload payload, String reTopic, String deviceId) {
		AbstractMessage message = new MessageBuilder().setTopic(deviceTopic + deviceId) // find here the device topic
				.setResponseTopic(reTopic).setTo(deviceId).setFrom(reId.toString())
				.setMessageType(payload.getClass().getName()).addPayload(payload).build();

		try {
			reConn.send(message);
		} catch (ConnectionException e) {
			LOGGER.error(e.getMessage());
		}
	}

	/**
	 * Convert IOLITE device to CHARIOT Device
	 * @param ioliteDevice
	 * @return
	 */
	private de.gtarc.chariot.deviceapi.Device createCHARIOTDevice(Device kuraDevice) {

		de.gtarc.chariot.deviceapi.Device chDevice = new DeviceBuilder().buildSensingDevice();
		chDevice.setUUIdentifier(UUID.fromString("a1f114f5-f47f-449d-b579-6605bba061c8"));
		// get properties and
//		chDevice.setVendor(ioliteDevice.getManufacturer().get());
//		chDevice.setFriendlyName(ioliteDevice.getName());
//		chDevice.setName(ioliteDevice.getIdentifier());
//		// chDevice.setDeviceStatus(ioliteDevice.getStatus().asString());
//		boolean isControllable = false;
//		for (Field field : ioliteDevice.getProperties()) {
//			DevicePropertyImpl prop = new DevicePropertyImpl();
//			prop.setKey(field.getName());
//			// TODO prop.setType( field.getType());
//			prop.setUnit("");
//			prop.setWritable(field.isWritable() + "");
//			if (field.isWritable()) {
//				isControllable = true;
//			}
//
//			Optional<String> val = field.asString();
//			if (val.isPresent()) {
//				prop.setValue(field.asString().get());
//			} else {
//				prop.setValue("");
//			}
//			if (field.decrement()) {
//				prop.setMin("");
//			}
//			if (field.increment()) {
//				prop.setMax("");
//			}
//			chDevice.addMandatoryProperties(prop);
//		}

//		if (isControllable) {
//			chDevice.setType("Actuator");
//		}
		chDevice.setType("Sensor");

		return chDevice;
	}

	/**
	 * Update device data in IOLITE device 
	 * @param chDevice
	 */
	public void updateDeviceData(de.gtarc.chariot.deviceapi.Device chDevice) {

		boolean doesExist = false;
		for (DeviceStatus ds : devicesStatus) {
			if (ds.deviceID.toString().equalsIgnoreCase(chDevice.getUUIdentifier().toString())) {
				doesExist = true;
				List<Property> deviceProperties = chDevice.getMandatoryProperties();
				for (Property prop : deviceProperties) {
				
			// ds.kuraDevice.setValue(((DeviceProperty) prop).getKey(), ((DeviceProperty) prop).getValue().toString());
				}
				ds.chDevice = chDevice;
			}
		}
		if (!doesExist) {
			LOGGER.info("Device with UUID:" + chDevice.getUUIdentifier() + " does not exist!");
		}
	}


	/**
	 * Remove device from the agent world and in the proxy application
	 *
	 * @param device
	 * @throws ConnectionException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public boolean unregisterDevice(de.gtarc.chariot.deviceapi.Device device)
			throws ConnectionException, InterruptedException, ExecutionException {
		RegistrationClient registrationHandler = new RegistrationClient();
		AbstractMessage result = null;

		int i = 0;
		boolean succeded = false;

		while (i < 5) {
			result = registrationHandler.removeDevice(device, device.getUUIdentifier().toString(), "").get().result;
			// TODO: Message Handling is not completed
			if (result.getPayload().toString().contains("SUCCEDED")) {
				i = 5;
				succeded = true;
			}
			Thread.sleep(1000);
		}
		if (!succeded) {

			LOGGER.error("Device with ID:" + device.getUUIdentifier() + " could not be removed!");
		}
		return succeded;
	}

	/**
	 * Keep alive device the running device
	 *
	 * @param device
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void keepAliveDevice(de.gtarc.chariot.deviceapi.Device device)throws InterruptedException, ExecutionException {
		device.setConnectionHandler(reConn);
		RegistrationClient registrationClient = new RegistrationClient();
		AbstractMessage newResult = registrationClient.keepAlive(device, null, "", "", "").get().result;
		System.out.println("KEEPALIVE registrar agent-" + ((PayloadResponse) newResult.getPayload()).response);
		if (!((PayloadResponse) newResult.getPayload()).response.equalsIgnoreCase("Success")) {
			LOGGER.error("Keep alive is failed:" + ((PayloadResponse) newResult.getPayload()).response);
		}
	}

	/**
	 * Deactivate running device
	 *
	 * @param device
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void deactivateDevice(de.gtarc.chariot.deviceapi.Device device)
			throws InterruptedException, ExecutionException {
		device.setConnectionHandler(reConn);
		RegistrationClient registrationClient = new RegistrationClient();
		AbstractMessage newResult = registrationClient.deactivateDevice(device, null, "").get().result;
		System.out.println("DEACTIVATION registrar agent-" + ((PayloadResponse) newResult.getPayload()).response);
		if (!((PayloadResponse) newResult.getPayload()).response.equalsIgnoreCase("Success")) {
			LOGGER.error("device deactivation is failed:" + ((PayloadResponse) newResult.getPayload()).response);
		}
	}

	/**
	 *Update device 
	 *
	 * @param device
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	public void updateDevice(de.gtarc.chariot.deviceapi.Device device)
			throws InterruptedException, ExecutionException, TimeoutException {
		
		device.setConnectionHandler(reConn);
		RegistrationClient registrationClient = new RegistrationClient();
		AbstractMessage newResult = registrationClient.updateDevice(device, null, "").get(5, TimeUnit.SECONDS).result;
		System.out.println("UPDATE DEVICE registrar agent-" + ((PayloadResponse) newResult.getPayload()).response);
		if (((PayloadResponse) newResult.getPayload()).response.equalsIgnoreCase("Success")) {
			LOGGER.info("Device data model is updated.");
		}
	}

	public DeviceManager console(String id, String obj) {
		System.out.println(id + ":" + obj);
		return this;
	}

	@Override
	public void connectionStateChanged(Connection arg0, ConnectionStatus arg1) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * All messages sent by CHARIOT Middleware are received in this function and handled.
	 */
	@Override
	
	public void onMessageReceived(IMessage iMessage, Connection arg1) throws ConnectionException {
		AbstractMessage result = null;
		result = (AbstractMessage) iMessage;
		System.out.println("client message includes this topic: " + result.getTopic());
		if (result.getPayload() instanceof PayloadResponse) { // from registration
			System.out.println("client message result received, message: " + ((PayloadResponse) result.getPayload()).response);
			if (((PayloadResponse) result.getPayload()).response.equals("DEVICE-UPDATE")) {}
		} else if (result.getPayload() instanceof PayloadDevice) { // from its agent
			String message = result.getPayload().getJsonString(result.getPayload().getClass());

			PayloadDevice device = ((PayloadDevice) result.getPayload());
			this.updateDeviceData(this.convertPayloadDeviceToDevice(result.getFrom(),device));
			System.out.println("payload device:" + message);

		} else if (result.getPayload() instanceof PayloadDeviceProperty) { // from its agent
			String message = result.getPayload().getJsonString(result.getPayload().getClass());
			PayloadDeviceProperty property = ((PayloadDeviceProperty) result.getPayload());
			this.updateDeviceProperty(result.getFrom(),property);
			System.out.println("payload device property:" + message);
		} else {
			LOGGER.info("result instance cannot be detected!");
		}
		
	}
	/**
	 * 
	 * @param identifier
	 * @param prop
	 */
	private void updateDeviceProperty(String identifier, PayloadDeviceProperty prop) {
		for (DeviceStatus ds : this.devicesStatus) {
			if(ds.chDevice.getUUIdentifier().toString().equalsIgnoreCase(identifier)) {
				for(Property dprop:ds.chDevice.getMandatoryProperties()) {
					if (dprop.getKey().equalsIgnoreCase(prop.getKey())) {
						ds.chDevice.getMandatoryProperties().remove(dprop);
						// update chariot  device property
						// TODO: some devices do not have max and min
						ds.chDevice.addMandatoryProperties(new DevicePropertyImpl(Long.parseLong(prop.getTimestamp()),prop.getKey(),prop.getType(), prop.getValue().toString(), prop.getUnit(), prop.getWritable().toString(), prop.getMin(), prop.getMax()));
						
						
						//TODO: update KURA  device property
						//ds.kuraDevice.setValue(((DeviceProperty) prop).getKey(), ((DeviceProperty) prop).getValue().toString());
						
						return;
					}
				}
			}
		}	
	}

	


	

}
