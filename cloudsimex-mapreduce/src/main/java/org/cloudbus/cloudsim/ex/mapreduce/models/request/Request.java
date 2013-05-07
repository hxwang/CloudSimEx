package org.cloudbus.cloudsim.ex.mapreduce.models.request;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	public String jobFile;

	public Request(double submissionTime, int deadline, double budget, String jobFile, UserClass userClass) {
		id = Id.pollId(Request.class);
		this.submissionTime = submissionTime;
		this.budget = budget;
		this.deadline = deadline;
		this.jobFile = jobFile;
		this.userClass = userClass;
		firstSubmissionTime = -1;
		lastFinishTime = -1;
		totalCost = 0.0;
		mapAndReduceVmProvisionList = new ArrayList<VmInstance>();
		reduceOnlyVmProvisionList = new ArrayList<VmInstance>();
		schedulingPlan = new HashMap<Integer, Integer>();

		job = readJobYAML(jobFile);
		//Add Extra Map Tasks
		List<MapTask> copyOfMapTasks = new ArrayList<MapTask>(job.mapTasks);
		for (MapTask mapTask : copyOfMapTasks) {
			for(int i=mapTask.extraTasks; i > 0; i--)
				job.mapTasks.add(new MapTask(1,mapTask.dSize,mapTask.mi,mapTask.intermediateData));
		}
		//Set Request Id and data source name in all Map Tasks
		for (MapTask mapTask : job.mapTasks) {
			mapTask.requestId = id;
			mapTask.dataSourceName = job.dataSourceName;
		}
		//Set Request Id and data size in all Reduce Tasks
		for (ReduceTask reduceTask : job.reduceTasks) {
			reduceTask.requestId = id;
			reduceTask.updateDSize(this);
		}
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
		Task task = getTaskFromId(cloudletId, job);
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
	
	
	///// STATIC METHODS ////
	
	public static Task getTaskFromId(int taskId, Job job) {
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
	
	/***
	 * Get VM provisioning plan from a scheduling plan
	 */
	public static ArrayList<ArrayList<VmInstance>> getProvisioningPlan(Map<Integer, Integer> schedulingPlan, List<VmInstance> nVMs, Job job)
	{
		ArrayList<ArrayList<VmInstance>> provisioningPlans = new ArrayList<ArrayList<VmInstance>>(2); //To remove the temporary VMs
		//Index 0 for: mapAndReduceVmProvisionList
		provisioningPlans.add(new ArrayList<VmInstance>());
		//Index 1 for: reduceOnlyVmProvisionList
		provisioningPlans.add(new ArrayList<VmInstance>());
		
		for (Map.Entry<Integer, Integer> entry : schedulingPlan.entrySet()) {
			Task task = job.getTask(entry.getKey());
			if(task instanceof MapTask)
				for (VmInstance vm : nVMs) {
					if (entry.getValue() == vm.getId())
						if (!provisioningPlans.get(0).contains(vm) && !provisioningPlans.get(1).contains(vm))
							provisioningPlans.get(0).add(vm);
				}
			else
				for (VmInstance vm : nVMs) {
					if (entry.getValue() == vm.getId())
						if (!provisioningPlans.get(0).contains(vm) && !provisioningPlans.get(1).contains(vm))
							provisioningPlans.get(1).add(vm);
				}
		}
		
		return provisioningPlans;
	}

}
