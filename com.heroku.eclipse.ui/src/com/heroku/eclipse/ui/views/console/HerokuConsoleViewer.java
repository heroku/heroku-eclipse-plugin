package com.heroku.eclipse.ui.views.console;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsoleViewer;

/**
 * @author tom.schindl@bestsolution.at
 *
 */
public class HerokuConsoleViewer extends TextConsoleViewer {

	public HerokuConsoleViewer(Composite parent, TextConsole console) {
		super(parent, console);
	}

	public void setFocus() {
		getTextWidget().setFocus();
	}
}
