package com.yugabyte.simulation.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yugabyte.simulation.dao.InvocationResult;
import com.yugabyte.simulation.model.YBServerModel;
import com.yugabyte.simulation.model.ybm.NodeInfo;
import com.yugabyte.simulation.model.ybm.NodeInfoCloudInfo;
import com.yugabyte.simulation.model.ybm.YbmNodeListResponseModel;

@RestController
public class YBMCloudApiController {
    @Value("${ybm.account-id}")
    private String accountId;

    @Value("${ybm.api-key}")
    private String apiKey;

    @Value("${ybm.project-id}")
    private String projectId;

    @Value("${ybm.cluster-id}")
    private String clusterId;

    @Value("${ybm.baseuri}")
    private String baseUri;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping("/api/ybm/nodes")
    public ResponseEntity<YbmNodeListResponseModel> getListOfNodesApi(@RequestParam(name = "accountid", required = false) String aAccountId
            , @RequestParam(name = "projectid", required = false) String aProjectId
            , @RequestParam(name = "clusterid", required = false) String aClusterId
            ){

        // If the accountid, projectid and clusterid are not coming as part of the request, we will use the one provided by java params or yaml file
        if(aAccountId == null){
            aAccountId = accountId;
        }
        if(aProjectId == null){
            aProjectId = projectId;
        }
        if(aClusterId == null){
            aClusterId = clusterId;
        }

        YbmNodeListResponseModel model = getNodeList(aAccountId,aProjectId,aClusterId);
        System.out.println("model:"+model);

        if(model == null){
            return new ResponseEntity<>(new YbmNodeListResponseModel(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @GetMapping("/api/ybm/projects")
    public ResponseEntity<String> getProjects(@RequestParam(name = "accountid", required = false) String aAccountId){
        if(aAccountId == null){
            aAccountId = accountId;
        }
        StringBuilder sb = new StringBuilder(baseUri);
        sb.append("/").append(aAccountId).append("/projects");

        String responseFromCall = null;

        try {
            responseFromCall = webClientBuilder.build()
                    .get()
                    .uri(sb.toString())
                    .header("Authorization","Bearer "+apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            responseFromCall = "error:"+e.getMessage();
            return new ResponseEntity<>(responseFromCall,HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(responseFromCall,HttpStatus.OK);
    }

    @GetMapping("/api/ybm/clusters")
    public ResponseEntity<String> getClusters(@RequestParam(name = "accountid", required = false) String aAccountId
                                ,@RequestParam(name = "projectid", required = false) String aProjectId){
        // If the accountid, projectid are not coming as part of the request, we will use the one provided by java params or yaml file
        if(aAccountId == null){
            aAccountId = accountId;
        }
        if(aProjectId == null){
            aProjectId = projectId;
        }
        StringBuilder sb = new StringBuilder(baseUri);
        sb.append("/").append(aAccountId).append("/projects/").append(aProjectId).append("/clusters");

        String responseFromCall = null;
        try {
            responseFromCall = webClientBuilder.build()
                    .get()
                    .uri(sb.toString())
                    .header("Authorization","Bearer "+apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            responseFromCall = "error:"+e.getMessage();
            return new ResponseEntity<>(responseFromCall,HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(responseFromCall, HttpStatus.OK);
    }


    @GetMapping("/api/ybm/restartnodes")
    public ResponseEntity<List<String>>  restartNodes(@RequestParam(name = "accountid", required = false) String aAccountId
            ,@RequestParam(name = "projectid", required = false) String aProjectId
            ,@RequestParam(name = "clusterid", required = false) String aClusterId){

        // If the accountid, projectid and clusterid are not coming as part of the request, we will use the one provided by java params or yaml file
        if(aAccountId == null){
            aAccountId = accountId;
        }
        if(aProjectId == null){
            aProjectId = projectId;
        }
        if(aClusterId == null){
            aClusterId = clusterId;
        }

        List<String> response = new ArrayList<>();
        // Lets first find the list of stopped nodes.
        YbmNodeListResponseModel model = getNodeList(aAccountId,aProjectId,aClusterId);
        List<String> stoppedNodes = new ArrayList<>();

        if(model != null && model.data != null){
            for(int i = 0; i < model.data.size(); i++){
                if(!model.data.get(i).is_node_up){
                    stoppedNodes.add(model.data.get(i).name);
                }
            }
        }
        else{
            response.add("I couldn't find the stopped node");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        System.out.println("Number of stopped nodes:"+stoppedNodes.size());

        if(stoppedNodes.size() == 0){
            response.add("All Nodes are Up and nothing to restart!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        boolean errorEncountered = false;
        // We will start the stopped nodes now.
        for(String nodeName: stoppedNodes){
            StringBuilder sbUriToStartNodes = new StringBuilder(baseUri);
            sbUriToStartNodes.append("/").append(aAccountId).append("/projects/").append(aProjectId).append("/clusters/").append(aClusterId).append("/node-ops");
            String body = "{\n" +
                    "  \"action\": \"START\",\n" +
                    "  \"node_name\": \""+nodeName+"\"\n" +
                    "}";
            String responseFromCall = null
                    ;
            try {
                responseFromCall = webClientBuilder.build()
                        .post()
                        .uri(sbUriToStartNodes.toString())
                        .header("Authorization","Bearer "+apiKey)
                        .header("Content-Type","application/json")
                        .body(BodyInserters.fromValue(body))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } catch (Exception e) {
                responseFromCall = "{ \"error\": \""+e.getMessage()+"\"}";
                errorEncountered = true;
            }
            response.add(responseFromCall);
            System.out.println("responseFromCall:"+responseFromCall);
        }
        return new ResponseEntity<>(response, errorEncountered?HttpStatus.BAD_REQUEST:HttpStatus.OK);

    }

    @GetMapping("/api/ybm/stopnodes")
        public ResponseEntity<List<String>> stopNodes(@RequestParam(name = "nodenames", required = true) String nodenames
                                    ,@RequestParam(name = "accountid", required = false) String aAccountId
                                    ,@RequestParam(name = "projectid", required = false) String aProjectId
                                    ,@RequestParam(name = "clusterid", required = false) String aClusterId){

        // If the accountid, projectid and clusterid are not coming as part of the request, we will use the one provided by java params or yaml file
        if(aAccountId == null){
            aAccountId = accountId;
        }
        if(aProjectId == null){
            aProjectId = projectId;
        }
        if(aClusterId == null){
            aClusterId = clusterId;
        }

        String []nodeList =nodenames.split(",");

        List<String> listOfResponses = new ArrayList<>();
        // Lets first find the list of stopped nodes.
        StringBuilder sbUri = new StringBuilder(baseUri);
        sbUri.append("/").append(aAccountId).append("/projects/").append(aProjectId).append("/clusters/").append(aClusterId).append("/node-ops");

        boolean errorEncountered = false;

        for(String nodename: nodeList){
            String body = "{\n" +
                    "  \"action\": \"STOP\",\n" +
                    "  \"node_name\": \""+nodename+"\"\n" +
                    "}";
            String responseFromCall = null;
            try{
                responseFromCall = webClientBuilder.build()
                        .post()
                        .uri(sbUri.toString())
                        .header("Authorization","Bearer "+apiKey)
                        .header("Content-Type","application/json")
                        .body(BodyInserters.fromValue(body))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block()
                        ;
                System.out.println("responseFromCall:"+responseFromCall);

            }
            catch(Exception e){
                e.printStackTrace();
                responseFromCall = "{ \"error\": \""+e.getMessage()+"\"}";
                errorEncountered = true;
            }
            listOfResponses.add(responseFromCall);

        }

        return new ResponseEntity<>(listOfResponses,errorEncountered?HttpStatus.BAD_REQUEST:HttpStatus.OK);
    }


    @GetMapping("/api/ybm/scale")
    public InvocationResult scaleCluster(@RequestParam(name = "accountid", required = false) String aAccountId
            ,@RequestParam(name = "projectid", required = false) String aProjectId
            ,@RequestParam(name = "clusterid", required = false) String aClusterId
            ,@RequestParam(name = "numnodes", required = true) int numnodes){

        if(aAccountId == null){
            aAccountId = accountId;
        }
        if(aProjectId == null){
            aProjectId = projectId;
        }
        if(aClusterId == null){
            aClusterId = clusterId;
        }
        String responseFromCall = null;

        // Lets get the cluster details first. We will use this info to send a put request.
        String clusterInfoJson = getClusterDetails(aAccountId,aProjectId,aClusterId);
        /*
        sample for reference:
        clusterInfoJson:{"data":{"spec":{"name":"dynamic-ermine","cloud_info":{"code":"AWS","region":"us-east-2"},"cluster_info":{"cluster_tier":"PAID","cluster_type":"SYNCHRONOUS","num_nodes":3,"fault_tolerance":"ZONE","node_info":{"num_cores":8,"memory_mb":32768,"disk_size_gb":400},"is_production":true,"version":34},"network_info":{"single_tenant_vpc_id":"998c0742-8894-41eb-8ee8-3a2fcd8e474b"},"software_info":{"track_id":"d9618d5e-9591-445b-a280-705770f5fb30"},"cluster_region_info":[{"placement_info":{"cloud_info":{"code":"AWS","region":"us-east-2"},"num_nodes":3,"vpc_id":"998c0742-8894-41eb-8ee8-3a2fcd8e474b","num_replicas":null,"multi_zone":true},"is_default":true,"is_affinitized":true}]},"info":{"id":"250c053d-921e-481c-9143-754f6f906377","state":"Active","endpoint":"us-east-2.250c053d-921e-481c-9143-754f6f906377.aws.ybdb.io","endpoints":{"us-east-2":"us-east-2.250c053d-921e-481c-9143-754f6f906377.aws.ybdb.io"},"project_id":"773f1a68-4efa-46a5-9a60-87c3b0f045df","software_version":"2.14.1.0-b36","cluster_network_version":"V2","read_replica_info":[],"metadata":{"created_on":"2022-09-20T20:15:51.180Z","updated_on":"2022-09-22T20:31:04.692Z"}}}}
         */
        System.out.println("clusterInfoJson:"+clusterInfoJson);

        try{
            // Need to clean this  code. It is such a kludge right now.
            ObjectNode node = new ObjectMapper().readValue(clusterInfoJson, ObjectNode.class);
            JsonNode specNode = node.get("data").get("spec");
            ObjectNode clusterInfo = (ObjectNode)specNode.get("cluster_info");
            clusterInfo.put("num_nodes",numnodes);

            JsonNode clusterRegionInfo =  specNode.get("cluster_region_info");
            if(clusterRegionInfo.isArray()){
                for(JsonNode arrNode: clusterRegionInfo){
                    ((ObjectNode)arrNode.get("placement_info")).put("num_nodes",numnodes);
                }
            }
//            System.out.println(specNode.toPrettyString());

            // this is good call body example
            //String test = "{\n  \"cloud_info\": {\n    \"code\": \"AWS\",\n    \"region\": \"us-east-2\"\n  },\n  \"cluster_info\": {\n    \"cluster_tier\": \"PAID\",\n    \"cluster_type\": \"SYNCHRONOUS\",\n    \"fault_tolerance\": \"ZONE\",\n    \"is_production\": true,\n    \"node_info\": {\n      \"disk_size_gb\": 400,\n      \"memory_mb\": 32768,\n      \"num_cores\": 8\n    },\n    \"num_nodes\": 6,\n    \"version\": 34\n  },\n  \"cluster_region_info\": [\n    {\n      \"is_affinitized\": true,\n      \"is_default\": true,\n      \"placement_info\": {\n        \"cloud_info\": {\n          \"code\": \"AWS\",\n          \"region\": \"us-east-2\"\n        },\n        \"multi_zone\": true,\n        \"num_nodes\": 6,\n        \"num_replicas\": null,\n        \"vpc_id\": \"998c0742-8894-41eb-8ee8-3a2fcd8e474b\"\n      }\n    }\n  ],\n  \"name\": \"dynamic-ermine\",\n  \"network_info\": {\n    \"single_tenant_vpc_id\": \"998c0742-8894-41eb-8ee8-3a2fcd8e474b\"\n  },\n  \"software_info\": {\n    \"track_id\": \"d9618d5e-9591-445b-a280-705770f5fb30\"\n  }\n}";
            // we are ready to make the PUT call now
            StringBuffer sb = new StringBuffer(baseUri);
            sb.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters/").append(clusterId);
            responseFromCall = webClientBuilder.build()
                    .put()
                    .uri(sb.toString())
                    .header("Authorization","Bearer "+apiKey)
                    .header("Content-Type","application/json")
                    .bodyValue(specNode.toPrettyString())
                    //.body(BodyInserters.fromValue(specNode.toString()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block()
            ;
            System.out.println("responseFromCall:"+responseFromCall);
        }
        catch (Exception e){
        	System.err.printf("Error scaling cluster, response was %s\n", e.getMessage());
            e.printStackTrace();
            return new InvocationResult(e);
        }
        return new InvocationResult(responseFromCall);
    }


    public YbmNodeListResponseModel getNodeList(String accountId, String projectId, String clusterId){
        StringBuilder sbUri = new StringBuilder(baseUri);
        sbUri.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters/").append(clusterId).append("/nodes");
        YbmNodeListResponseModel model = null;
        try {
            model = webClientBuilder.build()
                    .get()
                    .uri(sbUri.toString())
                    .header("Authorization","Bearer "+apiKey)
                    .retrieve()
                    .bodyToMono(YbmNodeListResponseModel.class)
                    .block();
        } catch (Exception e) {
            System.out.println("error:"+e.getMessage());
        }
        return model;
    }

    public List<YBServerModel> getNodeListForTopology(){
        if(accountId == null || projectId == null || clusterId == null){
            return null;
        }
        List<YBServerModel> result = new ArrayList<>();
        YbmNodeListResponseModel model = getNodeList(accountId,projectId,clusterId);
        if(model != null){
            List<NodeInfo> list = model.data;
            for(NodeInfo node: list){
                try {
                    YBServerModel ybServerModel = new YBServerModel();
                    NodeInfoCloudInfo nodeInfoCloudInfo = node.cloud_info;
                    String nodeName = node.name;
                    ybServerModel.setHost(nodeName);
                    ybServerModel.setRegion(nodeInfoCloudInfo.region);
                    ybServerModel.setZone(nodeInfoCloudInfo.zone);
                    ybServerModel.setNodeUp(node.is_node_up);
                    ybServerModel.setMaster(node.is_master);
                    ybServerModel.setTserver(node.is_tserver);
                    ybServerModel.setReadReplica(node.is_read_replica);
                    result.add(ybServerModel);
                } catch (Exception e) {
                    System.out.println("error:"+e.getMessage());
                }
            }
        }
        return result;


    }

    private String getClusterDetails(String accountId, String projectId, String clusterId){
        StringBuilder sb = new StringBuilder(baseUri);
        sb.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters/").append(clusterId);

        String responseFromCall = null;

        try {
            responseFromCall = webClientBuilder.build()
                    .get()
                    .uri(sb.toString())
                    .header("Authorization","Bearer "+apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            responseFromCall = "error:"+e.getMessage();
        }

        return responseFromCall;
    }


}
