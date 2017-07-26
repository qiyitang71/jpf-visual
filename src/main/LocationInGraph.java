import java.util.HashMap;
import java.util.Map;

import gov.nasa.jpf.util.Pair;

public class LocationInGraph {
	private Map<Pair<Integer, Integer>, Object> contentMap;
	private Map<Pair<Integer, Integer>, Object> summaryMap;
	private Map<Integer, Object> swimMap;

	public LocationInGraph() {
		contentMap = new HashMap<Pair<Integer, Integer>, Object>();
		swimMap = new HashMap<Integer, Object>();
	}

	public Object getContentCell(int row, int line) {
		Pair<Integer, Integer> p = new Pair<>(row, line);
		if (contentMap.containsKey(p)) {
			return contentMap.get(p);
		}
		return null;
	}

	public Object getContentCell(Pair<Integer, Integer> pair) {
		return contentMap.get(pair);
	}

	public void addContentCell(int row, int line, Object cell) {
		contentMap.put(new Pair<Integer, Integer>(row, line), cell);
	}

	public void addContentCell(Pair<Integer, Integer> pair, Object cell) {
		contentMap.put(pair, cell);
	}

	public Object getSwimCell(int rowNum) {
		return swimMap.get(rowNum);
	}

	public void addSwimCell(int row, Object cell) {
		swimMap.put(row, cell);

	}

}
