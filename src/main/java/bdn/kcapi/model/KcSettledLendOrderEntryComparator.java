package bdn.kcapi.model;

import java.util.Comparator;

public class KcSettledLendOrderEntryComparator implements Comparator<KcSettledLendOrderEntry> {

	@Override
	public int compare(KcSettledLendOrderEntry l, KcSettledLendOrderEntry r) {
		if (l == null || r == null || l.getSettledAt() == null || r.getSettledAt() == null) {
			return 0;
		}
		return l.getSettledAt().compareTo(r.getSettledAt());
	}

}
