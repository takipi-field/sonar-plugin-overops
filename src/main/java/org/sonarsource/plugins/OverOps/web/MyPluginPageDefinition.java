package org.sonarsource.plugins.OverOps.web;

import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.Page.Scope;
import org.sonar.api.web.page.PageDefinition;

public class MyPluginPageDefinition implements PageDefinition {

	@Override
	public void define(Context context) {
		context.addPage(Page.builder("overops/custom_page_4_project").setName("Custom Project Page (Pure JS)")
				.setScope(Scope.COMPONENT).build())

				.addPage(Page.builder("overops/measures_history").setName("Custom Project Page using ReactJS")
						.setScope(Scope.COMPONENT).build())

				.addPage(Page.builder("overops/custom_page_4_admin").setName("Custom Admin Page").setScope(Scope.GLOBAL)
						.setAdmin(Boolean.TRUE).build())

				.addPage(Page.builder("overops/sanity_check").setName("Custom Admin Page Sanity Check")
						.setScope(Scope.GLOBAL).setAdmin(Boolean.TRUE).build())

				.addPage(Page.builder("overops/custom_page_global").setName("Custom Global Page").setScope(Scope.GLOBAL)
						.build())

				.addPage(Page.builder("overops/overops_config_form").setName("OverOps Configuration")
						.setScope(Scope.COMPONENT).setAdmin(Boolean.FALSE).build());
	}
}
