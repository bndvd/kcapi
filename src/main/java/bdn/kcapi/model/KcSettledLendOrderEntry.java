package bdn.kcapi.model;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class KcSettledLendOrderEntry {

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	public static final String COL_TRADE_ID = "tradeId";
	public static final String COL_STL_DTTM = "settledAt";
	public static final String COL_CURRENCY = "currency";
	public static final String COL_SIZE = "size";
	public static final String COL_INTEREST = "interest";
	public static final String COL_REPAID = "repaid";
	public static final String COL_DAILY_INT_RATE = "dailyIntRate";
	public static final String COL_TERM = "term";
	public static final String COL_NOTE = "note";


	private String tradeId;
	private Date settledAt;
	private String currency;
	private BigDecimal size;
	private BigDecimal interest;
	private BigDecimal repaid;
	private BigDecimal dailyIntRate;
	private Integer term;
	private String note;
	
	
	public KcSettledLendOrderEntry(String tradeId, Date settledAt, String currency, BigDecimal size, BigDecimal interest, BigDecimal repaid,
			BigDecimal dailyIntRate, Integer term, String note) {
		this.tradeId = tradeId;
		this.settledAt = settledAt;
		this.currency = currency;
		this.size = size;
		this.interest = interest;
		this.repaid = repaid;
		this.dailyIntRate = dailyIntRate;
		this.term = term;
		this.note = note;
	}


	public String getTradeId() {
		return (tradeId == null) ? "" : tradeId.trim();
	}


	public Date getSettledAt() {
		return settledAt;
	}


	public String getSettledAtStr() {
		return (settledAt == null) ? "" : SDF.format(settledAt);
	}


	public String getCurrency() {
		return (currency == null) ? "" : currency.trim();
	}


	public BigDecimal getSize() {
		return size;
	}


	public String getSizeStr() {
		return (size == null) ? "" : size.toPlainString();
	}


	public BigDecimal getInterest() {
		return interest;
	}


	public String getInterestStr() {
		return (interest == null) ? "" : interest.toPlainString();
	}


	public BigDecimal getRepaid() {
		return repaid;
	}


	public String getRepaidStr() {
		return (repaid == null) ? "" : repaid.toPlainString();
	}


	public BigDecimal getDailyIntRate() {
		return dailyIntRate;
	}


	public String getDailyIntRateStr() {
		return (dailyIntRate == null) ? "" : dailyIntRate.toPlainString();
	}


	public Integer getTerm() {
		return term;
	}


	public String getTermStr() {
		return (term == null) ? "" : term.toString();
	}


	public String getNote() {
		return (note == null) ? "" : note.trim();
	}
	
}
