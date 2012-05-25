package com.heroku.eclipse.core.services.model;

/**
 * Simple bean representing an App template  
 * @author udo.rader@bestsolution.at
 */
public class AppTemplate {
	private String language;
	private long id;
	private String displayName;
	private String templateName;
	private String templateDescription = ""; //$NON-NLS-1$
	private String frameworksLibs = ""; //$NON-NLS-1$
	private String addons = ""; //$NON-NLS-1$
	private String buildType = ""; //$NON-NLS-1$
	
	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}
	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}
	/**
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * @return the templateName
	 */
	public String getTemplateName() {
		return templateName;
	}
	/**
	 * @param templateName the templateName to set
	 */
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	/**
	 * @return the templateDescription
	 */
	public String getTemplateDescription() {
		return templateDescription;
	}
	/**
	 * @param templateDescription the templateDescription to set
	 */
	public void setTemplateDescription(String templateDescription) {
		this.templateDescription = templateDescription;
	}
	/**
	 * @return the frameworksLibs
	 */
	public String getFrameworksLibs() {
		return frameworksLibs;
	}
	/**
	 * @param frameworksLibs the frameworksLibs to set
	 */
	public void setFrameworksLibs(String frameworksLibs) {
		this.frameworksLibs = frameworksLibs;
	}
	/**
	 * @return the addons
	 */
	public String getAddons() {
		return addons;
	}
	/**
	 * @param addons the addons to set
	 */
	public void setAddons(String addons) {
		this.addons = addons;
	}
	/**
	 * @return the buildType
	 */
	public String getBuildType() {
		return buildType;
	}
	/**
	 * @param buildType the buildType to set
	 */
	public void setBuildType(String buildType) {
		this.buildType = buildType;
	}
}
