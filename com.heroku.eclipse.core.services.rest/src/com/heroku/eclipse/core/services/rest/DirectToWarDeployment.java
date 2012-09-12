package com.heroku.eclipse.core.services.rest;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.WarDeploymentService;
import com.herokuapp.directto.client.DeployRequest;
import com.herokuapp.directto.client.DirectToHerokuClient;
import com.herokuapp.directto.client.EventSubscription;
import com.herokuapp.directto.client.EventSubscription.Event;
import com.herokuapp.directto.client.EventSubscription.Subscriber;
import com.herokuapp.directto.client.VerificationException;

public class DirectToWarDeployment implements WarDeploymentService {

	@Override
	public void deploy(String apiKey, String appName, File war) {
		DirectToHerokuClient client= new DirectToHerokuClient.Builder()
			.setApiKey(apiKey)
			.setConsumersUserAgent("heroku-eclipse-plugin/TODO")
			.build();
		
		Map<String,File> files = new HashMap<String,File>();
		files.put("war", war);
		
		DeployRequest req = new DeployRequest("war", appName, files).setEventSubscription(
			new EventSubscription().subscribe(EnumSet.allOf(Event.class), 
				new Subscriber() {
			        public void handle(Event event) {
			            System.out.println(event);
			        }}));
		
		try {
			client.verify(req);
		} catch (VerificationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		client.deploy(req);
	}

}
