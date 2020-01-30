package com.overops.plugins.sonar.model;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.takipi.api.client.data.event.Location;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
import com.takipi.api.client.util.regression.RegressionStringUtil;

import org.apache.commons.lang.StringUtils;

public class Event {
	public static final String FIELDS = "id,stack_frames,link,name,typeMessage,introduced_by,labels";

	private int id;
	private String link;
	private String name;
	private String typeMessage;
	private String introducedBy;
	private String labels;
	private String stackFrames;
	private Location location;
	private String filePath;
	private String deployment;
	private String criticalExceptionTypes;

	public Event(Series<SeriesRow> events, int index, String deployment, String criticalExceptionTypes, String appUrl) {
		this.deployment = deployment;
		this.criticalExceptionTypes = criticalExceptionTypes;

		// order determined by Event.FIELDS: id,stack_frames,link,name,typeMessage,introduced_by
		id = Integer.parseInt((String) events.getValue(0, index)); // id
		stackFrames  = (String) events.getValue(1, index);         // stack_frames
		link         = (String) events.getValue(2, index);         // link
		name         = (String) events.getValue(3, index);         // name
		typeMessage  = (String) events.getValue(4, index);         // typeMessage
		introducedBy = (String) events.getValue(5, index);         // introduced_by
		labels       = (String) events.getValue(6, index);         // labels

		// parse stackFrames
		if (StringUtils.isBlank(stackFrames)) {
			throw new IllegalArgumentException("Missing stack_frames for event id " + id);
		}

		// format link
		link = appUrl + "/tale.html?snapshot=" + link + "&source=70";

		Type listType = new TypeToken<ArrayList<Location>>(){}.getType();
		List<Location> locationList = new Gson().fromJson(stackFrames, listType);

		if (locationList.size() < 1) {
			throw new IllegalArgumentException("Missing location for event id " + id);
		}

		// there is only one location in the list
		location = locationList.get(0);

		// location.class_name;
		// location.original_line_number;

		// match **/com/example/path/ClassName.java
		StringBuilder matchPattern = new StringBuilder("**");
		matchPattern.append(File.separator);
		matchPattern.append(location.class_name.replace(".", File.separator));
		matchPattern.append(".java");

		filePath = matchPattern.toString();
	}

	public int getId() {
		return id;
	}
	public String getLink() {
		return link;
	}
	public String getName() {
		return name;
	}
	public String getTypeMessage() {
		return typeMessage;
	}
	public String getIntroducedBy() {
		return introducedBy;
	}
	public String getLabels() {
		return labels;
	}
	public Location getLocation() {
		return location;
	}
	public String getFilePath() {
		return filePath;
	}
	public String getDeployment() {
		return deployment;
	}

	// makes adding to hashmap more clear
	public String getKey() {
		return getFilePath();
	}

	// quality gates (see com.takipi.api.client.util.cicd.ProcessQualityGates)
	public boolean isNew() {
		return deployment.contains(introducedBy);
	}
	public boolean isCritical() {
		return criticalExceptionTypes.contains(name);
	}
	public boolean isResurfaced() {
		return labels.contains(RegressionStringUtil.RESURFACED_ISSUE);
	}

	// friendly, formatted issue message
	public String getMessage() {
		StringBuilder sb = new StringBuilder();

		if (isNew()) sb.append("New, ");
		if (isCritical()) sb.append("Critical, ");
		if (isResurfaced()) sb.append("Resurfaced, ");

		sb.append(typeMessage);
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}

		Event that = (Event) obj;
		return this.id == that.getId();
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public String toString() {
		return "Event [criticalExceptionTypes=" + criticalExceptionTypes + ", deployment=" + deployment + ", filePath="
				+ filePath + ", id=" + id + ", introducedBy=" + introducedBy + ", labels=" + labels + ", link=" + link
				+ ", location=" + location + ", name=" + name + ", stackFrames=" + stackFrames + ", typeMessage=" + typeMessage
				+ "]";
	}
}
