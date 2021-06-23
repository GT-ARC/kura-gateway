package org.eclipse.kura.gateway;

import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingMessagesKura {
	private static final Logger s_logger = LoggerFactory.getLogger(StatusServlet.class);
	CoapClientKura coapClient = null;
	public JSONArray regDevice(JSONObject rawContent, JSONArray devicelist) {
		try {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("Model", rawContent.getJSONObject("GeneralDescription").getString("Model"));
			jsonObj.put("DeploymentDate", rawContent.getString("DeploymentDate"));
			jsonObj.put("CanMeasure", rawContent.getJSONObject("GeneralDescription").getJSONObject("CanMeasure"));
			jsonObj.put("Unit", rawContent.getJSONObject("GeneralDescription").getJSONObject("CanMeasure"));
			jsonObj.put("Location", rawContent.getJSONObject("Location"));
			jsonObj.put("ConnectedDevice",rawContent.getJSONObject("ConnectedDevice"));
			jsonObj.put("ID",rawContent.getString("ID"));
			jsonObj.put("UUID",rawContent.getString("UUID"));
			jsonObj.put("wlanIPv4",rawContent.getJSONObject("Connection").getString("wlanIPv4"));
			jsonObj.put("wlanIPv6",rawContent.getJSONObject("Connection").getString("wlanIPv6"));
			jsonObj.put("lowpanIPv6",rawContent.getJSONObject("Connection").getString("lowpanIPv6"));
			jsonObj.put("Status","Active");
			//Put jsonObj into 123765
			devicelist.put(jsonObj);
			s_logger.info("An updated JSON: " + devicelist.toString());
		} catch(JSONException exp) {
			s_logger.error(exp.getMessage());
		}
		return devicelist;			
	}
	public JSONArray updateDeviceRegistration(JSONObject rawContent, JSONArray devicelist) {
		String uuid = rawContent.getString("UUID");
		for (int i = 0; i < devicelist.length(); ++i) {
			if(devicelist.getJSONObject(i).getString("UUID").equalsIgnoreCase(uuid) ) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("Model", rawContent.getJSONObject("GeneralDescription").getString("Model"));
				jsonObj.put("DeploymentDate", rawContent.getString("DeploymentDate"));
				jsonObj.put("CanMeasure", rawContent.getJSONObject("GeneralDescription").getJSONObject("CanMeasure"));
				jsonObj.put("Unit", rawContent.getJSONObject("GeneralDescription").getJSONObject("CanMeasure"));
				jsonObj.put("Location", rawContent.getJSONObject("Location"));
				jsonObj.put("ConnectedDevice",rawContent.getJSONObject("ConnectedDevice"));
				jsonObj.put("ID",rawContent.getString("ID"));
				jsonObj.put("UUID",rawContent.getString("UUID"));
				jsonObj.put("wlanIPv4",rawContent.getJSONObject("Connection").getString("wlanIPv4"));
				jsonObj.put("wlanIPv6",rawContent.getJSONObject("Connection").getString("wlanIPv6"));
				jsonObj.put("lowpanIPv6",rawContent.getJSONObject("Connection").getString("lowpanIPv6"));
				jsonObj.put("Status","Active");
				devicelist.remove(i); // remote the same device
				devicelist.put(jsonObj);
				s_logger.info("Device with id "+uuid +"  registration is updated!");
				return devicelist;
			}
		}
		return devicelist;
	}
	
	//Update keepAlive devices
	/*
	 * {
		  "Type": "KeepAlive",
		  "Content": {
		    "ObjectType": "Sensor",
		    "UUID": "3dbe27d1",
		    "ID": "0001",
		    "Output": {
		      "Temperature": 22.0,
		      "Humidity": 14.0
		    },
		    "Unit": {
		      "Temperature": "Celcius",
		      "Humidity": "Percent"
		    },
		    "AliveTime": "2019-02-12 17:31:54"
		  }
		}

	 */
	public JSONArray keepDeviceAlive(JSONObject rawContent, JSONArray devicelist) {
		coapClient = new CoapClientKura();
		for (int i = 0; i < devicelist.length(); ++i) {
			try {
				if(devicelist.getJSONObject(i).getString("UUID").equalsIgnoreCase(rawContent.getString("UUID"))){
			    	devicelist.getJSONObject(i).put("AliveTime", rawContent.getString("AliveTime"));
			    	devicelist.getJSONObject(i).put("Output", rawContent.getJSONObject("Output"));
			    	devicelist.getJSONObject(i).put("Status","Active");
			    	devicelist.getJSONObject(i).remove("DeactivationTime");
			    	coapClient.coapClientSendAgent(devicelist.getJSONObject(i));
			    }
			} catch(JSONException exp) {
				s_logger.error(exp.getMessage());
			}
		}
		s_logger.info("A JSON object is updated with a KeepAlive message. The updated JSON is: " + devicelist.toString());
		return devicelist;
	}
	/*
	 * {
		  "Type": "Removal",
		  "Content": {
		    "ObjectType": "Sensor",
		    "UUID": "3dbe27d1",
		    "ID": "0001",
		    "RemovalTime": "2019-02-12 17:31:54"
		  }
		}
		{
		  "Type":"Deactivation",
		  "Content": {
		    "ObjectType": "Sensor",
		    "UUID": "3dbe27d1",
		    "ID": "0001",
		    "DeactivationTime": "2019-02-12 17:31:54"
		  }
		}


	 */
	//Update the Status of the Device as deactive
	public JSONArray deactivateDevice(JSONObject rawContent, JSONArray devicelist) {
		for (int i = 0; i < devicelist.length(); ++i) {
			try {
			    if( devicelist.getJSONObject(i).getString("UUID").equalsIgnoreCase(rawContent.getString("UUID"))){
			    	devicelist.getJSONObject(i).put("Status","Deactive");
			    	devicelist.getJSONObject(i).put("DeactivationTime", rawContent.getString("DeactivationTime"));
			    	devicelist.getJSONObject(i).remove("AliveDate");
			    }
			} catch(JSONException exp) {
				s_logger.error(exp.getMessage());
			}
		}
		s_logger.info("A JSON object is updated with a Deactivation message. The updated JSON is: " + devicelist.toString());
		return devicelist;
	}
	/*
	 * {
		  "Type":"Update",
		  "Content": {
		    "ObjectType": "Sensor",
		    "UUID": "3dbe27d1",
		    "ID": "0001",
		    "UpdatedContent": {
		      "Location": {
		        "Latitude": 52.1111,
		        "Longitude": 13.1111
		      },
		      "Connection": {
		        "lowpanIPv6": "fe80::8888:1%lowpan0",
		        "wlanIPv4": "172.24.1.138",
		        "wlanIPv6": "fe80::ba27:ebff:fe65:6064%wlan0"
		      }
		    }
		  }
		}
		*/
	//Update the properties of the registered devices
	public JSONArray updateDeviceData(JSONObject rawContent, JSONArray devicelist) {
		String uuid = rawContent.getString("UUID");
		for (int i = 0; i < devicelist.length(); ++i) {
			if(devicelist.getJSONObject(i).getString("UUID").equalsIgnoreCase(uuid) ) {
				JSONObject device = devicelist.getJSONObject(i);
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("Model", device.getString("Model"));
				jsonObj.put("DeploymentDate", device.getString("DeploymentDate"));
				jsonObj.put("CanMeasure", device.getJSONObject("CanMeasure"));
				jsonObj.put("Unit", device.getJSONObject("CanMeasure"));
				jsonObj.put("Location", rawContent.getJSONObject("UpdatedContent").getJSONObject("Location"));
				jsonObj.put("ConnectedDevice",device.getJSONObject("ConnectedDevice"));
				jsonObj.put("ID",rawContent.getString("ID"));
				jsonObj.put("UUID",rawContent.getString("UUID"));
				jsonObj.put("wlanIPv4",rawContent.getJSONObject("Connection").getString("wlanIPv4"));
				jsonObj.put("wlanIPv6",rawContent.getJSONObject("Connection").getString("wlanIPv6"));
				jsonObj.put("lowpanIPv6",rawContent.getJSONObject("Connection").getString("lowpanIPv6"));
				jsonObj.put("Status","Active");
				devicelist.remove(i); // remote the same device
				devicelist.put(jsonObj);
				s_logger.info("Device with id "+uuid +"  registration is updated!");
				return devicelist;
			}
		}
		return devicelist;
	}

	/*
	 * 
		{
		  "Type": "Output",
		  "Content": {
		    "ObjectType": "Sensor",
		    "UUID": "3dbe27d1",
		    "ID": "0001",
		    "Output": {
		      "Temperature": 22.0,
		      "Humidity": 14.0
		    },
		    "Unit": {
		      "Temperature": "Celcius",
		      "Humidity": "Percent"
		    },
		    "Time": "2019-02-12 17:31:54"
		  }
		}

	 */
	//Update the output value
	public JSONArray outputDevice(JSONObject rawContent, JSONArray devicelist) {
		int a=0;
		coapClient = new CoapClientKura();
		for (int i = 0; i < devicelist.length(); ++i) {
			try{
				if(devicelist.getJSONObject(i).getString("UUID").equalsIgnoreCase(rawContent.getString("UUID"))){
			    	devicelist.getJSONObject(i).put("Output", rawContent.getJSONObject("Output"));
			    	s_logger.info("Trying to pass the data to the Agent.");
			    	coapClient.coapClientSendAgent(devicelist.getJSONObject(i));
			    	a=i;
			    }
			} catch(Exception exp) {
				s_logger.error(exp.getMessage());
			}
		}
		s_logger.info("A JSON object is updated with an Output message. The updated JSON is: " + devicelist.getJSONObject(a).toString());
		return devicelist;
	}

}