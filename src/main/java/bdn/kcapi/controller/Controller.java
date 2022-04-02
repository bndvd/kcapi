package bdn.kcapi.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.kucoin.sdk.KucoinClientBuilder;
import com.kucoin.sdk.KucoinRestClient;
import com.kucoin.sdk.rest.response.Pagination;
import com.kucoin.sdk.rest.response.SettledTradeItem;

import bdn.kcapi.model.KcSettledLendOrderEntry;
import bdn.kcapi.model.KcSettledLendOrderEntryComparator;
import bdn.kcapi.model.LedgerTransaction;
import bdn.kcapi.model.LedgerTransaction.LedgerTransactionType;
import bdn.kcapi.model.LedgerTransactionComparator;

public class Controller {
	
	private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL;
	private static final MathContext PRECISION = new MathContext(34, RoundingMode.HALF_UP);
	private static Set<String> USD_STABLECOINS = new HashSet<>();
	
	static {
		USD_STABLECOINS.add("USDC");
		USD_STABLECOINS.add("USDT");
		USD_STABLECOINS.add("BUSD");
		USD_STABLECOINS.add("DAI");
		USD_STABLECOINS.add("UST");
		USD_STABLECOINS.add("PAX");
		USD_STABLECOINS.add("HUSD");
		USD_STABLECOINS.add("TUSD");
		USD_STABLECOINS.add("GUSD");
	}
	
	
	public static void process(String baseUrl, String key, String secret, String passphrase, String outputFolder,
			Integer delayPageQueries, Integer delayHistQueries, String[] coinsToQueryHistoricals) throws ControllerException {
		
		if (baseUrl == null || baseUrl.trim().equals("") || key == null || key.trim().equals("") || secret == null || secret.trim().equals("") || 
				passphrase == null || passphrase.trim().equals("") || outputFolder == null || outputFolder.trim().equals("") ||
				delayPageQueries == null || delayHistQueries == null) {
			throw new ControllerException("Inputs (key, secret, passphrase, outfolder) are null/insufficient");
		}
		
		File folder = new File(outputFolder);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new ControllerException("Output folder "+folder+" does not exist or is not directory");
		}
		
		
		KucoinClientBuilder builder = new KucoinClientBuilder().withBaseUrl(baseUrl).withApiKey(key, secret, passphrase);
		KucoinRestClient kcClient = builder.buildRestClient();
		
		System.out.println("INFO: Initiating Settled Lend Order History API call");
		
		List<KcSettledLendOrderEntry> ksloeList = readSettledLendOrderHistory(kcClient, delayPageQueries);
		if (ksloeList == null) {
			throw new ControllerException("Settled Lend Order History API call failed to generate list");
		}
		System.out.println("INFO: Read "+ksloeList.size()+" events from Settled Lend Order History API call");
		
		
		// build a new KC client for the historical rate queries, lessening the chance of too many requests http error
		builder = new KucoinClientBuilder().withBaseUrl(baseUrl).withApiKey(key, secret, passphrase);
		kcClient = builder.buildRestClient();
		
		List<LedgerTransaction> ltList = generateLedgerTransactions(ksloeList, kcClient, delayHistQueries, coinsToQueryHistoricals);
		if (ltList == null) {
			System.err.println("ERROR: Failed to generate Ledger Transactions");
		}
		System.out.println("INFO: Generated "+ltList.size()+" Ledger Transactions");
		

		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String now = LocalDateTime.now().format(dtf);

		String ksloeOutFileName = now + "_kucoin_settledlendorders.csv";
		File ksloeOutputFile = new File(folder, ksloeOutFileName);
		writeKcEventEntries(ksloeList, ksloeOutputFile);
		System.out.println("INFO: Wrote "+ksloeList.size()+" settled lend order entries to "+ksloeOutputFile.getAbsolutePath());
		
		if (ltList != null) {
			String ltOutFileName = "Ledger_kucoin_" + now + ".csv";
			File ltOutputFile = new File(folder, ltOutFileName);
			writeLedgerTransactionEntries(ltList, ltOutputFile);
			System.out.println("INFO: Wrote "+ltList.size()+" ledger transaction entries to "+ltOutputFile.getAbsolutePath());
		}
	}
	
	
	private static List<KcSettledLendOrderEntry> readSettledLendOrderHistory(KucoinRestClient kcClient, Integer delayPageQueries)
			throws ControllerException {
		List<KcSettledLendOrderEntry> result = new ArrayList<>();
		
		if (kcClient == null || delayPageQueries == null) {
			throw new ControllerException("readSettledLendOrderHistory received null argument(s)");
		}
		
		try {
			final int pageSize = 50;
			long numPagesTotal = -1;
			int idxCurrPage = 1;
			int numEventsInCurrPage = 0;
			
			do {
				System.out.println("INFO: Querying Page "+idxCurrPage+(numPagesTotal>0 ? " of " + 
						( numPagesTotal>100 ? 100 : numPagesTotal ) : "")+" from KuCoin service");
				
				Pagination<SettledTradeItem> page = kcClient.loanAPI().querySettledTrade(null, idxCurrPage, pageSize);
				if (numPagesTotal < 0) {
					// initialize
					numPagesTotal = page.getTotalPage();
					// KuCoin does not allow > 100 pages to be queries
					if (numPagesTotal > 100) {
						System.err.println("WARNING: # pages: "+numPagesTotal+" exceeded max returnable pages of 100. Will return most recent 100 pages of data.");
					}
				}
				else if (numPagesTotal != page.getTotalPage()) {
					// total number changed since we started querying, throw an exception
					throw new ControllerException("SettledLendOrder event count was inconsistent. Init cnt: "+numPagesTotal+
							" new cnt: "+page.getTotalPage()+". Please rerun the program.");
				}
				
				List<SettledTradeItem> stiList = page.getItems();
				numEventsInCurrPage = stiList.size();
				if (stiList != null) {
					for (SettledTradeItem sti : stiList) {
						KcSettledLendOrderEntry ksloe = new KcSettledLendOrderEntry(
								sti.getTradeId(),
								sti.getSettledAt(),
								sti.getCurrency(),
								sti.getSize(),
								sti.getInterest(),
								sti.getRepaid(),
								sti.getDailyIntRate(),
								sti.getTerm(),
								sti.getNote()
								);
						result.add(ksloe);
					}
				}
				
				// sleep between service calls to avoid the KuCoin Request Rate Limit (up to 10 requests / sec)
				try {
					Thread.sleep(delayPageQueries);
				}
				catch(InterruptedException ie) {}
				
				idxCurrPage++;
			}
			while ((idxCurrPage <= numPagesTotal) && (idxCurrPage <= 100) && (numEventsInCurrPage > 0));
		
			
			System.out.println("INFO: Successfully read "+(idxCurrPage-1)+" pages, "+result.size()+" settled orders from KuCoin service");
			try {
				// pause so the user can see the results before exiting
				Thread.sleep(5000);
			}
			catch(InterruptedException ie) {}
		}
		catch (IOException ioe) {
			throw new ControllerException(ioe.getMessage());
		}

		KcSettledLendOrderEntryComparator ksloec = new KcSettledLendOrderEntryComparator();
		result.sort(ksloec);
		
		return result;
	}
	
	
	private static List<LedgerTransaction> generateLedgerTransactions(List<KcSettledLendOrderEntry> ksloeList,
			KucoinRestClient kcClient, Integer delayHistQueries, String[] coinsToQueryHistoricals) throws ControllerException {
		
		if (ksloeList == null || kcClient == null || delayHistQueries == null) {
			throw new ControllerException("KC Event entries or kcClient is null");
		}
		
		List<LedgerTransaction> result = new ArrayList<>();
		Date endAt = new Date();
		// enforce an initial wait before KuCoin API historic data queries to lessen the chance of too many requests http error
		boolean initialWait = false;
		
		// map for queried historic data by currency
		Map<String, List<List<String>>> acctToHistoricData = new HashMap<>();
		
		Set<String> coinsToQueryHistoricalsSet = null;
		if (coinsToQueryHistoricals != null && coinsToQueryHistoricals.length > 0) {
			coinsToQueryHistoricalsSet = new HashSet<String>();
			for (String c : coinsToQueryHistoricals) {
				coinsToQueryHistoricalsSet.add(c);
			}
		}
		
		
		for (KcSettledLendOrderEntry ksloe : ksloeList) {
			String acct = ksloe.getCurrency();
			Date settledAt = ksloe.getSettledAt();
			LocalDateTime dttm = Instant.ofEpochMilli(settledAt.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
			LedgerTransactionType type = LedgerTransactionType.INCOME;
			String src = "kucoin";
			String dest = "kucoin";
			BigDecimal coinAmnt = ksloe.getRepaid().subtract(ksloe.getSize());
			
			BigDecimal usdAmnt = null;
			BigDecimal usdPerUnit = null;
			if (USD_STABLECOINS.contains(ksloe.getCurrency())) {
				usdAmnt = coinAmnt;
				usdPerUnit = BigDecimal.ONE;
			}
			else if (coinsToQueryHistoricalsSet == null || coinsToQueryHistoricalsSet.contains(acct)) {
				List<List<String>> histData = acctToHistoricData.get(acct);
				if (histData == null) {
					if (!initialWait) {
						System.out.println("INFO: Initial wait before Historic Rate queries ("+(delayHistQueries/1000)+" sec delay)");
						try {
							Thread.sleep(delayHistQueries);
						}
						catch(InterruptedException ie) {}
						initialWait = true;
					}
					
					histData = queryHistoricData(kcClient, acct, settledAt, endAt, delayHistQueries);
					if (histData == null) {
						throw new ControllerException("Historic rate query returned null data for: "+acct);
					}
					acctToHistoricData.put(acct, histData);
				}
				
				// read historic rate to determine the USD value of the transaction
				usdPerUnit = extractHistoricRate(histData, settledAt);
				if (usdPerUnit != null) {
					usdAmnt = usdPerUnit.multiply(coinAmnt);
				}
			}
			
			LedgerTransaction lt = new LedgerTransaction(acct, dttm, type, src, dest, coinAmnt, usdAmnt, usdPerUnit, null, null);
			result.add(lt);
		}
		
		LedgerTransactionComparator ltc = new LedgerTransactionComparator();
		result.sort(ltc);
		
		return result;
	}
	
	
	private static List<List<String>> queryHistoricData(KucoinRestClient kcClient, String currency, Date startDate, Date endDate,
			Integer delayHistQueries) throws ControllerException {
		
		if (kcClient == null || currency == null || currency.trim().equals("") || delayHistQueries == null) {
			throw new ControllerException("Inputs for historic rate query are null/empty");
		}
		
		List<List<String>> result = null;
		// rates are in USD, proxied by USDT
		String symbol = currency + "-USDT";
		long unixTime = startDate.getTime() / 1000L;
		// include the previous 24 hours before start date to ensure at least one data point
		long startAt = unixTime - (60L * 60L * 24L);
		unixTime = endDate.getTime() / 1000L;
		long endAt = unixTime;
		
		
		System.out.println("INFO: Querying Historic Rate: "+symbol+", start date \""+startDate+"\" from KuCoin service ("+
				(delayHistQueries/1000)+" sec delay)");

		// sleep between service calls to avoid the KuCoin Request Rate Limit for historic rate query
		try {
			Thread.sleep(delayHistQueries);
		}
		catch(InterruptedException ie) {}
		
		try {
			List<List<String>> rateData = kcClient.historyAPI().getHistoricRates(symbol, startAt, endAt, "1day");
			if (rateData == null || rateData.isEmpty()) {
				throw new ControllerException("Historic rate query returned no rate data for "+symbol+", start="+startAt+", end="+endAt);
			}
			result = rateData;
		}
		catch (IOException ioe) {
			throw new ControllerException(ioe.getMessage());
		}
		
		return result;
	}
	
	
	private static BigDecimal extractHistoricRate(List<List<String>> data, Date dttm) throws ControllerException {
		if (data == null || data.isEmpty() || dttm == null) {
			return null;
		}
		
		BigDecimal result = null;
		long unixDttm = dttm.getTime() / 1000L;

		for (List<String> candle : data) {
			if (candle == null || candle.size() < 7) {
				throw new ControllerException("extractHistoricRate encountered corrupt candle entry");
			}
			long timestamp = Long.valueOf(candle.get(0));
			
			if (timestamp < unixDttm) {
				String highPriceStr = candle.get(3);
				String lowPriceStr = candle.get(4);
				
				BigDecimal highPrice = new BigDecimal(highPriceStr);
				BigDecimal lowPrice = new BigDecimal(lowPriceStr);
				// take the average of highest and lowest price
				result = highPrice.add(lowPrice).divide(new BigDecimal(2), PRECISION);
				break;
			}
		}
		
		return result;
	}
		
	
	private static void writeKcEventEntries(List<KcSettledLendOrderEntry> ksloeList, File outputFile) throws ControllerException {
		if (ksloeList == null || outputFile == null) {
			throw new ControllerException("KC Event entries or output file is null");
		}
		if (outputFile.exists()) {
			throw new ControllerException("Could not write to output file "+outputFile.getAbsolutePath()+" as it already exists");
		}
		
		try {
			CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSV_FORMAT);
			
			printer.printRecord(
					KcSettledLendOrderEntry.COL_TRADE_ID,
					KcSettledLendOrderEntry.COL_STL_DTTM,
					KcSettledLendOrderEntry.COL_CURRENCY,
					KcSettledLendOrderEntry.COL_SIZE,
					KcSettledLendOrderEntry.COL_INTEREST,
					KcSettledLendOrderEntry.COL_REPAID,
					KcSettledLendOrderEntry.COL_DAILY_INT_RATE,
					KcSettledLendOrderEntry.COL_TERM,
					KcSettledLendOrderEntry.COL_NOTE
					);
			
			
			for (KcSettledLendOrderEntry ksloe : ksloeList) {
				printer.printRecord(
						ksloe.getTradeId(),
						ksloe.getSettledAtStr(),
						ksloe.getCurrency(),
						ksloe.getSizeStr(),
						ksloe.getInterestStr(),
						ksloe.getRepaidStr(),
						ksloe.getDailyIntRateStr(),
						ksloe.getTermStr(),
						ksloe.getNote()
						);
			}
			
			printer.close(true);
		}
		catch (IOException ioExc) {
			throw new ControllerException(ioExc.getMessage());
		}
	}
	
	private static void writeLedgerTransactionEntries(List<LedgerTransaction> ltList, File outputFile) throws ControllerException {
		if (ltList == null || outputFile == null) {
			throw new ControllerException("Ledger Transaction entries or output file is null");
		}
		if (outputFile.exists()) {
			throw new ControllerException("Could not write to output file "+outputFile.getAbsolutePath()+" as it already exists");
		}
		
		try {
			CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSV_FORMAT);
			
			printer.printRecord(
					LedgerTransaction.COL_TXN_ACCT,
					LedgerTransaction.COL_TXN_DTTM,
					LedgerTransaction.COL_TXN_TYPE,
					LedgerTransaction.COL_TXN_SRC,
					LedgerTransaction.COL_TXN_DEST,
					LedgerTransaction.COL_TXN_COIN_AMNT,
					LedgerTransaction.COL_TXN_USD_AMNT,
					LedgerTransaction.COL_TXN_USD_PER_UNIT,
					LedgerTransaction.COL_TXN_FEE_COIN,
					LedgerTransaction.COL_TXN_BRKR_FEE_USD
					);
			
			
			for (LedgerTransaction lt : ltList) {
				printer.printRecord(
						lt.getTxnAcct(),
						lt.getTxnDttmStr(LedgerTransaction.DTF_DASH),
						lt.getTxnTypeStr(),
						lt.getTxnSrc(),
						lt.getTxnDest(),
						lt.getTxnCoinAmntStr(),
						lt.getTxnUsdAmntStr(),
						lt.getTxnUsdPerUnitStr(),
						lt.getTxnFeeCoinStr(),
						lt.getTxnBrkrFeeUsdStr()
						);
			}
			
			printer.close(true);
		}
		catch (IOException ioExc) {
			throw new ControllerException(ioExc.getMessage());
		}
	}
	
	
}
