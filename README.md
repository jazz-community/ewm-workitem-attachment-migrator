# ewm-workitem-attachment-migrator
Utility for the IBM EWM application for cleaning up and fixing work item attachments in the repository

Attachments Migrator 1.1
================================
The Attachments Migrator Utility is a stand-alone Java application used to analyze attachments in a repository and allow to change the access context of the attachments to that of the work item it is linked to. It supports the following commands:
		
Command: analyzeAndUpdateAttachments
Description: Fetch all attachments in a repository and update based on arguments. List all the attachments:
- which are linked to multiple work items, option to remove the multiple links.
- linked to a work item but are project owned, option to update the context to that of the work item.
- not linked to any work item, option to link to the specified work item.
- have any attachments linked to a deleted work item, option to remove the link to the deleted work item.

Argument Reference
==================

Note: Argument values containing whitespace must be enclosed in double quote characters.

-h, -help
    Prints this help message.

-uri, -serverUri=<URL>
    The fully qualified URL of IBM Engineering Workflow Management in the following format:
    https://<server>:<port>/<context root>
    Note: The fully qualified URL is case sensitive.
	
-u, -userName=<userName>
	The username for a valid user for the server.

-pw, -password=<password>
	The password for the username for the server.

-update
	Update the access context of attachments that are project owned with the access context of the work item it is associated with, update the project of the attachment if different than the work item.

-rmDelWILink
	Remove link to deleted work items from the attachments.

-rmMultiWILinks
	Remove multiple work links from the attachment.

-addWorkItemId
	The work item id to associate attachments that are not associated with any work item.

-deleteAttachmentsNoWI, delAttachmentsNoWI
	Deletes the attachments which has no work item associated with it.

-l, -log=<file>
    [Optional] Logs information to the specified log file (relative file name or absolute file path).
    Note: The log file is rotated (deleted) on every execution of the tool.
 
-p, -project=<project item id>
	[Optional] Analysis is done only for the specified project

Example usage:
 <java.home>/bin/java.exe -jar AttachmentMigrationUtility.jar -uri=<https://hostname:9443/ccm> -u=<user> -pw=<password> -c=analyzeAndUpdateAttachments -l=<c:/temp/migration.log>
 
