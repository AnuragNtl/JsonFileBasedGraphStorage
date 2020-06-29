package com.clay.jsonfilestorageprovider;

import com.clay.graphstorage.converter.GraphOutput;
import com.clay.graphstorage.converter.GraphParser;
import com.clay.graphstorage.entities.Graph;
import com.clay.graphstorage.entities.Node;
import com.clay.graphstorage.entities.NodeProperty;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import clay.jsonfilestorageprovider.entity.Task;
import java.io.FileWriter;
import java.io.PrintWriter;

@Slf4j
public class JsonFileStorageServiceProvider implements GraphParser, GraphOutput {


	public static final String FILE_NAME_KEY = "fileName";

    public static final String DIRECTORY_NAME_KEY = "directoryName";

    private String parserCurrentDirectory = ".";
    
    private String outputCurrentDirectory = ".";
	
	private void writeToFile(String data, String fileName) throws IOException {
		Files.writeString(FileSystems.getDefault().getPath(outputCurrentDirectory, fileName), data, StandardCharsets.UTF_8);
	}

	private String getFileData(String filePath) throws IOException {
		return Files.readString(FileSystems.getDefault().getPath(parserCurrentDirectory, filePath), StandardCharsets.UTF_8);
	}

	private String fileLoadPath;

	@Override
	public void setGraphParserProperties(Map<String, String> properties) {
		if(!properties.containsKey(FILE_NAME_KEY)) {
            if(properties.containsKey(DIRECTORY_NAME_KEY)) {
                parserCurrentDirectory = properties.get(DIRECTORY_NAME_KEY);
            } else {
                throw new RuntimeException("No file name specified");
            }
        } else {	
            fileLoadPath = properties.get(FILE_NAME_KEY);	
        }
	}


	
	@Override
	public Graph loadGraph(Map<String, String> queryParams) {
		try {
            if(fileLoadPath != null)
                return getGraphFromJson(getFileData(fileLoadPath));
            else if(queryParams.containsKey(FILE_NAME_KEY))
                return getGraphFromJson(getFileData(queryParams.get(FILE_NAME_KEY)));
            else
                throw new IOException("File not specified");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String fileOutputPath;

	@Override
	public void setGraphOutputProperties(Map<String, String> properties) {
        if(!properties.containsKey(FILE_NAME_KEY)) {
            if(properties.containsKey(DIRECTORY_NAME_KEY)) {
                outputCurrentDirectory = properties.get(DIRECTORY_NAME_KEY);
            } else {
                throw new RuntimeException("No file name specified");
            }
        } else {
            fileOutputPath = properties.get(FILE_NAME_KEY);	
        }
	}


	@Override
	public void outputGraph(Graph graph, Map<String, String> outputProperties) {
		Node rootNode = graph.getRootNode();
		Task rootTask = convertNodeToTask(rootNode);
		ObjectMapper objectMapper = getConfiguredObjectMapper();
		try {
            if(fileOutputPath != null)
                writeToFile(objectMapper.writeValueAsString(rootTask), fileOutputPath);
            else if(outputProperties.containsKey(FILE_NAME_KEY))
                writeToFile(objectMapper.writeValueAsString(rootTask), outputProperties.get(FILE_NAME_KEY));
            else
                throw new IOException("File not specified");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Task convertNodeToTask(Node node) {

		Task task = new Task();
		Map<String, NodeProperty<?> > nodeProperties = node.getProperties();
		nodeProperties.keySet().forEach(propertyKey -> task.setProperty(propertyKey, nodeProperties.get(propertyKey).getValue()));
		List<Node> connectsToList = node.getConnectsToList();	
		task.setTaskList(connectsToList.stream().map(connectsToNode -> convertNodeToTask(connectsToNode)).collect(Collectors.toList()));
		return task;
	}
	
	private Graph getGraphFromJson(String data) {
		Task task = getInnerModelFromJson(data);	
        log.info("Got task from JSON file: {}" , task);
		Graph graph = new Graph();
		Node rootNode = convertTaskToNode(graph, null, task);
		graph.setRootNode(rootNode);
        log.info("Equivalent graph: {}" , graph);
		return graph;
	}

	private Node convertTaskToNode(Graph graph, Node comesFrom, Task task) {
		Object id = task.getProperty("id");
		Node node = new Node(graph, comesFrom, id != null ? Long.valueOf(id.toString()) : null);
		Map<String, Object> taskProperties = task.allProperties();
		taskProperties.keySet().forEach(taskPropertyKey -> node.addProperty(taskPropertyKey, taskProperties.get(taskPropertyKey), Object.class));
		if(task.getTaskList() != null) {
			List<Node> subNodes = task.getTaskList().stream().map(subtask -> convertTaskToNode(graph, node, subtask)).collect(Collectors.toList());
			node.setConnectsToNodesAndUpdateComesFromInEach(subNodes, graph);		
		}
		return node; 
	}

	private Task getInnerModelFromJson(String data) {
		try {
			Task task = getConfiguredObjectMapper().readValue(data, Task.class);
			return task;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static ObjectMapper objectMapper = null;
	private static ObjectMapper getConfiguredObjectMapper() {
		if(objectMapper == null) {
			objectMapper = new ObjectMapper();
			objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
		return objectMapper;
	}

    public List<String> getAllGraphIds() {
        return Arrays.asList(new File(parserCurrentDirectory).listFiles()).stream()
            .map(file -> file.getName())
            .filter(fileName -> fileName.toLowerCase().endsWith(".json"))
            .collect(Collectors.toList());
    }
};

