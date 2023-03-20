package agent_lambda;

import agent_lambda.actions.Stop;
import agent_lambda.collectors.Ec2Collector;
import agent_lambda.collectors.LambdaCollector;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.function.aws.MicronautRequestHandler;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Objects;

public class FunctionRequestHandler extends MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger log = LoggerFactory.getLogger(FunctionRequestHandler.class);
    @Inject
    ObjectMapper objectMapper;

    @Inject
    Stop stop;

    @Inject
    Ec2Collector ec2Collector;

    @Inject
    LambdaCollector lambdaCollector;

    @Override
    public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Starting Zabbix Agent lambda.");
            if(Objects.nonNull(input.getBody()) && input.getBody().equals("stop")){
                log.info("Stopping instances.");
                stop.stop();
            }
            if(Objects.nonNull(input.getBody()) && input.getBody().equals("ec2")){
                log.info("Collecting EC2 metrics.");
                ec2Collector.collect();
            }
            if(Objects.nonNull(input.getBody()) && input.getBody().equals("lambda")){
                log.info("Collecting LAMBDA metrics.");
                lambdaCollector.collect();
            }
            String json = objectMapper.writeValueAsString(Collections.singletonMap("result", "success"));
            response.setStatusCode(200);
            response.setBody(json);
        } catch (JsonProcessingException e) {
            response.setStatusCode(500);
        }
        return response;
    }

}
