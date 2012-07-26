package com.heroku.eclipse.core.services.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProcGroup {
	private String name;
	private List<HerokuProc> processes = new ArrayList<HerokuProc>();
	
	public ProcGroup( String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void add(HerokuProc proc) {
		processes.add(proc);
	}
	
	public List<HerokuProc> getProcesses() {
		return Collections.unmodifiableList(processes);
	}
	
	public String extractDynoName(com.heroku.api.Proc herokuProc) {
		String[] pn = herokuProc.getProcess().split("\\."); //$NON-NLS-1$
		
		if ( pn.length > 1 ) {
			return  pn[pn.length-2];
		}
		else {
			return pn[0];
		}
	}

}
