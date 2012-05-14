package com.heroku.eclipse.ui.junit;

import junit.framework.Assert;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.heroku.eclipse.ui.junit.util.Eclipse;

@RunWith(SWTBotJunit4ClassRunner.class)
public class Preferences {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();
	
	private SWTBotShell preferencePage;
	
	
	@Before
	public void before() throws Exception {
		preferencePage = new Eclipse().openPreferencePage(preferencePage);
		SWTBotTreeItem herokuItem = preferencePage.bot().tree().getTreeItem("Heroku");
		herokuItem.select();
		
		
	}
	
	private SWTBotText textApiKey() {
		return preferencePage.bot().text(3);
	}
	
	private SWTBotText textUsername() {
		return preferencePage.bot().text(1);
	}
	
	private SWTBotText textPassword() {
		return preferencePage.bot().text(2);
	}
	
	private SWTBotButton buttonGetApiKey() {
		return preferencePage.bot().button("Get API Key");
	}
	
	private static class TextChangeCondition implements ICondition {

		private SWTBotText target;
		private String initText;
		
		public TextChangeCondition(SWTBotText target) {
			this.target = target;
		}
		
		@Override
		public boolean test() throws Exception {
			return !initText.equals(target.getText());
		}

		@Override
		public void init(SWTBot bot) {
			initText = target.getText();
		}

		@Override
		public String getFailureMessage() {
			return "Text did not change!";
		}
		
	}
	
	
	@Test
	public void testInvalidLogin() throws InterruptedException {
		textApiKey().setText(""); // first remove the api key
		
		textUsername().typeText("bli");
		textPassword().typeText("bla");
		buttonGetApiKey().click();
		
		bot.waitUntil(new TextChangeCondition(textApiKey()), 5000); // wait for the response
		
		Assert.assertEquals("", textApiKey().getText());
	}
	
	@Test
	public void testValidLogin() throws InterruptedException {
		textApiKey().setText(""); // first remove the api key
		
		textUsername().typeText(Credentials.VALID_JUNIT_USER);
		textPassword().typeText(Credentials.VALID_JUNIT_PWD);
		buttonGetApiKey().click();
		
		bot.waitUntil(new TextChangeCondition(textApiKey()), 5000); // wait for the response
		
		Assert.assertEquals(Credentials.VALID_JUNIT_APIKEY, textApiKey().getText());
	}
	
	
	@After
	public void after() throws Exception {
		if (preferencePage != null) {
			preferencePage.close();
		}
	}
}
