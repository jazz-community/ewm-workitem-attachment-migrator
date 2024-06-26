/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2019. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.team.tap.tools.attachmentsMigrator.util;

import org.eclipse.core.runtime.IProgressMonitor;

public class ProgressMonitor implements IProgressMonitor {

	public void beginTask(String name, int totalWork) {
		print(name);
	}

	public void done() {
	}

	public void internalWorked(double work) {
	}

	public boolean isCanceled() {
		return false;
	}

	public void setCanceled(boolean value) {
	}

	public void setTaskName(String name) {
		print(name);
	}

	public void subTask(String name) {
		print(name);
	}

	public void worked(int work) {
	}

	private void print(String name) {
		if (name != null && !"".equals(name)) //$NON-NLS-1$
			System.out.println(name);
	}
}
