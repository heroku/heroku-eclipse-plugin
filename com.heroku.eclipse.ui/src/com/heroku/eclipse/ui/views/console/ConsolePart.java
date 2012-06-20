package com.heroku.eclipse.ui.views.console;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * @author tom.schindl@bestsolution.at
 * 
 */
public class ConsolePart {
	private App app;
	private HerokuConsoleViewer viewer;
	private MessageConsole console;

	public ConsolePart(App app) {
		this.app = app;
	}

	public App getApp() {
		return app;
	}

	public Composite createUI(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

		console = HerokuUtils.getConsole(app.getName());
		viewer = new HerokuConsoleViewer(container, console);
		runConsole();

		return container;
	}

	private void runConsole() {
		final MessageConsoleStream out = console.newMessageStream();

		String streamName = "logstream-" + app.getName(); //$NON-NLS-1$

		Thread t = new Thread(streamName) {
			@Override
			public void run() {
				byte[] buffer = new byte[1024];
				int bytesRead;
				try {
					InputStream is = Activator.getDefault().getService().getApplicationLogStream(app);
					while ((bytesRead = is.read(buffer)) != -1) {
						if (out.isClosed()) {
							break;
						}
						out.write(buffer, 0, bytesRead);
					}
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (HerokuServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		t.setDaemon(true);
		t.start();
	}

	public void setFocus() {
		viewer.setFocus();
	}
}
