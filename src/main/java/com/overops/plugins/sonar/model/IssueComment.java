package com.overops.plugins.sonar.model;

public class IssueComment {
	private int line;
	private String message;
	private String comment;

	public static final String LINK_TEXT = "View event analysis in OverOps →";

	public IssueComment() {

	}

	public IssueComment(Event event) {
		this.setCommentFromLink(event.getLink());
		this.setLine(event.getLocation().original_line_number);
		this.setMessage(event.getMessage());
	}

	public void setLine(int line) {
		this.line = line;
	}
	public int getLine() {
		return line;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getMessage() {
		return message;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getComment() {
		return comment;
	}

	private void setCommentFromLink(String link) {
		// use markdown
		StringBuilder sb = new StringBuilder("[" + LINK_TEXT + "]");
		sb.append("(");
		sb.append(link);
		sb.append(")");

		this.comment = sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}

		IssueComment that = (IssueComment) obj;

		return this.getLine() == that.getLine() && this.getMessage().equals(that.getMessage());
	}

	@Override
	public int hashCode() {
		return message.hashCode() + line;
	}

	@Override
	public String toString() {
		return "IssueComment [comment=" + comment + ", line=" + line + ", message=" + message + "]";
	}

}