/**
 * 
 */
package com.heroku.eclipse.core.services;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface for an importer bound to a specific nature such as maven
 * @author udo.rader@bestsolution.at
 */
public interface HerokuProjectNatureImporter {
	/**
	 * Determines, if a project has the importers nature 
	 * @param prj 
	 * 				the Eclipse project to investigate
	 * @return <code>true</code>, if the project has the nature, <code>false</code> if not
	 */
	public boolean hasNature( IProject prj );
	
	/**
	 * Determines, if a project has the importers nature 
	 * @param prj 
	 * @param pm 
	 * @return <code>true</code>, if the project has the nature, <code>false</code> if not
	 */
	public boolean enableNature( IProject prj, IProgressMonitor pm );

}
