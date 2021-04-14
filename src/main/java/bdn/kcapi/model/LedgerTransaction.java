package bdn.kcapi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class LedgerTransaction {
	
	public static final String COL_TXN_ACCT = "Acct";
	public static final String COL_TXN_DTTM = "UTC Dttm";
	public static final String COL_TXN_TYPE = "Txn Type";
	public static final String COL_TXN_SRC = "Src";
	public static final String COL_TXN_DEST = "Dest";
	public static final String COL_TXN_COIN_AMNT = "Txn COIN";
	public static final String COL_TXN_USD_AMNT = "Txn USD";
	public static final String COL_TXN_USD_PER_UNIT = "Txn USD/COIN";
	public static final String COL_TXN_FEE_COIN = "Txn Fee COIN";
	public static final String COL_TXN_BRKR_FEE_USD = "Brkr Fee USD";

	public static enum LedgerTransactionType {
		ACQUIRE, DISPOSE, TRANSFER, INCOME
	}
	
	private static HashMap<LedgerTransactionType, String> enumToTypeStr = new HashMap<>();
	static {
		enumToTypeStr.put(LedgerTransactionType.ACQUIRE, "acq");
		enumToTypeStr.put(LedgerTransactionType.DISPOSE, "disp");
		enumToTypeStr.put(LedgerTransactionType.TRANSFER, "tran");
		enumToTypeStr.put(LedgerTransactionType.INCOME, "inc");
	}
	

	public static final DateTimeFormatter DTF_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	public static final DateTimeFormatter DTF_SLASH = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
	
	private String txnAcct = null;
	private LocalDateTime txnDttm = null;
	private LedgerTransactionType txnType = null;
	private String txnSrc = null;
	private String txnDest = null;
	private BigDecimal txnCoinAmnt = null;
	private BigDecimal txnUsdAmnt = null;
	private BigDecimal txnUsdPerUnit = null;
	private BigDecimal txnFeeCoin = null;
	private BigDecimal txnBrkrFeeUsd = null;
	

	public LedgerTransaction(String txnAcct, LocalDateTime txnDttm, LedgerTransactionType txnType, String txnSrc,
			String txnDest, BigDecimal txnCoinAmnt, BigDecimal txnUsdAmnt, BigDecimal txnUsdPerUnit,
			BigDecimal txnFeeCoin, BigDecimal txnBrkrFeeUsd) {
		this.txnAcct = txnAcct;
		this.txnDttm = txnDttm;
		this.txnType = txnType;
		this.txnSrc = txnSrc;
		this.txnDest = txnDest;
		this.txnCoinAmnt = txnCoinAmnt;
		this.txnUsdAmnt = txnUsdAmnt;
		this.txnUsdPerUnit = txnUsdPerUnit;
		this.txnFeeCoin = txnFeeCoin;
		this.txnBrkrFeeUsd = txnBrkrFeeUsd;
	}


	public String getTxnAcct() {
		return (txnAcct == null) ? "" : txnAcct.trim();
	}


	public LocalDateTime getTxnDttm() {
		return txnDttm;
	}


	public String getTxnDttmStr(DateTimeFormatter dtf) {
		return txnDttm.format(dtf);
	}
	
	
	public LedgerTransactionType getTxnType() {
		return txnType;
	}


	public String getTxnTypeStr() {
		return (txnType == null) ? "" : enumToTypeStr.get(txnType);
	}
	
	
	public String getTxnSrc() {
		return (txnSrc == null) ? "" : txnSrc.trim();
	}


	public String getTxnDest() {
		return (txnDest == null) ? "" : txnDest.trim();
	}


	public BigDecimal getTxnCoinAmnt() {
		return txnCoinAmnt;
	}


	public String getTxnCoinAmntStr() {
		return (txnCoinAmnt == null) ? "" : txnCoinAmnt.toPlainString();
	}


	public BigDecimal getTxnUsdAmnt() {
		return txnUsdAmnt;
	}


	public String getTxnUsdAmntStr() {
		return (txnUsdAmnt == null) ? "" : txnUsdAmnt.toPlainString();
	}


	public BigDecimal getTxnUsdPerUnit() {
		return txnUsdPerUnit;
	}


	public String getTxnUsdPerUnitStr() {
		return (txnUsdPerUnit == null) ? "" : txnUsdPerUnit.toPlainString();
	}


	public BigDecimal getTxnFeeCoin() {
		return txnFeeCoin;
	}


	public String getTxnFeeCoinStr() {
		return (txnFeeCoin == null) ? "" : txnFeeCoin.toPlainString();
	}


	public BigDecimal getTxnBrkrFeeUsd() {
		return txnBrkrFeeUsd;
	}


	public String getTxnBrkrFeeUsdStr() {
		return (txnBrkrFeeUsd == null) ? "" : txnBrkrFeeUsd.toPlainString();
	}


	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(getTxnDttm()).append(",")
			.append(getTxnType()).append(",")
			.append(getTxnCoinAmnt()).append(",")
			.append(getTxnUsdAmnt()).append(",")
			.append(getTxnUsdPerUnit()).append(",")
			.append(getTxnFeeCoin()).append(",")
			.append(getTxnBrkrFeeUsd()).append(",");
		
		return buf.toString();
	}

}
