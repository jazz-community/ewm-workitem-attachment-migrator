/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2019,2024. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.team.tap.tools.attachmentsMigrator.util;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.ibm.team.tap.tools.attachmentsMigrator.AttachmentMigrationUtility;

/**
 * <p>
 * Apache Log4j logger utilities.
 * </p>
 * 
 */
public class LogUtils {

	private static Logger fLogger= null;
	private static RollingFileAppender fRollingFileAppender;
	private static Logger fRootLogger;

	// 50 MB
	private static long MAX_FILE_SIZE= 50000000;
	private static int MAX_BACKUP_INDEX= 10;
	private static int BUFFER_SIZE= 4000;

	private static Logger getLogger() {

		if (fLogger == null) {
			fLogger= LogManager.getLogger(AttachmentMigrationUtility.class);
		}

		return fLogger;
	}

  public static void setLogFile(String file) throws IOException {

        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final Layout layout = PatternLayout.createDefaultLayout(config);
        Appender appender = FileAppender.createAppender(file, "false", "false", "File", "true",
            "false", "false", "4000", layout, null, "false", null, config);
        appender.start();
        config.addAppender(appender);
        AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
        AppenderRef[] refs = new AppenderRef[] {ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger("false", Level.DEBUG, "com.ibm.team.tap.tools.attachmentsMigrator.AttachmentMigrationUtility",
            "true", refs, null, config, null );
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("com.ibm.team.tap.tools.attachmentsMigrator.AttachmentMigrationUtility", loggerConfig);
        ctx.updateLoggers();
	}

	public static void logDebug(String message) {
		getLogger().debug(message);
	}

	public static void logDebug(String message, Throwable throwable) {
		getLogger().debug(message, throwable);
	}

	public static void logTrace(String message) {
		getLogger().trace(message);
	}

	public static void logTrace(String message, Throwable throwable) {
		getLogger().trace(message, throwable);
	}

	public static void logInfo(String message) {
		getLogger().info(message);
	}

	public static void logInfo(String message, Throwable throwable) {
		getLogger().info(message, throwable);
	}

	public static void logWarning(String message) {
		getLogger().warn(message);
	}

	public static void logWarning(String message, Throwable throwable) {
		getLogger().warn(message, throwable);
	}

	public static void logError(String message) {
		getLogger().error(message);
	}

	public static void logError(String message, Throwable throwable) {
		getLogger().error(message, throwable);
	}

	public static void logFatal(String message) {
		getLogger().fatal(message);
	}

	public static void logFatal(String message, Throwable throwable) {
		getLogger().fatal(message, throwable);
	}
}
