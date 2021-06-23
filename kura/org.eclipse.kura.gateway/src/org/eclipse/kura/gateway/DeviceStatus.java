package org.eclipse.kura.gateway;

import java.util.UUID;

public class DeviceStatus {
	Device kuraDevice = null;
	UUID deviceID = null;
	de.gtarc.chariot.deviceapi.Device  chDevice = null;
	String topic = null;
	DeviceState state = DeviceState.UNREGISTERED;
	public DeviceStatus(Device iod,de.gtarc.chariot.deviceapi.Device chd, String topic, DeviceState state) {
		this.kuraDevice = iod;
		this.chDevice = chd;
		this.state = state;
		this.topic = topic;
		this.deviceID = chd.getUUIdentifier();
	}
}
