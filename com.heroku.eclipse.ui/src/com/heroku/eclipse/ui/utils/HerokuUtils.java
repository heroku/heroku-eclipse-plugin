package com.heroku.eclipse.ui.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;

/**
 * (Mostly) static container for various utility functionality
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuUtils {
	private static HashMap<String,Thread> logReaders = new HashMap<String, Thread>();

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
	 * 
	 * @param something
	 * @return true, if the string contains "something", false if not
	 */
	public static boolean isNotEmpty(String something) {
		return (something == null || something.trim().isEmpty()) ? false : true;
	}

	/**
	 * Returns an empty String when the given String is null, otherwise the
	 * passed in String.
	 * 
	 * @param something
	 * @return either an empty String or the passed in non empty String
	 */
	public static String ensureNotNull(String something) {
		return (something == null) ? "" : something; //$NON-NLS-1$
	}

	/**
	 * Returns the console used by the plugin, allowing to contribute messages to Eclipse's console view  
	 * @return the MessageConsole
	 */
	public static MessageConsole getConsole(String appName) {
		String consoleName = Activator.CONSOLE_NAME+" - "+appName; //$NON-NLS-1$
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (consoleName.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(consoleName, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		
		return myConsole;
	}
	
	public static Thread getLogViewerThread( String viewerName, MessageConsole console, App app ) {
		if ( logReaders.containsKey(viewerName) ) {
			return logReaders.get(viewerName);
		}
		else {
			Thread t = new Thread(viewerName){
				@Override
				public void run() {
//					byte[] buffer = new byte[1024];
//					int bytesRead;
//					try {
//						InputStream is = herokuService.getApplicationLogStream(app);
//						while ((bytesRead = is.read(buffer)) != -1) {
//							if ( out.isClosed() ) {
//								break;
//							}
//							out.write(buffer, 0, bytesRead);
//						}
//					}
//					catch (IOException e) {
//					// 	TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					catch (HerokuServiceException e) {
//					// 	TODO Auto-generated catch block
//						e.printStackTrace();
//					}
				}
			};
		
			t.setDaemon(true);
			logReaders.put(viewerName, t);
			return t;
		}
	}


	public static <R, A> R runOnDisplay(Viewer viewer, final A argument, final RunnableWithReturn<R, A> runnable) {
		return runOnDisplay(viewer.getControl(), argument, runnable);
	}

	public static <R, A> R runOnDisplay(Control control, final A argument, final RunnableWithReturn<R, A> runnable) {
		if (control.getDisplay() != null && !control.getDisplay().isDisposed()) {
			if (control.getDisplay().getThread() == Thread.currentThread()) {
				return runnable.run(argument);
			}
			else {
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
		if (!control.isDisposed() && control.getDisplay() != null && !control.getDisplay().isDisposed()) {
			if (async) {
				control.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						runnable.run(argument);
					}
				});
			}
			else {
				if (control.getDisplay().getThread() == Thread.currentThread()) {
					runnable.run(argument);
				}
				else {
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
}
