package com.yugabyte.simulation.controller;

import com.yugabyte.simulation.model.ybm.YbmNodeListResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

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

    @GetMapping("/api/ybm/restartnodes")
    public List<String>  restartNodes(){
        // Lets first find the list of stopped nodes.
        StringBuilder sbUri = new StringBuilder(baseUri);
        sbUri.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters/").append(clusterId).append("/nodes");
        List<String> response = new ArrayList<>();

        YbmNodeListResponseModel model = webClientBuilder.build()
                .get()
                .uri(sbUri.toString())
                .header("Authorization","Bearer "+apiKey)
                .retrieve()
                .bodyToMono(YbmNodeListResponseModel.class)
                .block()
                ;

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
            String responseFromCall = webClientBuilder.build()
                    .post()
                    .uri(sbUriToStartNodes.toString())
                    .header("Authorization","Bearer "+apiKey)
                    .header("Content-Type","application/json")
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block()
                    ;
            response.add(responseFromCall);
            System.out.println("responseFromCall:"+responseFromCall);
        }
        return response;

    }

    @GetMapping("/api/ybm/stopnode/{nodename}")
    public String stopNode(@PathVariable String nodename){
        // Lets first find the list of stopped nodes.
        StringBuilder sbUri = new StringBuilder(baseUri);
        sbUri.append("/").append(accountId).append("/projects/").append(projectId).append("/clusters/").append(clusterId).append("/nodes/op");

        String body = "{\n" +
                "  \"action\": \"STOP\",\n" +
                "  \"node_name\": \""+nodename+"\"\n" +
                "}";


        String responseFromCall = webClientBuilder.build()
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
        return responseFromCall;

    }


}
