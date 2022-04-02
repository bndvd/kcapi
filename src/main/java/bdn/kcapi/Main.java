package bdn.kcapi;

import bdn.kcapi.controller.Controller;
import bdn.kcapi.controller.ControllerException;

public class Main {

	public static void main(String[] args) {
		System.out.println("INFO: KC-API STARTED");

		if (args.length >= 6) {
			String baseUrl = args[0];
			String key = args[1];
			String secret = args[2];
			String passphrase = args[3];
			String outputFolder = args[4];
			
			String delayPageQueriesStr = args[5];
			Integer delayPageQueries = 10000;
			try {
				delayPageQueries = Integer.parseInt(delayPageQueriesStr);
			}
			catch (Exception exc) {
				System.err.println("ERROR in parsing delay-page: " + exc.getMessage());
			}
			
			String delayHistQueriesStr = args[6];
			Integer delayHistQueries = 60000;
			try {
				delayHistQueries = Integer.parseInt(delayHistQueriesStr);
			}
			catch (Exception exc) {
				System.err.println("ERROR in parsing delay-hist: " + exc.getMessage());
			}
			
			String[] coinsToQueryHistoricals = null;
			if (args.length > 7) {
				coinsToQueryHistoricals = new String[args.length-7];
				for (int i = 7; i < args.length; i++) {
					coinsToQueryHistoricals[i-7] = args[i];
				}
			}
			
			try {
				Controller.process(baseUrl, key, secret, passphrase, outputFolder, delayPageQueries, delayHistQueries, coinsToQueryHistoricals);
			}
			catch (ControllerException exc) {
				System.err.println("ERROR: " + exc.getMessage());
			}
		}
		else {
			System.err.println("ERROR: Usage: {java-main} urlbase key secret passphrase outfolder delay-page delay-hist [coins]");
		}
		
		System.out.println("INFO: KC-API EXITED");
		System.exit(0);
	}

}
