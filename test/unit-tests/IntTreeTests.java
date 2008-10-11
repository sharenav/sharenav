import java.util.Arrays;
import java.util.Random;

import de.ueller.gps.tools.intTree;
import junit.framework.TestCase;

public class IntTreeTests extends TestCase {

	int[] ranNums;

	private intTree initIntTree(int ordering) {
		intTree it = new intTree();
		ranNums = new int[10000];
		Random r = new Random();
		ranNums[0] = r.nextInt();
		it.put(ranNums[0], new Integer(ranNums[0]));
		for (int i = 1; i < ranNums.length; i++) {
			switch (ordering) {
			case 0:
				ranNums[i] = ranNums[i - 1] + r.nextInt(10000) + 1;
				break;
			case 1:
				ranNums[i] = ranNums[i - 1] - r.nextInt(10000) - 1;
				break;
			case 2:
				ranNums[i] = r.nextInt(10000);
				break;
			}
			it.put(ranNums[i], new Integer(ranNums[i]));
		}
		return it;
	}

	public void testPut() {
		intTree it = initIntTree(0);
		it = initIntTree(1);
		it = initIntTree(2);
	}

	public void testGet() {
		for (int s = 0; s < 2; s++) {
			intTree it = initIntTree(s);
			for (int i = 0; i < ranNums.length; i++) {
				assertEquals(ranNums[i], ((Integer) it.get(ranNums[i]))
						.intValue());
			}
		}
	}

	public void testGetValueIdx() {
		for (int s = 0; s < 1; s++) { // Don't use random ordering as may
										// contain duplicates
			intTree it = initIntTree(s);
			int[] sortRanNums = ranNums.clone();
			Arrays.sort(sortRanNums);
			for (int i = 0; i < sortRanNums.length; i++) {
				assertEquals("Key of idx " + i + " was not as expected",
						new Integer(sortRanNums[i]), it.getValueIdx(i));
			}
		}
	}

	public void testGetKeyIdx() {
		for (int s = 0; s < 1; s++) { // Don't use random ordering as may
										// contain duplicates
			intTree it = initIntTree(s);
			int[] sortRanNums = ranNums.clone();
			Arrays.sort(sortRanNums);
			for (int i = 0; i < sortRanNums.length; i++) {
				assertEquals("Key of idx " + i + " was not as expected",
						sortRanNums[i], it.getKeyIdx(i));
			}
		}
	}

	public void testSize() {
		for (int s = 0; s < 1; s++) { // Don't use random ordering as may
										// contain duplicates
			intTree it0 = new intTree();
			assertEquals("Testing the empty tree", 0, it0.size());
			intTree it = initIntTree(s);
			assertEquals(ranNums.length, it.size());
		}
	}

	public void testPopFirstKey() {
		for (int s = 0; s < 1; s++) { // Don't use random ordering as may
										// contain duplicates
			intTree it = initIntTree(s);
			int[] sortRanNums = ranNums.clone();
			Arrays.sort(sortRanNums);
			for (int i = 0; i < sortRanNums.length; i++) {
				assertEquals(sortRanNums[i], it.popFirstKey());
				assertEquals(ranNums.length - i - 1, it.size());
			}
		}
	}

	public void testRemoveAll() {
		for (int s = 0; s < 2; s++) {
			intTree it = initIntTree(s);
			it.removeAll();
			assertEquals("Test number of entries before deleting:", 0, it
					.size());
			for (int i = 0; i < ranNums.length; i++) {
				assertEquals("Test elements are deleted:", null, it
						.get(ranNums[i]));
			}
		}
	}

	public void testRemove() {
		for (int s = 0; s < 1; s++) { // Don't use random ordering as may
										// contain duplicates
			intTree it = initIntTree(s);
			Random rr = new Random();
			int noRemov = 0;
			for (int i = 0; i < ranNums.length; i++) {
				if (rr.nextInt(100) < 20) {
					assertEquals(ranNums[i], ((Integer) it.get(ranNums[i]))
							.intValue());
					it.remove(ranNums[i]);
					noRemov++;
					assertEquals(ranNums.length - noRemov, it.size());
					assertEquals(null, it.get(ranNums[i]));
				}
			}
		}
	}

	public void testClone() {
		for (int s = 0; s < 2; s++) {
			intTree it = initIntTree(s);
			intTree it2 = new intTree();
			it2.clone(it);

			assertEquals(it.size(), it2.size());
			for (int i = 0; i < it.size(); i++) {
				assertEquals(((Integer) it.get(ranNums[i])).intValue(),
						((Integer) it2.get(ranNums[i])).intValue());
			}
			for (int i = 0; i < it.size(); i++) {
				assertEquals(it.getKeyIdx(i), it2.getKeyIdx(i));
			}
			for (int i = 0; i < it.size(); i++) {
				assertEquals(it.getValueIdx(i), it2.getValueIdx(i));
			}
		}

	}

}
