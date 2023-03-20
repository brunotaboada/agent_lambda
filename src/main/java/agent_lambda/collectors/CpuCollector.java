package agent_lambda.collectors;

import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static java.lang.System.currentTimeMillis;

@Singleton
public class CpuCollector {
    private static final Logger log = LoggerFactory.getLogger(CpuCollector.class);
    private final Hosts hosts;

    public CpuCollector(Hosts hosts) {
        this.hosts = hosts;
    }

    public void collect() {
        try {

            var jsch = hosts.getjSch();
            var hostsInfos = hosts.getHosts();
            if (hostsInfos.isEmpty()) {
                log.info("Nothing to do.");
                return;
            }

            ZabbixSender zabbixSender = hosts.zabbixSender(jsch, hostsInfos);

            if (Objects.isNull(zabbixSender)) {
                log.info("Zabbix server is down.");
                return;
            }

            for (var host : hostsInfos) {
                var session = hosts.getSession(jsch, "ubuntu", host.ip);
                var cpuInfo = hosts.getChannel(session, "sudo mpstat 1 1 | grep Average");
                if(!cpuInfo.isEmpty()){
                    var value = cpuInfo.get(0).split("   ")[1].replace("all","").trim();
                    if(value.equals("")){
                        value = StringUtils.substringAfterLast(cpuInfo.get(0),"all").split("    ")[1];
                    }
                    host.cpu = value;
                }
                session.disconnect();

                var cpuReq = DataObject.builder()
                    .host(host.id)
                    .key("cpu.usage")
                    .value(host.cpu)
                    .clock(currentTimeMillis() / 1000)
                    .build();

                var cpuResponse = zabbixSender.send(cpuReq);

                if (cpuResponse.success()) {
                    log.info("Cpu utilization metric sent successfully for " + host.id);
                } else {
                    log.error("Unable to send the Cpu utilization for " + host.id);
                }
            }
        } catch (Exception e) {
            log.error("An error occurred when seding metrics.", e);
        }
    }

}
