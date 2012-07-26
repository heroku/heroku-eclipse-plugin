package com.heroku.eclipse.ui.junit;

import java.util.List;

import junit.framework.Assert;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( SWTBotJunit4ClassRunner.class )
public class TestCase {
	protected static final String TESTPROJECT_NAME_PREFIX = "besojunit";

	public SWTWorkbenchBot bot = null;

	public TestCase() {
		bot = new SWTWorkbenchBot();

	}

	@Before
	public void before() throws Exception {
		UIThreadRunnable.syncExec( new VoidResult() {
			public void run() {
				resetWorkbench();
			}
		} );
		// close the welcome view if it's open.
		List<SWTBotView> list = bot.views();
		for ( SWTBotView v : list ) {
			if ( v.getTitle().equals( "Welcome" ) ) {
				v.close();
				break;
			}
		}
	}

	/**
	 * Ggf. offene Fenster schließen, alle Editoren schliessen, aktuelle Perspektive zuruecksetzen, Standard-Perspektive aktivieren, diese auch zurücksetzen
	 */
	protected void resetWorkbench() {
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = workbenchWindow.getActivePage();
			Shell activeShell = Display.getCurrent().getActiveShell();
			if ( activeShell != null && activeShell != workbenchWindow.getShell() ) {
				activeShell.close();
			}
			page.closeAllEditors( false );
			page.resetPerspective();
			String defaultPerspectiveId = workbench.getPerspectiveRegistry().getDefaultPerspective();
			workbench.showPerspective( defaultPerspectiveId, workbenchWindow );
			page.resetPerspective();
		}
		catch ( WorkbenchException e ) {
			throw new RuntimeException( e );
		}
	}

	@Test
	public void dontUseUIThread() {
		Assert.assertNotSame( bot.getDisplay().getThread(), Thread.currentThread() );
	}
}
