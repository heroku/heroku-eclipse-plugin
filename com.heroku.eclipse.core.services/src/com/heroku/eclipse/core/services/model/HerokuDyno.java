package com.heroku.eclipse.core.services.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HerokuDyno {
	private final String name;
	private final String appName;
	private final String command;
	private final List<HerokuProc> processes = new ArrayList<HerokuProc>();
	
	public HerokuDyno( String name, String appName, String command) {
		this.name = name;
		this.appName = appName;
		this.command = command;
	}
	
	public String getAppName() {
		return appName;
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

	public DynoStateState getState() {
		for( HerokuProc p : getProcesses() ) {
			String s = p.getHerokuProc().getState();
			if( ! "up".equals(s) && ! "idle".equals(s) ) {
				return DynoStateState.UNKNOWN;
			}
		}
		
		return DynoStateState.OK;
	}

	public String getCommand() {
		// TODO Auto-generated method stub
		return null;
	}

}
