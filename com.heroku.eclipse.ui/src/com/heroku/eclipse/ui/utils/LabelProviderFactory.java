package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import com.heroku.api.App;

public class LabelProviderFactory {
	public static ColumnLabelProvider createApp_Name() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				App app = (App) element;
				return app.getName();
			}
		};
	}
	
	public static ColumnLabelProvider createApp_GitUrl() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				App app = (App) element;
				return app.getGitUrl();
			}
		};
	}
	
	public static ColumnLabelProvider createApp_Url() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				App app = (App) element;
				return app.getWebUrl();
			}
		};
	}
	
	public static ColumnLabelProvider createApp_Status() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				App app = (App) element;
				return app.getCreateStatus();
			}
		};
	}
}
