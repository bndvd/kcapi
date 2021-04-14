package bdn.kcapi.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	
	
	public static void process(String baseUrl, String key, String secret, String passphrase, String outputFolder) throws ControllerException {
		if (baseUrl == null || baseUrl.trim().equals("") || key == null || key.trim().equals("") || secret == null || secret.trim().equals("") || 
				passphrase == null || passphrase.trim().equals("") || outputFolder == null || outputFolder.trim().equals("")) {
			throw new ControllerException("Inputs (key, secret, passphrase, outfolder) are null/insufficient");
		}
		
		File folder = new File(outputFolder);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new ControllerException("Output folder "+folder+" does not exist or is not directory");
		}
		
		
		KucoinClientBuilder builder = new KucoinClientBuilder().withBaseUrl(baseUrl).withApiKey(key, secret, passphrase);
		KucoinRestClient kcClient = builder.buildRestClient();
		
		System.out.println("INFO: Initiating Settled Lend Order History API call");
		
		List<KcSettledLendOrderEntry> ksloeList = readSettledLendOrderHistory(kcClient);
		if (ksloeList == null) {
			throw new ControllerException("Settled Lend Order History API call failed to generate list");
		}
		System.out.println("INFO: Read "+ksloeList.size()+" events from Settled Lend Order History API call");
		
		
		List<LedgerTransaction> ltList = generateLedgerTransactions(ksloeList);
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
	
	
	private static List<KcSettledLendOrderEntry> readSettledLendOrderHistory(KucoinRestClient kcClient) throws ControllerException {
		List<KcSettledLendOrderEntry> result = new ArrayList<>();
		
		try {
			final int pageSize = 50;
			long numPagesTotal = -1;
			int idxCurrPage = 1;
			int numEventsInCurrPage = 0;
			
			do {
				Pagination<SettledTradeItem> page = kcClient.loanAPI().querySettledTrade(null, idxCurrPage, pageSize);
				if (numPagesTotal < 0) {
					// initialize
					numPagesTotal = page.getTotalPage();
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
				
				idxCurrPage++;
			}
			while ((idxCurrPage <= numPagesTotal) && (numEventsInCurrPage > 0));
		}
		catch (IOException ioe) {
			throw new ControllerException(ioe.getMessage());
		}
		
		KcSettledLendOrderEntryComparator ksloec = new KcSettledLendOrderEntryComparator();
		result.sort(ksloec);
		
		return result;
	}
	
	
	private static List<LedgerTransaction> generateLedgerTransactions(List<KcSettledLendOrderEntry> ksloeList) throws ControllerException {
		if (ksloeList == null) {
			throw new ControllerException("KC Event entries or output file is null");
		}
		
		List<LedgerTransaction> result = new ArrayList<>();
		
		for (KcSettledLendOrderEntry ksloe : ksloeList) {
			String acct = ksloe.getCurrency();
			LocalDateTime dttm = Instant.ofEpochMilli(ksloe.getSettledAt().getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
			LedgerTransactionType type = LedgerTransactionType.INCOME;
			String src = "kucoin";
			String dest = "kucoin";
			BigDecimal coinAmnt = ksloe.getRepaid().subtract(ksloe.getSize());
			
			BigDecimal usdAmnt = null;
			if (USD_STABLECOINS.contains(ksloe.getCurrency())) {
				usdAmnt = coinAmnt;
			}
			
			LedgerTransaction lt = new LedgerTransaction(acct, dttm, type, src, dest, coinAmnt, usdAmnt, null, null, null);
			result.add(lt);
		}
		
		LedgerTransactionComparator ltc = new LedgerTransactionComparator();
		result.sort(ltc);
		
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
