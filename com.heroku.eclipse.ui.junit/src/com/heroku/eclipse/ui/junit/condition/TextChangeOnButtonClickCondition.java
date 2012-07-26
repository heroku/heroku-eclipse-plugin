package com.heroku.eclipse.ui.junit.condition;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;

public class TextChangeOnButtonClickCondition extends TextChangeOnActionCondition {
	private final SWTBotButton buttonToClick;

	public TextChangeOnButtonClickCondition( SWTBotText target, SWTBotButton buttonToClick ) {
		super( target );
		this.buttonToClick = buttonToClick;
	}

	@Override
	protected void doAction() {
		buttonToClick.click();
	}

}
