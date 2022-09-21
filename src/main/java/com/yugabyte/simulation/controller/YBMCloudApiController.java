package com.yugabyte.simulation.controller;

import com.yugabyte.simulation.model.ybm.YbmNodeListResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

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
    public YbmNodeListResponseModel getListOfNodesApi(){
        return getNodeList(accountId,projectId,clusterId);
    }

    @GetMapping("/api/ybm/projects")
    public String getProjects(){
        StringBuilder sb = new StringBuilder(baseUri);
        sb.append("/").append(accountId).append("/projects");

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

    @GetMapping("/api/ybm/clusters/")
    public String getClusters(){
        StringBuilder sb = new StringBuilder(baseUri);
        sb.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters");

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
    public List<String>  restartNodes(){
        List<String> response = new ArrayList<>();
        // Lets first find the list of stopped nodes.
        YbmNodeListResponseModel model = getNodeList(accountId,projectId,clusterId);
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
            sbUriToStartNodes.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters/").append(clusterId).append("/nodes/op");
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

    @GetMapping("/api/ybm/stopnodes/{nodenames}")
    public List<String> stopNodes(@PathVariable String[] nodenames){
        List<String> listOfResponses = new ArrayList<>();
        // Lets first find the list of stopped nodes.
        StringBuilder sbUri = new StringBuilder(baseUri);
        sbUri.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters/").append(clusterId).append("/nodes/op");

        for(String nodename: nodenames){
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

    private Mono<? extends Throwable> handleError(String message) {
        return Mono.error(Exception::new);
    }

    private YbmNodeListResponseModel getNodeList(String accountId, String projectId, String clusterId){
        StringBuilder sbUri = new StringBuilder(baseUri);
        sbUri.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters/").append(clusterId).append("/nodes");
        YbmNodeListResponseModel model = null
                ;
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
