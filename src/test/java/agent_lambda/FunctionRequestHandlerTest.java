package agent_lambda;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FunctionRequestHandlerTest {

    private static FunctionRequestHandler handler;

    @BeforeAll
    public static void setupServer() {
        handler = new FunctionRequestHandler();
    }

    @AfterAll
    public static void stopServer() {
        if (handler != null) {
            handler.getApplicationContext().close();
        }
    }

    @Test
    @Disabled
    public void testStopInstances() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/");
        request.setBody("stop");
        APIGatewayProxyResponseEvent response = handler.execute(request);
        assertEquals(200, response.getStatusCode().intValue());
        assertEquals("{\"result\":\"success\"}", response.getBody());
    }

    @Test
    public void testEc2Collector() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/");
        request.setBody("ec2");
        APIGatewayProxyResponseEvent response = handler.execute(request);
        assertEquals(200, response.getStatusCode().intValue());
        assertEquals("{\"result\":\"success\"}", response.getBody());
    }

    @Test
    public void testLambdaCollector() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/");
        request.setBody("lambda");
        APIGatewayProxyResponseEvent response = handler.execute(request);
        assertEquals(200, response.getStatusCode().intValue());
        assertEquals("{\"result\":\"success\"}", response.getBody());
    }

    @Test
    public void testCpuCollector() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/");
        request.setBody("cpu");
        APIGatewayProxyResponseEvent response = handler.execute(request);
        assertEquals(200, response.getStatusCode().intValue());
        assertEquals("{\"result\":\"success\"}", response.getBody());
    }
}
