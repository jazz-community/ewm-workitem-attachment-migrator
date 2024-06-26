/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2019. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.team.tap.tools.attachmentsMigrator.util;

public class AttachmentUpdateCommand {
	private boolean fUpdate;
	private boolean fRemoveDeletedLinks;
	private boolean fRemoveMultipleWILinks;
	private String fAddWorkItemId;
	private boolean fDelete;
	
	public AttachmentUpdateCommand(boolean update, boolean removeDeletedLinks, boolean removeMultipleWILinks, String workItemId, boolean delete) {
		fUpdate= update;
		fRemoveDeletedLinks= removeDeletedLinks;
		fRemoveMultipleWILinks= removeMultipleWILinks;
		fAddWorkItemId = workItemId;
		fDelete = delete;
	}

	public boolean isUpdate() {
		return fUpdate;
	}

	public boolean isRemoveDeletedLinks() {
		return fRemoveDeletedLinks;
	}

	public boolean isRemoveMultipleWILinks() {
		return fRemoveMultipleWILinks;
	}

	public String getWorkItemId() {
		return fAddWorkItemId;
	}

	public boolean isDelete() {
		return fDelete;
	}	
}
