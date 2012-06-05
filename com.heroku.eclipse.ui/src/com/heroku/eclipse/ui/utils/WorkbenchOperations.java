package com.heroku.eclipse.ui.utils;

import org.eclipse.ui.IWorkbenchPart;

public class WorkbenchOperations {
	public interface IWorkbenchPartExtension extends IWorkbenchPart {
		public void setPartName(String partName);
	}
	
	public static RunnableWithParameter<String> setPartName(final IWorkbenchPartExtension part) {
		return new RunnableWithParameter<String>() {

			@Override
			public void run(String argument) {
				part.setPartName(argument);
			}
		};
	}
}
