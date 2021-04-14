package bdn.kcapi.model;

import java.util.Comparator;

public class LedgerTransactionComparator implements Comparator<LedgerTransaction> {

	@Override
	public int compare(LedgerTransaction l, LedgerTransaction r) {
		if (l == null || r == null || l.getTxnDttm() == null || r.getTxnDttm() == null) {
			return 0;
		}
		return l.getTxnDttm().compareTo(r.getTxnDttm());
	}

}
