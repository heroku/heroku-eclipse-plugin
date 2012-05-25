package com.heroku.eclipse.ui.views;


import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;


/**
 * The main view of the Heroclipse plugin   
 * 
 * @author udo.rader@bestsolution.at
 */
public class HerokuApplicationManagerViewPart extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.heroku.eclipse.ui.views.HerokuApplicationManager"; //$NON-NLS-1$

	private TableViewer viewer;

	public void createPartControl(Composite parent) {
		TableViewer viewer = new TableViewer(parent);

		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText("App Status");
		}

		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText("Name");
		}
		
		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText("Git Url");
		}
		
		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText("App Url");
		}
		
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	static class DialogImpl extends TitleAreaDialog {

		public DialogImpl(Shell parentShell) {
			super(parentShell);
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = (Composite) super.createDialogArea(parent);
			
			CTabFolder folder = new CTabFolder(container, SWT.BOTTOM | SWT.BORDER);
			
			{
				CTabItem item = new CTabItem(folder, SWT.NONE);
				item.setText("Application Info");
				
			}
			
			{
				CTabItem item = new CTabItem(folder, SWT.NONE);
				item.setText("Collaborators");				
			}

			{
				CTabItem item = new CTabItem(folder, SWT.NONE);
				item.setText("Environment Variables");				
			}

			
			return container;
		}
	}
}