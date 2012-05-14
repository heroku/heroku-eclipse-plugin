package com.heroku.eclipse.ui.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;

/**
 * (Mostly) static container for various utility functionality
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuUtils {
	/**
	 * Displays an internal, "really" unexpected error
	 * 
	 * @param shell
	 * @param t
	 */
	public static void internalError(Shell shell, Throwable t) {
		Status status;
		String message;

		if (t == null) {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString("Heroku_Common_Error_InternalError")); //$NON-NLS-1$
			message = null;
		}
		else {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString("Heroku_Common_Error_InternalError"), t); //$NON-NLS-1$
			message = Messages.getString("Heroku_Common_Error_InternalError"); //$NON-NLS-1$
		}

		ErrorDialog.openError(shell, Messages.getString("Heroku_Common_Error_InternalError_Title"), message, status); //$NON-NLS-1$
	}

	/**
	 * Displays an error message related to an Heroku service
	 * 
	 * @param shell
	 * @param t
	 */
	public static void herokuError(Shell shell, Throwable t) {
		Status status;
		String message;

		if (t == null) {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString("Heroku_Common_Error_HerokuError")); //$NON-NLS-1$
			message = null;
		}
		else {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString("Heroku_Common_Error_HerokuError"), t); //$NON-NLS-1$
			message = Messages.getString("Heroku_Common_Error_HerokuError"); //$NON-NLS-1$
		}

		ErrorDialog.openError(shell, Messages.getString("Heroku_Common_Error_HerokuError_Title"), message, status); //$NON-NLS-1$
	}

	/**
	 * Displays a simple error message, typically used for errors originating
	 * from the users
	 * 
	 * @param shell
	 * @param title
	 * @param message
	 */
	public static void userError(Shell shell, String title, String message) {
		Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);

		ErrorDialog.openError(shell, title, null, status);
	}

}
