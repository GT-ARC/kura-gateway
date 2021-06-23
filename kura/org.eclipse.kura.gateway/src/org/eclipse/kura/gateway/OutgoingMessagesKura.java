package org.eclipse.kura.gateway;

import org.json.JSONArray;
import org.json.JSONObject;

public class OutgoingMessagesKura {
	
	
	public JSONArray removeDevice(int index, JSONArray devicelist) {
		CoapClientKura coapClient = new CoapClientKura();
		JSONObject removal= new JSONObject("{\"Type\":\"Removal\",\"Content\":{\"ObjectType\":\"Sensor\",\"ID\":\"\",\"UUID\":\"\",\"wlanIPv4\":\"\",\"lowpanIPv6\":\"\"}}");
		//get the ID of the device to be removed
		String ID = devicelist.getJSONObject(index).getString("ID");
		String UUID = devicelist.getJSONObject(index).getString("UUID");
		String wlanIPv4 = devicelist.getJSONObject(index).getString("wlanIPv4");
		String lowpanIPv6 = devicelist.getJSONObject(index).getString("lowpanIPv6");
		//put ID of the device to the removal message content
		removal.getJSONObject("Content").put("ID", ID);
		removal.getJSONObject("Content").put("UUID", UUID);
		removal.getJSONObject("Content").put("wlanIPv4", wlanIPv4);
		removal.getJSONObject("Content").put("lowpanIPv6", lowpanIPv6);
		//remove the registration of the device from /api/status
		devicelist.remove(index);
		//pass removal JSONObject to the CoAP
		coapClient.coapClientKura(removal);
		return devicelist;
	}
	//deactivate the device
	public void deactivateDevice(int index, JSONArray devicelist) {
		CoapClientKura coapClient = new CoapClientKura();
		JSONObject deact= new JSONObject("{\"Type\":\"Deactivation\",\"Content\":{\"ObjectType\":\"Sensor\",\"ID\":\"\",\"UUID\":\"\",\"wlanIPv4\":\"\",\"lowpanIPv6\":\"\",\"DeactivationTime\":\"\"}}");
		//get the ID of the device to be deactivated
		String ID = devicelist.getJSONObject(index).getString("ID");
		String UUID = devicelist.getJSONObject(index).getString("UUID");
		String wlanIPv4 = devicelist.getJSONObject(index).getString("wlanIPv4");
		String lowpanIPv6 = devicelist.getJSONObject(index).getString("lowpanIPv6");
		//put ID of the device to the deactivation message content
		deact.getJSONObject("Content").put("ID", ID);
		deact.getJSONObject("Content").put("UUID", UUID);
		deact.getJSONObject("Content").put("wlanIPv4", wlanIPv4);
		deact.getJSONObject("Content").put("lowpanIPv6", lowpanIPv6);
		//pass deact JSONObject to the CoAP
		//coapClient(deact);
		coapClient.coapClientKura(deact);
		return;
		
	}
	
	//send a keepAlive message to node with specified ID
	public void sendKeepAliveMessage(int index, JSONArray devicelist) {
		CoapClientKura coapClient = new CoapClientKura();
		JSONObject keepAlive= new JSONObject("{\"Type\":\"KeepAlive\",\"Content\":{\"ID\":\"2167419\",\"UUID\":\"2167419\",\"AliveDate\":\"\",\"Output\":\"24.375\",\"Unit\":\"Celcius\",\"wlanIPv4\":\"\",\"lowpanIPv6\":\"\"}}");
		//get the ID of the device to be kept alive
		String ID = devicelist.getJSONObject(index).getString("ID");
		String UUID = devicelist.getJSONObject(index).getString("UUID");
		String wlanIPv4 = devicelist.getJSONObject(index).getString("wlanIPv4");
		String lowpanIPv6 = devicelist.getJSONObject(index).getString("lowpanIPv6");
		//put ID of the device to the keepAlive message content
		keepAlive.getJSONObject("Content").put("ID", ID);
		keepAlive.getJSONObject("Content").put("UUID", UUID);
		keepAlive.getJSONObject("Content").put("wlanIPv4", wlanIPv4);
		keepAlive.getJSONObject("Content").put("lowpanIPv6", lowpanIPv6);
		//pass keepAlive JSONObject to the CoAP
		coapClient.coapClientKura(keepAlive);
		return;
		
		
	}
}
