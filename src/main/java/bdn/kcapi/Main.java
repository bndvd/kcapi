package bdn.kcapi;

import bdn.kcapi.controller.Controller;
import bdn.kcapi.controller.ControllerException;

public class Main {

	public static void main(String[] args) {
		System.out.println("INFO: KC-API STARTED");

		if (args.length == 5) {
			String baseUrl = args[0];
			String key = args[1];
			String secret = args[2];
			String passphrase = args[3];
			String outputFolder = args[4];
			
			try {
				Controller.process(baseUrl, key, secret, passphrase, outputFolder);
			}
			catch (ControllerException exc) {
				System.err.println("ERROR: " + exc.getMessage());
			}
		}
		else {
			System.err.println("ERROR: Usage: {java-main} urlbase key secret passphrase outfolder");
		}
		
		System.out.println("INFO: KC-API EXITED");
		System.exit(0);
	}

}
