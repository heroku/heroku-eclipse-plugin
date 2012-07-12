package com.heroku.eclipse.core.services.junit.common;

public class Credentials {

	// USER#1
	public static final String VALID_JUNIT_USER1 = System.getProperty("heroku.junit.user1") == null ? System.getenv("HEROKU_TEST_USERNAME_1") : System.getProperty("heroku.junit.user1");
	public static final String VALID_JUNIT_PWD1 = System.getProperty("heroku.junit.pwd1") == null ? System.getenv("HEROKU_TEST_PWD_1") : System.getProperty("heroku.junit.pwd1");
	public static final String VALID_JUNIT_APIKEY1 = System.getProperty("heroku.junit.apikey1") == null ? System.getenv("HEROKU_TEST_APIKEY_1") : System.getProperty("heroku.junit.apikey1");
	/**
	 * Maximum amount of processes that this user may have according to his Heroku account 
	 */
	public static final int VALID_JUNIT_SCALE_LIMIT1 = 1;
	
	// USER#2
	public static final String VALID_JUNIT_USER2 = System.getProperty("heroku.junit.user2") == null ? System.getenv("HEROKU_TEST_USERNAME_2") : System.getProperty("heroku.junit.user2");
	public static final String VALID_JUNIT_PWD2 = System.getProperty("heroku.junit.pwd2") == null ? System.getenv("HEROKU_TEST_PWD_2") : System.getProperty("heroku.junit.pwd2");
	public static final String VALID_JUNIT_APIKEY2 = System.getProperty("heroku.junit.apikey2") == null ? System.getenv("HEROKU_TEST_APIKEY_2") : System.getProperty("heroku.junit.apikey2");
	/**
	 * Maximum amount of processes that this user may have according to his Heroku account 
	 */
	public static final int VALID_JUNIT_SCALE_LIMIT2 = 1;
	
	public static final String INVALID_PUBLIC_SSH_KEY1 = "invalidsshkey";
	public static final String INVALID_PUBLIC_SSH_KEY2 = "somewhere over the rainbow";
	public static final String INVALID_PUBLIC_SSH_KEY3 = "what äö&| this";
	
	public static final String VALID_PUBLIC_SSH_KEY1 = "ssh-dss AAAAB3NzaC1kc3MAAACBALIP8L272zL/On5jZOwaDRyQ7a4RdPFzTV3LVSldv/7E2PWMYAfIcSw7ZcObFcUU9MkqZbGCntbaYWvv2ay7M35SuyiSbKurs7LeL3tdiGGDRH3htVVfntpVEBT3qYyHk54c6ozUHfesPDmIgxw0md8uN+sH+5qXiZdbi3dlWZiJAAAAFQCcnPT/VzkWw6ph2YkESKyPNw17vwAAAIAJ5dqvb6l/CFxr4tKILvshST35poaDQKIyran0NAgV0TXldmso1dJUCOFYtrsduIRorcYXdMVrVzJc9wTH9rPrkOgPmzFf6wMaJgkREsfiNFViJVuyKh3bhMFhIaF/AISBLzFdpU3h+yI4WYjDpVwNHfc0u1MAuE7w2LvGNi4V0gAAAIBODauAy96FbIHksOvPVa2phxD6ruRx4iSrcU3RRYGor4O/jdSN+JyfPr0Twh4LozzrmKB8MWMcqw9CmBikXR4bScw90ByvTg97Ca0GSGtpyy/o3+rpju/Y1/qPMPy869FNFiszlc/sYdRtNL+xZuaQ4MK7+i1rv8107lsfoMnCtg== valid_key1";
	public static final String VALID_PUBLIC_SSH_KEY1_DESCRIPTION = "valid_key1";
	public static final String VALID_PUBLIC_SSH_KEY2 = "ssh-dss AAAAB3NzaC1kc3MAAACBAO+k0bjIYbDBXxlVsMqQRDos+Sb9l+pI7bEQQUclymLtjeT5jpn5tdvRyoKvmUo1ssw0BkyJSejzV46Yu+rbBHBjWnIMEsl015Wi65fm1Wgd1cVtT15WOTUD29A0uaeCi/XrjVRNO4Z22BqXzurggEqNpx10MkGNCT9DRIVv45bxAAAAFQD8ah03gF8ypVWAPq/7Vjb1k2E9mwAAAIBtEIW4ALjes/TIHPcBddBmPRYWtg+IXQzAPQlFQukhli0ZeEeYBw0gj6rnMR4MFisadwRUbEW5HtY/68uE7tKy4uJyRGk1/B5XYSqC3FxPgt1DO1gCF21fmtbZMi98EaYQg4DP91TbthNamZ8r1wjxVNvgc+L4/j35Or3sNWxGagAAAIBfUJ9CWvP6Ory7xZ+ET1z+z4ojbqmAB3COcVivjE2p4I02qexSmwuqRiZyLzfXLA6QtCu99Vq/ewRRdfGtvhnJBLKi79GDscz2y4UxAUxxNMWF6WZ8ZBlM5VeE0b6Puft80Vx4ZJLK5IWlj6+DS/q0XDiKqA65nAY5CUDll7k/hw== valid_key2";
	public static final String VALID_PUBLIC_SSH_KEY2_DESCRIPTION = "valid_key2";
	
	
}
