package com.heroku.eclipse.ui.utils;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
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
	
	/**
	 * Simple test checking if the given string is either null or empty
	 * @param something
	 * @return true, if the string contains "something", false if not 
	 */
	public static boolean isNotEmpty( String something ) {
		return ( something == null || something.trim().isEmpty() ) ? false : true;
	}

	/**
	 * Returns an empty String when the given String is null, otherwise
	 * the passed in String. 
	 * @param something
	 * @return either an empty String or the passed in non empty String
	 */
	public static String ensureNotNull( String something ) {
		return (something == null) ? "" : something; //$NON-NLS-1$
	}
	
	public static <R,A> R runOnDisplay(Viewer viewer, final A argument, final RunnableWithReturn<R,A> runnable) {
		return runOnDisplay(viewer.getControl(), argument, runnable);
	}
	
	public static <R,A> R runOnDisplay(Control control, final A argument, final RunnableWithReturn<R,A> runnable) {
		if( control.getDisplay() != null && ! control.getDisplay().isDisposed() ) {
			if( control.getDisplay().getThread() == Thread.currentThread() ) {
				return runnable.run(argument);
			} else {
				final AtomicReference<R> ref = new AtomicReference<R>();
				control.getDisplay().syncExec(new Runnable() {
					
					@Override
					public void run() {
						ref.set(runnable.run(argument));
					}
				});
				return ref.get();
			}
		}
		return null;
	}
	
	public static <A> void runOnDisplay(boolean async, Viewer viewer, final A argument, final RunnableWithParameter<A> runnable) {
		runOnDisplay(async, viewer.getControl(), argument, runnable);
	}
	
	public static <A> void runOnDisplay(boolean async, Control control, final A argument, final RunnableWithParameter<A> runnable) {
		if( control.getDisplay() != null && ! control.getDisplay().isDisposed() ) {
			if( async ) {
				control.getDisplay().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						runnable.run(argument);
					}
				});
			} else {
				if( control.getDisplay().getThread() == Thread.currentThread() ) {
					runnable.run(argument);
				} else {
					control.getDisplay().syncExec(new Runnable() {
						
						@Override
						public void run() {
							runnable.run(argument);
						}
					});
				}				
			}
		}
	}
	
	public static String notNull(String value) {
		return value == null ? "" : value;
	}
}
