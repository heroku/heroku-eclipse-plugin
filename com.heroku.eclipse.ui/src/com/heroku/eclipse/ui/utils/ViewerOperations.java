package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.viewers.Viewer;

public class ViewerOperations {
	public static <I> RunnableWithParameter<I> input(final Viewer viewer) {
		return new RunnableWithParameter<I>() {

			@Override
			public void run(I argument) {
				viewer.setInput(argument);
			}
			
		};
	}
}
