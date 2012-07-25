package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.SafeRunnable;

public abstract class SafeRunnableAction extends Action {
	public SafeRunnableAction(String title) {
		super(title);
	}
	
	public SafeRunnableAction(String title, ImageDescriptor desc) {
		super(title,desc);
	}
	
	@Override
	public final void run() {
		SafeRunnable.run(new SafeRunnable() {
			
			@Override
			public void run() throws Exception {
				safeRun();
			}
		});
	}
	
	protected abstract void safeRun();
}
