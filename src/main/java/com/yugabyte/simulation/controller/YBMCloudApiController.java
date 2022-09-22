package com.yugabyte.simulation.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

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
    public YbmNodeListResponseModel getListOfNodesApi(@RequestParam(name = "accountid", required = false) String aAccountId
            ,@RequestParam(name = "projectid", required = false) String aProjectId
            ,@RequestParam(name = "clusterid", required = false) String aClusterId
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

        return getNodeList(aAccountId,aProjectId,aClusterId);
    }

    @GetMapping("/api/ybm/projects")
    public String getProjects(@RequestParam(name = "accountid", required = false) String aAccountId){
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
        }

        return responseFromCall;
    }

    @GetMapping("/api/ybm/clusters")
    public String getClusters(@RequestParam(name = "accountid", required = false) String aAccountId
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
        }

        return responseFromCall;
    }


    @GetMapping("/api/ybm/restartnodes")
    public List<String>  restartNodes(@RequestParam(name = "accountid", required = false) String aAccountId
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
            return response;
        }
        System.out.println("Number of stopped nodes:"+stoppedNodes.size());

        if(stoppedNodes.size() == 0){
            response.add("All Nodes are Up and nothing to restart!");
            return response;
        }

        // We will start the stopped nodes now.
        for(String nodeName: stoppedNodes){
            StringBuilder sbUriToStartNodes = new StringBuilder(baseUri);
            sbUriToStartNodes.append("/").append(aAccountId).append("/projects/").append(aProjectId).append("/clusters/").append(aClusterId).append("/nodes/op");
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
                responseFromCall = "error:"+e.getMessage();
            }
            response.add(responseFromCall);
            System.out.println("responseFromCall:"+responseFromCall);
        }
        return response;

    }

    @GetMapping("/api/ybm/stopnodes")
    public List<String> stopNodes(@RequestParam(name = "nodenames", required = true) String nodenames
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
        sbUri.append("/").append(aAccountId).append("/projects/").append(aProjectId).append("/clusters/").append(aClusterId).append("/nodes/op");

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
                responseFromCall = "error:"+e.getMessage();
            }
            listOfResponses.add(responseFromCall);

        }

        return listOfResponses;
    }


    private YbmNodeListResponseModel getNodeList(String accountId, String projectId, String clusterId){
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


}
