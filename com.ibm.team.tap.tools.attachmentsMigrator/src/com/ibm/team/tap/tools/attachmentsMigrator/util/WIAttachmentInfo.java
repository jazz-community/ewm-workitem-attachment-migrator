/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2019. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.team.tap.tools.attachmentsMigrator.util;

public class WIAttachmentInfo {
	private String fProjectName;
	private int fWorkItemId;
	private String fAttachmentName;
	private int fAttachmentId;
	private long fContentLength;

	public WIAttachmentInfo(String projectName, int workItemId, String attachmentName, int attachmentId, long contentLength) {
		fProjectName= projectName;
		fWorkItemId= workItemId;
		fAttachmentName= attachmentName;
		fAttachmentId= attachmentId;
		fContentLength= contentLength;
	}

	public String getAttachmentName() {
		return fAttachmentName;
	}

	public int getAttachmentId() {
		return fAttachmentId;
	}

	public int getWorkItemId() {
		return fWorkItemId;
	}

	public String getProjectName() {
		return fProjectName;
	}	

	public long getContentLength() {
		return fContentLength;
	}	
}
