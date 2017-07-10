
import gov.nasa.jpf.vm.Transition;

public class TextLine {
	private String text;
	private boolean isCG;
	private boolean isSrc;
	private Transition tran;
	private int lineNum;
	private int groupNum;
	private boolean isFirst = false;
	private boolean isLast = false;
	private int stepStart;
	private int stepEnd;

	public TextLine(String text, boolean isCG, boolean isSrc, Transition tran, int groupNum, int lineNum) {
		this.text = text;
		this.isCG = isCG;
		this.isSrc = isSrc;
		this.tran = tran;
		this.lineNum = lineNum;
		this.groupNum = groupNum;
	}

	public String getText() {
		return text;
	}

	public boolean isCG() {
		return isCG;
	}

	public boolean isSrc() {
		return isSrc;
	}

	public Transition getTransition() {
		return tran;
	}

	public int getLineNum() {
		return lineNum;
	}

	public int getGroupNum() {
		return groupNum;
	}

	public boolean isFirst() {
		return isFirst;
	}

	public boolean isLast() {
		return isLast;
	}

	public void setFirst() {
		isFirst = true;
	}

	public void setLast() {
		isLast = true;
	}

	public int getStartStep() {
		return stepStart;
	}

	public int getEndStep() {
		return stepEnd;
	}

	public void setStartStep(int ss) {
		stepStart = ss;
	}

	public void setEndStep(int se) {
		stepEnd = se;
	}

}
