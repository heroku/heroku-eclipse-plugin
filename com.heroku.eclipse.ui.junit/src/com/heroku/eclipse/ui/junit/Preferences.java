package com.heroku.eclipse.ui.junit;

import junit.framework.Assert;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.heroku.eclipse.core.constants.PreferenceConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.junit.common.Credentials;
import com.heroku.eclipse.ui.junit.condition.TextChangeOnButtonClickCondition;
import com.heroku.eclipse.ui.junit.util.Eclipse;

@RunWith(SWTBotJunit4ClassRunner.class)
public class Preferences extends TestCase {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	{
		SWTBotPreferences.DEFAULT_KEY = HerokuServices.ROOT_WIDGET_ID;
	}

	private SWTBotShell preferencePage;

	@Before
	public void before() throws Exception {
		super.before();
		preferencePage = new Eclipse().openPreferencePage(preferencePage);
		SWTBotTreeItem herokuItem = preferencePage.bot().tree()
				.getTreeItem("Heroku");
		herokuItem.select();
	}

	private SWTBotText textApiKey() {
		return preferencePage.bot().textWithId(PreferenceConstants.P_API_KEY);
	}

	private SWTBotText textUsername() {
		return preferencePage.bot().textWithId(PreferenceConstants.P_EMAIL);
	}

	private SWTBotText textPassword() {
		return preferencePage.bot().textWithId(PreferenceConstants.P_PASSWORD);
	}

	private SWTBotButton buttonGetApiKey() {
		return preferencePage.bot().buttonWithId(
				PreferenceConstants.B_FETCH_API_KEY);
	}

	@Test
	public void testOpenPreferences() throws Exception {
	}

	@Test
	public void testInvalidLogin() throws InterruptedException {
		textApiKey().setText("blabla"); // first remove the api key

		textUsername().setText("bli");
		textPassword().setText("bla");

		bot.waitUntil(new TextChangeOnButtonClickCondition(textApiKey(),
				buttonGetApiKey()), 5000); 

		Assert.assertEquals("", textApiKey().getText());
	}

	@Test
	public void testValidLogin() throws InterruptedException {
		textApiKey().setText(""); // first remove the api key

		textUsername().setText(Credentials.VALID_JUNIT_USER1);
		textPassword().setText(Credentials.VALID_JUNIT_PWD1);

		bot.waitUntil(new TextChangeOnButtonClickCondition(textApiKey(),
				buttonGetApiKey()), 5000); 

		Assert.assertEquals(Credentials.VALID_JUNIT_APIKEY1, textApiKey()
				.getText());
	}

	@After
	public void after() throws Exception {
		if (preferencePage != null) {
			preferencePage.close();
		}
	}
}
