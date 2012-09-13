package com.heroku.eclipse.core.services.rest;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.heroku.eclipse.core.services.WarDeploymentService;
import com.herokuapp.directto.client.DeployRequest;
import com.herokuapp.directto.client.DirectToHerokuClient;
import com.herokuapp.directto.client.EventSubscription;
import com.herokuapp.directto.client.EventSubscription.Event;
import com.herokuapp.directto.client.EventSubscription.Subscriber;
import com.herokuapp.directto.client.VerificationException;

public class DirectToWarDeployment implements WarDeploymentService {

	@Override
	public void deploy(final IProgressMonitor pm, String apiKey, String appName, File war) {
		final DirectToHerokuClient client= new DirectToHerokuClient.Builder()
			.setApiKey(apiKey)
			.setConsumersUserAgent("heroku-eclipse-plugin/TODO")
			.build();
		
		final Map<String,File> files = new HashMap<String,File>();
		files.put("war", war);
		
		final DeployRequest req = new DeployRequest("war", appName, files).setEventSubscription(new EventSubscription()
			.subscribe(EnumSet.allOf(Event.class), new Subscriber() {
				
				final int MAX_PROGRESS = 10;
				final int MAX_PROGRESS_BUFFER = 2;
				int progress = 0;
				
				void incrementProgress() {
					if (progress < MAX_PROGRESS - MAX_PROGRESS_BUFFER) {
						pm.worked(progress++);
					}
				}
				
				@Override
				public void handle(Event event) {
					System.out.println(event.name());
					
					switch(event) {
						case DEPLOY_PRE_VERIFICATION_START: pm.beginTask("Verifying...", MAX_PROGRESS);  break;
						case UPLOAD_START:                  pm.subTask("Uploading...");                  break;
						case POLL_START:                    pm.subTask("Deploying...");                  break;
						case POLLING:                                                                    break;
						case DEPLOY_END:                    pm.done();                                   return;
						default:                                                                         return;
					}
					
					incrementProgress();
				}
			})
		);
		
		try {
			client.verify(req);
		} catch (VerificationException e) {
			e.printStackTrace();
			throw new RuntimeException(e); // TODO show proper error message
		}
		client.deploy(req);
	}
}
