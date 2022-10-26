package com.yugabyte.simulation.controller;

import com.yugabyte.simulation.model.YBServerModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.yugabyte.simulation.dao.BooleanInvocationResult;
import com.yugabyte.simulation.dao.Configuration;
import com.yugabyte.simulation.dao.InvocationResult;
import com.yugabyte.simulation.exception.InvalidPasswordException;
import com.yugabyte.simulation.services.ConfigurationService;

import java.util.List;

@RestController
@CrossOrigin(origins="*")
@RequestMapping("/user")
public class UserController {
	// The configuration service is set up take an email, but we're not using
	// this at the moment, so just use a default string.
	private static final String VALIDATION_STRING = "DEFAULT_USER";
	
	@Autowired
	private ConfigurationService configService;
	
	@Autowired
	private YBMCloudApiController ybmController;
	
	@RequestMapping(method = RequestMethod.GET, value = "/test") 
	public void test() {
		this.setInitialPassword("passowrd");
		this.validatePassword("passowrd");
		this.isExistingUser();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/login")
	@ResponseBody
	public InvocationResult login(String email, String password) {
		if (configService.validatePassword(password)) {
			return new InvocationResult("Ok");
		}
		else  {
			return new InvocationResult(new InvalidPasswordException());
		}
	}

	@RequestMapping(value = "/existingUser")
	public BooleanInvocationResult isExistingUser() {
		BooleanInvocationResult result;
		try {
			result = new BooleanInvocationResult(!configService.newUser());
		}
		catch (Exception e) {
			result = new BooleanInvocationResult(e);
		}
		return result;
	}

	@RequestMapping(value = "/resetUser")
	public BooleanInvocationResult resetUser() {
		BooleanInvocationResult result;
		try {
			result = new BooleanInvocationResult(!configService.resetUser());
		}
		catch (Exception e) {
			result = new BooleanInvocationResult(e);
		}
		return result;
	}

	@RequestMapping(value = "/validatePassword", method = RequestMethod.POST)
	public BooleanInvocationResult validatePassword(@RequestBody String password) {
		BooleanInvocationResult result;
		try {
			result = new BooleanInvocationResult(configService.validatePassword(password));
		}
		catch (Exception e) {
			result = new BooleanInvocationResult(e);
		}
		return result;
	}

	@RequestMapping(value = "/setInitialPassword", method = RequestMethod.POST)
	public BooleanInvocationResult setInitialPassword(@RequestBody String password) {
		try {
			if (configService.newUser()) {
				configService.createUser(VALIDATION_STRING, password);
				return new BooleanInvocationResult(true);
			}
			else {
				return new BooleanInvocationResult(false);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return new BooleanInvocationResult(e);
		}
	}
	
	@RequestMapping(value = "/getConfiguration", method = RequestMethod.GET)
	public Configuration getConfiguration() {
		if (configService.newUser()) {
			return null;
		}
		else {
			return configService.getConfiguration(true);
		}
	}
	
	@RequestMapping(value = "/saveConfiguration", method = RequestMethod.POST)
	public InvocationResult saveConfiguration(@RequestBody Configuration config) {
		try {



			configService.saveConfiguration(config);
			if (config.getManagementType().equals("Yugabyte Managed")) {
				Configuration decryptedConfig = configService.getConfiguration(false);
				ybmController.setConfiguration(decryptedConfig);

				List<YBServerModel> list = ybmController.getNodeListForTopology();

				if(list == null || list.isEmpty()){
					throw new Exception("Unable to get the Cluster Info with provided inputs. Please check the inputs and try saving again.");
				}

			}
			return new InvocationResult("Ok");
		}
		catch (Exception e) {
			e.printStackTrace();
			return new InvocationResult(e);
		}
	}
}
