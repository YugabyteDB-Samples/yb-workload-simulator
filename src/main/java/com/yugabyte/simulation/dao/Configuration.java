package com.yugabyte.simulation.dao;

public class Configuration {
	private String managementType;
	private String accessKey;
	private String accountId;
	private String projectId;
	private String clusterId;
	private LoginInformation login;
	
	public Configuration() {
	}
	
	public Configuration(String managementType, String accessKey, String accountId, String projectId,
			String clusterId, LoginInformation login) {
		super();
		this.managementType = managementType;
		this.accessKey = accessKey;
		this.accountId = accountId;
		this.projectId = projectId;
		this.clusterId = clusterId;
		this.login = login;
	}

	public String getManagementType() {
		return managementType;
	}

	public void setManagementType(String managementType) {
		this.managementType = managementType;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getClusterId() {
		return clusterId;
	}

	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}
	
	public LoginInformation getLogin() {
		return login;
	}
	
	public void setLogin(LoginInformation login) {
		this.login = login;
	}
	@Override
	public Configuration clone() {
		return new Configuration(managementType, accessKey, accountId, projectId, clusterId, login.clone());
	}
	
}
