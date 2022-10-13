package com.yugabyte.simulation.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.yugabyte.simulation.dao.Configuration;
import com.yugabyte.simulation.dao.LoginInformation;

@Service
public class ConfigurationService {
	
	@Autowired
	private CryptoService cryptoService;

	private static final String SALT = "YugayteManagementConsole";
	private Configuration configuration = null;
	private String password;
	private final String configFilePath;
	public static final String MASK = "********";

	public ConfigurationService() {
		String finalConfigFile = null;
		try {
			Path configDirPath = Paths.get(System.getProperty("user.home", "."), ".yugabyte");
			Files.createDirectories(configDirPath);
			finalConfigFile = configDirPath.toString();
			Path configFilePath  = Paths.get(finalConfigFile, "config.yml");
			finalConfigFile = configFilePath.toString();
		}
		catch (IOException ioe) {
			System.err.println("Error starting Configuration Service: " +ioe.getMessage());
			ioe.printStackTrace();
		}
		this.configFilePath = finalConfigFile;
	}

	private Configuration getConfiguration() {
		if (configuration == null) {
			File file = new File(configFilePath);

			if (!file.exists()) {
				configuration = new Configuration();
				saveConfiguration();
			}
			else {
				// Instantiating a new ObjectMapper as a YAMLFactory
				ObjectMapper om = new ObjectMapper(new YAMLFactory());

				try {
					configuration = om.readValue(file, Configuration.class);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return configuration;
	}


	private void saveConfiguration() {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		try {
			om.writeValue(new File(configFilePath), configuration);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Configuration decryptConfiguration(Configuration configuration, boolean maskSecrets) {
		if (password == null || configuration.getAccessKey() == null || configuration.getAccessKey().isBlank()) {
			return configuration;
		}

		Configuration decryptedConfiguration = configuration.clone();

		String loginInitialVector = getConfiguration().getLogin().getInitVector();
		try {
			String decryptedAccessKey = cryptoService.decryptPasswordBased(
					configuration.getAccessKey(),
					cryptoService.getKeyFromPassword(password, SALT),
					cryptoService.getIvParameterSpec(loginInitialVector));
			
			if (maskSecrets) {
				if (decryptedAccessKey.length() <= 8) {
					// Should not happen
					decryptedAccessKey = MASK;
				}
				else {
					decryptedAccessKey = decryptedAccessKey.substring(0, 4) +
							MASK +
							decryptedAccessKey.substring(decryptedAccessKey.length() - 4);
				}
			}
			decryptedConfiguration.setAccessKey(decryptedAccessKey);
			return decryptedConfiguration;
			
		} catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
					| InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
					| InvalidKeySpecException e) {

			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Configuration getConfiguration(boolean maskSecrets) {
		Configuration configuration = getConfiguration();
		return decryptConfiguration(configuration, maskSecrets);
	}

	public void saveConfiguration(Configuration configuration) {
		if (configuration.getLogin() == null) {
			configuration.setLogin(getConfiguration().getLogin());
		}
		
		// If the user has not changed the access key, just get the encrypted one off the server.
		if (configuration.getAccessKey() == null || configuration.getAccessKey().isEmpty()) {
			configuration.setAccessKey("");
		}
		else if (configuration.getAccessKey().indexOf(MASK) >= 0) {
			configuration.setAccessKey(getConfiguration().getAccessKey());
			this.configuration = configuration;
		}
		else {
			Configuration encryptedConfiguration = configuration.clone();
			LoginInformation login = getConfiguration().getLogin();
			String encryptedAccessKey;
			try {
				encryptedAccessKey = cryptoService.encryptPasswordBased(
						configuration.getAccessKey(),
						cryptoService.getKeyFromPassword(password, SALT),
						cryptoService.getIvParameterSpec(login.getInitVector()));
				
			} catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
					| InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
					| InvalidKeySpecException e) {

				e.printStackTrace();
				throw new RuntimeException(e);
			}
	
			encryptedConfiguration.setAccessKey(encryptedAccessKey);
			this.configuration = encryptedConfiguration;
		}
		saveConfiguration();
	}

	public boolean newUser() {
		Configuration config = getConfiguration();
		if (config.getLogin() == null || config.getLogin().getEmail() == null || config.getLogin().getValidation() == null) {
			return true;
		}
		return false;
	}

	public boolean resetUser() {
		if (!newUser()) {
			Configuration config = getConfiguration();
			config.getLogin().setValidation(null);
			config.setAccessKey(null);
			saveConfiguration(config);
			return true;
		}
		return false;
	}
	
	public void createUser(String email, String password) {
		LoginInformation login = new LoginInformation();
		login.setEmail(email);
		login.setInitVector(cryptoService.generateIvAsString());
		try {
			login.setValidation(
					cryptoService.encryptPasswordBased(
							email,
							cryptoService.getKeyFromPassword(password, SALT),
							cryptoService.getIvParameterSpec(login.getInitVector())
					)
			);
			getConfiguration().setLogin(login);
			saveConfiguration();

		} catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
				| InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
				| InvalidKeySpecException e) {

			throw new RuntimeException(e);
		}
	}

	public boolean validatePassword(String password) {
		LoginInformation login = getConfiguration().getLogin();
		String validationString;
		try {
			validationString = cryptoService.encryptPasswordBased(
					login.getEmail(),
					cryptoService.getKeyFromPassword(password, SALT),
					cryptoService.getIvParameterSpec(login.getInitVector()));

			if (validationString.equals(login.getValidation())) {
				this.password = password;
				return true;
			}

		} catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
				| InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException
				| InvalidKeySpecException e) {

			e.printStackTrace();
		}
		return false;

	}
}

