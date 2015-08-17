package easyLinking;

import java.util.Comparator;

public class IdComparator implements Comparator<String[]> {

	@Override
	// o1 = "3 4 5", o2 = "8 9"    return -1
	// o1 = "3 4 5", o2 = "0 1"    return 1
	// o1 = "3 4 5", o2 = "4 5 6"  ERROR
	public int compare(String[] o1, String[] o2) {
		
		int o1min = Integer.parseInt(o1[0].split(" ")[0]),
				o1max = Integer.parseInt(o1[0].split(" ")[o1[0].split(" ").length-1]);
		int o2min = Integer.parseInt(o2[0].split(" ")[0]),
				o2max = Integer.parseInt(o2[0].split(" ")[o2[0].split(" ").length-1]);
		if (o1max <= o2min) {
			return -1;
		} else if (o1min >= o2max) {
			return 1;
		} else {
			System.out.print(o1min + " " + o1max + " " + o2min + " " + o2max);
			System.err.println("ERROR: id range should not overlap");
			//System.exit(0);
			if ((o1max-o1min) >= (o2max-o2min)) {
				return 1;
			} else {
				return -1;
			}
		}
	}

}
