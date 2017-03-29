package org.jga.eclipse.plugin.navigatormarker.plugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;

import org.osgi.framework.BundleContext;

public class NavigatorMarkerPlugin extends AbstractUIPlugin {

	public static final String ID = "org.jga.navmark.plugin";
	
	public static final String ERROR_MESSAGE = "Unexpected Exception";
	
	private static NavigatorMarkerPlugin plugin;

	public NavigatorMarkerPlugin() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		NavigatorMarkerPlugin.plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		NavigatorMarkerPlugin.plugin = null;
		super.stop(context);
	}

	public static NavigatorMarkerPlugin getDefault() {
		return NavigatorMarkerPlugin.plugin;
	}

	public static void logInfo(String partId, String message) {
		NavigatorMarkerPlugin.log(partId, IStatus.INFO, IStatus.OK, message, null);
	}

	public static void logError(String partId, Throwable exception) {
		NavigatorMarkerPlugin.logError(partId, NavigatorMarkerPlugin.ERROR_MESSAGE, exception);
	}

	public static void logError(String partId, String message, Throwable exception) {
		NavigatorMarkerPlugin.log(partId, IStatus.ERROR, IStatus.OK, message, exception);
	}

	public static void log(String partId, int severity, int code, String message, Throwable exception) {
		StatusManager.getManager().handle(new Status(severity, partId, code, message, exception));
	}

}
