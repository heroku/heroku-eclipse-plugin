package com.heroku.eclipse.core.services.rest;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.log.LogService;

import com.heroku.eclipse.core.services.WarDeploymentService;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.herokuapp.directto.client.DeployRequest;
import com.herokuapp.directto.client.DeploymentException;
import com.herokuapp.directto.client.DirectToHerokuClient;
import com.herokuapp.directto.client.EventSubscription;
import com.herokuapp.directto.client.EventSubscription.Event;
import com.herokuapp.directto.client.EventSubscription.Subscriber;
import com.herokuapp.directto.client.VerificationException;

public class DirectToWarDeployment implements WarDeploymentService {

	@Override
	public void deploy(final ProgressMonitor pm, final String apiKey, final String appName, final File war) throws HerokuServiceException {
		final DirectToHerokuClient client= new DirectToHerokuClient.Builder()
			.setApiKey(apiKey)
			.setConsumersUserAgent("heroku-eclipse-plugin/TODO")
			.build();
		
		final Map<String,File> files = new HashMap<String,File>();
		files.put("war", war);
		
		final DeployRequest req = new DeployRequest("war", appName, files).setEventSubscription(new EventSubscription()
			.subscribe(EnumSet.allOf(Event.class), new Subscriber() {
				@Override
				public void handle(Event event) {
					Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "event=" + event.name() + " appName=" + appName + " war=" + war);
					
					switch(event) {
						case DEPLOY_PRE_VERIFICATION_START: pm.start(); pm.preparing(); break;
						case UPLOAD_START:                  pm.uploading();             break;
						case POLL_START:                    pm.deploying();             break;
						case DEPLOY_END:                    pm.done();                  break;
					}
				}
			})
		);
		
		try {
			client.verify(req);
		} catch (VerificationException e) {
			final StringBuilder msgs = new StringBuilder();
			for (String msg : e.getMessages()) {
				msgs.append(msg + "; ");
			}
			
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "WAR deployment failed to app " + appName + "\n" + msgs, e);
			throw new HerokuServiceException(msgs.toString(), e);
		}
		
		try {
			client.deploy(req);
		} catch (DeploymentException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "WAR deployment failed to app " + appName + "\n" + e.getDetails(), e);
			throw new HerokuServiceException(e.getDetails(), e);
		}
	}
}
