package com.heroku.eclipse.ui.junit;

import static org.junit.Assert.fail;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.heroku.eclipse.core.services.HerokuServices;

@RunWith( SWTBotJunit4ClassRunner.class )
public class Delete extends TestCase {
	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	{
		SWTBotPreferences.DEFAULT_KEY = HerokuServices.ROOT_WIDGET_ID;
	}

	@Before
	public void before() throws Exception {
		super.before();
	}

	@Test
	public void testDeleteProjectLocal() throws Exception {
		final SWTBot packExplorer = bot.viewByTitle( "Project Explorer" ).bot();
		SWTBotTree tree = packExplorer.tree();
		if ( tree.getAllItems().length == 0 ) {
			fail( "no projects found" );
		}
		else {
			final int projectCount = tree.getAllItems().length;
			tree.getAllItems()[0].contextMenu( "Delete" ).click();
			SWTBotShell delShell = bot.shell( "Delete Resources" );
			delShell.bot().checkBox().select();
			SWTBotButton bFinish = delShell.bot().button( "OK" );
			try {
				bot.waitUntil( new EmptyPackageExplorerCondition( bFinish, packExplorer, projectCount ), 5 * 1000 );
			}
			catch ( Exception e ) {
				// try again
				tree.getAllItems()[0].contextMenu( "Delete" ).click();
				delShell = bot.shell( "Delete Resources" );
				delShell.bot().checkBox().select();
				bFinish = delShell.bot().button( "OK" );
				bot.waitUntil( new EmptyPackageExplorerCondition( bFinish, packExplorer, projectCount ), 5 * 1000 );
			}

		}
	}

	private final class EmptyPackageExplorerCondition implements ICondition {
		private final SWTBotButton bFinish;
		private final SWTBot packExplorer;
		private final int projectCount;

		private EmptyPackageExplorerCondition( SWTBotButton bFinish, SWTBot packExplorer, int projectCount ) {
			this.bFinish = bFinish;
			this.packExplorer = packExplorer;
			this.projectCount = projectCount;
		}

		@Override
		public boolean test() throws Exception {
			if ( packExplorer.tree().getAllItems().length == ( projectCount - 1 ) ) {
				return true;
			}
			else {
				return false;
			}
		}

		@Override
		public void init( SWTBot bot ) {
			bFinish.click();
		}

		@Override
		public String getFailureMessage() {
			return "Project was not deleted from project explorer";
		}
	}
}
