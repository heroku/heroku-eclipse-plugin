package com.heroku.eclipse.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.heroku.api.App;
import com.heroku.eclipse.ui.views.console.ConsolePart;

/**
 * @author tom.schindl@bestsolution.at
 */
public class ConsoleViewPart extends ViewPart {
	public static final String ID = "com.heroku.eclipse.ui.HerokuLogConsoles";

	private CTabFolder f;

	@Override
	public void createPartControl(Composite parent) {
		f = new CTabFolder(parent, SWT.BOTTOM);
	}

	@Override
	public void setFocus() {
		f.setFocus();
	}

	public void openLog(App app) {
		CTabItem item = findConsole(app);
		ConsolePart p;

		if (item == null) {
			item = new CTabItem(f, SWT.CLOSE);
			item.setText(app.getName());

			p = new ConsolePart(app);
			item.setData(p);
			item.setControl(p.createUI(f));
		}
		else {
			p = (ConsolePart) item.getData();
		}

		f.setSelection(item);
		p.setFocus();
	}

	private CTabItem findConsole(App app) {
		for (CTabItem i : f.getItems()) {
			if (((ConsolePart) i.getData()).getApp().getId() == app.getId()) {
				return i;
			}
		}
		return null;
	}
}
