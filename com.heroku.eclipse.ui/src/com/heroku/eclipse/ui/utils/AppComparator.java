package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices.APP_FIELDS;

/**
 * @author udo.rader@bestsolution.at
 *
 */
public class AppComparator extends ViewerComparator {
	
	public static final String SORT_IDENTIFIER = "S_COL_ID"; //$NON-NLS-1$
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int rv = 0;

		App a = (App) e1;
		App b = (App) e2;
		
		APP_FIELDS appField = null;
		int sortDir = 0;
		
		if ( viewer instanceof TableViewer ) {
			if ( ((TableViewer) viewer).getTable().getSortColumn() != null ) {
				appField = (APP_FIELDS) ((TableViewer) viewer).getTable().getSortColumn().getData(SORT_IDENTIFIER);
			}
			sortDir = ((TableViewer) viewer).getTable().getSortDirection();
		}
		else if ( viewer instanceof TreeViewer ) {
			if ( ((TreeViewer) viewer).getTree().getSortColumn() != null ) {
				appField = (APP_FIELDS) ((TreeViewer) viewer).getTree().getSortColumn().getData(SORT_IDENTIFIER);
			}
			sortDir = ((TreeViewer) viewer).getTree().getSortDirection();
		}
		
		if ( appField == null ) {
			rv = a.getName().compareTo(b.getName());
		}
		else {
			switch (appField) {
				default:
					rv = a.getName().compareTo(b.getName());
					break;
				case APP_GIT_URL:
					rv = a.getGitUrl().compareTo(b.getGitUrl());
					break;
				case APP_WEB_URL:
					rv = a.getWebUrl().compareTo(b.getWebUrl());
					break;
			}
		}
		
		if (sortDir == SWT.DOWN) {
			rv = -rv;
		}

		return rv;
	}
}
