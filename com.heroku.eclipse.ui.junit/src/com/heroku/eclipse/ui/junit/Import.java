package com.heroku.eclipse.ui.junit;

import static org.junit.Assert.assertTrue;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.heroku.eclipse.core.constants.AppImportConstants;
import com.heroku.eclipse.core.constants.PreferenceConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.junit.common.Credentials;
import com.heroku.eclipse.ui.messages.Messages;

@RunWith( SWTBotJunit4ClassRunner.class )
public class Import extends TestCase {
	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	{
		SWTBotPreferences.DEFAULT_KEY = HerokuServices.ROOT_WIDGET_ID;
	}

	@Before
	public void before() throws Exception {
		super.before();
	}

	@Test
	public void testImportExistingProject() throws Exception {
		bot.menu( "File" ).menu( "Import..." ).click();
		bot.tree().getTreeItem( Messages.getString( "HerokuUI.heroku" ) ).expand().getNode( Messages.getString( "HerokuUI.ImportWizardName" ) ).select();
		bot.button( "Next >" ).click();
		// if not properly configured, an additional step is necessary
		SWTBotShell shellDlg = null;
		try {
			shellDlg = bot.shell( Messages.getString( "Heroku_Common_Error_HerokuPrefsMissing_Title" ) );
		}
		catch ( Exception e ) {
			// nothing here
		}
		if ( shellDlg != null ) {
			shellDlg.bot().button( "Yes" ).click();
			shellDlg.bot().sleep( 2000 );
			SWTBotShell shellPref = bot.shell( "Preferences" );
			shellPref.bot().textWithId( PreferenceConstants.P_EMAIL ).setText( Credentials.VALID_JUNIT_USER1 );
			shellPref.bot().textWithId( PreferenceConstants.P_PASSWORD ).setText( Credentials.VALID_JUNIT_PWD1 );
			shellPref.bot().buttonWithId( PreferenceConstants.B_FETCH_API_KEY ).click();
			shellPref.bot().button( "OK" ).click();
			// t.sleep(5000);
		}
		SWTBotShell shellSelectProject = bot.activeShell();
		shellSelectProject.bot().tableWithId( AppImportConstants.V_APPS_LIST ).select( 0 );
		shellSelectProject.bot().button( "Next >" ).click();

		SWTBotShell shellSelectWizard = bot.activeShell();
		assertTrue( shellSelectWizard.bot().radioWithId( AppImportConstants.B_AUTODETECT ).isSelected() );
		shellSelectWizard.bot().button( "Finish" ).click();

		// assertTrue(false);
	}
}
