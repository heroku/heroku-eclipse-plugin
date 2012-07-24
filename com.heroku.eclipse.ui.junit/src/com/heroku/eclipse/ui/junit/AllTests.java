package com.heroku.eclipse.ui.junit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	Preferences.class, 
//	NewFromTemplate.class,
//	HerokuView.class,
//	Delete.class,
//	Import.class,
//	CleanUp.class,
})
public class AllTests {

}
