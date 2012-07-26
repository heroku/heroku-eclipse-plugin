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

@RunWith( SWTBotJunit4ClassRunner.class )
public class Preferences extends TestCase {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	{
		SWTBotPreferences.DEFAULT_KEY = HerokuServices.ROOT_WIDGET_ID;
	}

	private SWTBotShell preferencePage;

	@Before
	public void before() throws Exception {
		super.before();
		preferencePage = new Eclipse().openPreferencePage( null );
		SWTBotTreeItem herokuItem = preferencePage.bot().tree().getTreeItem( "Heroku" );
		herokuItem.select();
	}

	@After
	public void after() throws Exception {
	}

	private SWTBotText getTextApiKey() {
		return preferencePage.bot().textWithId( PreferenceConstants.P_API_KEY );
	}

	private SWTBotText getTextUsername() {
		return preferencePage.bot().textWithId( PreferenceConstants.P_EMAIL );
	}

	private SWTBotText getTextPassword() {
		return preferencePage.bot().textWithId( PreferenceConstants.P_PASSWORD );
	}

	private SWTBotButton getButtonGetApiKey() {
		return preferencePage.bot().buttonWithId( PreferenceConstants.B_FETCH_API_KEY );
	}

	@Test
	public void testInvalidLogin() throws InterruptedException {
		getTextApiKey().setText( "blabla" ); // first remove the api key

		getTextUsername().setText( "bli" );
		getTextPassword().setText( "bla" );

		bot.waitUntil( new TextChangeOnButtonClickCondition( getTextApiKey(), getButtonGetApiKey() ), 20 * 1000 );

		Assert.assertEquals( "", getTextApiKey().getText() );
	}

	@Test
	public void testValidLogin() throws InterruptedException {
		getTextApiKey().setText( "" ); // first remove the api key

		getTextUsername().setText( Credentials.VALID_JUNIT_USER1 );
		getTextPassword().setText( Credentials.VALID_JUNIT_PWD1 );

		bot.waitUntil( new TextChangeOnButtonClickCondition( getTextApiKey(), getButtonGetApiKey() ), 5000 );

		Assert.assertEquals( Credentials.VALID_JUNIT_APIKEY1, getTextApiKey().getText() );
	}
}
