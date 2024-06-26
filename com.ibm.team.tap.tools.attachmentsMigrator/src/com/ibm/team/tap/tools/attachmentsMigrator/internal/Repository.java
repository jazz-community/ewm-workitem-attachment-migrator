/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2019. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.team.tap.tools.attachmentsMigrator.internal;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler;
import com.ibm.team.repository.client.ITeamRepository.ILoginHandler.ILoginInfo;
import com.ibm.team.repository.common.TeamRepositoryException;

/**
 * Repository related commands
 */
public class Repository {

	public static ITeamRepository login(IProgressMonitor monitor, String serverUri, String user, String password)
			throws TeamRepositoryException {
		if (!TeamPlatform.isStarted()) {
			TeamPlatform.startup();
		}

		ITeamRepository repository = TeamPlatform.getTeamRepositoryService().getTeamRepository(serverUri);
		repository.registerLoginHandler(new LoginHandler(user, password));
		monitor.subTask("Contacting " + repository.getRepositoryURI() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		repository.login(monitor);
		monitor.subTask("User " + user + " has logged in to " + repository.getRepositoryURI()); //$NON-NLS-1$ //$NON-NLS-2$
		return repository;
	}

	/**
	 * Internal login handler to perform the login to the repository
	 * 
	 */
	private static class LoginHandler implements ILoginHandler, ILoginInfo {

		private String fUserId;
		private String fPassword;

		private LoginHandler(String userId, String password) {
			fUserId = userId;
			fPassword = password;
		}

		public String getUserId() {
			return fUserId;
		}

		public String getPassword() {
			return fPassword;
		}

		public ILoginInfo challenge(ITeamRepository repository) {
			return this;
		}
	}
}
