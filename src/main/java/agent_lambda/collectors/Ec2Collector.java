package agent_lambda.collectors;

import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static java.lang.System.currentTimeMillis;

@Singleton
public class Ec2Collector {
    private static final Logger log = LoggerFactory.getLogger(Ec2Collector.class);
    private final Hosts hosts;

    public Ec2Collector(Hosts hosts) {
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
                var memInfo = hosts.getChannel(session, "sudo df -h | grep /dev/root");

                if(!memInfo.isEmpty()){
                    var strings = memInfo.get(0).split(" ");
                    host.free = strings[11].replace("G","");
                    host.size = strings[7].replace("G","");
                    host.used = strings[9].replace("G","");
                }
                session.disconnect();

                var freeReq = DataObject.builder()
                    .host(host.id)
                    .key("disk.free")
                    .value(host.free)
                    .clock(currentTimeMillis() / 1000)
                    .build();

                var usedReq = DataObject.builder()
                    .host(host.id)
                    .key("disk.used")
                    .value(host.used)
                    .clock(currentTimeMillis() / 1000)
                    .build();

                var sizeReq = DataObject.builder()
                    .host(host.id)
                    .key("disk.size")
                    .value(host.size)
                    .clock(currentTimeMillis() / 1000)
                    .build();

                var freeResponse = zabbixSender.send(freeReq);
                var usedResponse = zabbixSender.send(usedReq);
                var sizeResponse = zabbixSender.send(sizeReq);

                if (freeResponse.success()) {
                    log.info("Disk free metric sent successfully for " + host.id);
                } else {
                    log.error("Unable to send the disk free metric for " + host.id);
                }
                if (usedResponse.success()) {
                    log.info("Disk used metric sent successfully for " + host.id);
                } else {
                    log.error("Unable to send the disk used metric for " + host.id);
                }
                if (sizeResponse.success()) {
                    log.info("Disk size metric sent successfully for " + host.id);
                } else {
                    log.error("Unable to send the disk size metric for " + host.id);
                }
            }
        } catch (Exception e) {
            log.error("An error occurred when seding metrics.", e);
        }
    }

}
