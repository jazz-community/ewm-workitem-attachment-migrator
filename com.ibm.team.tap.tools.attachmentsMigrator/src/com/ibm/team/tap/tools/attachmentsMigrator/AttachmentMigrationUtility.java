/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2019,2024. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/

package com.ibm.team.tap.tools.attachmentsMigrator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;

import com.ibm.team.tap.tools.attachmentsMigrator.util.ProgressMonitor;
import com.ibm.team.tap.tools.attachmentsMigrator.internal.Attachment;
import com.ibm.team.tap.tools.attachmentsMigrator.internal.Repository;
import com.ibm.team.tap.tools.attachmentsMigrator.util.AttachmentUpdateCommand;
import com.ibm.team.tap.tools.attachmentsMigrator.util.Constants;
import com.ibm.team.tap.tools.attachmentsMigrator.util.LogUtils;

public class AttachmentMigrationUtility {
	
	private static enum CmdLineArg {
		COMMANDS("-c", "-commands"), //$NON-NLS-1$ //$NON-NLS-2$
		LOG("-l", "-log"), //$NON-NLS-1$ //$NON-NLS-2$
		HELP("-h", "-help", false), //$NON-NLS-1$ //$NON-NLS-2$
		USER_NAME("-u", "-username"), //$NON-NLS-1$ //$NON-NLS-2$
		PASSWORD("-pw", "-password"), //$NON-NLS-1$ //$NON-NLS-2$
		SERVER_URI("-uri", "-serverUri"), //$NON-NLS-1$ //$NON-NLS-2$
		UPDATE("-update", "-update", false), //$NON-NLS-1$ //$NON-NLS-2$
		REMOVE_DELETED_WI_LINK("-rmDelWILink", "-rmDelWILink", false), //$NON-NLS-1$ //$NON-NLS-2$
		REMOVE_MULTI_WI_LINK("-rmMultiWILinks", "-rmMultiWILinks", false), //$NON-NLS-1$ //$NON-NLS-2$
		ADD_WI_ID("-addWorkItemId", "-addWorkItemId"), //$NON-NLS-1$ //$NON-NLS-2$
		DELETE_ATTACHMENTS_WITH_NO_WI("-deleteAttachmentsNoWI", "-delAttachmentsNoWI", false), //$NON-NLS-1$ //$NON-NLS-2$
		PROJECT("-p", "-project"); //$NON-NLS-1$ //$NON-NLS-2$

		private final String fShortName;
		private final String fLongName;
		private final boolean fIsValueRequired;
		private String fValue = null;

		private CmdLineArg(String shortName, String longName) {
			this(shortName, longName, true);
		}

		private CmdLineArg(String shortName, String longName, boolean isValueRequired) {
			fShortName = shortName;
			fLongName = longName;
			fIsValueRequired = isValueRequired;
		};

		public boolean isEqual(String name) {
			return fShortName.equalsIgnoreCase(name) || fLongName.equalsIgnoreCase(name);
		}

		public boolean isValueRequired() {
			return fIsValueRequired;
		}

		public String getValue() {
			return fValue;
		}

		public void setValue(String value) {
			if (value != null) {
				fValue = value.trim();
			}
		}

		public String getName() {
			return fLongName;
		}
	}

	public static void main(String[] args) {
		
		TeamPlatform.startup();
		int systemExitCode = 0;
		try {
			String userName = null;
			String password = null;
			String serverUri = null;
			boolean update = false;
			boolean remDeletedWILink = false;
			boolean remMultiWILinks = false;
			String addWorkItemId = null;
			boolean delete = false;
			String projectItemId= null;
			//Level level = Level.INFO;

			//setUpLoggingLevels(level);
			List<CmdLineArg> cmdArgs = processArgs(args);
			if ((cmdArgs.contains(CmdLineArg.HELP)) || (cmdArgs.size() == 0)) {
				InputStream inputStream = AttachmentMigrationUtility.class.getResourceAsStream("readme.txt"); //$NON-NLS-1$
				BufferedReader readmeReader = new BufferedReader(new InputStreamReader(inputStream));
				String line = null;
				while ((line = readmeReader.readLine()) != null) {
					System.out.println(line);
				}
				System.exit(systemExitCode);
			}

			if (cmdArgs.contains(CmdLineArg.LOG)) {
				LogUtils.setLogFile(CmdLineArg.LOG.getValue());
			}

			if (cmdArgs.contains(CmdLineArg.USER_NAME)) {
				userName = CmdLineArg.USER_NAME.getValue();
			}

			if (cmdArgs.contains(CmdLineArg.PASSWORD)) {
				password = CmdLineArg.PASSWORD.getValue();
			}

			if (cmdArgs.contains(CmdLineArg.SERVER_URI)) {
				serverUri = CmdLineArg.SERVER_URI.getValue();
			}

			if (cmdArgs.contains(CmdLineArg.UPDATE)) {
				update = cmdArgs.contains(CmdLineArg.UPDATE);
			}

			if (cmdArgs.contains(CmdLineArg.REMOVE_DELETED_WI_LINK)) {
				remDeletedWILink = cmdArgs.contains(CmdLineArg.REMOVE_DELETED_WI_LINK);
			}

			if (cmdArgs.contains(CmdLineArg.REMOVE_MULTI_WI_LINK)) {
				remMultiWILinks = cmdArgs.contains(CmdLineArg.REMOVE_MULTI_WI_LINK);
			}

			if (cmdArgs.contains(CmdLineArg.ADD_WI_ID)) {
				addWorkItemId = CmdLineArg.ADD_WI_ID.getValue();
			}

			if (cmdArgs.contains(CmdLineArg.PROJECT)) {
				projectItemId = CmdLineArg.PROJECT.getValue();
			}
			
			if (cmdArgs.contains(CmdLineArg.DELETE_ATTACHMENTS_WITH_NO_WI)) {
				delete = cmdArgs.contains(CmdLineArg.DELETE_ATTACHMENTS_WITH_NO_WI);
				if (delete) {
					Scanner scanner = new Scanner(System.in);
					System.out.print("Are you sure you want to delete all the attachments which are not linked to any work items?\n" + "Enter Yes to continue\n"); //$NON-NLS-1$ //$NON-NLS-2$
					String confirm = scanner.nextLine();
					scanner.close();

					if (!confirm.equals("Yes")) { //$NON-NLS-1$
						System.out.print(CmdLineArg.DELETE_ATTACHMENTS_WITH_NO_WI.getName() + " requested but confirmation declined"); //$NON-NLS-1$
						System.exit(systemExitCode);
					}
				}
			}
			
			if (userName == null || password == null) {
				throw new IllegalArgumentException("Invalid user name or password."); //$NON-NLS-1$
			} else if (serverUri == null) {
				throw new IllegalArgumentException("Server uri required for the operation."); //$NON-NLS-1$
			}

			if (!cmdArgs.contains(CmdLineArg.COMMANDS)) {
				throw new IllegalArgumentException(
						CmdLineArg.COMMANDS.getName() + " requires one (or more) valid commands"); //$NON-NLS-1$
			}

			String[] commands = CmdLineArg.COMMANDS.getValue().split("\\,"); //$NON-NLS-1$

			List<String> commandsList = new ArrayList<String>();

			if (commands.length == 0) {
				throw new IllegalArgumentException(
						CmdLineArg.COMMANDS.getName() + " requires one (or more) valid commands"); //$NON-NLS-1$
			} else {
				for (String command : commands) {
					if (Constants.COMMAND_ANALYZE_AND_UPDATE_ATTACHMENTS.equals(command)) {
						commandsList.add(command);
					} else {
						throw new IllegalArgumentException(
								CmdLineArg.COMMANDS.getName() + " contains invalid command '" + command + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}

			printToConsoleAndLog("Requested commands are: " + commandsList + "."); //$NON-NLS-1$ //$NON-NLS-2$
			if (commandsList.contains(Constants.COMMAND_ANALYZE_AND_UPDATE_ATTACHMENTS)) {
				printToConsoleAndLog(Constants.COMMAND_ANALYZE_AND_UPDATE_ATTACHMENTS + " started."); //$NON-NLS-1$
				AttachmentUpdateCommand updateCommand = new AttachmentUpdateCommand(update, remDeletedWILink, remMultiWILinks, addWorkItemId, delete);
				analyzeAndUpdateAttachments(serverUri, userName, password, projectItemId, updateCommand);
				printToConsoleAndLog(Constants.COMMAND_ANALYZE_AND_UPDATE_ATTACHMENTS + " completed."); //$NON-NLS-1$
			}
		} catch (Throwable t) {
			LogUtils.logError(t.toString(), t);
			systemExitCode = 1;
		} finally {
			System.exit(systemExitCode);
		}
	}

	private static void printToConsoleAndLog(String message) {
		System.out.println(message);
		LogUtils.logInfo("-----------------------------------------------------"); //$NON-NLS-1$
		LogUtils.logInfo(message);
		LogUtils.logInfo("-----------------------------------------------------"); //$NON-NLS-1$
		
	}

	private static void analyzeAndUpdateAttachments(String serverUri, String userName, String password, String projectItemId, AttachmentUpdateCommand updateCommand) {
		try {
			IProgressMonitor monitor = new ProgressMonitor();
			ITeamRepository repository = Repository.login(monitor, serverUri, userName, password);
			Attachment.analyzeAndUpdateWIAttachmentsInRepository(repository, projectItemId, updateCommand, monitor);
			Attachment.printAnalysis();
		} catch (TeamRepositoryException e) {
			System.out.println("Exception: " + e.getMessage()); //$NON-NLS-1$
		} finally {
			TeamPlatform.shutdown();
		}
	}

	private static List<CmdLineArg> processArgs(String[] args) throws IllegalArgumentException {
		ArrayList<CmdLineArg> argList = new ArrayList<CmdLineArg>();
		for (String arg : args) {
			boolean found = false;
			for (CmdLineArg cmd : CmdLineArg.values()) {
				String[] nameVal = arg.split("\\="); //$NON-NLS-1$
				if (cmd.isEqual(nameVal[0])) {
					found = true;
					if (nameVal.length > 1 && !cmd.isValueRequired()) {
						parseCmdException(cmd.getName() + " / " + cmd.fShortName + " requires no value"); //$NON-NLS-1$ //$NON-NLS-2$
					} else if (nameVal.length > 1) {
						cmd.setValue(nameVal[1]);
					} else if (cmd.isValueRequired()) {
						parseCmdException(cmd.getName() + " / " + cmd.fShortName + " requires a value"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					argList.add(cmd);
					break;
				}
			}
			if (!found) {
				parseCmdException(arg + " is an invalid argument"); //$NON-NLS-1$
			}
		}
		return argList;
	}

	private static void parseCmdException(String message) {
		System.out.println("Invalid syntax: " + message); //$NON-NLS-1$
		throw new IllegalArgumentException(message);
	}

//	private static void setUpLoggingLevels(Level level) {
//		Configurator.initialize(new DefaultConfiguration());
//		Configurator.setRootLevel(level);
//
//		if (level != Level.DEBUG) {
//			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog"); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//	}
}