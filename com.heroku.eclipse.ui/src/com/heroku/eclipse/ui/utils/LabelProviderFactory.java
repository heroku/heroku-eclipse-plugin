package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.heroku.api.App;
import com.heroku.api.Collaborator;

public class LabelProviderFactory {
	/*
	 * ==========================================
	 * App Element
	 * ==========================================
	 */
	
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
	
	/*
	 * ==========================================
	 * Contributor Element
	 * ==========================================
	 */
	
	public static ColumnLabelProvider createCollaborator_Email() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Collaborator c = (Collaborator) element;
				return c.getEmail();
			}
		};
	}
	
	public static ColumnLabelProvider createCollaborator_Owner(final RunnableWithReturn<Boolean, Collaborator> ownerCheckCallback) {
		return new ColumnLabelProvider() {
			
			@Override
			public String getText(Object element) {
				return "";
			}
			
			@Override
			public Image getImage(Object element) {
				if( ownerCheckCallback.run((Collaborator) element) ) {
					return IconKeys.getImage(IconKeys.ICON_APPLICATION_OWNER); 
				}
				return super.getImage(element);
			}
		};
	}
}
