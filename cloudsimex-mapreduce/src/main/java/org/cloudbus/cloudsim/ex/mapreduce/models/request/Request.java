package org.cloudbus.cloudsim.ex.mapreduce.models.request;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.ex.mapreduce.models.cloud.VmInstance;
import org.cloudbus.cloudsim.ex.mapreduce.models.cloud.VmType;
import org.cloudbus.cloudsim.ex.util.Id;
import org.yaml.snakeyaml.Yaml;

public class Request extends SimEvent {
	public int id;
	public double submissionTime;
	public double budget;
	public int deadline;
	public Job job;
	public UserClass userClass;
	public double firstSubmissionTime;
	public double lastFinishTime;
	
	public List<VmInstance> mapAndReduceVmProvisionList;
	public List<VmInstance> reduceOnlyVmProvisionList;
	
	public Map<Integer, Integer> schedulingPlan; //<Task ID, VM ID>
	public double totalCost;
	
	public String policy;

	public Request(double submissionTime, double budget, int deadline, String jobFile, String policy, UserClass userClass) {
		id = Id.pollId(Request.class);
		this.submissionTime = submissionTime;
		this.budget = budget;
		this.deadline = deadline;
		this.userClass = userClass;
		job = readJobYAML(jobFile);
		firstSubmissionTime = -1;
		lastFinishTime = -1;
		totalCost = 0.0;
		
		mapAndReduceVmProvisionList = new ArrayList<VmInstance>();
		reduceOnlyVmProvisionList = new ArrayList<VmInstance>();
		
		schedulingPlan = new HashMap<Integer, Integer>();
		
		this.policy = policy;

		
		for (MapTask mapTask : job.mapTasks) {
			mapTask.requestId = id;
			mapTask.dataSourceName = job.dataSourceName;
		}

		for (ReduceTask reduceTask : job.reduceTasks) {
			reduceTask.requestId = id;
			
			//set the dSize for reduce tasks
			reduceTask.updateDSize(this);
		}
	}

	public Task getTaskFromId(int taskId) {
		for (MapTask mapTask : job.mapTasks) {
			if (mapTask.getCloudletId() == taskId)
				return mapTask;
		}

		for (ReduceTask reduceTask : job.reduceTasks) {
			if (reduceTask.getCloudletId() == taskId)
				return reduceTask;
		}

		return null;
	}

	private Job readJobYAML(String jobFile) {
		Job job = new Job();

		Yaml yaml = new Yaml();
		InputStream document = null;

		try {
			document = new FileInputStream(new File(jobFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		job = (Job) yaml.load(document);

		return job;
	}

	public boolean isTaskInThisRequest(int cloudletId) {
		Task task = getTaskFromId(cloudletId);
		if(task == null)
			return false;
		else
			return true;
	}
	
	public VmInstance getProvisionedVmFromTaskId(int TaskId)
	{
		int vmInstanceId = -1;
		if(schedulingPlan.containsKey(TaskId))
			vmInstanceId = schedulingPlan.get(TaskId);
		else
			return null;
		
		return getProvisionedVm(vmInstanceId);
	}
	
	public VmInstance getProvisionedVm(int vmInstanceId)
	{
		for (VmInstance vmInstance : mapAndReduceVmProvisionList) {
			if(vmInstance.getId() == vmInstanceId)
				return vmInstance;
		}
		
		for (VmInstance vmInstance : reduceOnlyVmProvisionList) {
			if(vmInstance.getId() == vmInstanceId)
				return vmInstance;
		}
		
		return null;
	}

}
