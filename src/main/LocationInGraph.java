import java.util.HashMap;
import java.util.Map;

import gov.nasa.jpf.util.Pair;

public class LocationInGraph {
	private Map<Pair<Integer, Integer>, Object> contentMap;
	private Map<Integer, Object> swimMap;
	private Map<Pair<Integer, Integer>, SummaryCell> summaryMap;
	private Map<Integer, Object> rowCellMap;
	private Map<Integer, Object> rightCellMap;
	private Map<Integer, Object> summaryBorderMap;
	private Map<Integer, Object> switchMap;

	public LocationInGraph() {
		contentMap = new HashMap<>();
		swimMap = new HashMap<>();
		summaryMap = new HashMap<>();
		rowCellMap = new HashMap<>();
		rightCellMap = new HashMap<>();
		summaryBorderMap = new HashMap<>();
		switchMap = new HashMap<>();
	}

	public Object getSwitchCell(int row) {
		if (switchMap.containsKey(row)) {
			return switchMap.get(row);
		}
		return null;
	}

	public void addSwitchCell(int row, Object cell) {
		switchMap.put(row, cell);
	}

	public Object getContentCell(int row, int line) {
		Pair<Integer, Integer> p = new Pair<>(row, line);
		if (contentMap.containsKey(p)) {
			return contentMap.get(p);
		}
		return null;
	}

	public Object getContentCell(Pair<Integer, Integer> pair) {
		if (contentMap.containsKey(pair)) {
			return contentMap.get(pair);
		}
		return null;
	}

	public void addContentCell(int row, int line, Object cell) {
		contentMap.put(new Pair<Integer, Integer>(row, line), cell);
	}

	public void addContentCell(Pair<Integer, Integer> pair, Object cell) {
		contentMap.put(pair, cell);
	}

	public Object getSwimCell(int rowNum) {
		if (swimMap.containsKey(rowNum)) {
			return swimMap.get(rowNum);
		}
		return null;
	}

	public void addSwimCell(int row, Object cell) {
		swimMap.put(row, cell);

	}

	public SummaryCell getSummaryCell(int row, int line) {
		Pair<Integer, Integer> p = new Pair<>(row, line);
		if (summaryMap.containsKey(p)) {
			return summaryMap.get(p);
		}
		return null;
	}

	public SummaryCell getSummaryCell(Pair<Integer, Integer> pair) {
		if (summaryMap.containsKey(pair)) {
			return summaryMap.get(pair);
		}
		return null;
	}

	public void addSummaryCell(int row, int line, SummaryCell cell) {
		summaryMap.put(new Pair<Integer, Integer>(row, line), cell);
	}

	public void addSummaryCell(Pair<Integer, Integer> pair, SummaryCell cell) {
		summaryMap.put(pair, cell);
	}

	public void addRowCell(int row, Object cell) {
		rowCellMap.put(row, cell);
	}

	public Object getRowCell(int row) {
		if (rowCellMap.containsKey(row)) {
			return rowCellMap.get(row);
		}
		return null;
	}

	public void addRightCell(int row, Object cell) {
		rightCellMap.put(row, cell);
	}

	public Object getRightCell(int row) {
		if (rightCellMap.containsKey(row)) {
			return rightCellMap.get(row);
		}
		return null;
	}

	public void addSummaryBorderCell(int row, Object cell) {
		summaryBorderMap.put(row, cell);
	}

	public Object getSummaryBorderCell(int row) {
		if (summaryBorderMap.containsKey(row)) {
			return summaryBorderMap.get(row);
		}
		return null;
	}
}
