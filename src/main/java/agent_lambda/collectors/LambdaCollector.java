package agent_lambda.collectors;

import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import static java.lang.System.currentTimeMillis;

@Singleton
public class LambdaCollector {

    private static final Logger log = LoggerFactory.getLogger(LambdaCollector.class);
    private final Hosts hosts;
    private final CloudWatchClient cloudWatchClient;

    public LambdaCollector(CloudWatchClient cloudWatchClient, Hosts hosts) {
        this.cloudWatchClient = cloudWatchClient;
        this.hosts = hosts;
    }

    public void collect() {
        var startTime = Instant.now().minus(Duration.ofHours(1));
        var endTime = Instant.now();
        var metricDataQueries = new ArrayList<MetricDataQuery>() {{
            add(MetricDataQuery.builder()
                .id("m1")
                .metricStat(MetricStat.builder()
                    .metric(Metric.builder()
                        .namespace("AWS/Lambda")
                        .dimensions(Dimension.builder()
                            .name("FunctionName")
                            .value("agent_lambda")
                            .build())
                        .metricName("Invocations")
                        .build())
                    .period(60*60)
                    .stat("SampleCount")
                    .build())
                .returnData(true)
                .build());
        }};

        var request = GetMetricDataRequest.builder()
            .startTime(startTime)
            .endTime(endTime)
            .metricDataQueries(metricDataQueries)
            .build();

        var metrics = cloudWatchClient.getMetricData(request);

        if (metrics.hasMetricDataResults()) {
            var mResults = metrics.metricDataResults();
            if (!mResults.isEmpty()) {
                if (!mResults.get(0).values().isEmpty()) {
                    var hostsInfos = this.hosts.getHosts();
                    if (hostsInfos.isEmpty()) {
                        log.info("Nothing to do.");
                        return;
                    }

                    ZabbixSender zabbixSender = hosts.zabbixSender(hosts.getjSch(), hostsInfos);
                    if (Objects.isNull(zabbixSender)) {
                        log.info("Zabbix server is down.");
                        return;
                    }

                    var invocations = DataObject.builder()
                        .host("Agent Lambda")
                        .key("lambda.invocations")
                        .value(String.valueOf(mResults.get(0).values().get(0).intValue()))
                        .clock(currentTimeMillis() / 1000)
                        .build();

                    var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("America/Montreal"));

                    var lastInvocation = DataObject.builder()
                        .host("Agent Lambda")
                        .key("lambda.last.invocation")
                        .value(simpleDateFormat.format(new Date()))
                        .clock(currentTimeMillis() / 1000)
                        .build();

                    try {
                        var response = zabbixSender.send(invocations);
                        if (response.success()) {
                            log.info("Lambda invocations metric sent successfully.");
                        } else {
                            log.info("Unable to send lambda invocations metric.");
                        }
                        response = zabbixSender.send(lastInvocation);
                        if (response.success()) {
                            log.info("Lambda last invocation metric sent successfully.");
                        } else {
                            log.info("Unable to send lambda last invocation metric.");
                        }
                    } catch (IOException e) {
                        log.error("Unable to send agent lambda metrics.");
                    }

                }
            }
        }
    }
}