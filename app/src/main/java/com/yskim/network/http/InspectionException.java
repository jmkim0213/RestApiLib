package com.yskim.network.http;

public class InspectionException extends Exception {
	private int mInspectionCode;
	private String mInspectionMessage;
	
	private long mInspectionStartDate;
	private long mInspectionEndDate;
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2710596075392724371L;

	public InspectionException() { }

	public InspectionException(String detailMessage) {
		super(detailMessage);

	}

	public InspectionException(Throwable throwable) {
		super(throwable);
	}

	public InspectionException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
	
	public int getInspectionCode() {
		return mInspectionCode;
	}
	
	public void setInspectionCode(int code) {
		mInspectionCode = code;
	}
	
	public String getInspectionMessage() {
		return mInspectionMessage;
	}

	public void setInspectionMessage(String message) {
		mInspectionMessage = message;
	}
	
	public long getInspectionStartDate() {
		return mInspectionStartDate;
	}
	
	public void setInspectionStartDate(long startDate) {
		mInspectionStartDate = startDate;
	}
	
	public long getInspectionEndDate() {
		return mInspectionEndDate;
	}
	
	public void setInspectionEndDate(long endDate) {
		mInspectionEndDate = endDate;
	}
}
