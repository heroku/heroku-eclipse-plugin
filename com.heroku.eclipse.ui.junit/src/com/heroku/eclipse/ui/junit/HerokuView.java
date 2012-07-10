package com.heroku.eclipse.ui.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.heroku.eclipse.core.constants.HerokuEditorConstants;
import com.heroku.eclipse.core.constants.HerokuViewConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.ui.messages.Messages;

@RunWith(SWTBotJunit4ClassRunner.class)
public class HerokuView extends TestCase {

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
	public void testOpenHerokuView() throws Exception {
		bot.menu("Window").menu("Show View").menu("Other...").click();
		bot.tree().getTreeItem(Messages.getString("HerokuUI.heroku")).expand().getNode(Messages.getString("HerokuUI.viewName")).select();
		bot.button("OK").click();
		
		SWTBotView herokuView = bot.viewByTitle(Messages.getString("HerokuUI.viewName"));
		
		final SWTBotTree tree = herokuView.bot().treeWithId(HerokuViewConstants.V_APPS_LIST);
		
		herokuView.bot().waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				return tree.getAllItems().length > 0;
			}

			@Override
			public void init(SWTBot bot) {
				// nothing here
			}

			@Override
			public String getFailureMessage() {
				return "no heroku apps found";
			}
		}, 10000);
		
		
		Assert.assertTrue("no heroku apps found", tree.getAllItems().length > 0);
		
		tree.select(0).contextMenu(Messages.getString("HerokuAppManagerViewPart_Refresh")).click();
		
		final String firstProjectName = tree.cell(0, 0);
		tree.select(0).contextMenu(Messages.getString("HerokuAppManagerViewPart_AppInfoShort")).click();
		
		herokuView.bot().waitUntil(new ICondition() {
			@Override
			public boolean test() throws Exception {
				List<SWTBotEditor> editors;
				synchronized (bot.editors()) {
					editors = Collections.unmodifiableList(bot.editors());
				}
				for (SWTBotEditor editor : editors) {
					if (editor.getTitle().equals(firstProjectName)) {
						return true;
					}
				}
				return false;
			}

			@Override
			public void init(SWTBot bot) {
				// nothing here
			}

			@Override
			public String getFailureMessage() {
				return "heroku app info editor " + firstProjectName
						+ " did not open";
			}
		}, 10000);

		SWTBotEditor editor = bot.editorByTitle(firstProjectName);
		assertEquals(firstProjectName, editor.bot().text().getText());
		
		editor.bot().tabItemWithId(HerokuEditorConstants.P_COLLABORATION);
		editor.bot().tabItemWithId(HerokuEditorConstants.P_ENVIRONMENT);
		
		assertTrue(true);
		
//		// if not properly configured, an additional step is necessary
//		SWTBotShell shellDlg = bot.shell(Messages.getString("Heroku_Common_Error_HerokuPrefsMissing_Title"));
//		if (shellDlg != null) {
//			shellDlg.bot().button("Yes").click();
//			shellDlg.bot().sleep(2000);
//			SWTBotShell shellPref = bot.shell("Preferences");
//			shellPref.bot().textWithId(PreferenceConstants.P_EMAIL).setText(Credentials.VALID_JUNIT_USER1);
//			shellPref.bot().textWithId(PreferenceConstants.P_PASSWORD).setText(Credentials.VALID_JUNIT_PWD1);
//			shellPref.bot().buttonWithId(PreferenceConstants.B_FETCH_API_KEY).click();
//			shellPref.bot().button("OK").click();
//			//t.sleep(5000);
//		}
//		SWTBotShell shellProject = bot.activeShell();
//		shellProject.bot().tableWithId(AppCreateConstants.V_TEMPLATES_LIST).select(0);
//
//		// an project name including invalid characters
//		shellProject.bot().textWithId(AppCreateConstants.C_APP_NAME).setText(INVALID_PROJECT_NAME);
//		shellProject.bot().button("Finish").click();
//		shellProject.bot().text(" "	+ Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists"));
//
//		// this project already exists
//		shellProject.bot().textWithId(AppCreateConstants.C_APP_NAME).setText(HerokuTestConstants.EXISTING_FOREIGN_APP);
//		shellProject.bot().button("Finish").click();
//		shellProject.bot().text(" "	+ Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists"));
//
//		// this project name should work
//		final  String projectName = PROJECT_NAME_PREFIX + System.currentTimeMillis();
//		shellProject.bot().textWithId(AppCreateConstants.C_APP_NAME).setText(projectName);
//		final SWTBotButton bFinish = shellProject.bot().button("Finish");
//		final SWTBot packExplorer =  bot.viewByTitle("Project Explorer").bot();
//		
//		bot.waitUntil(new ICondition() {
//
//			@Override
//			public boolean test() throws Exception {
//				if (packExplorer.tree().getAllItems().length == 0 ) {
//					return false;
//				}
//				else {
//					packExplorer.tree().getTreeItem(projectName);
//					return true;
//				}
//			}
//
//			@Override
//			public void init(SWTBot bot) {
//				// TODO Auto-generated method stub
//				bFinish .click();
//			}
//
//			@Override
//			public String getFailureMessage() {
//				return "Project did not appear in project explorer";
//			}
//			
//		}, 60000);
//		
//		assertTrue(true);
	}

	@After
	public void after() throws Exception {
	}
}
