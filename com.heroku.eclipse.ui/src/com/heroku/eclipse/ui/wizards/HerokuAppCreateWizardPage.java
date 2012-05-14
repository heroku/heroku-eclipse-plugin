/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * 
 * @author udo.rader@bestsolution.at
 *
 */
public class HerokuAppCreateWizardPage extends WizardPage {
	/**
	 * @param pageName
	 */
	protected HerokuAppCreateWizardPage(String pageName) {
		super(pageName);
		// TODO Auto-generated constructor stub
	}

	public HerokuAppCreateWizardPage() {
		super("page name");
		setTitle("app listing");
		setDescription("app templates listing description");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		// META:
		// #1: ensure valid prefs
		// #2: step1 listAllApps, single select
		// #3: import
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		
		Button b = new Button( composite, SWT.NONE );
		b.setText("a button");
		

//		// explicit table creation so that we can individually intercept
//		// oncheck events per row, credits go to tom: http://tomsondev.bestsolution.at/2008/10/08/disable-parts-swt-tabletree-with-swt-check/
//		final Table wrapperTable = new Table( composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.CHECK );
//		//
//		// // Attach a listener directly after the creation
//		wrapperTable.addListener( SWT.Selection, new Listener() {
//			public void handleEvent( Event event ) {
//				if ( event.detail == SWT.CHECK ) {
//					System.err.println("checkcheckcheck");
//				}
//			}
//		} );
//
//		CheckboxTableViewer tableViewer = new CheckboxTableViewer( wrapperTable );
//
//		// Adding comparator
////		tableViewer.setComparator( comparator );
//
////		ObservableListContentProvider cp = new ObservableListContentProvider();
////		tableViewer.setContentProvider( cp );
//		Table table = tableViewer.getTable();
//		GridData gd_table = new GridData( SWT.FILL, SWT.FILL, true, true, 8, 1 );
//
//		table.setLayoutData( gd_table );
//		table.setHeaderVisible( true );
//
////		imgFactory = ImageFactory.getInstance();
//
//		{
//			TableViewerColumn tableViewerColumnSync = new TableViewerColumn( tableViewer, SWT.NONE );
//			TableColumn checkboxColumn = tableViewerColumnSync.getColumn();
//			checkboxColumn.setWidth( 20 );
//			checkboxColumn.setResizable( false );
//			tableViewerColumnSync.setLabelProvider( new ColumnLabelProvider() {
//				@Override
//				public String getText( Object element ) {
//					return "";
//				}
//			} );
//
//			checkboxColumn.addSelectionListener( new SelectionAdapter() {
//				@Override
//				public void widgetSelected( SelectionEvent e ) {
//					System.err.println("cbc header click");
////					if ( isCustomerView ) {
////						if ( ! service.isDirty() || parent.save( false ) ) {
////							if ( tableViewer.getCheckedElements().length != tasks.size() ) {
////								getCurrentSelected().clear();
////								getCurrentSelected().addAll( (Collection<?>) tableViewer.getInput() );
////							}
////							else {
////								getCurrentSelected().clear();
////							}
////						}
////					}
//				}
//			} );
//		}
//
//		{
//			TableViewerColumn tableViewerColumn = new TableViewerColumn( tableViewer, SWT.NONE );
//			TableColumn tc = tableViewerColumn.getColumn();
//			tc.setWidth( 100 );
//			tc.setText( "col1header" );
////			tableViewerColumn.setLabelProvider( new GenericMapCellLabelProvider( "{0}", prop.observeDetail( cp.getKnownElements() ) ) ); //$NON-NLS-1$
//			tableViewer.getTable().setSortDirection( SWT.DOWN );
//
////			widgetRegistry.put( TABLE_COLUMNS.SERVICE_REQUEST_NUMBER, tc );
//		}
//		
//		{
//			TableViewerColumn tableViewerColumn = new TableViewerColumn( tableViewer, SWT.NONE );
//			TableColumn tc = tableViewerColumn.getColumn();
//			tc.setWidth( 100 );
//			tc.setText( "col2header" );
////			tableViewerColumn.setLabelProvider( new GenericMapCellLabelProvider( "{0}", prop.observeDetail( cp.getKnownElements() ) ) ); //$NON-NLS-1$
//			tableViewer.getTable().setSortDirection( SWT.DOWN );
//
////			widgetRegistry.put( TABLE_COLUMNS.SERVICE_REQUEST_NUMBER, tc );
//		}
	}

}
