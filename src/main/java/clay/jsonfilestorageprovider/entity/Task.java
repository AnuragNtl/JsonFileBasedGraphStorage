package clay.jsonfilestorageprovider.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import lombok.ToString;

@ToString
public class Task {

	private List<Task> taskList;

	private Map<String, Object> properties = new HashMap<>();

	public List<Task> getTaskList() {
		return taskList;
	}

	public void setTaskList(List<Task> taskList) {
		this.taskList = taskList;
	}

	@JsonAnySetter
	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}

    @JsonAnyGetter
	public Map<String, Object> getProperty() {
		return properties;
	}

    public Object getProperty(String key) {
        return properties.get(key);
    }

	public Map<String, Object> allProperties() {
		return properties;
	}
};

