package com.heroku.eclipse.ui.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.heroku.api.App;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.utils.IconKeys;

public class ApplicationEditorInput implements IEditorInput {
	private App app;

	public ApplicationEditorInput(App app) {
		this.app = app;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return IconKeys.getImageDescriptor(IconKeys.ICON_APPINFO_EDITOR_ICON);
	}

	@Override
	public String getName() {
		return app.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return Messages.getFormattedString("HerokuAppInformationPart_Tooltip", app.getName()); //$NON-NLS-1$
	}

	public App getApp() {
		return app;
	}
	
	public void setApp(App app) {
		this.app = app;
	}
	
	@Override
	public boolean equals(Object obj) {
		if( obj instanceof ApplicationEditorInput ) {
			return ((ApplicationEditorInput) obj).app.getId().equals(app.getId());
		}
		return super.equals(obj);
	}
}