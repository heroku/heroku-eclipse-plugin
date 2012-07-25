package com.heroku.eclipse.ui.junit.condition;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;

public abstract class TextChangeOnActionCondition implements ICondition {
	private SWTBotText target;
	private String initText;

	public TextChangeOnActionCondition( SWTBotText target ) {
		this.target = target;
	}

	@Override
	public boolean test() throws Exception {
		return !initText.equals( target.getText() );
	}

	@Override
	public void init( SWTBot bot ) {
		initText = target.getText();
		doAction();
	}

	@Override
	public String getFailureMessage() {
		return "Text did not change!";
	}

	protected abstract void doAction();
}
