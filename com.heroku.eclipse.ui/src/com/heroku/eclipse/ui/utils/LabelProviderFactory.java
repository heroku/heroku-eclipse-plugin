package com.heroku.eclipse.ui.utils;

import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.api.Proc;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.model.HerokuProc;
import com.heroku.eclipse.core.services.model.KeyValue;

/**
 * Factory reponsible for creating various labels for App and Proc instances
 *  
 * @author tom.schindl@bestsolution.at
 */
public class LabelProviderFactory {
	/*
	 * ========================================== 
	 * App Element
	 * ==========================================
	 */

	/**
	 * @param services
	 * @param procListCallback
	 * @return the fitting LabelProvider
	 */
	public static ColumnLabelProvider createName(final HerokuServices services, final RunnableWithReturn<List<HerokuProc>, App> procListCallback) {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof App) {
					App app = (App) element;
					return app.getName();
				}
				else if (element instanceof HerokuProc) {
					HerokuProc proc = (HerokuProc) element;
					return proc.getDynoName() + " (" + proc.getPrettyState() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof App) {
					List<HerokuProc> l = procListCallback.run((App) element);
					if (l != null) {
						ProcessState total = ProcessState.UNKNOWN;
						for (Proc p : l) {
							ProcessState s = ProcessState.parseRest(p.getState());
							if (s.ordinal() < total.ordinal()) {
								total = s;
							}
						}
						return getStateIcon(total);
					}
				}
				else if (element instanceof Proc) {
					Proc p = (Proc) element;
					return getStateIcon(ProcessState.parseRest(p.getState()));
				}

				return super.getImage(element);
			}

			private Image getStateIcon(ProcessState state) {
				if (state == ProcessState.IDLE) {
					return IconKeys.getImage(IconKeys.ICON_PROCESS_IDLE);
				}
				else if (state == ProcessState.UP) {
					return IconKeys.getImage(IconKeys.ICON_PROCESS_UP);
				}
				else {
					return IconKeys.getImage(IconKeys.ICON_PROCESS_UNKNOWN);
				}
			}
		};
	}

	/**
	 * @return the name of an App as a LabelProvider
	 */
	public static ColumnLabelProvider createApp_Name() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof App) {
					App app = (App) element;
					return app.getName();
				}
				return ""; //$NON-NLS-1$
			}
		};
	}
	
	/**
	 * @return the git URL of an App as a LabelProvider
	 */
	public static ColumnLabelProvider createApp_GitUrl() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof App) {
					App app = (App) element;
					return app.getGitUrl();
				}
				return ""; //$NON-NLS-1$
			}
		};
	}

	/**
	 * @return the web URL of an App as a LabelProvider
	 */
	public static ColumnLabelProvider createApp_Url() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof App) {
					App app = (App) element;
					return app.getWebUrl();
				}
				return ""; //$NON-NLS-1$
			}
		};
	}

	/*
	 * ========================================== 
	 * Contributor Element
	 * ==========================================
	 */

	/**
	 * @return the email address of a collaborator as a LabelProvider
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

	/**
	 * @param ownerCheckCallback
	 * @return the LabelProvider indicating an App owner
	 */
	public static ColumnLabelProvider createCollaborator_Owner(final RunnableWithReturn<Boolean, Collaborator> ownerCheckCallback) {
		return new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				return ""; //$NON-NLS-1$
			}

			@Override
			public Image getImage(Object element) {
				if (ownerCheckCallback.run((Collaborator) element)) {
					return IconKeys.getImage(IconKeys.ICON_APPLICATION_OWNER);
				}
				return super.getImage(element);
			}
		};
	}
	
	/*
	 * ========================================== 
	 * Environment variables
	 * ==========================================
	 */

	/**
	 * @return the name of an environment variable as a LabelProvider
	 */
	public static ColumnLabelProvider createEnv_Key() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((KeyValue)element).getKey();
			}
		};
	}

	/**
	 * @return the value of an environment variable as a LabelProvider
	 */
	public static ColumnLabelProvider createEnv_Value() {
		return new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((KeyValue)element).getValue();
			}
		};
	}
}
