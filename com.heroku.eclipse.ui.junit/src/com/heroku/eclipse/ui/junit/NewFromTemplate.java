package com.heroku.eclipse.ui.junit;

import static org.junit.Assert.assertTrue;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.heroku.eclipse.core.constants.AppCreateConstants;
import com.heroku.eclipse.core.constants.PreferenceConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.junit.common.Credentials;
import com.heroku.eclipse.core.services.junit.common.HerokuTestConstants;
import com.heroku.eclipse.ui.messages.Messages;

@RunWith(SWTBotJunit4ClassRunner.class)
public class NewFromTemplate extends TestCase {

	private static final String INVALID_PROJECT_NAME = "MylittleSWTBotTest";
	private static final String PROJECT_NAME_PREFIX = "besojunit";
	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	{
		SWTBotPreferences.DEFAULT_KEY = HerokuServices.ROOT_WIDGET_ID;
	}

	@Before
	public void before() throws Exception {
		super.before();
	}

	@Test
	public void testCreateNewProjectFromTemplate() throws Exception {
		// close the welcome tab
		if (Boolean.TRUE.toString().equals(System.getProperty("heroku.devel"))) {
			bot.viewByTitle("Welcome").close();
		}

		bot.menu("File").menu("New").menu("Other...").click();
		bot.tree().getTreeItem(Messages.getString("HerokuUI.heroku")).expand().getNode(Messages.getString("HerokuUI.CreateWizardName")).select();
		bot.button("Next >").click();
		// if not properly configured, an additional step is necessary
		SWTBotShell shellDlg = bot.shell(Messages.getString("Heroku_Common_Error_HerokuPrefsMissing_Title"));
		if (shellDlg != null) {
			shellDlg.bot().button("Yes").click();
			shellDlg.bot().sleep(2000);
			SWTBotShell shellPref = bot.shell("Preferences");
			shellPref.bot().textWithId(PreferenceConstants.P_EMAIL).setText(Credentials.VALID_JUNIT_USER1);
			shellPref.bot().textWithId(PreferenceConstants.P_PASSWORD).setText(Credentials.VALID_JUNIT_PWD1);
			shellPref.bot().buttonWithId(PreferenceConstants.B_FETCH_API_KEY).click();
			shellPref.bot().button("OK").click();
			//t.sleep(5000);
		}
		SWTBotShell shellProject = bot.activeShell();
		shellProject.bot().tableWithId(AppCreateConstants.V_TEMPLATES_LIST).select(0);

		// an project name including invalid characters
		shellProject.bot().textWithId(AppCreateConstants.C_APP_NAME).setText(INVALID_PROJECT_NAME);
		shellProject.bot().button("Finish").click();
		shellProject.bot().text(" "	+ Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists"));

		// this project already exists
		shellProject.bot().textWithId(AppCreateConstants.C_APP_NAME).setText(HerokuTestConstants.EXISTING_FOREIGN_APP);
		shellProject.bot().button("Finish").click();
		shellProject.bot().text(" "	+ Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists"));

		// this project name should work
		final  String projectName = PROJECT_NAME_PREFIX + System.currentTimeMillis();
		shellProject.bot().textWithId(AppCreateConstants.C_APP_NAME).setText(projectName);
		final SWTBotButton bFinish = shellProject.bot().button("Finish");
		final SWTBot packExplorer =  bot.viewByTitle("Project Explorer").bot();
		
		bot.waitUntil(new ICondition() {

			@Override
			public boolean test() throws Exception {
				if (packExplorer.tree().getAllItems().length == 0 ) {
					return false;
				}
				else {
					packExplorer.tree().getTreeItem(projectName);
					return true;
				}
			}

			@Override
			public void init(SWTBot bot) {
				// TODO Auto-generated method stub
				bFinish .click();
			}

			@Override
			public String getFailureMessage() {
				return "Project did not appear in project explorer";
			}
			
		}, 60000);
		
		assertTrue(true);
	}

	@After
	public void after() throws Exception {
	}
}
