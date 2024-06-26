/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2019. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.team.tap.tools.attachmentsMigrator.internal;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.internal.util.CollectionUtils;

public class Project {

	@SuppressWarnings("unchecked")
	public static List<IProjectArea> findAllProjects(ITeamRepository repository, IProgressMonitor monitor)
			throws TeamRepositoryException {
		IProcessItemService service = (IProcessItemService) repository.getClientLibrary(IProcessItemService.class);
		return CollectionUtils.removeNulls(service.findAllProjectAreas(IProcessClientService.ALL_PROPERTIES, monitor));
	}

}
