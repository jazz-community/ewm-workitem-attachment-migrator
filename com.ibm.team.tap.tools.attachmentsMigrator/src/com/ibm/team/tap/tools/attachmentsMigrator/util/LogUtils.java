/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2019,2024. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.team.tap.tools.attachmentsMigrator.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

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

	private static Logger getLogger() {

		if (fLogger == null) {
			fLogger= LogManager.getLogger(AttachmentMigrationUtility.class);
		}

		return fLogger;
	}

	public static void setLogFile(String file) {

		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		builder.setConfigurationName("Default");
		LayoutComponentBuilder layout = builder.newLayout("PatternLayout")
				.addAttribute("pattern", "%d{dd MMM yyyy HH:mm:ss,SSSZ} [%t] %5p %c: %m%n");
		AppenderComponentBuilder appender = builder.newAppender("TRS Validator Utility log file appender", "File")
				.addAttribute("fileName", file)
				.add(layout);
		RootLoggerComponentBuilder rootLogger = builder.newRootLogger();
		builder.add(appender);
		rootLogger.add(builder.newAppenderRef("TRS Validator Utility log file appender"));
        builder.add(rootLogger);
		Configurator.reconfigure(builder.build());
			
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
