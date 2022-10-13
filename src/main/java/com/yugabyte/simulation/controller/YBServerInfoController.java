package com.yugabyte.simulation.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.yugabyte.simulation.dao.WorkloadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.yugabyte.simulation.dao.YBServerInfoDAO;
import com.yugabyte.simulation.model.YBServerModel;
import com.yugabyte.simulation.services.TimerService;

@RestController
public class YBServerInfoController {
    @Autowired
    private YBServerInfoDAO ybServerInfoDAO;

	@Value("${ybm.pulltopologyfromapi}")
	private boolean pullTopologyFromApi;

	@Autowired
	private YBMCloudApiController ybmCloudApiController;

    @GetMapping("/api/ybserverinfo")
    public List<YBServerModel> getYBServerInfo(){
		if(pullTopologyFromApi){
			List<YBServerModel> list = ybmCloudApiController.getNodeListForTopology();
			if(list != null && !list.isEmpty()){
				return list;
			}

		}
    	return ybServerInfoDAO.getAll();
    }

    @GetMapping("/api/ybserverinfo/{target}")
    public List<YBServerModel> getYBServerInfo(@PathVariable(name = "target", required =  true) String target){
    	switch (target) {
    	case "YBA":
			return ybServerInfoDAO.getAll();
    	case "YBM":
			List<YBServerModel> list = ybmCloudApiController.getNodeListForTopology();
			if(list != null && !list.isEmpty()){
				return list;
			}
			else {
		    	return ybServerInfoDAO.getAll();
			}
		default:
	    	return ybServerInfoDAO.getAll();
    	}
    }

    private volatile boolean hasNode6 = true;
    @GetMapping("/api/node6visible/{visible}")
    public void setNode6Visible(@PathVariable(name = "visible")boolean isVisible) {
    	this.hasNode6 = isVisible;
    }
    
    @GetMapping("/api/ybserverinfoSample")
//    @GetMapping("/api/ybserverinfo")
    public List<YBServerModel> getYBServerInfoSample(){
    	List<YBServerModel> list = new ArrayList<YBServerModel>();
    	YBServerModel model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.1");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.2");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.3");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.4");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.5");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1a");
    	list.add(model);

    	if (hasNode6 ) {
	    	model = new YBServerModel();
	    	model.setCloud("aws");
	    	model.setHost("127.0.0.6");
	    	model.setPort("9100");
	    	model.setRegion("us-west-1");
	    	model.setZone("us-west-1b");
	    	list.add(model);
    	}
    	
    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.7");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1b");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.8");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1b");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.9");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1b");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.10");
    	model.setPort("9100");
    	model.setRegion("us-west-1");
    	model.setZone("us-west-1b");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.11");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.12");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.13");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.14");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.15");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1a");
    	list.add(model);
    	
    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.16");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1c");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.17");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1c");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.18");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1c");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.19");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1c");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.20");
    	model.setPort("9100");
    	model.setRegion("us-east-1");
    	model.setZone("us-east-1c");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.21");
    	model.setPort("9100");
    	model.setRegion("us-south-1");
    	model.setZone("us-south-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.22");
    	model.setPort("9100");
    	model.setRegion("us-south-1");
    	model.setZone("us-south-1a");
    	list.add(model);
    	
    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.23");
    	model.setPort("9100");
    	model.setRegion("us-south-1");
    	model.setZone("us-south-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.24");
    	model.setPort("9100");
    	model.setRegion("us-south-1");
    	model.setZone("us-south-1a");
    	list.add(model);

    	model = new YBServerModel();
    	model.setCloud("aws");
    	model.setHost("127.0.0.25");
    	model.setPort("9100");
    	model.setRegion("us-south-1");
    	model.setZone("us-south-1a");
    	list.add(model);

    	return list;
    }
}
