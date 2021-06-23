package org.eclipse.kura.gateway;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Objects;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
@SuppressWarnings("serial")
public final class StatusServlet extends HttpServlet {
	private static final Logger s_logger = LoggerFactory.getLogger(StatusServlet.class);
	private static int index = 0;
	
	
	static JSONArray devicelist = new JSONArray();
// 	http://stackoverflow.com/questions/2010990/how-do-you-return-a-json-object-from-a-java-servlet
	public StatusServlet() {
		index++;
		s_logger.info("status servlet index number is " + index);
		
	}
	
	public void jsonHandle(JSONObject raw) {
		IncomingMessagesKura incomingMessagesKura = new IncomingMessagesKura();
		String type="", ID="", UUID="";
		try {
			type = raw.getString("Type");
			ID = raw.getJSONObject("Content").getString("ID");
			UUID = raw.getJSONObject("Content").getString("UUID");
		} catch(JSONException exp) {
			s_logger.error(exp.getMessage());
		}
		
		if(!type.equalsIgnoreCase("") && !ID.equalsIgnoreCase("") && !UUID.equalsIgnoreCase("")) {
			boolean registrationCheck = isDeviceRegistered(ID, UUID);
			//Message Type: first Registration
			if(!registrationCheck) {
				if(type.equalsIgnoreCase("Registration")) {
					devicelist = incomingMessagesKura.regDevice(raw.getJSONObject("Content"), devicelist);
					s_logger.info("Registration of the new device is done.");
				}	
			} else {
				// new registration
				if(type.equalsIgnoreCase("Registration")) {
					devicelist = incomingMessagesKura.updateDeviceRegistration(raw.getJSONObject("Content"), devicelist);
					s_logger.info("Registration of the new device is done.");
				}	
				//Message Type: Output
				if(type.equalsIgnoreCase("Output")) {
					//pass "Content" object of the JSON to related function
					devicelist = incomingMessagesKura.outputDevice(raw.getJSONObject("Content"), devicelist);
					s_logger.info("New output is arrived.");
				}
				//Message Type: KeepAlive
				else if(type.equalsIgnoreCase("KeepAlive")) {
				    //pass "Content" object of the JSON to related function
					devicelist = incomingMessagesKura.keepDeviceAlive(raw.getJSONObject("Content"), devicelist);
					s_logger.info("keepAlive message is arrived.");
				}
				//Message Type: Update
				else if(type.equalsIgnoreCase("Update") ) {
					//pass "Content" object of the JSON to related function
					devicelist = incomingMessagesKura.updateDeviceData(raw.getJSONObject("Content"), devicelist);
					s_logger.info("Update message is arrived.");
				}
				//Message Type: Deactivation
				else if(type.equalsIgnoreCase("Deactivation")) {
					//pass "Content" object of the JSON to related function
					devicelist = incomingMessagesKura.deactivateDevice(raw.getJSONObject("Content"), devicelist);
					s_logger.info("Deactivation message is arrived.");
				}
				if(Objects.equals(type, "Removal")) {
					s_logger.info("Device with ID " + raw.getJSONObject("Content").getString("UUID") + " answered to removal message and is no longer registered to system.");
					for (int i = 0; i < devicelist.length(); ++i) {
						if(Objects.equals(devicelist.getJSONObject(i).getString("UUID"), UUID)) {
							devicelist.remove(i);
						}
					}
				}
				else {
					s_logger.info("The given topic is unknown! ->"+ type);
				}
			}
		}else {
			s_logger.info("JSON Object doesn't include all following parameters:ID, UUID, Type");
		}
		
		return;
	}
	
	// Check if incoming ID is registered
	public boolean isDeviceRegistered(String ID, String UUID) {
		for (int i = 0; i < devicelist.length(); ++i) {
			if(devicelist.getJSONObject(i).getString("UUID").equalsIgnoreCase(UUID) ) {
				s_logger.info("Device with "+UUID +" is available!");
				return true;
			}
		}
		return false;
	}
	//Check if incoming message has type : Registration
	public boolean typeCheck(String type) {
		boolean regMsg = false;
		if(type.equalsIgnoreCase("Registration")){
    		regMsg = true;
		}
		return regMsg;
	}
	

	public final void doGet(HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("application/json");
        //s_logger.info("req.getRequestURI(): {}", request.getRequestURI());
        //s_logger.info("req.getRequestURL(): {}", request.getRequestURL());
    	s_logger.info("action:"+request.getParameter("action") +" dID:"+request.getParameter("deviceID"));
    	
    	String getParameter = request.getParameter("action");
    	OutgoingMessagesKura outgoingMessagesKura = new OutgoingMessagesKura(); 
    	//action type : removal
        if(getParameter.equalsIgnoreCase("Removal")) {
        	for (int index = 0; index < devicelist.length(); ++index) {
    		    if(devicelist.getJSONObject(index).getString("UUID").equalsIgnoreCase(request.getParameter("deviceID"))){
    		    	s_logger.info("Removal request from KURAwebUI is detected.");
    		    	outgoingMessagesKura.removeDevice(index,devicelist);
    		    }
    		}
        }
        //action type : KeepAlive
        else if(getParameter.equalsIgnoreCase("KeepAlive") ) {
        	for (int index = 0; index < devicelist.length(); ++index) {
    		    if(devicelist.getJSONObject(index).getString("UUID").equalsIgnoreCase(request.getParameter("deviceID"))){
    		    	s_logger.info("KeepAlive request from KURAwebUI is detected.");
    		    	outgoingMessagesKura.sendKeepAliveMessage(index,devicelist);
    		    }
    		}
        }
        //action type : Deactivation
        else if(getParameter.equalsIgnoreCase("Deactivation")) {
        	for (int index = 0; index < devicelist.length(); ++index) {
    		    if(devicelist.getJSONObject(index).getString("UUID").equalsIgnoreCase(request.getParameter("deviceID"))){
    		    	s_logger.info("Deactivation request from KURAwebUI is detected.");
    		    	outgoingMessagesKura.deactivateDevice(index,devicelist);
    		    }
    		}
        }
        
		PrintWriter out = null;
		HashMap<String,Object> map = new HashMap<>();
		/*
		map.put("Helloooooo", "World");
		map.put("EnjoyLevel", r.nextInt(50));
		*/
		try {
			out = response.getWriter();
		} catch (IOException e) {
			s_logger.error("Servlet Error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		// Assuming your json object is **jsonObject**, perform the following, it will return your json object
		
		//JSONObject d2 = new JSONObject("{\"Type\":\"Registration\",\"Content\":{\"ObjectType\":\"Sensor\",\"GeneralDescription\":{\"ID\":1234567, \"Model\":\"DHT11\",\"Manufacturer\":\"Adafruit\",\"CanMeasure\":{\"Temp\":\"Celcius\",\"Hum\":\"Percent\"},\"DeploymentDate\":\"21.03.2018\"},\"Location\":{\"Latitude\":52.5155,\"Longitude\":13.4062},\"Connectivity\":{\"ConnectedDevice\":\"RaspberryPi3\",\"OS\":\"Raspbian\",\"Distro\":\"Debian8\",\"DeviceIP\":\"\",\"Url\":\"\"},\"Specifications\":{\"General\":{\"VoltageInput\":\"3to5V\",\"OutputType\":\"Digital\",\"MaxCurrent\":\"2.5mA\",\"PINs\":[\"Gnd\",\"Vcc\",\"Data\"],\"MaxFreq\":\"1Hz\"},\"Limitations\":{\"Temp\":{\"Min\":0,\"Max\":50,\"Acc\":\"pm2\"},\"Hum\":{\"Min\":20,\"Max\":90,\"Acc\":\"%5\"}}}}}");
		//JSONArray ja = new JSONArray();
		//ja.put(d2);
		
		s_logger.debug("device list size:" +devicelist.length());
		out.print(devicelist.toString());
		out.flush();
	}
}