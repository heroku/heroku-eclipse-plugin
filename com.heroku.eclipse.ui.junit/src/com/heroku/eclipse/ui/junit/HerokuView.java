package com.heroku.eclipse.ui.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.heroku.eclipse.core.constants.HerokuEditorConstants;
import com.heroku.eclipse.core.constants.HerokuViewConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.ui.messages.Messages;

@RunWith( SWTBotJunit4ClassRunner.class )
public class HerokuView extends TestCase {
	private SWTBotView herokuView;

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	{
		SWTBotPreferences.DEFAULT_KEY = HerokuServices.ROOT_WIDGET_ID;
	}

	@Before
	public void before() throws Exception {
		super.before();
	}

	@After
	public void after() throws Exception {
	}

	@Test
	public void testOpenHerokuView() throws Exception {
		final SWTBotTree tree = openViewAndGetTree();

		Assert.assertTrue( "no heroku apps found", tree.getAllItems().length > 0 );

		tree.select( 0 ).contextMenu( Messages.getString( "HerokuAppManagerViewPart_Refresh" ) ).click();
	}

	private SWTBotTree openViewAndGetTree() {
		bot.menu( "Window" ).menu( "Show View" ).menu( "Other..." ).click();
		bot.tree().getTreeItem( Messages.getString( "HerokuUI.heroku" ) ).expand().getNode( Messages.getString( "HerokuUI.viewName" ) ).select();
		bot.button( "OK" ).click();

		herokuView = bot.viewByTitle( Messages.getString( "HerokuUI.viewName" ) );

		// refresh
		// herokuView.bot().toolbarButton().click();

		final SWTBotTree tree = herokuView.bot().treeWithId( HerokuViewConstants.V_APPS_LIST );

		herokuView.bot().waitUntil( new ICondition() {
			@Override
			public boolean test() throws Exception {
				return tree.getAllItems().length > 0;
			}

			@Override
			public void init( SWTBot bot ) {
				// nothing here
			}

			@Override
			public String getFailureMessage() {
				return "no heroku apps found";
			}
		}, 30000 );

		return tree;
	}

	@Test
	public void testOpenAppInfo() throws Exception {
		final SWTBotTree tree = openViewAndGetTree();
		tree.select( 0 ).contextMenu( Messages.getString( "HerokuAppManagerViewPart_AppInfoShort" ) ).click();
		EditorOpenCondition c = new EditorOpenCondition();
		herokuView.bot().waitUntil( c, 20 * 1000 );

		SWTBotEditor editor = bot.editorByTitle( c.getProjectName() );
		assertEquals( c.getProjectName(), editor.bot().text().getText() );

		editor.bot().tabItemWithId( HerokuEditorConstants.P_COLLABORATION ).activate();
		assertTrue( editor.bot().table().rowCount() > 0 );

		editor.bot().tabItemWithId( HerokuEditorConstants.P_ENVIRONMENT ).activate();
		assertTrue( editor.bot().table().rowCount() > 0 );
	}

	@Test
	public void testImportNo() throws Exception {
		final SWTBotTree tree = openViewAndGetTree();
		SWTBotMenu menuItem = tree.select( 0 ).contextMenu( Messages.getString( "HerokuAppManagerViewPart_Import" ) );
		if ( menuItem.isEnabled() ) {
			menuItem.click();
			herokuView.bot().activeShell().bot().button( "No" ).click();
		}
	}

	@Test
	public void testRestart() throws Exception {
		final SWTBotTree tree = openViewAndGetTree();

		SWTBotMenu menuItem = tree.select( 0 ).contextMenu( Messages.getString( "HerokuAppManagerViewPart_Restart" ) );
		if ( menuItem.isEnabled() ) {
			menuItem.click();
			herokuView.bot().activeShell().bot().button( "Yes" ).click();
		}
	}

	@Test
	public void testViewLogs() throws Exception {
		final SWTBotTree tree = openViewAndGetTree();

		SWTBotMenu menuItem = tree.select( 0 ).contextMenu( Messages.getString( "HerokuAppManagerViewPart_ViewLogs" ) );
		if ( menuItem.isEnabled() ) {
			menuItem.click();
			SWTBotView consoleView = bot.viewByTitle( "Console" );
			final SWTBotStyledText consoleText = consoleView.bot().styledText();
			consoleView.bot().waitUntil( new ICondition() {
				@Override
				public boolean test() throws Exception {
					return consoleText.getText().length() > 0;
				}

				@Override
				public void init( SWTBot bot ) {
				}

				@Override
				public String getFailureMessage() {
					return "no text was shown in console";
				}

			}, 10 * 1000 );
		}
	}

	// @Test
	public void testScale() throws Exception {
		final SWTBotTree tree = openViewAndGetTree();

		SWTBotMenu menuItem = tree.select( 0 ).contextMenu( Messages.getString( "HerokuAppManagerViewPart_Scale" ) );
		if ( menuItem.isEnabled() ) {
			System.out.println( "###################################################################################" );
			System.out.println( "enabled - click it" );
			menuItem.click();

			// bot.waitUntil(new ICondition() {
			//
			// @Override
			// public boolean test() throws Exception {
			// return bot
			// .activeShell()
			// .getText()
			// .equals(Messages
			// .getString("HerokuAppManagerViewPart_Scale_Title"));
			// }
			//
			// @Override
			// public void init(SWTBot bot) {
			// }
			//
			// @Override
			// public String getFailureMessage() {
			// return "scale dialog did not open";
			// }
			//
			// }, 10 * 1000);

			// bot.sh

			bot.activeShell().bot().button( "OK" ).click();
		}
	}

	@Test
	public void testDestroy() throws Exception {
		final SWTBotTree tree = openViewAndGetTree();

		SWTBotMenu menuItem = tree.select( 0 ).contextMenu( Messages.getString( "HerokuAppManagerViewPart_Destroy" ) );
		if ( menuItem.isEnabled() ) {
			menuItem.click();
			herokuView.bot().activeShell().bot().button( "No" ).click();
		}
	}

	@Test
	public void testOpen() throws Exception {
		final SWTBotTree tree = openViewAndGetTree();

		SWTBotMenu menuItem = tree.select( 0 ).contextMenu( Messages.getString( "HerokuAppManagerViewPart_Open" ) );
		if ( menuItem.isEnabled() ) {
			// propably we don't have a browser in test environment
			// menuItem.click();
		}
	}

	public class EditorOpenCondition implements ICondition {
		private String projectName;

		@Override
		public boolean test() throws Exception {
			List<SWTBotEditor> editors;
			synchronized ( bot.editors() ) {
				editors = Collections.unmodifiableList( bot.editors() );
			}
			for ( SWTBotEditor editor : editors ) {
				if ( editor.getTitle().startsWith( TESTPROJECT_NAME_PREFIX ) ) {
					projectName = editor.getTitle();
					return true;
				}
			}
			return false;
		}

		@Override
		public void init( SWTBot bot ) {
			// nothing here
		}

		@Override
		public String getFailureMessage() {
			return "heroku app info editor did not open";
		}

		public String getProjectName() {
			return projectName;
		}
	}
}
