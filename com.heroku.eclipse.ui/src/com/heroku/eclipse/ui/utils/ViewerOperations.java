package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

public class ViewerOperations {
	public static <I> RunnableWithParameter<I> input(final Viewer viewer) {
		return new RunnableWithParameter<I>() {

			@Override
			public void run(I argument) {
				TreePath[] paths = null;
				TreeViewer tv = null;
				if( viewer instanceof TreeViewer ) {
					tv = (TreeViewer) viewer;
					paths = tv.getExpandedTreePaths();
				}
				viewer.setInput(argument);
				
				if( paths != null ) {
					tv.setExpandedTreePaths(paths);
				}
			}
			
		};
	}
}
