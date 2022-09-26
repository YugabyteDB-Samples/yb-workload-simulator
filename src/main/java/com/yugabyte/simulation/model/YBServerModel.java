package com.yugabyte.simulation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YBServerModel {
    private String host;
    private String port;
    private String cloud;
    private String region;
    private String zone;
    private String inet_server;
    
    private boolean nodeUp = true;
    private boolean master = false;
    private boolean tserver = false;
    private boolean readReplica = false;

    public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getCloud() {
		return cloud;
	}
	public void setCloud(String cloud) {
		this.cloud = cloud;
	}
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
	public String getZone() {
		return zone;
	}
	public void setZone(String zone) {
		this.zone = zone;
	}
	public String getInet_server() {
		return inet_server;
	}
	public void setInet_server(String inet_server) {
		this.inet_server = inet_server;
	}
	public boolean isNodeUp() {
		return nodeUp;
	}
	public void setNodeUp(boolean nodeUp) {
		this.nodeUp = nodeUp;
	}
	public boolean isMaster() {
		return master;
	}
	public void setMaster(boolean master) {
		this.master = master;
	}
	public boolean isTserver() {
		return tserver;
	}
	public void setTserver(boolean tserver) {
		this.tserver = tserver;
	}
	public boolean isReadReplica() {
		return readReplica;
	}
	public void setReadReplica(boolean readReplica) {
		this.readReplica = readReplica;
	}
}
