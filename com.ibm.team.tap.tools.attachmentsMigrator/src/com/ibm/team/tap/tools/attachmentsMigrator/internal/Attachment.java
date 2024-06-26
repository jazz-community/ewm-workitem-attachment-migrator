/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2019. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.team.tap.tools.attachmentsMigrator.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.team.links.client.ILinkManager;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.ILink;
import com.ibm.team.links.common.ILinkCollection;
import com.ibm.team.links.common.ILinkQueryPage;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.process.internal.common.ProcessPackage;

import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.ItemNotFoundException;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.repository.common.query.IItemQuery;
import com.ibm.team.tap.tools.attachmentsMigrator.util.AttachmentUpdateCommand;
import com.ibm.team.tap.tools.attachmentsMigrator.util.LogUtils;
import com.ibm.team.tap.tools.attachmentsMigrator.util.WIAttachmentInfo;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.QueryIterator;
import com.ibm.team.workitem.common.internal.model.query.BaseAttachmentQueryModel.AttachmentQueryModel;
import com.ibm.team.workitem.common.model.IAttachment;
import com.ibm.team.workitem.common.model.IAttachmentHandle;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;

public class Attachment {
	private static List<WIAttachmentInfo> fWIAttachmentsAccessContextMismatch = new ArrayList<WIAttachmentInfo>();
	private static List<WIAttachmentInfo> fWIAttachmentsProjectAreaMismatch = new ArrayList<WIAttachmentInfo>();
	private static List<WIAttachmentInfo> fWIAttachmentsMultipleWIs = new ArrayList<WIAttachmentInfo>();
	private static int fWIAttachmentsMultipleWIsCount= 0;
	private static List<WIAttachmentInfo> fWIAttachmentsNoWI = new ArrayList<WIAttachmentInfo>();
	private static List<WIAttachmentInfo> fWIAttachmentsWIDeleted = new ArrayList<WIAttachmentInfo>();
	private static ItemProfile<IAttachment> SMALL_PROFILE_WITH_CONTENT= IAttachment.SMALL_PROFILE.createExtension(Arrays.asList(new String[] { IAttachment.CONTENT_PROPERTY }));

	public static String[] PROCESS_PROVIDER_PROFILE = new String[] {
			ProcessPackage.eINSTANCE.getProjectArea_InternalProcessProvider().getName() };
	public static ItemProfile<IProjectArea> PROJECT_CHECK_PROFILE = ItemProfile.PROJECT_AREA_DEFAULT
			.createExtension(PROCESS_PROVIDER_PROFILE);

	public static void analyzeAndUpdateWIAttachmentsInRepository(ITeamRepository repository, String projectItemId, AttachmentUpdateCommand updateCommand, IProgressMonitor monitor)
			throws TeamRepositoryException {
		final IAuditableClient auditableClient = getAuditableClient(repository);
		final ILinkManager linkManager = getLinkManager(repository);

		boolean update = false;
		boolean removeDeletedLinks = false;
		boolean removeMultipleWILinks = false;
		IWorkItem workItemToAddLink = null;
		boolean delete = false;

		if (updateCommand != null) {
			update = updateCommand.isUpdate();
			removeDeletedLinks = updateCommand.isRemoveDeletedLinks();
			removeMultipleWILinks = updateCommand.isRemoveMultipleWILinks();
			String workItemIdString = updateCommand.getWorkItemId();
			if (workItemIdString != null) {
				int workItemId = Integer.parseInt(workItemIdString);
				workItemToAddLink = getIWorkItemClient(repository).findWorkItemById(workItemId, IWorkItem.FULL_PROFILE, monitor);
				if (workItemToAddLink == null) {
					LogUtils.logError("Cannot find the specified work item " + workItemId); //$NON-NLS-1$
					return;
				}
			}
			delete = updateCommand.isDelete();
		}

		AttachmentQueryModel model = AttachmentQueryModel.ROOT;
		IItemQuery query = IItemQuery.FACTORY.newInstance(model);
		if (projectItemId != null && projectItemId.length() != 0) {
			UUID itemId= UUID.valueOf(projectItemId);
			IProjectAreaHandle projectAreaHandle= (IProjectAreaHandle) IProjectArea.ITEM_TYPE.createItemHandle(itemId, null);
			query.filter(model.projectArea()._eq(projectAreaHandle));
		}

		QueryIterator<IAttachmentHandle> iter = auditableClient.getItemQueryIterator(query, null, null, null);
		try {
			while (iter.hasNext(monitor)) {
				IAttachment attachment = auditableClient.resolveAuditable(iter.next(monitor), SMALL_PROFILE_WITH_CONTENT,
						monitor);
				IReference attachmentReference = IReferenceFactory.INSTANCE.createReferenceToItem(attachment, attachment.getName());

				IProjectArea projectArea = auditableClient.resolveAuditable(attachment.getProjectArea(),
						PROJECT_CHECK_PROFILE, monitor);
				String attachmentProjectName= (projectArea != null) ? projectArea.getName() : "No Project Information"; //$NON-NLS-1$

				ILinkQueryPage linkQueryResult = linkManager.findLinks(WorkItemLinkTypes.ATTACHMENT, attachmentReference, monitor);

				/* For each link */
				ILinkCollection allLinksFromHereOn = linkQueryResult.getAllLinksFromHereOn();
				if (allLinksFromHereOn.isEmpty()) {
					if (workItemToAddLink != null) {
						ILink link = saveLink(linkManager, workItemToAddLink, attachmentReference, monitor);
						allLinksFromHereOn.addAll((Collection<? extends ILink>) new ArrayList<ILink>(Arrays.asList(link)));
						LogUtils.logInfo("Linking attachment " + attachment.getId() + " - " //$NON-NLS-1$ //$NON-NLS-2$
								+ attachment.getName() + " to work item " + workItemToAddLink.getId() + "."); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						if (delete) {
							deleteWorkItemAttachment(repository, attachment, monitor);
						} else {
							WIAttachmentInfo wiAttachmentInfo = new WIAttachmentInfo(attachmentProjectName, 0,
									attachment.getName(), attachment.getId(), attachment.getContent().getRawLength());
							fWIAttachmentsNoWI.add(wiAttachmentInfo);
						}
					}
				}

				boolean foundMultipleWI = false;
				for (ILink link : allLinksFromHereOn) {
					try {
						IReference workItemRef = link.getSourceRef();
						if (!workItemRef.isItemReference()) {
							continue;
						}

						if ((allLinksFromHereOn.size() > 1) && removeMultipleWILinks && foundMultipleWI) {
							deleteLink(linkManager, link, monitor);
							continue;
						}

						IItemHandle referencedItem = ((IItemReference) workItemRef).getReferencedItem();
						if (referencedItem instanceof IWorkItemHandle) {
							IWorkItem workItem = auditableClient.resolveAuditable((IWorkItemHandle) referencedItem,
									IWorkItem.DEFAULT_PROFILE, monitor);							
							if (allLinksFromHereOn.size() > 1) {
								foundMultipleWI = true;
								if (!removeMultipleWILinks) {
									WIAttachmentInfo wiAttachmentInfo = new WIAttachmentInfo(attachmentProjectName, workItem.getId(),
											attachment.getName(), attachment.getId(), attachment.getContent().getRawLength());
									fWIAttachmentsMultipleWIs.add(wiAttachmentInfo);
								} else {
									LogUtils.logInfo("Multiple work item links to attachment " + attachment.getId() + " - " //$NON-NLS-1$ //$NON-NLS-2$
											+ attachment.getName() + " ... retaining link to work item " + workItem.getId() + "."); //$NON-NLS-1$ //$NON-NLS-2$
									checkAndUpdateWorkItemAttachment(repository, workItem, attachment, attachmentProjectName, update, monitor);									
								}
							} else {
								checkAndUpdateWorkItemAttachment(repository, workItem, attachment, attachmentProjectName, update, monitor);
							}
						}
					} catch (ItemNotFoundException e) {
						if (removeDeletedLinks) {
							deleteLink(linkManager, link, monitor);
						} else {
							WIAttachmentInfo wiAttachmentInfo = new WIAttachmentInfo(attachmentProjectName, 0,
									attachment.getName(), attachment.getId(), attachment.getContent().getRawLength());
							fWIAttachmentsWIDeleted.add(wiAttachmentInfo);							
						}
					}
				}
				if (foundMultipleWI) {
					fWIAttachmentsMultipleWIsCount++;
				}
			}
		} finally {
			iter.close();
		}
	}

	private static void checkAndUpdateWorkItemAttachment(ITeamRepository repository, IWorkItem workItem, IAttachment attachment,
			String projectName, boolean update, IProgressMonitor monitor) throws TeamRepositoryException {
		boolean saveAttachment = false;
		IWorkItemClient workItemClient = getIWorkItemClient(repository);
		
		if (update) {
			attachment = (IAttachment) attachment.getWorkingCopy();			
		}

		if (!isSameAccessContext(workItem, attachment)) {
			if (update) {
				LogUtils.logInfo("Updating context for attachment " + attachment.getId() + " - " + attachment.getName() + " to context of work item " + workItem.getId() + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				saveAttachment = updateAttachmentContext(attachment, workItem.getContextId());
			} else {
				WIAttachmentInfo wiAttachmentInfo = new WIAttachmentInfo(projectName, workItem.getId(),
						attachment.getName(), attachment.getId(), attachment.getContent().getRawLength());
				fWIAttachmentsAccessContextMismatch.add(wiAttachmentInfo);				
			}
		}

		final IAuditableClient auditableClient = getAuditableClient(repository);		
		IProjectArea attachmentProjectArea = auditableClient.resolveAuditable(attachment.getProjectArea(), PROJECT_CHECK_PROFILE, monitor);
		IProjectArea wiProjectArea = auditableClient.resolveAuditable(workItem.getProjectArea(), PROJECT_CHECK_PROFILE, monitor);

		if (wiProjectArea != null && attachmentProjectArea != null && !wiProjectArea.getItemId().equals(attachmentProjectArea.getItemId())) {
			if (update) {
				LogUtils.logInfo("Updating project area for attachment " + attachment.getId() + " - " + attachment.getName() + " from project area - " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						attachmentProjectArea.getName() + " to the project area of work item " + workItem.getId() + " - " + //$NON-NLS-1$ //$NON-NLS-2$
						wiProjectArea.getName() + "."); //$NON-NLS-1$
				saveAttachment = updateAttachmentProject(attachment, wiProjectArea);
			} else {
				WIAttachmentInfo wiAttachmentInfo = new WIAttachmentInfo(attachmentProjectArea.getName(), workItem.getId(),
						attachment.getName(), attachment.getId(), attachment.getContent().getRawLength());
				fWIAttachmentsProjectAreaMismatch.add(wiAttachmentInfo);				
			}
		} else if (wiProjectArea != null && attachmentProjectArea == null) {
			if (update) {
				LogUtils.logInfo("Updating project area for attachment " + attachment.getId() + " - " + attachment.getName() +  //$NON-NLS-1$ //$NON-NLS-2$
						" to the project area of work item " + workItem.getId() + " - " + //$NON-NLS-1$ //$NON-NLS-2$
						wiProjectArea.getName() + "."); //$NON-NLS-1$ 
				saveAttachment = updateAttachmentProject(attachment, wiProjectArea);
			} else {
				WIAttachmentInfo wiAttachmentInfo = new WIAttachmentInfo(null, workItem.getId(),
						attachment.getName(), attachment.getId(), attachment.getContent().getRawLength());
				fWIAttachmentsProjectAreaMismatch.add(wiAttachmentInfo);				
			}
		}

		if (saveAttachment) {
			workItemClient.saveAttachment(attachment, new NullProgressMonitor());
		}
	}

	private static void deleteWorkItemAttachment(ITeamRepository repository, IAttachment attachment, IProgressMonitor monitor) throws TeamRepositoryException {
		IWorkItemClient workItemClient = getIWorkItemClient(repository);
		LogUtils.logInfo("Deleting attachment " + attachment.getId() + " - " + attachment.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		workItemClient.deleteAttachment(attachment, monitor);
	}
	
	private static ILink saveLink(ILinkManager linkManager, IWorkItem workItem, IReference reference, IProgressMonitor monitor) throws TeamRepositoryException {
		// Attachment links are ignored in Work Item History
		// See, com.ibm.team.workitem.service.internal.WorkItemHistoryService.isValidLinkTypeForHistory(ILinkType)
		IReference workItemReference = IReferenceFactory.INSTANCE.createReferenceToItem(workItem, workItem.getHTMLSummary().getPlainText());
		ILink link= linkManager.createLink(WorkItemLinkTypes.ATTACHMENT, workItemReference, reference);
		linkManager.saveLink(link, monitor);
		return link;
	}

	private static void deleteLink(ILinkManager linkManager, ILink link, IProgressMonitor monitor) throws TeamRepositoryException {
		// Attachment links are ignored in Work Item History
		// See, com.ibm.team.workitem.service.internal.WorkItemHistoryService.isValidLinkTypeForHistory(ILinkType)
		linkManager.deleteLink(link, monitor);
		LogUtils.logInfo("Deleting link: sourceRef " + link.getSourceRef().getComment() + " targetRef: " + link.getTargetRef().getComment() + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static boolean isSameAccessContext(IWorkItem workItem, IAttachment attachment) {
		UUID workItemContextId = workItem.getContextId();
		UUID attachmentContextId = attachment.getContextId();

		return workItemContextId.equals(attachmentContextId);	
	}
	
	private static boolean updateAttachmentProject(IAttachment attachment, IProjectArea projectArea) throws TeamRepositoryException {
		if (projectArea != null) {
			((com.ibm.team.workitem.common.internal.model.Attachment)attachment).setProjectArea(projectArea);
			return true;
		}
		return false;
	}
	
	private static boolean updateAttachmentContext(IAttachment attachment, UUID newContext) throws TeamRepositoryException {
		if (newContext != null) {
			attachment.setContextId(newContext);
			return true;
		}
		return false;
	}
	
	private static IAuditableClient getAuditableClient(ITeamRepository repository) {
		return (IAuditableClient) repository.getClientLibrary(IAuditableClient.class);
	}

	private static ILinkManager getLinkManager(ITeamRepository repository) {
		return (ILinkManager) repository.getClientLibrary(ILinkManager.class);
	}

	private static IWorkItemClient getIWorkItemClient(ITeamRepository repository) {
		return (IWorkItemClient) repository.getClientLibrary(IWorkItemClient.class);
	}
	
	public static void printAnalysis() {
		if (!fWIAttachmentsAccessContextMismatch.isEmpty()) {
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			LogUtils.logInfo("Attachments with out the same access context as the work item: " + fWIAttachmentsAccessContextMismatch.size()); //$NON-NLS-1$
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			for (WIAttachmentInfo wiAttachmentInfo : fWIAttachmentsAccessContextMismatch) {
				LogUtils.logInfo("Attachment " + wiAttachmentInfo.getAttachmentId() + " - " //$NON-NLS-1$ //$NON-NLS-2$
						+ wiAttachmentInfo.getAttachmentName() + " linked to work item " + wiAttachmentInfo.getWorkItemId() //$NON-NLS-1$
						+ " in project " + wiAttachmentInfo.getProjectName()); //$NON-NLS-1$
			}
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
		}

		if (!fWIAttachmentsMultipleWIs.isEmpty()) {
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			LogUtils.logInfo("Attachments linked to multiple work items: " + fWIAttachmentsMultipleWIsCount); //$NON-NLS-1$
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			for (WIAttachmentInfo wiAttachmentInfo : fWIAttachmentsMultipleWIs) {
				LogUtils.logInfo("Attachment " + wiAttachmentInfo.getAttachmentId() + " - " //$NON-NLS-1$ //$NON-NLS-2$
						+ wiAttachmentInfo.getAttachmentName() + " linked to work item " + wiAttachmentInfo.getWorkItemId() //$NON-NLS-1$
						+ " in project " + wiAttachmentInfo.getProjectName()); //$NON-NLS-1$
			}
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
		}

		if (!fWIAttachmentsProjectAreaMismatch.isEmpty()) {		
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			LogUtils.logInfo("Attachments in different project than the work item: " + fWIAttachmentsProjectAreaMismatch.size()); //$NON-NLS-1$
			LogUtils.logInfo("----------------------------------------------------------------------"); //$NON-NLS-1$
			for (WIAttachmentInfo wiAttachmentInfo : fWIAttachmentsProjectAreaMismatch) {
				LogUtils.logInfo("Attachment " + wiAttachmentInfo.getAttachmentId() + " - " //$NON-NLS-1$ //$NON-NLS-2$
						+ wiAttachmentInfo.getAttachmentName() + " linked to work item " + wiAttachmentInfo.getWorkItemId() //$NON-NLS-1$
						+ " in project " + wiAttachmentInfo.getProjectName()); //$NON-NLS-1$
			}
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
		}

		long totalBytes= 0;
		if (!fWIAttachmentsNoWI.isEmpty()) {
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			LogUtils.logInfo("Attachments linked to no work items: " + fWIAttachmentsNoWI.size()); //$NON-NLS-1$
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			for (WIAttachmentInfo wiAttachmentInfo : fWIAttachmentsNoWI) {
				LogUtils.logInfo("Attachment " + wiAttachmentInfo.getAttachmentId() + " - " //$NON-NLS-1$ //$NON-NLS-2$
						+ wiAttachmentInfo.getAttachmentName() + " in project " + wiAttachmentInfo.getProjectName() + " with size " + processRawByteCount(wiAttachmentInfo.getContentLength())); //$NON-NLS-1$ //$NON-NLS-2$
				totalBytes+= wiAttachmentInfo.getContentLength();
			}
			LogUtils.logInfo("Total attachments size: " + processRawByteCount(totalBytes)); //$NON-NLS-1$
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
		}

		if (!fWIAttachmentsWIDeleted.isEmpty()) {		
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			LogUtils.logInfo("Attachments linked to deleted work items: " + fWIAttachmentsWIDeleted.size()); //$NON-NLS-1$
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
			totalBytes= 0;
			for (WIAttachmentInfo wiAttachmentInfo : fWIAttachmentsWIDeleted) {
				LogUtils.logInfo("Attachment " + wiAttachmentInfo.getAttachmentId() + " - " //$NON-NLS-1$ //$NON-NLS-2$
						+ wiAttachmentInfo.getAttachmentName() + " in project " + wiAttachmentInfo.getProjectName() + " with size " + processRawByteCount(wiAttachmentInfo.getContentLength())); //$NON-NLS-1$ //$NON-NLS-2$
				totalBytes+= wiAttachmentInfo.getContentLength();
			}
			LogUtils.logInfo("Total attachments size: " + processRawByteCount(totalBytes)); //$NON-NLS-1$
			LogUtils.logInfo("-----------------------------------------------------------------------"); //$NON-NLS-1$
		}
	}

	public static String processRawByteCount(long bytes) {
	    int unit= 1000;
	    if (bytes < unit) return bytes + " B"; //$NON-NLS-1$
	    int exponent= (int) (Math.log(bytes) / Math.log(unit));
	    String prefix= "KMGTPE".charAt(exponent - 1) + ""; //$NON-NLS-1$ //$NON-NLS-2$ 
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exponent), prefix); //$NON-NLS-1$
	}	
}
