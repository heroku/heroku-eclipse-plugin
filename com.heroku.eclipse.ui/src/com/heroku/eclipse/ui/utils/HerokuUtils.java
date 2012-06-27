package com.heroku.eclipse.ui.utils;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.heroku.api.App;
import com.heroku.api.Proc;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.preferences.HerokuPreferencePage;

/**
 * (Mostly) static container for various utility functionality
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuUtils {
	private static HashMap<String, Thread> logReaders = new HashMap<String, Thread>();

	private static class ErrorData {
		final Shell shell;
		final IStatus status;
		final String title;
		final String message;

		public ErrorData(Shell shell, IStatus status, String title, String message) {
			this.shell = shell;
			this.status = status;
			this.title = title;
			this.message = message;
		}
	}

	private static class ErrorRunnable implements RunnableWithParameter<ErrorData> {
		@Override
		public void run(ErrorData e) {
			ErrorDialog.openError(e.shell, e.title, e.message, e.status);
		}
	}

	/**
	 * Displays an internal, "really" unexpected error
	 * 
	 * @param shell
	 * @param t
	 */
	public static void internalError(final Shell shell, Throwable t) {
		showError(shell, t, "Heroku_Common_Error_InternalError"); //$NON-NLS-1$
	}

	/**
	 * Displays an error message related to an Heroku service
	 * 
	 * @param shell
	 * @param t
	 */
	public static void herokuError(Shell shell, Throwable t) {
		showError(shell, t, "Heroku_Common_Error_HerokuError"); //$NON-NLS-1$
	}

	private static void showError(Shell shell, Throwable t, String messageKey) {
		Status status;
		String message;

		if (t == null) {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString(messageKey));
			message = null;
		}
		else {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString(messageKey), t);
			message = Messages.getString(messageKey);
		}

		runOnDisplay(true, shell, new ErrorData(shell, status, Messages.getString(messageKey + "_Title"), message), new ErrorRunnable()); //$NON-NLS-1$
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

		runOnDisplay(true, shell, new ErrorData(shell, status, title, null), new ErrorRunnable());
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
	 * Returns the console used by the plugin, allowing to contribute messages
	 * to Eclipse's console view
	 * 
	 * @param appName
	 * @return the MessageConsole
	 */
	public static MessageConsole getConsole(String appName) {
		String consoleName = Activator.CONSOLE_NAME + " - " + appName; //$NON-NLS-1$
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

	public static Thread getLogViewerThread(String viewerName, MessageConsole console, App app) {
		if (logReaders.containsKey(viewerName)) {
			return logReaders.get(viewerName);
		}
		else {
			Thread t = new Thread(viewerName) {
				@Override
				public void run() {
					// byte[] buffer = new byte[1024];
					// int bytesRead;
					// try {
					// InputStream is =
					// herokuService.getApplicationLogStream(app);
					// while ((bytesRead = is.read(buffer)) != -1) {
					// if ( out.isClosed() ) {
					// break;
					// }
					// out.write(buffer, 0, bytesRead);
					// }
					// }
					// catch (IOException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// }
					// catch (HerokuServiceException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// }
				}
			};

			t.setDaemon(true);
			logReaders.put(viewerName, t);
			return t;
		}
	}

	/**
	 * Runs a Runnable synchronously in the display thread determined by the
	 * given Viewer
	 * 
	 * @param viewer
	 * @param argument
	 * @param runnable
	 * @return
	 */
	public static <R, A> R runOnDisplay(Viewer viewer, final A argument, final RunnableWithReturn<R, A> runnable) {
		return runOnDisplay(viewer.getControl(), argument, runnable);
	}

	/**
	 * Runs a Runnable synchronously in the display thread determined by the
	 * given Control
	 * 
	 * @param control
	 * @param argument
	 * @param runnable
	 * @return
	 */
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

	/**
	 * Runs a Runnable either asynchronously or synchronously in the display
	 * thread determined by the given Viewer
	 * 
	 * @param async
	 * @param viewer
	 * @param argument
	 * @param runnable
	 */
	public static <A> void runOnDisplay(boolean async, Viewer viewer, final A argument, final RunnableWithParameter<A> runnable) {
		runOnDisplay(async, viewer.getControl(), argument, runnable);
	}

	/**
	 * Runs a Runnable either asynchronously or synchronously in the display
	 * thread determined by the given Control
	 * 
	 * @param async
	 * @param control
	 * @param argument
	 * @param runnable
	 */
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
	
	/**
	 * Retrieves the process name of a process without the process counter.
	 * So for a Proc.getProcess() returning foo.1, you will get only foo   
	 * @param p
	 * @return the process name stripped from the process counter
	 */
	public static String getProcessName( Proc p ) {
		String[] pn = p.getProcess().split("\\."); //$NON-NLS-1$
		
		if ( pn.length > 1 ) {
			return pn[pn.length-2];
		}
		else {
			return pn[0];
		}
	}
	
	/**
	 * Delivers a somewhat unique id for the given process, consisting of its
	 * app name and its process name.
	 * @param p
	 * @return the crafted process id
	 */
	public static String getProcessId( Proc p ) {
		return p.getAppName()+"<>"+getProcessName(p); //$NON-NLS-1$
	}
	
	/**
	 * Verifies that the preferences are valid and if not, asks the user if he/she
	 * wants to setup the preferences, otherwise return null
	 * 
	 * @param service 
	 * @param parent
	 * @return true, if the prefs are OK, false if not
	 */
	public static boolean verifyPreferences(HerokuServices service, Shell parent) {
		boolean isOk = true;
		
		// ensure that we have valid prefs
		try {
			while ( ! service.isReady() ) {
				if ( MessageDialog.openQuestion(parent, Messages.getString("Heroku_Common_Error_HerokuPrefsMissing_Title"), Messages.getString("Heroku_Common_Error_HerokuPrefsMissing_Question")) ) { //$NON-NLS-1$ //$NON-NLS-2$
					PreferenceDialog p = PreferencesUtil.createPreferenceDialogOn(null, HerokuPreferencePage.ID, null, null);
					p.open();
				}
				else {
					return false;
				}
			}
		}
		catch (HerokuServiceException e) {
			if (e.getErrorCode() == HerokuServiceException.SECURE_STORE_ERROR) {
				HerokuUtils.userError(parent,
						Messages.getString("HerokuApp_Common_Error_SecureStoreInvalid_Title"), Messages.getString("HerokuApp_Common_Error_SecureStoreInvalid")); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
			else {
				e.printStackTrace();
				HerokuUtils.internalError(parent, e);
				return false;
			}
		}

		return isOk;
	}


}
