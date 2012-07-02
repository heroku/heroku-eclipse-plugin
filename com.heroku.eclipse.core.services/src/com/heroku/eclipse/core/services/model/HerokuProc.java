package com.heroku.eclipse.core.services.model;

/**
 * An extended version of the {@link com.heroku.api.Proc}
 * class, providing additional, calculated information about a HerokuProc
 * such as its somewhat "unique" id.
 * @author udo.rader@bestsolution.at
 */
public class HerokuProc {
	private static final long serialVersionUID = 1L;
	
	private String uniqueId;
	private String dynoName;
	private com.heroku.api.Proc herokuProc;
	
	/**
	 * @param herokuProc
	 */
	public HerokuProc(com.heroku.api.Proc herokuProc) {
		setDynoName(herokuProc);
		setUniqueId(herokuProc);
		setHerokuProc(herokuProc);
	}
	
	/**
	 * Helper method delivering a somewhat unique id,
	 * consisting of its app name and its process name.
	 * 
	 * @return the crafted process id
	 */
	public String getUniqueId() {
		return uniqueId;
	}
	/**
	 * @param uniqueId the uniqueId to set
	 */
	private void setUniqueId(com.heroku.api.Proc herokuProc) {
		this.uniqueId = herokuProc.getAppName()+"<>"+getDynoName(); //$NON-NLS-1$
	}
	
	/**
	 * Helper method retrieving the process name (aka "Dyno") of a process without the
	 * process counter. So for a HerokuProc.getProcess() returning foo.1, you will get
	 * only foo
	 * 
	 * @return the process name stripped from the process counter
	 */
	public String getDynoName() {
		return dynoName;
	}
	/**
	 * @param processName
	 * 			the com.heroku.api.Proc process name 
	 */
	private void setDynoName(com.heroku.api.Proc herokuProc) {
		String[] pn = herokuProc.getProcess().split("\\."); //$NON-NLS-1$
		
		if ( pn.length > 1 ) {
			dynoName =  pn[pn.length-2];
		}
		else {
			dynoName = pn[0];
		}
	}

	/**
	 * @return the herokuProc
	 */
	public com.heroku.api.Proc getHerokuProc() {
		return herokuProc;
	}

	/**
	 * @param herokuProc the herokuProc to set
	 */
	public void setHerokuProc(com.heroku.api.Proc herokuProc) {
		this.herokuProc = herokuProc;
	}
}
