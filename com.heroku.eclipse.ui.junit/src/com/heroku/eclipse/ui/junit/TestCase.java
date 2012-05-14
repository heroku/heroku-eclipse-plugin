package com.heroku.eclipse.ui.junit;

import static org.junit.Assert.assertEquals;
import junit.framework.Assert;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class TestCase {
	public SWTWorkbenchBot bot = null;

	public TestCase() {
		bot = new SWTWorkbenchBot();
		
	}
	
	@Before
	public void before() throws Exception {
        UIThreadRunnable.syncExec(new VoidResult() {
            public void run() {
                resetWorkbench();
            }
        });
	}
	
	   /**
     * Ggf. offene Fenster schließen, alle Editoren schliessen, aktuelle
     * Perspektive zuruecksetzen, Standard-Perspektive aktivieren, diese auch
     * zurücksetzen
     */
    private void resetWorkbench() {
        try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
            IWorkbenchPage page = workbenchWindow.getActivePage();
            Shell activeShell = Display.getCurrent().getActiveShell();
            if (activeShell != workbenchWindow.getShell()) {
                activeShell.close();
            }
            page.closeAllEditors(false);
            page.resetPerspective();
            String defaultPerspectiveId = workbench.getPerspectiveRegistry().getDefaultPerspective();
            workbench.showPerspective(defaultPerspectiveId, workbenchWindow);
            page.resetPerspective();
        } catch (WorkbenchException e) {
            throw new RuntimeException(e);
        }
    }
	
    @Test
    public void dontUseUIThread() {
    	Assert.assertNotSame(bot.getDisplay().getThread(), Thread.currentThread());
    }
    
    @Test
	public void testOpenPreferences() throws Exception {
		bot.menu("Window").menu("Preferences").click();
		bot.shell("Preferences");
		
		bot.tree().select("Heroku");
	
		bot.text(1).selectAll().typeText(Credentials.VALID_JUNIT_USER);
		bot.text(2).selectAll().typeText(Credentials.VALID_JUNIT_PWD);
		
		bot.button("Get API Key").click();
		
		bot.text(3).wait(3000); // we wait until the service is done
		
		String apiKey = bot.text(3).getText();
		
		System.err.println("apiKey = " + apiKey);
		
		assertEquals(Credentials.VALID_JUNIT_APIKEY, apiKey);
		
	}
	
	
}
